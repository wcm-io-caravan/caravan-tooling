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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.ImmutableList;

/**
 * Describes a HAL link relation.
 */
public class LinkRelation implements Comparable<LinkRelation> {

  private String rel;
  private String descriptionMarkup;
  private String jsonSchemaRef;
  private String[] rels;
  private Service service;

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

  /**
   * @return Get link relations of embedded resources.
   */
  public List<LinkRelation> getEmbeddedResourcesLinkRelations() {
    if (rels == null) {
      return ImmutableList.of();
    }
    return Arrays.stream(rels)
        .map(this::lookupLinkRelation)
        .filter(linkRelation -> linkRelation != null)
        .collect(Collectors.toList());
  }

  private LinkRelation lookupLinkRelation(String lookupRel) {
    return service.getLinkRelations().stream()
        .filter(linkRelation -> StringUtils.equals(linkRelation.getRel(), lookupRel))
        .findFirst().orElse(null);
  }

  /**
   * @param value Link relation names of embeddes resources
   */
  public void setEmbeddedResourcesLinkRelations(String[] value) {
    this.rels = value;
  }

  /**
   * @return Filename for generated markup file.
   */
  public String getFilename() {
    return "rel-" + StringUtils.replace(getRel(), ":", "-") + ".html";
  }

  void setService(Service service) {
    this.service = service;
  }

  @Override
  public int hashCode() {
    return rel.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof LinkRelation)) {
      return false;
    }
    return StringUtils.equals(rel, ((LinkRelation)obj).rel);
  }

  @Override
  public int compareTo(LinkRelation o) {
    return StringUtils.defaultString(rel).compareTo(o.getRel());
  }

}
