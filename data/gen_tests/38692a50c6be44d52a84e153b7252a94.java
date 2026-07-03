src/main/java/com/moandjiezana/toml/Identifier.java;104;itest("", 105).given(quoted,true).given(ALLOWED_CHARS_KEYS,"").given(c,'O').checkFalse(group());
src/main/java/com/moandjiezana/toml/Identifier.java;104;itest("", 105).given(quoted,false).given(ALLOWED_CHARS_KEYS,"").given(c,'C').checkTrue(group());
src/main/java/com/moandjiezana/toml/Identifier.java;104;itest("", 105).given(quoted,false).given(ALLOWED_CHARS_KEYS,"NAI==qIlai").given(c,'').checkFalse(group());
