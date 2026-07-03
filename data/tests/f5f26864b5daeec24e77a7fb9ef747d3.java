src/main/java/net/sf/mpxj/RecurringData.java;663;itest("", 663).given(calendar, new Calendar.Builder().setInstant(1000).build()).given(startDate, 1000).checkTrue(group());
src/main/java/net/sf/mpxj/RecurringData.java;663;itest("", 663).given(calendar, new Calendar.Builder().setInstant(1001).build()).given(startDate, 1000).checkTrue(group());
src/main/java/net/sf/mpxj/RecurringData.java;663;itest("", 663).given(calendar, new Calendar.Builder().setInstant(999).build()).given(startDate, 1000).checkFalse(group());
