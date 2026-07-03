package org.genie;

import org.apache.maven.plugin.surefire.SurefireMojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.genie.constants.Constants;

/** Declares some necessary parameters that will be used extensively later. */
public class BaseMojo extends SurefireMojo implements Constants {
    /**
     * The path to a source file containing target statements, relative to the root of the project.
     * e.g., src/main/java/org/genie/BaseMojo.java.
     */
    @Parameter(property = "filePath")
    protected String filePath;

    /** A list of line numbers in string format separated by coma. e.g., 15,19. */
    @Parameter(property = "lineNumbers")
    protected String lineNumbers;

    /** Path to a target file that contains all pairs of (filePath, lineNumber) of interest. */
     @Parameter(property = "targetFile")
     protected String targetFile;

    /** Name of the artifacts directory (which stores important Genie metadata).*/
    @Parameter(property = "artifactsDir", defaultValue = ".genie")
    protected String artifactsDir;

    /**
     * Name of the test generation tool for the GeneratorMojo.
     * If the value is "genie", unit tests will be generated with all supported tools.
     * Otherwise, accepts a value of either "evosuite" or "randoop".
     */
    @Parameter(property = "tool", defaultValue = GENIE)
    protected String tool;

    /** Decides whether to output timer logging statements for the program. */
    @Parameter(property = "timerOn", defaultValue = "false")
    protected boolean timerOn;

    /**
     * In the case that Maven debug run (with -X) is enabled, printCommand decides whether to print the command that is
     * being run in a subprocess, as those commands can be quite lengthy to print.
     */
    @Parameter(property = "printCommand", defaultValue = "false")
    protected boolean printCommand;
}
