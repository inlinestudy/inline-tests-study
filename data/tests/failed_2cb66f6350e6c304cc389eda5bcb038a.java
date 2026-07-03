src/main/java/de/redsix/pdfcompare/PdfComparator.java;149;itest("", 149).given(expectedStreamSupplier,  null).given(actualStreamSupplier, null).checkTrue(group());
src/main/java/de/redsix/pdfcompare/PdfComparator.java;149;itest("", 149).given(expectedStreamSupplier, () -> new FileInputStream("actualFile")).given(actualStreamSupplier, null).checkTrue(group());
src/main/java/de/redsix/pdfcompare/PdfComparator.java;149;itest("", 149).given(expectedStreamSupplier, null).given(actualStreamSupplier, () -> new FileInputStream("actualFile")).checkTrue(group());
src/main/java/de/redsix/pdfcompare/PdfComparator.java;149;itest("", 149).given(expectedStreamSupplier, () -> new FileInputStream("actualFile")).given(actualStreamSupplier, () -> new FileInputStream("actualFile")).checkFalse(group());
