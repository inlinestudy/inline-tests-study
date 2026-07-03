package org.genie;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.genie.util.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * Genie generator uses unit test generation tools to automatically generate unit tests for the extracted programs from
 * the previous extractor mojo.
 */
@Mojo(name = "generator", requiresDependencyResolution = ResolutionScope.TEST)
public class GeneratorMojo extends ExtractorMojo {
    /** Specifies a path to a evosuite jar to override the default evosuiute jar used. */
    @Parameter(property = "evosuiteJar")
    protected String evosuiteJar;

    /** Specifies a path to a randoop jar to override the default randoop jar used. */
    @Parameter(property = "randoopJar")
    protected String randoopJar;

    /** Decides whether to generate EvoSuite tests using multiple threads instead of in sequence. */
    @Parameter(property = "multiThread", defaultValue = "true")
    protected boolean multiThread;

    @Parameter(property = "evosuiteCustomTimeout", defaultValue = "" + EVOSUITE_TIME_LIMIT_S)
    protected int evosuiteCustomTimeout;

    /** A directory that keeps a copy of generated unit tests from test-generation tools. */
    protected String generatedTestsDirPath;

    /** A directory that contains all the jars that are extracted from the Genie jar. */
    protected String jarsDirPath;

    /** A directory that contains all the executables that are extracted from the Genie jar. */
    protected String executablesDirPath;

    // Evosuite
    /** A version of the classpath list that is used by EvoSuite, containing only the extracted class. */
    protected String evosuiteClasspathList;

    /** A directory containing logs generated when running evosuite. */
    protected String evosuiteLogDir;

    /** A directory that contains evosuite-genereated unit tests for the extracted program. */
    protected String evosuiteTestsDir;

    /** A directory that contains test execution reports that are generated from running evosuite-generated tests. */
    protected String evosuiteReportDir;

    /** The log that keeps the output of the evosuite generation process. */
    protected String evosuiteGenerationLog;

    // Randoop
    /** A version of the classpath list that is used by Randoop, which removes all inner classes. */
    protected String randoopClasspathList;

    /** A directory containing logs generated when running randoop. */
    protected String randoopLogDir;

    /** A directory that contains randoop-genereated unit tests. */
    protected String randoopTestsDir;

    /** The log that keeps the output of the randoop generation process. */
    protected String randoopGenerationLog;

    /**
     * Extracts jars and executables from Genie's src/main/resources to the `jars` and `executables` directory under
     * the artifact directory.
     */
    private void extractJarsAndExecutables() {
        this.jarsDirPath = genieDir + File.separator + JARS;
        this.executablesDirPath = genieDir + File.separator + EXECUTABLES;
        try {
            String thisJarAbsolutePath = new File(
                    this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI()).getAbsolutePath();
            List<String> command = new ArrayList<>(Arrays.asList("jar", "-xf", thisJarAbsolutePath, JARS));
            Utils.runSubprocess(command, new File(genieDir), new File(DEV_NULL), 0, false);
            command = new ArrayList<>(Arrays.asList("jar", "-xf", thisJarAbsolutePath, EXECUTABLES));
            Utils.runSubprocess(command, new File(genieDir), new File(DEV_NULL), 0, false);
            File majorExecutableFile = new File(executablesDirPath + File.separator + MAJOR_EXECUTABLE + "-"
                    + MAJOR_VERSION + File.separator + "bin" + File.separator + MAJOR_EXECUTABLE);
            majorExecutableFile.setExecutable(true); // Need to add execution permission on-the-fly.
        } catch (URISyntaxException ex) {
            getLog().error(LOG_LABEL + SPACE + "Error extracting jars.");
        }
    }

    /**
     * This method builds the classpath list file for EvoSuite by scanning for files that match the naming convention of
     * ClassName_lineNumber.
     */
    private void writeEvosuiteClasspathList() {
        getLog().info(LOG_LABEL + SPACE + "Writing evosuite classpath list.");

        try {
            PrintWriter writer = new PrintWriter(evosuiteClasspathList);
            BufferedReader reader;
            try {
                reader = new BufferedReader(new FileReader(classpathList));
                String classpath = reader.readLine();
                while (classpath != null) {
                    // Extracted classes use the format of ClassName_lineNumber.
                    // TODO: Caution: It's possible that source code also uses this format.
                    String[] classpathParts = classpath.split("\\.");
                    String className = classpathParts[classpathParts.length - 1];
                    String[] classNameParts = className.split("_");
                    String lineNumberStr = classNameParts[classNameParts.length - 1];
                    try {
                        // No exception means that the portion after _ is indeed a number.
                        Integer.parseInt(lineNumberStr);
                        writer.println(classpath);
                    } catch (NumberFormatException ex) {}
                    classpath = reader.readLine();
                }
                reader.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            writer.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /** This method builds the classpath list file for Randoop by removing all inner classes. */
    private void writeRandoopClasspathList() {
        getLog().info(LOG_LABEL + SPACE + "Writing randoop classpath list.");

        try {
            PrintWriter writer = new PrintWriter(randoopClasspathList);
            BufferedReader reader;
            try {
                reader = new BufferedReader(new FileReader(classpathList));
                String classpath = reader.readLine();
                while (classpath != null) {
                    // Exclude all inner classes and anonymous inner classes.
                    if (!classpath.contains("$")) {
                        writer.println(classpath);
                    }
                    classpath = reader.readLine();
                }
                reader.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            writer.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /** Generates evosuite tests for classes specified in the evosuite classpath list file. */
    private void generateEvosuiteTests() {
        getLog().info(LOG_LABEL + SPACE + "Generating evosuite tests.");

        this.evosuiteLogDir = logDir + File.separator + "evosuite";
        this.evosuiteTestsDir = basedir + File.separator + "evosuite-tests";
        this.evosuiteReportDir = basedir + File.separator + "evosuite-report";
        this.evosuiteGenerationLog = evosuiteLogDir + File.separator + "generation-log.txt";
        File evosuiteLogFile = new File(evosuiteGenerationLog);
        if (!evosuiteLogFile.exists()) {
            evosuiteLogFile.getParentFile().mkdirs();
        }
        if (new File(evosuiteTestsDir).exists() && new File(evosuiteReportDir).exists()) {
            getLog().info(LOG_LABEL + SPACE + "Evosuite tests found, using existing ones and skipping generation.");
            return;
        }

        try {
            List<String> classpaths = Files.readAllLines(Paths.get(evosuiteClasspathList));
            if (multiThread) {
                List<Thread> threads = new ArrayList<>();
                classpaths.stream().forEach(classpath -> {
                    Thread evosuiteGenerationThread = new Thread(() -> {
                        List<String> command = new ArrayList<>();
                        command.addAll(Arrays.asList("java", "-jar", evosuiteJar, "-DCP_file_path", deps, "-class",
                                classpath, "-seed", DEFAULT_SEED, "-Dsearch_budget=" + evosuiteCustomTimeout,
                                "-Duse_separate_classloader=false", "-Dminimize=false", "-Dassertion_strategy=all",
                                "-Dfilter_assertions=true", "-Dvirtual_fs=false", "-Dvirtual_net=false",
                                "-Dsandbox_mode=OFF", "-Dfilter_sandbox_tests=true", "-Dmax_loop_iterations=-1"));
                        Utils.runSubprocess(command, basedir, new File(evosuiteGenerationLog),
                                evosuiteCustomTimeout * 5, true);
                    });
                    threads.add(evosuiteGenerationThread);
                    evosuiteGenerationThread.start();
                });
                for (Thread thread : threads) {
                    thread.join();
                }
            } else {
                for (String classpath : classpaths) {
                    List<String> command = new ArrayList<>();
                    command.addAll(Arrays.asList("java", "-jar", this.evosuiteJar, "-DCP_file_path", deps, "-class",
                            classpath, "-seed", DEFAULT_SEED, "-Dsearch_budget=" + evosuiteCustomTimeout,
                            "-Duse_separate_classloader=false", "-Dminimize=false", "-Dassertion_strategy=all",
                            "-Dfilter_assertions=true", "-Dvirtual_fs=false", "-Dvirtual_net=false",
                            "-Dsandbox_mode=OFF", "-Dfilter_sandbox_tests=true", "-Dmax_loop_iterations=-1"));
                    Utils.runSubprocess(command, basedir, new File(this.evosuiteGenerationLog),
                            evosuiteCustomTimeout * 5, true);
                }
            }
        } catch (FileNotFoundException ex) {
            getLog().error(String.format("%s Error reading evosuite classpath list at %s, file not found.", LOG_LABEL,
                    evosuiteClasspathList));
            ex.printStackTrace();
        } catch (IOException | InterruptedException ex) {
            ex.printStackTrace();
        }

        // Exit early if no tests were generated.
        if (tool.equals(EVOSUITE) && !Files.exists(Paths.get(evosuiteTestsDir))) {
            getLog().error(String.format("%s %s No tests were generated, check %s.", LOG_LABEL, EARLY_EXIT_LABEL,
                    evosuiteLogDir));
            System.exit(1);
        }

        // For each generated test method, modify its timeout.
        try (Stream<Path> stream = Files.walk(Paths.get(evosuiteTestsDir))) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .forEach(path -> Utils.updateEvosuiteTestTimeout(path.toString(), EVOSUITE_SINGLE_TEST_TIMEOUT_MS));
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        // Copy the generated tests to the generated tests directory.
        try {
            Utils.copyRecursively(Paths.get(evosuiteTestsDir),
                    Paths.get(generatedTestsDirPath + File.separator + "evosuite-tests"));
            Utils.copyRecursively(Paths.get(evosuiteReportDir),
                    Paths.get(generatedTestsDirPath + File.separator + "evosuite-report"));
        } catch (IOException ex) {
            getLog().error(LOG_LABEL + SPACE + "Error copying evosuite tests.");
            ex.printStackTrace();
        }
    }

    /** Generates unit tests for the entire project using randoop. */
    private void generateRandoopTests() {
        getLog().info(LOG_LABEL + SPACE + "Generating randoop tests.");

        this.randoopLogDir = logDir + File.separator + "randoop";
        this.randoopTestsDir = basedir + File.separator + "randoop-tests";
        this.randoopGenerationLog = randoopLogDir + File.separator + "generation-log.txt";
        File randoopLogFile = new File(randoopGenerationLog);
        if (!randoopLogFile.exists()) {
            randoopLogFile.getParentFile().mkdirs();
        }
        File randoopTestsFile = new File(randoopTestsDir);
        if (!randoopTestsFile.exists()) {
            randoopTestsFile.mkdirs();
        } else {
            getLog().info(LOG_LABEL + SPACE + "Randoop tests found, using existing ones and skipping generation.");
            return;
        }

        long classpathCount = 0;
        try (Stream<String> stream = Files.lines(Paths.get(classpathList), StandardCharsets.UTF_8)) {
            classpathCount = stream.count();
        } catch (IOException ex) {
            getLog().error(LOG_LABEL + SPACE + "Error reading classpath list file.");
            ex.printStackTrace();
        }
        long timeout = RANDOOP_TIMEOUT_S;
        if (classpathCount * RANDOOP_SINGLE_CLASS_TIMEOUT_S < RANDOOP_TIMEOUT_S) {
            timeout = classpathCount * RANDOOP_SINGLE_CLASS_TIMEOUT_S;
        }

        List<String> command = new ArrayList<>();
        command.addAll(Arrays.asList("java", "-classpath", this.randoopJar + File.pathSeparator + getDepsFileContent(),
                "randoop.main.Main", "gentests", "--time-limit=" + timeout, "--usethreads=true",
                "--randomseed=" + DEFAULT_SEED, "--classlist=" + randoopClasspathList));
        if (printCommand) {
            getLog().debug("Running command: " + String.join(" ", command));
        }
        Utils.runSubprocess(command, randoopTestsFile, new File(this.randoopGenerationLog),
                timeout + RANDOOP_EXTRA_TIMEOUT_S, false);

        try {
            Utils.copyRecursively(Paths.get(randoopTestsDir),
                    Paths.get(generatedTestsDirPath + File.separator + "randoop-tests"));
        } catch (IOException ex) {
            getLog().error(LOG_LABEL + SPACE + "Error copying randoop tests.");
        }
    }

    /**
     * Executes the main functionality of Genie generator.
     * @throws MojoExecutionException
     * @throws MojoFailureException
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        super.execute();
        getLog().info(LOG_LABEL + SPACE + TOP_LEVEL_BAR + "Genie generator started." + TOP_LEVEL_BAR);
        this.generatedTestsDirPath = genieDir + File.separator + "generated-tests";
        this.evosuiteClasspathList = genieDir + File.separator + "evosuite-classpath-list.txt";
        this.randoopClasspathList = genieDir + File.separator + "randoop-classpath-list.txt";

        extractJarsAndExecutables();
        if (this.evosuiteJar == null) {
            this.evosuiteJar = jarsDirPath + File.separator + EVOSUITE_JAR;
        }
        if (this.randoopJar == null) {
            this.randoopJar = jarsDirPath + File.separator + RANDOOP_JAR;
        }

        File generatedTestsDir = new File(generatedTestsDirPath);
        if (!generatedTestsDir.exists()) {
            generatedTestsDir.mkdirs();
        }
        if (this.tool.equals(GENIE)) {
            writeEvosuiteClasspathList();
            writeRandoopClasspathList();
            generateEvosuiteTests();
            generateRandoopTests();
        } else if (this.tool.equals(EVOSUITE)) {
            writeEvosuiteClasspathList();
            generateEvosuiteTests();
        } else if (this.tool.equals(RANDOOP)) {
            writeRandoopClasspathList();
            generateRandoopTests();
        } else {
            getLog().error(String.format("%s Unrecognized tool \"%s\" during test generation.", LOG_LABEL, this.tool));
        }
    }
}
