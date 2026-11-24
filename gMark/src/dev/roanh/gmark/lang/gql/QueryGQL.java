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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import dev.roanh.gmark.ast.OperationType;
import dev.roanh.gmark.ast.QueryTree;
import dev.roanh.gmark.lang.generic.GraphPattern;
import dev.roanh.gmark.lang.generic.GraphPattern.Direction;
import dev.roanh.gmark.lang.generic.GraphPattern.Node;
import dev.roanh.gmark.util.IndentWriter;

/**
 * A GQL MATCH path and its RETURN clause.
 */
public record QueryGQL(GraphPattern pattern, List<Projection> projections, boolean distinct) implements GQL{

	public QueryGQL{
		Objects.requireNonNull(pattern, "pattern");
		projections = List.copyOf(projections);
		if(projections.isEmpty()){
			throw new IllegalArgumentException("RETURN requires at least one expression.");
		}

		Map<String, Boolean> variables = new HashMap<>();
		register(variables, pattern.start().variable(), true);
		for(var step : pattern.steps()){
			register(variables, step.edge().variable(), false);
			register(variables, step.next().variable(), true);
		}
		for(Projection projection : projections){
			if(!variables.containsKey(projection.variable())){
				throw new IllegalArgumentException("Unknown RETURN variable: " + projection.variable());
			}
		}
	}

	private static void register(Map<String, Boolean> variables, Optional<String> name, boolean node){
		name.ifPresent(variable->{
			Boolean previous = variables.putIfAbsent(variable, node);
			if(previous != null && previous != node){
				throw new IllegalArgumentException("Variable is used as both a node and an edge: " + variable);
			}
		});
	}

	@Override
	public String toFormalSyntax(){
		StringBuilder query = new StringBuilder("MATCH ").append(formatNode(pattern.start()));
		for(var step : pattern.steps()){
			var edge = step.edge();
			query.append(edge.direction() == Direction.INCOMING ? "<-[" : "-[");
			query.append(formatElement(edge.variable(), edge.label()));
			query.append(edge.direction() == Direction.OUTGOING ? "]->" : "]-");
			query.append(formatNode(step.next()));
		}

		query.append(distinct ? " RETURN DISTINCT " : " RETURN ");
		query.append(projections.stream().map(projection->ParserGQL.formatIdentifier(projection.variable()) +
			projection.property().map(name->"." + ParserGQL.formatIdentifier(name)).orElse("") +
			projection.alias().map(name->" AS " + ParserGQL.formatIdentifier(name)).orElse(""))
			.collect(Collectors.joining(", ")));
		return query.toString();
	}

	private static String formatNode(Node node){
		return "(" + formatElement(node.variable(), node.label()) + ")";
	}

	private static String formatElement(Optional<String> variable, Optional<String> label){
		return variable.map(ParserGQL::formatIdentifier).orElse("") +
			label.map(name->":" + ParserGQL.formatIdentifier(name)).orElse("");
	}

	@Override
	public String toString(){
		return toFormalSyntax();
	}

	@Override
	public OperationType getOperationType(){
		throw new UnsupportedOperationException("GQL is evaluated directly as a property graph pattern.");
	}

	@Override
	public QueryTree toAbstractSyntaxTree(){
		throw new UnsupportedOperationException("GQL is evaluated directly as a property graph pattern.");
	}

	@Override
	public void writeSQL(IndentWriter writer){
		throw new UnsupportedOperationException("GQL to SQL conversion is not implemented.");
	}

	@Override
	public void writeXML(IndentWriter writer){
		throw new UnsupportedOperationException("GQL XML export is not implemented.");
	}
}
