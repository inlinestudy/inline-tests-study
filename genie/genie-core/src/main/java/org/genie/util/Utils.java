package org.genie.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.javaparser.Range;
import com.github.javaparser.Position;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import org.apache.commons.text.StringEscapeUtils;

/** Utility class that contains many widely-used methods. */
public class Utils {
    public static String escapeString(String str) {
        return StringEscapeUtils.escapeJava(str);
    }

    public static boolean isValidVariableName(String name) {
        String pattern = "^[a-zA-Z_$][a-zA-Z0-9_$]*$";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(name);
        return m.matches();
    }

    public static String rename(String input) {
        if (input.startsWith("this.")) {
            input = input.substring(5);
        }
        if (input.startsWith("super.")) {
            input = input.substring(6);
        }
        return input.replace("*", "time").replace("+", "plus").replace("-", "minus").replace("/", "divide")
                .replace("=", "equal").replace("!", "not").replace(">", "greater").replace("<", "less")
                .replace("&", "and").replace("|", "or").replace("^", "xor").replace("%", "mod").replace("?", "question")
                .replace("(", "_").replace(")", "_").replace(" ", "").replace("[", "__").replace("]", "")
                .replace("\"", "_")
                .replace(".", "__").replace(",", "_").replace("'", "_");
    }

    public static String getOutput(String outputPath) throws IOException {
        // read output file
        return java.nio.file.Files.lines(java.nio.file.Paths.get(outputPath))
                .collect(java.util.stream.Collectors.joining("\n"));
    }

    public static void prepareProject(String projectDir, String sha) {
        // checkout the project at the given sha with bash command
        try {
            ProcessBuilder builder = new ProcessBuilder();
            builder.command("bash", "-c", "git checkout " + sha + " && " + "git clean -xfd");
            builder.directory(new File(projectDir)); // Optional: Set your working directory

            Process process = builder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            int exitValue = process.waitFor();
            System.out.println("Exit value: " + exitValue);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static int runSubprocess(List<String> command, File basedir, File output, long timeout, boolean append) {
        int exitCode;
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.inheritIO();
            pb.directory(basedir);
            pb.redirectErrorStream(true);
            if (output != null) {
                if (append) {
                    pb.redirectOutput(ProcessBuilder.Redirect.appendTo(output));
                } else {
                    pb.redirectOutput(output);
                }
            }
            Process process = pb.start();
            if (timeout > 0) {
                exitCode = process.waitFor(timeout, TimeUnit.SECONDS) ? 0 : 1;
            } else {
                exitCode = process.waitFor();
            }
        } catch (IOException | InterruptedException ex) {
            ex.printStackTrace();
            exitCode = 1;
        }
        return exitCode;
    }

    public static void copyRecursively(Path src, Path dest) throws IOException {
        Files.walk(src).forEach(srcPath -> {
                try {
                    Path targetPath = dest.resolve(src.relativize(srcPath));
                    Files.copy(srcPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
        });
    }

    /**
     * Updates the @timeout annotation of Evosuite-generated unit tests with a new timeout value.
     * @param src path to the source file containing evosuite-generated unit tests
     * @param newTimeout new timeout value in milliseconds
     */
    public static void updateEvosuiteTestTimeout(String src, int newTimeout) {
        File file = new File(src);
        try {
            CompilationUnit cu = StaticJavaParser.parse(Paths.get(src));
            cu.accept(new ModifierVisitor<Void>() {
                @Override
                public MethodDeclaration visit(MethodDeclaration md, Void arg) {
                    md.getAnnotations().forEach(annotation -> {
                        if (annotation.getNameAsString().equals("Test")) {
                            annotation.asNormalAnnotationExpr().getPairs().forEach(timeout -> {
                                if (timeout.getNameAsString().equals("timeout")) {
                                    timeout.getValue().asIntegerLiteralExpr().setValue("" + newTimeout);
                                }
                            });
                        }
                    });
                    return (MethodDeclaration) super.visit(md, arg);
                }
            }, null);
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(cu.toString());
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static boolean recursiveDelete(File toDelete) {
        File[] contents = toDelete.listFiles();
        if (contents != null) {
            for (File file : contents) {
                recursiveDelete(file);
            }
        }
        return toDelete.delete();
    }

    /**
     * Sometimes there are statements/conditions that span across multiple lines.
     * This method finds the first line of the statement/condition given a line number in the statement/condition.
     * @param pathToFile
     * @param inputLineNumber
     * @return
     */
    public static int findFirstLine(String pathToFile, int inputLineNumber) throws IOException {
        CompilationUnit cu = StaticJavaParser.parse(Paths.get(pathToFile));

        // Case 1: Part of an if
        Optional<Integer> ifCondBeginLine = cu.findAll(IfStmt.class)
                .stream()
                .filter(ifStmt -> ifStmt.getCondition().getRange().isPresent())
                .filter(ifStmt -> {
                    Range r = ifStmt.getCondition().getRange().get();
                    return r.begin.line <= inputLineNumber && r.end.line >= inputLineNumber;
                })
                .map(ifStmt -> ifStmt.getCondition().getBegin().get().line)
                .min(Integer::compareTo); // innermost if
        if (ifCondBeginLine.isPresent()) {
            return ifCondBeginLine.get();
        }

        // Case 2: Not a part of an if
        Optional<Statement> stmt = cu.findAll(Statement.class).stream()
                .filter(s -> s.getRange().isPresent())
                .filter(s -> {
                    Range r = s.getRange().get();
                    return r.begin.line <= inputLineNumber && r.end.line >= inputLineNumber;
                })
                .min(Comparator.comparingInt(
                        s -> s.getRange().get().end.line - s.getRange().get().begin.line
                ));

        return stmt.map(s -> s.getRange().get().begin.line)
                .orElse(inputLineNumber);
    }

    public static Set<Integer> findAllLines(String pathToFile, int inputLineNumber) throws IOException {
        CompilationUnit cu = StaticJavaParser.parse(Paths.get(pathToFile));

        // 1. Check if the line is part of an if-condition
        Optional<IfStmt> targetIf = cu.findAll(IfStmt.class).stream()
                .filter(ifStmt -> ifStmt.getRange().isPresent() && ifStmt.getCondition().getRange().isPresent())
                .filter(ifStmt -> {
                    Range condRange = ifStmt.getCondition().getRange().get();
                    return condRange.begin.line <= inputLineNumber && condRange.end.line >= inputLineNumber;
                })
                // innermost if
                .min(Comparator.comparingInt(
                        ifStmt -> ifStmt.getRange().get().end.line - ifStmt.getRange().get().begin.line
                ));

        Range targetRange = null;
        if (targetIf.isPresent()) {
            IfStmt ifStmt = targetIf.get();
            int beginLine = ifStmt.getBegin().get().line;
            int endLine = ifStmt.getCondition().getRange().get().end.line;
            targetRange = new Range(
                    new Position(beginLine, 1),
                    new Position(endLine, Integer.MAX_VALUE)
            );
        } else {
            // 2. Otherwise, fall back to the enclosing statement
            Optional<Statement> stmt = cu.findAll(Statement.class).stream()
                    .filter(s -> s.getRange().isPresent())
                    .filter(s -> {
                        Range r = s.getRange().get();
                        return r.begin.line <= inputLineNumber && r.end.line >= inputLineNumber;
                    })
                    .min(Comparator.comparingInt(
                            s -> s.getRange().get().end.line - s.getRange().get().begin.line
                    ));

            if (stmt.isPresent()) {
                targetRange = stmt.get().getRange().get();
            }
        }

        if (targetRange == null) {
            return new HashSet<>(inputLineNumber);
        }
        Set<Integer> toReturn = new HashSet<>();
        for (int i = targetRange.begin.line; i <= targetRange.end.line; i++) {
            toReturn.add(i);
        }
        return toReturn;
    }
}
