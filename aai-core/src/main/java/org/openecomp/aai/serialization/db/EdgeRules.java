/*-
 * ============LICENSE_START=======================================================
 * org.openecomp.aai
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights reserved.
 * ================================================================================
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
 * ============LICENSE_END=========================================================
 */

package org.openecomp.aai.serialization.db;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import org.openecomp.aai.db.props.AAIProperties;
import org.openecomp.aai.dbmodel.DbEdgeRules;
import org.openecomp.aai.exceptions.AAIException;
import org.openecomp.aai.introspection.Version;
import org.openecomp.aai.serialization.db.exceptions.EdgeMultiplicityException;
import org.openecomp.aai.serialization.db.exceptions.NoEdgeRuleFoundException;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.jayway.jsonpath.JsonPath;

import static com.jayway.jsonpath.Filter.filter;
import static com.jayway.jsonpath.Criteria.where;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.Filter;

import java.util.Scanner;

public class EdgeRules {
	
	private EELFLogger logger = EELFManager.getInstance().getLogger(EdgeRules.class);
	
	private Multimap<String, String> deleteScope = 	DbEdgeRules.DefaultDeleteScope;
	
	private DocumentContext rulesDoc;
	
	/**
	 * Loads the most recent DbEdgeRules json file for later parsing.
	 * Only need most recent version for actual A&AI operations that call this class; 
	 *   the old ones are only used in tests.
	 */
	private EdgeRules() {

		InputStream is = getClass().getResourceAsStream("/dbedgerules/DbEdgeRules_" + Version.getLatest().toString() + ".json");

		Scanner scanner = new Scanner(is);
		String json = scanner.useDelimiter("\\Z").next();
		scanner.close();
		rulesDoc = JsonPath.parse(json);
	}
	
	private static class Helper {
		private static final EdgeRules INSTANCE = new EdgeRules();
	}
	
	/**
	 * Gets the single instance of EdgeRules.
	 *
	 * @return single instance of EdgeRules
	 */
	public static EdgeRules getInstance() {
		return Helper.INSTANCE;

	}
	
	/**
	 * Adds the tree edge.
	 *
	 * @param aVertex the out vertex
	 * @param bVertex the in vertex
	 * @return the edge
	 * @throws AAIException the AAI exception
	 */
	public Edge addTreeEdge(GraphTraversalSource traversalSource, Vertex aVertex, Vertex bVertex) throws AAIException {
		return this.addEdge(EdgeType.TREE, traversalSource, aVertex, bVertex, false);
	}
	
	/**
	 * Adds the edge.
	 *
	 * @param aVertex the out vertex
	 * @param bVertex the in vertex
	 * @return the edge
	 * @throws AAIException the AAI exception
	 */
	public Edge addEdge(GraphTraversalSource traversalSource, Vertex aVertex, Vertex bVertex) throws AAIException {
		return this.addEdge(EdgeType.COUSIN, traversalSource, aVertex, bVertex, false);
	}
	
	/**
	 * Adds the tree edge.
	 *
	 * @param aVertex the out vertex
	 * @param bVertex the in vertex
	 * @return the edge
	 * @throws AAIException the AAI exception
	 */
	public Edge addTreeEdgeIfPossible(GraphTraversalSource traversalSource, Vertex aVertex, Vertex bVertex) throws AAIException {
		return this.addEdge(EdgeType.TREE, traversalSource, aVertex, bVertex, true);
	}
	
	/**
	 * Adds the edge.
	 *
	 * @param aVertex the out vertex
	 * @param bVertex the in vertex
	 * @return the edge
	 * @throws AAIException the AAI exception
	 */
	public Edge addEdgeIfPossible(GraphTraversalSource traversalSource, Vertex aVertex, Vertex bVertex) throws AAIException {
		return this.addEdge(EdgeType.COUSIN, traversalSource, aVertex, bVertex, true);
	}
	
	/**
	 * Adds the edge.
	 *
	 * @param type the type
	 * @param aVertex the out vertex
	 * @param bVertex the in vertex
	 * @return the edge
	 * @throws AAIException the AAI exception
	 */
	private Edge addEdge(EdgeType type, GraphTraversalSource traversalSource, Vertex aVertex, Vertex bVertex, boolean isBestEffort) throws AAIException {

		EdgeRule rule = this.getEdgeRule(type, aVertex, bVertex);

		Edge e = null;
		
		Optional<String> message = this.validateMultiplicity(rule, traversalSource, aVertex, bVertex);
		
		if (message.isPresent() && !isBestEffort) {
			throw new EdgeMultiplicityException(message.get());
		}
		if (!message.isPresent()) {
			if (rule.getDirection().equals(Direction.OUT)) {
				e = aVertex.addEdge(rule.getLabel(), bVertex);
			} else if (rule.getDirection().equals(Direction.IN)) {
				e = bVertex.addEdge(rule.getLabel(), aVertex);
			}
			
			this.addProperties(e, rule);
		}
		return e;
	}

	/**
	 * Adds the properties.
	 *
	 * @param edge the edge
	 * @param rule the rule
	 */
	public void addProperties(Edge edge, EdgeRule rule) {
		
		// In DbEdgeRules.EdgeRules -- What we have as "edgeRule" is a comma-delimited set of strings.
		// The first item is the edgeLabel.
		// The second in the list is always "direction" which is always OUT for the way we've implemented it.
		// Items starting at "firstTagIndex" and up are all assumed to be booleans that map according to 
		// tags as defined in EdgeInfoMap.
		// Note - if they are tagged as 'reverse', that means they get the tag name with "-REV" on it
		Map<String, String> propMap = rule.getEdgeProperties();
		
		for (String key : propMap.keySet()) {
			String revKeyname = key + "-REV";
			String triple = propMap.get(key);
			if(triple.equals("true")){
				edge.property(key, true);
				edge.property(revKeyname,false);
			} else if (triple.equals("false")) {
				edge.property(key, false);
				edge.property(revKeyname,false);
			} else if (triple.equals("reverse")) {
				edge.property(key, false);
				edge.property(revKeyname,true);
			}
		}
	}
	
	/**
	 * Checks if any edge rules exist between the two given nodes, in either A|B or B|A order.
	 *
	 * @param nodeA - node at one end of the edge
	 * @param nodeB - node at the other end
	 * @return true, if any such rules exist
	 */
	public boolean hasEdgeRule(String nodeA, String nodeB) {
		Filter aToB = filter(
				where("from").is(nodeA).and("to").is(nodeB)
				);
		Filter bToA = filter(
				where("from").is(nodeB).and("to").is(nodeA)
				);
		
		List<Object> results = rulesDoc.read("$.rules.[?]", aToB);
		results.addAll(rulesDoc.read("$.rules.[?]", bToA));

		return !results.isEmpty();
		
	}
	
	/**
	 * Checks if any edge rules exist between the two given nodes, in either A|B or B|A order.
	 *
	 * @param aVertex - node at one end of the edge
	 * @param bVertex - node at the other end
	 * @return true, if any such rules exist
	 */
	public boolean hasEdgeRule(Vertex aVertex, Vertex bVertex) {
		String outType = aVertex.<String>property("aai-node-type").orElse(null);
		String inType = bVertex.<String>property("aai-node-type").orElse(null);
		
		return this.hasEdgeRule(outType, inType);
		
	}
	
	/**
	 * Gets all the edge rules that exist between the given node types.
	 * The rules will be phrased in terms of out|in, though this will
	 * also find rules defined as in|out (it will flip the direction in
	 * the EdgeRule object returned accordingly to match out|in).
	 * 
	 * @param outType 
	 * @param inType
	 * @return Map<String edgeLabel, EdgeRule rule> where edgeLabel is the label name
	 * @throws AAIException
	 */
	public Map<String, EdgeRule> getEdgeRules(String outType, String inType) throws AAIException {
		Map<String, EdgeRule> result = new HashMap<>();
		EdgeRule rule = null;
		for (EdgeType type : EdgeType.values()) {
			try {
				rule = this.getEdgeRule(type, outType, inType);
				result.put(rule.getLabel(), rule);
			} catch (NoEdgeRuleFoundException e) {
				continue;
			}
		}
		
		return result;
	}
	
	/**
	 * Gets the edge rule of the given type that exists between A and B.
	 * Will check B|A as well, and flips the direction accordingly if that succeeds
	 * to match the expected A|B return.
	 *
	 * @param type - the type of edge you're looking for
	 * @param nodeA - first node type
	 * @param nodeB - second node type
	 * @return EdgeRule describing the rule in terms of A|B, if there is any such rule
	 * @throws AAIException if no such edge exists
	 */
	public EdgeRule getEdgeRule(EdgeType type, String nodeA, String nodeB) throws AAIException {
		//try A to B
		List<Map<String, String>> aToBEdges = rulesDoc.read("$.rules.[?]", buildFilter(type, nodeA, nodeB));
		if (!aToBEdges.isEmpty()) {
			//lazily stop iterating if we find a match
			//should there be a mismatch between type and isParent,
			//the caller will receive something.
			//this operates on the assumption that there are at most two rules
			//for a given vertex pair
			return buildRule(aToBEdges.get(0));
		}
		
		//we get here if there was nothing for A to B, so let's try B to A
		List<Map<String, String>> bToAEdges = rulesDoc.read("$.rules.[?]", buildFilter(type, nodeB, nodeA));
		if (!bToAEdges.isEmpty()) {
			return flipDirection(buildRule(bToAEdges.get(0))); //bc we need to return as A|B, so flip the direction to match
		}
		
		//found none
		throw new NoEdgeRuleFoundException("no " + type.toString() + " edge between " + nodeA + " and " + nodeB);
	}
	
	/**
	 * Builds a JsonPath filter to search for an edge from nodeA to nodeB with the given edge type (cousin or parent/child)
	 * 
	 * @param type
	 * @param nodeA - start node
	 * @param nodeB - end node
	 * @return
	 */
	private Filter buildFilter(EdgeType type, String nodeA, String nodeB) {
		if (EdgeType.COUSIN.equals(type)) {
			return filter(
					where("from").is(nodeA).and("to").is(nodeB).and("isParent").is("false")
					);
		} else {
			return filter(
					where("from").is(nodeA).and("to").is(nodeB).and("isParent").is("true")).or(
							where("from").is(nodeA).and("to").is(nodeB).and("isParent").is("reverse")	
					);
		}
	}
	
	/**
	 * Puts the give edge rule information into an EdgeRule object. 
	 * 
	 * @param edge - the edge information returned from JsonPath
	 * @return EdgeRule containing that information
	 */
	private EdgeRule buildRule(Map<String, String> edge) {
		EdgeRule rule = new EdgeRule();
		rule.setLabel(edge.get("label"));
		rule.setDirection(edge.get("direction"));
		rule.setMultiplicityRule(edge.get("multiplicity"));
		rule.setIsParent(edge.get("isParent"));
		rule.setUsesResource(edge.get("usesResource"));
		rule.setHasDelTarget(edge.get("hasDelTarget"));
		rule.setServiceInfrastructure(edge.get("SVC-INFRA"));
		return rule;
	}
	
	/**
	 * If getEdgeRule gets a request for A|B, and it finds something as B|A, the caller still expects
	 * the returned EdgeRule to reflect A|B directionality. This helper method flips B|A direction to
	 * match this expectation.
	 * 
	 * @param rule whose direction needs flipped
	 * @return the updated rule
	 */
	private EdgeRule flipDirection(EdgeRule rule) {
		if (Direction.IN.equals(rule.getDirection())) {
			rule.setDirection(Direction.OUT);
			return rule;
		} else if (Direction.OUT.equals(rule.getDirection())) {
			rule.setDirection(Direction.IN);
			return rule;
		} else { //direction is BOTH, flipping both is still both
			return rule; 
		}
	}
	
	/**
	 * Gets the edge rule of the given type that exists between A and B.
	 * Will check B|A as well, and flips the direction accordingly if that succeeds
	 * to match the expected A|B return.
	 *
	 * @param type - the type of edge you're looking for
	 * @param aVertex - first node type
	 * @param bVertex - second node type
	 * @return EdgeRule describing the rule in terms of A|B, if there is any such rule
	 * @throws AAIException if no such edge exists
	 */
	public EdgeRule getEdgeRule(EdgeType type, Vertex aVertex, Vertex bVertex) throws AAIException {
		String outType = aVertex.<String>property(AAIProperties.NODE_TYPE).orElse(null);
		String inType = bVertex.<String>property(AAIProperties.NODE_TYPE).orElse(null);
		
		return this.getEdgeRule(type, outType, inType);

		
	}
	
	/**
	 * Gets the delete semantic.
	 *
	 * @param nodeType the node type
	 * @return the delete semantic
	 */
	public DeleteSemantic getDeleteSemantic(String nodeType) {
		Collection<String> semanticCollection = deleteScope.get(nodeType);
		String semantic = semanticCollection.iterator().next();
		
		return DeleteSemantic.valueOf(semantic);
		
	}
	
	/**
	 * Validate multiplicity.
	 *
	 * @param rule the rule
	 * @param aVertex the out vertex
	 * @param bVertex the in vertex
	 * @return true, if successful
	 * @throws AAIException the AAI exception
	 */
	private Optional<String> validateMultiplicity(EdgeRule rule, GraphTraversalSource traversalSource, Vertex aVertex, Vertex bVertex) {

		if (rule.getDirection().equals(Direction.OUT)) {
			
		} else if (rule.getDirection().equals(Direction.IN)) {
			Vertex tempV = bVertex;
			bVertex = aVertex;
			aVertex = tempV;
		}
				
		String aVertexType = aVertex.<String>property(AAIProperties.NODE_TYPE).orElse(null);
		String bVertexType =  bVertex.<String>property(AAIProperties.NODE_TYPE).orElse(null);
		String label = rule.getLabel();
		MultiplicityRule multiplicityRule = rule.getMultiplicityRule();
		List<Edge> outEdges = traversalSource.V(aVertex).outE(label).where(__.inV().has(AAIProperties.NODE_TYPE, bVertexType)).toList();
		List<Edge> inEdges = traversalSource.V(bVertex).inE(label).where(__.outV().has(AAIProperties.NODE_TYPE, aVertexType)).toList();
		String detail = "";
		if (multiplicityRule.equals(MultiplicityRule.ONE2ONE)) {
			if (inEdges.size() >= 1 || outEdges.size() >= 1 ) {
				detail = "multiplicity rule violated: only one edge can exist with label: " + label + " between " + aVertexType + " and " + bVertexType;
			}
		} else if (multiplicityRule.equals(MultiplicityRule.ONE2MANY)) {
			if (inEdges.size() >= 1) {
				detail = "multiplicity rule violated: only one edge can exist with label: " + label + " between " + aVertexType + " and " + bVertexType;
			}
		} else if (multiplicityRule.equals(MultiplicityRule.MANY2ONE)) {
			if (outEdges.size() >= 1) {
				detail = "multiplicity rule violated: only one edge can exist with label: " + label + " between " + aVertexType + " and " + bVertexType;
			}
		} else {
			
		}
		
		if (!"".equals(detail)) {
			return Optional.of(detail);
		} else  {
			return Optional.empty();
		}
		
				
	}
	
	/**
	 * Gets all the edge rules we define.
	 * 
	 * @return Multimap<String "from|to", EdgeRule rule>
	 */
	public Multimap<String, EdgeRule> getAllRules() {
		Multimap<String, EdgeRule> result = ArrayListMultimap.create();
		
		List<Map<String, String>> rules = rulesDoc.read("$.rules.*");
		for (Map<String, String> rule : rules) {
			EdgeRule er = buildRule(rule);
			String name = rule.get("from") + "|" + rule.get("to");
			result.put(name, er);
		}
		
		return result;
	}
	
}
