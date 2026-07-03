src/main/java/org/pf4j/update/UpdateManager.java;222;itest("", 222).given(repositoriesJson, Paths.get("repositories.json")).checkTrue(group());
src/main/java/org/pf4j/update/UpdateManager.java;222;itest("", 222).given(repositoriesJson, Paths.get("repositories.jsonx")).checkFalse(group());
src/main/java/org/pf4j/update/UpdateManager.java;222;itest("", 222).given(repositoriesJson, null).checkFalse(group());
