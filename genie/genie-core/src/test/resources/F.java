package test;

import java.io.IOException;

class F {

   protected char[] buffer;

   public char readChar() throws IOException {
      char c;
      int bufpos = 0;
      buffer[bufpos] = c = (char) (hexval(c) << 12 |
            hexval(ReadByte()) << 8 |
            hexval(ReadByte()) << 4 |
            hexval(ReadByte()));

      return c;
   }

   public int hexval(char c) throws IOException {
      if (c >= '0' && c <= '9')
         return (c - '0');
      else if (c >= 'a' && c <= 'f')
         return (c - 'a' + 10);
      else if (c >= 'A' && c <= 'F')
         return (c - 'A' + 10);
      else
         throw new IOException();
   }

   protected char ReadByte() throws IOException {
      return 0;
   }
}
