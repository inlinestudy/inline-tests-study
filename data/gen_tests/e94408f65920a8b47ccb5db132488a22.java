src/main/java/org/bychan/core/basic/Repl.java;67;itest("", 66).given(snippet,"").checkTrue(group());
src/main/java/org/bychan/core/basic/Repl.java;67;itest("", 66).given(snippet,"^(quit|end|q)$").checkFalse(group());
src/main/java/org/bychan/core/basic/Repl.java;67;itest("", 66).given(snippet,"q").checkTrue(group());
