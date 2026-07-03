src/main/java/org/nutz/weixin/util/Wxs.java;157;itest("", 157).given(xml, "<xml><body>test</body></xml>").checkTrue(map != null);
src/main/java/org/nutz/weixin/util/Wxs.java;157;itest("", 157).given(xml, "<xml><body>good</body></xml>").checkEq(map.toString(), "{body=good}");
