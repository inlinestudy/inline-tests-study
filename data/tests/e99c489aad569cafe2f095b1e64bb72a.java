src/main/java/nonapi/io/github/classgraph/fastzipfilereader/ZipFileSliceReader.java;151;itest("", 151).given(off, -1).given(zipFileSlice.len, 10).checkTrue(group());
src/main/java/nonapi/io/github/classgraph/fastzipfilereader/ZipFileSliceReader.java;151;itest("", 151).given(off, 3).given(zipFileSlice.len, 10).checkTrue(group());
src/main/java/nonapi/io/github/classgraph/fastzipfilereader/ZipFileSliceReader.java;151;itest("", 151).given(off, 2).given(zipFileSlice.len, 10).checkFalse(group());
