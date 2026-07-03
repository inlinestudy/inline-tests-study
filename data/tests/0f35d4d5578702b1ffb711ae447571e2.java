src/main/java/net/snowflake/client/jdbc/cloud/storage/SnowflakeGCSClient.java;1039;itest("", 1039).given(isEncrypting(), true).given(getEncryptionKeySize(), 256).checkTrue(group());
src/main/java/net/snowflake/client/jdbc/cloud/storage/SnowflakeGCSClient.java;1039;itest("", 1039).given(isEncrypting(), false).given(getEncryptionKeySize(), 256).checkFalse(group());
src/main/java/net/snowflake/client/jdbc/cloud/storage/SnowflakeGCSClient.java;1039;itest("", 1039).given(isEncrypting(), true).given(getEncryptionKeySize(), 257).checkFalse(group());
src/main/java/net/snowflake/client/jdbc/cloud/storage/SnowflakeGCSClient.java;1039;itest("", 1039).given(isEncrypting(), false).given(getEncryptionKeySize(), 257).checkFalse(group());
