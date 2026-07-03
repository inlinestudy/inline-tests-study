src/main/java/com/github/jinahya/bit/io/BitIoConstraints.java;101;itest("", 101).given(i, 2).given(MAX_SIZES, new int[]{1, 10, 2}).checkEq(MAX_SIZES[0], 1).checkEq(MAX_SIZES[1], 10).checkEq(MAX_SIZES[2], 20);
src/main/java/com/github/jinahya/bit/io/BitIoConstraints.java;101;itest("", 101).given(MAX_SIZES[i - 1], 10).checkEq(MAX_SIZES[i], 20);
