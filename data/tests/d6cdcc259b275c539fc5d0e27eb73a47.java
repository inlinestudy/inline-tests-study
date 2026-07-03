src/main/java/org/nanopub/extra/server/NanopubSurfacePattern.java;12;itest("FirstTrue", 12).given(uriPattern, null).checkTrue(group());
src/main/java/org/nanopub/extra/server/NanopubSurfacePattern.java;12;itest("SecondTrue", 12).given(uriPattern, "").checkTrue(group());
src/main/java/org/nanopub/extra/server/NanopubSurfacePattern.java;12;itest("BothFalse", 12).given(uriPattern, "abc").checkFalse(group());
