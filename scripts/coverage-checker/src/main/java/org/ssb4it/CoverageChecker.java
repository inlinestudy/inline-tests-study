package org.ssb4it;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.analysis.ICounter;
import org.jacoco.core.tools.ExecFileLoader;

/**
 * This class takes a set of jacoco.exec files obtained from running
 * some "tests" and produces the map of coverage for each line in all
 * classes that are covered per test. The name of each jacoco.exec is
 * used as the test name. Tests can be at any granularity level.
 */

public class CoverageChecker {
    public boolean covered = false;
    private final File execFile;
    private final File classesDirectory;

    private ExecFileLoader execFileLoader;

    public CoverageChecker(final File execFile, final File classesDirectory) {
        this.execFile = execFile;
        this.classesDirectory = classesDirectory;
    }

    public void create(File execFile, String fileOfInterest, String lineOfInterest) throws IOException {
        Map<String, Map<String, Map<Integer, Integer>>> covMap
                = new HashMap<String, Map<String, Map<Integer, Integer>>>();

        // Read a jacoco.exec file.
        loadExecutionData(execFile);

        // Run the structure analyzer on a single class folder to build up
        // the coverage model. The process would be similar if your classes
        // were in a jar file. Typically you would create a bundle for each
        // class folder and each jar you want in your report. If you have
        // more than one bundle you will need to add a grouping node to your
        // report
        String testName = execFile.getName().substring(0, execFile.getName().lastIndexOf("."));

        covMap.put(testName, new HashMap<String, Map<Integer, Integer>>());

        analyzeStructure(testName, covMap);
        // Need a step here to clean up PUTs
        Map<String, Map<String, Map<Integer, Integer>>> cleanedMap = new HashMap<>();
        for (Map.Entry<String, Map<String, Map<Integer, Integer>>> entry : covMap.entrySet()) {
            String cleanedKey = entry.getKey().split("\\[")[0];
            Map<String, Map<Integer, Integer>> value = entry.getValue();
            value.putAll(covMap.getOrDefault(entry.getKey(), new HashMap<>()));
            cleanedMap.put(cleanedKey, value);
        }

        // First level: Test to a map from every class to whether its lines are covered or not
        for (Map.Entry<String, Map<String, Map<Integer, Integer>>> entry : covMap.entrySet()) {
            // Second level: A class to the lines that it covers
            for (Map.Entry<String, Map<Integer, Integer>> classEntry : entry.getValue().entrySet()) {
                String className = classEntry.getKey();
                List<Integer> coveredLines = classEntry.getValue().entrySet().stream()
                        .filter(e -> e.getValue() >= ICounter.FULLY_COVERED)
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toList());
                for (Integer coveredLine : coveredLines) {
                    if (className.equals(fileOfInterest) && coveredLine == Integer.parseInt(lineOfInterest)) {
                        covered = true;
                    }
                }
            }
        }
    }

    private void loadExecutionData(File execFile) throws IOException {
        execFileLoader = new ExecFileLoader();
        execFileLoader.load(execFile);
    }

    private void analyzeStructure(String testName, Map<String, Map<String, Map<Integer, Integer>>> covMap) throws IOException {
        final CoverageBuilder coverageBuilder = new CoverageBuilder();
        final Analyzer analyzer = new Analyzer(execFileLoader.getExecutionDataStore(), coverageBuilder);

        analyzer.analyzeAll(classesDirectory);

        for (IClassCoverage icc : coverageBuilder.getClasses()) {
            String name = icc.getName().replace('/', '.');
            Map<Integer, Integer> covLinesMap = new HashMap<Integer, Integer>();

            for (int i = icc.getFirstLine(); i <= icc.getLastLine(); i++) {
                covLinesMap.put(i, icc.getLine(i).getStatus());
            }

            covMap.get(testName).put(name, covLinesMap);
        }
    }

    public static void main(final String[] args) throws IOException {
        File jacocoExecFile = new File(args[0]);
        File classesDirectory = new File(args[1]);
        String fileOfInterest = args[2];
        String lineOfInterest = args[3];
        try {
            fileOfInterest = fileOfInterest.replace(".java", "").split("src/main/java/")[1].replace("/", ".");
            CoverageChecker checker = new CoverageChecker(jacocoExecFile, classesDirectory);
            checker.create(checker.execFile, fileOfInterest, lineOfInterest);
            System.out.println(checker.covered ? "COVERED" : "NOT_COVERED");
        } catch (Exception ex) {
            System.out.println("NO_INFO");
        }
    }
}
