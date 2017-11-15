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

package com.github.checkstyle.regression.internal;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.RmCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import com.google.common.collect.Iterables;

/**
 * Contains utility methods for git component test.
 * @author LuoLiangchen
 */
public final class GitUtils {
    private static final Collection<Repository> TEMP_REPOSITORIES = new LinkedList<>();

    /** Prevents instantiation. */
    private GitUtils() {
    }

    public static Repository createNewRepository() throws IOException {
        final File repoDir = File.createTempFile("TestTempRepository", "");
        if (!repoDir.delete()) {
            throw new IOException("Could not delete temporary file " + repoDir);
        }
        final Repository repository = FileRepositoryBuilder.create(new File(repoDir, ".git"));
        repository.create();
        TEMP_REPOSITORIES.add(repository);
        return repository;
    }

    public static void createNewBranchAndCheckout(Repository repository, String branchName)
            throws GitAPIException {
        try (Git git = new Git(repository)) {
            if (git.branchList().call().stream()
                    .anyMatch(ref -> ref.getName().equals(Constants.R_HEADS + branchName))) {
                git.branchDelete().setBranchNames(branchName).setForce(true).call();
            }
            git.branchCreate().setName(branchName).call();
            git.checkout().setName(branchName).call();
        }
    }

    public static void checkoutBranch(Repository repository, String branchName)
            throws GitAPIException {
        try (Git git = new Git(repository)) {
            git.checkout().setName(branchName).call();
        }
    }

    public static File addAnEmptyFileAndCommit(Repository repository, String fileName)
            throws IOException, GitAPIException {
        try (Git git = new Git(repository)) {
            final File file = new File(repository.getDirectory().getParent(), fileName);
            if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
                throw new IOException("Could not create directory " + file.getParentFile());
            }
            if (!file.createNewFile()) {
                throw new IOException("Could not create file " + file);
            }
            git.add().addFilepattern(fileName).call();
            git.commit().setMessage("add " + fileName).call();
            return file;
        }
    }

    public static void addAllAndCommit(Repository repository, String message)
            throws GitAPIException {
        try (Git git = new Git(repository)) {
            // In JGit, the "add ." could not add the deleted files into staging area.
            // To obtain the same behavior as git CLI command "git add .", we have to
            // use RmCommand to handle deleted files.
            final Status status = git.status().call();
            if (!status.getMissing().isEmpty() || !status.getRemoved().isEmpty()) {
                final RmCommand rm = git.rm().setCached(true);
                Iterables.concat(status.getMissing(), status.getRemoved())
                        .forEach(rm::addFilepattern);
                rm.call();
            }
            git.add().addFilepattern(".").call();
            git.commit().setMessage(message).call();
        }
    }

    public static void removeFileAndCommit(Repository repository, String fileName)
            throws GitAPIException {
        try (Git git = new Git(repository)) {
            git.rm().addFilepattern(fileName).call();
            git.commit().setMessage("rm " + fileName).call();
        }
    }

    public static void clearTempRepositories() throws IOException {
        for (Repository repository : TEMP_REPOSITORIES) {
            FileUtils.deleteDirectory(repository.getDirectory().getParentFile());
        }
    }
}
