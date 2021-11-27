package org.dudinea.explang.reader;

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

import org.dudinea.explang.Symbol;
import org.dudinea.explang.Utils;
import org.dudinea.explang.Keyword;
//import org.dudinea.explang.SexpParser;
import org.dudinea.explang.ParseCtx;
import org.dudinea.explang.ASTN;
import org.dudinea.explang.ASTNList;
import org.dudinea.explang.Utils;
import org.dudinea.explang.ParserEOFException;
import org.dudinea.explang.ParserException;

@RunWith(Parameterized.class)
public class ReaderTest {
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
		{ "1",new Integer(1), null, false},
		{ "333", new Integer(333), null, false},
		{ "  333  ", new Integer(333), null, false},
		{ "foo", new Symbol("foo"), null, false},
		// FIXME: numbers starting with +
		//{ "+1", new Integer("1"), null, false},
		{ "1 2", new Integer(1), null, false},
		{ "()", Utils.list(), null, false},
		{ "|abc|", new Symbol("abc"), null, false},
		{ "\\a\\b\\c", new Symbol("abc"), null, false},
				{ "3(",new Integer(3), null, false},
				{ "\"foo\"", "foo", null, false},
		
		{ "-1", new Integer(-1), null, false},
		{ "1.1", new Double(1.1), null, false},
		{ "1.0", new Double(1.0), null, false},
		{ "1.0f", new Float(1.0), null, false},
		{ "1.0F", new Float(1.0), null, false},
		// FIXME: numbers starting with .
		//{ ".99",  new Float(0.99), null, false},
		{ "1.0d", new Double(1.0), null, false},
		{ "1.0D", new Double(1.0), null, false},
		{ "-1.0D", new Double(-1.0), null, false},

		{ "1L", new Long(1), null, false},
		{ "1i", new Integer(1), null, false},
		{ "1I", new Integer(1), null, false},
		{ "1s", new Short((short)1), null, false},
		{ "1S", new Short((short)1), null, false},
		{ "1b", new Byte((byte)1), null, false},
		{ "1B", new Byte((byte)1), null, false},
		
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
        System.out.println("\n\n TEST #: "+(testNum++));
        LispReader parser = new LispReader();
        try {
            System.out.println("\n\nIN:  "+in);
	    ParseCtx pctx = new ParseCtx("test");
	    ASTN ast = parser.parse(pctx, in, 1);
            System.out.println("AST: " + ast);
            Object output = ((List)stripAST(ast)).get(0);
            System.out.println("OUT: " + output);	    
            System.out.println("ExP: " + expOut);
            System.out.println("OER: " + ast.hasProblems());	    
            System.out.println("EER: " + hasErrors);
	    
            //System.out.println("STR: " + parser.sexpToString(output));
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
                System.err.println("Got expected exception: " + ex);
            } else {
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
	    List la = (List) a;
	    List lb = (List) b;
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
