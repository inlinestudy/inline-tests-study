src/main/java/hudson/plugins/emailext/ExtendedEmailPublisherDescriptor.java;325;itest("", 325).given(acc, null).checkTrue(group());
src/main/java/hudson/plugins/emailext/ExtendedEmailPublisherDescriptor.java;325;itest("", 325).given(acc, new MailAccount()).given(acc.getSmtpUsername(), "foo").checkFalse(group());
src/main/java/hudson/plugins/emailext/ExtendedEmailPublisherDescriptor.java;325;itest("", 325).given(acc, new MailAccount()).given(acc.getSmtpUsername(), "").checkTrue(group());
