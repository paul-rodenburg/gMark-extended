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
package dev.roanh.gmark.output;

import dev.roanh.gmark.lang.generic.GraphPattern;
import dev.roanh.gmark.query.Query;

/**
 * Lowers a generated query body to a shared property graph pattern.
 */
public final class GraphPatternLowering{

	private GraphPatternLowering(){
	}

	public static GraphPattern lower(Query query){
		// TODO Translate the conjuncts and preserve variables shared by multiple conjuncts.
		throw new UnsupportedOperationException("Workload lowering to graph patterns is not implemented yet.");
	}
}
