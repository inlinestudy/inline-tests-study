package org.genie.util;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.ast.type.TypeParameter;
import com.github.javaparser.utils.Pair;

/**
 * This class is used to store the context of the visitor.
 */
public class Context {
    public String srcPath;
    public int lineNumber;
    public List<Integer> lineNumbers = new ArrayList<>();
    public String outputFilePath;
    public String task;

    // Log variables.
    public String logPath;
    public String r0TestPath;
    public String r1TestPath;
    public String classesDirectory;
    public String className; // It is possbile that there are inner classes, so we need to
                             // store the class name that contains the target statement.

    public Set<String> logVariablesBefore = new HashSet<>();
    public Set<String> logVariablesAfter = new HashSet<>();
    public ArrayDeque<Set<String>> locals = new ArrayDeque();

    public boolean logVariables = false;
    public Map<String, String> logVariablesWithTypeBefore = new HashMap<>();
    public Map<String, String> logVariablesWithTypeAfter = new HashMap<>();
    public String line;
    public Map<String, Pair<Integer, Integer>> innerClassToLineNum = new HashMap<>();
    public ArrayDeque<String> nestedClassStack = new ArrayDeque<>();
    public String primaryClass;
    public Set<Node> rename = new HashSet<>(); // Map the method calls/fields that need to be renamed to their types.
    public NodeList<ReferenceType> thrownExceptions = new NodeList<>();
    public NodeList<TypeParameter> genericTypes = new NodeList<>();
}
