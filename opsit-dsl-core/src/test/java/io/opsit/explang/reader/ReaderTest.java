package io.opsit.explang.reader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import io.opsit.explang.ASTN;
import io.opsit.explang.ASTNList;
import io.opsit.explang.AbstractTest;
import io.opsit.explang.Keyword;
import io.opsit.explang.ParseCtx;
import io.opsit.explang.ParserEOFException;
import io.opsit.explang.ParserException;
import io.opsit.explang.Symbol;
import io.opsit.explang.Utils;

@RunWith(Parameterized.class)
public class ReaderTest extends AbstractTest {
  static int testNum = 0;
    

  public static Symbol sym(String str) {
    return new Symbol(str);
  }
  public static Keyword keyword(String str) {
    return new Keyword(str);
  }

  Object expOut;
  String in;
  String expExc;
  boolean hasErrors;
        
  public ReaderTest(String in, Object expOut, String expExc, boolean hasErrors) {
    this.in = in;
    this.expOut = expOut;
    this.expExc = expExc;
    this.hasErrors = hasErrors;
  }
        
  @Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {
        { "1",Integer.valueOf(1), null, false},
        { "333", Integer.valueOf(333), null, false},
        { "  333  ", Integer.valueOf(333), null, false},
        { "foo", new Symbol("foo"), null, false},
        // FIXME: numbers starting with +
        //{ "+1", Integer.valueOf("1"), null, false},
        { "1 2", Integer.valueOf(1), null, false},
        { "()", Utils.list(), null, false},
        { "|abc|", new Symbol("abc"), null, false},
        { "\\a\\b\\c", new Symbol("abc"), null, false},
        { "3(",Integer.valueOf(3), null, false},
        { "\"foo\"", "foo", null, false},
		
        { "-1", Integer.valueOf(-1), null, false},
        { "1.1", Double.valueOf(1.1), null, false},
        { "1.0", Double.valueOf(1.0), null, false},
        { "1.0f", Float.valueOf(1.0f), null, false},
        { "1.0F", Float.valueOf(1.0f), null, false},
        // FIXME: numbers starting with .
        //{ ".99",  Float.valueOf(0.99), null, false},
        { "1.0d", Double.valueOf(1.0), null, false},
        { "1.0D", Double.valueOf(1.0), null, false},
        { "-1.0D", Double.valueOf(-1.0), null, false},

        { "1L", Long.valueOf(1), null, false},
        { "1i", Integer.valueOf(1), null, false},
        { "1I", Integer.valueOf(1), null, false},
        { "1s", Short.valueOf((short)1), null, false},
        { "1S", Short.valueOf((short)1), null, false},
        { "1b", Byte.valueOf((byte)1), null, false},
        { "1B", Byte.valueOf((byte)1), null, false},
		
        { "null", null, null, false},
        { "Null", null, null, false},
        { "NULL", null, null, false},
        { "NIL", null, null, false},
		
        { "TRUE", Boolean.TRUE, null, false},
        { "true", Boolean.TRUE, null, false},
        { "True", Boolean.TRUE,  null, false},                                 
        { "False", Boolean.FALSE, null, false},
        { "false", Boolean.FALSE, null, false},
        { "FALSE", Boolean.FALSE,  null, false},
        { ":foo",  new Keyword(":foo"), null, false},
        { ":", new Symbol(":"), null, false},
        { "\\0123", new Symbol("0123"), null, false},
        { "|0123|", new Symbol("0123"), null, false},
        { "01\\23", new Symbol("0123"), null, false},		
        { "(foo)", Utils.list(sym("foo")), null, false},
		
        { "(foo bar (baz))",
          Utils.list(sym("foo"),sym("bar"),Utils.list(sym("baz"))), null, false},
        { "( \"(foo bar (baz)\" is a string)",
          Utils.list("(foo bar (baz)",sym("is"),sym("a"),sym("string")), null, false},

        { "(:a)", Utils.list(keyword(":a")), null, false},
        { "(:)", Utils.list(sym(":")), null, false},
		
        { "((()))", Utils.list(Utils.list(Utils.list())), null, false},
                 
        { "(\"a\" a)", Utils.list("a",sym("a")), null, false},
        { "#\"^ABC$\"", Pattern.compile("^ABC$"),null, false},
        { "#\"d\\\"s\"", Pattern.compile("d\\\"s"),null, false},		
        //**** ERRORS 
        { ")", new ParserException(new ParseCtx("test",0,0,0,0),"Too many right parentheses"),null, true},

		
        /*
        //{ "aaaa bbbb", null, "Too many expressions", false},
        { "aaaa bbbb", sym("aaaa"), null, false},*/
        { "(aaa \"bbb)", Utils.list(sym("aaa"),
                                    new ParserEOFException(new ParseCtx("test",0,9,9,0),"unclosed '\"'"),
                                    new ParserEOFException("Unexpected EOF")), null, true}
		
		
      });
  }

  @Test
  public void testExprParse() {
    log("\n\n TEST #: "+(testNum++));
    LispReader parser = new LispReader();
    try {
      log("\n\nIN:  "+in);
      ParseCtx pctx = new ParseCtx("test");
      ASTN ast = parser.parse(pctx, in, 1);
      log("AST: " + ast);
      @SuppressWarnings("unchecked")
        Object output = ((List<Object>)stripAST(ast)).get(0);
      log("OUT: " + output);	    
      log("ExP: " + expOut);
      log("OER: " + ast.hasProblems());	    
      log("EER: " + hasErrors);
	    
      //log("STR: " + parser.sexpToString(output));
      //Assert.assertTrue(output instanceof List);
      //List <Object>outList = (List<Object>) output;
      //Assert.assertEquals(1, outList.size());
      Object outExpr = output;
      Assert.assertEquals(hasErrors, ast.hasProblems());
	    
      if (null != expExc) {
        Assert.fail("expected exception containing " + expExc);
      } else {
        Assert.assertTrue(treeEqual(expOut,outExpr));
        // if ((expOut instanceof Pattern) &&
        //     (outExpr instanceof Pattern)) {
        //     // FIXME: string repr. works, but how to do it properly?
        //     Assert.assertEquals(expOut.toString(),outExpr.toString());
        // } else {
        //     Assert.assertEquals(expOut, outExpr);
        // }
      }
    } catch (Exception ex) {
      if (null != expExc && ex.getMessage().contains(expExc)) {
        log("Got expected exception: " + ex);
      } else {
        flushLog();
        Assert.fail("expected exception containing: " + expExc + " but got: " + ex);
      }
    }
  }


  private boolean treeEqual(Object a, Object b) {
    // we need custom comparison of ReaderExceptions and Patterns
    if (null == a ) {
      return null == b;
    } else if (null == b) {
      return null == a;
    } else if (a.getClass() != b.getClass()) {
      return false;
    } else if (a instanceof List) {
      @SuppressWarnings("unchecked")
        List<Object> la = (List<Object>) a;
      @SuppressWarnings("unchecked")
        List<Object> lb = (List<Object>) b;
      if (la.size() != lb.size()) {
        return false;
      }
      for (int i = 0; i < la.size(); i++) {
        if (! treeEqual(la.get(i), lb.get(i))) {
          return false;
        }
      }
      return true;
    } else if (a instanceof ParserException) {
      ParserException ea = (ParserException)a;
      ParserException eb = (ParserException)b;
      return ea.getMessage().equals(eb.getMessage());
    } else if ((a instanceof Pattern) &&
               (b instanceof Pattern)) {
      // FIXME: string repr. works, but how to do it properly?
      return a.toString().equals(b.toString());
    } else {
      return a.equals(b);
    }
  }
    
    
  private Object stripAST(ASTN obj) {
    if (obj.getProblem() != null) {
      return  obj.getProblem();
    } else {
      if (obj.isList()) {
        List <Object> newLst = new ArrayList<Object>();
        for (ASTN el : ((ASTNList)obj)) {
          newLst.add(stripAST(el));
        }
        return newLst;
      } else  {
        return obj.getObject();
      }
    }
  }
}
