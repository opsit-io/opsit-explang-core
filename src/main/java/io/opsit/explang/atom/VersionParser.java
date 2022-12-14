package io.opsit.explang.atom;

import io.opsit.explang.ParseCtx;
import io.opsit.version.Version;
 
public  class VersionParser implements AtomParser {
  private static EscStringParser sp = new EscStringParser();

  @Override
  public boolean parse(String str, Object[]holder, ParseCtx pctx) {
    if (str.length() < 4) {
      return false;
    }
    if (!str.startsWith("v\"")) {
      return false;
    }
    final String [] stringHolder = new String[1];
    boolean result = sp.parse(str.substring(1), stringHolder, pctx); 
    if (result) {
      try {
        holder[0] = Version.parseVersion(stringHolder[0]);
        return true;
      } catch (Exception ex) {
        throw new RuntimeException(ex);
      }
    }
    return false;
  }
}
