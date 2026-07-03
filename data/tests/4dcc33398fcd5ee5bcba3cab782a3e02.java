mapreduce/src/main/java/com/marklogic/mapreduce/ForestDocument.java;145;itest("", 145).given(metadata, null).checkTrue(group());
mapreduce/src/main/java/com/marklogic/mapreduce/ForestDocument.java;145;itest("", 145).given(metadata, new HashMap<String, String>()).checkTrue(group());
mapreduce/src/main/java/com/marklogic/mapreduce/ForestDocument.java;145;itest("", 145).given(metadata, new HashMap<String, String>(){{put("a", "b");}}).checkFalse(group());
