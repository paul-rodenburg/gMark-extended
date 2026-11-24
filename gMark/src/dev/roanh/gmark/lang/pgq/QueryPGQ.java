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
package dev.roanh.gmark.lang.pgq;

import dev.roanh.gmark.ast.OperationType;
import dev.roanh.gmark.ast.QueryTree;
import dev.roanh.gmark.lang.generic.GraphPattern;
import dev.roanh.gmark.util.IndentWriter;

/**
 * Starter representation of a parsed SQL/PGQ query.
 */
public record QueryPGQ(GraphPattern pattern) implements PGQ{

	// TODO Model the graph reference and the GRAPH_TABLE and COLUMNS clauses.

	@Override
	public OperationType getOperationType(){
		throw new UnsupportedOperationException("SQL/PGQ query operations are not implemented yet.");
	}

	@Override
	public QueryTree toAbstractSyntaxTree(){
		throw new UnsupportedOperationException("SQL/PGQ AST conversion is not implemented yet.");
	}

	@Override
	public String toFormalSyntax(){
		throw new UnsupportedOperationException("SQL/PGQ formal output is not implemented yet.");
	}

	@Override
	public void writeSQL(IndentWriter writer){
		throw new UnsupportedOperationException("SQL/PGQ SQL output is not implemented yet.");
	}

	@Override
	public void writeXML(IndentWriter writer){
		throw new UnsupportedOperationException("SQL/PGQ XML output is not implemented yet.");
	}
}
