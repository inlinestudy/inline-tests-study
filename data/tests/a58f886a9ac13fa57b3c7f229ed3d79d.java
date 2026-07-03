src/main/java/org/takes/ts/fork/FkRegex.java;127;itest("", 127).given(this.pattern, Pattern.compile("abc")).given(new RqQuery(req).query().getPath(), "xxabcxx").checkTrue(matcher.find());
