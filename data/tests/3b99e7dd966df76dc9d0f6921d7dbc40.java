src/main/java/org/mitre/dsmiley/httpproxy/ProxyServlet.java;429;itest("FirstTrue", 429).given(header.getName(), "set-cookie").checkTrue(group());
src/main/java/org/mitre/dsmiley/httpproxy/ProxyServlet.java;429;itest("SecondTrue", 429).given(header.getName(), "set-cookie2").checkTrue(group());
src/main/java/org/mitre/dsmiley/httpproxy/ProxyServlet.java;429;itest("BothFalse", 429).given(header.getName(), "Cookie").checkFalse(group());
