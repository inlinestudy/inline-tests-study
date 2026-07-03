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
import java.util.Optional;

import com.github.javaparser.Range;
import com.github.javaparser.Position;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.visitor.ModifierVisitor;

public class LineFinder {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: need arguments <file-path> <line-number>");
            System.exit(1);
        }

        String filePath = args[0];
        int lineNumber = Integer.parseInt(args[1]);

        try {
            System.out.println(findFirstLine(filePath, lineNumber));
        } catch (Exception e) {
            System.out.println("ERROR");
        }
    }
    
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
}
