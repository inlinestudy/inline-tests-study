src/main/java/net/jodah/lyra/internal/ChannelHandler.java;54;itest("", 54).given(closed, true).given(method.getDeclaringClass(), Channel.class).checkTrue(group());
src/main/java/net/jodah/lyra/internal/ChannelHandler.java;54;itest("", 54).given(closed, true).given(method.getDeclaringClass(), String.class).checkFalse(group());
src/main/java/net/jodah/lyra/internal/ChannelHandler.java;54;itest("", 54).given(closed, false).given(method.getDeclaringClass(), Channel.class).checkFalse(group());
src/main/java/net/jodah/lyra/internal/ChannelHandler.java;54;itest("", 54).given(closed, false).given(method.getDeclaringClass(), String.class).checkFalse(group());
