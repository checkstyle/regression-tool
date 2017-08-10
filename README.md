# Regression-tool

Regression-tool is an automation tool to do regression testing based on proposed patch(Pull Request) 
of checkstyle project. The tool could generate configuration based on Git changes, which is used to 
generate diff report between the base and the patch branch.

## Setup

### Requirements

- Checkstyle repository which including both master and patch branch need to be cloned.
In most case, it should be your forked repository.
- [Contribution repository](https://github.com/checkstyle/contribution) need to be cloned.
- Regression-tool uses environment variable `M2_HOME` or system property `maven.home` to find the maven binary files.
You need to specify either `M2_HOME` or `maven.home` in advance. You could get the path of maven home by running `mvn -v`.

### Clone

Just clone this repository to your local.

```bash
$ git clone git@github.com:checkstyle/regression-tool.git
```

Or use HTTPS mode.
```bash
$ git clone https://github.com/checkstyle/regression-tool.git
```

### Generate Jar

Generate a `regression-tool-XX-all.jar`(XX means the version of regression-tool) in the `target` folder.

```bash
$ cd /path/to/regression-tool
$ mvn clean package -Passembly
```

## Usage

```bash
$ java -jar regression-tool-XX-all.jar -r <arg> -p <arg> [-t <arg>] [--stopAfterConfigGeneration]
```

### Arguments

#### checkstyleRepoPath (r)

The path of the checkstyle repository. **Required, one argument.**

#### patchBranch (p)

The name of the PR branch, which would be compared with the master. **Required, one argument.**

#### stopAfterConfigGeneration

Indicates that regression-tool would stop after generating config. By default, the tool would generate 
the diff report after generating config. **Optional, no argument.**

The report generation might requires network and be time-consuming. 
If you are not able to connect to network or just don't want to do the generation right now, 
you could use this mode.


#### checkstyleTesterPath (t)

The path of the checkstyle-tester directory. If you are **NOT** using `--stopAfterConfigGeneration` mode, this 
option is required, otherwise this could be absent. **Optional, one argument.**

### Example

[Here is an example in CI](./.ci/travis/checkstyle_regression_no_exception.sh).

Generate regression report for branch `issue1234`.

```bash
$ java -jar target/regression-tool-XX-all.jar -r /path/to/checkstyle/ -p issue1234 -t /path/to/contribution/checkstyle-tester/
```

You could also use long options.

```bash
$ java -jar target/regression-tool-XX-all.jar --checkstyleRepoPath /path/to/checkstyle/ --patchBranch issue1234 --checkstyleTesterPath /path/to/contribution/checkstyle-tester/
```

Generate only the config file and don't generate the report.

```bash
$ java -jar target/regression-tool-XX-all.jar -r /path/to/checkstyle/ -p issue1234 --stopAfterConfigGeneration
```

### Output

The config file would be generated in current working directory.

The report would be generated somewhere in TBD.
