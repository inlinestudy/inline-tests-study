src/main/java/io/github/classgraph/json/JSONParser.java;271;itest("", 271).given(longVal, -2147483648L).checkTrue(group());
src/main/java/io/github/classgraph/json/JSONParser.java;271;itest("", 271).given(longVal, -2147483647L).checkTrue(group());
src/main/java/io/github/classgraph/json/JSONParser.java;271;itest("", 271).given(longVal, -2147483649L).checkFalse(group());
src/main/java/io/github/classgraph/json/JSONParser.java;271;itest("", 271).given(longVal, 2147483647L).checkTrue(group());
src/main/java/io/github/classgraph/json/JSONParser.java;271;itest("", 271).given(longVal, 2147483646L).checkTrue(group());
src/main/java/io/github/classgraph/json/JSONParser.java;271;itest("", 271).given(longVal, 2147483649L).checkFalse(group());
