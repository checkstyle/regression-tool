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

package com.github.checkstyle.regression.git;

/**
 * Represents git changes of a file.
 * @author LuoLiangchen
 */
public class GitChange {
    /** The path of the changed file. */
    private final String path;

    /**
     * Creates a new {@link GitChange} instance.
     * @param path the path of the changed file
     */
    GitChange(String path) {
        this.path = path;
    }

    /**
     * Gets the path of the changed file.
     * @return the path of the changed file
     */
    public String getPath() {
        return path;
    }
}
