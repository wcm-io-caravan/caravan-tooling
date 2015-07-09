/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2014 wcm.io
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package io.wcm.maven.plugins.i18n;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Set;

import org.apache.maven.model.Build;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.SelectorUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.factories.SchemaFactoryWrapper;
import com.google.common.reflect.ClassPath;

/**
 * Generates JSON schema files for Java POJOs using Jackson 'jackson-module-jsonSchema'.
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.PROCESS_CLASSES, requiresProject = true, threadSafe = true)
public class GenerateMojo extends AbstractMojo {

  /**
   * Paths containing the class files to generate JSON schema for.
   */
  @Parameter(defaultValue = "${project.build.directory}/classes")
  private String source;

  /**
   * Ant-style include patterns for java package names to include. Use "." as separator.
   */
  @Parameter(required = true)
  private Set<String> includes;

  /**
   * Ant-style exclude patterns for java package names to exclude. Use "." as separator.
   */
  @Parameter
  private Set<String> excludes;

  /**
   * Relative target path for the generated resources.
   */
  @Parameter(defaultValue = "JSON-SCHEMA-INF")
  private String target;

  @Parameter(defaultValue = "generated-json-schema-resources")
  private String generatedResourcesFolderPath;

  @Parameter(property = "project", required = true, readonly = true)
  private MavenProject project;

  private File generatedResourcesFolder;

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    try {
      File classesDirectory = getSourceDirectory();
      if (!classesDirectory.exists()) {
        return;
      }

      // build class loader to get classes to generate resources for
      URL[] urls = project.getCompileClasspathElements().stream()
          .map(path -> {
            try {
              return new File(path).toURI().toURL();
            }
            catch (MalformedURLException ex) {
              throw new RuntimeException(ex);
            }
          })
          .toArray(size -> new URL[size]);
      // set plugin classloader as parent classloader e.g. to support jackson annotations
      ClassLoader classesClassLoader = URLClassLoader.newInstance(urls, getClass().getClassLoader());

      // generate schema for all classes that match includes/excludes
      ClassPath.from(classesClassLoader).getAllClasses().stream()
      .filter(info -> isIncluded(info.getName()) && !isExcluded(info.getName()))
      .map(info -> info.load())
      .forEach(this::generateSchema);

      // add as resources to classpath
      addResource(getGeneratedResourcesFolder().getPath(), target);
    }
    catch (Throwable ex) {
      throw new MojoExecutionException("Generationg JSON Schema files failed: " + ex.getMessage(), ex);
    }
  }

  private boolean isIncluded(String className) {
    if (includes == null || includes.size() == 0) {
      // generate nothing if no include defined - it makes no sense to generate for all classes from classpath
      return false;
    }
    return includes.stream()
        .filter(pattern -> SelectorUtils.matchPath(pattern, className, ".", true))
        .count() > 0;
  }

  private boolean isExcluded(String className) {
    if (excludes == null || excludes.size() == 0) {
      return false;
    }
    return excludes.stream()
        .filter(pattern -> SelectorUtils.matchPath(pattern, className, ".", true))
        .count() == 0;
  }

  private void generateSchema(Class clazz) {
    try {
      File targetFile = new File(getGeneratedResourcesFolder(), clazz.getName() + ".json");
      getLog().info("Generate JSON schema: " + targetFile.getName());
      SchemaFactoryWrapper schemaFactory = new SchemaFactoryWrapper();
      OBJECT_MAPPER.acceptJsonFormatVisitor(OBJECT_MAPPER.constructType(clazz), schemaFactory);
      JsonSchema jsonSchema = schemaFactory.finalSchema();
      OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValue(targetFile, jsonSchema);
    }
    catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  private void addResource(String sourceDirectory, String targetPath) {

    // construct resource
    Resource resource = new Resource();
    resource.setDirectory(sourceDirectory);
    resource.setTargetPath(targetPath);

    // add to build
    Build build = this.project.getBuild();
    build.addResource(resource);
    getLog().debug("Added resource: " + resource.getDirectory() + " -> " + resource.getTargetPath());
  }

  /**
   * Get directory containing source i18n files.
   * @return directory containing source i18n files.
   * @throws IOException
   */
  private File getSourceDirectory() throws IOException {
    File file = new File(source);
    if (!file.isDirectory()) {
      getLog().debug("Could not find directory at '" + source + "'");
    }
    return file.getCanonicalFile();
  }

  private File getGeneratedResourcesFolder() {
    if (generatedResourcesFolder == null) {
      String generatedResourcesFolderAbsolutePath = this.project.getBuild().getDirectory() + "/" + generatedResourcesFolderPath;
      generatedResourcesFolder = new File(generatedResourcesFolderAbsolutePath);
      if (!generatedResourcesFolder.exists()) {
        generatedResourcesFolder.mkdirs();
      }
    }
    return generatedResourcesFolder;
  }

}
