package io.opsit.explang.atom;

import io.opsit.explang.ParseCtx;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Locale;

public class NumberParser implements AtomParser {
  protected static final Locale numLocale = Locale.forLanguageTag("en-US");

  @Override
  // FIXME: DRY with Utils.parseNumber(), exc. handling, configurable Locale
  public boolean parse(String str, Object[] holder, ParseCtx pctx) throws AtomParseException {
    if (((str.length() > 1) && (str.startsWith("+") || str.startsWith("-")))
        || ((str.length() > 0) && (str.charAt(0) >= '0' && str.charAt(0) <= '9'))) {
      NumberFormat nf = NumberFormat.getInstance(numLocale);
      ParsePosition pos = new ParsePosition(str.startsWith("+") ? 1 : 0);
      try {
        // FIXME: toUpperCase here  looks like kluge, what a proper way to handle it?
        // should we just document that we use locale settings
        // or make it configurable?
        Number n = (Number) nf.parseObject(str.toUpperCase(), pos);
        Character typeSpec = null;
        if (pos.getIndex() + 1 == str.length()) {
          typeSpec = str.charAt(pos.getIndex());
        } else if (pos.getIndex() < str.length()) {
          return false;
        }
        if (str.contains(".") && (n instanceof Long)) {
          n = n.doubleValue();
        }
        if (null != typeSpec) {
          switch (typeSpec.charValue()) {
            case 'l':
            case 'L':
              holder[0] = n.longValue();
              break;
            case 'b':
            case 'B':
              holder[0] = n.byteValue();
              break;
            case 's':
            case 'S':
              holder[0] = n.shortValue();
              break;
            case 'i':
            case 'I':
              holder[0] = n.intValue();
              break;
            case 'f':
            case 'F':
              holder[0] = n.floatValue();
              break;
            case 'd':
            case 'D':
              holder[0] = n.doubleValue();
              break;
            default:
              throw new AtomParseException(
                  pctx,
                  String.format(
                      "Failed to parse numeric literal '%s': invalid type modifier %s",
                      str, typeSpec));
          }
        } else {
          holder[0] = (n instanceof Double) ? n : n.intValue();
        }
        return true;
      } catch (Exception ex) {
        throw new AtomParseException(
            pctx, String.format("Failed to parse numeric literal '%s'", str));
      }
    } else {
      return false;
    }
  }
}
