src/main/java/com/mercadopago/net/MPRestClient.java;193;itest("", 193).given(requestOptions, MPRequestOptions.builder().setAccessToken(null).build()).checkFalse(group());
src/main/java/com/mercadopago/net/MPRestClient.java;193;itest("", 193).given(requestOptions, MPRequestOptions.builder().setAccessToken("").build()).checkFalse(group());
src/main/java/com/mercadopago/net/MPRestClient.java;193;itest("", 193).given(requestOptions, MPRequestOptions.builder().setAccessToken("null").build()).checkTrue(group());
