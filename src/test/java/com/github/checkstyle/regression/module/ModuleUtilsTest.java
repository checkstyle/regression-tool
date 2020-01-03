////////////////////////////////////////////////////////////////////////////////
// checkstyle: Checks Java source code for adherence to a set of rules.
// Copyright (C) 2001-2020 the original author or authors.
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

import static com.github.checkstyle.regression.internal.TestUtils.assertUtilsClassHasPrivateConstructor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.github.checkstyle.regression.data.GitChange;
import com.github.checkstyle.regression.data.ImmutableGitChange;
import com.github.checkstyle.regression.data.ImmutableModuleExtractInfo;
import com.github.checkstyle.regression.data.ModuleExtractInfo;
import com.github.checkstyle.regression.extract.ExtractInfoProcessor;

public class ModuleUtilsTest {
    private static final String BASE_PACKAGE = "com.puppycrawl.tools.checkstyle";

    private static final String JAVA_MAIN_SOURCE_PREFIX =
            "src/main/java/com/puppycrawl/tools/checkstyle/";

    private static final String JAVA_TEST_SOURCE_PREFIX =
            "src/test/java/com/puppycrawl/tools/checkstyle/";

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        final InputStream is = ExtractInfoProcessor.class.getClassLoader()
                .getResourceAsStream("checkstyle_modules.json");
        final InputStreamReader reader = new InputStreamReader(is, Charset.forName("UTF-8"));
        final Method method = ExtractInfoProcessor.class
                .getDeclaredMethod("getModuleExtractInfosFromReader", Reader.class);
        method.setAccessible(true);
        final Map<String, ModuleExtractInfo> map =
                (Map<String, ModuleExtractInfo>) method.invoke(ExtractInfoProcessor.class, reader);
        ModuleUtils.setNameToModuleExtractInfo(map);
    }

    @Test
    public void testIsProperUtilsClass() throws Exception {
        assertUtilsClassHasPrivateConstructor(ModuleUtils.class);
    }

    @Test
    public void testIsCheckstyleModule() {
        final GitChange change = ImmutableGitChange.builder()
                .path(JAVA_MAIN_SOURCE_PREFIX + "checks/coding/EmptyStatementCheck.java")
                .build();
        final boolean result = ModuleUtils.isCheckstyleModule(change);
        assertTrue("EmptyStatementCheck should be considered as a checkstyle module", result);
    }

    @Test
    public void testIsCheckstyleModuleNonModule() {
        final GitChange change = ImmutableGitChange.builder()
                .path(JAVA_MAIN_SOURCE_PREFIX + "PackageObjectFactory.java")
                .build();
        final boolean result = ModuleUtils.isCheckstyleModule(change);
        assertFalse("PackageObjectFactory should not be considered as a checkstyle module", result);
    }

    @Test
    public void testIsCheckstyleModuleNonMainFile() {
        final GitChange change = ImmutableGitChange.builder()
                .path("backup/test/java/foo/Foo.java")
                .build();
        final boolean result =
                ModuleUtils.isCheckstyleModule(change);
        assertFalse("Non main file should not be consideres as a checkstyle module", result);
    }

    @Test
    public void testIsCheckstyleUtility() {
        final GitChange change = ImmutableGitChange.builder()
                .path(JAVA_MAIN_SOURCE_PREFIX + "utils/CheckUtils.java")
                .build();
        final boolean result = ModuleUtils.isCheckstyleUtility(change);
        assertTrue("CheckUtils should be considered as a checkstyle utility", result);
    }

    @Test
    public void testIsCheckstyleUtilityNonUtility() {
        final GitChange change = ImmutableGitChange.builder()
                .path(JAVA_MAIN_SOURCE_PREFIX + "checks/coding/EmptyStatementCheck.java")
                .build();
        final boolean result = ModuleUtils.isCheckstyleUtility(change);
        assertFalse("EmptyStatementCheck should not be considered as a checkstyle utility",
                result);
    }

    @Test
    public void testIsCheckstyleUtilityNonMainFile() {
        final GitChange change = ImmutableGitChange.builder()
                .path("src/test/java/foo/Foo.java")
                .build();
        final boolean result =
                ModuleUtils.isCheckstyleUtility(change);
        assertFalse("Non main file should not be consideres as a checkstyle utility", result);
    }

    @Test
    public void testIsCheckstyleModuleTest() {
        final GitChange change = ImmutableGitChange.builder()
                .path(JAVA_TEST_SOURCE_PREFIX + "checks/coding/EmptyStatementCheckTest.java")
                .build();
        final boolean result = ModuleUtils.isCheckstyleModuleTest(change);
        assertTrue("EmptyStatementCheckTest should be considered as a checkstyle module test",
                result);
    }

    @Test
    public void testIsCheckstyleModuleTestNonTestClass() {
        final GitChange change = ImmutableGitChange.builder()
                .path(JAVA_TEST_SOURCE_PREFIX + "internal/CheckUtil.java")
                .build();
        final boolean result = ModuleUtils.isCheckstyleModuleTest(change);
        assertFalse("CheckUtil should not be considered as a checkstyle module test",
                result);
    }

    @Test
    public void testIsCheckstyleModuleTestNonTestFile() {
        final GitChange change = ImmutableGitChange.builder()
                .path("backup/test/java/foo/Foo.java")
                .build();
        final boolean result = ModuleUtils.isCheckstyleModuleTest(change);
        assertFalse("CheckUtil should not be considered as a checkstyle module test",
                result);
    }

    @Test
    public void testConvertModuleChangeToExtractInfo() {
        final ModuleExtractInfo moduleExtractInfo = ModuleUtils
                .getModuleExtractInfo(BASE_PACKAGE + ".checks.coding.EmptyStatementCheck");
        final ModuleExtractInfo expected = ImmutableModuleExtractInfo.builder()
                .name("EmptyStatementCheck")
                .packageName(BASE_PACKAGE + ".checks.coding")
                .parent("TreeWalker")
                .build();
        assertEquals("The extract info of EmptyStatementCheck is wrong",
                expected, moduleExtractInfo);
    }
}
