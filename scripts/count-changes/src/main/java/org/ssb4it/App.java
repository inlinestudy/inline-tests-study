package org.ssb4it;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;

public class App {
    /**
     * Each element in the list is a tuple of (class changed count, method changed count, statement changed count, total revision count) for one target statement in history. */
    private static List<List<Integer>> changes = new ArrayList<>();
    /** These fields are cleared for each target statement in history. */
    private static int classChangedCount = -1;
    private static int methodChangedCount = -1;
    private static int statementChangedCount = -1;
    private static int totalRevisionCount = 0;

    private static int classHash = 0;
    private static int methodHash = 0;
    private static int statementHash = 0;
    private static int previousLineNumber = 0;

    private static Statement previousStatement = null;

    /** Unused for single-statement. */
    private static void reset() {
        classChangedCount = 0;
        methodChangedCount = 0;
        statementChangedCount = 0;
        totalRevisionCount = 0;
    }

    private static String cloneAndCheckout(String url, String commit, String cacheDir) {
        try {
            if (cacheDir == null) {
                Path tmpDir = Files.createTempDirectory("tmp");
                System.out.println("Temporarily cloning project to: " + tmpDir.toAbsolutePath());
                ProcessBuilder pb = new ProcessBuilder("git", "clone", url, tmpDir.toAbsolutePath().toString());
                pb.start().waitFor(); // Must wait for the clone to finish.
                ProcessBuilder pb2 = new ProcessBuilder("git", "checkout", commit);
                pb2.directory(tmpDir.toFile());
                pb2.start().waitFor(); // Must wait for the checkout to finish.
                return tmpDir.toAbsolutePath().toString();
            } else {
                if (!Files.exists(Paths.get(cacheDir))) {
                    Files.createDirectory(Paths.get(cacheDir));
                }
                Path projectDir = Paths.get(cacheDir, url.split("/")[url.split("/").length - 2] + "_" + url.split("/")[url.split("/").length - 1]);
                if (!Files.exists(projectDir)) {
                    ProcessBuilder pb = new ProcessBuilder("git", "clone", url, projectDir.toAbsolutePath().toString());
                    pb.start().waitFor(); // Must wait for the clone to finish.
                }
                ProcessBuilder pb2 = new ProcessBuilder("git", "checkout", commit);
                pb2.directory(projectDir.toFile());
                pb2.start().waitFor(); // Must wait for the checkout to finish.
                return projectDir.toAbsolutePath().toString();
            }
        } catch (IOException | InterruptedException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    private static void processSnapshot(String path, String lineNumber, String projectDir) {
        CompilationUnit compilationUnit = null;
        try {
            compilationUnit = StaticJavaParser.parse(Paths.get(path));
        } catch (Exception ex) {
            ex.printStackTrace();
            return;
        }
        int currClassHash = compilationUnit.hashCode();
        if (currClassHash != classHash) {
            classChangedCount++;
            classHash = currClassHash;
        }
        int line = Integer.parseInt(lineNumber);
        Optional<BodyDeclaration> enclosingMethod = compilationUnit.findFirst(BodyDeclaration.class, declaration ->
            (declaration instanceof MethodDeclaration || declaration instanceof ConstructorDeclaration) &&
                    declaration.getRange().map(range -> range.begin.line <= line && range.end.line >= line).orElse(false)
        );
        if (enclosingMethod.isPresent()) {
            int currMethodHash = enclosingMethod.get().hashCode();
            if (currMethodHash != methodHash) {
                methodChangedCount++;
                methodHash = currMethodHash;
            }
        }
        Optional<Statement> statement = compilationUnit.findFirst(Statement.class, stmt -> stmt.getRange().map(range -> range.begin.line <= line && range.end.line >= line).orElse(false));
        if (statement.isPresent()) {
            int currStatementHash = statement.get().hashCode();
            if (currStatementHash != statementHash) {
                statementChangedCount++;
                statementHash = currStatementHash;
            }
        }
    }

    public static void main(String[] args) {
        try {
            String csvFile = args[0];
            String project = args[1];
            String url = "https://github.com/" + project;
            String cacheDir = null;
            if (args.length > 2) {
                cacheDir = args[2];
            }
            List<String> lines = Files.readAllLines(Paths.get(csvFile));
            for (String line : lines) {
                totalRevisionCount++;
                String[] fields = line.split(",");
                String commit = fields[0];
                String path = fields[1];
                String lineNumber = fields[2];
                String projectDir = cloneAndCheckout(url, commit, cacheDir);
                path = projectDir + File.separator + path;
                if (lineNumber.equals("None")) {
                    if (statementHash != 0) {
                        int bestEffortLine = -1;
                        try {
                            CompilationUnit cu = StaticJavaParser.parse(Paths.get(path));
                            List<Statement> stmts = cu.findAll(Statement.class);
                            for (Statement stmt : stmts) {
                                if (stmt.hashCode() == statementHash) {
                                    Optional<com.github.javaparser.Range> r = stmt.getRange();
                                    if (r.isPresent()) {
                                        bestEffortLine = r.get().begin.line;
                                        break;
                                    }
                                }
                            }
                        } catch (Exception ex) {} finally {
                            lineNumber = String.valueOf(previousLineNumber);
                        }
                        if (bestEffortLine != -1) {
                            lineNumber = String.valueOf(bestEffortLine);
                        }
                    } else {
                        lineNumber = String.valueOf(previousLineNumber);
                    }
                }
                previousLineNumber = Integer.parseInt(lineNumber);
                processSnapshot(path, lineNumber, projectDir);
            }
            System.out.println(classChangedCount + "," + methodChangedCount + "," + statementChangedCount + "," + totalRevisionCount);
        } catch (Exception ex) {
            // ex.printStackTrace();
        }
    }
}
