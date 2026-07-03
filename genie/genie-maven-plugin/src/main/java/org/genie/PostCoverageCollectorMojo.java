package org.genie;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.genie.util.Utils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Collect coverage information for inline tests post R2 generation. */
@Mojo(name = "post-coverage-collector", requiresDependencyResolution = ResolutionScope.TEST)
public class PostCoverageCollectorMojo extends ReducerMojo {
    @Parameter(property = "postItestsFile")
    private String postItestsFile;

    /** Decides whether to skip transplantation, in the case that the source code is already transplanted. */
    @Parameter(property = "skipTransplant")
    private boolean skipTransplant;

    // TODO: Remove this
    @Parameter(property = "originalSource")
    private String originalSource;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        initialize();
        String postCoverageCollectionDir = genieDir + File.separator + "post-coverage-collection";

        if (!skipTransplant) {
            transplant(postItestsFile, DEV_NULL, false, DEV_NULL);
        }
        // Parse
        String postSrc = postCoverageCollectionDir + File.separator + "src";
        new File(postSrc).mkdirs();
        // TODO: Only supports 1 file for the time being.
        parseInlineTests(originalSource, postSrc, logDir + File.separator + "post-parse.txt");
        String postBin = postCoverageCollectionDir + File.separator + "bin";
        new File(postBin).mkdirs();
        // Instrument
        try {
            List<String> postSrcs = Files.walk(Paths.get(postSrc)).filter(Files::isRegularFile).filter(
                            path -> path.getFileName().toString().endsWith(".java"))
                    .map(Path::toAbsolutePath).map(Path::toString).collect(Collectors.toList());
            String callHead = "org.raninline.InstrumentHelper.logVariableAndGenerateTest(\"";
            for (String postSrcFile : postSrcs) {
                CompilationUnit cu = StaticJavaParser.parse(Paths.get(postSrcFile));
                cu.accept(new ModifierVisitor<String>() {
                    @Override
                    public Visitable visit(MethodDeclaration m, String context) {
                        BlockStmt body = m.getBody().orElse(new BlockStmt());
                        String callBody = "\", \"null\", \"/dev/null\", \"/dev/null\", \"" + originalSource
                                + "\", 0, null, \"null\", " + context + ", \"" + postBin + "\");";
                        body.addStatement(0,
                                StaticJavaParser.parseStatement(callHead  + "target-statement-start" + callBody));
                        int lastIndex = body.getStatements().size();
                        body.addStatement(lastIndex,
                                StaticJavaParser.parseStatement(callHead  + "target-statement-after" + callBody));
                        body.addStatement(
                                StaticJavaParser.parseStatement(callHead  + "target-statement-end" + callBody));
                        body.addStatement(StaticJavaParser.parseStatement(callHead  + "check-coverage" + callBody));
                        return super.visit(m, context);
                    }
                }, Paths.get(postSrcFile).getFileName().toString().replace(".java", ".class"));
                PrintWriter pw = new PrintWriter(postSrcFile);
                pw.println(cu);
                pw.flush();
                pw.close();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        // Compile
        List<String> command = new ArrayList<>();
        command.addAll(Arrays.asList("javac", "-cp", getDepsFileContent() + File.pathSeparator + TARGET_CLASSES
                + File.pathSeparator + postSrc, "-d", postBin));
        try {
            List<String> postSrcs = Files.walk(Paths.get(postSrc)).filter(Files::isRegularFile).filter(
                    path -> path.getFileName().toString().endsWith(".java"))
                    .map(Path::toAbsolutePath).map(Path::toString).collect(Collectors.toList());
            command.addAll(postSrcs);
            Utils.runSubprocess(command, basedir, new File(logDir + File.separator + "post-compile.txt"), 0, true);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        // Execute
        Set<String> packages = new HashSet<>();
        try (Stream<Path> stream = Files.walk(Paths.get(postBin))) {
            packages = stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".class"))
                    .filter(path -> !path.equals(Paths.get(postBin)))
                    .map(Path::toString)
                    .map(path -> path.split(postBin)[1]
                            // Substring starts from 1 to remove the extra separator character.
                            .substring(1, path.split(postBin)[1].lastIndexOf(File.separator))
                            .replace(File.separatorChar, '.'))
                    .collect(Collectors.toSet());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        command = new ArrayList<>();
        command.addAll(Arrays.asList("java", "-javaagent:" + genieDir + File.separator + "jars" + File.separator
                + JACOCO_AGENT_JAR, "-jar", genieDir + File.separator + "jars" + File.separator + JUNIT_STANDALONE_JAR,
                "-cp", genieDir + File.separator + "jars" + File.separator + JACOCO_AGENT_JAR + File.pathSeparator
                        + getDepsFileContent().replace(this.testClassesDirectory.toString(), "") + File.pathSeparator
                        + postBin, "--select-package"));
        command.addAll(packages);
        Utils.runSubprocess(command, basedir, new File(logDir + File.separator + "post-execute.txt"), 0, true);
    }
}
