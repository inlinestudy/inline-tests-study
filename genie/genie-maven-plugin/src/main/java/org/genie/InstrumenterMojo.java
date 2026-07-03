package org.genie;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.genie.util.Utils;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The instrumenter mojo is similar to the extract mojo in that it also extracts target statements out of their context,
 * but additionally they instrument statements from raninline/exli to the extracted program, so that during execution,
 * inline tests will be constructed on-the-fly.
 */
@Mojo(name = "instrumenter", requiresDependencyResolution = ResolutionScope.TEST)
public class InstrumenterMojo extends GeneratorMojo {
    /**
     * Executes the main functionality of Genie instrumenter.
     * @throws MojoExecutionException
     * @throws MojoFailureException
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        super.execute();
        getLog().info(LOG_LABEL + SPACE + TOP_LEVEL_BAR + "Genie instrumenter started." + TOP_LEVEL_BAR);

        // extract-generate-instrument is more efficient than extract-instrument-generate.
        for (Iterator<Map.Entry<String, Set<Integer>>> it = inputs.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, Set<Integer>> entry = it.next();
            try {
                Integer[] inputLines = new Integer[entry.getValue().size()];
                entry.getValue().toArray(inputLines);
                Set<Integer> newFirstLines = new HashSet<>();
                Set<Integer> newLines = new HashSet<>();
                for (int i = 0; i < inputLines.length; i++) {
                    newFirstLines.add(Utils.findFirstLine(basedir + File.separator + entry.getKey(), inputLines[i]));
                    newLines.addAll(Utils.findAllLines(basedir + File.separator + entry.getKey(), inputLines[i]));
                }
                entry.setValue(newFirstLines);
                List<Integer> newLinesList = newLines.stream().sorted().collect(Collectors.toList());
                String[] lineNumbers = new String[newLinesList.size()];
                for (int i = 0; i < newLines.size(); i++) {
                    lineNumbers[i] = newLinesList.get(i).toString();
                }
//                Integer[] intArray = new Integer[entry.getValue().size()];
//                entry.getValue().toArray(intArray);
//                String[] lineNumbers = new String[intArray.length];
//                for (int i = 0; i < intArray.length; i++) {
//                    lineNumbers[i] = intArray[i].toString();
//                }
                Parser.extractStmtIntoNewMethod(entry.getKey(), lineNumbers, deps, appSrcPath, null, r0TestPath,
                        r1TestPath, classesDirectory, "true");
            } catch (IOException ex) {
                getLog().error(LOG_LABEL + SPACE + "Failed to extract and instrument target statement from "
                        + entry.getKey() + " in instrumenter.");
                ex.printStackTrace();
            }
        }
    }
}
