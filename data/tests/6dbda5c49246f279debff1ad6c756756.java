src/main/java/com/twilio/sdk/readers/UsageTriggerReader.java;22;itest("", 22).given(client.getAccountSid(), "foo").checkEq(request.getMethod(), HttpMethod.GET).checkFalse(request.getUri().isEmpty());
