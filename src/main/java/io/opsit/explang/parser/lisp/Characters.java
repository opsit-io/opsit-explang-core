package io.opsit.explang.parser.lisp;

import io.opsit.explang.ParserException;
import io.opsit.explang.Utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Characters {
  public static final Map<String,Character> nameToChar =
      Utils.map("NULL",(char)0, "NUL",(char)0, 
                "SOH",      (char)1,
                "STX",      (char)2,
                "ETX",      (char)3,
                "EOT",      (char)4,
                "ENQ",      (char)5,
                "ACK",      (char)6,
                "BELL", (char)7, "BEL", (char)7,
                "BACKSPACE",(char)8, "BS", (char)8,
                "TAB",      (char)9, "HT", (char)9,
                "NEWLINE", (char)10, "LINEFEED",(char)10,"LF",(char)10,
                "VT",      (char)11,
                "PAGE",    (char)12, "FF", (char)12,
                "RETURN",  (char)13, "CR", (char)13,
                "SO",      (char)14,
                "SI",      (char)15,
                "DLE",     (char)16,
                "DC1",     (char)17,
                "DC2",     (char)18,
                "DC3",     (char)19,
                "DC4",     (char)20,
                "NAK",     (char)21,
                "SYN",     (char)22,
                "ETB",     (char)23,
                "CAN",     (char)24,
                "EM",      (char)25,
                "SUB",     (char)26,
                "ESCAPE",  (char)27, "ESC", (char)27,
                "FS",      (char)28,
                "GS",      (char)29,
                "RS",      (char)30,
                "US",      (char)31,
                "SPACE",   (char)32,
                "DEL",     (char)127, "RUBOUT", (char)127,"DELETE",(char)127,
                "NBSP",    (char)160, "NO-BREAK_SPACE", (char)160);
    
  private static final Map<String, Integer> codePoints =
      Collections.synchronizedMap(new HashMap<String, Integer>(1024));
  private static volatile boolean incomplete = true;
  private static volatile int lastLookup = Character.MIN_CODE_POINT - 1;

  /**
   * Get character code by name of code point.
   */
  // FIXME: is there better way to get the mapping?
  public static int getCodePoint(String name) {
    name = name.toUpperCase().replace('_', ' ');
    final Integer cp = codePoints.get(name);
    if (cp == null && incomplete) {
      synchronized (codePoints) {
        while (lastLookup < Character.MAX_CODE_POINT) {
          lastLookup++;
          String unicodeName = Character.getName(lastLookup);
          if (unicodeName != null) {
            codePoints.put(unicodeName.toUpperCase(), lastLookup);
            if (unicodeName.equalsIgnoreCase(name)) {
              return lastLookup;
            }
          }
        }
        incomplete = false;
      }
    }
    return null == cp ? -1 : cp;
  }

  /**
   * Get character by name of code point.
   */
  public static char nameToChar(String name) throws ParserException {
    String upper = name.toUpperCase();
    Character chr = Characters.nameToChar.get(upper);
    if (chr != null) {
      return chr;
    }
    if (upper.startsWith("U")) {
      int length = upper.length();
      if (length > 1 && length <= 5) {
        try {
          final int i = Integer.parseInt(upper.substring(1), 16);
          return (char) i;
        } catch (NumberFormatException e) {
          // fall through
        }
        ;
      }
    }
    int cp = getCodePoint(upper);
    if (cp >= 0) {
      return (char) cp;
    }
    throw new ParserException("Unrecognized character name: \"" + name + '"');
  }
}
