////////////////////////////////////////////////////////////////////////////////
// checkstyle: Checks Java source code for adherence to a set of rules.
// Copyright (C) 2001-2017 the original author or authors.
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
////////////////////////////////////////////////////////////////////////////////

package com.github.checkstyle.regression.data;

import java.util.List;

import org.immutables.gson.Gson;
import org.immutables.value.Value;

/**
 * Represents the extract information of a checkstyle module.
 * @author LuoLiangchen
 */
@Gson.TypeAdapters
@Value.Immutable
public abstract class ModuleExtractInfo {
    /**
     * The package name of this module.
     * @return the package name of this module
     */
    public abstract String packageName();

    /**
     * The name of this module.
     * @return the name of this module.
     */
    public abstract String name();

    /**
     * The parent of this module.
     * The value should be either "TreeWalker" or "Checker".
     * @return the parent of this module.
     */
    public abstract String parent();

    /**
     * The properties of this module.
     * @return the properties of this module
     */
    public abstract List<ModuleProperty> properties();

    /**
     * The full qualified name of this module.
     * @return the full qualified name of this module
     */
    public String fullName() {
        return packageName() + "." + name();
    }

    /** Represents a property of checkstyle module. */
    @Gson.TypeAdapters
    @Value.Immutable
    public interface ModuleProperty {
        /**
         * The name of this property.
         * @return the name of this property
         */
        String name();

        /**
         * The type of this property.
         * The value should be one of the followings:
         *     - Pattern
         *     - SeverityLevel
         *     - boolean
         *     - Scope
         *     - double[]
         *     - int[]
         *     - String[]
         *     - String
         *     - URI
         *     - AccessModifier[]
         *     - int
         * @return the type of this property
         */
        String type();
    }
}
