src/main/java/org/java_websocket/SSLSocketChannel2.java;235;itest("", 235).given(inData.remaining(), 0).given(log.isTraceEnabled(), false).checkFalse(group());
src/main/java/org/java_websocket/SSLSocketChannel2.java;235;itest("", 235).given(inData.remaining(), 0).given(log.isTraceEnabled(), true).checkFalse(group());
src/main/java/org/java_websocket/SSLSocketChannel2.java;235;itest("", 235).given(inData.remaining(), 1).given(log.isTraceEnabled(), false).checkFalse(group());
src/main/java/org/java_websocket/SSLSocketChannel2.java;235;itest("", 235).given(inData.remaining(), 1).given(log.isTraceEnabled(), true).checkTrue(group());
