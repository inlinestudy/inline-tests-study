src/main/java/org/opensky/libadsb/ModeSDecoder.java;148;itest("", 148).given(es1090.getMessage()[1], 0x0).checkFalse(hasMe11Bit);
src/main/java/org/opensky/libadsb/ModeSDecoder.java;148;itest("", 148).given(es1090.getMessage()[1], 0x20).checkTrue(hasMe11Bit);
src/main/java/org/opensky/libadsb/ModeSDecoder.java;148;itest("", 148).given(es1090.getMessage()[1], 0x10).checkFalse(hasMe11Bit);
src/main/java/org/opensky/libadsb/ModeSDecoder.java;148;itest("", 148).given(es1090.getMessage()[1], 0x30).checkTrue(hasMe11Bit);
