/*
 * gMark: A domain- and query language-independent query workload generator and query language utility library.
 * Copyright (C) 2021  Roan Hofland (roan@roanh.dev).  All rights reserved.
 * Copyright (C) 2026 Paul Rodenburg.
 * GitHub Repository: https://github.com/paul-rodenburg/gMark-extended
 * Upstream Repository: https://github.com/RoanH/gMark
 *
 * gMark is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * gMark is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dev.roanh.gmark.lang.gql;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import dev.roanh.gmark.lang.generic.GraphPattern;
import dev.roanh.gmark.lang.generic.GraphPattern.Direction;
import dev.roanh.gmark.lang.generic.GraphPattern.Edge;
import dev.roanh.gmark.lang.generic.GraphPattern.Node;
import dev.roanh.gmark.lang.generic.GraphPattern.Step;
import dev.roanh.gmark.lang.gql.GQL.Projection;
import dev.roanh.gmark.type.schema.Predicate;

/**
 * Parser for a single fixed-length MATCH path followed by RETURN.
 */
public final class ParserGQL{

	private static final Pattern REGULAR_IDENTIFIER = Pattern.compile("[\\p{L}_][\\p{L}\\p{N}_]*");
	private static final Pattern QUOTED_IDENTIFIER = Pattern.compile("\"(?:[^\"]|\"\")+\"");
	private static final Pattern NUMERIC_LABEL = Pattern.compile("[0-9]+");

	private final String query;
	private int position;

	private ParserGQL(String query){
		this.query = query;
	}

	public static QueryGQL parse(String query){
		if(query == null){
			throw new IllegalArgumentException("No GQL query provided.");
		}
		return new ParserGQL(query).parseQuery();
	}

	/**
	 * Keeps label names in the pattern for resolution by the property graph.
	 */
	public static QueryGQL parse(String query, List<Predicate> labels){
		return parse(query);
	}

	private QueryGQL parseQuery(){
		requireKeyword("MATCH");
		Node start = parseNode();
		List<Step> steps = new ArrayList<>();
		while(peek("-") || peek("<-")){
			steps.add(new Step(parseEdge(), parseNode()));
		}

		requireKeyword("RETURN");
		boolean distinct = takeKeyword("DISTINCT");
		List<Projection> projections = new ArrayList<>();
		do{
			String variable = identifier();
			Optional<String> property = take(".") ? Optional.of(identifier()) : Optional.empty();
			Optional<String> alias = takeKeyword("AS") ? Optional.of(identifier()) : Optional.empty();
			projections.add(new Projection(variable, property, alias));
		}while(take(","));

		take(";");
		skipWhitespace();
		if(position != query.length()){
			throw expected("end of query");
		}
		return new QueryGQL(new GraphPattern(start, steps), projections, distinct);
	}

	private Node parseNode(){
		require("(");
		Element element = parseElement();
		require(")");
		return new Node(element.variable(), element.label());
	}

	private Edge parseEdge(){
		if(take("->")){
			return new Edge(Optional.empty(), Optional.empty(), Direction.OUTGOING);
		}
		boolean incoming = take("<-");
		if(!incoming){
			require("-");
		}
		Direction direction = incoming ? Direction.INCOMING : Direction.UNDIRECTED;
		if(!take("[")){
			return new Edge(Optional.empty(), Optional.empty(), direction);
		}

		Element element = parseElement();
		require("]");
		if(!incoming && take("->")){
			return new Edge(element.variable(), element.label(), Direction.OUTGOING);
		}
		require("-");
		return new Edge(element.variable(), element.label(), direction);
	}

	private Element parseElement(){
		boolean unnamedWithLabel = takeKeyword("IS");
		Optional<String> variable = unnamedWithLabel ? Optional.empty() : readIdentifier();
		Optional<String> label = Optional.empty();
		if(unnamedWithLabel || take(":") || takeKeyword("IS")){
			label = Optional.of(token(NUMERIC_LABEL).or(()->readIdentifier()).orElseThrow(()->expected("a label")));
		}
		return new Element(variable, label);
	}

	private record Element(Optional<String> variable, Optional<String> label){
	}

	private String identifier(){
		return readIdentifier().orElseThrow(()->expected("an identifier"));
	}

	private Optional<String> readIdentifier(){
		return token(QUOTED_IDENTIFIER)
			.map(name->name.substring(1, name.length() - 1).replace("\"\"", "\""))
			.or(()->token(REGULAR_IDENTIFIER));
	}

	private Optional<String> token(Pattern pattern){
		skipWhitespace();
		var matcher = pattern.matcher(query).region(position, query.length());
		if(!matcher.lookingAt()){
			return Optional.empty();
		}
		position = matcher.end();
		return Optional.of(matcher.group());
	}

	static String formatIdentifier(String name){
		if(REGULAR_IDENTIFIER.matcher(name).matches() && !name.equalsIgnoreCase("DISTINCT") && !name.equalsIgnoreCase("IS")){
			return name;
		}
		return "\"" + name.replace("\"", "\"\"") + "\"";
	}

	private boolean takeKeyword(String keyword){
		skipWhitespace();
		int end = position + keyword.length();
		if(query.regionMatches(true, position, keyword, 0, keyword.length()) &&
			(end == query.length() || !Character.isUnicodeIdentifierPart(query.codePointAt(end)))){
			position = end;
			return true;
		}
		return false;
	}

	private void requireKeyword(String keyword){
		if(!takeKeyword(keyword)){
			throw expected(keyword);
		}
	}

	private void require(String symbol){
		if(!take(symbol)){
			throw expected("'" + symbol + "'");
		}
	}

	private boolean take(String symbol){
		if(!peek(symbol)){
			return false;
		}
		position += symbol.length();
		return true;
	}

	private boolean peek(String symbol){
		skipWhitespace();
		return query.startsWith(symbol, position);
	}

	private void skipWhitespace(){
		while(position < query.length() && Character.isWhitespace(query.charAt(position))){
			position++;
		}
	}

	private IllegalArgumentException expected(String token){
		return new IllegalArgumentException("Expected " + token + " at character " + (position + 1) + ".");
	}
}
