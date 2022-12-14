package io.opsit.explang.parser.sexp;

import static io.opsit.explang.Utils.list;

import io.opsit.explang.ASTN;
import io.opsit.explang.ASTNLeaf;
import io.opsit.explang.ASTNList;
import io.opsit.explang.ArgSpec;
import io.opsit.explang.IParser;
import io.opsit.explang.OperatorDesc;
import io.opsit.explang.ParseCtx;
import io.opsit.explang.ParserException;
import io.opsit.explang.atom.AtomParseException;
import io.opsit.explang.atom.AtomParser;
import io.opsit.explang.atom.BooleanParser;
import io.opsit.explang.atom.EscStringParser;
import io.opsit.explang.atom.KeywordParser;
import io.opsit.explang.atom.NullParser;
import io.opsit.explang.atom.NumberParser;
import io.opsit.explang.atom.RegexpParser;
import io.opsit.explang.atom.SymbolParser;
import io.opsit.explang.atom.VersionParser;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;

// @SuppressWarnings({"serial"})
public class SexpParser implements IParser {
  protected OperatorDesc[] operatorDescs = new OperatorDesc[] {};

  @Override
  public ASTNList parse(ParseCtx pctx, String input) {
    return parse(pctx, input, Integer.MAX_VALUE);
  }

  public ASTNList parse(ParseCtx pctx, String input, int maxExprs) {
    final InputStream is = new ByteArrayInputStream(input.getBytes());
    return parse(pctx, is, maxExprs);
  }

  public ASTNList parse(ParseCtx pctx, InputStream is, int maxExprs) {
    final Reader reader = new InputStreamReader(is);
    return parse(pctx, reader, maxExprs);
  }

  @Override
  public ASTNList parse(ParseCtx pctx, Reader reader, int maxExprs) {
    final List<ASTN> sexp = list(new ASTNList(list(), pctx.clone()));
    boolean inStr = false;
    boolean inComment = false;
    int depth = 0;
    StringBuffer buf = new StringBuffer();
    int lineStart = 0;
    int code;
    ParseCtx spctx = null;
    Exception problem = null;
    try {
      for (int i = 0; true; i++) {
        if (0 == depth && ((ASTNList) sexp.get(0)).size() >= maxExprs) {
          break;
        }
        code = reader.read();
        if (code < 0) {
          break;
        }
        pctx.setPos(i - lineStart);
        pctx.setOff(i);
        char chr = (char) code;
        if (inComment) {
          if (chr == '\n') {
            inComment = false;
          }
        } else {
          if (chr == '"') {
            inStr = !inStr;
            if (buf.length() == 0) {
              spctx = pctx.clone();
            }
            buf.append(chr);
          } else if ((!inStr) && (chr == '(')) {
            if (buf.length() > 0) {
              addParsedAtom(sexp, buf, pctx, spctx);
            }
            depth++;
            sexp.add(new ASTNList(list(), pctx.clone()));
          } else if ((!inStr) && chr == ')') {
            depth--;
            if (depth < 0) {
              sexp.add(
                  new ASTNLeaf(
                      null, pctx, new ParserException(pctx, "Too many right parentheses")));
            }
            if (buf.length() > 0) {
              addParsedAtom(sexp, buf, pctx, spctx);
            }
            ASTN tmp = sexp.remove(sexp.size() - 1);
            ((ASTNList) sexp.get(sexp.size() - 1)).add(tmp);
          } else if ((!inStr) && " \n\r\t".indexOf(chr) > -1) {
            if (buf.length() > 0) {
              addParsedAtom(sexp, buf, pctx, spctx);
            }
          } else if ((!inStr) && ';' == chr) {
            inComment = true;
          } else {
            if (buf.length() == 0) {
              spctx = pctx.clone();
            }
            buf.append(chr);
          }
        }
        if ('\n' == chr) {
          pctx.setLine(pctx.getLine() + 1);
          lineStart = i + 1;
        }
      }
    } catch (IOException ex) {
      problem = new ParserException(pctx.clone(), "I/O exception", ex);
    }
    if (inStr) {
      problem = new ParserException(pctx.clone(), "unclosed '\"'");
    }
    if (depth > 0) {
      problem = new ParserException(pctx.clone(), "unbalanced '('");
    }
    if (buf.length() > 0) {
      addParsedAtom(sexp, buf, pctx, spctx);
    }
    ASTNList resultList = (ASTNList) sexp.get(0);
    if (null != problem) {
      resultList.problem = problem;
    }
    return resultList;
  }

  private void addParsedAtom(List<ASTN> sexp, StringBuffer buf, ParseCtx pctx, ParseCtx spctx) {
    ((ASTNList) sexp.get(sexp.size() - 1)).add(parseAtom(buf.toString(), spctx.clone().upto(pctx)));
    clearBuf(buf);
  }

  private ASTN parseAtom(String string, ParseCtx pctx) {
    final Object[] holder = new Object[1];
    for (AtomParser parser : atomParsers) {
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
  }

  protected AtomParser[] atomParsers =
      new AtomParser[] {
        new NullParser(),
        new BooleanParser(),
        new NumberParser(),
        new EscStringParser(),
        new RegexpParser(),
        new VersionParser(),
        new KeywordParser(),
        new SymbolParser()
      };

  protected String sexpToString(Object obj) {
    if (null == obj) {
      return "()";
    } else if (obj instanceof List) {
      StringBuffer str = new StringBuffer();
      str.append('(');
      for (Object member : ((List<?>) obj)) {
        if (str.length() > 1) {
          str.append(" ");
        }
        str.append(sexpToString(member));
      }
      str.append(')');
      return str.toString();
    } else if (obj instanceof String) {
      return String.format("\"%s\"", obj);
    } else {
      return obj.toString();
    }
  }

  protected void clearBuf(StringBuffer buf) {
    buf.delete(0, buf.length());
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

  @Override
  public OperatorDesc[] getOperatorDescs() {
    return operatorDescs;
  }
}
