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
package io.wcm.caravan.maven.plugins.haldocs.model;

import java.util.List;

import com.google.common.collect.ImmutableList;

/**
 * Describes a HAL RESTful service.
 */
public class Service {

  private String serviceId;
  private String name;
  private String descriptionMarkup;
  private List<LinkRelation> linkRelations = ImmutableList.of();

  public String getServiceId() {
    return this.serviceId;
  }

  public void setServiceId(String serviceId) {
    this.serviceId = serviceId;
  }

  public String getName() {
    return this.name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescriptionMarkup() {
    return this.descriptionMarkup;
  }

  public void setDescriptionMarkup(String descriptionMarkup) {
    this.descriptionMarkup = descriptionMarkup;
  }

  public List<LinkRelation> getLinkRelations() {
    return this.linkRelations;
  }

  public void setLinkRelations(List<LinkRelation> linkRelations) {
    this.linkRelations = linkRelations;
  }

  public String getFilename() {
    return "index.html";
  }

}