src/main/java/xdean/jex/util/string/StringUtil.java;82;itest("", 82).given(from, 10).given(to, 5).given(sourceText, "helloworld").checkTrue(group());
src/main/java/xdean/jex/util/string/StringUtil.java;82;itest("", 82).given(from, -1).given(to, 5).given(sourceText, "helloworld").checkTrue(group());
src/main/java/xdean/jex/util/string/StringUtil.java;82;itest("", 82).given(from, 0).given(to, 5).given(sourceText, "hi").checkTrue(group());
src/main/java/xdean/jex/util/string/StringUtil.java;82;itest("", 82).given(from, 0).given(to, 5).given(sourceText, "helloworld").checkFalse(group());
