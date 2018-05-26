////////////////////////////////////////////////////////////////////////////////
// checkstyle: Checks Java source code for adherence to a set of rules.
// Copyright (C) 2001-2018 the original author or authors.
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
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.SystemUtils;
import org.eclipse.jgit.lib.Repository;
import org.junit.After;
import org.junit.Test;

import com.github.checkstyle.regression.data.GitChange;
import com.github.checkstyle.regression.data.ImmutableGitChange;
import com.github.checkstyle.regression.internal.GitUtils;

public class DiffParserTest {
    @After
    public void tearDown() throws Exception {
        GitUtils.clearTempRepositories();
    }

    @Test
    public void testIsProperUtilsClass() throws Exception {
        assertUtilsClassHasPrivateConstructor(DiffParser.class);
    }

    @Test
    public void testParseAddChange() throws Exception {
        try (Repository repository = GitUtils.createNewRepository()) {
            GitUtils.addAnEmptyFileAndCommit(repository, "HelloWorld");
            GitUtils.createNewBranchAndCheckout(repository, "foo");
            GitUtils.addAnEmptyFileAndCommit(repository, "AddedFile");
            final List<GitChange> changes = DiffParser.parse(
                    repository.getDirectory().getParent(), "foo");
            assertEquals("There should be 1 change detected", 1, changes.size());
            final GitChange expected = ImmutableGitChange.builder()
                    .path("AddedFile")
                    .build();
            assertEquals("The change is not as expected", expected, changes.get(0));
        }
    }

    @Test
    public void testParseModifyChange() throws Exception {
        try (Repository repository = GitUtils.createNewRepository()) {
            final File helloWorld = GitUtils.addAnEmptyFileAndCommit(repository, "HelloWorld");
            GitUtils.createNewBranchAndCheckout(repository, "foo");
            Files.write(helloWorld.toPath(), "hello world!".getBytes(Charset.forName("UTF-8")),
                    StandardOpenOption.APPEND);
            GitUtils.addAllAndCommit(repository, "append text to HelloWorld");
            final List<GitChange> changes = DiffParser.parse(
                    repository.getDirectory().getParent(), "foo");
            assertEquals("There should be 1 change detected", 1, changes.size());
            final GitChange expected = ImmutableGitChange.builder()
                    .path("HelloWorld")
                    .addAddedLines(0)
                    .build();
            assertEquals("The change is not as expected", expected, changes.get(0));
        }
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

    @Test
    public void testParseRenameChange() throws Exception {
        try (Repository repository = GitUtils.createNewRepository()) {
            GitUtils.addAnEmptyFileAndCommit(repository, "HelloWorld");
            GitUtils.createNewBranchAndCheckout(repository, "foo");
            GitUtils.addAnEmptyFileAndCommit(repository, "HelloWorldFoo");
            GitUtils.removeFileAndCommit(repository, "HelloWorld");
            final List<GitChange> changes = DiffParser.parse(
                    repository.getDirectory().getParent(), "foo");
            assertEquals("There should be 1 change detected", 1, changes.size());
            final GitChange expected = ImmutableGitChange.builder()
                    .path("HelloWorldFoo")
                    .build();
            assertEquals("The change is not as expected", expected, changes.get(0));
        }
    }

    @Test
    public void testParseCopyChange() throws Exception {
        try (Repository repository = GitUtils.createNewRepository()) {
            GitUtils.addAnEmptyFileAndCommit(repository, "a.txt");
            GitUtils.createNewBranchAndCheckout(repository, "foo");
            GitUtils.addAnEmptyFileAndCommit(repository, "b.txt");
            GitUtils.addAnEmptyFileAndCommit(repository, "src/com/foo/c.java");
            GitUtils.addAnEmptyFileAndCommit(repository, "src/com/foo/d.java");
            GitUtils.removeFileAndCommit(repository, "a.txt");
            final List<GitChange> changes = DiffParser.parse(
                    repository.getDirectory().getParent(), "foo");
            assertEquals("There should be 3 change detected", 3, changes.size());
            final GitChange expected0 = ImmutableGitChange.builder()
                    .path("b.txt")
                    .build();
            assertEquals("The change is not as expected", expected0, changes.get(0));
            final GitChange expected1 = ImmutableGitChange.builder()
                    .path("src/com/foo/c.java")
                    .build();
            assertEquals("The change is not as expected", expected1, changes.get(1));
            final GitChange expected2 = ImmutableGitChange.builder()
                    .path("src/com/foo/d.java")
                    .build();
            assertEquals("The change is not as expected", expected2, changes.get(2));
        }
    }

    @Test
    public void testParsePrBranchBehindMaster() throws Exception {
        try (Repository repository = GitUtils.createNewRepository()) {
            GitUtils.addAnEmptyFileAndCommit(repository, "HelloWorld");
            GitUtils.createNewBranchAndCheckout(repository, "foo");
            GitUtils.addAnEmptyFileAndCommit(repository, "ChangeInFoo");
            GitUtils.checkoutBranch(repository, "master");
            GitUtils.addAnEmptyFileAndCommit(repository, "ChangeInMaster");
            final List<GitChange> changes = DiffParser.parse(
                    repository.getDirectory().getParent(), "foo");
            assertEquals("There should be 1 change detected", 1, changes.size());
            final GitChange expected = ImmutableGitChange.builder()
                    .path("ChangeInFoo")
                    .build();
            assertEquals("The only change path should be 'ChangeInFoo'",
                    expected, changes.get(0));
        }
    }

    @Test
    public void testParsePrBranchWithMultipleCommits() throws Exception {
        try (Repository repository = GitUtils.createNewRepository()) {
            GitUtils.addAnEmptyFileAndCommit(repository, "HelloWorld");
            GitUtils.createNewBranchAndCheckout(repository, "foo");
            GitUtils.addAnEmptyFileAndCommit(repository, "AddInCommit1");
            GitUtils.addAnEmptyFileAndCommit(repository, "AddInCommit2");
            final List<GitChange> changes = DiffParser.parse(
                    repository.getDirectory().getParent(), "foo");
            assertEquals("There should be 2 change detected", 2, changes.size());
            final GitChange expected0 = ImmutableGitChange.builder()
                    .path("AddInCommit1")
                    .build();
            assertEquals("The change is not as expected", expected0, changes.get(0));
            final GitChange expected1 = ImmutableGitChange.builder()
                    .path("AddInCommit2")
                    .build();
            assertEquals("The change is not as expected", expected1, changes.get(1));
        }
    }

    @Test
    public void testParseFilePermissionChange() throws Exception {
        // Skip this UT on Windows
        if (!SystemUtils.IS_OS_WINDOWS) {
            try (Repository repository = GitUtils.createNewRepository()) {
                final File file = GitUtils.addAnEmptyFileAndCommit(repository, "HelloWorld");
                GitUtils.createNewBranchAndCheckout(repository, "foo");
                final Set<PosixFilePermission> permissions =
                        Files.getPosixFilePermissions(file.toPath());
                permissions.add(PosixFilePermission.OWNER_EXECUTE);
                Files.setPosixFilePermissions(file.toPath(), permissions);
                GitUtils.addAllAndCommit(repository, "Change file mode");
                final List<GitChange> changes = DiffParser.parse(
                        repository.getDirectory().getParent(), "foo");
                assertEquals("There should be 1 change detected", 1, changes.size());
                final GitChange expected = ImmutableGitChange.builder()
                        .path("HelloWorld")
                        .build();
                assertEquals("The change is not as expected", expected, changes.get(0));
            }
        }
    }

    @Test
    public void testParseLineChangesAddLine() throws Exception {
        try (Repository repository = GitUtils.createNewRepository()) {
            final File helloWorld = GitUtils.addAnEmptyFileAndCommit(repository, "HelloWorld");
            Files.write(helloWorld.toPath(), "line 0\n"
                    .getBytes(Charset.forName("UTF-8")), StandardOpenOption.APPEND);
            GitUtils.addAllAndCommit(repository, "add original line");
            GitUtils.createNewBranchAndCheckout(repository, "foo");
            Files.write(helloWorld.toPath(), "line 1 added\nline 2 added\n"
                    .getBytes(Charset.forName("UTF-8")), StandardOpenOption.APPEND);
            GitUtils.addAllAndCommit(repository, "add line 1, 2");
            final List<GitChange> changes = DiffParser.parse(
                    repository.getDirectory().getParent(), "foo");
            assertEquals("There should be 1 change detected", 1, changes.size());
            final GitChange expected = ImmutableGitChange.builder()
                    .path("HelloWorld")
                    .addAddedLines(1, 2)
                    .build();
            assertEquals("The change is not as expected", expected, changes.get(0));
        }
    }

    @Test
    public void testParseLineChangesRemoveLine() throws Exception {
        try (Repository repository = GitUtils.createNewRepository()) {
            final File helloWorld = GitUtils.addAnEmptyFileAndCommit(repository, "HelloWorld");
            Files.write(helloWorld.toPath(), "line 0\nline 1 to be removed\nline 2 to be removed\n"
                    .getBytes(Charset.forName("UTF-8")), StandardOpenOption.APPEND);
            GitUtils.addAllAndCommit(repository, "add original three lines");
            GitUtils.createNewBranchAndCheckout(repository, "foo");
            Files.write(helloWorld.toPath(), "line 0\n"
                    .getBytes(Charset.forName("UTF-8")), StandardOpenOption.TRUNCATE_EXISTING);
            GitUtils.addAllAndCommit(repository, "remove two lines");
            final List<GitChange> changes = DiffParser.parse(
                    repository.getDirectory().getParent(), "foo");
            assertEquals("There should be 1 change detected", 1, changes.size());
            final GitChange expected = ImmutableGitChange.builder()
                    .path("HelloWorld")
                    .addDeletedLines(1, 2)
                    .build();
            assertEquals("The change is not as expected", expected, changes.get(0));
        }
    }

    @Test
    public void testParseLineChangesModifyLine() throws Exception {
        try (Repository repository = GitUtils.createNewRepository()) {
            final File helloWorld = GitUtils.addAnEmptyFileAndCommit(repository, "HelloWorld");
            Files.write(helloWorld.toPath(), "line 0\nline 1\nline 2\n"
                    .getBytes(Charset.forName("UTF-8")), StandardOpenOption.APPEND);
            GitUtils.addAllAndCommit(repository, "add original three lines");
            GitUtils.createNewBranchAndCheckout(repository, "foo");
            Files.write(helloWorld.toPath(), "line 0\nline 1 changed\nline 2 changed\n"
                    .getBytes(Charset.forName("UTF-8")), StandardOpenOption.TRUNCATE_EXISTING);
            GitUtils.addAllAndCommit(repository, "modify two lines");
            final List<GitChange> changes = DiffParser.parse(
                    repository.getDirectory().getParent(), "foo");
            assertEquals("There should be 1 change detected", 1, changes.size());
            final GitChange expected = ImmutableGitChange.builder()
                    .path("HelloWorld")
                    .addAddedLines(1, 2)
                    .addDeletedLines(1, 2)
                    .build();
            assertEquals("The change is not as expected", expected, changes.get(0));
        }
    }
}
