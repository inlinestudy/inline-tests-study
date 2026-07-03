package org.ssb4it;

import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.BinaryExpr.Operator;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.ast.Node;
import com.github.javaparser.Range;

import java.util.Arrays;
import java.util.List;

public class ClassifyTargetStatementVisitor extends VoidVisitorAdapter<Context> {
    final static List<String> STRING_MANIPULATION = Arrays.asList("split", "substring", "indexOf", "format",
            "replace");
    final static List<String> REGEX = Arrays.asList("matches", "find", "group");
    final static List<String> STREAM = Arrays.asList("stream");

    public static boolean containsLine(Node node, Context ctx) {
        if (!node.hasRange()) {
            return false;
        }
        Range range = node.getRange().get();
        return ctx.lineNumber >= range.begin.line && ctx.lineNumber <= range.end.line;
    }

    public static boolean moreSpecificThanCurrent(int begin, int end, Context ctx) {
        if (ctx.begin == -1 && ctx.end == -1) {
            ctx.begin = begin;
            ctx.end = end;
            return true;
        }
        if (begin > ctx.begin || end < ctx.end) {
            ctx.begin = begin;
            ctx.end = end;
            return true;
        }
        return false;
    }

    public void visit(final BinaryExpr binaryExpr, final Context ctx) {
        if (!containsLine(binaryExpr, ctx)) {
            return;
        }
        Operator operator = binaryExpr.getOperator();
        Expression left = binaryExpr.getLeft();
        Expression right = binaryExpr.getRight();
        // bit manipulation
        if (binaryExpr.getOperator().equals(BinaryExpr.Operator.BINARY_AND) ||
                binaryExpr.getOperator().equals(BinaryExpr.Operator.BINARY_OR) ||
                binaryExpr.getOperator().equals(BinaryExpr.Operator.XOR) ||
                binaryExpr.getOperator().equals(BinaryExpr.Operator.LEFT_SHIFT) ||
                binaryExpr.getOperator().equals(BinaryExpr.Operator.SIGNED_RIGHT_SHIFT) ||
                binaryExpr.getOperator().equals(BinaryExpr.Operator.UNSIGNED_RIGHT_SHIFT)) {
            System.out.println("target stmt bit");
            ctx.hasVerdict = true;
            return;
        }
        // string concatenation
        if (operator.equals(BinaryExpr.Operator.PLUS)
                && (left.calculateResolvedType().isReferenceType()
                        && left.calculateResolvedType().describe().equals("java.lang.String")
                    || right.calculateResolvedType().isReferenceType()
                        && right.calculateResolvedType().describe().equals("java.lang.String")
                )
        ) {
            System.out.println("target stmt string concatenation");
            // ctx.hasVerdict = true;
            // return;
        }
        // arithmetic operations
        if (operator.equals(BinaryExpr.Operator.PLUS)
                || operator.equals(BinaryExpr.Operator.MINUS)
                || operator.equals(BinaryExpr.Operator.MULTIPLY)
                || operator.equals(BinaryExpr.Operator.DIVIDE)
                || operator.equals(BinaryExpr.Operator.REMAINDER)) {
            System.out.println("target stmt arithmetic operation");
            // ctx.hasVerdict = true;
            // return;
        }
        // boolean operations
        if (operator.equals(BinaryExpr.Operator.AND)
                || operator.equals(BinaryExpr.Operator.OR)
                || operator.equals(BinaryExpr.Operator.XOR)) {
            System.out.println("target stmt boolean operation");
            // ctx.hasVerdict = true;
            // return;
        }
        // comparison operations
        if (operator.equals(BinaryExpr.Operator.EQUALS)
                || operator.equals(BinaryExpr.Operator.NOT_EQUALS)
                || operator.equals(BinaryExpr.Operator.LESS)
                || operator.equals(BinaryExpr.Operator.LESS_EQUALS)
                || operator.equals(BinaryExpr.Operator.GREATER)
                || operator.equals(BinaryExpr.Operator.GREATER_EQUALS)) {
            System.out.println("target stmt comparison operation");
            // ctx.hasVerdict = true;
            // return;
        }
        super.visit(binaryExpr, ctx);
        if (!ctx.hasVerdict && moreSpecificThanCurrent(
                binaryExpr.getBegin().get().line,
                binaryExpr.getEnd().get().line, ctx)) {
            System.out.println("generic binary expression");
        }
        return;
    }

    public void visit(final AssignExpr assignExpr, final Context ctx) {
        if (!containsLine(assignExpr, ctx)) {
            return;
        }
        if (assignExpr.getOperator().equals(AssignExpr.Operator.BINARY_AND) ||
                assignExpr.getOperator().equals(AssignExpr.Operator.BINARY_OR) ||
                assignExpr.getOperator().equals(AssignExpr.Operator.XOR) ||
                assignExpr.getOperator().equals(AssignExpr.Operator.LEFT_SHIFT) ||
                assignExpr.getOperator().equals(AssignExpr.Operator.SIGNED_RIGHT_SHIFT) ||
                assignExpr.getOperator().equals(AssignExpr.Operator.UNSIGNED_RIGHT_SHIFT)) {
            System.out.println("target stmt bit");
            ctx.hasVerdict = true;
            return;
        }
        super.visit(assignExpr, ctx);
//        if (!ctx.hasVerdict && moreSpecificThanCurrent(
//                assignExpr.getBegin().get().line,
//                assignExpr.getEnd().get().line, ctx)) {
//            System.out.println("generic assignment or declaration");
//        }
    }

    public void visit(final MethodCallExpr methodCallExpr, final Context ctx) {
        if (!containsLine(methodCallExpr, ctx)) {
            return;
        }
        if (methodCallExpr.getScope().isPresent()) {
            // regex
            String scope = methodCallExpr.getScope().get().toString();
            if (REGEX.contains(methodCallExpr.getNameAsString())) {
                System.out.println("target stmt regex");
                ctx.hasVerdict = true;
                return;
            }

            // string manipulation
            if ("String".equals(scope)
                    || STRING_MANIPULATION.contains(methodCallExpr.getNameAsString())) {
                System.out.println("target stmt string");
                ctx.hasVerdict = true;
                return;
            }

            // stream
            if (scope.equals("Stream")
                    && methodCallExpr.getNameAsString().equals("of")) {
                System.out.println("target stmt stream");
                ctx.hasVerdict = true;
                return;
            }
        }
        // stream
        if (STREAM.contains(methodCallExpr.getNameAsString())) {
            System.out.println("target stmt stream");
            ctx.hasVerdict = true;
            return;
        }
        super.visit(methodCallExpr, ctx);
//        if (!ctx.hasVerdict) {
//            System.out.println("generic method call");
//        }
    }

    public void visit(ObjectCreationExpr ObjectCreationExpr, Context ctx) {
        // skip anonymous class
        if (ObjectCreationExpr.getAnonymousClassBody().isPresent()) {
            return;
        }
        super.visit(ObjectCreationExpr, ctx);
    }

    public void visit(ReturnStmt returnStmt, Context ctx) {
        if (!containsLine(returnStmt, ctx)) {
            return;
        }
        System.out.println("return stmt");
    }
}
