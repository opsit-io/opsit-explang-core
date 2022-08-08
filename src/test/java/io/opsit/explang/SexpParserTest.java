package io.opsit.explang;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import io.opsit.explang.parser.sexp.SexpParser;

@RunWith(Parameterized.class)
public class SexpParserTest extends AbstractTest {
  static int testNum = 0;
    
  public static List<Object> list(Object ... objs) {
    return Arrays.asList(objs);
  }

  public static Symbol sym(String str) {
    return new Symbol(str);
  }
  public static Keyword keyword(String str) {
    return new Keyword(str);
  }

  Object expOut;
  String in;
  String expExc;
        
  public SexpParserTest(String in, Object expOut, String expExc) {
    this.in = in;
    this.expOut = expOut;
    this.expExc = expExc;
  }
        
  @Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {
        { "()", list(), null},
        { "foo", sym("foo"), null},
        { "1", Integer.valueOf(1), null},
        { "111", Integer.valueOf(111), null},
        { "+1", Integer.valueOf(1), null},
        { "-1", Integer.valueOf(-1), null},
        { "1.1", Double.valueOf(1.1), null},
        { "1.0", Double.valueOf(1.0), null},
        { "1.0f", Float.valueOf((float)1.0), null},
        { "1.0F", Float.valueOf((float)1.0), null},
        { "1.0d", Double.valueOf(1.0), null},
        { "1.0D", Double.valueOf(1.0), null},
        { "-1.0D", Double.valueOf(-1.0), null},
        { "1L", Long.valueOf(1), null},
        { "1i", Integer.valueOf(1), null},
        { "1I", Integer.valueOf(1), null},
        { "1s", Short.valueOf((short)1), null},
        { "1S", Short.valueOf((short)1), null},
        { "1b", Byte.valueOf((byte)1), null},
        { "1B", Byte.valueOf((byte)1), null},
        { "null", null, null},
        { "Null", null, null},
        { "NULL", null, null},
        { "NIL", null, null},
        { "TRUE", Boolean.TRUE, null},
        { "true", Boolean.TRUE, null},
        { "True", Boolean.TRUE,  null},                                 
        { "False", Boolean.FALSE, null},
        { "false", Boolean.FALSE, null},
        { "FALSE", Boolean.FALSE,  null},                               
        { "\"foo\"", "foo", null},
                 
        { "(foo)", list(sym("foo")), null},
        { "(foo bar (baz))",
          list(sym("foo"),sym("bar"),list(sym("baz"))), null},
        { "( \"(foo bar (baz)\" is a string)",
          list("(foo bar (baz)",sym("is"),sym("a"),sym("string")), null},

        { "(:a)", list(keyword(":a")), null},
        { "(:)", list(sym(":")), null},

        { "((()))", list(list(list())), null},
                 
        { "(\"a\" a)", list("a",sym("a")), null},
        //{ "aaaa bbbb", null, "Too many expressions"},
        { "aaaa bbbb", sym("aaaa"), null}
        //                 { "(aaa \"bbb)", null, "unclosed '\"'"}
      });
  }

  @Test
  public void testExprParse() {
    clearLog();
    log("\n\n TEST #: "+(testNum++));
    SexpParser parser = new SexpParser();
    try {
      log("\n\nIN:  "+in);
      ParseCtx pctx = new ParseCtx("test");
      Object output = stripAST(parser.parse(pctx, in, 1));
        
      log("OUT: " + output);
      log("ExP: " + expOut);
      log("STR: " + parser.sexpToString(output));
      Assert.assertTrue(output instanceof List);
      @SuppressWarnings("unchecked")
        List <Object>outList = (List<Object>) output;
      Assert.assertEquals(1, outList.size());
      Object outExpr = outList.get(0);
      if (null != expExc) {
        Assert.fail("expected exception containing " + expExc);
      } else {
        Assert.assertEquals(expOut, outExpr);
      }

    } catch (Exception ex) {
      if (null != expExc && ex.getMessage().contains(expExc)) {
        System.err.println("Got expected exception: " + ex);
      } else {
        flushLog();
        Assert.fail("expected exception containing: " + expExc + " but got: " + ex);
      }
    }
  }

  private Object stripAST(ASTN obj) {
    if (obj.isList()) {
      List <Object> newLst = new ArrayList<Object>();
      for (ASTN el : ((ASTNList)obj)) {
        newLst.add(stripAST(el));
      }
      return newLst;
    } else  {
      return obj.getObject();
    } 
    //  throw new RuntimeException("Unexpected onject in AST" + obj);
  }
}

