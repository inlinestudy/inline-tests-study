package org.genie;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;

import org.genie.util.Utils;
import org.junit.jupiter.api.Test;

import com.github.javaparser.resolution.UnsolvedSymbolException;

public class FindVariableWithTypeTest {

    @Test
    public void testResolveString() throws IOException {
        // Test if the type is resolved to fully qualified name.
        Parser parser = new Parser();
        String inputPath = "src/test/resources/fvwt/A.java";
        String appSrcPath = "src/test/resources/fvwt";
        String outputPath = "src/test/resources/fvwt/A.txt";
        parser.collectVariablesWithType(inputPath, 5, null, appSrcPath, outputPath);
        String output = Utils.getOutput(outputPath);
        assertEquals("\ns:java.lang.String", output);
        parser.collectVariablesWithType(inputPath, 6, null, appSrcPath, outputPath);
        output = Utils.getOutput(outputPath);
        assertEquals("s:java.lang.String\ns:java.lang.String", output);
    }

    @Test
    public void testResolveType() throws IOException {
        // This test requires the existence of Asana_java-asana in
        // ${home_dir}/projects/exli-internal/_downloads
        Parser parser = new Parser();
        String projectDir = System.getProperty("user.home") + "/projects/exli-internal/_downloads/Asana_java-asana/";
        String inputPath = projectDir + "src/main/java/com/asana/resources/Teams.java";
        if (!java.nio.file.Files.exists(java.nio.file.Paths.get(inputPath))) {
            return;
        }
        String sha = "52fef9b";
        Utils.prepareProject(projectDir, sha);

        String appSrcPath = projectDir + "src/main/java";
        String outputPath = "src/test/resources/fvwt/Teams.txt";
        parser.collectVariablesWithType(inputPath, 35, null, appSrcPath, outputPath);
        String output = Utils.getOutput(outputPath);
        assertEquals("organization:java.lang.String\npath:java.lang.String", output);
    }

    @Test
    public void testResolveMethodCallReturnType() throws IOException {
        // TODO: we need to resolve the return type of the method call.
        // Test if the non-primitive type is resolved to fully qualified name.
        // Test file is a code snippet from
        // hyperledger_fabric-sdk-java/src/main/java/org/hyperledger/fabric/sdk/NetworkConfig.java
        // 798
        Parser parser = new Parser();
        String inputPath = "src/test/resources/fvwt/B.java";
        String appSrcPath = "src/test/resources/fvwt";
        String outputPath = "src/test/resources/fvwt/B.txt";
        String depFilePath = System.getProperty("user.dir")
                + "/../../generated-tests/hyperledger_fabric-sdk-java/randoop-deps.txt";
        if (!java.nio.file.Files.exists(java.nio.file.Paths.get(depFilePath))) {
            System.out.println("Dependency file not found: " + depFilePath);
            return;
        }
        // parser.collectVariablesWithType(inputPath, 64, depFilePath, appSrcPath,
        // outputPath);
        // String output = Utils.getOutput(outputPath);
        // assertEquals("pemBytes:byte[]\njsonTlsCaCerts:javax.json.JsonObject",
        // output);
        assertThrows(UnsolvedSymbolException.class, () -> {
            parser.collectVariablesWithType(inputPath, 64, null, appSrcPath, outputPath);
        });
    }
}
