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

import io.wcm.caravan.maven.plugins.haldocs.generator.ServiceDocGenerator;
import io.wcm.caravan.maven.plugins.haldocs.model.Service;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Generates HAL documentation for service.
 */
@Mojo(name = "generate-hal-docs", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, requiresProject = true, threadSafe = true)
public class GenerateHalDocsMojo extends AbstractBaseMojo {

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
      // generate HTML documentation for service
      Service service = getServiceInfos();
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
  private Service getServiceInfos() {
    Service service = new Service();

    service.setServiceId(serviceId);
    service.setName(project.getName());

    return service;
  }

  @Override
  protected String getGeneratedResourcesDirectoryPath() {
    return generatedResourcesDirectory;
  }

}
