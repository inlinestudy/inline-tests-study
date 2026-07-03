package org.genie;

import java.io.IOException;

public class App {
    public static void main(String[] args) throws IOException {
        String task = args[0];
        if (task.equals("collect-method")) {
            // Collect method that contains the line number.
            String filePath = args[1];
            int lineNumber = Integer.parseInt(args[2]);
            String outputFilePath = args[3];
            Parser.collectMethod(filePath, lineNumber, outputFilePath);
        } else if (task.equals("collect-line")) {
            // Collect statement that contains the line number.
            String filePath = args[1];
            int lineNumber = Integer.parseInt(args[2]);
            String outputFilePath = args[3];
            boolean keepInlineComment = Boolean.parseBoolean(args[4]);
            Parser.collectLine(filePath, lineNumber, outputFilePath, keepInlineComment);
        } else if (task.equals("collect-variables")) {
            // Collect variables that are used in the line number.
            String filePath = args[1];
            int lineNumber = Integer.parseInt(args[2]);
            String outputFilePath = args[3];
            Parser.collectVariables(filePath, lineNumber, outputFilePath);
        } else if (task.equals("collect-variables-with-type")) {
            // Collect variables that are used in the line number with their types.
            String filePath = args[1];
            int lineNumber = Integer.parseInt(args[2]);
            String depFilePath = args[3];
            String appSrcPath = args[4];
            String outputFilePath = args[5];
            Parser.collectVariablesWithType(filePath, lineNumber, depFilePath, appSrcPath, outputFilePath);
        } else if (task.equals("add-method")) {
            // Extract the target statement into a new method and a new class. Collect
            // variables in the line number and add a method with these variables as
            // arguments. Also log the variables before and after the target statements if
            // log variables is set as true.
            String filePath = args[1];
            String[] lineNumbers = args[2].split(",");
            String depFilePath = args[3];
            String appSrcPath = args[4];
            String logPath = args[5];
            String r0TestPath = args[6];
            String r1TestPath = args[7];
            String classesDirectory = args[8];
            String logVariables = args[9];
            Parser.extractStmtIntoNewMethod(filePath, lineNumbers, depFilePath, appSrcPath, logPath, r0TestPath,
                    r1TestPath,
                    classesDirectory, logVariables);
        } else {
            System.out.println("Invalid task");
        }
    }
}
