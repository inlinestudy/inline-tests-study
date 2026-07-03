src/main/java/org/gitlab4j/api/NamespaceApi.java;90;itest("", 90).given(query, "foo").given(page, 5).given(perPage, 1).checkEq(formData.asMap().toString(), "{search=[foo], page=[5], per_page=[1]}");
