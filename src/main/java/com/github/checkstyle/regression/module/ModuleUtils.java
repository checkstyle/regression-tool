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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.github.checkstyle.regression.data.GitChange;
import com.github.checkstyle.regression.data.ModuleExtractInfo;

/**
 * Contains utility methods related to checkstyle module.
 * @author LuoLiangchen
 */
public final class ModuleUtils {
    /** The compiled regex pattern of the path of Java source files. */
    private static final Pattern JAVA_SOURCE_PARTTEN =
            Pattern.compile("src/(main|test)/java/(.+)\\.java");

    /** The postfix of a test of a checkstyle module. */
    private static final String TEST_POSTFIX = "Test";

    /** The map of full qualified name to module extract info. */
    private static final Map<String, ModuleExtractInfo> NAME_TO_MODULE_EXTRACT_INFO =
            new HashMap<>();

    /** Prevents instantiation. */
    private ModuleUtils() {
    }

    /**
     * Sets the map of full qualified name to module extract info with the given map.
     * @param map the given map
     */
    public static void setNameToModuleExtractInfo(Map<String, ModuleExtractInfo> map) {
        NAME_TO_MODULE_EXTRACT_INFO.clear();
        NAME_TO_MODULE_EXTRACT_INFO.putAll(map);
    }

    /**
     * Checks whether the corresponding file of a change may be considered as
     * a checkstyle module.
     * Changes of checkstyle modules are Java main source files, of which full
     * qualified names are in the extract module info map.
     * @param change change to check
     * @return true if the corresponding file of a change may be considered as
     *      a checkstyle module
     */
    public static boolean isCheckstyleModule(GitChange change) {
        final boolean returnValue;
        if (isJavaMainSource(change)) {
            final String fullName = convertJavaSourceChangeToFullName(change);
            returnValue = NAME_TO_MODULE_EXTRACT_INFO.containsKey(fullName);
        }
        else {
            returnValue = false;
        }
        return returnValue;
    }

    /**
     * Checks whether the corresponding file of a change may be considered as
     * a checkstyle utility class.
     * Changes of checkstyle utility classes are Java main source files, of which full
     * qualified names are not in the extract module info map.
     * @param change change to check
     * @return true if the corresponding file of a change may be considered as
     *      a checkstyle utility class.
     */
    public static boolean isCheckstyleUtility(GitChange change) {
        final boolean returnValue;
        if (isJavaMainSource(change)) {
            final String fullName = convertJavaSourceChangeToFullName(change);
            returnValue = !NAME_TO_MODULE_EXTRACT_INFO.containsKey(fullName);
        }
        else {
            returnValue = false;
        }
        return returnValue;
    }

    /**
     * Checks whether the corresponding file of a change may be considered as
     * a test of checkstyle module.
     * Changes of checkstyle module tests are Java test source files, of which full
     * qualified names end with "Test" and the full names of corresponding modules are
     * in the extract module info map.
     * @param change change to check
     * @return true if the corresponding file of a change may be considered as
     *      a test of checkstyle module
     */
    public static boolean isCheckstyleModuleTest(GitChange change) {
        final boolean returnValue;
        if (isJavaTestSource(change)) {
            final String fullName = convertJavaSourceChangeToFullName(change);
            if (fullName.endsWith(TEST_POSTFIX)) {
                returnValue = NAME_TO_MODULE_EXTRACT_INFO.containsKey(
                        fullName.substring(0, fullName.length() - TEST_POSTFIX.length()));
            }
            else {
                returnValue = false;
            }
        }
        else {
            returnValue = false;
        }
        return returnValue;
    }

    /**
     * Checks whether the corresponding file of a change may be considered as
     * a Java main source file.
     * Changes of Java main source files are which have path matching
     * {@code JAVA_SOURCE_PARTTEN} and in "main" directory.
     * @param change change to check
     * @return true if the corresponding file of a change may be considered as
     *      a Java main source file.
     */
    private static boolean isJavaMainSource(GitChange change) {
        final boolean returnValue;
        final Matcher matcher = JAVA_SOURCE_PARTTEN.matcher(change.path());
        if (matcher.find()) {
            returnValue = "main".equals(matcher.group(1));
        }
        else {
            returnValue = false;
        }
        return returnValue;
    }

    /**
     * Checks whether the corresponding file of a change may be considered as
     * a Java test source file.
     * Changes of Java test source files are which have path matching
     * {@code JAVA_SOURCE_PARTTEN} and in "test" directory.
     * @param change change to check
     * @return true if the corresponding file of a change may be considered as
     *      a Java test source file.
     */
    private static boolean isJavaTestSource(GitChange change) {
        final boolean returnValue;
        final Matcher matcher = JAVA_SOURCE_PARTTEN.matcher(change.path());
        if (matcher.find()) {
            returnValue = "test".equals(matcher.group(1));
        }
        else {
            returnValue = false;
        }
        return returnValue;
    }

    /**
     * Gets the module extract info from the given full qualified name.
     * @param fullName the given full qualified name
     * @return the module extract info got from the given full qualified name
     */
    public static ModuleExtractInfo getModuleExtractInfo(String fullName) {
        return NAME_TO_MODULE_EXTRACT_INFO.get(fullName);
    }

    /**
     * Converts a change of Java source file to its full qualified name.
     * @param change the change instance of Java source file
     * @return the corresponding full qualified name
     */
    public static String convertJavaSourceChangeToFullName(GitChange change) {
        return Arrays.stream(JAVA_SOURCE_PARTTEN.matcher(change.path())
                .replaceAll("$2").split("/")).collect(Collectors.joining("."));
    }
}
