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

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import dev.roanh.gmark.util.graph.generic.IntGraph;

/**
 * Read-only graph input with stable node and edge identifiers.
 */
public interface PropertyGraph{
	Stream<Node> nodes();

	Node node(int id);

	Stream<Edge> outgoing(int node);

	Stream<Edge> incoming(int node);

	public sealed interface Element permits Node, Edge{
		Set<String> labels();

		Map<String, Object> properties();
	}

	public record Node(int id, Set<String> labels, Map<String, Object> properties) implements Element{
		public Node{
			labels = Set.copyOf(labels);
			properties = Map.copyOf(properties);
		}
	}

	public record Edge(String id, int source, int target, Set<String> labels, Map<String, Object> properties) implements Element{
		public Edge{
			Objects.requireNonNull(id, "id");
			labels = Set.copyOf(labels);
			properties = Map.copyOf(properties);
		}
	}

	/**
	 * Adapts a gMark integer graph. Vertices expose {@code id}, and edges expose
	 * {@code id}, {@code source}, {@code target}, and {@code label}. A numeric
	 * edge label N can be matched as LN or N. Duplicate triples represent one edge.
	 */
	static PropertyGraph fromIntGraph(IntGraph graph){
		Objects.requireNonNull(graph, "graph");
		return new PropertyGraph(){
			@Override
			public Stream<Node> nodes(){
				return IntStream.range(0, graph.getVertexCount()).mapToObj(this::node);
			}

			@Override
			public Node node(int id){
				return new Node(id, Set.of(), Map.of("id", id));
			}

			@Override
			public Stream<Edge> outgoing(int node){
				return graph.getOutgoingEdges(node).stream().distinct().map(edge->edge(node, edge.target(), edge.label()));
			}

			@Override
			public Stream<Edge> incoming(int node){
				return graph.getIncomingEdges(node).stream().distinct().map(edge->edge(edge.source(), node, edge.label()));
			}

			private Edge edge(int source, int target, int label){
				String id = source + ":" + target + ":" + label;
				return new Edge(id, source, target, Set.of("L" + label, Integer.toString(label)),
					Map.of("id", id, "source", source, "target", target, "label", label));
			}
		};
	}
}
