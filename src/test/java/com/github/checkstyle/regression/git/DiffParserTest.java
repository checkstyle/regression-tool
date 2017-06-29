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

package com.github.checkstyle.regression.git;

import static com.github.checkstyle.regression.internal.TestUtils.assertUtilsClassHasPrivateConstructor;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.List;

import org.eclipse.jgit.lib.Repository;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.github.checkstyle.regression.data.GitChange;
import com.github.checkstyle.regression.internal.GitUtils;

public class DiffParserTest {
    private Repository repository;

    @Before
    public void setUp() throws Exception {
        repository = GitUtils.createNewRepository();
        final File helloWorld = GitUtils.addAnEmptyFileAndCommit(repository, "HelloWorld");
        GitUtils.createNewBranchAndCheckout(repository, "foo");
        Files.write(helloWorld.toPath(), "hello world!".getBytes(), StandardOpenOption.APPEND);
        GitUtils.addAllAndCommit(repository, "append text to HelloWorld");
    }

    @After
    public void tearDown() throws Exception {
        repository.close();
        GitUtils.clearTempRepositories();
    }

    @Test
    public void testIsProperUtilsClass() throws Exception {
        assertUtilsClassHasPrivateConstructor(DiffParser.class);
    }

    @Test
    public void testParse() throws Exception {
        final List<GitChange> changes = DiffParser.parse(
                repository.getDirectory().getParent(), "foo");
        assertEquals(1, changes.size());
        assertEquals("HelloWorld", changes.iterator().next().getPath());
    }

    @Test
    public void testParseDeleteChange() throws Exception {
        try (Repository repository = GitUtils.createNewRepository()) {
            GitUtils.addAnEmptyFileAndCommit(repository, "HelloWorld");
            GitUtils.addAnEmptyFileAndCommit(repository, "FileToDelete");
            GitUtils.createNewBranchAndCheckout(repository, "foo");
            GitUtils.removeFileAndCommit(repository, "FileToDelete");
            final List<GitChange> changes = DiffParser.parse(
                    repository.getDirectory().getParent(), "foo");
            assertEquals("There should be no changes detected", 0, changes.size());
        }
    }
}
