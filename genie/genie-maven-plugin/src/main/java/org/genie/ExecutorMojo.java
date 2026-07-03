package org.genie;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.genie.util.Utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The executor mojo executes the unit tests that are generated from the previous stage. In the process, instrumented
 * statements from ranininline/exli will compose r0 inline tests by collecting values seen during testing, as well as
 * collecting coverage information to reduce r0 tests into r1 tests. So, this mojo should be more adequately called as
 * the ExecutionReductionComposer, because it does three things.
 */
@Mojo(name = "executor", requiresDependencyResolution = ResolutionScope.TEST)
public class ExecutorMojo extends InstrumenterMojo {
    /** Path to a JUnit standalone jar which will replace the default JUnit standalone jar. */
    @Parameter(property = "junitStandaloneJar")
    protected String junitStandaloneJar;

    /** Path to a JaCoCo agent jar which will replace the default JaCoCo agent jar. */
    @Parameter(property = "jacocoAgentJar")
    protected String jacocoAgentJar;

    /** Path to EvoSuite-generated unit tests' compilation log. */
    private String evosuiteCompilationLog;

    /** Path to EvoSuite-generated unit tests' execution log. */
    private String evosuiteExecutionLog;

    /** Path to the file that is a list of paths to Randoop-generated unit test source code. */
    private String randoopSources;

    /** Path to Randoop-generated unit tests' compilation log. */
    private String randoopCompilationLog;

    private String randoopExecutionLog;

    /** Compiles EvoSuite tests by calling javac with all the source code. */
    private void compileEvosuiteTests() {
        getLog().info(LOG_LABEL + SPACE + "Compiling evosuite tests.");

        evosuiteCompilationLog = evosuiteLogDir + File.separator + "compilation-log.txt";

        Set<String> srcSet = new HashSet<>();
        try (Stream<Path> stream = Files.walk(Paths.get(evosuiteTestsDir))) {
            srcSet = stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .map(Path::toString)
                    .collect(Collectors.toSet());
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        List<String> command = new ArrayList<>();
        command.addAll(Arrays.asList("javac", "-cp", evosuiteJar + File.pathSeparator + junitStandaloneJar
                    + File.pathSeparator + getDepsFileContent()));
        command.addAll(srcSet);
        if (printCommand) {
            getLog().debug(LOG_LABEL + SPACE + "Running command: " + String.join(" ", command));
        }
        Utils.runSubprocess(command, basedir, new File(evosuiteCompilationLog), 0, false);
    }

    /** Executes EvoSuite tests by using JUnitCore with JaCoCo agent attached. */
    private void executeEvosuiteTests(Set<String> classes) {
        getLog().info(LOG_LABEL + SPACE + "Executing evosuite tests.");

        evosuiteExecutionLog = evosuiteLogDir + File.separator + "execution-log.txt";

        List<String> command = new ArrayList<>();
        command.addAll(Arrays.asList("java", "-javaagent:" + jacocoAgentJar, "-cp", jacocoAgentJar + File.pathSeparator
                        + evosuiteTestsDir + File.pathSeparator + evosuiteJar + File.pathSeparator
                        + getDepsFileContent(),
                "org.junit.runner.JUnitCore"));
        command.addAll(classes);
        if (printCommand) {
            getLog().debug(LOG_LABEL + SPACE + "Running command: " + String.join(" ", command));
        }
        Utils.runSubprocess(command, basedir, new File(evosuiteExecutionLog), 0, false);
    }

    /** Compiles and executes EvoSuite-generated tests by calling compileEvosuiteTests() and executeEvosuiteTests(). */
    private void compileAndExecuteEvosuiteTests() {
        getLog().info(LOG_LABEL + SPACE + "Compiling and executing evosuite tests.");

        // Caution! It is VERY important to compile the project again before executing generated unit tests. Otherwise,
        // the bytecode for the extracted class may be the version that is un-instrumented!
        compileMavenProject();
        writeClasspathList();
        Set<String> classes = null;
        try (Stream<Path> stream = Files.walk(Paths.get(evosuiteTestsDir))) {
            classes = stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith("Test.java"))
                    .map(path -> path.toString().replace(evosuiteTestsDir + File.separator, "").replace(".java", "")
                            .replace('/', '.'))
                    .collect(Collectors.toSet());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        compileEvosuiteTests();
        executeEvosuiteTests(classes);
    }

    /**
     * Compiles and executes Randoop-generated tests.
     * @return true if at least 1 test is executed, otherwise no test is executed
     */
    private boolean compileAndExecuteRandoopTests() throws MojoExecutionException {
        this.randoopSources = genieDir + File.separator + "randoop-sources.txt";
        this.randoopCompilationLog = randoopLogDir + File.separator + "compilation-log.txt";
        this.randoopExecutionLog = randoopLogDir + File.separator + "execution-log.txt";

        // Caution! It is VERY important to compile the project again before executing generated unit tests. Otherwise,
        // the bytecode for the extracted class may be the version that is un-instrumented!
        compileMavenProject();
        writeClasspathList();
        try (PrintWriter writer = new PrintWriter(randoopSources)) {
            try (Stream<Path> sources = Files.walk(Paths.get(randoopTestsDir))) {
                sources.filter(Files::isRegularFile).filter(path -> path.getFileName().toString().endsWith(".java"))
                        .map(Path::toString).forEach(writer::println);
            } catch (IOException ex) {
                getLog().error(LOG_LABEL + SPACE + "Failed to traverse " + randoopTestsDir + ".");
                ex.printStackTrace();
            }
        } catch (FileNotFoundException ex) {
            getLog().error(LOG_LABEL + SPACE + "Failed to write " + randoopSources + ".");
            ex.printStackTrace();
        }

        String sourcePath = basedir + File.separator + SRC_MAIN_JAVA;

        List<String> compilationCommand = new ArrayList<>(Arrays.asList("javac", "-classpath", getDepsFileContent(),
                "@" + randoopSources, "-sourcepath", sourcePath));
        if (printCommand) {
            getLog().debug(LOG_LABEL + SPACE + "Running command: " + String.join(" ", compilationCommand));
        }
        Utils.runSubprocess(compilationCommand, basedir, new File(randoopCompilationLog), 0, false);

        // Need to check if RegressionTest exist at all.
        File randoopRegressionTestFile = new File(randoopTestsDir + File.separator + "RegressionTest.class");
        boolean hasRegressionTest = randoopRegressionTestFile.exists() && !randoopRegressionTestFile.isDirectory();
        if (hasRegressionTest) {
            List<String> executionCommand = new ArrayList<>(Arrays.asList("java", "-javaagent:" + jacocoAgentJar,
                    "-classpath", jacocoAgentJar + File.pathSeparator + sourcePath + File.pathSeparator
                            + getDepsFileContent() + File.pathSeparator + randoopTestsDir,
                    "org.junit.runner.JUnitCore", "RegressionTest"));
            if (printCommand) {
                getLog().debug(LOG_LABEL + SPACE + "Running command: " + String.join(" ", executionCommand));
            }
            Utils.runSubprocess(executionCommand, basedir, new File(randoopExecutionLog), 0, false);
        }
        // Need to check if ErrorTest exist at all.
        File randoopErrorTestFile = new File(randoopTestsDir + File.separator + "ErrorTest.class");
        boolean hasErrorTest = randoopErrorTestFile.exists() && !randoopErrorTestFile.isDirectory();
        if (hasErrorTest) {
            List<String> executionCommand = new ArrayList<>(Arrays.asList("java", "-javaagent:" + jacocoAgentJar,
                    "-classpath", jacocoAgentJar + File.pathSeparator + sourcePath + File.pathSeparator
                            + getDepsFileContent() + File.pathSeparator + randoopTestsDir,
                    "org.junit.runner.JUnitCore", "ErrorTest"));
            if (printCommand) {
                getLog().debug(LOG_LABEL + SPACE + "Running command: " + String.join(" ", executionCommand));
            }
            Utils.runSubprocess(executionCommand, basedir, new File(randoopExecutionLog), 0, true);
        }
        return hasRegressionTest || hasErrorTest;
    }

    /**
     * This method replaces all instances of `checkEq(condExprRes,true/false)`
     * in generated inline tests with `checkTrue/checkFalse(group())`.
     */
    private void replaceWithGroup() {
        getLog().info(LOG_LABEL + SPACE + "Replacing condExprRes with group.");

        String[] pathStrings = new String[]{r0TestPath, r1TestPath};
        for (String pathString : pathStrings) {
            String tmp = pathString + ".tmp";
            try (Stream<String> inputStream = Files.lines(Paths.get(pathString), StandardCharsets.UTF_8);
                    PrintWriter output = new PrintWriter(tmp)) {
                inputStream.map(line -> line.replaceAll("checkEq\\s*\\(\\s*condExprRes\\s*,\\s*true\\s*\\)",
                                "checkTrue(group())"))
                        .map(line -> line.replaceAll("checkEq\\s*\\(\\s*condExprRes\\s*,\\s*false\\s*\\)",
                                "checkFalse(group())"))
                        .forEachOrdered(output::println);
                Files.move(Paths.get(tmp), Paths.get(pathString), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ex) {
                getLog().error(LOG_LABEL + SPACE + "Error reading inline tests from " + pathString + ".");
                ex.printStackTrace();
            }
        }
    }

    /**
     * Executes the main functionality of Genie executor.
     * @throws MojoExecutionException
     * @throws MojoFailureException
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        super.execute();
        getLog().info(LOG_LABEL + SPACE + TOP_LEVEL_BAR + "Genie executor started." + TOP_LEVEL_BAR);

        if (this.jacocoAgentJar == null) {
            this.jacocoAgentJar = jarsDirPath + File.separator + JACOCO_AGENT_JAR;
        }
        if (this.junitStandaloneJar == null) {
            this.junitStandaloneJar = jarsDirPath + File.separator + JUNIT_STANDALONE_JAR;
        }
        if (tool.equals(GENIE)) {
            compileAndExecuteEvosuiteTests();
            compileAndExecuteRandoopTests();
        } else if (tool.equals(EVOSUITE)) {
            compileAndExecuteEvosuiteTests();
        } else if (tool.equals(RANDOOP)) {
            compileAndExecuteRandoopTests();
        }
        if (!Files.exists(Paths.get(r0TestPath)) && !Files.exists(Paths.get(r1TestPath))) {
            getLog().error(String.format("%s %s No inline tests were generated.", LOG_LABEL, EARLY_EXIT_LABEL));
            System.exit(1);
        }
        replaceWithGroup();
    }
}
