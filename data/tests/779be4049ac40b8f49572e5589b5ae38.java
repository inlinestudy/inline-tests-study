src/main/java/spark/CustomErrorPages.java;38;itest("", 38).given(status, 404).checkEq(customPage, "<html><body><h2>404 Not found</h2></body></html>");
src/main/java/spark/CustomErrorPages.java;38;itest("", 38).given(status, 0).checkEq(customPage, "<html><body><h2>500 Internal Error</h2></body></html>");
