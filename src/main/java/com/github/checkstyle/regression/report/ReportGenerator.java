////////////////////////////////////////////////////////////////////////////////
// checkstyle: Checks Java source code for adherence to a set of rules.
// Copyright (C) 2001-2022 the original author or authors.
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

package com.github.checkstyle.regression.report;

import java.io.File;
import java.io.IOException;

/**
 * Generates the regression diff report.
 * @author LuoLiangchen
 */
public final class ReportGenerator {
    /** Prevents instantiation. */
    private ReportGenerator() {
    }

    /**
     * Generates the regression diff report.
     * @param testerPath the path to the directory which holds 'diff.groovy'
     * @param repoPath   the path to the checkstyle repository
     * @param branch     the name of the PR branch
     * @param configFile the generated config file
     * @return the directory of the generated reports
     * @throws InterruptedException failure of running CLI
     * @throws IOException          failure of running CLI
     */
    public static File generate(
            String testerPath, String repoPath, String branch, File configFile)
            throws InterruptedException, IOException {
        final Process process = new ProcessBuilder()
                .directory(new File(testerPath))
                .command(
                        "groovy", "diff.groovy",
                        "-r", repoPath,
                        "-b", "master",
                        "-p", branch,
                        "-c", configFile.getAbsolutePath(),
                        "-l", "projects-to-test-on.properties"
                )
                .inheritIO()
                .start();
        final int code = process.waitFor();
        if (code != 0) {
            throw new IllegalStateException("an error occurred when running diff.groovy");
        }
        final File reportDir = new File(testerPath, "reports/diff");
        if (!reportDir.exists() || !reportDir.isDirectory()) {
            throw new IOException("report does not exist or it is not a directory");
        }
        return reportDir;
    }
}
