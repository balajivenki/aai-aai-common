/*-
 * ============LICENSE_START=======================================================
 * org.openecomp.aai
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 * ============LICENSE_END=========================================================
 */

package org.openecomp.aai.query.builder;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import org.openecomp.aai.exceptions.AAIException;
import org.openecomp.aai.introspection.Introspector;
import org.openecomp.aai.introspection.Loader;
import org.openecomp.aai.parsers.query.QueryParser;
import org.openecomp.aai.parsers.query.QueryParserStrategy;
import org.openecomp.aai.serialization.db.EdgeType;
import org.openecomp.aai.serialization.db.exceptions.NoEdgeRuleFoundException;

/**
 * The Class QueryBuilder.
 */
public abstract class QueryBuilder implements Iterator<Vertex> {

	protected QueryParserStrategy factory = null;
	
	protected Loader loader = null;
	
	protected boolean optimize = false;
	
	protected Vertex start = null;
	protected final GraphTraversalSource source;
	
	/**
	 * Instantiates a new query builder.
	 *
	 * @param loader the loader
	 */
	public QueryBuilder(Loader loader, GraphTraversalSource source) {
		this.loader = loader;
		this.source = source;
	}
	
	/**
	 * Instantiates a new query builder.
	 *
	 * @param loader the loader
	 * @param start the start
	 */
	public QueryBuilder(Loader loader, GraphTraversalSource source, Vertex start) {
		this.loader = loader;
		this.start = start;
		this.source = source;
	}
	
	/**
	 * Gets the vertices by indexed property.
	 *
	 * @param key the key
	 * @param value the value
	 * @return the vertices by indexed property
	 */
	public abstract QueryBuilder getVerticesByIndexedProperty(String key, Object value);
	
	/**
	 * Gets the vertices by property.
	 *
	 * @param key the key
	 * @param value the value
	 * @return the vertices by property
	 */
	public abstract QueryBuilder getVerticesByProperty(String key, Object value);
	
	/**
	 * filters by all the values for this property
	 * @param key
	 * @param values
	 * @return vertices that match these values
	 */
	public abstract QueryBuilder getVerticesByIndexedProperty(String key, List<?> values);

	/**
	 * filters by all the values for this property
	 * @param key
	 * @param values
	 * @return vertices that match these values
	 */
	public abstract QueryBuilder getVerticesByProperty(String key, List<?> values);

	/**
	 * Gets the child vertices from parent.
	 *
	 * @param parentKey the parent key
	 * @param parentValue the parent value
	 * @param childType the child type
	 * @return the child vertices from parent
	 */
	public abstract QueryBuilder getChildVerticesFromParent(String parentKey, String parentValue, String childType);
		
	/**
	 * Gets the typed vertices by map.
	 *
	 * @param type the type
	 * @param map the map
	 * @return the typed vertices by map
	 */
	public abstract QueryBuilder getTypedVerticesByMap(String type, LinkedHashMap<String, String> map);

	/**
	 * Creates the DB query.
	 *
	 * @param obj the obj
	 * @return the query builder
	 */
	public abstract QueryBuilder createDBQuery(Introspector obj);
	
	/**
	 * Creates the key query.
	 *
	 * @param obj the obj
	 * @return the query builder
	 */
	public abstract QueryBuilder createKeyQuery(Introspector obj);
	
	/**
	 * Creates the container query.
	 *
	 * @param obj the obj
	 * @return the query builder
	 */
	public abstract QueryBuilder createContainerQuery(Introspector obj);
	
	/**
	 * Creates the edge traversal.
	 *
	 * @param parent the parent
	 * @param child the child
	 * @return the query builder
	 */
	public abstract QueryBuilder createEdgeTraversal(EdgeType type, Introspector parent, Introspector child) throws AAIException;
	
	/**
	 * Creates the edge traversal.
	 *
	 * @param parent the parent
	 * @param child the child
	 * @return the query builder
	 */
	public abstract QueryBuilder createEdgeTraversal(EdgeType type, Vertex parent, Introspector child) throws AAIException;

	public QueryBuilder createEdgeTraversal(EdgeType type, String outNodeType, String inNodeType) throws NoEdgeRuleFoundException, AAIException {
		Introspector out = loader.introspectorFromName(outNodeType);
		Introspector in = loader.introspectorFromName(inNodeType);
		
		return createEdgeTraversal(type, out, in);
	}

	/**
	 * Creates the query from URI.
	 *
	 * @param uri the uri
	 * @return the query parser
	 * @throws UnsupportedEncodingException the unsupported encoding exception
	 * @throws AAIException the AAI exception
	 */
	public abstract QueryParser createQueryFromURI(URI uri) throws UnsupportedEncodingException, AAIException;
	
	/**
	 * Creates the query from URI.
	 *
	 * @param uri the uri
	 * @param queryParams the query params
	 * @return the query parser
	 * @throws UnsupportedEncodingException the unsupported encoding exception
	 * @throws AAIException the AAI exception
	 */
	public abstract QueryParser createQueryFromURI(URI uri, MultivaluedMap<String, String> queryParams) throws UnsupportedEncodingException, AAIException;

	/**
	 * Creates a queryparser from a given object name.
	 * 
	 * @param objName - name of the object type as it appears in the database
	 * @return
	 */
	public abstract QueryParser createQueryFromObjectName(String objName);
	
	/**
	 * Creates the query from relationship.
	 *
	 * @param relationship the relationship
	 * @return the query parser
	 * @throws UnsupportedEncodingException the unsupported encoding exception
	 * @throws AAIException the AAI exception
	 */
	public abstract QueryParser createQueryFromRelationship(Introspector relationship) throws UnsupportedEncodingException, AAIException;

	/**
	 * Gets the parent query.
	 *
	 * @return the parent query
	 */
	public abstract QueryBuilder getParentQuery();
	
	/**
	 * Gets the query.
	 *
	 * @return the query
	 */
	public abstract <T> T getQuery();
	
	/**
	 * Form boundary.
	 */
	public abstract void markParentBoundary();
	
	public abstract QueryBuilder limit(long amount);
	/**
	 * New instance.
	 *
	 * @param start the start
	 * @return the query builder
	 */
	public abstract QueryBuilder newInstance(Vertex start);
	
	/**
	 * New instance.
	 *
	 * @return the query builder
	 */
	public abstract QueryBuilder newInstance();
	
	/**
	 * Gets the start.
	 *
	 * @return the start
	 */
	public abstract Vertex getStart();

	protected Object correctObjectType(Object obj) {
		
		if (obj != null && obj.getClass().equals(Long.class)) {
			return new Integer(obj.toString());
		}
		
		return obj;
	}
	/**
	 * uses all fields in the introspector to create a query
	 * 
	 * @param obj
	 * @return
	 */
	public abstract QueryBuilder exactMatchQuery(Introspector obj);

	/**
	 * lets you join any number of QueryBuilders
	 * <b>be careful about starting with a union it will not use indexes</b>
	 * @param builder
	 * @return
	 */
	public abstract QueryBuilder union(QueryBuilder[] builder);
	
	public abstract QueryBuilder where(QueryBuilder[] builder);
	public abstract void markContainer();

	public abstract QueryBuilder getContainerQuery();

	public abstract List<Vertex> toList();
	
		
}