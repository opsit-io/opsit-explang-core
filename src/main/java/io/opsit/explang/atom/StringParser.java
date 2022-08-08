package io.opsit.explang.atom;

import io.opsit.explang.ParseCtx;

public  class StringParser implements AtomParser {
  @Override
  public boolean parse(String str, Object[] holder,  ParseCtx pctx) {
    if (str.length() >=2 &&
        str.startsWith("\"") &&
        str.endsWith("\"")) {
      holder[0]= str.substring(1,str.length()-1);
      return true;
    } else {
      return false;
    }
  }
}
