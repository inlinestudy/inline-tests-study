src/main/java/com/smattme/MysqlImportService.java;50;itest("", 50).given(jdbcConnString, null).checkTrue(group());
src/main/java/com/smattme/MysqlImportService.java;50;itest("", 50).given(jdbcConnString, "").checkTrue(group());
src/main/java/com/smattme/MysqlImportService.java;50;itest("", 50).given(jdbcConnString, "foo").checkFalse(group());
