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

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;


public class ServiceDocGeneratorTest {

  private ServiceDocGenerator underTest;

  @Before
  public void setUp() throws Exception {
    underTest = new ServiceDocGenerator();
  }

  @Test
  public void testGenerate() throws Exception {
    Service service = new Service();
    service.setServiceId("/my/test/service");
    service.setName("My Test Service");
    service.setDescriptionMarkup("<p>This is a lengthy description of the service. "
        + "This is a lengthy description of the service. This is a lengthy description of the service. "
        + "This is a lengthy description of the service. This is a lengthy description of the service. "
        + "This is a lengthy description of the service.</p>"
        + "<p>This is a lengthy description of the service.</p>");

    LinkRelation rel1 = new LinkRelation();
    rel1.setRel("ns:rel1");
    rel1.setDescriptionMarkup("<p>This is a lengthy description of the relation. "
        + "This is a lengthy description of the relation. This is a lengthy description of the relation. "
        + "This is a lengthy description of the relation. This is a lengthy description of the relation. "
        + "This is a lengthy description of the relation.</p>"
        + "<p>This is a lengthy description of the relation.</p>");
    rel1.setJsonSchemaRef("schema1.json");

    LinkRelation rel2 = new LinkRelation();
    rel2.setRel("ns:rel2");
    rel2.setDescriptionMarkup("<p>This is a description of the relation.</p>");
    rel2.setJsonSchemaRef("schema2.json");

    LinkRelation rel3 = new LinkRelation();
    rel3.setRel("ns:rel3");
    rel3.setDescriptionMarkup("<p>This is a description of the relation.</p>");
    rel3.setJsonSchemaRef("schema3.json");

    service.setLinkRelations(ImmutableList.of(rel1, rel2, rel3));
    rel1.setEmbeddedResourcesLinkRelations(ImmutableList.of(rel2, rel3));

    underTest.generate(service, new File("target/documentation-test"));
  }

}