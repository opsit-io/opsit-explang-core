package io.opsit.explang.parser.lisp;

import static io.opsit.explang.parser.lisp.LispParser.doReadChar;
import static io.opsit.explang.parser.lisp.LispParser.doUnreadChar;
import static io.opsit.explang.parser.lisp.LispParser.processChar;

import io.opsit.explang.ASTN;
import io.opsit.explang.ASTNLeaf;
import io.opsit.explang.ASTNList;
import io.opsit.explang.GlobPattern;
import io.opsit.explang.ParseCtx;
import io.opsit.explang.ParserEOFException;
import io.opsit.explang.ParserException;
import io.opsit.explang.Symbol;
import io.opsit.explang.Utils;
import java.io.IOException;
import java.io.PushbackReader;
import java.util.regex.Pattern;

public class MacroFuncs {
  private static String UNEXPECTED_IO_EXCEPTION = "Unexpected I/O Exception";
  private static String UNEXPECTED_EOF_EXCEPTION = "Unexpected EOF";

  public static class ReadRightParen implements IReaderMacroFunc {

    public ASTN execute(char terminator, PushbackReader is, ReadTable rt, ParseCtx pctx) {
      return new ASTNLeaf(
          terminator, pctx, new ParserException(pctx, "Too many right parentheses"));
    }
  }

  public static class ReadList implements IReaderMacroFunc {
    /**
     * Read list ( ...  ).
     */
    public ASTN execute(char terminator, PushbackReader is, ReadTable rt, ParseCtx pctx) {
      ASTNList lst = new ASTNList(Utils.list(), pctx.clone());
      while (true) {
        char c;
        try {
          c = flushWhitespace(pctx, is, rt);
        } catch (ParserException ex) {
          lst.add(new ASTNLeaf(null, pctx, ex));
          break;
        }
        if (c == ')') {
          break;
        }
        ASTN node = processChar(pctx, c, is, rt);
        if (null == node) {
          // EOF
          lst.add(new ASTNLeaf(null, pctx, new ParserEOFException(pctx, UNEXPECTED_EOF_EXCEPTION)));
          break;
        }
        if (!node.isComment()) {
          lst.add(node);
        }
      }
      return lst;
    }
  }


  /**
   * Handler for read dispatch character.
   */
  public static class ReadDispatchChar implements IReaderMacroFunc {
    /**
     *  Read optional dispatch numeric argument and dispatch
     *  subcharacter and execute its function if one is defined.
     */
    public ASTN execute(char dispChar, PushbackReader is, ReadTable rt, ParseCtx pctx) {
      // read optional numeric parameter
      ASTN result = null;
      int numArg = -1;
      char c;
      try {
        while (true) {
          int n = doReadChar(is, pctx);
          if (n < 0) {
            return new ASTNLeaf(null, pctx, new ParserEOFException(pctx, UNEXPECTED_EOF_EXCEPTION));
          }
          c = (char) n;
          if (c < '0' || c > '9') {
            break;
          }
          if (numArg < 0) {
            numArg = 0;
          }
          numArg = numArg * 10 + c - '0';
        }
      } catch (IOException e) {
        return new ASTNLeaf(null, pctx, new ParserException(pctx, UNEXPECTED_IO_EXCEPTION, e));
      }
      IDispatchMacroFunc fun;
      try {
        fun = rt.getDispatchMacroCharacter(dispChar, c);
      } catch (ParserException ex) {
        return new ASTNLeaf(null, pctx, new ParserException(pctx, UNEXPECTED_IO_EXCEPTION, ex));
      }

      if (null != fun) {
        result = fun.execute(is, pctx, rt, c, numArg);
      } else {
        // FIXME: return error if there is no dispatch function defined
        result = new ASTNLeaf(null, pctx.clone());
      }
      return result;
    }
  }

  /**
   * Handler for quoted (') expressions.
   */
  public static class ReadQuote implements IReaderMacroFunc {
    /**
     * Read quoted expression.
     */
    public ASTN execute(char terminator, PushbackReader is, ReadTable rt, ParseCtx pctx) {
      ParseCtx startPCtx = pctx.clone();
      ASTN q = new ASTNLeaf(new Symbol("QUOTE"), pctx.clone());
      char c;
      ASTN node;
      try {
        c = flushWhitespace(pctx, is, rt);
        node = processChar(pctx.clone(), c, is, rt);
      } catch (ParserException ex) {
        node = new ASTNLeaf(null, pctx, ex);
      }
      return new ASTNList(Utils.list(q, node), startPCtx);
    }
  }

  /**
   * Read comment expression.
   */
  public static class ReadComment implements IReaderMacroFunc {
    @Override
    public ASTN execute(char c, PushbackReader is, ReadTable rt, ParseCtx pctx) {
      StringBuilder buf = new StringBuilder(c);
      try {
        while (true) {
          int n = doReadChar(is, pctx);
          if ((n < 0) || (n == '\n')) {
            return new ASTNLeaf(buf.toString(), pctx, true);
          }
          buf.append((char) n);
        }
      } catch (IOException ioex) {
        return new ASTNLeaf(
            buf.toString(), pctx, new ParserException(pctx, UNEXPECTED_IO_EXCEPTION, ioex));
      }
    }
  }

  /**
   * Handler for string literals.
   */
  public static class ReadString implements IReaderMacroFunc {
    /**
     * Read string literals.
     */
    public ASTN execute(char terminator, PushbackReader is, ReadTable rt, ParseCtx pctx) {
      StringBuilder sb = new StringBuilder();
      try {
        while (true) {
          int n = doReadChar(is, pctx);
          if (n < 0) {
            // return error(new EndOfFile(this));
            return new ASTNLeaf(sb.toString(), pctx, new ParserEOFException(pctx, "unclosed '\"'"));
          }
          char c = (char) n;
          if (rt.getSyntaxType(c) == ReadTable.SYNTAX_TYPE_SINGLE_ESCAPE) {
            //          // Single escape.
            n = doReadChar(is, pctx);
            if (n < 0) {
              return new ASTNLeaf(
                  sb.toString(), pctx, new ParserEOFException(pctx, "unclosed '\"'"));
            }
            // support JAVA escape characters
            switch (n) {
              case (int) 'b':
                c = '\b';
                break;
              case (int) 't':
                c = '\t';
                break;
              case (int) 'n':
                c = '\n';
                break;
              case (int) 'f':
                c = '\f';
                break;
              case (int) 'r':
                c = '\r';
                break;
              default:
                c = (char) n;
            }
            sb.append(c);
            continue;
          }
          if (c == terminator) {
            break;
          }
          sb.append(c);
        }
      } catch (java.io.IOException e) {
        return new ASTNLeaf(
            sb.toString(), pctx, new ParserException(pctx, UNEXPECTED_IO_EXCEPTION, e));
      }
      return new ASTNLeaf(sb.toString(), pctx);
    }
  }

  private static char flushWhitespace(ParseCtx pctx, PushbackReader is, ReadTable rt)
      throws ParserException {
    try {
      while (true) {
        int n = doReadChar(is, pctx);
        if (n < 0) {
          throw new ParserEOFException(UNEXPECTED_EOF_EXCEPTION);
        }

        char c = (char) n; // ### BUG: Codepoint conversion
        if (!rt.isWhitespace(c)) {
          return c;
        }
      }
    } catch (IOException e) {
      throw new ParserException(pctx, "I/O exception", e);
    }
  }

  /**
   * Handler for #\ sequence (character literal).
   */
  public static class ReadSharpBackSlash implements IDispatchMacroFunc {
    /**
     * Read Character literal.
     */
    @Override
    public ASTN execute(PushbackReader is, ParseCtx pctx, ReadTable rt, char quot, int numArg) {
      try {
        int n = doReadChar(is, pctx);
        if (n < 0) {
          return new ASTNLeaf(null, pctx, new ParserEOFException(UNEXPECTED_EOF_EXCEPTION));
        }
        char c = (char) n;
        StringBuilder sb = new StringBuilder(String.valueOf(c));
        while (true) {
          n = doReadChar(is, pctx);
          if (n < 0) {
            break;
          }
          c = (char) n;
          if (rt.isWhitespace(c)) {
            break;
          }
          if (rt.getSyntaxType(c) == ReadTable.SYNTAX_TYPE_TERMINATING_MACRO) {
            doUnreadChar(is, pctx, c);
            break;
          }
          sb.append(c);
        }
        // if (Symbol.READ_SUPPRESS.symbolValue(thread) != NIL)
        //    return NIL;
        if (sb.length() == 1) {
          return new ASTNLeaf(sb.charAt(0), pctx.clone());
        }
        final String token = sb.toString();
        try {
          final char chr = Characters.nameToChar(token);
          return new ASTNLeaf(chr, pctx.clone());
        } catch (ParserException ex) {
          return new ASTNLeaf(null, pctx, ex);
        }
      } catch (IOException e) {
        return new ASTNLeaf(null, pctx, new ParserException(pctx, UNEXPECTED_IO_EXCEPTION, e));
      }
    }
  }
  

  public static class ReadSharpQuote implements IDispatchMacroFunc {
    @Override
    public ASTN execute(PushbackReader is, ParseCtx pctx, ReadTable rt, char quot, int numArg) {
      final ParseCtx newPctx = pctx.clone();
      ASTN f = new ASTNLeaf(new Symbol("FUNCTION"), newPctx);
      char c;
      try {
        c = flushWhitespace(pctx, is, rt);
      } catch (ParserException ex) {
        return new ASTNLeaf(null, pctx, ex);
      }
      ASTN node = processChar(pctx.clone(), c, is, rt);
      return new ASTNList(Utils.list(f, node), newPctx);
    }
  }
  
  public static class ReadSharpQMark implements IDispatchMacroFunc {
    @Override
    public ASTN execute(PushbackReader is, ParseCtx pctx, ReadTable rt, char quot, int numArg) {
      try {
        int n;
        StringBuilder sb = new StringBuilder();
        boolean esc = false;
        while (true) {
          n = doReadChar(is, pctx);
          if (n < 0) {
            return new ASTNLeaf(null, pctx, new ParserEOFException(UNEXPECTED_EOF_EXCEPTION));
          }
          char c = (char) n;
          if (c == '\\' && !esc) {
            esc = true;
            continue;
          }
          if (esc) {
            sb.append('\\');
            esc = false;
          } else if (c == '"') {
            break;
          }
          sb.append(c);
        }

        int flags = 0;
        while (true) {
          n = doReadChar(is, pctx);
          char c = (char) n;
          int f = Utils.parseRegexpFlag(c);
          if (0 == f) {
            doUnreadChar(is, pctx, n);
            break;
          }
          flags |= f;
        }

        final Pattern p = (0 == flags)
            ? Pattern.compile(sb.toString())
            : Pattern.compile(sb.toString(), flags);
        return new ASTNLeaf(p, pctx.clone());
        // if (Symbol.READ_SUPPRESS.symbolValue(thread) != NIL)
        //    return NIL;
      } catch (IOException ioex) {
        return new ASTNLeaf(null, pctx, new ParserException(pctx, UNEXPECTED_EOF_EXCEPTION, ioex));
      }
      // ASTN strASTN = ((IReaderMacroFunc)LispReader.READ_STRING).execute('"',is,rt,pctx);
      // final Pattern p = Pattern.compile((String)strASTN.getObject());
      // return  new ASTN(p, pctx.clone());
    }
  }


  public static class ReadSharpG implements IDispatchMacroFunc {
    @Override
    public ASTN execute(PushbackReader is, ParseCtx pctx, ReadTable rt, char quot, int numArg) {
      try {
        int n;
        StringBuilder sb = new StringBuilder();
        boolean inq = false;
        boolean esc = false;
        while (true) {
          n = doReadChar(is, pctx);
          if (n < 0) {
            return new ASTNLeaf(null, pctx, new ParserEOFException(UNEXPECTED_EOF_EXCEPTION));
          }
          char c = (char) n;
          if (!inq) {
            if (c == '"') {
              inq = true;
              continue;
            } else {
              return new ASTNLeaf(null,
                                  pctx,
                                  new ParserException(pctx,
                                                      "Unexpected character at start definition"));
            }
          }
          if (c == '\\' && !esc) {
            esc = true;
            continue;
          }
          if (esc) {
            sb.append('\\');
            esc = false;
          } else if (c == '"') {
            break;
          }
          sb.append(c);
        }

        int flags = 0;
        while (true) {
          n = doReadChar(is, pctx);
          char c = (char) n;
          int f = Utils.parseRegexpFlag(c);
          if (0 == f) {
            doUnreadChar(is, pctx, n);
            break;
          }
          flags |= f;
        }

        final Pattern p = (0 == flags)
            ? GlobPattern.compile(sb.toString())
            : GlobPattern.compile(sb.toString(), flags);
        return new ASTNLeaf(p, pctx.clone());
        // if (Symbol.READ_SUPPRESS.symbolValue(thread) != NIL)
        //    return NIL;
      } catch (IOException ioex) {
        return new ASTNLeaf(null, pctx, new ParserException(pctx, UNEXPECTED_EOF_EXCEPTION, ioex));
      }
      // ASTN strASTN = ((IReaderMacroFunc)LispReader.READ_STRING).execute('"',is,rt,pctx);
      // final Pattern p = Pattern.compile((String)strASTN.getObject());
      // return  new ASTN(p, pctx.clone());
    }
  }
  // //This will read regexp with java like syntax, i.e. \ escapes any character
  // //and, therefore, double escapes needed to get a regexp escape
  // public static class ReadSharpQMark implements IDispatchMacroFunc {
  //        @Override
  //        public ASTN execute(PushbackReader is,
  //                ParseCtx pctx,
  //                ReadTable rt,
  //                char quot,
  //                int numArg) {
  //        ASTN strASTN = ((IReaderMacroFunc)LispReader.READ_STRING).execute('"',is,rt,pctx);
  //        final Pattern p = Pattern.compile((String)strASTN.getObject());
  //        return  new ASTN(p, pctx.clone());
  //        }
  // };
}
