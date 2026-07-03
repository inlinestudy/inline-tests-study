src/main/java/org/osgl/inject/BeanSpec.java;442;itest("", 442).given(valueLoader,"0.xml").given(elementLoaders,"1.xml").given(isContainer,true).checkFalse(group());
src/main/java/org/osgl/inject/BeanSpec.java;442;itest("", 442).given(valueLoader,null).given(elementLoaders,"6.xml").given(isContainer,true).checkFalse(group());
src/main/java/org/osgl/inject/BeanSpec.java;442;itest("", 442).given(valueLoader,null).given(elementLoaders,"1.xml").given(isContainer,true).checkTrue(group());
