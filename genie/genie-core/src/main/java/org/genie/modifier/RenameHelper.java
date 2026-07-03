package org.genie.modifier;

import org.genie.util.Context;
import org.genie.util.Utils;

import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;

public class RenameHelper extends ModifierVisitor<Context> {
    @Override
    public Visitable visit(FieldAccessExpr n, Context ctx) {
        if (ctx.rename.contains(n)) {
            return new NameExpr(Utils.rename(n.toString()));
        }
        return super.visit(n, ctx);
    }

    @Override
    public Visitable visit(MethodCallExpr n, Context ctx) {
        if (ctx.rename.contains(n)) {
            return new NameExpr(Utils.rename(n.toString()));
        }
        return super.visit(n, ctx);
    }

    @Override
    public Visitable visit(Parameter n, Context ctx) {
        if (!Utils.isValidVariableName(n.getNameAsString())) {
            return new Parameter(n.getType(), Utils.rename(n.getNameAsString()));
        }
        return super.visit(n, ctx);
    }

    @Override
    public Visitable visit(AssignExpr n, Context ctx) {
        if (ctx.rename.contains(n.getTarget())) {
            return super.visit(new AssignExpr(new NameExpr(Utils.rename(n.getTarget().toString())), n.getValue(), n.getOperator()), ctx);
        }
        return super.visit(n, ctx);
    }

    @Override
    public Visitable visit(ArrayAccessExpr n, Context ctx) {
        if (ctx.rename.contains(n)) {
            return super.visit(new NameExpr(Utils.rename(n.toString())), ctx);
        }
        return super.visit(n, ctx);
    }
}
