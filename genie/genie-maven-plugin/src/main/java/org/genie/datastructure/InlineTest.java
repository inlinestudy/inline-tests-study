package org.genie.datastructure;

/** An object that represents an inline test in Genie. */
public class InlineTest {
    /** Round of the inline test. Either 0, 1 or 2.*/
    private final int round;

    /**
     * Serial of the inline test.
     * Currently set to (line number - 1) of the inline test in its inline test list file.
     */
    private final int serial;

    /** Relative path from project root to the original source code file containing the target statement. */
    private final String filePath;

    /** Line number of the target statement in the original source code file. */
    private final int lineNumber;

    /** Content of the inline test. */
    private final String itestContent;

    /**
     * Construct an inline test using all of its necessary components.
     * @param round round of the inline test, either 0, 1 or 2
     * @param serial serial of the inline test represented by its position in the inline tests list
     * @param filePath relative path from project root to the original source code file containing the target statement
     * @param lineNumber line number of the target statement in the original source code file
     * @param itestContent content of the inline test
     */
    public InlineTest(int round, int serial, String filePath, int lineNumber, String itestContent) {
        this.round = round;
        this.serial = serial;
        this.filePath = filePath;
        this.lineNumber = lineNumber;
        this.itestContent = itestContent;
    }

    /**
     * Construct an inline test using round and its line in the inline tests list file.
     * @param round round of the inline test, either 0, 1 or 2
     * @param serial serial of the inline test represented by its position in the inline tests list
     * @param line a line from either the R0 inline tests or R1 inline tests file
     */
    public InlineTest(int round, int serial, String line) {
        this.round = round;
        this.serial = serial;
        String[] components = line.split(";", 3);
        this.filePath = components[0];
        this.lineNumber = Integer.parseInt(components[1]);
        this.itestContent = components[2];
    }

    public int getRound() {
        return round;
    }

    public int getSerial() {
        return serial;
    }

    public String getFilePath() {
        return filePath;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public String getItestContent() {
        return itestContent;
    }
}
