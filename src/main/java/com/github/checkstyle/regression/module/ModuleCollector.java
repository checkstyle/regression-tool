////////////////////////////////////////////////////////////////////////////////
// checkstyle: Checks Java source code for adherence to a set of rules.
// Copyright (C) 2001-2021 the original author or authors.
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

package com.github.checkstyle.regression.module;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.github.checkstyle.regression.data.GitChange;
import com.github.checkstyle.regression.data.ModifiableModuleInfo;
import com.github.checkstyle.regression.data.ModuleExtractInfo;
import com.github.checkstyle.regression.data.ModuleInfo;

/**
 * Collects all the necessary information for the generation, in module level.
 * @author LuoLiangchen
 */
public final class ModuleCollector {
    /** Prevents instantiation. */
    private ModuleCollector() {
    }

    /**
     * Generates the module information from a list of changes.
     * @param changes the changes source
     * @return the module information generated from the given changes
     */
    public static List<ModuleInfo> generate(List<GitChange> changes) {
        final Map<String, ModifiableModuleInfo> moduleInfos = new LinkedHashMap<>();

        for (GitChange change : changes) {
            if (ModuleUtils.isCheckstyleModule(change)) {
                final String fullName = ModuleUtils.convertJavaSourceChangeToFullName(change);
                final ModuleExtractInfo extractInfo = ModuleUtils.getModuleExtractInfo(fullName);
                final ModifiableModuleInfo moduleInfo = ModifiableModuleInfo.create()
                        .setModuleExtractInfo(extractInfo);
                moduleInfos.put(extractInfo.fullName(), moduleInfo);
            }
        }

        return moduleInfos.values().stream()
                .map(ModifiableModuleInfo::toImmutable)
                .collect(Collectors.toList());
    }
}
