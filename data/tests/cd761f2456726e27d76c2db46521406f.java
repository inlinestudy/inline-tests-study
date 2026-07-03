src/main/java/org/ansj/library/DATDictionary.java;149;itest("", 149).given(item, new AnsjItem()).given(item.getStatus(), 0).checkTrue(group());
src/main/java/org/ansj/library/DATDictionary.java;149;itest("", 149).given(item, new AnsjItem()).given(item.getStatus(), 2).checkFalse(group());
src/main/java/org/ansj/library/DATDictionary.java;149;itest("", 149).given(item, new AnsjItem()).given(item.getStatus(), 1).checkTrue(group());
src/main/java/org/ansj/library/DATDictionary.java;149;itest("", 149).given(item, null).checkTrue(group());
