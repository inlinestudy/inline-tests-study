src/main/java/org/dasein/cloud/azure/Azure.java;165;itest("", 165).given(storageEndpoint, null).checkTrue(group());
src/main/java/org/dasein/cloud/azure/Azure.java;165;itest("", 165).given(storageEndpoint, "").checkTrue(group());
src/main/java/org/dasein/cloud/azure/Azure.java;165;itest("", 165).given(storageEndpoint, " ").checkFalse(group());
