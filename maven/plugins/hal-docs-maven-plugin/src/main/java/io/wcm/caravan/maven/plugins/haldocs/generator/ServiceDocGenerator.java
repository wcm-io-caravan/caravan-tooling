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
package io.wcm.caravan.maven.plugins.haldocs.generator;

import io.wcm.caravan.maven.plugins.haldocs.model.LinkRelation;
import io.wcm.caravan.maven.plugins.haldocs.model.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.FileWriterWithEncoding;
import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.StringUtils;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import com.github.jknack.handlebars.io.TemplateLoader;
import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ResourceInfo;

/**
 * Generate HTML documentation from model.
 */
public class ServiceDocGenerator {

  private static final String CLASSPATH_TEMPLATES = "TEMPLATE-INF/templates";
  private static final String CLASSPATH_FRONTEND = "TEMPLATE-INF/frontend";

  private final Handlebars handlebars;
  private final Template serviceTemplate;
  private final Template linkRelationTemplate;

  private final ClassPath classPath;

  /**
   * Initialize generator.
   * @throws IOException
   */
  public ServiceDocGenerator() throws IOException {
    TemplateLoader templateLoader = new ClassPathTemplateLoader("/" + CLASSPATH_TEMPLATES, "");
    handlebars = new Handlebars(templateLoader);
    serviceTemplate = handlebars.compile("index.html.hbs");
    linkRelationTemplate = handlebars.compile("linkRelation.html.hbs");

    classPath = ClassPath.from(getClass().getClassLoader());
  }

  /**
   * Generate HTML documentation.
   * @param service Service model
   * @param targetDir Target directory.
   */
  public void generate(Service service, File targetDir) {
    generateHtml(service, targetDir);
    copyFrontend(targetDir);
  }

  private void generateHtml(Service service, File targetDir) {
    generateServiceHtml(service, targetDir);
    service.getLinkRelations().forEach(rel -> generateLinkRelationHtml(service, rel, targetDir));
  }

  private void generateServiceHtml(Service service, File targetDir) {
    File targetFile = new File(targetDir, getServiceFilename(service));
    Map<String, Object> model = ImmutableMap.<String, Object>builder()
        .put("service", service)
        .build();
    generateTemplatedFile(model, serviceTemplate, targetFile);
  }

  private void generateLinkRelationHtml(Service service, LinkRelation linkRelation, File targetDir) {
    File targetFile = new File(targetDir, getLinkRelationFilename(linkRelation));
    Map<String, Object> model = ImmutableMap.<String, Object>builder()
        .put("service", service)
        .put("linkRelation", linkRelation)
        .build();
    generateTemplatedFile(model, linkRelationTemplate, targetFile);
  }

  private void generateTemplatedFile(Map<String, Object> model, Template template, File targetFile) {
    try (Writer writer = new FileWriterWithEncoding(targetFile, CharEncoding.UTF_8)) {
      template.apply(model, writer);
    }
    catch (IOException ex) {
      throw new RuntimeException("Unable to generate file: " + targetFile.getPath(), ex);
    }
  }

  private void copyFrontend(File targetDir) {
    classPath.getResources().stream()
    .filter(info -> StringUtils.startsWith(info.getResourceName(), CLASSPATH_FRONTEND))
    .forEach(info -> copyFrontendFile(info, targetDir));
  }

  private void copyFrontendFile(ResourceInfo resourceInfo, File targetDir) {
    String relativePath = StringUtils.substringAfter(resourceInfo.getResourceName(), CLASSPATH_FRONTEND);
    File targetFile = new File(targetDir, relativePath);
    try (InputStream is = resourceInfo.url().openStream()) {
      File directory = targetFile.getParentFile();
      if (!directory.exists()) {
        directory.mkdirs();
      }
      try (OutputStream os = new FileOutputStream(targetFile)) {
        IOUtils.copy(is, os);
      }
    }
    catch (IOException ex) {
      throw new RuntimeException("Error copying file " + resourceInfo.getResourceName() + " to " + targetFile.getPath(), ex);
    }
  }

  private String getServiceFilename(Service service) {
    return "index.html";
  }

  private String getLinkRelationFilename(LinkRelation linkRelation) {
    return "rel_" + StringUtils.replace(linkRelation.getRel(), ":", "$") + ".html";
  }

}