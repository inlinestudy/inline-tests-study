package org.genie;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.genie.datastructure.InlineTest;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * In Genie reducer, test results from mutated runs will be used to build a mapping from inline test to killed mutants.
 * Finally, a reduction algorithm (in the current case, greedy) will select a set of inline tests to be round 2 inline
 * tests.
 */
@Mojo(name = "reducer", requiresDependencyResolution = ResolutionScope.TEST)
public class ReducerMojo extends MutatorMojo {
    /**
     * In the case that a target statement changed from a state of having an inline test to not having an inline test
     * due to reduction by mutation, if `conservative` is set, then the list of inline tests will be looped again to see
     * if any r1 inline tests can be added back to make sure that there is at least one inline test that is associated
     * with that target statement.
     */
    @Parameter(property = "conservative", defaultValue = "false")
    private boolean conservative;

    /** A set that keeps track of already-killed mutants represented by their mutant id. */
    protected Set<String> killedMutants;
    protected Map<Integer, Set<String>> r0ItestToKilledMutants = new HashMap<>();
    protected Map<Integer, Set<String>> r1ItestToKilledMutants = new HashMap<>();
    protected Set<Integer> selectedR0 = new HashSet<>();
    protected Set<Integer> selectedR1 = new HashSet<>();
    protected Map<Integer, String> r0TestToTargetStatement = new HashMap<>();
    protected Map<Integer, String> r1TestToTargetStatement = new HashMap<>();
    protected Set<String> coveredTargetStatements = new HashSet<>();
    private List<InlineTest> r0InlineTests = new ArrayList<>();
    private List<InlineTest> r1InlineTests = new ArrayList<>();

    private void processTestCase(Map<Integer, Set<String>> aggregationMap, JSONObject testcase, String mutantId,
                                 int round) {
        String testId = "R" + round + "#"
                + testcase.optString("classname").substring(testcase.optString("classname").lastIndexOf(".") + 1) + "#"
                + testcase.optString("name");
        getLog().debug("Calling processTestCase with: round: " + round + " mutant: " + mutantId + " testId: " + testId
                + " hashCode: " + testIdToItestHash.get(testId));
        if (testcase.has("error") || testcase.has("failure")) {
            try {
                int serial = (round == 0 ? itestHashToR0Serial : itestHashToR1Serial)
                        .get(testIdToItestHash.get(testId));
                Set<String> newKilledSet = aggregationMap.getOrDefault(serial, new HashSet<>());
                newKilledSet.add(mutantId);
                aggregationMap.put(serial, newKilledSet);
            } catch (NullPointerException ex) {
                if (testIdToItestHash.isEmpty()) {
                    getLog().error(LOG_LABEL + SPACE + "testIdToItestHash is empty!");
                }
                if (testIdToItestHash.get(testId) == null) {
                    getLog().error(LOG_LABEL + SPACE + "No itest content available for testId: " + testId);
                } else {
                    getLog().error(LOG_LABEL + SPACE + "No serial available for this test content hash code: "
                            + testIdToItestHash.get(testId) + " for round " + round + ".");
                }
                ex.printStackTrace();
            }
        } else {
            int serial = (round == 0 ? itestHashToR0Serial : itestHashToR1Serial)
                    .get(testIdToItestHash.get(testId));
            // Even if a test kills no mutant, put an empty map there.
            // Later there will be a loop going over all the testIds.
            aggregationMap.put(serial, aggregationMap.getOrDefault(serial, new HashSet<>()));
        }
    }

    private void analyzeR0MutationReports() {
        try (Stream<Path> stream = Files.walk(Paths.get(r0MutatedRunsDir))) {
            stream.filter(Files::isRegularFile).filter(path -> path.toString().contains(JUNIT_REPORT))
                    .forEach(path -> {
                        getLog().debug(LOG_LABEL + SPACE + "Analysing junit report: " + path.toAbsolutePath());
                        // mutantId here is obtained from file path, as the name for the reports directory.
                        String mutantId = path.toString().replace(r0MutatedRunsDir + File.separator, "")
                                .replace(File.separator + JUNIT_REPORT, "");
                        try {
                            JSONObject root = XML.toJSONObject(new String(Files.readAllBytes(path)));
                            JSONObject testsuite = root.getJSONObject("testsuite");
                            try {
                                JSONArray testcases = testsuite.getJSONArray("testcase");
                                for (int i = 0; i < testcases.length(); i++) {
                                    processTestCase(r0ItestToKilledMutants, testcases.getJSONObject(i), mutantId, 0);
                                }
                            } catch (JSONException ex) {
                                try {
                                    JSONObject testcase = testsuite.getJSONObject("testcase");
                                    processTestCase(r0ItestToKilledMutants, testcase, mutantId, 0);
                                } catch (JSONException ex2) {
                                    ex2.printStackTrace();
                                    getLog().error("Malformed test report. Cannot identify testcase.");
                                }
                            }
                        } catch (IOException ex) {
                            ex.printStackTrace();
                            getLog().error("Malformed test report.");
                        }
                        getLog().debug(LOG_LABEL + SPACE + "Finished analysing junit report: "
                                + path.toAbsolutePath());
                    });
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void analyzeR1MutationReports() {
        try (Stream<Path> stream = Files.walk(Paths.get(r1MutatedRunsDir))) {
            stream.filter(Files::isRegularFile).filter(path -> path.toString().contains(JUNIT_REPORT))
                    .forEach(path -> {
                        getLog().debug(LOG_LABEL + SPACE + "Analysing junit report: " + path.toAbsolutePath());
                        // mutantId here is obtained from file path.
                        String mutantId = path.toString().replace(r1MutatedRunsDir + File.separator, "")
                                .replace(File.separator + JUNIT_REPORT, "");
                        try {
                            JSONObject root = XML.toJSONObject(new String(Files.readAllBytes(path)));
                            JSONObject testsuite = root.getJSONObject("testsuite");
                            try {
                                JSONArray testcases = testsuite.getJSONArray("testcase");
                                for (int i = 0; i < testcases.length(); i++) {
                                    processTestCase(r1ItestToKilledMutants, testcases.getJSONObject(i), mutantId, 1);
                                }
                            } catch (JSONException ex) {
                                try {
                                    JSONObject testcase = testsuite.getJSONObject("testcase");
                                    processTestCase(r1ItestToKilledMutants, testcase, mutantId, 1);
                                } catch (JSONException ex2) {
                                    ex2.printStackTrace();
                                    getLog().error("Malformed test report. Cannot identify testcase.");
                                }
                            }
                        } catch (IOException ex) {
                            ex.printStackTrace();
                            getLog().error("Malformed test report.");
                        }
                        getLog().debug(LOG_LABEL + SPACE + "Finished analysing junit report: "
                                + path.toAbsolutePath());
                    });
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Registers the content of r0 and r1 inline tests files. This method also builds a map from inline test serial to
     * target statement. The map was used only in conservative mode for adding back inline tests.
     * */
    private void registerInlineTests() {
        try {
            List<String> r0Lines = Files.readAllLines(Paths.get(r0TestPath));
            for (int i = 0; i < r0Lines.size(); i++) {
                r0InlineTests.add(new InlineTest(0, i, r0Lines.get(i)));
                r0TestToTargetStatement.put(i, r0Lines.get(i).split(";")[0] + "#" + r0Lines.get(i).split(";")[1]);
            }
            List<String> r1Lines = Files.readAllLines(Paths.get(r1TestPath));
            for (int i = 0; i < r1Lines.size(); i++) {
                r1InlineTests.add(new InlineTest(1, i, r1Lines.get(i)));
                r1TestToTargetStatement.put(i, r1Lines.get(i).split(";")[0] + "#" + r1Lines.get(i).split(";")[1]);
            }
        } catch (IOException ex) {
            getLog().error(LOG_LABEL + SPACE + "Error reading r0 and r1 inline tests.");
            ex.printStackTrace();
        }
    }

    private void addBackInlineTestsWithNoMutants() {
        // This is only done for r1.
        for (InlineTest inlineTest : r1InlineTests) {
            if (selectedR1.contains(inlineTest.getSerial())) {
                continue; // Skip if already selected.
            }
            // If there are no mutants that are based on the target statement that the inline test is based on,
            // add the inline test back to the selected set.
            if (!(mutants.stream().anyMatch(m -> m.getOriginalFilePath().equals(inlineTest.getFilePath())
                    && m.getLineNumber() == inlineTest.getLineNumber()))) {
                selectedR1.add(inlineTest.getSerial());
                coveredTargetStatements.add(r1TestToTargetStatement.get(inlineTest.getSerial()));
            }
        }
    }

    private void reduce() {
        for (int i = 0; i < r1ItestToKilledMutants.size(); i++) {
            if (killedMutants.containsAll(r1ItestToKilledMutants.getOrDefault(i, new HashSet<>()))
                    || r1ItestToKilledMutants.getOrDefault(i, new HashSet<>()).isEmpty()) {
                // If this particular inline test doesn't help killing more mutants, discard.
                continue;
            } else {
                killedMutants.addAll(r1ItestToKilledMutants.getOrDefault(i, new HashSet<>()));
                selectedR1.add(i);
                coveredTargetStatements.add(r1TestToTargetStatement.get(i));
            }
            if (killedMutants.size() == mutants.size()) {
                // This means that the current set of tests is enough to kill all mutants, reduction algorithm stops.
                return;
            }
        }
        for (int i = 0; i < r0ItestToKilledMutants.size(); i++) {
            if (killedMutants.containsAll(r0ItestToKilledMutants.getOrDefault(i, new HashSet<>()))
                    || r0ItestToKilledMutants.getOrDefault(i, new HashSet<>()).isEmpty()) {
                // If this particular inline test doesn't help killing more mutants, discard.
                continue;
            } else {
                killedMutants.addAll(r0ItestToKilledMutants.getOrDefault(i, new HashSet<>()));
                selectedR0.add(i);
                coveredTargetStatements.add(r0TestToTargetStatement.get(i));
            }
            if (killedMutants.size() == mutants.size()) {
                // This means that the current set of tests is enough to kill all mutants, reduction algorithm stops.
                return;
            }
        }
        // In the extreme case that there are no mutants,
        // or that no mutants are killed by any inline tests, add r1 tests back.
        if (mutants.isEmpty() || killedMutants.isEmpty()) {
            for (int i = 0; i < r1InlineTests.size(); i++) {
                selectedR1.add(i);
            }
        }
    }

    private void addBackForBasicCoverage() {
        for (int i = 0; i < r1TestToTargetStatement.size(); i++) {
            if (selectedR1.contains(i)) {
                continue;
            }
            if (!coveredTargetStatements.contains(r1TestToTargetStatement.get(i))) {
                coveredTargetStatements.add(r1TestToTargetStatement.get(i));
                selectedR1.add(i);
            }
        }
        for (int i = 0; i < r0TestToTargetStatement.size(); i++) {
            if (selectedR0.contains(i)) {
                continue;
            }
            if (!coveredTargetStatements.contains(r0TestToTargetStatement.get(i))) {
                coveredTargetStatements.add(r0TestToTargetStatement.get(i));
                selectedR0.add(i);
            }
        }
    }

    private void writeToR2AndKilledMutants(String reductionScheme) {
        String r2TestPath = genieDir + File.separator + "r2-" + tool + "-" + reductionScheme + ".txt";
        getLog().info(LOG_LABEL + SPACE + "Writing to r2 inline tests file: " + reductionScheme);
        List<String> r2Tests = new ArrayList<>();
        try {
            List<String> r1Tests = Files.readAllLines(Paths.get(r1TestPath));
            for (int i : selectedR1) {
                r2Tests.add(r1Tests.get(i));
            }
            if (!selectedR0.isEmpty()) {
                List<String> r0Tests = Files.readAllLines(Paths.get(r0TestPath));
                for (int i : selectedR0) {
                    r2Tests.add(r0Tests.get(i));
                }
            }
            try (PrintWriter pw = new PrintWriter(r2TestPath)) {
                for (String r2Test : r2Tests) {
                    pw.println(r2Test);
                }
            } catch (IOException ex) {
                getLog().error(LOG_LABEL + SPACE + "Error writing to " + r2TestPath + ".");
                ex.printStackTrace();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        String killedMutantsFile = genieDir + File.separator + "killed-mutants-" + reductionScheme + ".txt";
        getLog().info(LOG_LABEL + SPACE + "Writing killed mutants to " + killedMutantsFile + ".");
        try (PrintWriter pw = new PrintWriter(killedMutantsFile)) {
            for (String killedMutant : killedMutants) {
                pw.println(killedMutant);
            }
        } catch (IOException ex) {
            getLog().error(LOG_LABEL + SPACE + "Failed to write to " + killedMutantsFile + ".");
            ex.printStackTrace();
        }
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        super.execute();
        getLog().info(LOG_LABEL + SPACE + TOP_LEVEL_BAR + "Genie reducer started." + TOP_LEVEL_BAR);

        killedMutants = new HashSet<>();
        // First collect all the mapping, and then run the algorithm for better readability.
        analyzeR0MutationReports();
        analyzeR1MutationReports();

        registerInlineTests();

        getLog().debug(LOG_LABEL + SPACE + "r0 inline test to killed mutants:");
        for (Map.Entry<Integer, Set<String>> entry : r0ItestToKilledMutants.entrySet()) {
            getLog().debug(LOG_LABEL + SPACE + entry.getKey() + " => " + entry.getValue().toString());
        }
        getLog().debug(LOG_LABEL + SPACE + "r1 inline test to killed mutants:");
        for (Map.Entry<Integer, Set<String>> entry : r1ItestToKilledMutants.entrySet()) {
            getLog().debug(LOG_LABEL + SPACE + entry.getKey() + " => " + entry.getValue().toString());
        }

        reduce();
        writeToR2AndKilledMutants("aggressive");
        getLog().debug(LOG_LABEL + SPACE + "Selected r0 after reduction: " + selectedR0.toString());
        getLog().debug(LOG_LABEL + SPACE + "Selected r1 after reduction: " + selectedR1.toString());
        addBackInlineTestsWithNoMutants();
        writeToR2AndKilledMutants("standard");
        getLog().debug(LOG_LABEL + SPACE + "Selected r0 after adding back inline tests with no mutants: "
                + selectedR0.toString());
        getLog().debug(LOG_LABEL + SPACE + "Selected r1 after adding back inline tests with no mutants: "
                + selectedR1.toString());
        if (conservative) {
            addBackForBasicCoverage();
            writeToR2AndKilledMutants("conservative");
            getLog().debug(LOG_LABEL + SPACE + "Selected r0 after conservation: " + selectedR0.toString());
            getLog().debug(LOG_LABEL + SPACE + "Selected r1 after conservation: " + selectedR1.toString());
        }
        writeToR2AndKilledMutants("inlinetests");
        transplant(genieDir + File.separator + "r2-" + tool + "-inlinetests.txt",
                logDir + File.separator + "r2-transplant-log.txt", true,
                transplatedSourceCode + File.separator + "r2");
        for (String filePath : inputs.keySet()) {
            parseInlineTests(filePath, parsedItestsSrc + File.separator + "r2",
                    logDir + File.separator + "r2-parse-log.txt");
        }
        compileInlineTests(2);
        executeInlineTests(2, genieDir + File.separator + "r2-reports");
    }
}
