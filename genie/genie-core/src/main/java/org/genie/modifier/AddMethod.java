package org.genie.modifier;

import java.util.HashSet;
import java.util.stream.Collectors;

import org.genie.util.Constant;
import org.genie.util.Context;
import org.genie.util.Utils;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;

public class AddMethod extends ModifierVisitor<Context> {

    @Override
    public Visitable visit(final EnumDeclaration n, final Context ctx) {
        String oldClassName = ctx.className;
        ctx.className = n.getNameAsString();
        Visitable ret = super.visit(n, ctx);
        ctx.className = oldClassName;
        return ret;
    }

    @Override
    public Visitable visit(ClassOrInterfaceDeclaration node, Context ctx) {
        String oldClassName = ctx.className;
        ctx.className = node.getNameAsString();

        // Create new method with variables.
        MethodDeclaration method = node.addMethod(Constant.GENERATED_METHOD_NAME, Modifier.Keyword.PUBLIC);
        if (ctx.genericTypes.size() > 0) {
            // Add type parameters to method signature.
            method.setTypeParameters(ctx.genericTypes);
        }
        if (ctx.thrownExceptions.size() > 0) {
            method.setThrownExceptions(ctx.thrownExceptions);
        }
        for (String variable : ctx.logVariablesWithTypeBefore.keySet()) {
            String type = ctx.logVariablesWithTypeBefore.get(variable);
            method.addParameter(type, variable);
        }
        BlockStmt blockStmt = new BlockStmt();
        if (ctx.logVariables) {
            // Log "target-statement-start"
            Statement logStartStmt = buildLogStatement(Constant.TARGET_STMT_START, null, ctx);
            blockStmt.addStatement(logStartStmt);
            // Log variables before target statement.
            for (String variable : ctx.logVariablesWithTypeBefore.keySet()) {
                Statement logStmt = buildLogStatement(Constant.TARGET_STMT_BEFORE, variable, ctx);
                blockStmt.addStatement(logStmt);
            }
        }

        // Check if we need to add type to the variable.
        // We need to add a type for the left hand side, such as `this.a` and `a`.
        HashSet<String> LHSVariables = ctx.logVariablesWithTypeAfter.keySet().stream()
                .collect(Collectors.toCollection(HashSet::new));
        HashSet<String> RHSVariables = ctx.logVariablesWithTypeBefore.keySet().stream()
                .collect(Collectors.toCollection(HashSet::new));
        String[] tokens = ctx.line.split("=");
        for (String variable : LHSVariables) {
            if (!RHSVariables.contains(variable)) {
                // When LHS variable does not appear in RHS, we need to add type.
                String type = ctx.logVariablesWithTypeAfter.get(variable);
                // Add type to the variable.
                for (int i = 0; i < LHSVariables.size() && i < tokens.length; i++) {
                    if (tokens[i].trim().equals(Utils.rename(variable).trim())) {
                        if (!Utils.rename(tokens[i]).contains(" ")) {
                            String declareStmt = type + " " + tokens[i] + ";";
                            try {
                                blockStmt.addStatement(StaticJavaParser.parseStatement(declareStmt));
                            } catch (Exception e) {
                                throw new RuntimeException("Cannot add statement " + declareStmt + " to the method "
                                        + method.getNameAsString() + "\n" + e.getMessage());
                            }
                        }
                        break;
                    }
                }
            }
        }

        // Add target statement.
        try {
            blockStmt.addStatement(ctx.line);
        } catch (Exception e) {
            throw new RuntimeException("Cannot add line " + ctx.line + " to the method " + method.getNameAsString()
                    + "\n" + e.getMessage());
        }
        if (ctx.logVariables) {
            // Log variables after target statement.
            for (String variable : ctx.logVariablesWithTypeAfter.keySet()) {
                Statement logStmt = buildLogStatement(Constant.TARGET_STMT_AFTER, variable, ctx);
                blockStmt.addStatement(logStmt);
            }
            // Log "target-statement-end"
            Statement logEndStmt = buildLogStatement(Constant.TARGET_STMT_END, null, ctx);
            blockStmt.addStatement(logEndStmt);
            // Log "check-coverage"
            Statement checkCoverage = buildLogStatement(Constant.CHECK_COVERAGE, null, ctx);
            blockStmt.addStatement(checkCoverage);
        }
        method.setBody(blockStmt);

        Visitable ret = super.visit(node, ctx);
        ctx.className = oldClassName;
        return ret;
    }

    private static Statement buildLogStatement(String prompt, String variable, Context ctx) {
        String logStmtStr = Constant.LOG_CLASS_NAME + ".logVariableAndGenerateTest("
                + "\"" + prompt + "\""
                + ", " + "\"" + ctx.logPath + "\""
                + ", " + "\"" + ctx.r0TestPath + "\""
                + ", " + "\"" + ctx.r1TestPath + "\""
                + ", " + "\"" + ctx.srcPath + "\""
                + ", " + (ctx.lineNumber)
                + ", " + variable
                + ", " + "\"" + Utils.escapeString(variable) + "\""
                + ", " + ctx.className + ".class"
                + ", " + "\"" + ctx.classesDirectory + "\""
                + ");";
        return StaticJavaParser.parseStatement(logStmtStr);
    }
}
