////////////////////////////////////////////////////////////////////////////////
// checkstyle: Checks Java source code for adherence to a set of rules.
// Copyright (C) 2001-2018 the original author or authors.
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

import org.immutables.value.Value;

/**
 * Represents the related information of a checkstyle module.
 * Including the extract module information, the module name and the properties.
 * @author LuoLiangchen
 */
@Value.Immutable
@Value.Modifiable
public abstract class ModuleInfo {
    /**
     * The extract information of the module.
     * @return the extract information of the module
     */
    public abstract ModuleExtractInfo moduleExtractInfo();

    /**
     * The properties of the module.
     * @return the properties of the module
     */
    public abstract List<Property> properties();

    /**
     * The name of the module.
     * @return the name of the module
     */
    public String name() {
        return moduleExtractInfo().name();
    }

    /**
     * Represents the property and its settings of a module.
     */
    @Value.Immutable
    public interface Property {
        /**
         * The name of the property.
         * @return the name of the property
         */
        String name();

        /**
         * The value of the property.
         * @return the value of the property
         */
        String value();
    }
}
