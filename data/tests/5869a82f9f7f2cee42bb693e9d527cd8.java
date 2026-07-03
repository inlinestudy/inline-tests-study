src/main/java/io/github/bonigarcia/wdm/Shell.java;63;itest("", 63).given(command, "ls").given(folder, new File(".")).checkTrue(IOUtils.toString(process.getInputStream(), UTF_8).contains("pom.xml"));
