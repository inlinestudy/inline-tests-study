src/main/java/com/lambdaworks/redis/protocol/CommandHandler.java;149;itest("", 149).given(channel, new io.netty.channel.local.LocalChannel()).given(connected, true).checkTrue(group());
src/main/java/com/lambdaworks/redis/protocol/CommandHandler.java;149;itest("", 149).given(channel, new io.netty.channel.local.LocalChannel()).given(connected, false).checkFalse(group());
src/main/java/com/lambdaworks/redis/protocol/CommandHandler.java;149;itest("", 149).given(channel, null).given(connected, true).checkFalse(group());
src/main/java/com/lambdaworks/redis/protocol/CommandHandler.java;149;itest("", 149).given(channel, null).given(connected, false).checkFalse(group());
