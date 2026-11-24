/*
 * gMark: A domain- and query language-independent query workload generator and query language utility library.
 * Copyright (C) 2026 Paul Rodenburg.
 * GitHub Repository: https://github.com/paul-rodenburg/gMark-extended
 *
 * gMark is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * gMark is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package dev.roanh.gmark.lang.gql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.roanh.gmark.eval.PropertyGraph;
import dev.roanh.gmark.eval.PropertyGraphEvaluator;
import dev.roanh.gmark.eval.PropertyGraphQueryResult;
import dev.roanh.gmark.lang.generic.GraphPattern.Direction;
import dev.roanh.gmark.util.graph.generic.IntGraph;

public class GQLTest{

	@Test
	public void preservesDirectionsAndQuotedIdentifiers(){
		var query = ParserGQL.parse("""
			match ("first node")<-[:0]-(b)-(c)->(d) return distinct "first node".id AS "node id", d.id;
			""");
		assertEquals(List.of(Direction.INCOMING, Direction.UNDIRECTED, Direction.OUTGOING),
			query.pattern().steps().stream().map(step->step.edge().direction()).toList());
		assertEquals("node id", query.projections().getFirst().columnName());
		assertEquals(query, ParserGQL.parse(query.toFormalSyntax()));
	}

	@Test
	public void rejectsInvalidPatternsAndProjections(){
		assertThrows(IllegalArgumentException.class, ()->ParserGQL.parse("MATCH (a)-[]->* (b) RETURN a"));
		assertThrows(IllegalArgumentException.class, ()->ParserGQL.parse("MATCH (a) RETURN missing.id"));
		assertThrows(IllegalArgumentException.class, ()->ParserGQL.parse("MATCH (a)-[a]->(b) RETURN a"));
		assertThrows(IllegalArgumentException.class, ()->ParserGQL.parse("MATCH (a) RETURN a; MATCH (b) RETURN b"));
	}

	@Test
	public void evaluatesLabelsDirectionsAndProjection(){
		var outgoing = evaluate("MATCH (a)-[e:L0]->(b) RETURN a.id AS source, b.id AS target, e.label");
		assertEquals(List.of("source", "target", "e.label"), outgoing.getColumns());
		assertEquals(List.of(List.of(0, 1, 0), List.of(0, 2, 0), List.of(2, 2, 0)), outgoing.getRows());
		assertEquals(List.of(List.of(1, 0), List.of(2, 0), List.of(2, 2)),
			evaluate("MATCH (a)<-[IS L0]-(b) RETURN a.id, b.id").getRows());
	}

	@Test
	public void bindsRepeatedVariablesAndDoesNotDoubleCountSelfLoops(){
		assertEquals(List.of(List.of(2)), evaluate("MATCH (a)-[:L0]-(a) RETURN a.id").getRows());
		assertEquals(List.of(List.of(0, 1), List.of(0, 2), List.of(2, 2)),
			evaluate("MATCH (a)-[e:L0]->(b)<-[e:L0]-(a) RETURN a.id, b.id").getRows());
	}

	@Test
	public void appliesDistinctAfterProjectionAndIncludesIsolatedNodes(){
		assertEquals(List.of(List.of(0), List.of(2)),
			evaluate("MATCH (a)-[:L0]->() RETURN DISTINCT a.id").getRows());
		assertEquals(List.of(List.of(0), List.of(1), List.of(2), List.of(3)),
			evaluate("MATCH (a) RETURN a.id").getRows());
		assertEquals(List.of(), evaluate("MATCH (a:Person) RETURN a.id").getRows());
		assertNull(evaluate("MATCH (a) RETURN a.missing").getRows().getFirst().getFirst());
	}

	private PropertyGraphQueryResult evaluate(String query){
		IntGraph graph = new IntGraph(4, 2);
		graph.addEdge(0, 1, 0);
		graph.addEdge(0, 2, 0);
		graph.addEdge(0, 1, 0);
		graph.addEdge(1, 0, 1);
		graph.addEdge(2, 2, 0);
		return PropertyGraphEvaluator.evaluate(PropertyGraph.fromIntGraph(graph), ParserGQL.parse(query));
	}
}
