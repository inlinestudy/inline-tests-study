package org.ssb4it;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.resolution.declarations.ResolvedDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserFieldDeclaration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

// This program is based on ExLi's Target Statement Finder
public class App {
    public static void main(String[] args) {
        String srcPath = args[0];
        try {
            int lineNumber = Integer.parseInt(args[1]);
            boolean debug;
            if (args.length < 3) {
                debug = false;
            } else {
                debug = Boolean.parseBoolean(args[2]);
            }
            classifyTargetStatement(srcPath, lineNumber, debug);
        } catch (IOException ex) {
            System.err.println("Failed to parse the source file: " + srcPath);
            ex.printStackTrace();
        } catch (NumberFormatException ex) {
            System.err.println("Invalid line number: " + args[1]);
            ex.printStackTrace();
        }
    }

    public static void classifyTargetStatement(String srcPath, int lineNumber, boolean debug) throws IOException {
        String sourceRoot = srcPath.substring(0, srcPath.indexOf("src/main/java") + "src/main/java".length());
        CombinedTypeSolver typeSolver = new CombinedTypeSolver();
        typeSolver.add(new ReflectionTypeSolver());
        if (new File(sourceRoot).exists()) {
            typeSolver.add(new JavaParserTypeSolver(sourceRoot));
        }
        StaticJavaParser.getParserConfiguration().setSymbolResolver(new JavaSymbolSolver(typeSolver));
        CompilationUnit cu = StaticJavaParser.parse(Paths.get(srcPath));
        Context ctx = new Context();
        ctx.srcPath = srcPath;
        ctx.lineNumber = lineNumber;
        Optional<Statement> statement = getStatementAtLine(cu, lineNumber);
        if (statement.isPresent()) {
            if (debug) {
                System.out.println("Found statement at line " + lineNumber + ": " + statement.get());
                System.out.println("Statement type: " + statement.get().getClass().getSimpleName());
            }
            if (statement.get() instanceof ReturnStmt) {
                System.out.println("return statement");
                return;
            }
            if (statement.get() instanceof ThrowStmt) {
                System.out.println("throw statement");
                return;
            }
            if (statement.get() instanceof BlockStmt) {
                Optional<Node> parent = statement.get().getParentNode();
                if (parent.isPresent()) {
                    Node parentNode = parent.get();
                    if (parentNode instanceof IfStmt) {
                        if (isSimpleCondition((IfStmt) parentNode)) {
                            System.out.println("condition too simple");
                        } else {
                            System.out.println("if condition");
                        }
                    } else if (parentNode instanceof WhileStmt) {
                        System.out.println("while statement");
                    } else if (parentNode instanceof ForStmt) {
                        System.out.println("for statement");
                    } else if (parentNode instanceof MethodDeclaration
                            || parentNode instanceof ConstructorDeclaration) {
                        System.out.println("method or constructor declaration");
                    } else {
                        System.out.println("block statement");
                    }
                }
                return;
            }
            if (statement.get() instanceof ExpressionStmt) {
                if (((ExpressionStmt) statement.get()).getExpression().isAssignExpr()
                        || (((ExpressionStmt) statement.get()).getExpression().isVariableDeclarationExpr()
                        && ((ExpressionStmt) statement.get()).getExpression().asVariableDeclarationExpr()
                        .getVariables().stream().anyMatch(v -> v.getInitializer().isPresent()))) {
                    if (((ExpressionStmt) statement.get()).getExpression().isAssignExpr()) {
                        AssignExpr assignExpr = ((ExpressionStmt) statement.get()).getExpression().asAssignExpr();
                        Expression rhs = assignExpr.getValue();
                        if (rhs.isLiteralExpr() || rhs.isNameExpr()) {
                            System.out.println("RHS too simple");
                            return;
                        } else {
                            // RHS should use more than zero variables
                            if (!containsVariables(rhs)) { // Does not have variable
                                System.out.println("RHS too simple");
                                return;
                            }
                        }
                    } else {
                        VariableDeclarationExpr variableDeclarationExpr = ((ExpressionStmt) statement.get()).getExpression().asVariableDeclarationExpr();
                        // Design decision: how to handle int x = 1, y = w + ww;
                        // since we can still check y, I would say keep it as statement with assignment
                        // so RHS too simple only if all variables are too simple
                        boolean notSimple = false;
                        for (VariableDeclarator variableDeclarator : variableDeclarationExpr.getVariables()) {
                            if (variableDeclarator.getInitializer().isPresent()) {
                                Expression rhs = variableDeclarator.getInitializer().get();
                                if (!rhs.isLiteralExpr() && !rhs.isNameExpr() && containsVariables(rhs)) {
                                    // not literal, not just a variable, and contains at least one variable
                                    notSimple = true;
                                }
                            }
                        }
                        if (!notSimple) {
                            System.out.println("RHS too simple");
                            return;
                        }
                    }
                } else {
                    System.out.println("statement without assignment");
                    return;
                }
            } else if (statement.get() instanceof WhileStmt) {
                System.out.println("while condition");
            } else if (statement.get() instanceof ForStmt) {
                System.out.println("for-loop header");
            }
            ClassifyTargetStatementVisitor visitor = new ClassifyTargetStatementVisitor();
            statement.get().accept(visitor, ctx);
            if (!ctx.hasVerdict) {
                if (statement.get() instanceof ExpressionStmt) {
                    if (((ExpressionStmt) statement.get()).getExpression().isAssignExpr()
                            || (((ExpressionStmt) statement.get()).getExpression().isVariableDeclarationExpr()
                            && ((ExpressionStmt) statement.get()).getExpression().asVariableDeclarationExpr()
                            .getVariables().stream().anyMatch(v -> v.getInitializer().isPresent()))) {
                        if (((ExpressionStmt) statement.get()).getExpression().isAssignExpr()) {
                            AssignExpr assignExpr = ((ExpressionStmt) statement.get()).getExpression().asAssignExpr();
                            Expression rhs = assignExpr.getValue();
                            if (rhs.isLiteralExpr() || rhs.isNameExpr()) {
                                System.out.println("RHS too simple");
                            } else {
                                if (!containsVariables(rhs)) { // Does not have variable
                                    System.out.println("RHS too simple");
                                } else {
                                    System.out.println("statement with assignment");
                                }
                            }
                        } else {
                            VariableDeclarationExpr variableDeclarationExpr = ((ExpressionStmt) statement.get()).getExpression().asVariableDeclarationExpr();
                            // Design decision: how to handle int x = 1, y = w + ww;
                            // since we can still check y, I would say keep it as statement with assignment
                            // so RHS too simple only if all variables are too simple
                            boolean notSimple = false;
                            for (VariableDeclarator variableDeclarator : variableDeclarationExpr.getVariables()) {
                                if (variableDeclarator.getInitializer().isPresent()) {
                                    Expression rhs = variableDeclarator.getInitializer().get();
                                    if (!rhs.isLiteralExpr() && !rhs.isNameExpr() && containsVariables(rhs)) {
                                        // not literal, not just a variable, and contains at least one variable
                                        notSimple = true;
                                    }
                                }
                            }
                            if (!notSimple) {
                                System.out.println("RHS too simple");
                            } else {
                                System.out.println("statement with assignment");
                            }
                        }
                    } else {
                        System.out.println("statement without assignment");
                    }
                } else if (statement.get() instanceof IfStmt) {
                    if (isSimpleCondition(statement.get())) {
                        System.out.println("condition too simple");
                    } else {
                        System.out.println("if condition");
                    }
                } else if (statement.get() instanceof WhileStmt) {
                    System.out.println("while condition");
                } else if (statement.get() instanceof ForStmt) {
                    System.out.println("for-loop header");
                } else {
                    System.out.println("other statements");
                }
            }
        } else {
            System.out.println("not a statement");
        }
    }

    public static boolean isSimpleCondition(Statement statement) {
        IfStmt ifStmt = (IfStmt) statement;
        Expression cond = ifStmt.getCondition();
        // Check if an expression contains any non-constant local variables
        class LocalVarVisitor extends VoidVisitorAdapter<Void> {
            boolean found = false;
            @Override
            public void visit(NameExpr n, Void arg) {
                // Try to resolve the symbol and check if it's a local variable and not a constant
                try {
                    ResolvedValueDeclaration resolved = n.resolve();
                    // Only consider variables (not fields, not parameters, not static finals)
                    if (resolved.isVariable()) {
                        // Exclude static final (constants)
                        if (!(resolved.isField() && resolved.asField().isStatic() && (resolved.asField() instanceof JavaParserFieldDeclaration) && ((JavaParserFieldDeclaration) resolved.asField()).getWrappedNode().isFinal())) {
                            found = true;
                        }
                    }
                } catch (Throwable e) {
                    // If we can't resolve, be conservative and skip
                }
                super.visit(n, arg);
            }
        }
        LocalVarVisitor visitor = new LocalVarVisitor();
        try {
            cond.accept(visitor, null);
        } catch (Throwable e) {
            // fallback: if error, treat as too simple
            return true;
        }
        return !visitor.found;
    }

    public static boolean containsVariables(Expression expr) {
        return expr.findAll(Expression.class).stream()
                .anyMatch(e -> {
                    try {
                        if (e instanceof ThisExpr) return true;

                        if (e instanceof ArrayAccessExpr) {
                            return true;
                        }

                        if (e instanceof NameExpr) {
                            ResolvedDeclaration rd = ((NameExpr) e).resolve();
                            return rd instanceof ResolvedValueDeclaration
                                    && !(rd instanceof ResolvedTypeDeclaration)
                                    && !(rd instanceof ResolvedMethodDeclaration);
                        }

                        if (e instanceof FieldAccessExpr) {
                            ResolvedDeclaration rd = ((FieldAccessExpr) e).resolve();
                            return rd instanceof ResolvedValueDeclaration
                                    && !(rd instanceof ResolvedTypeDeclaration)
                                    && !(rd instanceof ResolvedMethodDeclaration);
                        }

                        return false;
                    } catch (RuntimeException ex) {
                        return false; // ignore unresolved nodes
                    }
                });
    }

    public static Optional<Statement> getStatementAtLine(CompilationUnit cu, int lineNumber) {
        return cu.findAll(Statement.class)
                .stream()
                .filter(stmt -> stmt.getRange().isPresent())
                .filter(stmt -> {
                    if (stmt instanceof IfStmt) {
                        IfStmt ifStmt = (IfStmt) stmt;
                        if (ifStmt.getCondition().getRange().isPresent()) {
                            int condBegin = ifStmt.getCondition().getRange().get().begin.line;
                            int condEnd = ifStmt.getCondition().getRange().get().end.line;
                            if (lineNumber >= condBegin && lineNumber <= condEnd) {
                                return true;
                            }
                        }
                    }
                    return lineNumber >= stmt.getRange().get().begin.line
                            && lineNumber <= stmt.getRange().get().end.line;
                })
                .min((s1, s2) -> {
                    boolean s1IsBlock = s1 instanceof BlockStmt;
                    boolean s2IsBlock = s2 instanceof BlockStmt;
                    if (s1IsBlock && !s2IsBlock) return 1;
                    if (!s1IsBlock && s2IsBlock) return -1;
                    int range1 = s1.getRange().get().end.line - s1.getRange().get().begin.line;
                    int range2 = s2.getRange().get().end.line - s2.getRange().get().begin.line;
                    return Integer.compare(range1, range2);
                });
    }
}
