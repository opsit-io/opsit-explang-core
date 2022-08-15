package io.opsit.explang.strconv.nop;

import io.opsit.explang.IStringConverter;

public class NopConverter implements IStringConverter {
  public String convert(String in) {
    return in;
  }
}
