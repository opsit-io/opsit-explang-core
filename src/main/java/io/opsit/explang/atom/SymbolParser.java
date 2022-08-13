package io.opsit.explang.atom;

import io.opsit.explang.ParseCtx;
import io.opsit.explang.Symbol;

public class SymbolParser implements AtomParser {

  @Override
  public boolean parse(String str, Object[] holder, ParseCtx pctx) throws AtomParseException {
    if (str.length() == 0) {
      throw new AtomParseException(pctx, "Symbol cannot be empty");
    }
    holder[0] = new Symbol(str);
    return true;
  }
}
