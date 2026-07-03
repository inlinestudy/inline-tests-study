src/main/java/com/adobe/epubcheck/ocf/OCFContainer.java;134;itest("", 134).given(url.isHierarchical(), true).given(contains(url), false).checkFalse(group());
src/main/java/com/adobe/epubcheck/ocf/OCFContainer.java;134;itest("", 134).given(url.isHierarchical(), false).given(contains(url), false).checkTrue(group());
src/main/java/com/adobe/epubcheck/ocf/OCFContainer.java;134;itest("", 134).given(url.isHierarchical(), true).given(contains(url), true).checkTrue(group());
src/main/java/com/adobe/epubcheck/ocf/OCFContainer.java;134;itest("", 134).given(url.isHierarchical(), false).given(contains(url), true).checkTrue(group());
