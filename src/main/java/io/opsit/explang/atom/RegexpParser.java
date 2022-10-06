package io.opsit.explang.atom;

import io.opsit.explang.GlobPattern;
import io.opsit.explang.ParseCtx;
import io.opsit.explang.Utils;
import java.util.regex.Pattern;


public class RegexpParser implements AtomParser {
  @Override
  public boolean parse(String str, Object[] holder, ParseCtx pctx) {
    boolean isGlob;
    if (str.length() < 3) {
      return false;
    }
    if (str.startsWith("r\"")) {
      isGlob = false;
    } else if (str.startsWith("g\"")) {
      isGlob = true;
    } else {
      return false;
    }
    StringBuilder sb = new StringBuilder();
    int flags = 0;
    // Exception problem = null;
    boolean inQuote = false;
    for (int i = 1; i < str.length(); i++) {
      char c = str.charAt(i);
      if (c == '"') {
        inQuote = !inQuote;
        continue;
      }
      if (!inQuote) {
        final int f = Utils.parseRegexpFlag(c);
        if (0 == c) {
          return false;
        }
        flags |= f;
        continue;
      }
      if (c == '\\') {
        i++;
        if (i < str.length()) {
          c = str.charAt(i);
          switch (c) {
            case '\"':
              break;
            default:
              sb.append('\\');
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
    // System.out.println("RegexpParser: str='" + sb.toString() + "', flags=" + flags);
    if (isGlob) {
      holder[0] = GlobPattern.compile(sb.toString(), flags);
    } else {
      holder[0] = Pattern.compile(sb.toString(), flags);
    }
    return true;
  }
}
