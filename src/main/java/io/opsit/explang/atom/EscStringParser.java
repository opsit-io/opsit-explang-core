package io.opsit.explang.atom;

import io.opsit.explang.ParseCtx;

public class EscStringParser implements AtomParser {
  @Override
  public boolean parse(String str, Object[] holder, ParseCtx pctx) {
    StringBuilder sb = new StringBuilder();
    // Exception problem = null;
    boolean inQuote = false;
    for (int i = 0; i < str.length(); i++) {
      char c = str.charAt(i);
      if (c == '"') {
        inQuote = !inQuote;
        continue;
      }
      if (!inQuote) {
        return false;
      }
      if (c == '\\') {
        i++;
        if (i < str.length()) {
          c = str.charAt(i);
          switch (c) {
            case 'b':
              c = '\b';
              break;
            case 't':
              c = '\t';
              break;
            case 'n':
              c = '\n';
              break;
            case 'f':
              c = '\f';
              break;
            case 'r':
              c = '\r';
              break;
            default:
              break;
          }
        } else {
          return false;
        }
      }
      sb.append(c);
      continue;
    }
    if (inQuote) {
      return false;
    }
    holder[0] = sb.toString();
    return true;
  }
}
