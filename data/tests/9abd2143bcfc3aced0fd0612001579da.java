src/main/java/de/digitalcollections/iiif/model/sharedcanvas/Manifest.java;85;itest("", 85).given(sequences, new ArrayList<>()).checkTrue(group());
src/main/java/de/digitalcollections/iiif/model/sharedcanvas/Manifest.java;85;itest("", 85).given(sequences, null).checkTrue(group());
src/main/java/de/digitalcollections/iiif/model/sharedcanvas/Manifest.java;85;itest("", 85).given(sequences, new ArrayList<>(java.util.Arrays.asList(new Sequence("foo", "bar")))).checkFalse(group());
