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

package com.github.checkstyle.regression;

import static com.github.checkstyle.regression.internal.TestUtils.assertUtilsClassHasPrivateConstructor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Locale;

import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.contrib.java.lang.system.SystemErrRule;
import org.junit.contrib.java.lang.system.SystemOutRule;
import org.junit.rules.TemporaryFolder;

public final class MainTest {
    private static final String USAGE = String.format(Locale.ROOT,
            "usage: java -jar regression-tool.jar -r <arg> -p <arg> [-t <arg>]%n"
                    + "       [--stopAfterConfigGeneration]%n"
                    + " -r,--checkstyleRepoPath <arg>     the path of the checkstyle repository%n"
                    + " -p,--patchBranch <arg>            the name of the PR branch%n"
                    + " -t,--checkstyleTesterPath <arg>   the path of the checkstyle-tester%n"
                    + "                                   directory%n"
                    + "    --stopAfterConfigGeneration    indicates that regression tool would%n"
                    + "                                   stop after generating config%n");

    private static final String EOL = System.getProperty("line.separator");

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();
    @Rule
    public final ExpectedSystemExit exit = ExpectedSystemExit.none();
    @Rule
    public final SystemErrRule systemErr = new SystemErrRule().enableLog().mute();
    @Rule
    public final SystemOutRule systemOut = new SystemOutRule().enableLog().mute();

    @Test
    public void testIsProperUtilsClass() throws Exception {
        assertUtilsClassHasPrivateConstructor(Main.class);
    }

    @Test
    public void testNoArguments() throws Exception {
        exit.expectSystemExitWithStatus(-1);
        exit.checkAssertionAfterwards(() -> {
            assertEquals("Unexpected ouput log", USAGE, systemOut.getLog());
            assertEquals("Unexpected system error log", "Missing required options: r, p" + EOL,
                    systemErr.getLog());
        });
        Main.main();
    }

    @Test
    public void testRepoNonExistent() throws Exception {
        try {
            Main.main("-r", "BAD", "-p", "BAD");
            fail("Exception is expected");
        }
        catch (IllegalArgumentException ex) {
            assertEquals("Invalid error message",
                    "path of local git repo must exist and be a directory",
                    ex.getLocalizedMessage());
        }
    }

    @Test
    public void testRepoEmpty() throws Exception {
        try {
            Main.main("-r", "", "-p", "BAD");
            fail("Exception is expected");
        }
        catch (IllegalArgumentException ex) {
            assertEquals("Invalid error message",
                    "path of local git repo must exist and be a directory",
                    ex.getLocalizedMessage());
        }
    }

    @Test
    public void testRepoFile() throws Exception {
        try {
            final File file = temporaryFolder.newFile();
            Main.main("-r", file.getCanonicalPath(), "-p", "BAD");
            fail("Exception is expected");
        }
        catch (IllegalArgumentException ex) {
            assertEquals("Invalid error message",
                    "path of local git repo must exist and be a directory",
                    ex.getLocalizedMessage());
        }
    }

    @Test
    public void testMissingTester() throws Exception {
        try {
            final File directory = temporaryFolder.newFolder();
            Main.main("-r", directory.getCanonicalPath(), "-p", "BAD");
            fail("Exception is expected");
        }
        catch (IllegalArgumentException ex) {
            assertEquals("Invalid error message",
                    "missing checkstyleTesterPath, which is required if you are not using"
                            + " --stopAfterConfigGeneration mode", ex.getLocalizedMessage());
        }
    }

    @Test
    public void testTesterNonExistent() throws Exception {
        try {
            final File directory = temporaryFolder.newFolder();
            Main.main("-r", directory.getCanonicalPath(), "-t", "BAD", "-p", "BAD");
            fail("Exception is expected");
        }
        catch (IllegalArgumentException ex) {
            assertEquals("Invalid error message",
                    "path of checkstyle tester must exist and be a directory",
                    ex.getLocalizedMessage());
        }
    }

    @Test
    public void testTesterFile() throws Exception {
        try {
            final File directory = temporaryFolder.newFolder();
            final File file = temporaryFolder.newFile();
            Main.main("-r", directory.getCanonicalPath(), "-t", file.getCanonicalPath(), "-p",
                    "BAD");
            fail("Exception is expected");
        }
        catch (IllegalArgumentException ex) {
            assertEquals("Invalid error message",
                    "path of checkstyle tester must exist and be a directory",
                    ex.getLocalizedMessage());
        }
    }
}
