package org.genie.collector;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.genie.util.Context;

import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;

public class CollectMethod extends ModifierVisitor<Context>{
    @Override
    public Visitable visit(final MethodDeclaration n, final Context ctx){
        if (n.getBegin().get().line <= ctx.lineNumber && n.getEnd().get().line >= ctx.lineNumber) {
            String method = n.getDeclarationAsString() + n.getBody().get().toString();
            // save the method to the output file
            try {
                Files.write(Paths.get(ctx.outputFilePath), method.getBytes());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return super.visit(n, ctx);
    }

    @Override
    public Visitable visit(final ConstructorDeclaration n, final Context ctx) {
        if (n.getBegin().get().line <= ctx.lineNumber && n.getEnd().get().line >= ctx.lineNumber) {
            String method = n.getDeclarationAsString() + n.getBody().toString();
            // save the method to the output file
            try {
                Files.write(Paths.get(ctx.outputFilePath), method.getBytes());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return super.visit(n, ctx);
    }
}
