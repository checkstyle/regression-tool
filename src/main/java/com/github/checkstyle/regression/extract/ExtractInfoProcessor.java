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

package com.github.checkstyle.regression.extract;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.github.checkstyle.regression.data.ModuleExtractInfo;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.sun.tools.javac.util.ServiceLoader;

/**
 * Processes the module extract info grabbed from checkstyle project.
 * @author LuoLiangchen
 */
public final class ExtractInfoProcessor {
    /** The {@link Gson} instance. */
    private static final Gson GSON;

    /** The type of {@code List<ModuleExtractInfo>}. */
    private static final Type TYPE_EXTRACT_INFO_LIST = new TypeToken<List<ModuleExtractInfo>>() {
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
     * Gets the module extract info map from the given reader. Map key is the
     * fully qualified module name.
     * @param reader the given reader
     * @return the full qualified name to module extract info map
     */
    public static Map<String, ModuleExtractInfo> getModuleExtractInfosFromReader(
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
