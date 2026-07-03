package org.genie.constants;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public interface Constants {
    String DEV_NULL = File.separator + "dev" + File.separator + "null";
    String DEFAULT_SEED = "42";
    List<String> SKIPS = Arrays.asList("-Dcheckstyle.skip", "-Drat.skip", "-Denforcer.skip", "-Danimal.sniffer.skip",
            "-Dmaven.javadoc.skip", "-Dfindbugs.skip", "-Dwarbucks.skip", "-Dmodernizer.skip", "-Dimpsort.skip",
            "-Dpmd.skip", "-Dxjc.skip", "-Dair.check.skip-all", "-Dfmt.skip", "-Djacoco.skip");
    List<String> SKIPS_NO_JACOCO = Arrays.asList("-Dcheckstyle.skip", "-Drat.skip", "-Denforcer.skip", "-Danimal.sniffer.skip",
            "-Dmaven.javadoc.skip", "-Dfindbugs.skip", "-Dwarbucks.skip", "-Dmodernizer.skip", "-Dimpsort.skip",
            "-Dpmd.skip", "-Dxjc.skip", "-Dair.check.skip-all", "-Dfmt.skip");
    int EVOSUITE_TIME_LIMIT_S = 120;
    int EVOSUITE_SINGLE_TEST_TIMEOUT_MS = 4000000;
    // Using 1/10 of the original value for faster evaluation.
    int RANDOOP_TIMEOUT_S = 1080;
    int RANDOOP_SINGLE_CLASS_TIMEOUT_S = 10;
    int RANDOOP_EXTRA_TIMEOUT_S = 180;
    String REPORT_FILE = "TEST-junit-jupiter";
    String SPACE = " ";
    String TOP_LEVEL_BAR = "=====";
    String BACKUP_SUFFIX = ".bak";
    String SRC_MAIN_JAVA = "src" + File.separator + "main" + File.separator + "java";
    String SRC_TEST_JAVA = "src" + File.separator + "test" + File.separator + "java";
    String TARGET_CLASSES = "target" + File.separator + "classes";
    int EXIT_NORMAL = 0;
    String JUNIT_REPORT = "TEST-junit-jupiter.xml";
    // Log labels
    String LOG_LABEL = "[GENIE]";
    String TIMER_LABEL = "[GENIE-TIMER]";
    String EARLY_EXIT_LABEL = "[EARLY-EXIT]";
    // Test generation tools
    String GENIE = "genie"; // Default value, which runs both tools.
    String EVOSUITE = "evosuite";
    String RANDOOP = "randoop";
    // Jars and executables
    String JARS = "jars";
    String EXECUTABLES = "executables";
    String EVOSUITE_JAR = "evosuite-master-1.2.1-SNAPSHOT.jar"; // Only this particular jar works.
    String RANDOOP_JAR = "randoop-all-4.3.3.jar";
    String JACOCO_AGENT_JAR = "org.jacoco.agent-0.8.12-runtime.jar";
    String JUNIT_STANDALONE_JAR = "junit-platform-console-standalone-1.12.0.jar";
    String MAJOR_VERSION = "2.0.0"; // This version supports Java 8.
    String MAJOR_EXECUTABLE = "major";
}
