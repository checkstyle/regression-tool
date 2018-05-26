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

package com.puppycrawl.tools.checkstyle;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.junit.Test;

import com.puppycrawl.tools.checkstyle.internal.utils.CheckUtil;
import com.puppycrawl.tools.checkstyle.utils.ModuleReflectionUtils;

/**
 * This file would be injected into checkstyle project and invoked by maven command
 * to generate the module extract info file.
 * @author LuoLiangchen
 */
public class ExtractInfoGeneratorTest {
    /**
     * Generates the extract info file named as "checkstyle_modules.json".
     * @throws IOException failure when generating the file
     */
    @Test
    public void generateExtractInfoFile() throws IOException {
        final List<Class<?>> modules = new ArrayList<>(CheckUtil.getCheckstyleModules());
        modules.sort(Comparator.comparing(Class::getSimpleName));
        final JsonUtil.JsonArray moduleJsonArray = new JsonUtil.JsonArray();
        for (Class<?> module : modules) {
            moduleJsonArray.add(createJsonObjectFromModuleClass(module));
        }
        final String jsonString = moduleJsonArray.toString();
        final File output = new File("checkstyle_modules.json");
        Files.write(output.toPath(), jsonString.getBytes(Charset.forName("UTF-8")),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * Creates Json object for a module from the module class.
     * @param clazz the given module class
     * @return the Json object describing the extract info of the module
     */
    private static JsonUtil.JsonObject createJsonObjectFromModuleClass(Class<?> clazz) {
        final JsonUtil.JsonObject object = new JsonUtil.JsonObject();

        final String name = clazz.getSimpleName();
        final String parent;
        if (ModuleReflectionUtils.isCheckstyleTreeWalkerCheck(clazz)
                || ModuleReflectionUtils.isTreeWalkerFilterModule(clazz)) {
            parent = "TreeWalker";
        }
        else if (ModuleReflectionUtils.isRootModule(clazz)) {
            parent = "";
        }
        else {
            parent = "Checker";
        }
        object.addProperty("name", name);
        object.addProperty("packageName", clazz.getPackage().getName());
        object.addProperty("parent", parent);

        final JsonUtil.JsonArray interfaces = new JsonUtil.JsonArray();
        final JsonUtil.JsonArray hierarchies = new JsonUtil.JsonArray();
        Arrays.stream(clazz.getInterfaces()).forEach(cls -> interfaces.add(cls.getCanonicalName()));
        Class<?> superClass = clazz.getSuperclass();
        while (!Object.class.equals(superClass)) {
            hierarchies.add(superClass.getCanonicalName());
            Arrays.stream(superClass.getInterfaces())
                    .forEach(cls -> interfaces.add(cls.getCanonicalName()));
            superClass = superClass.getSuperclass();
        }
        object.add("interfaces", interfaces);
        object.add("hierarchies", hierarchies);

        return object;
    }
}
