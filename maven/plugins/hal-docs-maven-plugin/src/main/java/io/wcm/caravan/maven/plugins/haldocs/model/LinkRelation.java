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

import org.apache.commons.lang3.StringUtils;

/**
 * Describes a HAL link relation.
 */
public class LinkRelation {

  private String rel;
  private String descriptionMarkup;
  private String jsonSchemaRef;
  private List<LinkRelation> embeddedResourcesLinkRelations;

  public String getRel() {
    return this.rel;
  }

  public void setRel(String rel) {
    this.rel = rel;
  }

  public String getDescriptionMarkup() {
    return this.descriptionMarkup;
  }

  public void setDescriptionMarkup(String descriptionMarkup) {
    this.descriptionMarkup = descriptionMarkup;
  }

  public String getJsonSchemaRef() {
    return this.jsonSchemaRef;
  }

  public void setJsonSchemaRef(String jsonSchemaRef) {
    this.jsonSchemaRef = jsonSchemaRef;
  }

  public List<LinkRelation> getEmbeddedResourcesLinkRelations() {
    return this.embeddedResourcesLinkRelations;
  }

  public void setEmbeddedResourcesLinkRelations(List<LinkRelation> embeddedResourcesLinkRelations) {
    this.embeddedResourcesLinkRelations = embeddedResourcesLinkRelations;
  }

  public String getFilename() {
    return "rel_" + StringUtils.replace(getRel(), ":", "-") + ".html";
  }

}
