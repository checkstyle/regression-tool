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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.github.checkstyle.regression.data.GitChange;
import com.github.checkstyle.regression.data.ImmutableGitChange;
import com.github.checkstyle.regression.data.ImmutableModuleExtractInfo;
import com.github.checkstyle.regression.data.ImmutableModuleInfo;
import com.github.checkstyle.regression.data.ModuleExtractInfo;
import com.github.checkstyle.regression.data.ModuleInfo;
import com.github.checkstyle.regression.extract.ExtractInfoProcessor;

public class ModuleInfoCollectorTest {
    private static final String BASE_PACKAGE =
            "com.puppycrawl.tools.checkstyle";

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
        assertUtilsClassHasPrivateConstructor(ModuleCollector.class);
    }

    @Test
    public void testGenerateConfigNodesForValidChanges1() {
        final GitChange moduleChange = ImmutableGitChange.builder()
                .path(JAVA_MAIN_SOURCE_PREFIX + "checks/coding/EmptyStatementCheck.java")
                .build();
        final GitChange testChange = ImmutableGitChange.builder()
                .path(JAVA_TEST_SOURCE_PREFIX + "checks/coding/EmptyStatementCheck.java")
                .build();
        final GitChange nonRelatedChange = ImmutableGitChange.builder()
                .path(JAVA_TEST_SOURCE_PREFIX + "checks/NewlineAtEndOfFileCheckTest.java")
                .build();
        final List<GitChange> changes = Arrays.asList(moduleChange, testChange, nonRelatedChange);
        final ModuleExtractInfo moduleExtractInfo = ImmutableModuleExtractInfo.builder()
                .name("EmptyStatementCheck")
                .packageName(BASE_PACKAGE + ".checks.coding")
                .parent("TreeWalker")
                .build();
        final List<ModuleInfo> moduleInfos =
                ModuleCollector.generate(changes);
        final ModuleInfo moduleInfo = ImmutableModuleInfo.builder()
                .moduleExtractInfo(moduleExtractInfo)
                .build();
        assertEquals("The size of the module info list should be 1", 1, moduleInfos.size());
        assertEquals("The module info of EmptyStatementCheck is wrong",
                moduleInfo, moduleInfos.get(0));
        // just for codecov, no need to check this.
        assertEquals("The module name is wrong", "EmptyStatementCheck", moduleInfos.get(0).name());
    }

    @Test
    public void testGenerateConfigNodesForValidChanges2() {
        final GitChange moduleChange = ImmutableGitChange.builder()
                .path(JAVA_MAIN_SOURCE_PREFIX + "checks/NewlineAtEndOfFileCheck.java")
                .build();
        final GitChange testChange = ImmutableGitChange.builder()
                .path(JAVA_TEST_SOURCE_PREFIX + "checks/NewlineAtEndOfFileCheckTest.java")
                .build();
        final GitChange nonRelatedChange = ImmutableGitChange.builder()
                .path(JAVA_TEST_SOURCE_PREFIX + "checks/coding/EmptyStatementCheck.java")
                .build();
        final List<GitChange> changes = Arrays.asList(moduleChange, testChange, nonRelatedChange);
        final ModuleExtractInfo moduleExtractInfo = ImmutableModuleExtractInfo.builder()
                .name("NewlineAtEndOfFileCheck")
                .packageName(BASE_PACKAGE + ".checks")
                .parent("Checker")
                .build();
        final List<ModuleInfo> moduleInfos =
                ModuleCollector.generate(changes);
        final ModuleInfo moduleInfo = ImmutableModuleInfo.builder()
                .moduleExtractInfo(moduleExtractInfo)
                .build();
        assertEquals("The size of the module info list should be 1", 1, moduleInfos.size());
        assertEquals("The module info of NewlineAtEndOfFileCheck is wrong",
                moduleInfo, moduleInfos.get(0));
    }

    @Test
    public void testGenerateConfigNodesForInvalidChanges() {
        final List<GitChange> changes = new LinkedList<>();
        changes.add(ImmutableGitChange.builder()
                .path(JAVA_MAIN_SOURCE_PREFIX + "PackageObjectFactory.java").build());
        changes.add(ImmutableGitChange.builder()
                .path("src/main/java/Bar.java").build());
        changes.add(ImmutableGitChange.builder()
                .path("foo/A.java").build());
        final List<ModuleInfo> moduleInfos =
                ModuleCollector.generate(changes);
        assertEquals("The size of the module info list should be 0", 0, moduleInfos.size());
    }
}
