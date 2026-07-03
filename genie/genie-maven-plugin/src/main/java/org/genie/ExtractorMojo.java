package org.genie;

import com.github.javaparser.Range;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.genie.util.Utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Genie extractor takes target statements as input, outputs source files that each contains a class that contains a
 * method that contains an extracted target statement.
 */
@Mojo(name = "extractor", requiresDependencyResolution = ResolutionScope.TEST)
public class ExtractorMojo extends BaseMojo {
    /** Absolute path to the Genie metadata directory. */
    protected String genieDir;

    /** Absolute path to the dependency file that contains a path list to be used in classpath. */
    protected String deps;

    /** Absolute path to the file that keeps all r0 inline tests. */
    protected String r0TestPath;

    /** Absolute path to the file that keeps all r1 inline tests. */
    protected String r1TestPath;

    /** Absolute path of the directory that keeps all Genie logs. */
    protected String logDir;

    /**
     * A list of absolute paths pointing to the source code for program and for test, respectively.
     * e.g., src/main/java:src/test/java.
     */
    protected String appSrcPath;

    /** The directory that contains all the bytecode of the program except for tests, i.e., target/classes. */
    protected String classesDirectory;

    /**
     * Absolute path to the file that contains the list of all classes in fully qualified notation form.
     * Example content:
     * org.genie.BaseMojo
     * org.genie.ExtractorMojo
     * ...
     */
    protected String classpathList;

    /**
     * This map is an aggregated form of input pairs consisted of (file path, line number) of target statements.
     * For instance, a target file containing:
     * A.java,5
     * A.java,6
     * B.java,3
     * will eventually be turned into this map:
     * {A.java -> {5, 6}, B.java -> {3}}
     */
    protected Map<String, Set<Integer>> inputs = new HashMap<>();

    /** Path to the output file of Exli Target Statement Finder (TSF). */
    protected String tsfOutput;

    /**
     * Path to the secondary target statements file. This file is obtained through expanding inputs from discovery mode.
     * For instance, the primary target statements file may have these lines:
     * src/main/java/org/example/A.java
     * src/main/java/org/example/B.java
     * In secondary target statement files, the lines above may be expanded to:
     * src/main/java/org/example/A.java,5
     * src/main/java/org/example/A.java,6
     * src/main/java/org/example/B.java,17
     * This file will ALWAYS be generated regardless of what mode Genie is in.
     */
    private String secondaryTargetStatements;

    /** Check that the input values does conflict. */
    private void inputCheck() throws MojoExecutionException {
        if (filePath != null && lineNumbers != null && targetFile != null) {
            throw new MojoExecutionException(
                    "Conflicting input arguments. filePath, lineNumbers and targetFile cannot co-exist.");
        }
    }

    /**
     * Initializes member variables declared.
     * Initialization can only be done here because basedir is not initialized yet when initializing this class.
     * Otherwise, other methods using basedir will encounter NullPointerException.
     */
    protected void initialize() {
        getLog().info(LOG_LABEL + SPACE + "Initializing.");

        this.genieDir = basedir + File.separator + artifactsDir;
        this.deps = genieDir + File.separator + "deps.txt";
        this.r0TestPath = genieDir + File.separator + "r0-" + tool + "-inlinetests.txt";
        this.r1TestPath = genieDir + File.separator + "r1-" + tool + "-inlinetests.txt";
        this.logDir = genieDir + File.separator + "logs";
        this.appSrcPath = basedir + File.separator + SRC_MAIN_JAVA
                + File.pathSeparator + basedir + File.separator + SRC_TEST_JAVA;
        this.classesDirectory = basedir + File.separator + TARGET_CLASSES;
        this.classpathList = genieDir + File.separator + "classpath-list.txt";
        this.tsfOutput = genieDir + File.separator + "tsf-output.txt";
        new File(this.genieDir).mkdirs();
        new File(this.logDir).mkdirs();
        this.secondaryTargetStatements = genieDir + File.separator + "secondary-target-statements.csv";
    }

    /**
     * Processes a single line of TSF output. TSF output is in the format of:
     * ...;absolute path of an input file;line number;...;...;...
     * Based on this, a line -> file pair will be added to inputs
     * @param tsfLine a line in the TSF output file
     * @return a string in the format of filePath,lineNumber
     */
    private String processTsfResult(String tsfLine) {
        String file = tsfLine.split(";")[1].replace(basedir + File.separator, "");
        int line = Integer.parseInt(tsfLine.split(";")[2]);
        Set<Integer> newLines = inputs.getOrDefault(file, new HashSet<>());
        newLines.add(line);
        inputs.put(file, newLines);
        getLog().debug(LOG_LABEL + SPACE + "Populated input with " + file + " => " + line + ".");
        return file + "," + line;
    }

    /** Obtains the content of deps file. The result will be a string of a list of paths separated by colon .*/
    protected String getDepsFileContent() {
        String cpString = null;
        try {
            cpString = Files.readAllLines(Paths.get(deps)).get(0);
        } catch (IOException ex) {
            getLog().error(LOG_LABEL + SPACE + "Error reading deps file.");
            ex.printStackTrace();
        } catch (ArrayIndexOutOfBoundsException ex) {
            getLog().error(LOG_LABEL + SPACE + "Deps file is empty.");
            ex.printStackTrace();
        }
        return cpString;
    }

    /**
     * Parses the target file into inputs of the format (srcFile, lines) that will be used for this program. An example
     * of a target file can look like:
     * src/main/java/org/example/A.java,5
     * src/main/java/org/example/A.java,6
     * src/main/java/org/example/B.java,12
     * src/main/java/org/example/C.java
     * Note that if line number is not specified, like C.java above, discovery mode is triggered, which will
     * automatically find ALL target statements in that file. However, if B.java with no line number is added to the
     * previous example, discovery mode will not be triggered, because B.java already has a line that contains 12 as the
     * line number.
     * One intermediate output that will be generated is a file called secondary target statements. This file will
     * further expand target statement entries with no line numbers into 0 or more entries with both file path and line
     * number.
     */
    private void parseTargetFile() {
        try (PrintWriter writer = new PrintWriter(secondaryTargetStatements)) {
            try {
                List<String> lines = Files.readAllLines(Paths.get(targetFile));
                for (String line : lines) {
                    if (line.contains(",")) {
                        String file = line.split(",")[0];
                        String lineNumber = line.split(",")[1];
                        Set<Integer> newLines = inputs.getOrDefault(file, new HashSet<>());
                        newLines.add(Integer.parseInt(lineNumber));
                        inputs.put(file, newLines);
                        writer.println(line);
                    } else {
                        // No line number specified
                        // Still have to update, because in the end we will be looking for keys with empty value.
                        // If there are no keys, then the search can't be done.
                        inputs.put(line, inputs.getOrDefault(line, new HashSet<>()));
                    }
                }
                List<String> filesWithNoLines = inputs.entrySet().stream().filter(e -> e.getValue().isEmpty())
                        .map(Map.Entry::getKey).collect(Collectors.toList());
                if (!filesWithNoLines.isEmpty()) {
                    for (String file : filesWithNoLines) {
                        List<String> command = new ArrayList<>(Arrays.asList("java", "-classpath", getDepsFileContent(),
                                "org.raninline.App", "target-stmt", Paths.get(file).toAbsolutePath().toString(),
                                tsfOutput));
                        Utils.runSubprocess(command, basedir, new File(logDir + File.separator + "tsf-log.txt"), 0,
                                true);
                    }
                    List<String> tsfResults = Files.readAllLines(Paths.get(tsfOutput));
                    tsfResults.forEach(result -> writer.println(processTsfResult(result)));
                }
            } catch (FileNotFoundException ex) {
                getLog().error("Target file not found: " + this.targetFile);
                ex.printStackTrace();
            } catch (IOException ex) {
                getLog().error("IOException occurred when trying to parse target file: " + this.targetFile);
                ex.printStackTrace();
            }
        } catch (IOException ex) {
            getLog().error(LOG_LABEL + SPACE + "Error writing secondary target statements file.");
        }
    }

    /**
     * In order to stay compatible with multi-file mode, even if the input is consisted of only one input filePath, it
     * will still be stored in the map data structure for keeping inputs.
     */
    private void processInput() {
        Set<Integer> lineNumbers = new HashSet<>();
        for (String line : this.lineNumbers.split(",")) {
            lineNumbers.add(Integer.parseInt(line));
        }
        this.inputs.put(filePath, lineNumbers);
    }

    /**
     * Project discovery mode is defined as the mode where all three input parameters that determines the generation
     * process (targetFile, filePath, and lineNumbers) are null. In this case, Genie will search for all source files in
     * project's source code (test not included). The result of this method will be further processed, so that these
     * files will turn into specific locations of target statements.
     */
    private void detectProjectDiscoveryMode() {
        if (targetFile == null && filePath == null && lineNumbers == null) {
            // primary-target-statements.csv is essentially a list of all source files in the project.
            this.targetFile = genieDir + File.separator + "primary-target-statements.csv";
            try (PrintWriter writer = new PrintWriter(targetFile)) {
                try (Stream<Path> sourcePaths = Files.walk(Paths.get(basedir + File.separator + SRC_MAIN_JAVA))) {
                    sourcePaths.filter(Files::isRegularFile)
                            .filter(path -> !path.getFileName().toString().equals("package-info.java"))
                            // Caution! This must be relative path from project root,
                            // otherwise there will be downstream problems.
                            .forEach(path -> writer.println(path.toString().replace(basedir + File.separator, "")));
                } catch (IOException ex) {
                    getLog().error(LOG_LABEL + SPACE + "Failed to traverse Java source files in project.");
                    ex.printStackTrace();
                }
            } catch (IOException ex) {
                getLog().error(LOG_LABEL + SPACE + "Failed to write to primary target statements file.");
            }
        }
    }

    /**
     * This method updates the deps file by invoking `mvn dependency:build-classpath` and set the output file to be
     * deps.txt. Then, absolute paths pointing to source classes and test classes directories will be appended to the
     * end.
     */
    private void writeDepsFile() {
        long start = System.currentTimeMillis();
        getLog().info(LOG_LABEL + SPACE + "Writing deps file.");

        try {
            InvocationRequest request = new DefaultInvocationRequest();
            request.setPomFile(new File(basedir + File.separator + "pom.xml"));
            request.addArg("dependency:build-classpath");
            request.addArg("-Dmdep.outputFile=" + deps);
            request.setOutputHandler(output -> {});
            request.setErrorHandler(error -> {});
            request.setBatchMode(true);

            Invoker invoker = new DefaultInvoker();

            InvocationResult result = invoker.execute(request);
            result.getExitCode();
        } catch (MavenInvocationException ex) {
            ex.printStackTrace();
        }
        try {
            File file = new File(deps);
            FileWriter writer = new FileWriter(file, true);
            // Appending target/classes
            writer.write(File.pathSeparator + basedir + File.separator + "target" + File.separator + "classes");
            // Appending test classes directory, note that some projects actually don't have tests.
            if (this.testClassesDirectory.exists()) {
                writer.write(File.pathSeparator + this.testClassesDirectory);
            }
            writer.close();
        } catch (IOException ex) {
            getLog().error(LOG_LABEL + SPACE + "Failed to append to deps file: " + deps + ".");
            ex.printStackTrace();
        }
        long end = System.currentTimeMillis();
        if (timerOn) {
            getLog().info(TIMER_LABEL + SPACE + "Writing deps file costs " + (end - start) + "ms.");
        }
    }

    /**
     * Writes to classpath-list.txt.
     * It contains the fully qualified path of all Java classes in the source code. Test code not included.
     */
    protected void writeClasspathList() {
        long start = System.currentTimeMillis();
        getLog().info(LOG_LABEL + SPACE + "Writing classpath list.");

        try (PrintWriter writer = new PrintWriter(classpathList)) {
            try (Stream<Path> stream = Files.walk(Paths.get(classesDirectory))) {
                stream.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().endsWith(".class"))
                        // package-info.class needs to be filtered out.
                        .filter(path -> !path.getFileName().toString().contains("package-info"))
                        .forEach(path -> // This lambda function turns a path into Java fqn.
                                writer.println(path.toString()
                                        .split("target" + File.separator + "classes" + File.separator)[1]
                                        .replace(".class", "")
                                        .replace(File.separatorChar, '.')
                                )
                        );
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        long end = System.currentTimeMillis();
        if (timerOn) {
            getLog().info(LOG_LABEL + SPACE + "Writing classpath list costs " + (end - start) + "ms.");
        }
    }

    /**
     * This method compiles the Maven project by invoking `mvn test-compile` along with a list of "skip" arguments.
     * @return the exit code of the compilation command if no exception occurred, otherwise 1
     */
    protected int compileMavenProjectHelper() {
        getLog().info(LOG_LABEL + SPACE + "Compiling project.");

        try {
            InvocationRequest request = new DefaultInvocationRequest();
            request.setPomFile(new File(basedir + File.separator + "pom.xml"));
            request.addArg("test-compile");
            request.addArgs(SKIPS);
            request.setOutputHandler(output -> {});
            request.setErrorHandler(error -> {});
            request.setBatchMode(true);

            Invoker invoker = new DefaultInvoker();

            InvocationResult result = invoker.execute(request);
            return result.getExitCode();
        } catch (MavenInvocationException ex) {
            ex.printStackTrace();
            return 1;
        }
    }

    /** A wrapper method for compiling the project, the real work is done in compileMavenProjectHelper(). */
    protected void compileMavenProject() {
        long start = System.currentTimeMillis();
        if (compileMavenProjectHelper() != EXIT_NORMAL) {
            getLog().error(LOG_LABEL + SPACE + "Failed to compile project. This will affect processes down the line.");
        }
        long end = System.currentTimeMillis();
        if (timerOn) {
            getLog().info(TIMER_LABEL + SPACE + "Compilation costs " + (end - start) + "ms.");
        }
    }

    /**
     * Compiles multiple source files in src/main/java to target/classes.
     * @param sources a list of path to the source file that should be compiled
     * @return 0 return code of the subprocess
     */
    private int compileMultipleSources(List<String> sources) {
        List<String> command = new ArrayList<>(Arrays.asList("javac", "-cp",
                getDepsFileContent() + File.pathSeparator + basedir + File.separator + SRC_MAIN_JAVA, "-d",
                basedir + File.separator + TARGET_CLASSES));
        command.addAll(sources);
        return Utils.runSubprocess(command, basedir, new File(logDir + File.separator + "compile-extracted-log.txt"),
                0, false);
    }

    /**
     * Executes the main functionality of Genie extractor.
     * @throws MojoExecutionException
     * @throws MojoFailureException
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info(LOG_LABEL + SPACE + TOP_LEVEL_BAR + "Genie extractor started." + TOP_LEVEL_BAR);

        inputCheck();
        initialize();
        writeDepsFile();
        detectProjectDiscoveryMode();
        if (targetFile != null) {
            parseTargetFile();
        }
        if (filePath != null && lineNumbers != null) {
            processInput();
        }
        // Check if input map is empty, exit early.
        compileMavenProject(); // Need to compile the code before running extractor.
        // TODO: Repeated code in Instrumenter
        for (Iterator<Map.Entry<String, Set<Integer>>> it = inputs.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, Set<Integer>> entry = it.next();
            try {
                Integer[] inputLines = new Integer[entry.getValue().size()];
                entry.getValue().toArray(inputLines);
                Set<Integer> newFirstLines = new HashSet<>();
                Set<Integer> newLines = new HashSet<>();
                for (int i = 0; i < inputLines.length; i++) {
                    newFirstLines.add(Utils.findFirstLine(basedir + File.separator + entry.getKey(), inputLines[i]));
                    newLines.addAll(Utils.findAllLines(basedir + File.separator + entry.getKey(), inputLines[i]));
                }
                entry.setValue(newFirstLines);
                List<Integer> newLinesList = newLines.stream().sorted().collect(Collectors.toList());
                String[] lineNumbers = new String[newLinesList.size()];
                for (int i = 0; i < newLines.size(); i++) {
                    lineNumbers[i] = newLinesList.get(i).toString();
                }
                getLog().debug("Entry: " + entry);
                getLog().debug("Line numbers: " + Arrays.toString(lineNumbers));
                Parser.extractStmtIntoNewMethod(entry.getKey(), lineNumbers, deps, appSrcPath, null, r0TestPath,
                        r1TestPath, classesDirectory, "false");
            } catch (IOException ex) {
                getLog().error(LOG_LABEL + SPACE + "Failed to extract target statement from " + entry.getKey() + ":"
                        + entry.getValue() + ".");
                ex.printStackTrace();
            } catch (UnsupportedOperationException ex) {
                getLog().error(LOG_LABEL + SPACE + "Failed to extract target statement from " + entry.getKey() + ":"
                        + entry.getValue() + " due to unsupported operation.");
                ex.printStackTrace();
            } catch (UnsolvedSymbolException ex) {
                getLog().error(LOG_LABEL + SPACE + "Failed to extract target statement from " + entry.getKey() + ":"
                        + entry.getValue() + " due to unresolved symbol.");
                ex.printStackTrace();
            }
        }
        List<String> toCompile = new LinkedList<>();
        for (Map.Entry<String, Set<Integer>> entry : inputs.entrySet()) {
            getLog().info("Entry: " + entry);
            for (Integer lineNumber : entry.getValue()) {
                String extractedSource = entry.getKey().replaceAll("\\.java$", "_" + lineNumber + ".java");
                toCompile.add(extractedSource);
            }
        }
        getLog().info("toCompile: " + toCompile.toString());
        getLog().info(LOG_LABEL + SPACE + "Compiling extracted source files.");
        if (compileMultipleSources(toCompile) != EXIT_NORMAL) {
            getLog().error(String.format("%s Failed to compile extracted source files. Check log at %s.", LOG_LABEL,
                    logDir + File.separator + "compile-extracted-log.txt"));
        }
        writeClasspathList();
    }
}
