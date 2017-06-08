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

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;

/**
 * Parses git diff between feature branch and master for the further use.
 * @author LuoLiangchen
 */
public final class DiffParser {
    /** Prevents instantiation. */
    private DiffParser() {
    }

    /**
     * Parses the diff between a given branch and the master in the give repository path.
     * @param repositoryPath the path of checkstyle repository
     * @param branchName     the name of the branch to be compared with master
     * @return a list of {@link GitChange} to represent the changes
     * @throws IOException     JGit library exception
     * @throws GitAPIException JGit library exception
     */
    public static List<GitChange> parse(String repositoryPath, String branchName)
            throws IOException, GitAPIException {
        final File gitDir = new File(repositoryPath, ".git");
        final Repository repository = new FileRepositoryBuilder().setGitDir(gitDir)
                .readEnvironment().findGitDir().build();
        final Git git = new Git(repository);
        final AbstractTreeIterator featureTreeParser =
                prepareTreeParser(repository, Constants.R_HEADS + branchName);
        final AbstractTreeIterator masterTreeParser =
                prepareTreeParser(repository, Constants.R_HEADS + "master");
        final List<GitChange> returnValue = git.diff()
                .setOldTree(masterTreeParser)
                .setNewTree(featureTreeParser)
                .call()
                .stream()
                .map(DiffParser::convertDiffEntryToGitChange)
                .collect(Collectors.toList());
        git.close();
        repository.close();
        return returnValue;
    }

    /**
     * Creates a tree parser from a commit, to be used by diff command.
     * @param repository the repository to parse diff
     * @param ref        the name of the ref to the commit; e.g., "refs/heads/master"
     * @return the tree parser
     * @throws IOException JGit library exception
     */
    private static AbstractTreeIterator prepareTreeParser(Repository repository, String ref)
            throws IOException {
        final Ref head = repository.exactRef(ref);
        final RevWalk walk = new RevWalk(repository);
        final RevCommit commit = walk.parseCommit(head.getObjectId());
        final RevTree tree = walk.parseTree(commit.getTree().getId());
        final CanonicalTreeParser treeParser = new CanonicalTreeParser();
        final ObjectReader reader = repository.newObjectReader();
        treeParser.reset(reader, tree.getId());
        reader.close();
        walk.dispose();
        walk.close();
        return treeParser;
    }

    /**
     * Converts a {@link DiffEntry} to {@link GitChange} for the further use.
     * @param diffEntry the {@link DiffEntry} instance to be converted
     * @return the {@link GitChange} instance converted from the given {@link DiffEntry}
     */
    private static GitChange convertDiffEntryToGitChange(DiffEntry diffEntry) {
        return new GitChange(diffEntry.getNewPath());
    }
}
