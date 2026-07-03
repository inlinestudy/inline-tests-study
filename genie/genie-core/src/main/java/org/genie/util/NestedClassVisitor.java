package org.genie.util;

import java.util.Iterator;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.utils.Pair;

public class NestedClassVisitor extends VoidVisitorAdapter<Context> {

    @Override
    public void visit(final ClassOrInterfaceDeclaration n, final Context ctx) {
        ctx.nestedClassStack.push(n.getNameAsString());
        if (n.isNestedType() && !n.isInterface()
                && !n.isEnumDeclaration() && !n.isAnnotationDeclaration()
                && !n.isPrivate()) {
            StringBuilder nestedClassName = new StringBuilder();
            Iterator it = ctx.nestedClassStack.descendingIterator();
            while (it.hasNext()) {
                nestedClassName.append(".");
                nestedClassName.append(it.next());
            }
            if (n.getParentNode().isPresent()) {
                ctx.innerClassToLineNum.put(nestedClassName.toString(),
                        new Pair(n.getParentNode().get().getRange().get().begin.line,
                                n.getParentNode().get().getRange().get().end.line));
            } else {
                ctx.innerClassToLineNum.put(nestedClassName.toString(),
                        new Pair(n.getRange().get().begin.line, n.getRange().get().end.line));
            }
        }
        super.visit(n, ctx);
        ctx.nestedClassStack.pop();
    }
}
