src/main/java/net/snowflake/client/jdbc/SnowflakeDatabaseMetaData.java;2769;itest("", 2769).given(e.getSQLState(), SqlState.NO_DATA).checkTrue(group());
src/main/java/net/snowflake/client/jdbc/SnowflakeDatabaseMetaData.java;2769;itest("", 2769).given(e.getSQLState(), SqlState.BASE_TABLE_OR_VIEW_NOT_FOUND).checkTrue(group());
src/main/java/net/snowflake/client/jdbc/SnowflakeDatabaseMetaData.java;2769;itest("", 2769).given(e.getSQLState(), SqlState.INTERNAL_ERROR).checkFalse(group());
