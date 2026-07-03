package org.genie;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.genie.datastructure.Mutant;
import org.genie.util.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Genie mutator does the following two things:
 * 1. Generates mutants for files containing target statements.
 * 2. Parsed inline tests are run against these mutants.
 * It's the job of the reducer to analyze the result of these runs.
 */
@Mojo(name = "mutator", requiresDependencyResolution = ResolutionScope.TEST)
public class MutatorMojo extends TransplanterMojo {
    /**
     * Specifies which mutation tool to use for generating mutants.
     * As of now, only supports "universalmutator" or "major". Uses universalmutator by default.
     */
    @Parameter(property = "mutator", defaultValue = "universalmutator")
    private String mutator;

    /** Path to the major executable file. */
    @Parameter(property = "majorExecutable")
    private String majorExecutable;

    /**
     * A file that is used to store line numbers that universalmutator mutates upon.
     * Note that all runs of universalmutator share this file.
     */
    protected String lineNumbersList;

    /** A directory that stores the source files of all mutants generated. */
    protected String mutantsDir;

    /** Log that captures all output from executing universalmutator. */
    private String umLog;

    /** Log that captures all output from executing major. */
    private String majorLog;

    /** A directory that keeps the results of executing inline tests against all the mutants. */
    protected String mutatedRunsDir;

    /** A directory that keeps the results of executing R0 inline tests against all the mutants. */
    protected String r0MutatedRunsDir;

    /** A directory that keeps the results of executing R1 inline tests against all the mutants. */
    protected String r1MutatedRunsDir;

    /** A list of mutants that are generated in Genie mutator. */
    protected List<Mutant> mutants = new ArrayList<>();

    /**
     * Prepares a line numbers file for the given input file. The input string is the path to the original source code
     * file that contains the target statements.
     * @param file the path to the unmodified source code file that contains target statements
     */
    private void prepareLineNumbersFile(String file) {
        lineNumbersList = genieDir + File.separator + "line-numbers.txt";
        try (PrintWriter pw = new PrintWriter(lineNumbersList)) {
            Set<Integer> lines = new HashSet<>();
            for (Integer line : inputs.get(file)) {
                Utils.findAllLines(file, line).forEach(lines::add);
            }
            for (Integer line : lines) {
                pw.println(line);
            }
        } catch (IOException ex) {
            getLog().error(LOG_LABEL + SPACE + "Error writing to line numbers file.");
            ex.printStackTrace();
        }
    }

    /**
     * Executes mutator for a file once at a time.
     * @param file the path to the unmodified source code file that contains target statements
     */
    private void mutate(String file) throws MojoExecutionException {
        getLog().info(LOG_LABEL + SPACE + "Generating mutants with " + mutator + ".");

        new File(mutantsDir).mkdirs();
        if (mutator.equals("universalmutator")) {
            umLog = logDir + File.separator + "universalmutator-log.txt";
            // We only have 1 line numbers list path to use, so all the files have to go in order.
            List<String> command = new ArrayList<>(Arrays.asList("mutate", file, "--noCheck",
                    "--mutantDir", mutantsDir, "--lines", lineNumbersList));
            Utils.runSubprocess(command, basedir, new File(umLog), 0, true);
        } else if (mutator.equals("major")) {
            majorLog = logDir + File.separator + "major-log.txt";
            List<String> command = new ArrayList<>(Arrays.asList(majorExecutable, "-cp", getDepsFileContent(),
                    "--export", "export.mutants", Paths.get(file).toAbsolutePath().toString()));
            Utils.runSubprocess(command, new File(mutantsDir), new File(majorLog), 0, true);
        } else {
            throw new MojoExecutionException("Unrecognized mutator, terminating.");
        }
    }

    /**
     * Because major stores mutants in a structured rather than a flattened manner, results from major have tot be
     * flattened to match the format of universalmutator for further downstream processing.
     * @param filePath file path of the original source code with target statements
     */
    private void flattenMajor(String filePath) {
        String nameWithoutExtension = Paths.get(filePath).getFileName().toString().split("\\.")[0];
        // Move out all the java source files and rename them.
        AtomicInteger mutantSerial = new AtomicInteger();
        try (Stream<Path> files = Files.walk(Paths.get(mutantsDir))) {
            files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .forEach(path -> {
                        try {
                            Files.move(path, Paths.get(mutantsDir + File.separator + nameWithoutExtension + ".mutant."
                                    + mutantSerial.getAndIncrement() + ".java"), StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException ex) {
                            getLog().error(LOG_LABEL + SPACE + "Failed to move " + path.getFileName().toString() + ".");
                            ex.printStackTrace();
                        }
                    });
        } catch (IOException ex) {
            getLog().error(LOG_LABEL + SPACE + "Failed to traverse mutants directory when flattening major.");
            ex.printStackTrace();
        }
        // Then recursively remove files and directories generated by major.
        try {
            Files.walk(Paths.get(mutantsDir + File.separator + "mutants"))
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException ex) {
            getLog().error(LOG_LABEL + SPACE + "Failed to delete major-generated directories when flattening major.");
            ex.printStackTrace();
        }
        // Finally remove all the log files.
        try (Stream<Path> files = Files.walk(Paths.get(mutantsDir))) {
            files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".log"))
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException ex) {
            getLog().error(LOG_LABEL + SPACE + "Failed to delete major-generated logs when flattening major.");
            ex.printStackTrace();
        }
    }

    /**
     * Registers a mutant, relative to an original code, to an internal data structure.
     * @param originalPath path string to the original code
     * @param mutantPath path to the mutant
     * @return whether the mutant should be removed
     */
    private boolean registerMutant(String originalPath, Path mutantPath) {
        int mutantSerial = Integer.parseInt(mutantPath.toString().replace(mutantsDir, "").split("\\.")[2]);
        try (BufferedReader originalReader = Files.newBufferedReader(Paths.get(originalPath));
             BufferedReader mutatedReader = Files.newBufferedReader(mutantPath)) {
            int lineNumber = 1;
            String originalLine, mutatedLine;
            while ((originalLine = originalReader.readLine()) != null) {
                mutatedLine = mutatedReader.readLine();
                // It's necessary that the line is indeed a line we care about.
                if (!originalLine.equals(mutatedLine) && inputs.get(originalPath).stream().map(rawLine -> {
                    try {
                        return Utils.findAllLines(originalPath, rawLine);
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }).flatMap(Set::stream).collect(Collectors.toSet()).contains(lineNumber)) {
                    if (mutatedLine.trim().matches("/\\*[\\s\\S]*?\\*/")) {
                        return true; // Do not register any mutants that are obtained through commenting out things.
                    }
                    Mutant mutant = new Mutant(mutantSerial, originalPath, mutantPath.toString(),
                            lineNumber, originalLine, mutatedLine);
                    mutants.add(mutant);
                    getLog().info(LOG_LABEL + SPACE + "Mutant added: " + mutant);
                    return false;
                }
                lineNumber++;
            }
        } catch (IOException ex) {
            getLog().error(LOG_LABEL + SPACE + "Error registering mutant " + mutantSerial + ".");
            ex.printStackTrace();
        }
        // If for all lines, either line doesn't change, or is not the line we care about, remove it.
        return true;
    }

    /**
     * Remove mutants that are obtained through either adding lines or deleting lines. This method also calls
     * registerMutant() in the middle to register information regarding a mutant into a data structure. During
     * registration, mutants that are obtained through commenting out the target statement is also removed.
     * @param file the original source code file to remove mutants for
     */
    private void removeIneffectiveMutants(String file) {
        int lineCount = 0;
        try (Stream<String> fileStream = Files.lines(Paths.get(file))) {
            lineCount = (int) fileStream.count();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        try (Stream<Path> mutants = Files.walk(Paths.get(mutantsDir))) {
            int finalLineCount = lineCount;
            mutants.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString() // toString() is necessary here.
                            // Make sure that only ineffective mutants relevant to the input file are chosen.
                            // TODO: Currently this fits the naming convention of universal mutator mutants.
                            /*
                             * Caution! the ".mutant." part is necessary, because some mutants would be deleted if
                             * there are some files with names that are prefix of others. For instance, for class App
                             * and class A, when processing mutants for A, mutated files for App will be removed because
                             * they are compared against the line count for A.
                             */
                            .startsWith(Paths.get(file).getFileName().toString().split("\\.")[0] + ".mutant."))
                    .filter(path -> {
                        boolean shouldRemove;
                        try {
                            // Select all mutant sources that don't have the same number of lines as the original source.
                            shouldRemove = finalLineCount != Files.lines(path).count();
                        } catch (IOException ex) {
                            ex.printStackTrace();
                            return true;
                        }
                        if (!shouldRemove) {
                            shouldRemove = registerMutant(file, path);
                        }
                        return shouldRemove;
                    }).forEach(path -> {
                        try {
                            Files.delete(path); // Remove those sources that match the previous condition.
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    });
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Compiles a single source file in src/main/java to target/classes.
     * @param source path to the source file that should be compiled
     * @return 0 return code of the subprocess
     */
    protected int compileSingleSource(String source) {
        List<String> command = new ArrayList<>(Arrays.asList("javac", "-cp",
                getDepsFileContent() + File.pathSeparator + basedir + File.separator + SRC_MAIN_JAVA, "-d",
                basedir + File.separator + TARGET_CLASSES, source));
        return Utils.runSubprocess(command, basedir, new File(DEV_NULL), 0, false);
    }

    private void compileMutants() {
        Set<Mutant> failedToCompile = new HashSet<>();
        for (Mutant mutant : mutants) {
            backUpOrRecover(mutant.getOriginalFilePath(), false);
            try {
                Files.copy(Paths.get(mutant.getMutantFilePath()), Paths.get(mutant.getOriginalFilePath()),
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            getLog().info(LOG_LABEL + SPACE + "Compiling mutant " + mutant.getId());
            if (compileSingleSource(mutant.getOriginalFilePath()) != EXIT_NORMAL) {
                getLog().error("Failed to compile mutant: " + mutant.getId()
                        + ", will be removed from mutant set.");
                failedToCompile.add(mutant);
                try {
                    Files.delete(Paths.get(mutant.getMutantFilePath()));
                } catch (IOException ex) {
                    getLog().error(LOG_LABEL + SPACE + "Failed to remove mutant file " + mutant.getMutantFilePath()
                            + ".");
                    ex.printStackTrace();
                }
                backUpOrRecover(mutant.getOriginalFilePath(), true);
                continue;
            }
            backUpOrRecover(mutant.getOriginalFilePath(), true);
        }
        mutants.removeAll(failedToCompile);
    }

    /** Writes all mutant to a file named `all-mutants.txt` under the artifact directory. */
    private void writeAllMutantsToFile() {
        try (PrintWriter pw = new PrintWriter(artifactsDir + File.separator + "all-mutants.txt")) {
            for (Mutant mutant : mutants) {
                pw.println(mutant.getId());
            }
        } catch (IOException ex) {
            getLog().error("Failed to write all mutants to file.");
            ex.printStackTrace();
        }
    }

    private void replaceWithMutant(Mutant mutant) {
        // Make sure you back up before running this!
        try {
            List<String> lines = Files.readAllLines(Paths.get(mutant.getOriginalFilePath()));
            if (mutant.getLineNumber() <= lines.size()) {
                // This way of mutation should be done BEFORE transplantation.
                lines.set(mutant.getLineNumber() - 1, mutant.getMutatedContent()); // lines from file is 0-based.
                Files.write(Paths.get(mutant.getOriginalFilePath()), lines);
                getLog().debug(LOG_LABEL + SPACE + "Replaced " + mutant.getOriginalContent() + " with "
                        + mutant.getMutatedContent() + ".");
            } else {
                getLog().error(LOG_LABEL + SPACE + "Line " + mutant.getLineNumber() + " does not exist in the file.");
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void runWithMutant(Mutant mutant, int round) {
        getLog().info(LOG_LABEL + SPACE + "Running r" + round + " inline tests with mutant " + mutant.getId() + ".");

        String reportDir = mutatedRunsDir + File.separator + "r" + round + File.separator + mutant.getId();
        new File(reportDir).mkdirs();
        for (String file : inputs.keySet()) {
            // It's not OK to only backup mutant's original file. Because transplantation will affect ALL files that
            // show up in the inline test list.
            backUpOrRecover(file, false);
        }
        replaceWithMutant(mutant);
        transplant((round == 0 ? r0TestPath : r1TestPath),
                logDir + File.separator + "r" + round + "-transplant-log.txt", false,
                transplatedSourceCode + File.separator + "r" + round);
        getLog().info(LOG_LABEL + SPACE + "Compiling mutant " + mutant.getId());
        compileSingleSource(mutant.getOriginalFilePath());
        Utils.recursiveDelete(new File(parsedItestsSrc + File.separator + "r" + round));
        Utils.recursiveDelete(new File(parsedItestsBin + File.separator + "r" + round));
        parseInlineTests(mutant.getOriginalFilePath(),
                parsedItestsSrc + File.separator + "r" + round, DEV_NULL);
        compileInlineTests(round);
        executeInlineTests(round, reportDir);
        for (String file : inputs.keySet()) {
            backUpOrRecover(file, true);
        }
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        super.execute();
        getLog().info(LOG_LABEL + SPACE + TOP_LEVEL_BAR + "Genie mutator started." + TOP_LEVEL_BAR);

        mutantsDir = genieDir + File.separator + "mutants";
        mutatedRunsDir = genieDir + File.separator + "mutated-runs";
        r0MutatedRunsDir = mutatedRunsDir + File.separator + "r0";
        r1MutatedRunsDir = mutatedRunsDir + File.separator + "r1";
        if (majorExecutable == null) {
            this.majorExecutable = executablesDirPath + File.separator + MAJOR_EXECUTABLE + "-"
                    + MAJOR_VERSION + File.separator + "bin" + File.separator + MAJOR_EXECUTABLE;
        }

        // TODO: Maybe add a parameter for controlling whether to use existing mutants or generate and override.
        if (! new File(mutantsDir).exists()) {
            for (String filePath : inputs.keySet()) {
                prepareLineNumbersFile(filePath);
                mutate(filePath);
                if (mutator.equals("major")) {
                    // TODO: major is more structured than um,
                    //  maybe consider adding structure to major-generated code instead.
                    flattenMajor(filePath);
                }
                removeIneffectiveMutants(filePath);
            }
        } else {
            // Skip mutant generation process if there are available mutants to use.
            // But still,all mutants must be registered with their original source files.
            for (String filePath : inputs.keySet()) {
                try (Stream<Path> mutants = Files.walk(Paths.get(mutantsDir))) {
                    Set<Path> matchingMutants = mutants.filter(Files::isRegularFile)
                            .filter(path -> path.getFileName().toString() // toString() is necessary here.
                                    // Make sure that only ineffective mutants relevant to the input file are chosen.
                                    // TODO: Currently this fits the naming convention of universal mutator mutants.
                                    .startsWith(Paths.get(filePath).getFileName().toString().split("\\.")[0]))
                            .collect(Collectors.toSet());
                    for (Path mutant : matchingMutants) {
                        registerMutant(filePath, mutant);
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
        compileMutants();
        writeAllMutantsToFile();
        for (int round = 0; round <= 1; round++) {
            for (Mutant mutant : mutants) {
                runWithMutant(mutant, round);
            }
        }
    }
}
