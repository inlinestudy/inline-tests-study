package org.genie;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.genie.util.Utils;
import org.junit.jupiter.api.Test;

public class CollectLineTest {
    @Test
    public void testCollectNestedIfCondition() throws IOException {
        Parser parser = new Parser();
        String inputPath = "src/test/resources/cl/A.java";
        String outputPath = "src/test/resources/cl/A.txt";
        parser.collectLine(inputPath, 32, outputPath, false);
        String output = Utils.getOutput(outputPath);
        assertEquals(
                "if (!StringUtils.isEmpty(String.valueOf(value)) && optionalBiItem.get().getAttributes().get(key) != null)",
                output);
    }

    @Test
    public void testCollectOuterLambdaExpression() throws IOException {
        Parser parser = new Parser();
        String inputPath = "src/test/resources/cl/B.java";
        String outputPath = "src/test/resources/cl/B.txt";
        parser.collectLine(inputPath, 8, outputPath, false);
        String output = Utils.getOutput(outputPath);
        assertEquals(
                "boolean found = dependencies.stream().anyMatch(d -> d.getRef().equals(dependency.getRef()));",
                output);
    }
}
