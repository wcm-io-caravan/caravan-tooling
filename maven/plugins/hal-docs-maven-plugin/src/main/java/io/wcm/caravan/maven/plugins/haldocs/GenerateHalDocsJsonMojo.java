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
import io.wcm.caravan.commons.haldocs.model.LinkRelation;
import io.wcm.caravan.commons.haldocs.model.Service;
import io.wcm.caravan.maven.plugins.haldocs.generator.ServiceDocGenerator;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.net.URLClassLoader;
import java.util.Arrays;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thoughtworks.qdox.JavaProjectBuilder;
import com.thoughtworks.qdox.model.JavaAnnotatedElement;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaField;

/**
 * Generates HAL documentation JSON files for service.
 */
@Mojo(name = "generate-hal-docs-json", defaultPhase = LifecyclePhase.PROCESS_CLASSES, requiresProject = true, threadSafe = true)
public class GenerateHalDocsJsonMojo extends AbstractBaseMojo {

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

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  static {
    // ensure only field serialization is used for jackson
    OBJECT_MAPPER.setVisibility(OBJECT_MAPPER.getSerializationConfig().getDefaultVisibilityChecker()
        .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
        .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
        .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
        .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
  }

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    try {
      // get classloader for all "compile" dependencies
      ClassLoader compileClassLoader = URLClassLoader.newInstance(getCompileClasspathElementURLs(), getClass().getClassLoader());

      // generate HTML documentation for service
      Service service = getServiceInfos(compileClassLoader);

      // generate JSON for service info

      ServiceDocGenerator generator = new ServiceDocGenerator(msg -> getLog().info(msg));
      generator.generate(service, getGeneratedResourcesDirectory());

      // add as resources to classpath
      addResource(getGeneratedResourcesDirectory().getPath(), target);
    }
    catch (Throwable ex) {
      throw new MojoExecutionException("Generating HAL documentation failed: " + ex.getMessage(), ex);
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

      serviceInfo.getFields().stream()
      .filter(field -> hasAnnotation(field, LinkRelationDoc.class))
      .map(field -> toLinkRelation(serviceInfo, field, compileClassLoader))
      .forEach(service::addLinkRelation);
    }

    // resolve link relations
    service.resolve();

    return service;
  }

  /**
   * Builds a {@link LinkRelation} from a field definition with {@link LinkRelationDoc} annotation.
   * @param javaClazz QDox class
   * @param javaField QDox field
   * @param compileClassLoader Classloader for compile dependencies
   * @return Link relation
   */
  private LinkRelation toLinkRelation(JavaClass javaClazz, JavaField javaField, ClassLoader compileClassLoader) {
    LinkRelation rel = new LinkRelation();

    rel.setDescriptionMarkup(javaField.getComment());

    rel.setRel(getStaticFieldValue(javaClazz, javaField, compileClassLoader, String.class));

    LinkRelationDoc relDoc = getAnnotation(javaClazz, javaField, compileClassLoader, LinkRelationDoc.class);
    rel.setJsonSchemaRef(relDoc.jsonSchema());
    Arrays.stream(relDoc.nested()).forEach(nested -> rel.addNestedLinkRelation(nested.value(), nested.description()));

    return rel;
  }

  /**
   * Checks if the given element has an annotation set.
   * @param clazz QDox class
   * @param annotationClazz Annotation class
   * @return true if annotation is present
   */
  private boolean hasAnnotation(JavaAnnotatedElement clazz, Class<? extends Annotation> annotationClazz) {
    return clazz.getAnnotations().stream()
        .filter(item -> item.getType().isA(annotationClazz.getName()))
        .count() > 0;
  }

  /**
   * Get constant field value.
   * @param javaClazz QDox class
   * @param javaField QDox field
   * @param compileClassLoader Classloader for compile dependencies
   * @param fieldType Field type
   * @return Value
   */
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

  /**
   * Get annotation for field.
   * @param javaClazz QDox class
   * @param javaField QDox field
   * @param compileClassLoader Classloader for compile dependencies
   * @param annotationType Annotation type
   * @return Annotation of null if not present
   */
  private <T extends Annotation> T getAnnotation(JavaClass javaClazz, JavaField javaField, ClassLoader compileClassLoader, Class<T> annotationType) {
    try {
      Class<?> clazz = compileClassLoader.loadClass(javaClazz.getFullyQualifiedName());
      Field field = clazz.getField(javaField.getName());
      return field.getAnnotation(annotationType);
    }
    catch (ClassNotFoundException | NoSuchFieldException | SecurityException | IllegalArgumentException ex) {
      throw new RuntimeException("Unable to get contanst value of field '" + javaClazz.getName() + "#" + javaField.getName() + ":\n" + ex.getMessage(), ex);
    }
  }

  @Override
  protected String getGeneratedResourcesDirectoryPath() {
    return generatedResourcesDirectory;
  }

}