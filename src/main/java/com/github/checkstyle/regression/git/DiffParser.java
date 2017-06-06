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
 * Git diff parser.
 * @author LuoLiangchen
 */
public class DiffParser {
    /** The prefix of branch head commit ref. */
    private static final String COMMIT_REF_PREFIX = "refs/heads/";

    /** The path of checkstyle repository. */
    private final String repositoryPath;

    /**
     * Creates a new {@code DiffParser} instance.
     * @param repositoryPath the path of checkstyle repository
     */
    public DiffParser(String repositoryPath) {
        this.repositoryPath = repositoryPath;
    }

    /**
     * Parses the diff between a given branch and the master.
     * @param branchName the name of the branch to be compared with master
     * @return a list of {@code GitChange} to represent the changes
     * @throws IOException JGit library exception
     * @throws GitAPIException JGit library exception
     */
    public List<GitChange> parse(String branchName) throws IOException, GitAPIException {
        final File gitDir = new File(repositoryPath + "/.git");
        try (Repository repository = new FileRepositoryBuilder().setGitDir(gitDir)
                .readEnvironment().findGitDir().build()) {
            try (Git git = new Git(repository)) {
                final AbstractTreeIterator featureTreeParser =
                        prepareTreeParser(repository, COMMIT_REF_PREFIX + branchName);
                final AbstractTreeIterator masterTreeParser =
                        prepareTreeParser(repository, COMMIT_REF_PREFIX + "master");
                return git.diff()
                        .setOldTree(masterTreeParser)
                        .setNewTree(featureTreeParser)
                        .call()
                        .stream()
                        .map(GitChange::new)
                        .collect(Collectors.toList());
            }
        }
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
        try (RevWalk walk = new RevWalk(repository)) {
            final RevCommit commit = walk.parseCommit(head.getObjectId());
            final RevTree tree = walk.parseTree(commit.getTree().getId());
            final CanonicalTreeParser treeParser = new CanonicalTreeParser();
            try (ObjectReader reader = repository.newObjectReader()) {
                treeParser.reset(reader, tree.getId());
            }
            walk.dispose();
            return treeParser;
        }
    }
}
