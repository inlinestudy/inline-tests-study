src/main/java/com/neovisionaries/ws/client/WebSocketInputStream.java;126;itest("", 126).given(buffer,new byte[] {0, -128}).checkEq(masked,true);
src/main/java/com/neovisionaries/ws/client/WebSocketInputStream.java;126;itest("", 126).given(buffer,new byte[] {-1, 53, -114, 85, -128, 1, -127, 1, -128}).checkEq(masked,false);
