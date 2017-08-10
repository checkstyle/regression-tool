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

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.immutables.value.Value;

import com.github.checkstyle.regression.configuration.ConfigGenerator;
import com.github.checkstyle.regression.data.GitChange;
import com.github.checkstyle.regression.data.ModuleExtractInfo;
import com.github.checkstyle.regression.data.ModuleInfo;
import com.github.checkstyle.regression.extract.ExtractInfoProcessor;
import com.github.checkstyle.regression.git.DiffParser;
import com.github.checkstyle.regression.module.ModuleCollector;
import com.github.checkstyle.regression.module.ModuleUtils;
import com.github.checkstyle.regression.report.ReportGenerator;

/**
 * Utility class, contains main function and its auxiliary routines.
 * @author LuoLiangchen
 */
public final class Main {
    /** Option name of the local checkstyle repository path. */
    private static final String OPT_CHECKSTYLE_REPO_PATH = "checkstyleRepoPath";

    /** Option name of the PR branch name. */
    private static final String OPT_PATCH_BRANCH = "patchBranch";

    /** Option name of checkstyle-tester path. */
    private static final String OPT_CHECKSTYLE_TESTER_PATH = "checkstyleTesterPath";

    /** Option name of whether to stop after generating config. */
    private static final String OPT_STOP_AFTER_CONFIG_GENERATION = "stopAfterConfigGeneration";

    /** The option order to be shown in the help text. */
    private static final List<String> OPT_ORDER = Arrays.asList(
            OPT_CHECKSTYLE_REPO_PATH, OPT_PATCH_BRANCH,
            OPT_CHECKSTYLE_TESTER_PATH, OPT_STOP_AFTER_CONFIG_GENERATION);

    /** Prevents instantiation. */
    private Main() {
    }

    /**
     * Executes CLI command.
     * @param args the CLI arguments
     * @throws Exception execute failure
     */
    public static void main(String[] args) throws Exception {
        final Options options = createOptions();
        final CommandLineParser parser = new DefaultParser();
        final HelpFormatter formatter = new HelpFormatter();
        formatter.setOptionComparator((first, second) -> {
            final int indexFirst = OPT_ORDER.indexOf(first.getLongOpt());
            final int indexSecond = OPT_ORDER.indexOf(second.getLongOpt());
            return Integer.compare(indexFirst, indexSecond);
        });
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        }
        catch (ParseException ex) {
            System.err.println(ex.getMessage());
            formatter.printHelp("java -jar regression-tool.jar", options, true);
            System.exit(1);
        }

        final Arguments arguments = ImmutableArguments.builder()
                .checkstyleRepoPath(cmd.getOptionValue(OPT_CHECKSTYLE_REPO_PATH))
                .branch(cmd.getOptionValue(OPT_PATCH_BRANCH))
                .checkstyleTesterPath(
                        Optional.ofNullable(cmd.getOptionValue(OPT_CHECKSTYLE_TESTER_PATH)))
                .stopAfterConfigGeneration(cmd.hasOption(OPT_STOP_AFTER_CONFIG_GENERATION))
                .build();

        validateArguments(arguments);
        runRegression(arguments);
    }

    /**
     * Creates and initializes the {@link Options} instance.
     * @return the initialized options
     */
    private static Options createOptions() {
        final Options options = new Options();

        final Option repo = Option.builder("r")
                .longOpt(OPT_CHECKSTYLE_REPO_PATH)
                .required()
                .hasArg()
                .desc("the path of the checkstyle repository")
                .build();
        repo.setRequired(true);
        options.addOption(repo);

        final Option branch = Option.builder("p")
                .longOpt(OPT_PATCH_BRANCH)
                .required()
                .hasArg()
                .desc("the name of the PR branch")
                .build();
        options.addOption(branch);

        final Option tester = Option.builder("t")
                .longOpt(OPT_CHECKSTYLE_TESTER_PATH)
                .required(false)
                .hasArg()
                .desc("the path of the checkstyle-tester directory")
                .build();
        options.addOption(tester);

        final Option stopAfterConfigGeneration = Option.builder()
                .longOpt(OPT_STOP_AFTER_CONFIG_GENERATION)
                .required(false)
                .hasArg(false)
                .desc("indicates that regression tool would stop after generating config")
                .build();
        options.addOption(stopAfterConfigGeneration);

        return options;
    }

    /**
     * Validates the parsed CLI arguments.
     * @param args the parsed CLI arguments.
     * @throws IllegalArgumentException the arguments are invalid
     */
    private static void validateArguments(Arguments args) {
        if (!existAndIsDirectory(args.checkstyleRepoPath())) {
            throw new IllegalArgumentException(
                    "path of local git repo must exist and be a directory");
        }
        if (!args.stopAfterConfigGeneration()) {
            if (args.checkstyleTesterPath().isPresent()) {
                if (!existAndIsDirectory(args.checkstyleTesterPath().get())) {
                    throw new IllegalArgumentException(
                            "path of checkstyle tester must exist and be a directory");
                }
            }
            else {
                throw new IllegalArgumentException("missing checkstyleTesterPath, which is "
                        + "required if you are not using --stopAfterConfigGeneration mode");
            }
        }
    }

    /**
     * Runs the regression tool.
     * @param args the parsed CLI arguments.
     * @throws Exception execute failure
     */
    private static void runRegression(Arguments args) throws Exception {
        final File config = generateConfig(args);
        System.out.println("config generated at " + config.getAbsolutePath());
        if (!args.stopAfterConfigGeneration()) {
            final File report = ReportGenerator.generate(args.checkstyleTesterPath().get(),
                    args.checkstyleRepoPath(), args.branch(), config);
            System.out.println("report generated at " + report.getAbsolutePath());
        }
    }

    /**
     * Generates the config file.
     * @param args the parsed CLI arguments.
     * @return the generated config file
     * @throws Exception generation failure
     */
    private static File generateConfig(Arguments args)
            throws Exception {
        final List<GitChange> changes = DiffParser.parse(args.checkstyleRepoPath(), args.branch());
        final Map<String, ModuleExtractInfo> extractInfos = ExtractInfoProcessor
                .getModuleExtractInfos(args.checkstyleRepoPath(), args.branch());
        ModuleUtils.setNameToModuleExtractInfo(extractInfos);
        final List<ModuleInfo> moduleInfos = ModuleCollector.generate(changes);
        final DateFormat format = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
        final String configFileName =
                String.format("config-%s-%s.xml", args.branch(), format.format(new Date()));
        return ConfigGenerator.generateConfig(configFileName, moduleInfos);
    }

    /**
     * Checks whether the file in given path exists and is a directory.
     * @param path the path to check
     * @return true if file exists and is a directory
     */
    private static boolean existAndIsDirectory(String path) {
        final File file = new File(path);
        return !path.isEmpty() && file.exists() && file.isDirectory();
    }

    /** Represents the CLI arguments. */
    @Value.Immutable
    /* default */ interface Arguments {
        /**
         * The local checkstyle repository path.
         * @return the local checkstyle repository path
         */
        String checkstyleRepoPath();

        /**
         * The PR branch name.
         * @return the PR branch name
         */
        String branch();

        /**
         * Checkstyle-tester path.
         * @return Checkstyle-tester path
         */
        Optional<String> checkstyleTesterPath();

        /**
         * Whether to stop after generating config.
         * @return whether to stop after generating config
         */
        boolean stopAfterConfigGeneration();
    }
}
