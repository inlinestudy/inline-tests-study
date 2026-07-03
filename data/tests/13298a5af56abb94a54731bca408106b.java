src/main/java/org/rythmengine/RythmEngine.java;1782;itest("", 1782).given(param.name, "foo").given(param.value, new String("bar")).checkTrue(group());
src/main/java/org/rythmengine/RythmEngine.java;1782;itest("", 1782).given(param.name, null).given(param.value, null).checkFalse(group());
src/main/java/org/rythmengine/RythmEngine.java;1782;itest("", 1782).given(param.name, null).given(param.value, new String("bar")).checkFalse(group());
src/main/java/org/rythmengine/RythmEngine.java;1782;itest("", 1782).given(param.name, "foo").given(param.value, null).checkFalse(group());
