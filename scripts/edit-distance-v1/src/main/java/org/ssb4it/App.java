package org.ssb4it;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.apache.commons.text.similarity.LevenshteinDistance;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.stmt.Statement;

public class App {
    /**
     * Get the statement from an entry
     * An entry can look like this:
     * jboss_jboss-jacc-api_spec/changed-ts.txt:a6dc45ccd301939c8f40bfc222fc81d65586cea2;0e15925d41da25435a15fe5cdaf3a66875aad10a;a6dc45ccd301939c8f40bfc222fc81d65586cea2;target stmt string;jboss_jboss-jacc-api_spec/src/main/java/javax/security/jacc/URLPatternSpec.java;85;null;;null;ONE_TO_ONE
     * @param entry
     * @return
     */
    public static String getSrc(String cacheDir, String entry) throws IOException, InterruptedException {
        String org = entry.split(";")[4].split("_")[0].split("/")[0]; // Assume that _ does not appear in the org name
        String repo = entry.split(";")[4].split("_", 2)[1].split("/")[0];
        String commit = entry.split(";")[2];
        String pathFromProjectRoot = entry.split(";")[4].split("/", 2)[1];
        String lineNumberStr = entry.split(";")[5];

        if (!Files.exists(Paths.get(cacheDir, org + "_" + repo))) {
            ProcessBuilder pb = new ProcessBuilder("git", "clone", "https://github.com/" + org + "/" + repo + ".git", org + "_" + repo);
            pb.directory(new File(cacheDir));
            pb.start().waitFor(); // Wait for the clone to finish.
        }
        ProcessBuilder pb = new ProcessBuilder("git", "checkout", "-f", commit);
        pb.directory(new File(cacheDir, org + "_" + repo));
        pb.start().waitFor(); // Wait for the checkout to finish.
        StaticJavaParser.getConfiguration().setAttributeComments(false);
        CompilationUnit cu = StaticJavaParser.parse(new File(cacheDir + File.separator + org + "_" + repo, pathFromProjectRoot));
        String targetStmtStr = cu.findAll(Statement.class).stream().filter(stmt -> stmt.getRange().get().begin.line == Integer.parseInt(lineNumberStr)).findFirst().get().removeComment().toString();
        return targetStmtStr;
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        boolean minimalisticOutput = false;
        if (args.length > 2) {
            minimalisticOutput = true;
        }
        String cacheDir = new File(args[0]).getAbsolutePath().toString();
        if (!minimalisticOutput) {
            System.out.println("Cache directory: " + cacheDir);
        }
        String entryFile = args[1];
        List<String> entryLines = Files.readAllLines(Paths.get(entryFile));
        // The following are for ONE_TO_ONE and ONE_TO_MANY ONLY
        boolean firstLine = true;
        String theOne = null;
        List<String> lines = new ArrayList<>();
        List<Integer> lengths = new ArrayList<>();
        List<Integer> eds = new ArrayList<>();
        String label = null;
        try {
            for (Iterator<String> it = entryLines.iterator(); it.hasNext();) {
                String entryLine = it.next();
                if (entryLine.endsWith("ADD") || entryLine.endsWith("DELETE")) {
                    String line = getSrc(cacheDir, entryLine);
                    System.out.println("First statement: " + line.split("\n")[0]);
                    System.out.println("First statement length: " + line.split("\n")[0].length());
                    return;
                }
                if (entryLine.endsWith("ONE_TO_ONE")) {
                    String first = getSrc(cacheDir, entryLine);
                    String second = getSrc(cacheDir, it.next());
                    System.out.println("First statement: " + first);
                    System.out.println("First statement length: " + first.length());
                    System.out.println("Second statement: " + second);
                    System.out.println("Second statement length: " + second.length());

                    LevenshteinDistance distance = new LevenshteinDistance(null);
                    int result = distance.apply(first, second);
                    System.out.println("Edit distance: " + result);
                    return;
                }
                if (entryLine.endsWith("ONE_TO_MANY")) {
                    label = "ONE_TO_MANY";
                    if (firstLine) {
                        firstLine = false;
                        theOne = getSrc(cacheDir, entryLine);
                        if (!minimalisticOutput) {
                            System.out.println("The one: " + theOne);
                        }
                        lengths.add(theOne.length());
                    } else {
                        String line = getSrc(cacheDir, entryLine);
                        lengths.add(line.length());
                        if (!minimalisticOutput) {
                            System.out.println("One of the many: " + line);
                        }
                        LevenshteinDistance distance = new LevenshteinDistance(null);
                        int result = distance.apply(theOne, line);
                        eds.add(result);
                    }
                }
                if (entryLine.endsWith("MANY_TO_ONE")) {
                    label = "MANY_TO_ONE";
                    if (!it.hasNext()) {
                        theOne = getSrc(cacheDir, entryLine);
                        if (!minimalisticOutput) {
                            System.out.println("The one: " + theOne);
                        }
                        lengths.add(theOne.length());
                        for (String line : lines) {
                            LevenshteinDistance distance = new LevenshteinDistance(null);
                            int result = distance.apply(theOne, line);
                            eds.add(result);
                        }
                    } else {
                        String line = getSrc(cacheDir, entryLine);
                        lengths.add(line.length());
                        if (!minimalisticOutput) {
                            System.out.println("One of the many: " + line);
                        }
                        lines.add(line);
                    }
                }
            }
        } catch (NoSuchElementException ex) {
            System.out.println("No such element exception");
            return;
        }
        if (!minimalisticOutput) {
            System.out.println("Lengths: " + lengths.toString().replaceAll("\\[", "").replaceAll("\\]", ""));
            System.out.println("EDs: " + eds.toString().replaceAll("\\[", "").replaceAll("\\]", ""));
        } else {
            if (label.equals("ONE_TO_MANY") || label.equals("MANY_TO_ONE")) {
                System.out.println(lengths.toString().replaceAll("\\[", "").replaceAll("\\]", "") + ";" + eds.toString().replaceAll("\\[", "").replaceAll("\\]", "") + ";" + Heuristic1.getVerdict(lengths, eds, label));
            }
        }
    }
}
