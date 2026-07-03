src/main/java/com/popbill/api/hometax/HTTaxinvoiceServiceImp.java;197;itest("", 197).given(TaxRegID, "").checkFalse(group());
src/main/java/com/popbill/api/hometax/HTTaxinvoiceServiceImp.java;197;itest("", 197).given(TaxRegID, "x").checkTrue(group());
src/main/java/com/popbill/api/hometax/HTTaxinvoiceServiceImp.java;197;itest("", 197).given(TaxRegID, " ").checkTrue(group());
src/main/java/com/popbill/api/hometax/HTTaxinvoiceServiceImp.java;197;itest("", 197).given(TaxRegID, null).checkFalse(group());
