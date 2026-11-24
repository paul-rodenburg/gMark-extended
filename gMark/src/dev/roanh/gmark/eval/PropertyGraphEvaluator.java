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
package dev.roanh.gmark.eval;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import dev.roanh.gmark.lang.gql.GQL;
import dev.roanh.gmark.lang.gql.GQL.Projection;
import dev.roanh.gmark.lang.pgq.PGQ;

/**
 * Evaluation entry points for SQL/PGQ and GQL.
 */
public final class PropertyGraphEvaluator{

	private PropertyGraphEvaluator(){
	}

	public static PropertyGraphQueryResult evaluate(PropertyGraph graph, PGQ query){
		// TODO Evaluate the graph pattern and apply the SQL/PGQ projection.
		throw new UnsupportedOperationException("SQL/PGQ evaluation is not implemented yet.");
	}

	public static PropertyGraphQueryResult evaluate(PropertyGraph graph, GQL query){
		return new Match(graph, query).evaluate();
	}

	private static final class Match{
		private final PropertyGraph graph;
		private final GQL query;
		private final Map<String, PropertyGraph.Element> bindings = new HashMap<>();
		private final List<List<Object>> rows = new ArrayList<>();

		private Match(PropertyGraph graph, GQL query){
			this.graph = graph;
			this.query = query;
		}

		private PropertyGraphQueryResult evaluate(){
			var start = query.pattern().start();
			try(var nodes = graph.nodes()){
				nodes.sequential().filter(node->start.label().map(node.labels()::contains).orElse(true))
					.forEach(node->withBinding(start.variable(), node, ()->matchSteps(node.id(), 0)));
			}
			List<String> columns = query.projections().stream().map(Projection::columnName).toList();
			List<List<Object>> result = query.distinct() ? rows.stream().distinct().toList() : List.copyOf(rows);
			return new PropertyGraphQueryResult(){
				@Override
				public List<String> getColumns(){
					return columns;
				}

				@Override
				public List<List<Object>> getRows(){
					return result;
				}
			};
		}

		private void matchSteps(int current, int index){
			if(index == query.pattern().steps().size()){
				rows.add(query.projections().stream().map(this::project).toList());
				return;
			}

			var step = query.pattern().steps().get(index);
			try(var edges = switch(step.edge().direction()){
				case OUTGOING -> graph.outgoing(current);
				case INCOMING -> graph.incoming(current);
				case UNDIRECTED -> Stream.concat(graph.outgoing(current), graph.incoming(current)).distinct();
			}){
				edges.sequential().filter(edge->step.edge().label().map(edge.labels()::contains).orElse(true)).forEach(edge->{
					var next = graph.node(edge.source() == current ? edge.target() : edge.source());
					if(step.next().label().map(next.labels()::contains).orElse(true)){
						withBinding(step.edge().variable(), edge, ()->
							withBinding(step.next().variable(), next, ()->matchSteps(next.id(), index + 1)));
					}
				});
			}
		}

		private Object project(Projection projection){
			PropertyGraph.Element element = bindings.get(projection.variable());
			if(element == null){
				throw new IllegalArgumentException("Unknown RETURN variable: " + projection.variable());
			}
			if(projection.property().isEmpty()){
				return element;
			}
			return element.properties().get(projection.property().orElseThrow());
		}

		private void withBinding(Optional<String> variable, PropertyGraph.Element element, Runnable match){
			if(variable.isEmpty()){
				match.run();
				return;
			}
			String name = variable.orElseThrow();
			PropertyGraph.Element previous = bindings.putIfAbsent(name, element);
			if(previous == null){
				try{
					match.run();
				}finally{
					bindings.remove(name);
				}
			}else if(sameElement(previous, element)){
				match.run();
			}
		}

		private boolean sameElement(PropertyGraph.Element first, PropertyGraph.Element second){
			return switch(first){
				case PropertyGraph.Node node -> second instanceof PropertyGraph.Node other && node.id() == other.id();
				case PropertyGraph.Edge edge -> second instanceof PropertyGraph.Edge other && edge.id().equals(other.id());
			};
		}
	}
}
