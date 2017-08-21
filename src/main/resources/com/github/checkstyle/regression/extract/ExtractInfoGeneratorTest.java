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

package com.puppycrawl.tools.checkstyle;

import java.beans.PropertyDescriptor;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.beanutils.PropertyUtils;
import org.junit.Test;

import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.AbstractFileSetCheck;
import com.puppycrawl.tools.checkstyle.checks.javadoc.AbstractJavadocCheck;
import com.puppycrawl.tools.checkstyle.internal.CheckUtil;
import com.puppycrawl.tools.checkstyle.internal.TestUtils;
import com.puppycrawl.tools.checkstyle.utils.ModuleReflectionUtils;

/**
 * This file would be injected into checkstyle project and invoked by maven command
 * to generate the module extract info file.
 * @author LuoLiangchen
 */
public class ExtractInfoGeneratorTest {
    /** Modules which do not have global properties to drop. */
    private static final List<String> XML_FILESET_LIST = Arrays.asList(
            "TreeWalker",
            "Checker",
            "Header",
            "Translation",
            "SeverityMatchFilter",
            "SuppressionFilter",
            "SuppressWarningsFilter",
            "BeforeExecutionExclusionFileFilter",
            "RegexpHeader",
            "RegexpOnFilename",
            "RegexpSingleline",
            "RegexpMultiline",
            "JavadocPackage",
            "NewlineAtEndOfFile",
            "UniqueProperties",
            "FileLength",
            "FileTabCharacter"
    );

    /** Properties of abstract check. */
    private static final Set<String> CHECK_PROPERTIES = getProperties(AbstractCheck.class);

    /** Properties of abstract Javadoc check. */
    private static final Set<String> JAVADOC_CHECK_PROPERTIES =
            getProperties(AbstractJavadocCheck.class);

    /** Properties of abstract file-set check. */
    private static final Set<String> FILESET_PROPERTIES = getProperties(AbstractFileSetCheck.class);

    /** Properties without document. */
    private static final List<String> UNDOCUMENTED_PROPERTIES = Arrays.asList(
            "Checker.classLoader",
            "Checker.classloader",
            "Checker.moduleClassLoader",
            "Checker.moduleFactory",
            "TreeWalker.classLoader",
            "TreeWalker.moduleFactory",
            "TreeWalker.cacheFile",
            "TreeWalker.upChild",
            "SuppressWithNearbyCommentFilter.fileContents",
            "SuppressionCommentFilter.fileContents"
    );

    /**
     * Generates the extract info file named as "checkstyle_modules.json".
     * @throws Exception failure when generating the file
     */
    @Test
    public void generateExtractInfoFile() throws Exception {
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
     * @throws Exception failure when creating Json object
     */
    private static JsonUtil.JsonObject createJsonObjectFromModuleClass(Class<?> clazz)
            throws Exception {
        final JsonUtil.JsonObject object = new JsonUtil.JsonObject();

        final String name = clazz.getSimpleName();
        final String parent;
        if (ModuleReflectionUtils.isCheckstyleCheck(clazz)) {
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

        final JsonUtil.JsonArray properties = new JsonUtil.JsonArray();
        for (String propertyName : getNecessaryProperties(clazz)) {
            final JsonUtil.JsonObject property = new JsonUtil.JsonObject();
            property.addProperty("name", propertyName);
            Arrays.stream(PropertyUtils.getPropertyDescriptors(clazz))
                    .filter(p -> p.getName().equals(propertyName))
                    .map(PropertyDescriptor::getPropertyType)
                    .map(Class::getSimpleName)
                    .findAny()
                    .ifPresent(type -> property.addProperty("type", type));
            properties.add(property);
        }
        object.add("properties", properties);

        return object;
    }

    /**
     * Gets the necessary properties of a checkstyle module.
     * Global properties and undocumented properties are not necessary for us.
     * @param clazz the class instance of the given module
     * @return a set of the necessary properties of the module
     * @throws Exception failure when getting properties
     */
    // -@cs[CyclomaticComplexity] many different kinds of module
    private static Set<String> getNecessaryProperties(Class<?> clazz)
            throws Exception {
        final Set<String> properties = getProperties(clazz);
        if (hasParentModule(clazz.getSimpleName())) {
            if (AbstractJavadocCheck.class.isAssignableFrom(clazz)) {
                properties.removeAll(JAVADOC_CHECK_PROPERTIES);
            }
            else if (ModuleReflectionUtils.isCheckstyleCheck(clazz)) {
                properties.removeAll(CHECK_PROPERTIES);
            }
        }
        if (ModuleReflectionUtils.isFileSetModule(clazz)) {
            properties.removeAll(FILESET_PROPERTIES);

            // override
            properties.add("fileExtensions");
        }

        // undocumented properties are not necessary
        properties.removeIf(prop -> UNDOCUMENTED_PROPERTIES.contains(
                clazz.getSimpleName() + "." + prop));

        final PackageObjectFactory factory = TestUtils.getPackageObjectFactory();
        final Object instance = factory.createModule(clazz.getSimpleName());

        if (ModuleReflectionUtils.isCheckstyleCheck(clazz)) {
            final AbstractCheck check = (AbstractCheck) instance;

            final int[] acceptableTokens = check.getAcceptableTokens();
            Arrays.sort(acceptableTokens);
            final int[] defaultTokens = check.getDefaultTokens();
            Arrays.sort(defaultTokens);
            final int[] requiredTokens = check.getRequiredTokens();
            Arrays.sort(requiredTokens);

            if (!Arrays.equals(acceptableTokens, defaultTokens)
                    || !Arrays.equals(acceptableTokens, requiredTokens)) {
                properties.add("tokens");
            }
        }

        if (AbstractJavadocCheck.class.isAssignableFrom(clazz)) {
            final AbstractJavadocCheck check = (AbstractJavadocCheck) instance;

            final int[] acceptableJavadocTokens = check.getAcceptableJavadocTokens();
            Arrays.sort(acceptableJavadocTokens);
            final int[] defaultJavadocTokens = check.getDefaultJavadocTokens();
            Arrays.sort(defaultJavadocTokens);
            final int[] requiredJavadocTokens = check.getRequiredJavadocTokens();
            Arrays.sort(requiredJavadocTokens);

            if (!Arrays.equals(acceptableJavadocTokens, defaultJavadocTokens)
                    || !Arrays.equals(acceptableJavadocTokens, requiredJavadocTokens)) {
                properties.add("javadocTokens");
            }
        }

        return properties;
    }

    /**
     * Gets the properties of a checkstyle module.
     * @param clazz the class instance of the given module
     * @return a set of the properties of the module
     */
    private static Set<String> getProperties(Class<?> clazz) {
        final Set<String> result = new TreeSet<>();
        final PropertyDescriptor[] map = PropertyUtils.getPropertyDescriptors(clazz);

        for (PropertyDescriptor p : map) {
            if (p.getWriteMethod() != null) {
                result.add(p.getName());
            }
        }

        return result;
    }

    /**
     * Checks whether a module has a parent that may contains global properties.
     * @param className the class name of given module
     * @return true if the module has a parent
     */
    private static boolean hasParentModule(String className) {
        return !XML_FILESET_LIST.contains(className) && XML_FILESET_LIST.stream()
                .map(name -> name + "Check").noneMatch(name -> name.equals(className));
    }
}
