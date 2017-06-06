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

package com.github.checkstyle.regression.generator;

import java.util.List;
import java.util.stream.Collectors;

import org.w3c.dom.Element;

import com.github.checkstyle.regression.git.GitChange;
import com.puppycrawl.tools.checkstyle.utils.ModuleReflectionUtils;

/**
 * The simplest generator implementation using only default options of checks.
 * @author LuoLiangchen
 */
public class DefaultConfigGenerator extends AbstractConfigGenerator {
    @Override
    protected List<Element> createModuleElements(List<GitChange> gitChanges) {
        return gitChanges.stream()
                .map(this::createModuleElement)
                .collect(Collectors.toList());
    }

    /**
     * Creates checkstyle module element from a {@code GitChange}.
     * @param gitChange the git change to be processed
     * @return the generated module element
     */
    private Element createModuleElement(GitChange gitChange) {
        final Element module = getXmlDocument().createElement(ELEMENT_MODULE);
        final String moduleName = createClassFromPath(gitChange.getPath()).getSimpleName();
        module.setAttribute(ATTR_NAME, moduleName);
        return module;
    }

    @Override
    protected boolean whetherToSkipChange(GitChange gitChange) {
        boolean returnValue = super.whetherToSkipChange(gitChange);
        if (!gitChange.getPath().startsWith("src/main/java")) {
            returnValue = true;
        }
        else {
            final Class<?> clazz = createClassFromPath(gitChange.getPath());
            if (clazz != null) {
                returnValue |= !ModuleReflectionUtils.isCheckstyleModule(clazz);
            }
            else {
                returnValue = true;
            }
        }
        return returnValue;
    }

    /**
     * Creates the class corresponding to the changed file path.
     * @param path the changed file path
     * @return the class corresponding to the changed file path
     */
    private Class<?> createClassFromPath(String path) {
        Class<?> returnValue;
        final String className = path
                .replace("src/main/java/", "")
                .replace(".java", "")
                .replaceAll("/", ".");
        try {
            returnValue = Class.forName(className);
        }
        catch (ClassNotFoundException ex) {
            returnValue = null;
        }
        return returnValue;
    }
}
