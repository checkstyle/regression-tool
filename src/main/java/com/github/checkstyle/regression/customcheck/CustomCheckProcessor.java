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

package com.github.checkstyle.regression.customcheck;

import java.io.File;
import java.util.Collections;
import java.util.List;

import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.DefaultConfiguration;
import com.puppycrawl.tools.checkstyle.TreeWalker;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.Configuration;

/**
 * Processes the specific file using our custom check.
 *
 * <p>This utility class would run a custom check on the file with the given path and return
 * the collected property info.</p>
 * @author LuoLiangchen
 */
public final class CustomCheckProcessor {
    /** Prevents instantiation. */
    private CustomCheckProcessor() {
    }

    /**
     * Processes the file with the given path using the given custom check.
     *
     * <p>The custom check needs a public/static function to retrieve the processing result.</p>
     * @param path       the path of the file
     * @param checkClass the class of the custom check
     * @throws CheckstyleException failure when running the check
     */
    public static void process(String path, Class<?> checkClass) throws CheckstyleException {
        final DefaultConfiguration moduleConfig = createModuleConfig(checkClass);
        final Configuration dc = createTreeWalkerConfig(moduleConfig);
        final Checker checker = new Checker();
        checker.setModuleClassLoader(Thread.currentThread().getContextClassLoader());
        checker.configure(dc);
        final List<File> processedFiles = Collections.singletonList(new File(path));
        checker.process(processedFiles);
    }

    /**
     * Creates {@link DefaultConfiguration} for the {@link TreeWalker}
     * based on the given {@link Configuration} instance.
     * @param config {@link Configuration} instance.
     * @return {@link DefaultConfiguration} for the {@link TreeWalker}
     *         based on the given {@link Configuration} instance.
     */
    private static DefaultConfiguration createTreeWalkerConfig(Configuration config) {
        final DefaultConfiguration dc =
                new DefaultConfiguration("configuration");
        final DefaultConfiguration twConf = createModuleConfig(TreeWalker.class);
        // make sure that the tests always run with this charset
        dc.addAttribute("charset", "UTF-8");
        dc.addChild(twConf);
        twConf.addChild(config);
        return dc;
    }

    /**
     * Creates {@link DefaultConfiguration} for the given class.
     * @param clazz the class of module
     * @return the {@link DefaultConfiguration} of the module class
     */
    private static DefaultConfiguration createModuleConfig(Class<?> clazz) {
        return new DefaultConfiguration(clazz.getName());
    }
}
