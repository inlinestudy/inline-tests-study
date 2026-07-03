package org.genie.collector;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.genie.modifier.AddMethod;
import org.genie.modifier.RenameHelper;
import org.genie.util.Constant;
import org.genie.util.Context;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.UnionType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.utils.Pair;

public class CollectLine extends VoidVisitorAdapter<Context> {
    PackageDeclaration packageDeclaration;
    NodeList<ImportDeclaration> imports;

    @Override
    public void visit(final CompilationUnit n, final Context ctx) {
        packageDeclaration = n.getPackageDeclaration().orElse(null);
        imports = n.getImports();
        super.visit(n, ctx);
    }

    @Override
    public void visit(final ExpressionStmt n, final Context ctx) {
        if (ctx.task.equals(Constant.TASK_ADD_METHOD)) {
            if (n.getBegin().isPresent()
                    && ctx.lineNumbers.contains(n.getBegin().get().line)) {
                // Target statement should contain AssignExpr or VariableDeclarationExpr
                recordTargetStmt(n, ctx);
            } else {
                super.visit(n, ctx);
            }
        } else {
            if (n.getBegin().isPresent() && n.getBegin().get().line == ctx.lineNumber) {
                if (ctx.task.equals(Constant.TASK_LINE)) {
                    String line = n.toString();
                    try {
                        Files.write(Paths.get(ctx.outputFilePath), line.getBytes());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } else if (ctx.task.equals(Constant.TASK_VARIABLES)) {
                    findVariables(n, ctx);
                } else if (ctx.task.equals(Constant.TASK_VARIABLES_WITH_TYPE)) {
                    findVariablesWithType(n, ctx);
                }
            } else {
                super.visit(n, ctx);
            }
        }
        // We do not need to visit the expression `super.visit(n,
        // ctx);` when the target statement is found because it is
        // possible that there are expression statements inside this
        // expression statement. For example, boolean found =
        // dependencies.stream().anyMatch(d ->
        // d.getRef().equals(dependency.getRef()));
    }

    @Override
    public void visit(final IfStmt n, final Context ctx) {
        if (n.getBegin().isPresent() && n.getBegin().get().line == ctx.lineNumber) {
            if (ctx.task.equals(Constant.TASK_LINE)) {
                String line = "if (" + n.getCondition().toString() + ")";
                try {
                    Files.write(Paths.get(ctx.outputFilePath), line.getBytes());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else if (ctx.task.equals(Constant.TASK_VARIABLES)) {
                findVariables(n.getCondition(), ctx);
            } else if (ctx.task.equals(Constant.TASK_VARIABLES_WITH_TYPE)) {
                findVariablesWithType(n.getCondition(), ctx);
            }
        }

        if (ctx.task.equals(Constant.TASK_ADD_METHOD) && n.getBegin().isPresent()
                && ctx.lineNumbers.contains(n.getBegin().get().line)) {
            recordTargetStmt(n.getCondition(), ctx);
        }
        // n.getCondition().accept(this, ctx);
        n.getElseStmt().ifPresent(l -> l.accept(this, ctx));
        n.getThenStmt().accept(this, ctx);
        // n.getComment().ifPresent(l -> l.accept(this, ctx));
    }

    @Override
    public void visit(final MethodDeclaration n, final Context ctx) {
        // Generic type e.g., <T> T get(T t).
        // This is used to rename the generic type to Object when we extract the target
        // statement into a new method.
        ctx.genericTypes.clear();
        ctx.genericTypes = n.getTypeParameters();
        super.visit(n, ctx);
    }

    private void recordTargetStmt(Node n, Context ctx) {
        // Find the checked exceptions that we need to throw.
        findCheckedException(n, ctx);
        ctx.lineNumber = n.getBegin().get().line;
        // Find the variables that we need to log before and after the target statement.
        // Find the variables that we need to rename.
        FindVariableWithType findVariableWithType = new FindVariableWithType();
        n.accept(findVariableWithType, ctx);
        // Rename the method calls and fields.
        RenameHelper renameHelper = new RenameHelper();
        n.accept(renameHelper, ctx);
        if (n instanceof Expression) {
            ctx.line = "boolean " + Constant.COND_EXPR_RES + " = (" + n.toString() + ");";
            ctx.logVariablesWithTypeAfter.put(Constant.COND_EXPR_RES, "boolean");
        } else {
            ctx.line = n.toString();
        }
        extractMethodIntoNewClass(ctx);
        resetContext(ctx);
    }

    private void findCheckedException(Node n, Context ctx) {
        // Find the parent method declaration.
        Node parent = n;
        while (parent != null && !(parent instanceof MethodDeclaration)) {
            parent = parent.getParentNode().orElse(null);
        }
        if (parent != null) {
            MethodDeclaration methodDeclaration = (MethodDeclaration) parent;
            if (methodDeclaration.getThrownExceptions() != null && methodDeclaration.getThrownExceptions().size() > 0) {
                ctx.thrownExceptions.addAll(methodDeclaration.getThrownExceptions());
            }
        }

        // Find the parent try statement.
        parent = n;
        while (parent != null && !(parent instanceof TryStmt)) {
            parent = parent.getParentNode().orElse(null);
        }
        if (parent != null) {
            TryStmt tryStmt = (TryStmt) parent;
            if (tryStmt.getCatchClauses() != null && tryStmt.getCatchClauses().size() > 0) {
                for (CatchClause catchClause : tryStmt.getCatchClauses()) {
                    Type typeInCatch = catchClause.getParameter().getType();
                    if (typeInCatch instanceof UnionType) {
                        UnionType unionType = (UnionType) typeInCatch;
                        for (Type type : unionType.getElements()) {
                            ctx.thrownExceptions.add(type.asReferenceType());
                        }
                    } else {
                        ctx.thrownExceptions.add(catchClause.getParameter().getType().asReferenceType());
                    }
                }
            }
        }
    }

    private void findVariables(Node n, Context ctx) {
        FindVariable visitor = new FindVariable();
        n.accept(visitor, ctx);
    }

    private void findVariablesWithType(Node n, Context ctx) {
        FindVariableWithType visitor = new FindVariableWithType();
        n.accept(visitor, ctx);
    }

    private void extractMethodIntoNewClass(Context ctx) {
        String outputFilePath = ctx.srcPath.replace(".java", "_" + String.valueOf(ctx.lineNumber) + ".java");
        File outputFile = new File(outputFilePath);
        try {
            CompilationUnit cu = new CompilationUnit();
            String newClassName = outputFile.toPath().getFileName().toString().replace(".java", "");
            // Copy package declaration.
            cu.setPackageDeclaration(packageDeclaration);

            ClassOrInterfaceDeclaration classDeclaration = cu.addClass(newClassName);
            classDeclaration.setModifiers(Modifier.Keyword.PUBLIC);
            AddMethod addMethod = new AddMethod();
            cu.accept(addMethod, ctx);
            // Rename the method calls and fields.
            RenameHelper renameHelper = new RenameHelper();
            cu.accept(renameHelper, ctx);
            // Copy the import statements and add non-private classes
            // to imports. It is possible that two classes have the
            // same name but in different packages. So we comment out
            // the code.
            Set<String> importedClasses = new HashSet<>();
            for (ImportDeclaration importDeclaration : imports) {
                String clazz = getClassName(importDeclaration.getNameAsString());
                importedClasses.add(clazz);
            }
            for (String clazz : ctx.innerClassToLineNum.keySet()) {
                Pair<Integer, Integer> lineNum = ctx.innerClassToLineNum.get(clazz);
                String className = getClassName(clazz);
                if (lineNum.a > ctx.lineNumber || lineNum.b < ctx.lineNumber || importedClasses.contains(className)) {
                    continue;
                }
                importedClasses.add(className);
                imports.add(
                        new ImportDeclaration(
                                packageDeclaration.getNameAsString() + clazz, false,
                                false));
            }
            cu.setImports(imports);
            Files.write(outputFile.toPath(), cu.toString().getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void resetContext(Context ctx) {
        ctx.line = null;
        ctx.logVariablesWithTypeBefore = new HashMap<>();
        ctx.logVariablesWithTypeAfter = new HashMap<>();
    }

    private static String getClassName(String classWithPackage) {
        String[] tokens = classWithPackage.split("\\.");
        return tokens[tokens.length - 1];
    }
}
