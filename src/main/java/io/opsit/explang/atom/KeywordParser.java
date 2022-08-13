package io.opsit.explang.atom;

import io.opsit.explang.Keyword;
import io.opsit.explang.ParseCtx;

public class KeywordParser implements AtomParser {
  @Override
  public boolean parse(String str, Object[] holder, ParseCtx pctx) {
    if (str.length() < 2 || ':' != str.charAt(0)) {
      return false;
    }
    holder[0] = new Keyword(str);
    return true;
  }
}
