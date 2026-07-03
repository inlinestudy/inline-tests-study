src/main/java/org/jcodec/codecs/aac/blocks/BlockICS.java;148;itest("", 148).given(in,"0.xml").given(bits,32).given(sect_len_incr,0).checkTrue(group());
src/main/java/org/jcodec/codecs/aac/blocks/BlockICS.java;148;itest("", 148).given(in,"4.xml").given(bits,48).given(sect_len_incr,-2587).checkFalse(group());
src/main/java/org/jcodec/codecs/aac/blocks/BlockICS.java;148;itest("", 148).given(in,"5.xml").given(bits,-1990).given(sect_len_incr,-2995).checkTrue(group());
src/main/java/org/jcodec/codecs/aac/blocks/BlockICS.java;148;itest("", 148).given(in,"1.xml").given(bits,937).given(sect_len_incr,937).checkFalse(group());
src/main/java/org/jcodec/codecs/aac/blocks/BlockICS.java;148;itest("", 148).given(in,"7.xml").given(bits,262143).given(sect_len_incr,0).checkFalse(group());
