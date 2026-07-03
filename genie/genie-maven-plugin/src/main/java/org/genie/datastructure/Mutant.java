package org.genie.datastructure;

import java.io.File;
import java.nio.file.Paths;

/** Represents the mutant object that mutation testing tools generate. */
public class Mutant {
    /** An integer identifier for the mutant based on a given file. */
    private final int serial;

    /** Relative file path for the original program from Maven's project root. */
    private final String originalFilePath;

    /** File path of the mutated program. */
    private final String mutantFilePath;

    /** Line number of the target statement mutated, in the original program. */
    private final int lineNumber;

    /** Original content of the target statement. */
    private final String originalContent;

    /** Mutated content of the target statement. */
    private final String mutatedContent;

    /**
     * Unique identifier of the mutant, composed of the name of the original source file, and the serial of the mutant.
     * e.g., the mutant for A.java of serial 4 (determined automatically by the mutation tool) will be labeled "A#4".
     */
    private final String id;

    public Mutant(int serial, String originalFilePath, String mutantFilePath, int lineNumber, String originalContent,
                  String mutatedContent) {
        this.serial = serial;
        this.originalFilePath = originalFilePath;
        this.mutantFilePath = mutantFilePath;
        this.lineNumber = lineNumber;
        this.originalContent = originalContent;
        this.mutatedContent = mutatedContent;
        // TODO: Think of a more elegant way to handle, hard-coded the source file directory to be src/main/java
        this.id = Paths.get(originalFilePath).toString()
                .split("src" + File.separator + "main" + File.separator + "java" + File.separator)[1]
                .split("\\.java")[0] + "#" + this.serial;
    }

    @Override
    public String toString() {
        return "{id: " + id + ", original filepath: " + originalFilePath + ", mutant filepath: " + mutantFilePath
                + ", line number: " + lineNumber + ", original content: " + originalContent + ", mutated content: "
                + mutatedContent + "}";}

    public int getSerial() {
        return serial;
    }

    public String getOriginalFilePath() {
        return originalFilePath;
    }

    public String getMutantFilePath() {
        return mutantFilePath;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public String getOriginalContent() {
        return originalContent;
    }

    public String getMutatedContent() {
        return mutatedContent;
    }

    public String getId() {
        return id;
    }
}
