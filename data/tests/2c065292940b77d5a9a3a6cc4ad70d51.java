src/main/java/org/casbin/jcasbin/main/Enforcer.java;75;itest("", 75).given(this.adapter, new org.casbin.jcasbin.persist.file_adapter.FilteredAdapter("foo")).given(isFiltered(), false).checkTrue(group());
src/main/java/org/casbin/jcasbin/main/Enforcer.java;75;itest("", 75).given(this.adapter, new org.casbin.jcasbin.persist.file_adapter.FilteredAdapter("foo")).given(isFiltered(), true).checkFalse(group());
src/main/java/org/casbin/jcasbin/main/Enforcer.java;75;itest("", 75).given(this.adapter, null).given(isFiltered(), false).checkFalse(group());
src/main/java/org/casbin/jcasbin/main/Enforcer.java;75;itest("", 75).given(this.adapter, null).given(isFiltered(), true).checkFalse(group());
