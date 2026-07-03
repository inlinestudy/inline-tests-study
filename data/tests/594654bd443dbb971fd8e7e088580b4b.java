src/main/java/com/github/davidmoten/rx/jdbc/QueryUpdateOnSubscribe.java;215;itest("", 215).given(query.context().batchSize(), 2).given(query.context().isTransactionOpen(), false).checkTrue(group());
src/main/java/com/github/davidmoten/rx/jdbc/QueryUpdateOnSubscribe.java;215;itest("", 215).given(query.context().batchSize(), 2).given(query.context().isTransactionOpen(), true).checkFalse(group());
src/main/java/com/github/davidmoten/rx/jdbc/QueryUpdateOnSubscribe.java;215;itest("", 215).given(query.context().batchSize(), 1).given(query.context().isTransactionOpen(), false).checkFalse(group());
src/main/java/com/github/davidmoten/rx/jdbc/QueryUpdateOnSubscribe.java;215;itest("", 215).given(query.context().batchSize(), 1).given(query.context().isTransactionOpen(), true).checkFalse(group());
