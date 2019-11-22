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

package com.github.checkstyle.regression.git;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang.math.IntRange;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import com.github.checkstyle.regression.data.GitChange;
import com.github.checkstyle.regression.data.ImmutableGitChange;

/**
 * Parses git diff between PR branch and master for the further use.
 * @author LuoLiangchen
 */
// -@cs[ClassDataAbstractionCoupling] We have to import many classes from JGit
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
        final List<GitChange> returnValue = new LinkedList<>();
        final File gitDir = new File(repositoryPath, ".git");
        final Repository repository = new FileRepositoryBuilder().setGitDir(gitDir)
                .readEnvironment().findGitDir().build();

        try {
            final TreeParserPair pair = getTreeParserPair(repository, branchName);
            final Git git = new Git(repository);
            final DiffFormatter formatter = new DiffFormatter(DisabledOutputStream.INSTANCE);
            formatter.setRepository(repository);

            try {
                final List<DiffEntry> diffs = git.diff()
                        .setOldTree(pair.commonAncestorTreeParser)
                        .setNewTree(pair.prTreeParser)
                        .call()
                        .stream()
                        .filter(entry -> entry.getChangeType() != DiffEntry.ChangeType.DELETE)
                        .collect(Collectors.toList());
                for (DiffEntry diff : diffs) {
                    returnValue.add(convertDiffEntryToGitChange(diff, formatter));
                }
            }
            finally {
                git.close();
            }
        }
        finally {
            repository.close();
        }

        return returnValue;
    }

    /**
     * Gets a TreeParserPair for the further use.
     * @param repository the repository to parse
     * @param branch     the name of the PR branch
     * @return the TreeParserPair prepared for the further use
     * @throws IOException JGit library exception
     */
    private static TreeParserPair getTreeParserPair(Repository repository, String branch)
            throws IOException {
        final TreeParserPair returnValue;
        final RevWalk walk = new RevWalk(repository);

        try {
            final RevCommit prCommit = walk.parseCommit(
                    repository.exactRef(Constants.R_HEADS + branch).getObjectId());
            final RevCommit masterCommit = walk.parseCommit(
                    repository.exactRef(Constants.R_HEADS + "master").getObjectId());
            final RevCommit commonAncestorCommit = getMergeBaseCommit(walk, prCommit, masterCommit);
            walk.dispose();

            returnValue = new TreeParserPair(prepareTreeParser(walk, prCommit),
                    prepareTreeParser(walk, commonAncestorCommit));
        }
        finally {
            walk.close();
        }

        return returnValue;
    }

    /**
     * Gets the merge-base of two commits.
     * A merge-base is a best common ancestor between two commits. One common ancestor is
     * better than another common ancestor if the latter is an ancestor of the former.
     * A common ancestor that does not have any better common ancestor is a best common ancestor.
     * @param walk    the {@link RevWalk} for computing merge bases
     * @param commitA the first commit to start the walk with
     * @param commitB the second commit to start the walk with
     * @return the merge-base of two commits
     * @throws IOException JGit library exception
     */
    private static RevCommit getMergeBaseCommit(
            RevWalk walk, RevCommit commitA, RevCommit commitB) throws IOException {
        walk.reset();
        walk.setRevFilter(RevFilter.MERGE_BASE);
        walk.markStart(commitA);
        walk.markStart(commitB);
        return walk.next();
    }

    /**
     * Creates a tree parser from a commit, to be used by diff command.
     * @param walk   the {@link RevWalk} to parse the tree
     * @param commit the commit to create tree parser from
     * @return the tree parser
     * @throws IOException JGit library exception
     */
    private static AbstractTreeIterator prepareTreeParser(RevWalk walk, RevCommit commit)
            throws IOException {
        final RevTree tree = walk.parseTree(commit.getTree().getId());
        final CanonicalTreeParser returnValue;
        returnValue = new CanonicalTreeParser();
        returnValue.reset(walk.getObjectReader(), tree.getId());
        return returnValue;
    }

    /**
     * Converts a {@link DiffEntry} to {@link GitChange} for the further use.
     * @param diffEntry the {@link DiffEntry} instance to be converted
     * @param formatter the diff formatter to provide the line changes information
     * @return the {@link GitChange} instance converted from the given {@link DiffEntry}
     * @throws IOException JGit library exception
     */
    private static GitChange convertDiffEntryToGitChange(
            DiffEntry diffEntry, DiffFormatter formatter) throws IOException {
        final List<Integer> addedLines = formatter.toFileHeader(diffEntry).toEditList().stream()
                .filter(edit -> edit.getBeginB() < edit.getEndB())
                .flatMapToInt(edit -> {
                    return Arrays.stream(
                            new IntRange(edit.getBeginB(), edit.getEndB() - 1).toArray());
                })
                .boxed()
                .collect(Collectors.toList());
        final List<Integer> deletedLines = formatter.toFileHeader(diffEntry).toEditList().stream()
                .filter(edit -> edit.getBeginA() < edit.getEndA())
                .flatMapToInt(edit -> {
                    return Arrays.stream(
                            new IntRange(edit.getBeginA(), edit.getEndA() - 1).toArray());
                })
                .boxed()
                .collect(Collectors.toList());
        return ImmutableGitChange.builder()
                .path(diffEntry.getNewPath())
                .addAllAddedLines(addedLines)
                .addAllDeletedLines(deletedLines)
                .build();
    }

    /** A pair of tree parsers: PR branch tree parser and common ancestor tree parser. */
    private static class TreeParserPair {
        /** The tree parser of the head commit of PR branch. */
        private final AbstractTreeIterator prTreeParser;

        /** The tree parser of the merge-base commit between PR branch and master. */
        private final AbstractTreeIterator commonAncestorTreeParser;

        /**
         * Creates a new TreeParserPair instance.
         * @param prTreeParser             the tree parser of the head commit of PR branch
         * @param commonAncestorTreeParser the tree parser of the merge-base commit between
         *                                 PR branch and master
         */
        TreeParserPair(
                AbstractTreeIterator prTreeParser, AbstractTreeIterator commonAncestorTreeParser) {
            this.prTreeParser = prTreeParser;
            this.commonAncestorTreeParser = commonAncestorTreeParser;
        }
    }
}
