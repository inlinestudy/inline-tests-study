package test;

import java.util.Set;

class Z {

  static <T extends Enum<T>> String toHex(Set<T> options) {
    int v = 0;
    for (T option : options) {
      v |= 1 << option.getValue();
    }
    return Integer.toHexString(v);
  }
}
