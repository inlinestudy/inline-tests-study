src/main/java/org/codelibs/elasticsearch/sstmpl/filter/SearchActionFilter.java;87;itest("EmptyCase", 87).given(templateSource, new org.elasticsearch.common.bytes.BytesArray("")).checkFalse(group());
src/main/java/org/codelibs/elasticsearch/sstmpl/filter/SearchActionFilter.java;87;itest("NullCase", 87).given(templateSource, null).checkFalse(group());
src/main/java/org/codelibs/elasticsearch/sstmpl/filter/SearchActionFilter.java;87;itest("TrueCase", 87).given(templateSource, new org.elasticsearch.common.bytes.BytesArray("{}")).checkTrue(group());
