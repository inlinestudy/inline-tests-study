src/main/java/net/openhft/chronicle/map/serialization/impl/ByteArrayMarshaller.java;61;itest("", 61).given(size,1L).given(ba.length,-1).given(ba,new byte[] {-128, 0, -127, 1, -1, 0, -31, 23, 0}).checkTrue(group());
src/main/java/net/openhft/chronicle/map/serialization/impl/ByteArrayMarshaller.java;61;itest("", 61).given(size,0L).given(ba.length,0).given(ba,new byte[] {1, 0, 1}).checkFalse(group());
src/main/java/net/openhft/chronicle/map/serialization/impl/ByteArrayMarshaller.java;61;itest("", 61).given(size,0L).given(ba.length,936).given(ba,new byte[] {}).checkTrue(group());
src/main/java/net/openhft/chronicle/map/serialization/impl/ByteArrayMarshaller.java;61;itest("", 61).given(size,0L).given(ba.length,0).given(ba,null).checkTrue(group());
