package io.opsit.explang.parser.lisp;

import io.opsit.explang.ASTN;
import io.opsit.explang.ASTNLeaf;
import io.opsit.explang.ASTNList;
import io.opsit.explang.ArgSpec;
import io.opsit.explang.IParser;
import io.opsit.explang.Keyword;
import io.opsit.explang.ParseCtx;
import io.opsit.explang.ParserEOFException;
import io.opsit.explang.ParserException;
import io.opsit.explang.Utils;
import io.opsit.explang.atom.AtomParseException;
import io.opsit.explang.atom.AtomParser;
import io.opsit.explang.atom.BooleanParser;
import io.opsit.explang.atom.KeywordParser;
import io.opsit.explang.atom.NullParser;
import io.opsit.explang.atom.NumberParser;
import io.opsit.explang.atom.SymbolParser;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.io.PushbackReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public class LispParser implements IParser {

  public interface ReaderMacro {
    public ASTN execute(PushbackInputStream is, Character chr);
  }

  public static final Object READ_COMMENT = new MacroFuncs.ReadComment();
  public static final Object READ_STRING = new MacroFuncs.ReadString();
  public static final Object READ_LIST = new MacroFuncs.ReadList();
  public static final Object READ_RIGHT_PAREN = new MacroFuncs.ReadRightParen();
  public static final Object READ_QUOTE = new MacroFuncs.ReadQuote();

  public static final Object READ_DISPATCH_CHAR = new MacroFuncs.ReadDispatchChar();
  public static final Object BACKQUOTE_MACRO = null;
  public static final Object COMMA_MACRO = null;
  public static final Object SHARP_LEFT_PAREN = null;
  public static final Object SHARP_STAR = null;
  public static final Object SHARP_DOT = null;
  public static final Object SHARP_COLON = null;
  public static final Object SHARP_A = null;
  public static final Object SHARP_B = null;
  public static final Object SHARP_C = null;
  public static final Object SHARP_O = null;
  public static final Object SHARP_P = null;
  public static final Object SHARP_R = null;
  public static final Object SHARP_S = null;
  public static final Object SHARP_X = null;

  public static final Object SHARP_QUOTE = new MacroFuncs.ReadSharpQuote();
  public static final Object SHARP_QMARK = new MacroFuncs.ReadSharpQMark();

  public static final Object SHARP_BACKSLASH = new MacroFuncs.ReadSharpBackSlash();
  public static final Object SHARP_VERTICAL_BAR = null;
  public static final Object SHARP_ILLEGAL = null;

  /** Parse up to maxExprs from the input stream. */
  @Override
  public ASTNList parse(ParseCtx pctx, Reader r, int maxExprs) {
    final PushbackReader pbr = new PushbackReader(r);
    final List<ASTN> lst = new ArrayList<ASTN>();
    ASTN parsed;
    for (int i = 0; (i < maxExprs) && (null != (parsed = parse_expr(pctx, pbr))); i++) {
      lst.add(parsed);
    }
    final ASTNList result = new ASTNList(lst, pctx);
    return result;
  }

  /** Parse up to maxExprs from the input string. */
  public ASTNList parse(ParseCtx pctx, String str, int maxExprs) {
    final InputStream is = new ByteArrayInputStream(str.getBytes());
    final Reader r = new InputStreamReader(is);
    return parse(pctx, r, maxExprs);
  }

  // @Override
  public ASTNList parse(ParseCtx pctx, String input) {
    return parse(pctx, input, Integer.MAX_VALUE);
  }

  // FIXME
  public final ReadTable rt = new ReadTable();

  /** parse one expression from input stream. */
  protected ASTN parse_expr(ParseCtx pctx, PushbackReader reader) {
    while (true) {
      int n = -1;
      try {
        n = doReadChar(reader, pctx);
      } catch (IOException e) {
        return new ASTNLeaf(null, pctx, e);
      }
      if (n < 0) {
        // EOF before start of any expression is OK
        // throw new ReaderException(pctx, "Unexpected EOF");
        return null;
      }
      char c = (char) n;
      if (rt.isWhitespace(c)) {
        continue;
      }

      ASTN result = processChar(pctx, c, reader, rt);
      if (result != null && !result.isComment()) {
        return result;
      }
    }
  }

  /** Process input character. */
  static final ASTN processChar(ParseCtx pctx, char c, PushbackReader r, ReadTable rt) {

    final IReaderMacroFunc handler = rt.getReaderMacroFunction(c);
    if (null != handler) {
      return handler.execute(c, r, rt, pctx);
    }
    // LispObject value;

    // if (handler instanceof ReaderMacroFunction) {
    //     thread._values = null;
    //     value = ((ReaderMacroFunction)handler).execute(this, c);
    // }
    // else if (handler != null && handler != NIL) {
    //     thread._values = null;
    //     value = handler.execute(this, LispCharacter.getInstance(c));
    // }
    // else
    return readToken(pctx, c, r, rt);

    // If we're looking at zero return values, set 'value' to null
    // if (value == NIL) {
    //     LispObject[] values = thread._values;
    //     if (values != null && values.length == 0) {
    //         value = null;
    //         thread._values = null; // reset 'no values' indicator
    //     }
    // }
    // return value;
  }

  private static final ASTN readToken(ParseCtx pctx, char c, PushbackReader r, ReadTable rt) {
    StringBuilder sb = new StringBuilder(String.valueOf(c));
    // final LispThread thread = LispThread.currentThread();
    BitSet flags = null;
    try {
      flags = doReadToken(pctx, r, sb, rt);
    } catch (ParserException ex) {
      return new ASTNLeaf(null, pctx, ex);
    }
    return parseToken(sb.toString(), flags, pctx, null == flags ? tokenParsers : escTokenParsers);
  }

  private static ASTN parseToken(String string, BitSet flags, ParseCtx pctx, AtomParser[] parsers) {
    final Object[] holder = new Object[1];
    for (AtomParser parser : parsers) {
      try {
        if (parser.parse(string, holder, pctx)) {
          return new ASTNLeaf(holder[0], pctx);
        }
      } catch (AtomParseException ex) {
        return new ASTNLeaf(string, pctx, ex);
      }
    }
    return new ASTNLeaf(
        string, pctx, new ParserException(String.format("Failed to parse atom '%s'", string)));
    //  throw new SexpParserException(pctx,
    //                    String.format("Failed to parse atom '%s'", string));
  }

  protected static AtomParser[] tokenParsers =
      new AtomParser[] {
        new NullParser(),
        new BooleanParser(),
        new NumberParser(),
        // new StringParser(),
        new KeywordParser(),
        new SymbolParser()
      };

  protected static AtomParser[] escTokenParsers =
      new AtomParser[] {
        // new StringParser(),
        new KeywordParser(), new SymbolParser()
      };

  private static BitSet doReadToken(ParseCtx pctx, PushbackReader r, StringBuilder sb, ReadTable rt)
      throws ParserException {
    BitSet flags = null;
    final Keyword readtableCase = rt.getReadtableCase();
    if (sb.length() > 0) {
      Utils.assertTrue(sb.length() == 1);
      char c = sb.charAt(0);
      byte syntaxType = rt.getSyntaxType(c);
      if (syntaxType == ReadTable.SYNTAX_TYPE_SINGLE_ESCAPE) {
        int n = -1;
        try {
          n = doReadChar(r, pctx);
        } catch (IOException e) {
          // error(new StreamError(this, e));
          throw new ParserException(pctx, e.getMessage(), e);
          // return flags;
        }
        if (n < 0) {
          // error(new EndOfFile(this));
          return null; // Not reached
        }

        sb.setCharAt(0, (char) n); // ### BUG: Codepoint conversion
        flags = new BitSet(1);
        flags.set(0);
      } else if (syntaxType == ReadTable.SYNTAX_TYPE_MULTIPLE_ESCAPE) {
        sb.setLength(0);
        sb.append(readMultipleEscape(pctx, r, rt));
        flags = new BitSet(sb.length());
        flags.set(0, sb.length());
      } else if (rt.isInvalid(c)) {
        rt.checkInvalid(c); // Signals a reader-error.
      } else if (readtableCase == Keyword.UPCASE) {
        sb.setCharAt(0, Character.toUpperCase(c));
      } else if (readtableCase == Keyword.DOWNCASE) {
        sb.setCharAt(0, Character.toLowerCase(c));
      }
    }
    try {
      while (true) {
        int n = doReadChar(r, pctx);
        if (n < 0) {
          break;
        }
        char c = (char) n; // ### BUG: Codepoint conversion
        if (rt.isWhitespace(c)) {
          doUnreadChar(r, pctx, n);
          break;
        }
        byte syntaxType = rt.getSyntaxType(c);
        if (syntaxType == ReadTable.SYNTAX_TYPE_TERMINATING_MACRO) {
          doUnreadChar(r, pctx, c);
          break;
        }
        rt.checkInvalid(c);
        if (syntaxType == ReadTable.SYNTAX_TYPE_SINGLE_ESCAPE) {
          n = doReadChar(r, pctx);
          if (n < 0) {
            break;
          }
          sb.append((char) n); // ### BUG: Codepoint conversion
          if (flags == null) {
            flags = new BitSet(sb.length());
          }
          flags.set(sb.length() - 1);
          continue;
        }
        if (syntaxType == ReadTable.SYNTAX_TYPE_MULTIPLE_ESCAPE) {
          int begin = sb.length();
          sb.append(readMultipleEscape(pctx, r, rt));
          int end = sb.length();
          if (flags == null) {
            flags = new BitSet(sb.length());
          }
          flags.set(begin, end);
          continue;
        }
        if (readtableCase == Keyword.UPCASE) {
          c = Character.toUpperCase(c);
        } else if (readtableCase == Keyword.DOWNCASE) {
          c = Character.toLowerCase(c);
        }
        sb.append(c);
      }
    } catch (IOException e) {
      throw new ParserException(pctx, e.getMessage(), e);
      // error(new StreamError(this, e));
      // return flags;
    }

    return flags;
  }

  protected static int doReadChar(PushbackReader reader, ParseCtx pctx) throws IOException {
    final int n = reader.read();
    if (n < 0) {
      return -1;
    }
    pctx.setOff(pctx.getOff() + 1);
    if (n == '\n') {
      pctx.setLine(pctx.getLine() + 1);
      pctx.setPos(0);
      return '\n';
    }
    pctx.setPos(pctx.getPos() + 1);
    return n;
  }

  protected static void doUnreadChar(PushbackReader reader, ParseCtx pctx, int n)
      throws IOException {
    // if (reader == null) {
    // streamNotCharacterInputStream();
    // }
    // FIXME! (how to return to end of prev line?
    pctx.setOff(pctx.getOff() - 1);
    pctx.setPos(pctx.getPos() - 1);
    if (n == '\n') {
      // n = eolChar;
      pctx.setLine(pctx.getLine() - 1);
    }

    reader.unread(n);
    // pastEnd = false;
  }

  private static String readMultipleEscape(ParseCtx pctx, PushbackReader r, ReadTable rt)
      throws ParserException {
    StringBuilder sb = new StringBuilder();
    try {
      while (true) {
        int n = doReadChar(r, pctx);
        if (n < 0) {
          // return serror(new EndOfFile(this));
          throw new ParserEOFException("Unexpected EOF");
        }

        char c = (char) n; // ### BUG: Codepoint conversion
        byte syntaxType = rt.getSyntaxType(c);
        if (syntaxType == ReadTable.SYNTAX_TYPE_SINGLE_ESCAPE) {
          n = doReadChar(r, pctx);
          if (n < 0) {
            throw new ParserEOFException("Unexpected EOF");
            // return serror(new EndOfFile(this));
          }
          sb.append((char) n); // ### BUG: Codepoint conversion
          continue;
        }
        if (syntaxType == ReadTable.SYNTAX_TYPE_MULTIPLE_ESCAPE) {
          break;
        }
        sb.append(c);
      }
    } catch (IOException e) {
      // return serror(new StreamError(this, e));
      throw new ParserException("IO Error: " + e.getMessage());
    }
    return sb.toString();
  }

  @Override
  public boolean supportREPLStream() {
    return true;
  }

  @Override
  public String formatArgSpec(ArgSpec spec) {
    StringBuilder buf = new StringBuilder(64);
    if (null != spec) {
      for (String str : spec.asSpecList()) {
        buf.append(" ").append(str);
      }
    } else {
      buf.append(ArgSpec.ARG_REST).append(" args");
    }
    return buf.toString();
  }
}
