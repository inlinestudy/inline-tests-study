package org.ssb4it;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;

import com.github.javaparser.Range;
import com.github.javaparser.Position;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.analysis.ICounter;
import org.jacoco.core.tools.ExecFileLoader;

/**
 * This class takes a jacoco.exec file and outputs the coverage information for
 * each line in a specified range of a source file.
 */
public class CoverageChecker {
    private final String relpathToSrc;
    private final int startLine;
    private final int endLine;
    private final File execFile;
    private final File classesDir;
    private ExecFileLoader execFileLoader;

    /** Create a new checker based for the given project. */
    public CoverageChecker(final String relpathToSrc, final int startLine, final int endLine, final File execFile, final File classesDir) {
        this.relpathToSrc = relpathToSrc;
        this.startLine = startLine;
        this.endLine = endLine;
        this.execFile = execFile;
        this.classesDir = classesDir;
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

    public void check(String outputFile) {
        execFileLoader = new ExecFileLoader();
        try {
            execFileLoader.load(execFile);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        String className = relpathToSrc.replace("src/main/java/", "").replace(".java", "").replace(File.separatorChar, '.');
        // System.out.println("Target class: " + className);
        final CoverageBuilder coverageBuilder = new CoverageBuilder();
        final Analyzer analyzer = new Analyzer(execFileLoader.getExecutionDataStore(), coverageBuilder);
        try {
            analyzer.analyzeAll(classesDir);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        IClassCoverage targetClass = null;
        for (IClassCoverage icc : coverageBuilder.getClasses()) {
            // System.out.println("Class: " + icc.getName().replace('/', '.'));
            if (icc.getName().replace('/', '.').equals(className)) {
                targetClass = icc;
                break;
            }
        }

        Map<Integer, Integer> lineMap = new HashMap<>();
        if (targetClass != null) {
            for (int i = targetClass.getFirstLine(); i <= targetClass.getLastLine(); i++) {
                int status = targetClass.getLine(i).getStatus();
                lineMap.put(i, status);
            }
        }

        PrintWriter writer;
        try {
            writer = new PrintWriter(outputFile);
            writer.println("file,line,status");
            for (int i = startLine; i <= endLine; i++) {
                int status = lineMap.get(i);
                writer.println(relpathToSrc + "," + i + "," + getStatus(status));
            }
            writer.close();
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }
    }

    public void check(String csvFilePathStr, String methodCovPathStr, String stmtCovPathStr, int stmtBeginLine) {
        execFileLoader = new ExecFileLoader();
        try {
            execFileLoader.load(execFile);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        String className = relpathToSrc.replace("src/main/java/", "").replace(".java", "").replace(File.separatorChar, '.');
        // System.out.println("Target class: " + className);
        final CoverageBuilder coverageBuilder = new CoverageBuilder();
        final Analyzer analyzer = new Analyzer(execFileLoader.getExecutionDataStore(), coverageBuilder);
        try {
            analyzer.analyzeAll(classesDir);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        IClassCoverage targetClass = null;
        for (IClassCoverage icc : coverageBuilder.getClasses()) {
            // System.out.println("Class: " + icc.getName().replace('/', '.'));
            if (icc.getName().replace('/', '.').equals(className)) {
                targetClass = icc;
                break;
            }
        }

        Map<Integer, Integer> lineMap = new HashMap<>();
        if (targetClass != null) {
            for (int i = targetClass.getFirstLine(); i <= targetClass.getLastLine(); i++) {
                int status = targetClass.getLine(i).getStatus();
                lineMap.put(i, status);
            }
        }

        List<Integer> methodLineStatus = new ArrayList<>();
        int statementStatus = ICounter.NOT_COVERED;
        PrintWriter writer;
        try {
            writer = new PrintWriter(csvFilePathStr);
            writer.println("file,line,status");
            for (int i = startLine; i <= endLine; i++) {
                int status = lineMap.get(i);
                methodLineStatus.add(status);
                if (i == stmtBeginLine) {
                    statementStatus = status;
                }
                writer.println(relpathToSrc + "," + i + "," + getStatus(status));
            }
            writer.close();
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }
        // Write result for method coverage
        try {
            PrintWriter methodWriter = new PrintWriter(methodCovPathStr);
            int countFullyCovered = 0;
            int countPartlyCovered = 0;
            int countNotCovered = 0;
            int countEmpty = 0;
            for (int status : methodLineStatus) {
                switch (status) {
                    case ICounter.FULLY_COVERED:
                        countFullyCovered++;
                        break;
                    case ICounter.PARTLY_COVERED:
                        countPartlyCovered++;
                        break;
                    case ICounter.NOT_COVERED:
                        countNotCovered++;
                        break;
                    case ICounter.EMPTY:
                        countEmpty++;
                        break;
                    default:
                        break;
                }
            }
            if (countFullyCovered == 0 && countPartlyCovered == 0) {
                methodWriter.println("NOT_COVERED");
            } else if (countPartlyCovered == 0 && countNotCovered == 0) {
                methodWriter.println("FULLY_COVERED");
            } else if (countFullyCovered != 0 || countPartlyCovered != 0) {
                methodWriter.println("PARTLY_COVERED");
            } else {
                methodWriter.println("UNKNOWN");
            }
            methodWriter.close();
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }
        // Write result for statement coverage
        try {
            PrintWriter stmtWriter = new PrintWriter(stmtCovPathStr);
            stmtWriter.println(getStatus(statementStatus));
            stmtWriter.close();
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }
    }

    private String getStatus(int status) {
        switch (status) {
            case ICounter.FULLY_COVERED:
                return "FULLY_COVERED";
            case ICounter.NOT_COVERED:
                return "NOT_COVERED";
            case ICounter.PARTLY_COVERED:
                return "PARTLY_COVERED";
            case ICounter.EMPTY:
                return "EMPTY";
            default:
                return "UNKNOWN";
        }
    }
}
