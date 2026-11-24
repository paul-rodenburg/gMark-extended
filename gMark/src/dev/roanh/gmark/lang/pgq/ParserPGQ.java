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
package dev.roanh.gmark.lang.pgq;

import java.util.List;

import dev.roanh.gmark.type.schema.Predicate;

/**
 * Parser skeleton for SQL/PGQ queries.
 */
public final class ParserPGQ{

	private ParserPGQ(){
	}

	public static PGQ parse(String query){
		return parse(query, List.of());
	}

	public static PGQ parse(String query, List<Predicate> labels){
		// TODO Parse the SQL/PGQ wrapper and MATCH pattern into a QueryPGQ.
		throw new UnsupportedOperationException("SQL/PGQ parsing is not implemented yet.");
	}
}
