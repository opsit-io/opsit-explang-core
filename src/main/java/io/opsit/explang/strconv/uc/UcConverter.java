package io.opsit.explang.strconv.uc;

import io.opsit.explang.IStringConverter;

public class UcConverter implements IStringConverter {
  public String convert(String str) {
    return null == str ? null : str.toUpperCase();
  }
}
