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

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

/**
 * Injects files to checkstyle repository, which would be invoked by maven command
 * to generate module extract info.
 * @author LuoLiangchen
 */
final class CheckstyleInjector implements Closeable {
    /** The path to checkstyle repository. */
    private final String repoPath;

    /** The name of PR branch. */
    private final String branch;

    /** The checkstyle repository. */
    private final Repository repository;

    /**
     * Creates a new instance of CheckstyleInjector.
     * @param repoPath the path to checkstyle repository
     * @param branch   the name of PR branch
     */
    CheckstyleInjector(String repoPath, String branch) {
        this.repoPath = repoPath;
        this.branch = branch;

        final File gitDir = new File(repoPath, ".git");
        final Repository repo;
        try {
            repo = new FileRepositoryBuilder().setGitDir(gitDir)
                    .readEnvironment().findGitDir().build();
        }
        catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
        repository = repo;
    }

    /**
     * Generates the module extract info file.
     * @return the module extract info file
     * @throws InjectException failure of generation
     */
    public File generateExtractInfoFile() throws InjectException {
        try {
            checkoutToPrBranch();
            copyInjectFilesToCheckstyleRepo();
            invokeMavenCommand();
            return new File(repoPath, "checkstyle_modules.json");
        }
        catch (IOException | GitAPIException ex) {
            throw new InjectException("unable to generate extract info file", ex);
        }
    }

    /**
     * Clears the injected files and the generated info file in checkstyle repository.
     * @throws InjectException failure of clearing
     */
    public void clearInjections() throws InjectException {
        final Git git = new Git(repository);

        try {
            git.clean().setCleanDirectories(true).call();
        }
        catch (GitAPIException ex) {
            throw new InjectException("unable to clear injections", ex);
        }
        finally {
            git.close();
        }
    }

    /** Closes the repository resource. */
    @Override
    public void close() {
        repository.close();
    }

    /**
     * Invokes Maven command to generate the extract info file in checkstyle repository.
     * @throws InjectException failure of invoking Maven
     */
    private void invokeMavenCommand() throws InjectException {
        final InvocationRequest request = new DefaultInvocationRequest();
        request.setPomFile(new File(repoPath, "pom.xml"));
        request.setGoals(Arrays.asList(
                "test", "-Dtest=ExtractInfoGeneratorTest#generateExtractInfoFile"));
        final Invoker invoker = new DefaultInvoker();
        try {
            final InvocationResult result = invoker.execute(request);
            if (result.getExitCode() != 0) {
                throw new InjectException("maven process exit with code: " + result.getExitCode());
            }
        }
        catch (MavenInvocationException ex) {
            throw new InjectException("maven invocation failed", ex);
        }
    }

    /**
     * Copies the injection files to checkstyle repository.
     * @throws IOException failure of copying
     */
    private void copyInjectFilesToCheckstyleRepo() throws IOException {
        final String path = "/com/github/checkstyle/regression/extract/";
        final File destDir = new File(repoPath, "src/test/java/com/puppycrawl/tools/checkstyle/");
        final String[] injections = {"ExtractInfoGeneratorTest.java", "JsonUtil.java"};
        for (String injection : injections) {
            final File destFile = new File(destDir, injection);
            FileUtils.copyInputStreamToFile(
                    getClass().getResourceAsStream(path + injection), destFile);
        }
    }

    /**
     * Checkouts to the PR branch in the given repository.
     * @throws GitAPIException JGit library exception
     */
    private void checkoutToPrBranch()
            throws GitAPIException {
        final Git git = new Git(repository);

        try {
            git.checkout().setName(branch).call();
        }
        finally {
            git.close();
        }
    }
}
