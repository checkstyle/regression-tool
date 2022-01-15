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

package com.github.checkstyle.regression.configuration;

import static com.github.checkstyle.regression.configuration.ConfigGenerator.DOCTYPE_PUBLIC;
import static com.github.checkstyle.regression.internal.FileUtils.readFile;
import static com.github.checkstyle.regression.internal.TestUtils.assertUtilsClassHasPrivateConstructor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Test;

import com.github.checkstyle.regression.data.ImmutableModuleExtractInfo;
import com.github.checkstyle.regression.data.ImmutableModuleInfo;
import com.github.checkstyle.regression.data.ModuleExtractInfo;
import com.github.checkstyle.regression.data.ModuleInfo;

public class ConfigGeneratorTest {
    private static final String BASE_PACKAGE = "com.puppycrawl.tools.checkstyle";

    private final Collection<File> outputConfigs = new LinkedList<>();

    @After
    public void tearDown() throws Exception {
        for (File outputConfig : outputConfigs) {
            try {
                FileUtils.forceDelete(outputConfig);
            }
            catch (IOException ignore) {
                // ignore, keep deleting
            }
        }
    }

    @Test
    public void testIsProperUtilsClass() throws Exception {
        assertUtilsClassHasPrivateConstructor(ConfigGenerator.class);
    }

    @Test
    public void testDtdVersion() throws Exception {
        final String checkstyleConfigUrl = "https://raw.githubusercontent.com/checkstyle/"
                + "checkstyle/master/config/checkstyle_checks.xml";
        try (InputStream response = new URL(checkstyleConfigUrl).openStream()) {
            final StringWriter writer = new StringWriter();
            IOUtils.copy(response, writer, "UTF-8");
            final String checkstyleConfg = writer.toString();
            assertTrue("Dtd version is not the latest one",
                    checkstyleConfg.contains(DOCTYPE_PUBLIC));
        }
    }

    @Test
    public void testGenerateConfigTextWithEmptyModuleInfos() throws Exception {
        final File excepted = getExpectedXml("expected_empty_module_infos.xml");

        final File actual = generateConfig(Collections.emptyList());
        assertEquals("Config is not as expected", readFile(excepted), readFile(actual));
    }

    @Test
    public void testGenerateConfigTextWithCheckerParentModule() throws Exception {
        final File excepted = getExpectedXml("expected_checker_parent_module.xml");

        final ModuleExtractInfo extractInfo = ImmutableModuleExtractInfo.builder()
                .name("FileLengthCheck")
                .packageName(BASE_PACKAGE + ".checks.sizes")
                .parent("Checker")
                .build();
        final ModuleInfo moduleInfo = ImmutableModuleInfo.builder()
                .moduleExtractInfo(extractInfo)
                .build();

        final File actual = generateConfig(Collections.singletonList(moduleInfo));

        assertEquals("Config is not as expected", readFile(excepted), readFile(actual));
    }

    @Test
    public void testGenerateConfigTextWithTreeWalkerParentModule() throws Exception {
        final File excepted = getExpectedXml("expected_tree_walker_parent_module.xml");

        final ModuleExtractInfo extractInfo = ImmutableModuleExtractInfo.builder()
                .name("HiddenFieldCheck")
                .packageName(BASE_PACKAGE + ".checks.coding")
                .parent("TreeWalker")
                .build();
        final ModuleInfo moduleInfo = ImmutableModuleInfo.builder()
                .moduleExtractInfo(extractInfo)
                .build();

        final File actual = generateConfig(Collections.singletonList(moduleInfo));
        assertEquals("Config is not as expected", readFile(excepted), readFile(actual));
    }

    @Test
    public void testGenerateConfigTextWithMiscModuleInfos() throws Exception {
        final File excepted = getExpectedXml("expected_misc_module_infos.xml");

        final ModuleExtractInfo extractInfo1 = ImmutableModuleExtractInfo.builder()
                .name("NewlineAtEndOfFileCheck")
                .packageName(BASE_PACKAGE + ".checks")
                .parent("Checker")
                .build();
        final ModuleInfo moduleInfo1 = ImmutableModuleInfo.builder()
                .moduleExtractInfo(extractInfo1)
                .build();

        final ModuleExtractInfo extractInfo2 = ImmutableModuleExtractInfo.builder()
                .name("EmptyStatementCheck")
                .packageName(BASE_PACKAGE + ".checks.coding")
                .parent("TreeWalker")
                .build();
        final ModuleInfo moduleInfo2 = ImmutableModuleInfo.builder()
                .moduleExtractInfo(extractInfo2)
                .build();

        final File actual = generateConfig(Arrays.asList(moduleInfo1, moduleInfo2));
        assertEquals("Config is not as expected", readFile(excepted), readFile(actual));
    }

    @Test
    public void testGenerateConfigToNewFileNoException() throws Exception {
        final File temp = File.createTempFile("TempNewFile", "");
        final String path = temp.getPath();
        // delete the file, to test generating file with mode StandardOpenOption.CREATE
        temp.delete();
        final File output = ConfigGenerator.generateConfig(path, Collections.emptyList());
        outputConfigs.add(output);
    }

    private File generateConfig(List<ModuleInfo> moduleInfos) throws Exception {
        final File temp = File.createTempFile("TestTempConfigOutput", "");
        final String path = temp.getPath();
        final File output = ConfigGenerator.generateConfig(path, moduleInfos);
        outputConfigs.add(output);
        return output;
    }

    private static File getExpectedXml(String fileName) {
        return new File(
                "src/test/resources/com/github/checkstyle/regression/configuration/" + fileName);
    }
}
