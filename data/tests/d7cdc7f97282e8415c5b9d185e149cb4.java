src/main/java/nonapi/io/github/classgraph/fastzipfilereader/ZipFileSliceReader.java;131;itest("", 131).given(off, -1).given(zipFileSlice.len, 10).checkTrue(group());
src/main/java/nonapi/io/github/classgraph/fastzipfilereader/ZipFileSliceReader.java;131;itest("", 131).given(off, 7).given(zipFileSlice.len, 10).checkTrue(group());
src/main/java/nonapi/io/github/classgraph/fastzipfilereader/ZipFileSliceReader.java;131;itest("", 131).given(off, 6).given(zipFileSlice.len, 10).checkFalse(group());
src/main/java/nonapi/io/github/classgraph/fastzipfilereader/ZipFileSliceReader.java;131;itest("", 131).given(off, 5).given(zipFileSlice.len, 10).checkFalse(group());
