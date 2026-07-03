src/main/java/com/lambdaworks/redis/resource/DefaultClientResources.java;96;itest("", 96).given(computationThreadPoolSize, 1).checkTrue(group());
src/main/java/com/lambdaworks/redis/resource/DefaultClientResources.java;96;itest("", 96).given(computationThreadPoolSize, 2).checkTrue(group());
src/main/java/com/lambdaworks/redis/resource/DefaultClientResources.java;96;itest("", 96).given(computationThreadPoolSize, 3).checkFalse(group());
src/main/java/com/lambdaworks/redis/resource/DefaultClientResources.java;96;itest("", 96).given(computationThreadPoolSize, 4).checkFalse(group());
