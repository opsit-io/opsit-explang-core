package io.opsit.explang.atom;

import  io.opsit.explang.ParseCtx;
import  io.opsit.explang.Keyword;

public class KeywordParser implements AtomParser {
  @Override
  public boolean parse(String str, Object[] holder, ParseCtx pctx) {
    if (str.length() < 2 || ':'!=str.charAt(0) )  {
      return false;
    }
    holder[0]= new Keyword(str);
    return true;
  }
}
