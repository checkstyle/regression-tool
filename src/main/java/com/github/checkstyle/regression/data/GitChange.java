////////////////////////////////////////////////////////////////////////////////
// checkstyle: Checks Java source code for adherence to a set of rules.
// Copyright (C) 2001-2022 the original author or authors.
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
 * Represents git changes of a file.
 * @author LuoLiangchen
 */
@Value.Immutable
public interface GitChange {
    /**
     * The path of the changed file.
     * @return the path of the changed file
     */
    String path();

    /**
     * The line numbers of the added changes.
     * The first line of a file is marked as line zero.
     * @return the line numbers of the added changes
     */
    List<Integer> addedLines();

    /**
     * The line numbers of the deleted changes.
     * The first line of a file is marked as line zero.
     * @return the line numbers of the deleted changes
     */
    List<Integer> deletedLines();
}
