package org.genie;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ExtractStmtIntoNewMethodTest {
        @Test
        public void testExtractOneStmt() throws IOException {
                // Test extracting one statement into a new method.
                // Generated new method is in: java/mlinline/target/test-classes/M_32.java
                String srcPath = Objects.requireNonNull(getClass().getClassLoader().getResource("M.java")).getPath();
                String[] lineNumbers = { "32" };
                String home = System.getProperty("user.home");
                String projectName = "TNG_property-loader";
                String appSrcPath = home + "/projects/exli-internal/_downloads/" + projectName + "/src/main/java";
                String depFilePath = home
                                + "/projects/exli-internal/log/teco-randoop-test/" + projectName
                                + "/randoop-tests/randoop-deps.txt";
                if (!new File(depFilePath).exists()) {
                        return;
                }
                String logPath = home + "/projects/mlinline-internal/java/mlinline/target/raninline.txt";
                String inlineTestPath = home + "/projects/mlinline-internal/java/mlinline/target/raninline.txt";
                String classesDirectory = home
                                + "/projects/mlinline-internal/java/mlinline/target/test-classes/instrument";

                Parser.extractStmtIntoNewMethod(srcPath, lineNumbers, depFilePath, appSrcPath, logPath,
                                inlineTestPath, inlineTestPath,
                                classesDirectory, "true");

                String outputFilePath = srcPath.replace(".java", "_" + lineNumbers[0] + ".java");
                assertTrue(new File(outputFilePath).exists());
                List<String> actualLines = Files
                                .readAllLines(Paths.get(outputFilePath))
                                .stream().map(String::trim).collect(Collectors.toList());
                assertTrue(actualLines.contains("java.lang.String[] includes;"));
                assertTrue(actualLines.contains(
                                "includes = properties.getProperty(INCLUDE_KEY).split(\",\");"));
        }

        @Test
        public void testExtractMulStmts() throws IOException {
                // Test extracting multiple statements into multiple methods.
                // Generated new method is in: java/mlinline/target/test-classes/N_1267.java
                // java/mlinline/target/test-classes/N_1269.java
                String srcPath = Objects.requireNonNull(getClass().getClassLoader().getResource("N.java")).getPath();
                String[] lineNumbers = { "88", "90" };
                String home = System.getProperty("user.home");
                String projectName = "hyperledger_fabric-sdk-java";
                String appSrcPath = home
                                + "/projects/exli-internal/_downloads/" + projectName + "/src/main/java";
                String depFilePath = home
                                + "/projects/exli-internal/log/teco-randoop-test/" + projectName
                                + "/randoop-tests/randoop-deps.txt";
                if (!new File(depFilePath).exists()) {
                        return;
                }
                String logPath = home + "/projects/mlinline-internal/java/mlinline/target/raninline.txt";
                String inlineTestPath = home + "/projects/mlinline-internal/java/mlinline/target/raninline.txt";
                String classesDirectory = home
                                + "/projects/mlinline-internal/java/mlinline/target/test-classes/instrument";

                Parser.extractStmtIntoNewMethod(srcPath, lineNumbers, depFilePath, appSrcPath, logPath,
                                inlineTestPath, inlineTestPath,
                                classesDirectory, "true");
                String[] declareStmts = {
                                "java.lang.String endPoint;",
                                "java.lang.String endPoint;"
                };
                String[] targetStmts = {
                                "endPoint = \"localhost\" + name.substring(name.lastIndexOf(':'));",
                                "endPoint = name.toLowerCase().trim();"
                };
                for (int i = 0; i < lineNumbers.length; i++) {
                        String outputFilePath = srcPath.replace(".java", "_" + lineNumbers[i] + ".java");
                        assertTrue(new File(outputFilePath).exists());
                        List<String> actualLines = Files
                                        .readAllLines(Paths.get(outputFilePath))
                                        .stream().map(String::trim).collect(Collectors.toList());
                        assertTrue(actualLines.contains(declareStmts[i]));
                        assertTrue(actualLines.contains(targetStmts[i]));
                }
        }

        @Test
        public void testLambdaExpr() throws IOException {
                // It is possible that a lambda expression contains assignment statements,
                // however, we do not want to extract the assignment statements inside lambda
                // expression into the new method.
                // Do not extract local variable in lambda expression to the new method's
                // parameters. In this test case, variable "d" should not be added to the
                // parameter.
                String srcPath = Objects.requireNonNull(getClass().getClassLoader().getResource("L.java")).getPath();
                String[] lineNumbers = { "22" };
                String home = System.getProperty("user.home");
                String projectName = "CycloneDX_cyclonedx-core-java";
                String appSrcPath = home + "/projects/exli-internal/_downloads/" + projectName + "/src/main/java";
                String depFilePath = home
                                + "/projects/exli-internal/log/teco-randoop-test/" + projectName
                                + "/randoop-tests/randoop-deps.txt";
                if (!new File(depFilePath).exists()) {
                        return;
                }
                String logPath = home + "/projects/mlinline-internal/java/mlinline/target/raninline.txt";
                String inlineTestPath = home + "/projects/mlinline-internal/java/mlinline/target/raninline.txt";
                String classesDirectory = home
                                + "/projects/mlinline-internal/java/mlinline/target/test-classes/instrument";

                Parser.extractStmtIntoNewMethod(srcPath, lineNumbers, depFilePath, appSrcPath, logPath,
                                inlineTestPath, inlineTestPath,
                                classesDirectory, "true");

                String outputFilePath = srcPath.replace(".java", "_" + lineNumbers[0] + ".java");
                assertTrue(new File(outputFilePath).exists());
                List<String> actualLines = null;
                try {
                        actualLines = Files
                                        .readAllLines(Paths.get(outputFilePath))
                                        .stream().map(String::trim).collect(Collectors.toList());
                } catch (IOException e) {
                        e.printStackTrace();
                }
                String targetStmt = "boolean found = dependencies.stream().anyMatch(d -> d.getRef().equals(dependency.getRef()));";
                assertTrue(actualLines.contains(targetStmt));
        }

        @Test
        public void testRemoveThisFromVariable() throws IOException {
                // Test removing "this." from a variable in the target statement and the
                // parameter of the generated method.
                // Generated new method is in: java/mlinline/target/test-classes/O_128.java
                String srcPath = Objects.requireNonNull(getClass().getClassLoader().getResource("O.java")).getPath();
                String[] lineNumbers = { "104" };
                String home = System.getProperty("user.home");
                String projectName = "mojohaus_build-helper-maven-plugin";
                String appSrcPath = home
                                + "/projects/exli-internal/_downloads/" + projectName + "/src/main/java";
                String depFilePath = home
                                + "/projects/exli-internal/log/teco-randoop-test/" + projectName
                                + "/randoop-tests/randoop-deps.txt";
                if (!new File(depFilePath).exists()) {
                        return;
                }
                String logPath = home + "/projects/mlinline-internal/java/mlinline/target/raninline.txt";
                String inlineTestPath = home + "/projects/mlinline-internal/java/mlinline/target/raninline.txt";
                String classesDirectory = home
                                + "/projects/mlinline-internal/java/mlinline/target/test-classes/instrument";

                Parser.extractStmtIntoNewMethod(srcPath, lineNumbers, depFilePath, appSrcPath, logPath,
                                inlineTestPath, inlineTestPath,
                                classesDirectory, "true");

                String outputFilePath = srcPath.replace(".java", "_" + lineNumbers[0] + ".java");
                assertTrue(new File(outputFilePath).exists());
                List<String> actualLines = Files
                                .readAllLines(Paths.get(outputFilePath))
                                .stream().map(String::trim).collect(Collectors.toList());
                assertTrue(actualLines.contains(
                                "String[] bits = locale.split(\"[,_]\");"));
                // The type of locale is String, not Locale.
                assertTrue(actualLines.contains(
                                "public void targetStmtGenerated(java.lang.String locale) throws MojoExecutionException, MojoFailureException {"));
        }

        @Test
        public void testRemoveThisLHS() throws IOException {
                // Test removing "this." from the left hand side of a target statement and the
                // parameter of the generated method.
                // Generated new method is in: java/mlinline/target/test-classes/OO_9.java
                String srcPath = Objects.requireNonNull(getClass().getClassLoader().getResource("OO.java")).getPath();
                String[] lineNumbers = { "13" };
                String home = System.getProperty("user.home");
                String projectName = "TNG_property-loader";
                String appSrcPath = home
                                + "/projects/exli-internal/_downloads/" + projectName + "/src/main/java";
                String depFilePath = home
                                + "/projects/exli-internal/log/teco-randoop-test/" + projectName
                                + "/randoop-tests/randoop-deps.txt";
                if (!new File(depFilePath).exists()) {
                        return;
                }
                String logPath = home + "/projects/mlinline-internal/java/mlinline/target/raninline.txt";
                String inlineTestPath = home + "/projects/mlinline-internal/java/mlinline/target/raninline.txt";
                String classesDirectory = home
                                + "/projects/mlinline-internal/java/mlinline/target/test-classes/instrument";

                Parser.extractStmtIntoNewMethod(srcPath, lineNumbers, depFilePath, appSrcPath, logPath,
                                inlineTestPath, inlineTestPath,
                                classesDirectory, "true");

                String outputFilePath = srcPath.replace(".java", "_" + lineNumbers[0] + ".java");
                assertTrue(new File(outputFilePath).exists());
                List<String> actualLines = Files
                                .readAllLines(Paths.get(outputFilePath))
                                .stream().map(String::trim).collect(Collectors.toList());
                assertTrue(actualLines.contains("java.net.URL url;"));
                assertTrue(actualLines.contains(
                                "url = new File(address.replace(\"/\", File__separator)).toURI().toURL();"));
        }

        @Test
        public void testRemoveThisFromMethod() throws IOException {
                // Test removing "this." from a method call in the target statement.
                // Generated new method is in: java/mlinline/target/test-classes/OOO_9.java
                String srcPath = Objects.requireNonNull(getClass().getClassLoader().getResource("OOO.java")).getPath();
                String[] lineNumbers = { "10" };
                String home = System.getProperty("user.home");
                String projectName = "finos_messageml-utils";
                String appSrcPath = home
                                + "/projects/exli-internal/_downloads/" + projectName + "/src/main/java";
                String depFilePath = home
                                + "/projects/exli-internal/log/teco-randoop-test/" + projectName
                                + "/randoop-tests/randoop-deps.txt";
                if (!new File(depFilePath).exists()) {
                        return;
                }
                String logPath = home + "/projects/mlinline-internal/java/mlinline/target/raninline.txt";
                String inlineTestPath = home + "/projects/mlinline-internal/java/mlinline/target/raninline.txt";
                String classesDirectory = home
                                + "/projects/mlinline-internal/java/mlinline/target/test-classes/instrument";

                Parser.extractStmtIntoNewMethod(srcPath, lineNumbers, depFilePath, appSrcPath, logPath,
                                inlineTestPath, inlineTestPath,
                                classesDirectory, "true");

                String outputFilePath = srcPath.replace(".java", "_" + lineNumbers[0] + ".java");
                assertTrue(new File(outputFilePath).exists());
                List<String> actualLines = Files
                                .readAllLines(Paths.get(outputFilePath))
                                .stream().map(String::trim).collect(Collectors.toList());
                String targetStmt = "boolean hasPermittedElementAsChild = getChildren__.stream().anyMatch(element -> elementTypes.contains(element.getClass()));";
                assertTrue(actualLines.contains(targetStmt));
        }

        @Test
        public void testExtractStmtWithTypeOnLHS() throws IOException {
                // Test when left hand side of the target statement is a variable with type, we
                // do not need to add type on the left hand side.
                String srcPath = Objects.requireNonNull(getClass().getClassLoader().getResource("P.java")).getPath();
                String[] lineNumbers = { "390", "400" };
                String home = System.getProperty("user.home");
                String projectName = "hyperledger_fabric-sdk-java";
                String appSrcPath = home
                                + "/projects/exli-internal/_downloads/" + projectName + "/src/main/java";
                String depFilePath = home
                                + "/projects/exli-internal/log/teco-randoop-test/" + projectName
                                + "/randoop-tests/randoop-deps.txt";
                if (!new File(depFilePath).exists()) {
                        return;
                }
                String logPath = home + "/projects/mlinline-internal/java/mlinline/target/raninline.txt";
                String inlineTestPath = home + "/projects/mlinline-internal/java/mlinline/target/raninline.txt";
                String classesDirectory = home
                                + "/projects/mlinline-internal/java/mlinline/target/test-classes/instrument";

                Parser.extractStmtIntoNewMethod(srcPath, lineNumbers, depFilePath, appSrcPath, logPath,
                                inlineTestPath, inlineTestPath,
                                classesDirectory, "true");

                String[] targetStmts = {
                                "String[] split = s.split(\":\");",
                                "String[] split = s.split(\":\");"
                };
                for (int i = 0; i < lineNumbers.length; i++) {
                        String outputFilePath = srcPath.replace(".java", "_" + lineNumbers[i] + ".java");
                        assertTrue(new File(outputFilePath).exists());
                        List<String> actualLines = Files
                                        .readAllLines(Paths.get(outputFilePath))
                                        .stream().map(String::trim).collect(Collectors.toList());
                        assertTrue(actualLines.contains(targetStmts[i]));
                }
        }

        @Test
        public void testExtractWithoutLoggingVariables() throws IOException {
                // We do not need to log variables if we do not need to generate inline tests
                // (when logVariables is False).
                String srcPath = Objects.requireNonNull(getClass().getClassLoader().getResource("Q.java")).getPath();
                String[] lineNumbers = { "6" };

                Parser.extractStmtIntoNewMethod(srcPath, lineNumbers, null, null, null, null, null, null, "false");

                String outputFilePath = srcPath.replace(".java", "_" + lineNumbers[0] + ".java");
                assertTrue(new File(outputFilePath).exists());
                List<String> actualLines = Files
                                .readAllLines(Paths.get(outputFilePath))
                                .stream().map(String::trim).collect(Collectors.toList());
                String targetStmt = "String newStr = s.split(\" \")[0] + \"!\";";
                assertTrue(actualLines.contains(targetStmt));
                assertTrue(actualLines.size() == 8);
        }

        @Test
        public void testIfCondition() throws IOException {
                // Test extracting condition expression, parsing it to a statement and adding it
                // into a new method.
                String srcPath = Objects.requireNonNull(getClass().getClassLoader().getResource("R.java")).getPath();
                String[] lineNumbers = { "33" };
                String home = System.getProperty("user.home");
                String projectName = "maxmind_geoip-api-java";
                String appSrcPath = home + "/projects/exli-internal/_downloads/" + projectName + "/src/main/java";
                String depFilePath = home
                                + "/projects/exli-internal/log/teco-randoop-test/" + projectName
                                + "/randoop-tests/randoop-deps.txt";
                if (!new File(depFilePath).exists()) {
                        return;
                }
                String logPath = home + "/projects/mlinline-internal/java/mlinline/target/raninline.txt";
                String inlineTestPath = home + "/projects/mlinline-internal/java/mlinline/target/raninline.txt";
                String classesDirectory = home
                                + "/projects/mlinline-internal/java/mlinline/target/test-classes/instrument";

                Parser.extractStmtIntoNewMethod(srcPath, lineNumbers, depFilePath, appSrcPath, logPath,
                                inlineTestPath, inlineTestPath,
                                classesDirectory, "true");

                String outputFilePath = srcPath.replace(".java", "_" + lineNumbers[0] + ".java");
                assertTrue(new File(outputFilePath).exists());
                List<String> actualLines = Files
                                .readAllLines(Paths.get(outputFilePath))
                                .stream().map(String::trim).collect(Collectors.toList());
                assertTrue(actualLines.contains(
                                "boolean condExprRes = (read == 3 && (delim[0] & 0xFF) == 255 && (delim[1] & 0xFF) == 255 && (delim[2] & 0xFF) == 255);"));
        }

        @Test
        public void testExtractCheckingRHSVarIsInRHS() throws IOException {
                // If LHS variable also appears in RHS, we do not need to add type to LHS
                // because ths RHS variable will be added to the method arguments.
                String srcPath = Objects.requireNonNull(getClass().getClassLoader().getResource("S.java")).getPath();
                String[] lineNumbers = { "6" };

                Parser.extractStmtIntoNewMethod(srcPath, lineNumbers, null, null, null, null, null, null, "false");

                String outputFilePath = srcPath.replace(".java", "_" + lineNumbers[0] + ".java");
                List<String> actualLines = null;
                try {
                        actualLines = Files
                                        .readAllLines(Paths.get(outputFilePath))
                                        .stream().map(String::trim).collect(Collectors.toList());
                } catch (IOException e) {
                        e.printStackTrace();
                }
                String targetStmt = "s = s.split(\" \")[0] + \"!\";";
                assertTrue(actualLines.contains(targetStmt));
        }

        @Test
        public void testImportStaticInnerClass() throws IOException {
                // When there is InnerClass::New (stream API) in the target statement, we need
                // to import this inner class.
                // restfb_restfb/target/generated-sources/delombok/com/restfb/types/Subscription_97.java
                String srcPath = Objects.requireNonNull(getClass().getClassLoader().getResource("T.java")).getPath();
                String[] lineNumbers = { "13" };
                String home = System.getProperty("user.home");
                String projectName = "restfb_restfb";
                String appSrcPath = home + "/projects/exli-internal/_downloads/" + projectName + "/src/main/lombok";
                String depFilePath = home
                                + "/projects/exli-internal/log/teco-randoop-test/" + projectName
                                + "/randoop-tests/randoop-deps.txt";
                if (!new File(depFilePath).exists()) {
                        return;
                }
                String logPath = home + "/projects/mlinline-internal/java/mlinline/target/raninline.txt";
                String inlineTestPath = home + "/projects/mlinline-internal/java/mlinline/target/raninline.txt";
                String classesDirectory = home
                                + "/projects/mlinline-internal/java/mlinline/target/test-classes/instrument";

                Parser.extractStmtIntoNewMethod(srcPath, lineNumbers, depFilePath, appSrcPath, logPath,
                                inlineTestPath, inlineTestPath,
                                classesDirectory, "false");

                String outputFilePath = srcPath.replace(".java", "_" + lineNumbers[0] + ".java");
                assertTrue(new File(outputFilePath).exists());
                List<String> actualLines = null;
                try {
                        actualLines = Files
                                        .readAllLines(Paths.get(outputFilePath))
                                        .stream().map(String::trim).collect(Collectors.toList());
                } catch (IOException e) {
                        e.printStackTrace();
                }
                String declareStmt = "java.util.List<test.T.SubscriptionField> fields;";
                assertTrue(actualLines.contains(declareStmt));
                String targetStmt = "fields = compatFields.stream().map(SubscriptionField::new).collect(Collectors.toList());";
                assertTrue(actualLines.contains(targetStmt));
                assertTrue(actualLines.contains("import test.T.SubscriptionField;"));
        }

        @Test
        public void testImportStaticInnerClassMultipleLevels() throws IOException {
                // When there is InnerClass::New (stream API) in the target statement, we need
                // to import this inner class.
                String srcPath = Objects.requireNonNull(getClass().getClassLoader().getResource("TT.java")).getPath();
                String[] lineNumbers = { "7" };
                String home = System.getProperty("user.home");
                String logPath = home + "/projects/mlinline-internal/java/mlinline/target/raninline.txt";
                String inlineTestPath = home + "/projects/mlinline-internal/java/mlinline/target/raninline.txt";
                String classesDirectory = home
                                + "/projects/mlinline-internal/java/mlinline/target/test-classes/instrument";

                Parser.extractStmtIntoNewMethod(srcPath, lineNumbers, null, null, logPath,
                                inlineTestPath, inlineTestPath,
                                classesDirectory, "false");

                String outputFilePath = srcPath.replace(".java", "_" + lineNumbers[0] + ".java");
                assertTrue(new File(outputFilePath).exists());
                List<String> actualLines = null;
                try {
                        actualLines = Files
                                        .readAllLines(Paths.get(outputFilePath))
                                        .stream().map(String::trim).collect(Collectors.toList());
                } catch (IOException e) {
                        e.printStackTrace();
                }
                assertTrue(actualLines.contains("import test.TT.A.B;"));
                // We do not import C.B because it has the same class name as A.B.
                assertFalse(actualLines.contains("import test.TT.C.B;"));
        }

        @Test
        public void testResolveMethodReturnType() throws IOException {
                // When there is a method call in the target statement, if it does not have a
                // scope and its return type is not void, we need to resolve the
                // return type of this method, and add it to the method arguments.
                String srcPath = Objects.requireNonNull(getClass().getClassLoader().getResource("U.java")).getPath();
                String[] lineNumbers = { "14" };
                String home = System.getProperty("user.home");
                String projectName = "restfb_restfb";
                String appSrcPath = home + "/projects/exli-internal/_downloads/" + projectName + "/src/main/lombok"
                                + ":"
                                + home + "/projects/exli-internal/_downloads/" + projectName + "/src/main/java";
                String depFilePath = home
                                + "/projects/exli-internal/log/teco-randoop-test/" + projectName
                                + "/randoop-tests/randoop-deps.txt";
                if (!new File(depFilePath).exists()) {
                        return;
                }
                String logPath = home + "/projects/mlinline-internal/java/mlinline/target/raninline.txt";
                String inlineTestPath = home + "/projects/mlinline-internal/java/mlinline/target/raninline.txt";
                String classesDirectory = home
                                + "/projects/mlinline-internal/java/mlinline/target/test-classes/instrument";

                Parser.extractStmtIntoNewMethod(srcPath, lineNumbers, depFilePath, appSrcPath, logPath,
                                inlineTestPath, inlineTestPath,
                                classesDirectory, "true");

                String outputFilePath = srcPath.replace(".java", "_" + lineNumbers[0] + ".java");
                assertTrue(new File(outputFilePath).exists());
                List<String> actualLines = null;
                try {
                        actualLines = Files
                                        .readAllLines(Paths.get(outputFilePath))
                                        .stream().map(String::trim).collect(Collectors.toList());
                } catch (IOException e) {
                        e.printStackTrace();
                }
                String targetStmt = "boolean condExprRes = (isBlank_getBirthday___ || getBirthday____split__divide____length < 2);";
                assertTrue(actualLines.contains(targetStmt));
        }

        @Test
        public void testResolveMethodReturnTypeV2() throws IOException {
                // Test if the SymbolSolver can resolve the return type of a method call.
                // TODO: Not sure if this kind of renaming makes sense.
                String srcPath = Objects.requireNonNull(getClass().getClassLoader().getResource("UU.java")).getPath();
                String[] lineNumbers = { "16" };
                String home = System.getProperty("user.home");
                String projectName = "TNG_property-loader";
                String appSrcPath = home + "/projects/exli-internal/_downloads/" + projectName + "/src/main/java";
                String depFilePath = home
                                + "/projects/exli-internal/log/teco-randoop-test/" + projectName
                                + "/randoop-tests/randoop-deps.txt";
                if (!new File(depFilePath).exists()) {
                        return;
                }
                String logPath = home + "/projects/mlinline-internal/java/mlinline/target/raninline.txt";
                String inlineTestPath = home + "/projects/mlinline-internal/java/mlinline/target/raninline.txt";
                String classesDirectory = home
                                + "/projects/mlinline-internal/java/mlinline/target/test-classes/instrument";

                Parser.extractStmtIntoNewMethod(srcPath, lineNumbers, depFilePath, appSrcPath, logPath,
                                inlineTestPath, inlineTestPath,
                                classesDirectory, "true");

                String outputFilePath = srcPath.replace(".java", "_" + lineNumbers[0] + ".java");
                assertTrue(new File(outputFilePath).exists());
                List<String> actualLines = null;
                try {
                        actualLines = Files
                                        .readAllLines(Paths.get(outputFilePath))
                                        .stream().map(String::trim).collect(Collectors.toList());
                } catch (IOException e) {
                        e.printStackTrace();
                }
                String declareStmt = "java.lang.String value;";
                assertTrue(actualLines.contains(declareStmt));
                String targetStmt = "value = getenv_matcher__group_1__;";
                assertTrue(actualLines.contains(targetStmt));
        }

        @Test
        public void testAddTypeToRenamedLHS() throws IOException {
                // Test if the SymbolSover could resolve the type of an element in an array.
                String srcPath = Objects.requireNonNull(getClass().getClassLoader().getResource("W.java")).getPath();
                String[] lineNumbers = { "6" };
                String home = System.getProperty("user.home");
                String projectName = "mp911de_logstash-gelf";
                String appSrcPath = home + "/projects/exli-internal/_downloads/" + projectName + "/src/main/java";
                String depFilePath = home
                                + "/projects/exli-internal/log/teco-randoop-test/" + projectName
                                + "/randoop-tests/randoop-deps.txt";
                if (!new File(depFilePath).exists()) {
                        return;
                }
                String logPath = home + "/projects/mlinline-internal/java/mlinline/target/raninline.txt";
                String inlineTestPath = home + "/projects/mlinline-internal/java/mlinline/target/raninline.txt";
                String classesDirectory = home
                                + "/projects/mlinline-internal/java/mlinline/target/test-classes/instrument";

                Parser.extractStmtIntoNewMethod(srcPath, lineNumbers, depFilePath, appSrcPath, logPath,
                                inlineTestPath, inlineTestPath,
                                classesDirectory, "false");

                String outputFilePath = srcPath.replace(".java", "_" + lineNumbers[0] + ".java");
                assertTrue(new File(outputFilePath).exists());
                List<String> actualLines = null;
                try {
                        actualLines = Files
                                        .readAllLines(Paths.get(outputFilePath))
                                        .stream().map(String::trim).collect(Collectors.toList());
                } catch (IOException e) {
                        e.printStackTrace();
                }
                String declareStmt = "byte buf__offsetplus1;";
                assertTrue(actualLines.contains(declareStmt));
                String targetStmt = "buf__offsetplus1 = (byte) ((s >> 8) & 0xff);";
                assertTrue(actualLines.contains(targetStmt));
        }

        @Test
        public void testAddTypeToLHS() throws IOException {
                // Test if the SymbolSover could resolve the type of an element in an array.
                String srcPath = Objects.requireNonNull(getClass().getClassLoader().getResource("WW.java")).getPath();
                String[] lineNumbers = { "31" };
                String home = System.getProperty("user.home");
                String projectName = "craftercms_core";
                String appSrcPath = home + "/projects/exli-internal/_downloads/" + projectName + "/src/main/java";
                String depFilePath = home
                                + "/projects/exli-internal/log/teco-randoop-test/" + projectName
                                + "/randoop-tests/randoop-deps.txt";
                if (!new File(depFilePath).exists()) {
                        return;
                }
                String logPath = home + "/projects/mlinline-internal/java/mlinline/target/raninline.txt";
                String inlineTestPath = home + "/projects/mlinline-internal/java/mlinline/target/raninline.txt";
                String classesDirectory = home
                                + "/projects/mlinline-internal/java/mlinline/target/test-classes/instrument";

                Parser.extractStmtIntoNewMethod(srcPath, lineNumbers, depFilePath, appSrcPath, logPath,
                                inlineTestPath, inlineTestPath,
                                classesDirectory, "false");

                String outputFilePath = srcPath.replace(".java", "_" + lineNumbers[0] + ".java");
                assertTrue(new File(outputFilePath).exists());
                List<String> actualLines = null;
                try {
                        actualLines = Files
                                        .readAllLines(Paths.get(outputFilePath))
                                        .stream().map(String::trim).collect(Collectors.toList());
                } catch (IOException e) {
                        e.printStackTrace();
                }
                String declareStmt = "int k;";
                assertTrue(actualLines.contains(declareStmt));
                String targetStmt = "k = mainDescriptorUrl.indexOf('/');";
                assertTrue(actualLines.contains(targetStmt));
        }

        @Test
        public void testRenameMethodCall() throws IOException {
                // Test if the method call is renamed correctly.
                String srcPath = Objects.requireNonNull(getClass().getClassLoader().getResource("X.java")).getPath();
                String[] lineNumbers = { "13" };
                String home = System.getProperty("user.home");
                String projectName = "maxmind_geoip-api-java";
                String appSrcPath = home + "/projects/exli-internal/_downloads/" + projectName + "/src/main/java";
                String depFilePath = home
                                + "/projects/exli-internal/log/teco-randoop-test/" + projectName
                                + "/randoop-tests/randoop-deps.txt";
                if (!new File(depFilePath).exists()) {
                        return;
                }
                String logPath = home + "/projects/mlinline-internal/java/mlinline/target/raninline.txt";
                String inlineTestPath = home + "/projects/mlinline-internal/java/mlinline/target/raninline.txt";
                String classesDirectory = home
                                + "/projects/mlinline-internal/java/mlinline/target/test-classes/instrument";

                Parser.extractStmtIntoNewMethod(srcPath, lineNumbers, depFilePath, appSrcPath, logPath,
                                inlineTestPath, inlineTestPath,
                                classesDirectory, "false");

                String outputFilePath = srcPath.replace(".java", "_" + lineNumbers[0] + ".java");
                assertTrue(new File(outputFilePath).exists());
                List<String> actualLines = null;
                try {
                        actualLines = Files
                                        .readAllLines(Paths.get(outputFilePath))
                                        .stream().map(String::trim).collect(Collectors.toList());
                } catch (IOException e) {
                        e.printStackTrace();
                }
                String targetStmt = "metroareaCombo += unsignedByteToInt_buffer__get___ << (j * 8);";
                assertTrue(actualLines.contains(targetStmt));
        }

        @Test
        public void testAddCheckedExceptionFromMethodSig() throws IOException {
                // Test if the checked exception can be migrated to the new method signature.
                String srcPath = Objects.requireNonNull(getClass().getClassLoader().getResource("E.java")).getPath();
                String[] lineNumbers = { "18" };
                String home = System.getProperty("user.home");
                String projectName = "lamarios_sherdog-parser";
                String appSrcPath = home + "/projects/exli-internal/_downloads/" + projectName + "/src/main/java";
                String depFilePath = home
                                + "/projects/exli-internal/log/teco-randoop-test/" + projectName
                                + "/randoop-tests/randoop-deps.txt";
                if (!new File(depFilePath).exists()) {
                        return;
                }
                String logPath = home + "/projects/mlinline-internal/java/mlinline/target/raninline.txt";
                String inlineTestPath = home + "/projects/mlinline-internal/java/mlinline/target/raninline.txt";
                String classesDirectory = home
                                + "/projects/mlinline-internal/java/mlinline/target/test-classes/instrument";

                Parser.extractStmtIntoNewMethod(srcPath, lineNumbers, depFilePath, appSrcPath, logPath,
                                inlineTestPath, inlineTestPath,
                                classesDirectory, "false");

                String outputFilePath = srcPath.replace(".java", "_" + lineNumbers[0] + ".java");
                assertTrue(new File(outputFilePath).exists());
                List<String> actualLines = null;
                try {
                        actualLines = Files
                                        .readAllLines(Paths.get(outputFilePath))
                                        .stream().map(String::trim).collect(Collectors.toList());
                } catch (IOException e) {
                        e.printStackTrace();
                }
                String declareStmt = "org.jsoup.nodes.Document doc;";
                assertTrue(actualLines.contains(declareStmt));
                String targetStmt = "doc = ParserUtils.parseDocument(String.format(url, page));";
                assertTrue(actualLines.contains(targetStmt));
                String signature = "public void targetStmtGenerated(int page, java.lang.String url) throws IOException, ParseException {";
                assertTrue(actualLines.contains(signature));
        }

        @Test
        public void testAddCheckedExceptionFromTryCatch() throws IOException {
                // Test if the catched exception can be thrown from the new method signature.
                String srcPath = Objects.requireNonNull(getClass().getClassLoader().getResource("EE.java")).getPath();
                String[] lineNumbers = { "13" };
                String home = System.getProperty("user.home");
                String projectName = "TNG_property-loader";
                String appSrcPath = home + "/projects/exli-internal/_downloads/" + projectName + "/src/main/java";
                String depFilePath = home
                                + "/projects/exli-internal/log/teco-randoop-test/" + projectName
                                + "/randoop-tests/randoop-deps.txt";
                if (!new File(depFilePath).exists()) {
                        return;
                }
                String logPath = home + "/projects/mlinline-internal/java/mlinline/target/raninline.txt";
                String inlineTestPath = home + "/projects/mlinline-internal/java/mlinline/target/raninline.txt";
                String classesDirectory = home
                                + "/projects/mlinline-internal/java/mlinline/target/test-classes/instrument";

                Parser.extractStmtIntoNewMethod(srcPath, lineNumbers, depFilePath, appSrcPath, logPath,
                                inlineTestPath, inlineTestPath,
                                classesDirectory, "false");

                String outputFilePath = srcPath.replace(".java", "_" + lineNumbers[0] + ".java");
                assertTrue(new File(outputFilePath).exists());
                List<String> actualLines = null;
                try {
                        actualLines = Files
                                        .readAllLines(Paths.get(outputFilePath))
                                        .stream().map(String::trim).collect(Collectors.toList());
                } catch (IOException e) {
                        e.printStackTrace();
                }
                String declareStmt = "java.net.URL url;";
                assertTrue(actualLines.contains(declareStmt));
                String targetStmt = "url = new File(address.replace(\"/\", File__separator)).toURI().toURL();";
                assertTrue(actualLines.contains(targetStmt));
                String signature = "public void targetStmtGenerated(java.lang.String address, java.lang.String File__separator) throws MalformedURLException {";
                assertTrue(actualLines.contains(signature));
        }

        @Test
        public void testMultiTryCatch() throws IOException {
                // Test if the array access is renamed correctly.
                String srcPath = Objects.requireNonNull(getClass().getClassLoader().getResource("EEE.java")).getPath();
                String[] lineNumbers = { "15" };
                String home = System.getProperty("user.home");
                String projectName = "AquaticInformatics_aquarius-sdk-java";
                String appSrcPath = home + "/projects/exli-internal/_downloads/" + projectName + "/src/main/java";
                String depFilePath = home
                                + "/projects/exli-internal/log/teco-randoop-test/" + projectName
                                + "/randoop-tests/randoop-deps.txt";
                if (!new File(depFilePath).exists()) {
                        return;
                }
                String logPath = home + "/projects/mlinline-internal/java/mlinline/target/raninline.txt";
                String inlineTestPath = home + "/projects/mlinline-internal/java/mlinline/target/raninline.txt";
                String classesDirectory = home
                                + "/projects/mlinline-internal/java/mlinline/target/test-classes/instrument";

                Parser.extractStmtIntoNewMethod(srcPath, lineNumbers, depFilePath, appSrcPath, logPath,
                                inlineTestPath, inlineTestPath,
                                classesDirectory, "true");

                String outputFilePath = srcPath.replace(".java", "_" + lineNumbers[0] + ".java");
                assertTrue(new File(outputFilePath).exists());
                List<String> actualLines = null;
                try {
                        actualLines = Files
                                        .readAllLines(Paths.get(outputFilePath))
                                        .stream().map(String::trim).collect(Collectors.toList());
                } catch (IOException e) {
                        e.printStackTrace();
                }
                String targetStmt = "String parameterName = m.group(1);";
                assertTrue(actualLines.contains(targetStmt));
                String signature = "public void targetStmtGenerated(java.util.regex.Matcher m) throws IllegalAccessException, UnsupportedEncodingException {";
                assertTrue(actualLines.contains(signature));
        }

        @Test
        public void testRenameArray() throws IOException {
                // Test if the array access is renamed correctly.
                String srcPath = Objects.requireNonNull(getClass().getClassLoader().getResource("Y.java")).getPath();
                String[] lineNumbers = { "13" };
                String home = System.getProperty("user.home");
                String projectName = "maxmind_geoip-api-java";
                String appSrcPath = home + "/projects/exli-internal/_downloads/" + projectName + "/src/main/java";
                String depFilePath = home
                                + "/projects/exli-internal/log/teco-randoop-test/" + projectName
                                + "/randoop-tests/randoop-deps.txt";
                if (!new File(depFilePath).exists()) {
                        return;
                }
                String logPath = home + "/projects/mlinline-internal/java/mlinline/target/raninline.txt";
                String inlineTestPath = home + "/projects/mlinline-internal/java/mlinline/target/raninline.txt";
                String classesDirectory = home
                                + "/projects/mlinline-internal/java/mlinline/target/test-classes/instrument";

                Parser.extractStmtIntoNewMethod(srcPath, lineNumbers, depFilePath, appSrcPath, logPath,
                                inlineTestPath, inlineTestPath,
                                classesDirectory, "true");

                String outputFilePath = srcPath.replace(".java", "_" + lineNumbers[0] + ".java");
                assertTrue(new File(outputFilePath).exists());
                List<String> actualLines = null;
                try {
                        actualLines = Files
                                        .readAllLines(Paths.get(outputFilePath))
                                        .stream().map(String::trim).collect(Collectors.toList());
                } catch (IOException e) {
                        e.printStackTrace();
                }
                String targetStmt = "databaseSegments__0 += (unsignedByteToInt_buf__j_ << (j * 8));";
                assertTrue(actualLines.contains(targetStmt));
                String arrayAccessStr = "databaseSegments[0], \"databaseSegments[0]\"";
                for (String line : actualLines) {
                        assertFalse(line.contains(arrayAccessStr));
                }
        }

        @Test
        public void testParseGenericType() throws IOException {
                // Test if the array access is renamed correctly.
                String srcPath = Objects.requireNonNull(getClass().getClassLoader().getResource("Z.java")).getPath();
                String[] lineNumbers = { "10" };
                String home = System.getProperty("user.home");
                String projectName = "uwolfer_gerrit-rest-java-client";
                String appSrcPath = home + "/projects/exli-internal/_downloads/" + projectName + "/src/main/java";
                String depFilePath = home
                                + "/projects/exli-internal/log/teco-randoop-test/" + projectName
                                + "/randoop-tests/randoop-deps.txt";
                if (!new File(depFilePath).exists()) {
                        return;
                }
                String logPath = home + "/projects/mlinline-internal/java/mlinline/target/raninline.txt";
                String inlineTestPath = home + "/projects/mlinline-internal/java/mlinline/target/raninline.txt";
                String classesDirectory = home
                                + "/projects/mlinline-internal/java/mlinline/target/test-classes/instrument";

                Parser.extractStmtIntoNewMethod(srcPath, lineNumbers, depFilePath, appSrcPath, logPath,
                                inlineTestPath, inlineTestPath,
                                classesDirectory, "true");

                String outputFilePath = srcPath.replace(".java", "_" + lineNumbers[0] + ".java");
                assertTrue(new File(outputFilePath).exists());
                List<String> actualLines = null;
                try {
                        actualLines = Files
                                        .readAllLines(Paths.get(outputFilePath))
                                        .stream().map(String::trim).collect(Collectors.toList());
                } catch (IOException e) {
                        e.printStackTrace();
                }
                String targetStmt = "v |= 1 << option.getValue();";
                assertTrue(actualLines.contains(targetStmt));
                String signature = "public <T extends Enum<T>> void targetStmtGenerated(int v, T option) {";
                assertTrue(actualLines.contains(signature));
        }

        @Test
        public void testMultiAssignExpr() throws IOException {
                // Test if the array access is renamed correctly.
                String srcPath = Objects.requireNonNull(getClass().getClassLoader().getResource("F.java")).getPath();
                String[] lineNumbers = { "12" };
                String home = System.getProperty("user.home");
                String projectName = "jkuhnert_ognl";
                String appSrcPath = home + "/projects/exli-internal/_downloads/" + projectName + "/src/main/java";
                String depFilePath = home
                                + "/projects/exli-internal/log/teco-randoop-test/" + projectName
                                + "/randoop-tests/randoop-deps.txt";
                if (!new File(depFilePath).exists()) {
                        return;
                }
                String logPath = home + "/projects/mlinline-internal/java/mlinline/target/raninline.txt";
                String inlineTestPath = home + "/projects/mlinline-internal/java/mlinline/target/raninline.txt";
                String classesDirectory = home
                                + "/projects/mlinline-internal/java/mlinline/target/test-classes/instrument";

                Parser.extractStmtIntoNewMethod(srcPath, lineNumbers, depFilePath, appSrcPath, logPath,
                                inlineTestPath, inlineTestPath,
                                classesDirectory, "true");

                String outputFilePath = srcPath.replace(".java", "_" + lineNumbers[0] + ".java");
                assertTrue(new File(outputFilePath).exists());
                List<String> actualLines = null;
                try {
                        actualLines = Files
                                        .readAllLines(Paths.get(outputFilePath))
                                        .stream().map(String::trim).collect(Collectors.toList());
                } catch (IOException e) {
                        e.printStackTrace();
                }
                String declareStmtOne = "char buffer__bufpos;";
                assertTrue(actualLines.contains(declareStmtOne));
                String declareStmtTwo = "char c;";
                assertTrue(actualLines.contains(declareStmtTwo));
                String targetStmt = "buffer__bufpos = c = (char) (hexval_c_ << 12 | hexval_ReadByte___ << 8 | hexval_ReadByte___ << 4 | hexval_ReadByte___);";
                assertTrue(actualLines.contains(targetStmt));
        }
}
