/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2015 wcm.io
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
package io.wcm.caravan.maven.plugins.haldocs;

import io.wcm.caravan.commons.haldocs.annotations.LinkRelationDoc;
import io.wcm.caravan.commons.haldocs.annotations.ServiceDoc;
import io.wcm.caravan.maven.plugins.haldocs.generator.ServiceDocGenerator;
import io.wcm.caravan.maven.plugins.haldocs.model.LinkRelation;
import io.wcm.caravan.maven.plugins.haldocs.model.Service;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URLClassLoader;
import java.util.stream.Collectors;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.thoughtworks.qdox.JavaProjectBuilder;
import com.thoughtworks.qdox.model.JavaAnnotatedElement;
import com.thoughtworks.qdox.model.JavaAnnotation;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaField;

/**
 * Generates HAL documentation for service.
 */
@Mojo(name = "generate-hal-docs", defaultPhase = LifecyclePhase.PROCESS_CLASSES, requiresProject = true, threadSafe = true)
public class GenerateHalDocsMojo extends AbstractBaseMojo {

  /**
   * Paths containing the java source files.
   */
  @Parameter(defaultValue = "${basedir}/src/main/java")
  private String source;

  /**
   * Service ID
   */
  @Parameter(required = true)
  private String serviceId;

  /**
   * Relative target path for the generated resources.
   */
  @Parameter(defaultValue = "HAL-DOCS-INF")
  private String target;

  @Parameter(defaultValue = "generated-hal-docs-resources")
  private String generatedResourcesDirectory;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    try {
      // get classloader for all "compile" dependencies
      ClassLoader compileClassLoader = URLClassLoader.newInstance(getCompileClasspathElementURLs());

      // generate HTML documentation for service
      Service service = getServiceInfos(compileClassLoader);
      ServiceDocGenerator generator = new ServiceDocGenerator();
      generator.generate(service, getGeneratedResourcesDirectory());

      // add as resources to classpath
      addResource(getGeneratedResourcesDirectory().getPath(), target);
    }
    catch (Throwable ex) {
      throw new MojoExecutionException("Generationg JSON Schema files failed: " + ex.getMessage(), ex);
    }
  }

  /**
   * Get service infos from current maven project.
   * @return Service
   */
  private Service getServiceInfos(ClassLoader compileClassLoader) {
    Service service = new Service();

    // get some service properties from pom
    service.setServiceId(serviceId);
    service.setName(project.getName());

    // find @ServiceDoc annotated class in source folder
    JavaProjectBuilder builder = new JavaProjectBuilder();
    builder.addSourceTree(new File(source));
    JavaClass serviceInfo = builder.getSources().stream()
        .flatMap(javaSource -> javaSource.getClasses().stream())
        .filter(javaClass -> hasAnnotation(javaClass, ServiceDoc.class))
        .findFirst().orElse(null);

    // populate further service information from @ServiceDoc class and @LinkRelationDoc fields
    if (serviceInfo != null) {
      service.setDescriptionMarkup(serviceInfo.getComment());
      service.setLinkRelations(serviceInfo.getFields().stream()
          .filter(field -> hasAnnotation(field, LinkRelationDoc.class))
          .map(field -> toLinkRelation(serviceInfo, field, compileClassLoader))
          .collect(Collectors.toList()));
    }

    return service;
  }

  private LinkRelation toLinkRelation(JavaClass javaClazz, JavaField field, ClassLoader compileClassLoader) {
    LinkRelation rel = new LinkRelation();
    rel.setDescriptionMarkup(field.getComment());
    rel.setRel(getStaticFieldValue(javaClazz, field, compileClassLoader, String.class));
    rel.setJsonSchemaRef(getJsonSchemaRef(field));
    return rel;
  }

  private String getJsonSchemaRef(JavaField field) {
    return getAnnotationValue(field, LinkRelationDoc.class, "jsonSchema", String.class);
  }

  private boolean hasAnnotation(JavaAnnotatedElement clazz, Class<?> annotationClazz) {
    return clazz.getAnnotations().stream()
        .filter(item -> item.getType().isA(annotationClazz.getName()))
        .count() > 0;
  }

  @SuppressWarnings("unchecked")
  private <T> T getAnnotationValue(JavaAnnotatedElement clazz, Class<?> annotationClazz, String property, Class<T> propertyType) {
    JavaAnnotation annotation = clazz.getAnnotations().stream()
        .filter(item -> item.getType().isA(annotationClazz.getName()))
        .findFirst().orElse(null);
    if (annotation != null) {
      return (T)annotation.getNamedParameter(property);
    }
    else {
      return null;
    }
  }

  @SuppressWarnings("unchecked")
  private <T> T getStaticFieldValue(JavaClass javaClazz, JavaField javaField, ClassLoader compileClassLoader, Class<T> fieldType) {
    try {
      Class<?> clazz = compileClassLoader.loadClass(javaClazz.getFullyQualifiedName());
      Field field = clazz.getField(javaField.getName());
      return (T)field.get(fieldType);
    }
    catch (ClassNotFoundException | NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException ex) {
      throw new RuntimeException("Unable to get contanst value of field '" + javaClazz.getName() + "#" + javaField.getName() + ":\n" + ex.getMessage(), ex);
    }
  }

  @Override
  protected String getGeneratedResourcesDirectoryPath() {
    return generatedResourcesDirectory;
  }

}
