package org.ssb4it;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

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
        String org = entry.split("_")[0]; // Assume that _ does not appear in the org name
        String repo = entry.split("_", 2)[1].split("/")[0];
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
        return targetStmtStr.split("\n")[0];
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        String cacheDir = new File(args[0]).getAbsolutePath().toString();
        System.out.println("Cache directory: " + cacheDir);
        String first = getSrc(cacheDir, args[1]);
        System.out.println("First statement: " + first);
        System.out.println("First statement length: " + first.length());
        String second = getSrc(cacheDir, args[2]);
        System.out.println("Second statement: " + second);
        System.out.println("Second statement length: " + second.length());

        LevenshteinDistance distance = new LevenshteinDistance(null);
        int result = distance.apply(first, second);
        System.out.println("Edit distance: " + result);
    }
}
