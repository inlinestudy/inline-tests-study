src/main/java/dig.java;110;itest("", 110).given(port, 0).checkFalse(group());
src/main/java/dig.java;110;itest("", 110).given(port, 65535).checkFalse(group());
src/main/java/dig.java;110;itest("", 110).given(port, 1).checkFalse(group());
src/main/java/dig.java;110;itest("", 110).given(port, 65534).checkFalse(group());
src/main/java/dig.java;110;itest("", 110).given(port, -1).checkTrue(group());
src/main/java/dig.java;110;itest("", 110).given(port, 65536).checkTrue(group());
