/**
 * ============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright © 2017 AT&T Intellectual Property. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 *
 * ECOMP is a trademark and service mark of AT&T Intellectual Property.
 */
package org.onap.aai.query.builder;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.onap.aai.AAISetup;
import org.onap.aai.db.props.AAIProperties;
import org.onap.aai.exceptions.AAIException;
import org.onap.aai.introspection.Loader;
import org.onap.aai.introspection.LoaderFactory;
import org.onap.aai.introspection.ModelType;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;

public class TraversalQueryTest extends AAISetup {

	private Loader loader;

	@Mock
	private GraphTraversalSource g;

	@Before
	public void configure() throws Exception {
		loader = LoaderFactory.createLoaderForVersion(ModelType.MOXY, AAIProperties.LATEST);
	}
	
	@Test
	public void unionQuery() {
		TraversalQuery<Vertex> tQ = new TraversalQuery<>(loader, g);
		TraversalQuery<Vertex> tQ2 = new TraversalQuery<>(loader, g);
		TraversalQuery<Vertex> tQ3 = new TraversalQuery<>(loader, g);
		tQ.union(
				tQ2.getVerticesByProperty("test1", "value1"),
				tQ3.getVerticesByIndexedProperty("test2", "value2"));
		
		GraphTraversal<Vertex, Vertex> expected = __.<Vertex>start()
				.union(__.has("test1", "value1"),__.has("test2", "value2"));
		
		assertEquals("they are equal", expected, tQ.getQuery());
		
	}

	@Ignore
	@Test
	public void traversalClones() throws UnsupportedEncodingException, AAIException, URISyntaxException {
		TraversalQuery<Vertex> tQ = new TraversalQuery<>(loader, g);
		QueryBuilder<Vertex> builder = tQ.createQueryFromURI(new URI("network/test-objects/test-object/key1")).getQueryBuilder();
		GraphTraversal<Vertex, Vertex> expected = __.<Vertex>start().has("vnf-id", "key1").has("aai-node-type", "test-object");
		GraphTraversal<Vertex, Vertex> containerExpected = __.<Vertex>start().has("aai-node-type", "test-object");
		
		assertEquals("query object", expected.toString(), builder.getQuery().toString());
		assertEquals("container query object", containerExpected.toString(), builder.getContainerQuery().getQuery().toString());
		
	}

	@Ignore
	@Test
	public void nestedTraversalClones() throws UnsupportedEncodingException, AAIException, URISyntaxException {
		
		TraversalQuery<Vertex> tQ = new TraversalQuery<>(loader, g);
		QueryBuilder<Vertex> builder = tQ.createQueryFromURI(new URI("network/generic-vnfs/generic-vnf/key1/l-interfaces/l-interface/key2")).getQueryBuilder();
		GraphTraversal<Vertex, Vertex> expected = __.<Vertex>start().has("vnf-id", "key1").has("aai-node-type", "generic-vnf").out("hasLInterface").has(AAIProperties.NODE_TYPE, "l-interface").has("interface-name", "key2");
		GraphTraversal<Vertex, Vertex> containerExpected = __.<Vertex>start().has("vnf-id", "key1").has("aai-node-type", "generic-vnf").out("hasLInterface").has(AAIProperties.NODE_TYPE, "l-interface");
		
		assertEquals("query object", expected.toString(), builder.getQuery().toString());
		assertEquals("container query object", containerExpected.toString(), builder.getContainerQuery().getQuery().toString());
		
	}
	
	
	
}