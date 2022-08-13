package io.opsit.explang.atom;

import io.opsit.explang.ParseCtx;

public class NullParser implements AtomParser {
  @Override
  public boolean parse(String str, Object[] holder, ParseCtx pctx) {
    if ("null".equalsIgnoreCase(str) || "nil".equalsIgnoreCase(str)) {
      holder[0] = null;
      return true;
    } else {
      return false;
    }
  }
}
