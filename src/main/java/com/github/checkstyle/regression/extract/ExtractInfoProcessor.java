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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

import com.github.checkstyle.regression.data.ModuleExtractInfo;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;

/**
 * Processes the module extract info grabbed from checkstyle project.
 * @author LuoLiangchen
 */
public final class ExtractInfoProcessor {
    /** The {@link Gson} instance. */
    private static final Gson GSON;

    /** The type of {@code List<ModuleExtractInfo>}. */
    private static final Type TYPE_EXTRACT_INFO_LIST = new TypeToken<List<ModuleExtractInfo>>() {
        // constructor is not visible
    }.getType();

    /** Prevents instantiation. */
    private ExtractInfoProcessor() {
    }

    static {
        final GsonBuilder gsonBuilder = new GsonBuilder();
        for (TypeAdapterFactory factory : ServiceLoader.load(TypeAdapterFactory.class)) {
            gsonBuilder.registerTypeAdapterFactory(factory);
        }
        GSON = gsonBuilder.create();
    }

    /**
     * Gets the module extract info map from the given branch of checkstyle repository.
     * @param repoPath the path of checkstyle repository
     * @param branch   the given branch on which to generate the extract info
     * @return the full qualified name to module extract info map
     * @throws InjectException failure when making injection
     */
    public static Map<String, ModuleExtractInfo> getModuleExtractInfos(
            String repoPath, String branch) throws InjectException {
        final Map<String, ModuleExtractInfo> returnValue;
        final CheckstyleInjector injector = new CheckstyleInjector(repoPath, branch);

        try {
            final File file = injector.generateExtractInfoFile();

            try {
                final Reader reader = new InputStreamReader(
                        new FileInputStream(file), Charset.forName("UTF-8"));
                returnValue = getModuleExtractInfosFromReader(reader);
            }
            catch (FileNotFoundException ex) {
                throw new InjectException(
                        "unable to find the generated module extract info file", ex);
            }
            finally {
                injector.clearInjections();
            }
        }
        finally {
            injector.close();
        }

        return returnValue;
    }

    /**
     * Gets the module extract info map from the given reader. Map key is the
     * fully qualified module name.
     * @param reader the given reader
     * @return the full qualified name to module extract info map
     */
    private static Map<String, ModuleExtractInfo> getModuleExtractInfosFromReader(
            Reader reader) {
        final List<ModuleExtractInfo> modules;

        try {
            try {
                modules = GSON.fromJson(reader, TYPE_EXTRACT_INFO_LIST);
            }
            finally {
                reader.close();
            }
        }
        catch (IOException ex) {
            throw new IllegalStateException(
                    "Failed when loaing hardcoded checkstyle module information", ex);
        }

        return modules.stream().collect(Collectors.toMap(
                ModuleExtractInfo::fullName, extractInfo -> extractInfo));
    }
}
