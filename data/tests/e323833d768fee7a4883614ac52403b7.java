src/main/java/com/lazerycode/jmeter/testrunner/TestManager.java;192;itest("", 192).given(outputReportFolder.exists(), true).given(outputReportFolder.mkdirs(), true).checkTrue(group());
src/main/java/com/lazerycode/jmeter/testrunner/TestManager.java;192;itest("", 192).given(outputReportFolder.exists(), false).given(outputReportFolder.mkdirs(), true).checkTrue(group());
src/main/java/com/lazerycode/jmeter/testrunner/TestManager.java;192;itest("", 192).given(outputReportFolder.exists(), true).given(outputReportFolder.mkdirs(), false).checkTrue(group());
src/main/java/com/lazerycode/jmeter/testrunner/TestManager.java;192;itest("", 192).given(outputReportFolder.exists(), false).given(outputReportFolder.mkdirs(), false).checkFalse(group());
