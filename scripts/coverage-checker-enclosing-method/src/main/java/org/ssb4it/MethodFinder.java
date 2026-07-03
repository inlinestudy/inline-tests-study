package org.ssb4it;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;

import java.nio.file.Paths;
import java.util.Optional;
import java.util.List;
import java.util.ArrayList;

public class MethodFinder {
    public static List<Integer> findMethodRange(String filePath, int lineNumber) {
        List<Integer> result = new ArrayList<>();
        try {
            JavaParser javaParser = new JavaParser();
            CompilationUnit compilationUnit = javaParser.parse(Paths.get(filePath)).getResult().orElseThrow(() -> new RuntimeException("Parsing failed"));

            Optional<BodyDeclaration> enclosingMethod = compilationUnit.findFirst(BodyDeclaration.class, declaration ->
                    (declaration instanceof MethodDeclaration || declaration instanceof ConstructorDeclaration) &&
                            declaration.getRange().map(range -> range.begin.line <= lineNumber && range.end.line >= lineNumber).orElse(false)
            );

            if (enclosingMethod.isPresent()) {
                BodyDeclaration method = enclosingMethod.get();
                int startLine = method.getRange().map(range -> range.begin.line).orElse(-1);
                int endLine = method.getRange().map(range -> range.end.line).orElse(-1);
                result.add(startLine);
                result.add(endLine);
            } else {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
        return result;
    }   
}
