////////////////////////////////////////////////////////////////////////////////
// checkstyle: Checks Java source code for adherence to a set of rules.
// Copyright (C) 2001-2019 the original author or authors.
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

package com.github.checkstyle.regression.extract;

import static com.github.checkstyle.regression.internal.TestUtils.assertUtilsClassHasPrivateConstructor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.github.checkstyle.regression.data.ImmutableModuleExtractInfo;
import com.github.checkstyle.regression.data.ModuleExtractInfo;

public class ExtractInfoProcessorTest {
    private static final String BASE_PACKAGE = "com.puppycrawl.tools.checkstyle";

    private Method getModuleExtractInfosFromReaderMethod;

    @Before
    public void setUp() throws Exception {
        getModuleExtractInfosFromReaderMethod = ExtractInfoProcessor.class
                .getDeclaredMethod("getModuleExtractInfosFromReader", Reader.class);
        getModuleExtractInfosFromReaderMethod.setAccessible(true);
    }

    @Test
    public void testIsProperUtilsClass() throws Exception {
        assertUtilsClassHasPrivateConstructor(ExtractInfoProcessor.class);
    }

    @Test
    public void testGetModuleExtractInfosFromReader() throws Exception {
        final InputStream is = ExtractInfoProcessor.class.getClassLoader()
                .getResourceAsStream("checkstyle_modules.json");
        final InputStreamReader reader = new InputStreamReader(is, Charset.forName("UTF-8"));
        final Map<String, ModuleExtractInfo> map = getModuleExtractInfosFromReader(reader);

        final String module1 = "NewlineAtEndOfFileCheck";
        final ModuleExtractInfo extractInfo1 = ImmutableModuleExtractInfo.builder()
                .name(module1)
                .packageName(BASE_PACKAGE + ".checks")
                .parent("Checker")
                .build();
        final String module2 = "EmptyStatementCheck";
        final ModuleExtractInfo extractInfo2 = ImmutableModuleExtractInfo.builder()
                .name(module2)
                .packageName(BASE_PACKAGE + ".checks.coding")
                .parent("TreeWalker")
                .build();
        assertEquals("There should be 2 entries in the extract info map.", 2, map.size());
        assertEquals("The extract info of NewlineAtEndOfFileCheck is wrong.",
                extractInfo1, map.get(BASE_PACKAGE + ".checks." + module1));
        assertEquals("The extract info of EmptyStatementCheck is wrong.",
                extractInfo2, map.get(BASE_PACKAGE + ".checks.coding." + module2));
    }

    @Test
    public void testGetNameToModuleInfosFromInputStreamWithException() throws Exception {
        final InputStream is = spy(ExtractInfoProcessor.class.getClassLoader()
                .getResourceAsStream("checkstyle_modules.json"));
        final InputStreamReader reader = new InputStreamReader(is, Charset.forName("UTF-8"));
        doThrow(IOException.class).when(is).close();

        try {
            getModuleExtractInfosFromReader(reader);
            fail("Exception is expected");
        }
        catch (InvocationTargetException ex) {
            assertEquals(
                    "Exception message is wrong",
                    "Failed when loaing hardcoded checkstyle module information",
                    ex.getTargetException().getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, ModuleExtractInfo> getModuleExtractInfosFromReader(Reader reader)
            throws Exception {
        return (Map<String, ModuleExtractInfo>) getModuleExtractInfosFromReaderMethod
                .invoke(ExtractInfoProcessor.class, reader);
    }
}
