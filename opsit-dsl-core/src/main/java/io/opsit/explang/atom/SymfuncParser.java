package io.opsit.explang.atom;

import io.opsit.explang.ParseCtx;
import static io.opsit.explang.Utils.symbol;
import static io.opsit.explang.Utils.list;

public  class SymfuncParser implements AtomParser {
  static private EscStringParser sp = new EscStringParser();
  public boolean parse(String str, Object[]holder, ParseCtx pctx) {
    if (str.length() < 4) {
      return false;
    }
    if (!str.startsWith("f\"")) {
      return false;
    }
    final String [] stringHolder = new String[1];
    boolean result = sp.parse(str.substring(1), stringHolder, pctx); 
    if (result) {
      holder[0] = list(symbol("FUNCTION"), symbol(stringHolder[0]));
      return true;
    }
    return false;
  }
}
