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
package dev.roanh.gmark.lang.generic;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A fixed-length MATCH path shared by SQL/PGQ and GQL queries. Steps are ordered
 * as written, starting at the first node in the pattern.
 * <p>
 * Repeated variable names refer to the same binding during evaluation.
 * Unnamed nodes and edges do not introduce a named binding.
 * @param start The first node in the path.
 * @param steps The successive edges and nodes. An empty list matches a single node.
 */
public record GraphPattern(Node start, List<Step> steps){

	public GraphPattern{
		Objects.requireNonNull(start, "start");
		steps = List.copyOf(steps);
	}

	/**
	 * A node pattern with an optional binding and label constraint.
	 * @param variable The variable to bind, if named.
	 * @param label The required label, if specified.
	 */
	public record Node(Optional<String> variable, Optional<String> label){
		public Node{
			Objects.requireNonNull(variable, "variable");
			Objects.requireNonNull(label, "label");
		}
	}

	/**
	 * An edge pattern with an optional binding and label constraint.
	 * @param variable The variable to bind, if named.
	 * @param label The required label, if specified.
	 * @param direction The direction relative to the order of nodes in the pattern.
	 */
	public record Edge(Optional<String> variable, Optional<String> label, Direction direction){
		public Edge{
			Objects.requireNonNull(variable, "variable");
			Objects.requireNonNull(label, "label");
			Objects.requireNonNull(direction, "direction");
		}
	}

	/**
	 * An edge connecting the current node and the next node in the pattern.
	 * @param edge The constraint on the connecting edge.
	 * @param next The next node in pattern order, irrespective of edge direction.
	 */
	public record Step(Edge edge, Node next){
		public Step{
			Objects.requireNonNull(edge, "edge");
			Objects.requireNonNull(next, "next");
		}
	}

	/**
	 * Direction of the edge relative to the path as written.
	 */
	public enum Direction{
		/** The edge points toward the next node. */
		OUTGOING,
		/** The edge points toward the current node. */
		INCOMING,
		/** The edge can point in either direction. */
		UNDIRECTED
	}
}
