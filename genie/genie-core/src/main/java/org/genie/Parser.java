package org.genie;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.stream.Collectors;

import org.genie.collector.CollectLine;
import org.genie.collector.CollectMethod;
import org.genie.util.Constant;
import org.genie.util.Context;
import org.genie.util.NestedClassVisitor;
import org.genie.util.TypeResolver;
import org.genie.util.TypeResolverUtil;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;

public class Parser {

    /**
     * Collect method that contains the line number.
     * 
     * @param srcPath
     * @param lineNumber
     * @param outputFilePath
     * @throws IOException
     */
    public static void collectMethod(String srcPath, int lineNumber, String outputFilePath) throws IOException {
        CompilationUnit cu = StaticJavaParser.parse(Paths.get(srcPath));
        Context ctx = new Context();
        ctx.lineNumber = lineNumber;
        ctx.srcPath = srcPath;
        ctx.outputFilePath = outputFilePath;
        ctx.task = Constant.TASK_METHOD;
        CollectMethod visitor = new CollectMethod();
        cu = (CompilationUnit) cu.accept(visitor, ctx);
    }

    /**
     * Collect statement that starts with the line number.
     * 
     * @param srcPath
     * @param lineNumber
     * @param outputFilePath
     * @param keepInlineComment
     * @throws IOException
     */
    public static void collectLine(String srcPath, int lineNumber, String outputFilePath, boolean keepInlineComment)
            throws IOException {
        if (!keepInlineComment) {
            // Ignore comments in source code.
            StaticJavaParser.getParserConfiguration().setAttributeComments(false);
        }
        CompilationUnit cu = StaticJavaParser.parse(Paths.get(srcPath));
        Context ctx = new Context();
        ctx.lineNumber = lineNumber;
        ctx.srcPath = srcPath;
        ctx.outputFilePath = outputFilePath;
        ctx.task = Constant.TASK_LINE;
        CollectLine visitor = new CollectLine();
        cu.accept(visitor, ctx);
    }

    /**
     * Collect variables that are used in the line number.
     * 
     * @param srcPath
     * @param lineNumber
     * @param outputPath
     * @throws IOException
     */
    public static void collectVariables(String srcPath, int lineNumber, String outputPath) throws IOException {
        // Ignore comments.
        StaticJavaParser.getParserConfiguration().setAttributeComments(false);
        CompilationUnit cu = StaticJavaParser.parse(Paths.get(srcPath));
        Context ctx = new Context();
        ctx.srcPath = srcPath;
        ctx.lineNumber = lineNumber;
        ctx.outputFilePath = outputPath;
        ctx.task = Constant.TASK_VARIABLES;
        CollectLine visitor = new CollectLine();
        cu.accept(visitor, ctx);
        String res = String.join(",", ctx.logVariablesBefore) + System.lineSeparator()
                + String.join(",", ctx.logVariablesAfter);
        Files.write(Paths.get(outputPath), res.getBytes());
    }

    /**
     * Collect variables that are used in the line number with their types.
     * 
     * @param srcPath
     * @param lineNumber
     * @param depFilePath
     * @param appSrcPath
     * @param outputPath
     * @throws IOException
     */
    public static void collectVariablesWithType(String srcPath, int lineNumber, String depFilePath, String appSrcPath,
            String outputPath) throws IOException {
        if (depFilePath != null) {
            TypeResolverUtil.depClassPaths = new String(Files.readAllBytes(Paths.get(depFilePath)));
        }
        TypeResolverUtil.appSrcPath = appSrcPath;
        TypeResolver.setup();
        // Ignore comments in source code.
        StaticJavaParser.getParserConfiguration().setAttributeComments(false);
        CompilationUnit cu = StaticJavaParser.parse(Paths.get(srcPath));
        Context ctx = new Context();
        ctx.srcPath = srcPath;
        ctx.lineNumber = lineNumber;
        ctx.outputFilePath = outputPath;
        ctx.task = Constant.TASK_VARIABLES_WITH_TYPE;
        CollectLine visitor = new CollectLine();
        cu.accept(visitor, ctx);
        String res = prettyPrintMap(ctx.logVariablesWithTypeBefore) + System.lineSeparator()
                + prettyPrintMap(ctx.logVariablesWithTypeAfter);
        Files.write(Paths.get(outputPath), res.getBytes());
    }

    private static String prettyPrintMap(Map<String, String> map) {
        // Key is variable name, value is variable type.
        return map.entrySet()
                .stream()
                .map(entry -> entry.getKey() + ":" + entry.getValue())
                .collect(Collectors.joining("@"));
    }

    /**
     * Collect variables in the line number and add a method with these variables as
     * arguments.
     * 
     * @param srcPath
     * @param lineNumber
     * @param outputPath
     * @param depsFilePath
     * @param appSrcPath
     * @param logPath
     * @param r1TestPath
     * @param classesDirectory
     * @throws IOException
     */
    public static void extractStmtIntoNewMethod(String srcPath, String[] lineNumbers,
            String depFilePath,
            String appSrcPath, String logPath, String r0TestPath, String r1TestPath, String classesDirectory,
            String logVariables)
            throws IOException {
        if (depFilePath != null) {
            TypeResolverUtil.depClassPaths = new String(Files.readAllBytes(Paths.get(depFilePath)));
        }
        TypeResolverUtil.appSrcPath = appSrcPath;
        TypeResolver.setup();

        // Ignore comments.
        StaticJavaParser.getParserConfiguration().setAttributeComments(false);
        CompilationUnit cu = StaticJavaParser.parse(Paths.get(srcPath));
        Context ctx = new Context();
        ctx.srcPath = srcPath;
        for (String lineNumber : lineNumbers) {
            ctx.lineNumbers.add(Integer.parseInt(lineNumber.trim()));
        }
        ctx.logPath = logPath;
        ctx.r0TestPath = r0TestPath;
        ctx.r1TestPath = r1TestPath;
        ctx.classesDirectory = classesDirectory;
        if (logVariables.equals("true") || logVariables.equals("True")) {
            ctx.logVariables = true;
        } else if (logVariables.equals("false") || logVariables.equals("False")) {
            ctx.logVariables = false;
        } else {
            throw new RuntimeException("Invalid log variables value: " + logVariables);
        }
        ctx.task = Constant.TASK_ADD_METHOD;
        // Find the primary class name.
        ctx.primaryClass = cu.getPrimaryTypeName().get();
        // List the names of nested classes in this file.
        cu.accept(new NestedClassVisitor(), ctx);
        // Find variables, extract target statements into methods, and add methods into
        // new classes.
        CollectLine visitor = new CollectLine();
        cu.accept(visitor, ctx);
    }
}