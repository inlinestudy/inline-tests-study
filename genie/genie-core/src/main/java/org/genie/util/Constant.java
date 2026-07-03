package org.genie.util;

import java.util.Arrays;
import java.util.List;

import com.github.javaparser.ast.expr.AssignExpr;

public class Constant {
    public final static List<AssignExpr.Operator> COMPOUND_ASSIGN_OPERATORS = Arrays.asList(AssignExpr.Operator.PLUS,
            AssignExpr.Operator.MINUS,
            AssignExpr.Operator.MULTIPLY, AssignExpr.Operator.DIVIDE, AssignExpr.Operator.BINARY_AND,
            AssignExpr.Operator.BINARY_OR, AssignExpr.Operator.XOR, AssignExpr.Operator.REMAINDER,
            AssignExpr.Operator.LEFT_SHIFT, AssignExpr.Operator.SIGNED_RIGHT_SHIFT,
            AssignExpr.Operator.UNSIGNED_RIGHT_SHIFT);

    public final static String TASK_LINE = "line";
    public final static String TASK_METHOD = "method";
    public final static String TASK_VARIABLES = "variables";
    public final static String TASK_VARIABLES_WITH_TYPE = "variables-with-type";
    public final static String TASK_ADD_METHOD = "add-method";

    public final static String TARGET_STMT_BEFORE = "target-statement-before";
    public final static String TARGET_STMT_AFTER = "target-statement-after";
    public final static String TARGET_STMT_START = "target-statement-start";
    public final static String TARGET_STMT_END = "target-statement-end";
    public final static String CHECK_COVERAGE = "check-coverage";
    public final static String LOG_CLASS_NAME = "org.raninline.InstrumentHelper";

    public final static String GENERATED_METHOD_NAME = "targetStmtGenerated";
    public final static String COND_EXPR_RES = "condExprRes";
}
