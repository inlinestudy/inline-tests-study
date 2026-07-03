package org.genie;

import com.github.javaparser.StaticJavaParser;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.genie.util.Utils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Genie transplanter transplants generated inline tests back to the original source code. */
@Mojo(name = "transplanter", requiresDependencyResolution = ResolutionScope.TEST)
public class TransplanterMojo extends ExecutorMojo {
    /** A directory that stores all the transplanted source code, under the artifact directory. */
    protected String transplatedSourceCode;

    /** A directory that is dedicated to store transplanted source code for r0 inline tests. */
    protected String transplantedWithR0;

    /** A directory that is dedicated to store transplanted source code for r1 inline tests. */
    protected String transplantedWithR1;

    /** A directory for storing the source code of parsed inline tests. */
    protected String parsedItestsSrc;

    /** A directory for storing the bytecode of parsed inline tests. */
    protected String parsedItestsBin;

    /**
     * Inline test hash is defined as the hash of the content of an inline test, for instance
     * itest("Randoop", 5).given.... Serial represents a 0-indexed line number of this content present in the R0 or R1
     * inline test list. For instance, if itest("Randoop", 5).given... shows up on line 7 of the R0 inline test list,
     * then the corresponding R0 map will register: hash(itest("Randoop", 5).given...) => 6.
     */
    protected Map<Integer, Integer> itestHashToR0Serial = new HashMap<>();
    protected Map<Integer, Integer> itestHashToR1Serial = new HashMap<>();

    // TODO: Convert the following data structures into ones that use the InlineTest object.
    /**
     * testId is defined as a string representing the inline test execution in JUnit report. For instance, for an inline
     * test that checks A.java:5, and is at line 10 after transplantation, when executing its compiled version, its
     * execution result will show something like
     * `<testcase name="testLine10()" classname="org.example.App_5Test" time="0.003">`. testId is composed of
     * information from testcase.name and testcase.classname, as well as the round of the inline test. In this case, it
     * will either be "R0#App_5Test#testLine10()" or "R1#App_5Test#testLine10()".
     * This data structure maintains a map such that its key and value are testId and itest content respectively.
     * Sample entry looks like: R0#App_5Test#testLine10() -> hash(itest("Randoop", 5).given...)
     * Eventually, this map will be combined with another map to find the serial of itests in r0 and r1 itest list.
     */
    protected Map<String, Integer> testIdToItestHash = new HashMap<>();

    private List<Integer> passingR0Serial = new ArrayList<>();
    private List<Integer> passingR1Serial = new ArrayList<>();
    private List<Integer> failingR0Serial = new ArrayList<>();
    private List<Integer> failingR1Serial = new ArrayList<>();

    /**
     * Move or copy a file while preserving its parent directory structure, useful for preserving directory structures
     * that are vital for Java package organization. Note that all path string arguments need to be in absolute path.
     * For example, if srcDir=/a, src=/a/b/c/d.txt, destDir=/e, then this operation will either move or copy d.txt from
     * /a/b/c/d.txt to /e/b/c/d.txt.
     * @param srcDir absolute path of the directory that contains a directory structure to preserve
     * @param src absolute path of the file to move/copy
     * @param destDir absolute path of the destination directory that should contain the structured directories
     * @param copy true if the operation is copy, false if the operation is move
     */
    protected void structurePreservingMoveCopy(String srcDir, String src, String destDir, boolean copy) {
        String dest = destDir + src.replace(srcDir, ""); // Don't need a File.separator here.
        File destFile = new File(dest);
        destFile.getParentFile().mkdirs();
        try {
            if (copy) {
                Files.copy(Paths.get(src), Paths.get(dest), StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.move(Paths.get(src), Paths.get(dest), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            getLog().error(LOG_LABEL + SPACE + "Move/copy source code failed.");
            getLog().debug("srcDir:" + srcDir);
            getLog().debug("src:" + src);
            getLog().debug("destDir:" + destDir);
            ex.printStackTrace();
        }
    }

    /**
     * This method transplants a list of inline tests to source code.
     * @param itestsList a file containing a list of inline tests
     * @param logFile path to a file that keeps the output generated during transplantation
     * @param needsCopy true if needing to copy the transplanted files elsewhere
     * @param copyDest destination to copy the transplanted files, ignored if needsCopy is unset
     */
    protected void transplant(String itestsList, String logFile, boolean needsCopy, String copyDest) {
        getLog().info(String.format("%s Transplanting inline tests from %s.", LOG_LABEL, itestsList));

        List<String> command = new ArrayList<>();
        command.addAll(Arrays.asList("java", "-cp", getDepsFileContent(), "org.raninline.App", "a", itestsList));
        Utils.runSubprocess(command, basedir, new File(logFile), 0, true);
        if (needsCopy) {
            new File(copyDest).mkdirs();
            for (String filePath : inputs.keySet()) {
                structurePreservingMoveCopy(basedir + File.separator + SRC_MAIN_JAVA,
                        Paths.get(filePath).toAbsolutePath().toString(), copyDest, true);
            }
        }
    }

    /**
     * Backs up or recovers a file. For instance, backing up README.md will obtain a copy README.md.bak under the same
     * directory. When recovering, the backup file README.md.bak will be removed, after its content is restored to the
     * file under the original name.
     * @param sourceCode path to the file that should be backed up, or the intended destination of recovery
     * @param recover false for backup mode, true for recovery mode
     */
    protected void backUpOrRecover(String sourceCode, boolean recover) {
        try {
            Files.copy(recover ? Paths.get(sourceCode + BACKUP_SUFFIX) : Paths.get(sourceCode),
                    recover ? Paths.get(sourceCode) : Paths.get(sourceCode + BACKUP_SUFFIX),
                    StandardCopyOption.REPLACE_EXISTING);
            if (recover) {
                Files.deleteIfExists(Paths.get(sourceCode + BACKUP_SUFFIX));
            }
        } catch (IOException ex){
            getLog().error(LOG_LABEL + SPACE + "Back up / recover source code failed.");
            ex.printStackTrace();
        }
    }

    /** Based on the content of transplanted source files, populate the testIdToItestContent mapping. */
    private void buildTestIdToItestHash() {
        itestHashToR0Serial.clear();
        itestHashToR1Serial.clear();
        try (Stream<Path> stream = Files.walk(Paths.get(transplantedWithR0))) {
            stream.filter(Files::isRegularFile).forEach(path -> {
                try {
                    String fileNameWithoutExtension = path.getFileName().toString().split("\\.")[0];
                    List<String> lines = Files.readAllLines(path);
                    for (int i = 0; i < lines.size(); i++) {
                        String trimmedLine = lines.get(i).trim();
                        // Whenever we see the itest pattern occurring in transplanted file,
                        // register a mapping to test id.
                        Pattern pattern = Pattern.compile("itest\\(\"[^\"]*\",\\s*(\\d+)\\)");
                        Matcher matcher = pattern.matcher(trimmedLine);
                        if (matcher.find()) {
                            String originalLineNumberStr = matcher.group(1);
                            int itestHashCode = StaticJavaParser.parseExpression(trimmedLine.replaceAll(";$", ""))
                                    .hashCode();
                            getLog().debug(LOG_LABEL + SPACE + "Putting key: " + "R0#" + fileNameWithoutExtension + "_"
                                    + originalLineNumberStr + "Test#testLine" + (i + 1) + "()" + "; value: " + itestHashCode
                                    + "; into testIdToItestHash");
                            testIdToItestHash.put("R0#" + fileNameWithoutExtension + "_" + originalLineNumberStr
                                    + "Test#testLine" + (i + 1) + "()", itestHashCode);
                        }
                    }
                } catch (IOException ex) {
                    getLog().error(LOG_LABEL + SPACE + "Error reading " + path.toAbsolutePath());
                    ex.printStackTrace();
                }
            });
        } catch (IOException ex) {
            getLog().error(LOG_LABEL + SPACE + "Error building testId to itest content for r0.");
            ex.printStackTrace();
        }
        try (Stream<Path> stream = Files.walk(Paths.get(transplantedWithR1))) {
            stream.filter(Files::isRegularFile).forEach(path -> {
                try {
                    String fileNameWithoutExtension = path.getFileName().toString().split("\\.")[0];
                    List<String> lines = Files.readAllLines(path);
                    for (int i = 0; i < lines.size(); i++) {
                        String trimmedLine = lines.get(i).trim();
                        // Whenever we see the itest pattern occurring in transplanted file,
                        // register a mapping to test id.
                        Pattern pattern = Pattern.compile("itest\\(\"[^\"]*\",\\s*(\\d+)\\)");
                        Matcher matcher = pattern.matcher(trimmedLine);
                        if (matcher.find()) {
                            String originalLineNumberStr = matcher.group(1);
                            int itestHashCode = StaticJavaParser.parseExpression(trimmedLine.replaceAll(";$", ""))
                                    .hashCode();
                            getLog().debug(LOG_LABEL + SPACE + "Putting key: " + "R1#" + fileNameWithoutExtension + "_"
                                    + originalLineNumberStr + "Test#testLine" + (i + 1) + "()" + "; value: " + itestHashCode
                                    + "; into testIdToItestHash");
                            testIdToItestHash.put("R1#" + fileNameWithoutExtension + "_" + originalLineNumberStr
                                    + "Test#testLine" + (i + 1) + "()", itestHashCode);
                        }
                    }
                } catch (IOException ex) {
                    getLog().error(LOG_LABEL + SPACE + "Error reading " + path.toAbsolutePath());
                    ex.printStackTrace();
                }
            });
        } catch (IOException ex) {
            getLog().error(LOG_LABEL + SPACE + "Error building testId to itest content for r1.");
            ex.printStackTrace();
        }
    }

    /** Based on the contents or r0 inline test list and r1 inline test list, construct the itestHashToSerial map. */
    private void buildItestHashToSerial() {
        itestHashToR0Serial.clear();
        itestHashToR1Serial.clear();
        try {
            List<String> r0Lines = Files.readAllLines(Paths.get(r0TestPath));
            for (int i = 0; i < r0Lines.size(); i++) {
                String itestConent = r0Lines.get(i).split(";", 3)[2];
                int hashCode = StaticJavaParser.parseExpression(itestConent.replaceAll(";$", "")).hashCode();
                itestHashToR0Serial.put(hashCode, i);
                getLog().debug(LOG_LABEL + SPACE + "Putting hashCode(" + itestConent.replaceAll(";$", "")
                        + ") to serial " + i + " for R0 with hashCode: " + hashCode + ".");
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        try {
            List<String> r1Lines = Files.readAllLines(Paths.get(r1TestPath));
            for (int i = 0; i < r1Lines.size(); i++) {
                String itestConent = r1Lines.get(i).split(";", 3)[2];
                int hashCode = StaticJavaParser.parseExpression(itestConent.replaceAll(";$", "")).hashCode();
                itestHashToR1Serial.put(hashCode, i);
                getLog().debug(LOG_LABEL + SPACE + "Putting hashCode(" + itestConent.replaceAll(";$", "")
                        + ") to serial " + i + " for R1 with hashCode: " + hashCode + ".");
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Parse inline test is the process of extracting a Java test class that can be executed on its own,
     * from an inline test.
     * @param source absolute or relative path to a transplanted source file
     * @param outputDir the directory to output the parsed inline test file to
     * @param logFile path to the log file that captures the output of the parsing process
     */
    protected void parseInlineTests(String source, String outputDir, String logFile) {
        getLog().info(String.format("%s Parsing inline tests from %s.", LOG_LABEL, source));

        new File(outputDir).mkdirs();
        List<String> command = new ArrayList<>();
        command.addAll(Arrays.asList("java", "-cp", getDepsFileContent(),
                "org.inlinetest.InlineTestRunnerSourceCode", "--input_file=" + source, "--assertion_style=junit",
                "--output_dir=" + outputDir, "--multiple_test_classes=true", "--dep_file_path=" + deps,
                "--app_src_path=" + basedir + File.separator + SRC_MAIN_JAVA));
        Utils.runSubprocess(command, basedir, new File(logFile), 0, true);
        if (printCommand) {
            getLog().debug(String.format("%s Running command: %s", LOG_LABEL, String.join(SPACE, command)));
        }
    }

    // TODO: This might have potential issues with classes having the same name but in different packages.
    //  could consider parse them into organized directories instead of having a flat structure.
    protected void compileInlineTests(int round) {
        getLog().info(LOG_LABEL + SPACE + "Compiling r" + round + " inline tests.");

        String compileLog = logDir + File.separator + "r" + round + "-compile-log.txt";
        String outputDir = parsedItestsBin + File.separator + "r" + round;
        new File(outputDir).mkdirs();
        Set<String> srcFiles = new HashSet<>();
        try (Stream<Path> stream = Files.walk(Paths.get(parsedItestsSrc + File.separator + "r" + round))) {
            srcFiles = stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .map(Path::toString)
                    .collect(Collectors.toSet());
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        List<String> command = new ArrayList<>();
        command.addAll(Arrays.asList("javac", "-cp", getDepsFileContent() + File.pathSeparator
                        + "target" + File.separator + "classes" + File.pathSeparator
                        + parsedItestsSrc + File.separator + "r" + round,
                "-d", outputDir));
        command.addAll(srcFiles);
        if (printCommand) {
            getLog().debug(LOG_LABEL + SPACE + "Running command: " + String.join(" ", command));
        }
        Utils.runSubprocess(command, basedir, new File(compileLog), 0, false);
    }

    protected void executeInlineTests(int round, String reportsDir) {
        getLog().info(LOG_LABEL + SPACE + "Executing r" + round + " inline tests.");

        Set<String> packages = new HashSet<>();
        String srcPath = parsedItestsBin + File.separator + "r" + round;
        try (Stream<Path> stream = Files.walk(Paths.get(srcPath))) {
            packages = stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".class"))
                    .filter(path -> !path.equals(Paths.get(srcPath)))
                    .map(Path::toString)
                    .map(path -> path.split(srcPath)[1]
                            // Substring starts from 1 to remove the extra separator character.
                            .substring(1, path.split(srcPath)[1].lastIndexOf(File.separator))
                            .replace(File.separatorChar, '.'))
                    .collect(Collectors.toSet());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        List<String> command = new ArrayList<>();
        command.addAll(Arrays.asList("java", "-jar", junitStandaloneJar, "-cp",
                // Don't want to execute default tests, so replace test classes directory with the empty string.
                getDepsFileContent().replace(this.testClassesDirectory.toString(), "")
                        // The test report is useful for removing failed inline tests.
                        + File.pathSeparator + srcPath, "--reports-dir", reportsDir));
        for (String pkg : packages) {
            command.add("--select-package");
            command.add(pkg);
        }
        if (printCommand) {
            getLog().debug(LOG_LABEL + SPACE + "Running command: " + String.join(" ", command));
        }
        Utils.runSubprocess(command, basedir, new File(logDir + File.separator + "r" + round + "-execution-log.txt"),
                3600, false);
    }

    private void processTestCase(int round, JSONObject testcase) {
        String testId = "R" + round + "#"
                + testcase.optString("classname").substring(testcase.optString("classname").lastIndexOf(".") + 1) + "#"
                + testcase.optString("name");
        getLog().debug("Calling processTestCase with: round: " + round + " testId: " + testId
                + " hashCode: " + testIdToItestHash.get(testId));
        int serial = (round == 0 ? itestHashToR0Serial : itestHashToR1Serial)
                .get(testIdToItestHash.get(testId));
        if (testcase.has("error") || testcase.has("failure")) {
            (round == 0 ? failingR0Serial : failingR1Serial).add(serial);
        } else {
            (round == 0 ? passingR0Serial : passingR1Serial).add(serial);
        }
    }

    private void writePassingAndFailingItests() {
        String passingR0Tests = genieDir + File.separator + "r0-passing-inlinetests.txt";
        String passingR1Tests = genieDir + File.separator + "r1-passing-inlinetests.txt";
        String failingR0Tests = genieDir + File.separator + "r0-failing-inlinetests.txt";
        String failingR1Tests = genieDir + File.separator + "r1-failing-inlinetests.txt";
        getLog().info(LOG_LABEL + SPACE + "Writing failed inline tests to "
                + failingR0Tests + " and " + failingR1Tests);
        try {
            List<String> r0Tests = Files.readAllLines(Paths.get(r0TestPath));
            List<String> r1Tests = Files.readAllLines(Paths.get(r1TestPath));

            PrintWriter failingR0Writer = new PrintWriter(failingR0Tests);
            for (int i : failingR0Serial) {
                failingR0Writer.println(r0Tests.get(i));
            }
            failingR0Writer.close();

            PrintWriter failingR1Writer = new PrintWriter(failingR1Tests);
            for (int i : failingR1Serial) {
                failingR1Writer.println(r1Tests.get(i));
            }
            failingR1Writer.close();

            PrintWriter passingR0Writer = new PrintWriter(passingR0Tests);
            for (int i : passingR0Serial) {
                passingR0Writer.println(r0Tests.get(i));
            }
            passingR0Writer.close();

            PrintWriter passingR1Writer = new PrintWriter(passingR1Tests);
            for (int i : passingR1Serial) {
                passingR1Writer.println(r1Tests.get(i));
            }
            passingR1Writer.close();

            r0TestPath = passingR0Tests;
            r1TestPath = passingR1Tests;
        } catch (IOException ex) {
            getLog().error(LOG_LABEL + SPACE + "Error writing passing and failing inline tests.");
            ex.printStackTrace();
        }
    }

    /** Removes failed R0 and R1 inline tests by executing them and check execution report. */
    private void removeFailingItests() {
        // This round of execution is necessary, as it filters out failing inline tests.
        for (int r = 0; r <= 1; r++) {
            Set<String> transplantedSources = new HashSet<>();
            try (Stream<Path> stream = Files.walk(Paths.get(transplatedSourceCode + File.separator + "r" + r))) {
                transplantedSources = stream.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().endsWith(".java"))
                        .map(Path::toAbsolutePath)
                        .map(Path::toString)
                        .collect(Collectors.toSet());
                getLog().debug("R" + r + " transplanted sources: " + transplantedSources);
                for (String source : transplantedSources) {
                    parseInlineTests(source, parsedItestsSrc + File.separator + "r" + r,
                            logDir + File.separator + "r" + r + "-parse-log.txt");
                }
            } catch (IOException ex) {
                getLog().error(LOG_LABEL + SPACE + "Failed to traverse "
                        + transplatedSourceCode + File.separator + "r" + r + ".");
                ex.printStackTrace();
            }
            compileInlineTests(r);
            executeInlineTests(r, genieDir + File.separator + "r" + r + "-reports");
        }
        // Analyze R0 report
        String r0Report = genieDir + File.separator + "r0-reports" + File.separator + JUNIT_REPORT;
        getLog().debug(LOG_LABEL + SPACE + "Analysing R0 execution report: " + r0Report);
        try {
            JSONObject root = XML.toJSONObject(new String(Files.readAllBytes(Paths.get(r0Report))));
            JSONObject testsuite = root.getJSONObject("testsuite");
            try {
                JSONArray testcases = testsuite.getJSONArray("testcase");
                for (int i = 0; i < testcases.length(); i++) {
                    processTestCase(0, testcases.getJSONObject(i));
                }
            } catch (JSONException ex) {
                try {
                    JSONObject testcase = testsuite.getJSONObject("testcase");
                    processTestCase(0, testcase);
                } catch (JSONException ex2) {
                    ex2.printStackTrace();
                    getLog().error("Malformed test report. Cannot identify testcase.");
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            getLog().error("Malformed test report.");
        }
        // Analyze R1 report
        String r1Report = genieDir + File.separator + "r1-reports" + File.separator + JUNIT_REPORT;
        getLog().debug(LOG_LABEL + SPACE + "Analysing R1 execution report: " + r1Report);
        try {
            JSONObject root = XML.toJSONObject(new String(Files.readAllBytes(Paths.get(r1Report))));
            JSONObject testsuite = root.getJSONObject("testsuite");
            try {
                JSONArray testcases = testsuite.getJSONArray("testcase");
                for (int i = 0; i < testcases.length(); i++) {
                    processTestCase(1, testcases.getJSONObject(i));
                }
            } catch (JSONException ex) {
                try {
                    JSONObject testcase = testsuite.getJSONObject("testcase");
                    processTestCase(1, testcase);
                } catch (JSONException ex2) {
                    ex2.printStackTrace();
                    getLog().error("Malformed test report. Cannot identify testcase.");
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            getLog().error("Malformed test report.");
        }
        writePassingAndFailingItests();
    }

    /**
     * Transplants inline tests generated in an isolated context back to source code.
     * @throws MojoExecutionException
     * @throws MojoFailureException
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        super.execute();
        getLog().info(LOG_LABEL + SPACE + TOP_LEVEL_BAR + "Genie transplanter started." + TOP_LEVEL_BAR);

        transplatedSourceCode = genieDir + File.separator + "transplanted-source-code";
        transplantedWithR0 = transplatedSourceCode + File.separator + "r0";
        transplantedWithR1 = transplatedSourceCode + File.separator + "r1";
        parsedItestsSrc = genieDir + File.separator + "parsed-itests" + File.separator + "src";
        parsedItestsBin = genieDir + File.separator + "parsed-itests" + File.separator + "bin";

        for (int round = 0; round <= 1; round++) {
            for (String filePath : inputs.keySet()) {
                backUpOrRecover(filePath, false);
            }
            transplant((round == 0 ? r0TestPath : r1TestPath),
                    logDir + File.separator + "r" + round + "-transplant-log.txt", true,
                    transplatedSourceCode + File.separator + "r" + round);
            for (String filePath : inputs.keySet()) {
                backUpOrRecover(filePath, true);
            }
        }
        buildTestIdToItestHash(); // This relies on having out-of-place transplanted R0 and R1 inline tests.
        buildItestHashToSerial();
        removeFailingItests();
        if (passingR0Serial.isEmpty() && passingR1Serial.isEmpty()) {
            getLog().error(
                    String.format("%s %s There are no passing R0 or R1 inline tests.", LOG_LABEL, EARLY_EXIT_LABEL));
            System.exit(1);
        }
        // Need to do the following again, because some inline tests are removed.
        // TODO: Rethink whether this approach is overly conservative,
        //  maybe some more light-weighted changes can be done.
        // These map building are done from source files, therefore we must re-transplant:
        for (int round = 0; round <= 1; round++) {
            for (String filePath : inputs.keySet()) {
                backUpOrRecover(filePath, false);
            }
            transplant((round == 0 ? r0TestPath : r1TestPath),
                    logDir + File.separator + "r" + round + "-transplant-log.txt", true,
                    transplatedSourceCode + File.separator + "r" + round);
            for (String filePath : inputs.keySet()) {
                backUpOrRecover(filePath, true);
            }
        }
        buildTestIdToItestHash();
        buildItestHashToSerial();
    }
}
