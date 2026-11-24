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

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import dev.roanh.gmark.lang.QueryLanguage;
import dev.roanh.gmark.lang.QueryLanguageSyntax;
import dev.roanh.gmark.lang.generic.GraphPattern;

/**
 * A read-only GQL query over a fixed-length graph pattern.
 */
public interface GQL extends QueryLanguageSyntax{

	GraphPattern pattern();

	List<Projection> projections();

	boolean distinct();

	@Override
	public default QueryLanguage getQueryLanguage(){
		return QueryLanguage.GQL;
	}

	/**
	 * A returned graph element or one of its properties.
	 */
	public record Projection(String variable, Optional<String> property, Optional<String> alias){
		public Projection{
			Objects.requireNonNull(variable, "variable");
			Objects.requireNonNull(property, "property");
			Objects.requireNonNull(alias, "alias");
		}

		public String columnName(){
			return alias.orElseGet(()->variable + property.map(name->"." + name).orElse(""));
		}
	}
}
