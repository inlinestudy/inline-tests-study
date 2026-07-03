src/main/java/com/lambdaworks/codec/CRC16.java;55;itest("", 55).given(b,1).given(crc,1).given(LOOKUP_TABLE,new int[] {1, 1, 1, 1}).checkEq(crc,257);
src/main/java/com/lambdaworks/codec/CRC16.java;55;itest("", 55).given(b,-1).given(crc,-1424).given(LOOKUP_TABLE,new int[] {-1, -1424, 0, -1, -1, -1, -1, -1424, -1424}).checkEq(crc,36863);
