mapreduce/src/main/java/com/marklogic/mapreduce/ContentWriter.java;740;itest("", 740).given(sid, -1).given(pendingUris[sid].size(), 0).checkFalse(group());
mapreduce/src/main/java/com/marklogic/mapreduce/ContentWriter.java;740;itest("", 740).given(sid, -1).given(pendingUris[sid].size(), 1).checkFalse(group());
mapreduce/src/main/java/com/marklogic/mapreduce/ContentWriter.java;740;itest("", 740).given(sid, 0).given(pendingUris[sid].size(), 0).checkFalse(group());
mapreduce/src/main/java/com/marklogic/mapreduce/ContentWriter.java;740;itest("", 740).given(sid, 0).given(pendingUris[sid].size(), 1).checkTrue(group());
