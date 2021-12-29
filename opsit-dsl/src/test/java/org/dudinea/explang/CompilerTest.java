package org.dudinea.explang;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.dudinea.explang.Compiler.Eargs;
import org.dudinea.explang.reader.LispReader;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import static org.dudinea.explang.Utils.list;
import static org.dudinea.explang.Utils.map;

@RunWith(Parameterized.class)
public class CompilerTest {
    static int testNum = 0;
    
    Object result;
    boolean logResult;
    IParser parser;
    String in;
    Throwable expExc;
    Map <String,Object> vars;        

    public CompilerTest(String in,
                        Object result,
                        boolean logResult,
                        Throwable expExc,
                        Map<String,Object> vars,
                        IParser parser) {
        this.in = in;
        this.result = result;
        this.expExc = expExc;
        this.logResult = logResult;
        this.vars = vars;
        this.parser = parser;
    }

    @Parameters
    public static Collection<Object[]> data() throws Exception {
        Collection<Object[]> c = new ArrayList<Object[]>();
        IParser p;
        p = new SexpParser();
        c.addAll(data(p));
        p = new LispReader();
        c.addAll(data(p));
        return c;
    }
    
    @SuppressWarnings("serial")
    public static Collection<Object[]> data(IParser p) throws Exception {
        TestBean testBean = new TestBean();
        Map<String,Object> beanVars = new HashMap<String,Object>();
        beanVars.put("testbean", testBean);
        String quine = "((LAMBDA (X) (LIST X (LIST (QUOTE QUOTE) X))) (QUOTE (LAMBDA (X) (LIST X (LIST (QUOTE QUOTE) X)))))";
        List<Object> parsedQuine =(List<Object>)((List<Object>)Utils.unASTN((new SexpParser()).parse(new ParseCtx("quine"),quine))).get(0);
        List data ;

	
        return Arrays.asList(new Object[][] {
                //empty list
                { "()",         new ArrayList<Object>(0)  , false, null, null, p},
                //atomic values
                { "1",         1 , true, null, null, p},
                { "\"foo\"",   "foo"  , true, null, null, p},
                { "\"\"",   ""  , false, null, null, p},
                { "TRUE" , Boolean.TRUE, true, null, null, p},
                { "FALSE" , Boolean.FALSE, false, null, null, p},
                { "NULL" , null,  false, null, null, p},
                
                //arithmetics
                { "(+)",                 0  , false, null, null, p},
                { "(+ 1b 2b)",           (byte)3, true, null, null, p},
                { "(+ 1s 2s)",           (short)3, true, null, null, p},
                { "(+ 1s 2)",            3, true, null, null, p},
                { "(+ 1 2 3 4)",         10  , true, null, null, p},
                { "(+ 1 (+ 2 3)  4)",    10  , true, null, null, p},
                { "(/ 2)",               0   , false, null, null, p},
                { "(/ 2b)",              (byte)0   , false, null, null, p},
                { "(/ 2.0)",             0.5   , true, null, null, p},
                { "(/ 20 10.0)",         2.0   , true, null, null, p},
                { "(/ 20 10.0f)",        (float)2.0   , true, null, null, p},         
                { "(/ 1024 2 4 )",       128 , true, null, null, p},
                { "(*)",                 1   , true, null, null, p},
                { "(* 8)",               8   , true, null, null, p},
                { "(* 3b 2b)",           (byte)6, true, null, null, p},
                { "(* 3s 2s)",           (short)6, true, null, null, p},
                { "(* 3s 2)",            6, true, null, null, p},
                { "(* 1 2 3 (* 2 2) 5)", 120 , true, null, null, p},
                { "(- 1b)",              (byte)-1  , true, null, null, p},
                { "(- 1s)",              (short)-1  , true, null, null, p},
                { "(- 1)",               -1  , true, null, null, p},
                { "(- 1L)",              -1L  , true, null, null, p},         
                { "(- 10 1 2 3 )",       4   , true, null, null, p},
                { "(- 10 1 2 3L )",      4L   , true, null, null, p},
                { "(% 100 10)",          0   , false,null, null, p},
                { "(% 10 3)",         1   , true, null, null, p},
                { "(REM -1 5)",      -1   , true, null, null, p},
                { "(MOD -1 5)",      4   , true, null, null, p},
                { "(MOD 13 4)",      1   , true, null, null, p},
                { "(REM 13 4)",      1   , true, null, null, p},
                { "(MOD -13 4)",      3   , true, null, null, p},
                { "(REM -13 4)",     -1   , true, null, null, p},
                { "(MOD 13 -4)",     -3   , true, null, null, p},
                { "(REM 13 -4)",     1   , true, null, null, p},
                { "(MOD -13 -4)",      -1   , true, null, null, p},
                { "(REM -13 -4)",      -1   , true, null, null, p},
                { "(REM -1 5)",      -1   , true, null, null, p},
                { "(MOD -1 5)",      4   , true, null, null, p},
                { "(MOD 13.4 1)",      0.4   , true, null, null, p},
                { "(REM 13.4 1)",      0.4   , true, null, null, p},
                { "(MOD  -13.4 1)",      0.6   , true, null, null, p},
                { "(REM  -13.4 1)",     -0.4   , true, null, null, p},
                { "(MOD  -13.4 1D)",      0.6   , true, null, null, p},
                { "(REM  -13.4 1D)",     -0.4   , true, null, null, p},
                { "(MOD  -13.4F 1F)",     (float) 0.6   , true, null, null, p},
                { "(REM  -13.4F 1F)",     (float)-0.4   , true, null, null, p},
                { "(MOD  -13.4F 1)",     (float) 0.6   , true, null, null, p},
                { "(REM  -13.4F 1)",     (float)-0.4   , true, null, null, p},
                { "(MOD  -13.4F 1.1F)",     (float) 0.9   , true, null, null, p},
                { "(REM  -13.4F 1.1F)",     (float)-0.2   , true, null, null, p},
                { "(MOD  -13.4 1.1)",      0.9   , true, null, null, p},
                { "(REM  -13.4 1.1)",     -0.2   , true, null, null, p},						
                // promotion
                { "(+ 1 2)",            new Integer(3) , true, null, null, p},
                { "(+ 1 2.0)",           new Double(3.0) , true, null, null, p},
                { "(+ 1.0 2.0)",         new Double(3.0) , true, null, null, p},
                { "(+ 1.0 2)",           new Double(3.0) , true, null, null, p},

                { "(- 1 2.0)",           new Double(-1.0) , true, null, null, p},
                { "(- 1.0 2.0)",         new Double(-1.0) , true, null, null, p},
                { "(- 1.0 2)",           new Double(-1.0) , true, null, null, p},

                { "(+ 1 2.0f)",          new Float(3.0) , true, null, null, p},
                { "(+ 1.0f 2.0 0)",      new Double(3.0) , true, null, null, p},
                
                { "(OR 0 3L 0)",           new Long(3)   , true, null, null, p},
                { "(OR 0 \"\" ())",       false            , false, null, null, p},
                { "(OR)",                 false            ,  false, null, null, p},
                { "(LET ((a NIL)) (LIST (OR (SETV a 0) (SETV a 2) (SETV a 3) ) a))",
                  list(2,2), true, null, null, p},
                { "(AND)",                 true             , true, null, null, p},
                { "(AND 1 2 3L)",           new Long(3)   , true, null, null, p},
                { "(AND 1 2 0L)",           new Long(0)   , false,null, null, p},
                { "(LET ((a NIL)) (LIST (AND (SETV a 1) (SETV a 0) (SETV a 3) ) a))",
                  list (0,0), true, null, null, p},
                { "(NOT 0 )",              true             , true, null, null, p},
                { "(NOT 0b )",             true             , true, null, null, p},
                { "(NOT 0s )",             true             , true, null, null, p},
                { "(NOT 0.0f )",           true             , true, null, null, p},
                { "(NOT 0.0 )",            true             , true, null, null, p},
                { "(NOT 0.0001 )",         false            , false, null, null, p},
                { "(NOT -0.0001 )",        false            , false, null, null, p},
                { "(NOT -0.0001f )",       false            , false, null, null, p},
                { "(NOT ())",              true             , true, null, null, p},
                { "(NOT 1)",               false            , false,null, null, p},
                { "(NOT 1)",               false            , false,null, null, p},

                { "(= 1)", true , true, null, null, p},
                { "(= 1 1 1)", true, true, null, null, p},
                { "(= 0  \"bbb\")", true, true, null, null, p},
                { "(= 0  \"bbb\")", true, true, null, null, p},               
                { "(< 0 1 2)", true, true, null, null, p},
                { "(< 0 1 2 2)", false, false, null, null, p},                
                { "(<= 0 1 2)", true, true, null, null, p},
                { "(<= 0 1 2 2 3)", true, true, null, null, p},
                { "(> 2 1 0)", true, true, null, null, p},
                { "(> 4  2 2 -1)", false, false, null, null, p},              
                { "(>= 2 1 0 0)", true, true, null, null, p},
                { "(= 0.0 0 0b 0.0f 0.s 0L)", true, true, null, null, p},
                { "(= 0 0.1)", false, false, null, null, p},          
                { "(> 0.1 0.0)", true, true, null, null, p},
                { "(> 0.1 0)", true, true, null, null, p},
                // this is not equal in Java
                //{ "(= 0.1 0.1F)", true, true, null, null, p},
                { "(= 3.0 3.0F 3L 3 3S 3B)", true, true, null, null, p},

                // java equal
                { "(EQUAL 1 1)", true, true, null, null, p},
                { "(EQUAL \"a\" \"a\" )", true, true, null, null, p},
                { "(EQUAL NIL NULL )", true, true, null, null, p},
                { "(EQUAL (SYMBOL \"A\") (QUOTE A))", true, true, null, null, p},
                // LOADR
                { "(PROGN (SETV *loaded* NIL) (LIST (LOAD \"./src/test/resources/org/dudinea/explang/resloadtest.lsp\") *loaded*))",
                  list(true, "some-result"), true, null, null, p},
                { "(PROGN (SETV *loaded* NIL) (LIST (LOADR \"/org/dudinea/explang/resloadtest.lsp\") *loaded*))",
                  list(true, "some-result"), true, null, null, p},
                // EVAL
                { "(EVAL (QUOTE (+ 1 2 3 )))",  6  , true, null, null, p},
                { "(FUNCTIONS-NAMES \"LOAD\")",  list("LOAD","LOADR")  , true, null, null, p},
                { "(FUNCTIONS-NAMES (RE-PATTERN \"^LOADR?$\"))",  list("LOAD","LOADR")  , true, null, null, p},
                // // degenerate LET cases:
                { "(LET ((v1)) v1)",  null  , false, null, null, p},
                { "(LET ((v1 1) (v2 (+ 1 2)))  (+ 1 2))",  3  , true, null, null, p},
                
                { "(LET ((v1 1) (v2 (+ 1 2)))  4)",        4  , true, null, null, p},
                // just return var value
                { "(LET ((v1 1)) v1)",                     1  , true, null, null, p},
                { "(LET ((v1 1)) (+ v1 v1))",              2  , true, null, null, p},
                { "(LET ((v1 1)(v2 2)) (+ v1 v2))",        3  , true, null, null, p},
                // enclosed let
                { "(LET ((v1 1)(v2 2)) (LET ((v3 3 )) (+ v1 v2 v3)))",  6, true, null, null, p},
                { "(LET ((v1 1)(v2 2)) (LET ((v1 4 )) (+ v1 v2)))",     6, true, null, null, p},
                
                // // // with expression as var value
                { "(LET ((v1 (+ 1 2))) v1)" ,               3  , true, null, null, p},
                { "(LET ((v1 (+ 1 2))) (+ v1 v1))" ,        6  , true, null, null, p},
                { "(LET ((v 1)) (LET ((v NIL)) v))",   null, false, null, null, p},
                // var ref in var defs
                { "(LET ((v1 2) (v2 v1) (v3 (+ v1 v2))) (+ v1 v2 v3))" ,
                  8  , true, null, null, p},
                // BOUNDP
                {"(BOUNDP (QUOTE MU))", false, false,null, null, p},
                {"(BOUNDP \"NU\")", false, false, null, null , p},
                {"(LET ((FOO 1)) (BOUNDP \"FOO\" \"bar\"))", false, false, null,null, p},
                {"(LET ((FOO 1)) (BOUNDP \"FOO\"))", true, true, null,null, p},
                {"(LET ((FOO 1)(BAR null)) (BOUNDP \"FOO\" \"BAR\"))", true, true, null,null, p},
                {"(LET ((FOO 1)) (LET ((BAR 2)) (BOUNDP (QUOTE BAR) (QUOTE FOO))))",
                 true, true, null,null, p},
		
                //  DLET (destructuring-bind
                {"(DLET (a b c d) (LIST 1 2 3 4) (LIST d c b a))", list(4,3,2,1), true, null, null, p},
                {"(DLET (a ) (LIST 1 2 3 4) a)", 1, true, null, null, p},
                {"(DLET (a b c d) (LIST 1 2 3 4) )", null, false, null, null, p},
                // QUOTE
                {"(QUOTE 1)", 1, true, null, null, p},
                {"(QUOTE NIL)", null, false, null, null, p},
                {"(QUOTE ())", list(), false, null, null, p},
                {"(QUOTE (FOO \"bar\" 1.0 (1)))", list(new Symbol("FOO"), "bar", 1.0, list(1)), true, null, null, p},
                // COND form
                { "(LET ((a 3)(b 3)) (COND ((= a b) (+ a b)) ((> a 0) (* a b)))) ", 6, true, null , null, p},
                { "(COND (1) (2 3))" , 1, true, null , null, p},
                { "(COND)" , null, false, null , null, p},
                { "(COND  (nil 2) (3 4))" , 4, true, null , null, p},
                { "(COND  (1 2) (3 4))" , 2, true, null , null, p},
                { "(LET ((a 2))  (COND ((= (SETV a 1) 1)) ((SETV a 3) (SETV a 5)) ((SETV a 4))) a)" , 1, true, null , null, p},
                // IF form
                { "(IF  1 2 3)" , new Integer(2), true, null , null, p},
                { "(IF  True 2 3)" , new Integer(2), true, null , null, p},
                { "(IF  False 2 3)" , new Integer(3), true, null , null, p},
                { "(IF  0 2 3)" , new Integer(3), true, null , null, p},
                { "(IF  0 2 3 4 5 6)" , new Integer(6), true, null , null, p},
                { "(IF  1 2 )" , new Integer(2), true, null , null, p},
                { "(IF  0 2 )" , null, false, null , null, p},
                { "(IF  1 (IF 0 1 2 ))" , new Integer(2), true, null , null, p},
                
                { "(PROGN 0 1 2)" , new Integer(2), true, null , null, p},
                { "(PROGN (PRINT \"HELLO\") (PRINT \"WORLD\") (PRINT \"!!!\"))","!!!", true, null , null, p},
                { "(PROGN)",null, false, null , null, p},
                
                { "(TRY)", null, false, null, null, p},
                { "(TRY 1 2 3)", 3, true, null, null, p},
                { "(TRY (PRINT 1) (+ 1 2))", 3, true, null, null, p},
                { "(TRY (PRINT 1) (+ 1 2) (FINALLY 4) )", 3, true, null, null, p},
                { "(TRY (THROW \"Exception\") "+
                  "     (CATCH java.lang.Exception ex 3) "+
                  "     (FINALLY 4))",	  3, true, null, null, p},
                { "(TRY (THROW (.N \"Exception\" (LIST \"fooo\"))) "+
                  "     (CATCH java.lang.Exception ex (. ex \"getMessage()\"))"+
                  "     (FINALLY 4))",	  "fooo", true, null, null, p},
                { "(TRY (THROW (.N \"Exception\" (LIST \"fooo\"))) "+
                  "     (CATCH java.lang.RuntimeException ex \"oops\")"+  
                  "     (CATCH java.lang.Throwable ex (. ex \"getMessage()\"))"+
                  "     (CATCH java.lang.Exception ex \"nope\")"+
                  "     (FINALLY 4))",	  "fooo", true, null, null, p},
                { "(LET ((L (LIST))) "+
                  "  (TRY (. L \"add\" (LIST 1) (LIST \"Object\" )) "+
                  "     (FINALLY (. L \"add\" (LIST 2) (LIST \"Object\" )))) "+
                  "  L)",	  list(1,2), true, null, null, p},
                { "(LET ((L (LIST))) "+
                  " (TRY "+
                  "  (TRY (. L \"fooo()\" )"+
                  "     (FINALLY (. L \"add\" (LIST 2) (LIST \"Object\" )))) "+
                  "  (CATCH java.lang.RuntimeException ex ex))"+
                  "  L)",  list(2), true, null, null, p},
                // LOOPS
                {"(LET ((r ())) (LIST (FOREACH (a \"ABC\")  (SETV r (APPEND r (LIST 1 a)))) r))", list(null, list(1,'A',1,'B',1,'C')), true, null,null,p},
                {"(LET ((r ())) (FOREACH (a (LIST 2 3 4))  (SETV r (APPEND r (LIST 1 a)))) r)", list(1,2,1,3,1,4), true, null,null,p},
                {"(LET ((r ()) (ar (MAKE-ARRAY 3))) (ASET ar 0 2) (ASET ar 1 3) (ASET ar 2 4) (FOREACH (a ar)  (SETV r (APPEND r (LIST 1 a)))) r)", list(1,2,1,3,1,4), true, null,null,p},
                {"(LET ((r ()) (ar (MAKE-ARRAY 3))) (ASET ar 0 2) (ASET ar 1 3) (ASET ar 2 4) (FOREACH (a ar)  (SETV r (APPEND r (LIST 1 a)))))", null, false, null,null,p},
                {"(LET ((r ()) (ar (MAKE-ARRAY 3))) (ASET ar 0 2) (ASET ar 1 3) (ASET ar 2 4) (FOREACH (a ar r)  (SETV r (APPEND r (LIST 1 a)))))", list(1,2,1,3,1,4), true, null,null,p},
                // RANGE
                {"(APPEND ()  (RANGE 0 0))",list(), false, null,null,p},
                {"(APPEND ()  (RANGE 0 -2))",list(), false, null,null,p},
                {"(APPEND ()  (RANGE 1 1))",list(), false, null,null,p},
                {"(APPEND ()  (RANGE -1 -1))",list(), false, null,null,p},  
                {"(APPEND ()  (RANGE 0 1))",list(0), true, null,null,p},  
                {"(APPEND ()  (RANGE 0 5))",list(0,1,2,3,4), true, null,null,p},  
                {"(APPEND ()  (RANGE 0 5 1))",list(0,1,2,3,4), true, null,null,p},

                {"(APPEND ()  (RANGE 0b 5b 1b))",list((byte)0,(byte)1,(byte)2,(byte)3,(byte)4), true, null,null,p},
                {"(APPEND ()  (RANGE 0 10 2))",list(0,2,4,6,8), true, null,null,p},
                {"(APPEND ()  (RANGE 0.0 10 2))",list(0.0,2.0,4.0,6.0,8.0), true, null,null,p},
                {"(APPEND ()  (RANGE 0.0 -10 -2))",list(0.0,-2.0,-4.0,-6.0,-8.0), true, null,null,p},
                {"(APPEND () (RANGE 1 -4 -1))", list(1,0,-1,-2,-3), true, null,null, p},
                {"(LET ((R (RANGE 0 1 0.05)) (I (. R \"iterator\" ()))) (. I  \"next\" ()) (. I  \"next\" ()))", 0.05, true, null, null, p},
                {"(LET ((R (RANGE 0 2))) (APPEND () R R))", list(0,1,0,1), true, null,null,p},
                {"(LET ((R (RANGE 0 3))) (LIST (LENGTH R) (APPEND () R ) (LENGTH R)))", list(3,list(0,1,2),3), true, null,null,p},
                {"(LET ((R (RANGE 0.0 3.0))) (LIST (LENGTH R) (APPEND () R ) (LENGTH R)))", list(3,list(0.0,1.0,2.0),3), true, null,null,p},
                {"(LENGTH (RANGE 1 4 1))", 3, true, null,null, p},
                {"(LENGTH (RANGE 1 -4 -1))", 5, true, null,null, p},
                {"(LENGTH (RANGE 1 4 -1))", 0, false, null,null, p},
                {"(LENGTH (RANGE 0 0))", 0 , false, null,null,p},
                {"(LENGTH (RANGE 0 0 3))", 0 , false, null,null,p},
                {"(LENGTH (RANGE 2 2 3))", 0 , false, null,null,p},
                {"(LENGTH (RANGE 0 10))", 10 , true, null,null,p},
                {"(LENGTH (RANGE 2 17 3))", 5 , true, null,null,p},
                {"(LENGTH (RANGE 2 18 3))", 6 , true, null,null,p},
                {"(LENGTH (RANGE 0.0 0))", 0 , false, null,null,p},
                {"(LENGTH (RANGE 0.0 0 3))", 0 , false, null,null,p},
                {"(LENGTH (RANGE 2.0 2 3))", 0 , false, null,null,p},
                {"(LENGTH (RANGE 0.0 10))", 10 , true, null,null,p},
                {"(LENGTH (RANGE 2.0 17 3))", 5 , true, null,null,p},
                {"(LENGTH (RANGE 2.0 18 3))", 6 , true, null,null,p},			
                // // // // LAMBDA
                { "((LAMBDA ()))", null, false, null, null, p},
                { "((LAMBDA (a b)) 1 2)", null, false, null, null, p},
                { "((LAMBDA (A B C) (+ A A B B C C)) 1 2 3)" , 12, true, null , null, p},
                { "((LAMBDA (x y) (LIST x y)) 1 2)",
                  list(1,2),  true , null, null, p},
                { "((LAMBDA (x &OPTIONAL y) (LIST x y)) 1 2)",
                  list(1,2),  true , null, null, p},
                { "((LAMBDA (x &OPTIONAL y) (LIST x y)) 1)",
                  list(1,null),  true , null, null, p},
                { "((LAMBDA (&REQUIRED x &OPTIONAL y) (LIST x y)) 1)",
                  list(1,null),  true , null, null, p},
                { "((LAMBDA (&REQUIRED x &OPTIONAL y &REQUIRED z) (LIST x y z)) 1 2)",
                  list(1,null,2),  true , null, null, p},		
                { "((LAMBDA (&OPTIONAL x &REQUIRED y z) (LIST x y z)) 1 2)",
                  list(null,1,2),  true , null, null, p},		
                { "((LAMBDA (x &REST y) (LIST x y )) 1 2 3 4 5)",
                  list(1,list(2,3,4,5)),  true , null, null, p},		
                { "((LAMBDA (x &REST y) (LIST x y )) 1 )",
                  list(1,list()),  true , null, null, p},		
                { "((LAMBDA (x &REST y) (LIST x y )) 1 (+ 1 2) (+ 4 5))",
                  list(1,list(3,9)),  true , null, null, p},		
                { "((LAMBDA (x &OPTIONAL y &REST z) (LIST x y z)) 1 2 3 4)",
                  list(1,2,list(3,4)),  true , null, null, p},		
                { "((LAMBDA (x &OPTIONAL y &REST z) (LIST x y z)) 1 )",
                  list(1,null,list()),  true , null, null, p},		
                { "((LAMBDA (&REST x) x))",
                  list(),  false , null, null, p},		
                { "((LAMBDA (a &REST x &REQUIRED y z) (LIST a x y z)) 1 2 3 4 5)",
                  list(1,list(2,3),4,5),  true , null, null, p},
                // keyword args
                { "((LAMBDA (&KEY a) (LIST a)) :a 1)",
                  list(1),  true , null, null, p},
                { "((LAMBDA (&KEY a b c) (LIST a b c)) :a 1 :b 2 :c 3)",
                  list(1,2,3),  true , null, null, p},
                { "((LAMBDA (&KEY a b c) (LIST a b c)) :a 1 :b 2 )",
                  list(1,2,null),  true , null, null, p},
                { "((LAMBDA (&KEY a b c) (LIST a b c)) :c 3 )",
                  list(null,null,3),  true , null, null, p},

                { "((LAMBDA (&KEY (a)) (LIST a )) :a 3 )",
                  list(3),  true , null, null, p},

                { "((LAMBDA (&KEY (a (+ 5 2))) (LIST a )) :a 3 )",
                  list(3),  true , null, null, p},

                { "((LAMBDA (&KEY (a (+ 5 2))) (LIST a )))",
                  list(7),  true , null, null, p},

                { "((LAMBDA (&KEY (a (+ 5 2) sa)) (LIST a sa)) :a 3 )",
                  list(3,true),  true , null, null, p},

                { "((LAMBDA (&KEY (a (+ 5 2) sa)) (LIST a sa)) )",
                  list(7,false),  true , null, null, p},
                { "((LAMBDA (&KEY (a NIL sa)) (LIST a sa)) )",
                  list(null,false),  true , null, null, p},
                { "((LAMBDA (&KEY (a NIL sa)) (LIST a sa)) :a NIL)",
                  list(null,true),  true , null, null, p},


                { "((LAMBDA (&OPTIONAL (a (+ 5 2) sa)) (LIST a sa)) 3 )",
                  list(3,true),  true , null, null, p},

                { "((LAMBDA (&OPTIONAL (a (+ 5 2) sa)) (LIST a sa)) )",
                  list(7,false),  true , null, null, p},
                { "((LAMBDA (&OPTIONAL (a NIL sa)) (LIST a sa)) )",
                  list(null,false),  true , null, null, p},
                { "((LAMBDA (&OPTIONAL (a NIL sa)) (LIST a sa)) NIL)",
                  list(null,true),  true , null, null, p},
		
                {"((LAMBDA (x &OPTIONAL (a (+ 1 x) sa)) (LIST x a sa)) 3)",
                 list(3, 4 , false), true, null, null, p},

                {"((LAMBDA (x &OPTIONAL (a (+ 1 x) sa)) (LIST x a sa)) 3 5)",
                 list(3, 5 , true), true, null, null, p},
		
                { "((LAMBDA (&LAZY a b c) (LIST a b c)) 1 2 3)",
                  list(1,2,3),  true , null, null, p},		
		
                { "(PROGN (DEFUN tkwf (&KEY a b c) (LIST a b c)) (tkwf :a 1 :b 2 ))",
                  list(1,2,null),  true , null, null, p},
                { "(PROGN (DEFUN tkwf (&KEY a b c) (LIST a b c)) (FUNCALL (FUNCTION tkwf) :a 1 :b 2 ))",
                  list(1,2,null),  true , null, null, p},
                { "(LET ((f (LAMBDA (A B C) (* A A B B C C)))) (FUNCALL f 1 2 3))", 36, true, null, null, p},
                {"(LET ((f (LAMBDA (x) (IF x (LET ((y (- x 1))) (FUNCALL f y)) 0)))) (FUNCALL f 1))",        0, false, null, null, p},
                {"(LET ((f (LAMBDA (x) (IF x (* x (FUNCALL f (- x 1))) 1)))) (FUNCALL f 5))", 120, true, null, null, p},
                
                {"(LIST)", list(), false, null, null, p},
                {"(LIST 1 2 3 4)", list(1,2,3,4), true, null, null, p},
                {"(LIST (+ 1 2) (+ 1 1) 3 4)", list(3,2,3,4), true, null, null, p},
                {"(NTH 2 (LIST 1 2 3 4))", new Integer(3), true, null, null, p},
                {"(NTH 5 (LIST 1 2 3 4))", null, false, null, null, p},
                {"(NTH 5 NIL)", null, false, null, null, p},
                {"(NTH 2 (. (LIST 1 2 3 4) \"toArray()\" ))", new Integer(3),
                 true, null, null, p},
                {"(NTH 5 (. (LIST 1 2 3 4) \"toArray()\" ))", null,    false, null, null, p},
                {"(LET ((s (MAKE-ARRAY 2 :element-type (QUOTE int)))) (NTH 0 s))", 0, false, null,null,p},
                {"(LET ((s (MAKE-ARRAY 2 :element-type (QUOTE int)))) (NTH 2 s))", null, false, null,null,p},
                {"(LET ((s (.N \"java.lang.StringBuilder\")))  (. s \"append\" (LIST \"abc\")) (NTH 2 s))", 'c', true, null,null,p},
                {"(LET ((s (.N \"java.lang.StringBuilder\")))  (. s \"append\" (LIST \"abc\")) (NTH 3 s))", null, false, null,null,p},
                {"(CONS 1 (LIST 2 3 4))", list(1, 2, 3, 4), true, null, null, p},
                {"(CONS 1 2)", list(1, 2),  true, null, null, p},
                {"(APPEND (LIST 1 2) (LIST 3) 4)", list(1, 2, 3, 4),  true, null, null, p},
                {"(APPEND)", list(),  false, null, null, p},
                {"(APPEND () 1 2 3)", list(1,2,3), true, null, null, p},
                {"(APPEND (MAKE-ARRAY 0 :element-type \"int\") 1 2 3)", Utils.array(1,2,3), true, null, null, p},
                {"(APPEND (MAKE-ARRAY 1 :element-type \"int\") 1 (LIST 2 3) (MAKE-ARRAY 2) \"AB\")", Utils.array(0,1,2,3,0,0,65,66), true, null, null, p},
                {"(APPEND (MAKE-ARRAY 1 :element-type \"char\") \"aa\" (LIST (CHAR 113) 65))", Utils.array('\0','a','a','q','A'), true,null,null,p},
                {"(APPEND (MAKE-ARRAY 1 :element-type \"byte\") 1 (LIST 2 3) (MAKE-ARRAY 2) \"AB\")", Utils.array((byte)0,(byte)1,(byte)2,(byte)3,(byte)0,(byte)0,(byte)65,(byte)66), true, null, null, p},
                {"(APPEND (MAKE-ARRAY 1 :element-type \"short\") 1 (LIST 2 3) (MAKE-ARRAY 2) \"AB\")", Utils.array((short)0,(short)1,(short)2,(short)3,(short)0,(short)0,(short)65,(short)66), true, null, null, p},
		
                {"(FIRST (LIST 1 2 3))",1, true, null, null, p},
                {"(REST (LIST 1 2 3))", list(2,3), true, null, null, p},
                {"(REST (LIST 1))", list(), false, null, null, p},
                {"(LENGTH \"1234\")", 4, true, null, null, p},
                {"(LENGTH (LIST 1 2 3 (LIST 5 6)))", 4, true, null, null, p},
                {"(LENGTH (MAKE-ARRAY 2 :element-type (QUOTE int)))", 2 ,true, null, null, p},
                {"(LENGTH (MAKE-ARRAY 2))", 2 ,true, null, null, p},
                {"(LENGTH NIL)", 0, false, null, null, p},
                {"(LENGTH \"\")", 0, false, null, null, p},
                {"(LENGTH 222)", 1, true, null, null, p},

                {"(SUBSEQ (LIST 1 2 3 4) 0)",   list(1,2,3,4) , true, null, null, p},
                {"(SUBSEQ (LIST 1 2 3 4) 1)",   list(2,3,4) , true, null, null, p},
                {"(SUBSEQ (LIST 1 2 3 4) 4)",   list() , false, null, null, p},
                {"(SUBSEQ (LIST 1 2 3 4) 0 3)", list(1,2,3) , true, null, null, p},
                {"(SUBSEQ (LIST 1 2 3 4) 1)",   list(2,3,4) , true, null, null, p},
                {"(SUBSEQ (LIST 1 2 3 4) 4 4)", list() , false, null, null, p},

                {"(SUBSEQ \"1234\" 0)",   "1234" , true, null, null, p},
                {"(SUBSEQ \"1234\" 1)",   "234", true, null, null, p},
                {"(SUBSEQ \"1234\" 4)",   "" , false, null, null, p},
                {"(SUBSEQ \"1234\" 0 3)", "123" , true, null, null, p},
                {"(SUBSEQ \"1234\" 1)",   "234" , true, null, null, p},
                {"(SUBSEQ \"1234\" 4 4)", "" , false, null, null, p},
                {"(IN  (CHAR 81)  \"Quogga\")", true , true, null, null, p},
                {"(IN  (CHAR 97)  \"Quogga\")", true , true, null, null, p},
                {"(IN  (CHAR 97)  \"Quagga\")", true , true, null, null, p},
                {"(IN  (CHAR 122)  \"Quagga\")", false , false, null, null, p},
                {"(IN  NIL  \"Quagga\")", false , false, null, null, p},
                {"(IN \"FOO\"   (LIST \"BAR\" \"BAZ\"))", false , false, null, null, p},
                {"(IN \"QQQ\"   (LIST \"BAR\" \"FOO\" \"QQQ\"))", true, true, null, null, p},
                {"(LET ((A (MAKE-ARRAY 4))) (ASET A 0 1) (ASET A 1 2) (ASET A 2 3) (ASET A 3 4)  (SUBSEQ A 0))",   new Object[] {1,2,3,4}  , true, null, null, p},
                {"(LET ((A (MAKE-ARRAY 4))) (ASET A 0 1) (ASET A 1 2) (ASET A 2 3) (ASET A 3 4)  (SUBSEQ A 1))",   new Object[] {2,3,4}  , true, null, null, p},
                {"(LET ((A (MAKE-ARRAY 4))) (ASET A 0 1) (ASET A 1 2) (ASET A 2 3) (ASET A 3 4)  (SUBSEQ A 4))",   new Object[] {}  , false, null, null, p},
		
                {"(LET ((A (MAKE-ARRAY 4 :element-type \"int\"))) (ASET A 0 1) (ASET A 1 2) (ASET A 2 3) (ASET A 3 4)  (SUBSEQ A 0))",   new int[] {1,2,3,4}  , true, null, null, p},
                {"(LET ((A (MAKE-ARRAY 4 :element-type \"int\"))) (ASET A 0 1) (ASET A 1 2) (ASET A 2 3) (ASET A 3 4)  (SUBSEQ A 1))",   new int[] {2,3,4}  , true, null, null, p},
                {"(LET ((A (MAKE-ARRAY 4  :element-type \"int\"))) (ASET A 0 1) (ASET A 1 2) (ASET A 2 3) (ASET A 3 4)  (SUBSEQ A 4))",   new int[] {}  , false, null, null, p},

		
                // COERCION
                {"(STRING 1)",  "1", true, null, null, p},
                {"(STRING \"a\")",  "a", true, null, null, p},
                {"(STRING true)",  "true", true, null, null, p},
                {"(STRING (CHAR 97))",  "a", true, null, null, p},
                {"(STRING NIL)",  "NIL", true, null, null, p},
                {"(BOOL 1)",  true, true, null, null, p},
                {"(BOOL 1)",  true, true, null, null, p},
                {"(BOOL 0)",  false, false, null, null, p},
                {"(BOOL \"\")",  false, false, null, null, p},
                {"(BOOL \"foo\")",  true, true, null, null, p},
                {"(BOOL true)",  true, true, null, null, p},
                {"(BOOL false)",  false, false, null, null, p},
                {"(BOOL ())",  false, false, null, null, p},
                {"(BOOL (LIST NIL))", true, true, null, null, p},
                {"(BOOL (MAKE-ARRAY 0))", false, false, null, null, p},
                {"(BOOL (MAKE-ARRAY 1))", true, true, null, null, p},
                {"(INT 1)",  1, true, null, null, p},
                {"(INT 1.0)",  1, true, null, null, p},
                {"(INT 0)",  0, false, null, null, p},
                {"(INT -1)", -1, true, null, null, p},
                {"(INT (CHAR 67))", 67, true, null, null, p},
                {"(INT (CHAR 0))", 0, false, null, null, p},
                {"(INT \"22\")", 22, true, null, null, p},
                {"(INT \" 22 \")", 22, true, null, null, p},
                {"(INT \" 22.1 \")", 22, true, null, null, p},
                {"(INT (LIST))", 0, false, null, null, p},
                {"(INT (LIST 1 2))", 2, true, null, null, p},
                {"(INT \"\")", null, false,
                 new ExecutionException(null,new RuntimeException("String '' cannot be coerced to Number",
                                                                  new NumberFormatException("Invalid numeric literal ''")))
                 , null, p},
                {"(INT \" QQ\")", null, false,
                 new ExecutionException(null,new RuntimeException("String ' QQ' cannot be coerced to Number",
                                                                  new NumberFormatException("Invalid numeric literal 'QQ'")))
                 , null, p},
                {"(LONG 1)",  1L, true, null, null, p},
                {"(LONG 1.0)",  1L, true, null, null, p},
                {"(LONG 0)",  0L, false, null, null, p},
                {"(LONG -1)", -1L, true, null, null, p},
                {"(LONG \"22\")", 22L, true, null, null, p},
                {"(LONG \" 22 \")", 22L, true, null, null, p},
                {"(LONG \" 22.1 \")", 22L, true, null, null, p},
                {"(LONG \"\")", null, false,
                 new ExecutionException(null,new RuntimeException("String '' cannot be coerced to Number",
                                                                  new NumberFormatException("Invalid numeric literal ''")))
                 , null, p},
                {"(LONG \" QQ\")", null, false,
                 new ExecutionException(null,new RuntimeException("String ' QQ' cannot be coerced to Number",
                                                                  new NumberFormatException("Invalid numeric literal 'QQ'")))
                 , null, p},

                {"(DOUBLE 1)",  1.0, true, null, null, p},
                {"(DOUBLE 1.0)",  1.0, true, null, null, p},
                {"(DOUBLE 0)",  0.0, false, null, null, p},
                {"(DOUBLE -1)", -1.0, true, null, null, p},
                {"(DOUBLE \"22\")", 22.0, true, null, null, p},
                {"(DOUBLE \" 22 \")", 22.0, true, null, null, p},
                {"(DOUBLE \" 22.1 \")", 22.1, true, null, null, p},
                {"(DOUBLE \"\")", null, false,
                 new ExecutionException(null,new RuntimeException("String '' cannot be coerced to Number",
                                                                  new NumberFormatException("Invalid numeric literal ''")))
                 , null, p},
                {"(DOUBLE \" QQ\")", null, false,
                 new ExecutionException(null,new RuntimeException("String ' QQ' cannot be coerced to Number",
                                                                  new NumberFormatException("Invalid numeric literal 'QQ'")))
                 , null, p},

                {"(FLOAT 1)",  (float)1.0, true, null, null, p},
                {"(FLOAT 1.0)",  (float)1.0, true, null, null, p},
                {"(FLOAT 0)",   (float)0.0, false, null, null, p},
                {"(FLOAT -1)",  (float)-1.0, true, null, null, p},
                {"(FLOAT \"22\")",  (float)22.0, true, null, null, p},
                {"(FLOAT \" 22 \")", (float)22.0, true, null, null, p},
                {"(FLOAT \" 22.1 \")", (float)22.1, true, null, null, p},
                {"(FLOAT \"\")", null, false,
                 new ExecutionException(null,new RuntimeException("String '' cannot be coerced to Number",
                                                                  new NumberFormatException("Invalid numeric literal ''")))
                 , null, p},
                {"(FLOAT \" QQ\")", null, false,
                 new ExecutionException(null,
                                        new RuntimeException("String ' QQ' cannot be coerced to Number",
                                                             new NumberFormatException("Invalid numeric literal 'QQ'")))
                 , null, p},
                {"(SHORT 1)",  (short)1, true, null, null, p},
                {"(SHORT 1.0)",  (short)1, true, null, null, p},
                {"(SHORT 0)",   (short)0, false, null, null, p},
                {"(SHORT -1)",  (short)-1, true, null, null, p},
                {"(SHORT \"22\")",  (short)22, true, null, null, p},
                {"(SHORT \" 22 \")", (short)22, true, null, null, p},
                {"(SHORT \" 22.1 \")", (short)22, true, null, null, p},
                {"(SHORT \"\")", null, false,
                 new ExecutionException(null,new RuntimeException("String '' cannot be coerced to Number",
                                                                  new NumberFormatException("Invalid numeric literal ''")))
                 , null, p},
                {"(SHORT \" QQ\")", null, false,
                 new ExecutionException(null,
                                        new RuntimeException("String ' QQ' cannot be coerced to Number",
                                                             new NumberFormatException("Invalid numeric literal 'QQ'")))
                 , null, p},
                {"(BYTE 1)",  (byte)1, true, null, null, p},
                {"(BYTE 1.0)",  (byte)1, true, null, null, p},
                {"(BYTE 0)",   (byte)0, false, null, null, p},
                {"(BYTE -1)",  (byte)-1, true, null, null, p},
                {"(BYTE \"22\")",  (byte)22, true, null, null, p},
                {"(BYTE \" 22 \")", (byte)22, true, null, null, p},
                {"(BYTE \" 22.1 \")", (byte)22, true, null, null, p},
                {"(BYTE \"\")", null, false,
                 new ExecutionException(null,new RuntimeException("String '' cannot be coerced to Number",
                                                                  new NumberFormatException("Invalid numeric literal ''")))
                 , null, p},
                {"(BYTE \" QQ\")", null, false,
                 new ExecutionException(null,
                                        new RuntimeException("String ' QQ' cannot be coerced to Number",
                                                             new NumberFormatException("Invalid numeric literal 'QQ'")))
                 , null, p},
                {"(CHAR NIL)", '\0', false, null, null, p},
                {"(CHAR 48)", '0', true, null, null, p},
                {"(CHAR (CHAR 48))", '0', true, null, null, p},
                {"(CHAR 198)", '\u00c6' , true, null, null, p}, //AE ligature
                {"(CHAR -58b)", '\u00c6' , true, null, null, p}, // AE ligature 
                {"(CHAR \"198\")", '\u00c6', true, null, null, p}, // AE ligature 
                {"(CHAR true)", 'T' , true, null, null, p},
                {"(CHAR false)", '\0',false, null, null, p},

                {"(SIGNUM 2)",1, true, null, null, p},
                {"(SIGNUM -2)",-1, true, null, null, p},
                {"(SIGNUM 0)",0, false, null, null, p},

                {"(SIGNUM 2L)",1L, true, null, null, p},
                {"(SIGNUM -2L)",-1L, true, null, null, p},
                {"(SIGNUM 0L)",0L, false, null, null, p},
                {"(SIGNUM -63072000000L)",-1L, true, null, null, p},

                {"(SIGNUM 2.0)",1.0, true, null, null, p},
                {"(SIGNUM -2.0)",-1.0, true, null, null, p},
                {"(SIGNUM 0.0)",0.0, false, null, null, p},

                {"(SIGNUM 2.0f)",(float)1.0, true, null, null, p},
                {"(SIGNUM -2.0f)",(float)-1.0, true, null, null, p},
                {"(SIGNUM 0.0f)",(float)0.0, false, null, null, p},

                {"(SIGNUM 2s)",(short)1.0, true, null, null, p},
                {"(SIGNUM -2s)",(short)-1.0, true, null, null, p},
                {"(SIGNUM 0s)",(short)0.0, false, null, null, p},

                {"(SIGNUM 2b)",(byte)1, true, null, null, p},
                {"(SIGNUM -2b)",(byte)-1, true, null, null, p},
                {"(SIGNUM 0b)",(byte)0, false, null, null, p},
                //FFI
                // medhod w/o parameters
                {"(. \"FOO\" \"length\" (LIST ) )", new Integer(3), true, null, null, p},
                {"(. \"FOO\" \"length\" () )", new Integer(3), true, null, null, p},
                {"(. \"FOO\" \"length()\" )", new Integer(3), true, null, null, p},
                // field of object
                {"(. 3 \"MAX_VALUE\" )", Integer.MAX_VALUE, true, null, null, p},
                {"(. 3 \"MAX_VALUE\" \"MIN_VALUE\" )", Integer.MIN_VALUE, true, null, null, p},
                // combinations
                {"(. \"FOO\" \"length\" () \"MAX_VALUE\" )", Integer.MAX_VALUE, true, null, null, p},
                {"(. \"FOO\" \"length().MIN_VALUE.MAX_VALUE\" )", Integer.MAX_VALUE, true, null, null, p},
                // arguments
                {"(. \"\" \"format\" (LIST \"foo=%d m=%s\" (. (LIST 1 \"qqq\") \"toArray()\")))",
                 "foo=1 m=qqq", true, null, null, p},
                // type specs
                {"(LET ((M (HASHMAP))(OT \"Object\")) (. M \"put\" (LIST 2 3L) (LIST OT OT) ) M)",
                 new HashMap<Object,Object>() {{this.put(2,3L);}}, true, null, null, p},
                {"(.S \"java.lang.Runtime\" \"getRuntime()\")",
                 Runtime.getRuntime(), true, null, null, p},
                {"(. \"foo\" \"charAt\" (LIST 1) (LIST \"int\"))", 'o', true, null, null, p},
                {"(.S \"java.lang.Runtime\" \"getRuntime().availableProcessors()\")",
                 Runtime.getRuntime().availableProcessors(), true, null, null, p},

                {"(.S \"Math\" \"abs\" (LIST -1.0) (LIST \"double\"))", 1.0, true, null, null, p},
                {"(.N \"java.util.LinkedList\")",            new LinkedList<Object>(), false, null, null, p},
                {"(.N \"java.util.Locale\" (LIST \"en\" \"US\"))",  new java.util.Locale("en","US"), true, null, null, p},

                {"(.N \"java.util.GregorianCalendar\" (LIST 1976 01 02) "+
                 "(LIST \"int\" \"int\" \"int\"))",
                 new java.util.GregorianCalendar(1976,01,02), true, null, null, p},
		 
                {"(CLASS \"java.lang.String\")", String.class, true, null, null, p},
                {"(CLASS (QUOTE java.lang.String))", String.class, true, null, null, p},
                {"(TYPE-OF \"FOO\")", String.class, true, null, null, p},
                {"(TYPEP 1 (CLASS (QUOTE java.lang.Integer)))", true, true, null, null, p},
                {"(TYPEP 1 (CLASS (QUOTE Number)))", true, true, null, null, p},
                {"(TYPEP () (QUOTE java.util.List))", true, true, null, null, p},
                {"(TYPEP \"\" \"String\")", true, true, null, null, p},
		
		
                {"(THROW \"bla\")",  null, true, new ExecutionException(null, "bla"), null, p},
                {"(THROW (.N \"java.lang.RuntimeException\" (LIST \"bla\")))",  null, true,
                 new ExecutionException(null, new RuntimeException("bla")), null, p},
		
                // {"(THROW \"foo\" (.N \"java.lang.RuntimeException\" (LIST \"bla\")))",  null, true,
                //  new ExecutionException(null, "foo", new RuntimeException("bla")), null, p},

                { "(MAP (LAMBDA (x) (+ x 1)) )" , null, true,  new ExecutionException(null, new RuntimeException("At least one sequence must be provided")) , null, p},
                { "(MAPPROD (LAMBDA (x) (+ x 1)) )" , null, true,  new ExecutionException(null, new RuntimeException("At least one sequence must be provided")) , null, p},
                { "(MAP (LAMBDA (x) (+ x 1)) (LIST 1 2 3))" , list(2,3,4), true, null , null, p},
                { "(MAP (LAMBDA (x y) (+ x (* 2 y))) (LIST 1 2 3) (LIST 4 5 6))" ,
                  list(9,12,15), true, null , null, p},
                { "(MAP (LAMBDA (x y) (+ x (* 2 y))) (LIST 1 2 3 4 5) (LIST 4 5 6))" ,
                  list(9,12,15), true, null , null, p},
                { "(MAP (LAMBDA (x y) (+ x (* 2 y))) (LIST) (LIST 4 5 6))" ,list(), false,null, null, p},
                {"(LET ((f (LAMBDA (x) (+ x 1))) (l (LIST 1 2 3))) (MAP f l))",
                 list(2,3,4), true, null , null, p},
                { "(MAP (LAMBDA (x y z) (+ x y z))  (LIST 1 2 3) (LIST 0 10) (LIST))" ,
                  list(), false, null , null, p},
                { "(MAP (FUNCTION +)  (LIST 1 2 3) (LIST 4 5 6) (LIST 7 8 9))" ,               
                  list(12, 15, 18), true, null , null, p},
                { "(MAPPROD (LAMBDA (x) (+ x 1)) (LIST 1 2 3))" , list(2,3,4), true, null , null, p},
                { "(MAPPROD (LAMBDA (x y) (+ x y))  (LIST 1 2 3) (LIST 0 10 20 30))" ,
                  list(1, 2 ,3, 11, 12, 13, 21,22,23,31,32,33), true, null , null, p},
                { "(MAPPROD (LAMBDA (x y z) (+ x y z))  (LIST 1 2 3) (LIST 0 10) (LIST 100))" ,
                  list(101, 102 ,103, 111, 112, 113), true, null , null, p},
                { "(MAPPROD (LAMBDA (x y z) (+ x y z))  (LIST 1 2 3) (LIST 0 10) (LIST))" ,
                  list(), false, null , null, p},
                { "(MAPPROD (LAMBDA (x y z) (+ x y z))  (LIST 1 2 3) (LIST 0 10) (LIST))" ,
                  list(), false, null , null, p},
                { "(MAPPROD (LAMBDA (x y z) (+ x y z))  (LIST) (LIST 0 10) (LIST 1))" ,
                  list(), false, null , null, p},
                { "(MAPPROD (LAMBDA (x ) (+ 1 x))  (LIST))" ,
                  list(), false, null , null, p},
                { "(MAPPROD (LAMBDA (x ) (+ 1 x))  NIL)" ,
                  list(), false, null , null, p},
                { "(MAPPROD (LAMBDA (x y) (+ x y))  (LIST 1) (LIST 1))" ,
                  list(2), true, null , null, p},

                { "(FILTER (LAMBDA (x) (> x 0))  (LIST -2 -1 0 1 2))" ,
                  list(1,2), true, null , null, p},
                
                { "(PROGN (DEFUN INCR (x) (+ 1 x))  (INCR 1))" , 2, true, null, null, p},
                { "(PROGN (DEFUN SELF (x) x)  (SELF 1))" , 1, true, null, null, p},
                { "(PROGN (DEFUN SELF (x) x)  (SELF NIL))" , null, false, null, null, p},
                
                
                { "(PROGN (DEFUN INCR (x) (+ 1 x))  (FUNCALL (FUNCTION INCR) 5))" , 6, true, null, null, p},
                                
                { "(FUNCALL (FUNCTION +) 1 2 3)" , 6, true, null, null, p},
                
                { "(FUNCALL (FUNCTION +) 1 (+ 1 1)  3)" , 6, true, null, null, p},
                { "(APPLY (FUNCTION +) 1 2 3 4)", 10, true, null, null, p},
                { "(APPLY (FUNCTION +) 1 (LIST 2 3 4))", 10, true, null, null, p},
                { "(APPLY (FUNCTION +) (LIST 1 2 3 4))", 10, true, null, null, p},
                { "(APPLY (FUNCTION +) )", 0, false, null, null, p},

                { "(FUNCALL (SYMBOL-FUNCTION \"+\") 1 2 3)" , 6, true, null, null, p},
                { "(APPLY (SYMBOL-FUNCTION \"+\") 1 2 3 4)", 10, true, null, null, p},
                { "(APPLY (LAMBDA (&KEY a b c) (LIST a b c)) :a 1 :b 2 ())",
                  list(1,2,null), true, null, null, p},
                { "(APPLY (LAMBDA (&KEY a b &REST r) (LIST a b r)) :a 1 2 3 4)",
                  list(1,null,list(2,3,4)), true, null, null, p},

                // call must be dynamic
                { "(PROGN (DEFUN zqq () 1) (DEFUN zpp () (zqq)) (DEFUN zqq () 2) (zpp))",
                  2, true, null, null, p}, 
		
                { "(REVERSE (LIST 7 5 2 3 4 -1 0))", list(0,-1,4,3,2,5,7),true, null, null, p},
                { "(REVERSE (LIST 2))", list(2),true, null, null, p},
                { "(REVERSE (LIST NIL))", list((Object)null),true, null, null, p},
                { "(REVERSE (LIST))", list(),false, null, null, p},
                { "(REVERSE NIL)", null,false, null, null, p},

                { "(LET ((s (LIST 1 2 3))) (LIST (REVERSE s) s))",
                  list(list(3,2,1),list(1,2,3)), true, null, null, p},
                { "(LET ((s (LIST 1 2 3))) (LIST (REVERSE! s) s))",
                  list(list(3,2,1),list(3,2,1)), true, null, null, p},

                { "(SORT (LIST 7 5 2 3 4 -1 0))", list(-1,0,2,3,4,5,7),true, null, null, p},
                { "(SORT (LAMBDA (a b) (- a b)) (LIST 7 5 2 3 4 -1 0))",
                  list(-1,0,2,3,4,5,7), true, null, null, p},
                { "(REDUCE (FUNCTION +) (LIST))", 0, false, null, null, p},
                { "(REDUCE (FUNCTION +) (LIST 1))", 1 , true, null,null, p},
                { "(REDUCE (FUNCTION +) (LIST 1 2))", 3, true, null, null, p},
                { "(REDUCE (FUNCTION +) 1 (LIST))", 1, true, null,null, p},
                { "(REDUCE (FUNCTION +) 1 (LIST 2 3))", 6, true, null,null, p},
                { "(REDUCE (FUNCTION +) 1 (LIST 2 3))", 6, true, null,null, p},
                { "(REDUCE (FUNCTION +) (LIST 1 2 3 4 5))", 15, true, null,null, p},
                { "(REDUCE (FUNCTION +) 1 (LIST 1 2 3 4 5))", 16, true, null,null, p},
                {"(LET ((l (LIST 7 5 1))) (LIST l (SORT l)))",
                 list(list(7,5,1), list(1,5,7)), true, null, null, p},
                {"(LET ((l (LIST 7 5 1))) (LIST l (SORT! l)))",
                 list(list(1, 5, 7), list(1,5,7)), true, null, null, p},
                // FIXME: tests for other seq. types sort
                // comments
                {";; (\r\n 3 ; 1 2 \n ;; )", 3, true, null, null, p},
                
                // recursive defun
                {"(PROGN (DEFUN fact (x) (IF x (* x (fact (- x 1))) 1)) (fact 5))", 120, true, null, null, p},

                {"(RE-PATTERN \"^F[0-9]$\")", Pattern.compile("^F[0-9]$"), true, null,null,p},
                {"(RE-MATCHER (RE-PATTERN \"^F[0-9]$\") \"BLA\")", Pattern.compile("^F[0-9]$").matcher("BLA"), true, null,null,p},
                {"(RE-MATCHES (RE-MATCHER (RE-PATTERN \"^F[0-9]$\") \"F1\"))", "F1", true, null,null,p},
                {"(RE-MATCHES (RE-MATCHER (RE-PATTERN \"^(F)([0-9])$\") \"F1\"))", list("F1","F","1"), true, null,null,p},
                {"(RE-MATCHES (RE-MATCHER (RE-PATTERN \"^(F)([0-9])$\") \"BLA\"))", null, false, null,null,p},
                {"(RE-MATCHES (RE-PATTERN \"^F[0-9]$\") \"F1\")", "F1", true, null,null,p},
                {"(RE-MATCHES (RE-PATTERN \"^(F)([0-9])$\") \"F1\")", list("F1","F","1"), true, null,null,p},
                {"(RE-MATCHES (RE-PATTERN \"^(F)([0-9])$\") \"BLA\")", null, false, null,null,p},
                {"(RE-FIND (RE-PATTERN \"(F)([0-9])\") \"BLA\")", null, false, null,null,p},
                {"(RE-FIND (RE-PATTERN \"(F)([0-9])\") \"F1\")", list("F1","F","1"), true, null,null,p},

                {"(DWIM-MATCHES \"a1231b\" (RE-PATTERN \"[0-9]\"))", list("1","2","3","1"), true, null,null,p},
                {"(DWIM-MATCHES \"a1231b\" \"1\")", list("1"), true, null,null,p},
                {"(DWIM-MATCHES NIL NIL)", list((Object)null), true, null,null,p},
                {"(DWIM-MATCHES 1 1)", list(1), true, null,null,p},
                {"(DWIM-MATCHES 1 1.0)", list(1), true, null,null,p},
                {"(DWIM-MATCHES 1.0 1)", list(1.0), true, null,null,p},
                {"(GET-IN  NIL        NIL)", null, false, null,null,p},
                {"(GET-IN  NIL        NIL \"Nope\")", null, false, null,null,p},
                {"(GET-IN  (LIST 1 2) NIL \"Nope\")", list(1,2), true, null,null,p},

                {"(GET-IN  (LIST)     0)", null, false,
                 new ExecutionException("Cannot use the provided class java.lang.Integer value as list of indexes"),null,p},
                {"(GET-IN  (LIST)     0 \"Nope\")", null, false,
                 new ExecutionException("Cannot use the provided class java.lang.Integer value as list of indexes"),null,p},
                {"(GET-IN  (LIST 10)  (LIST 0))", 10, true, null,null,p},
                {"(GET-IN  (LIST 10)  (LIST 0) \"Nope\")", 10, true, null,null,p},
                {"(GET-IN  (LIST 10)  (LIST 1) \"Nope\")", "Nope", true, null,null,p},
                {"(GET-IN  (LIST 10)  () \"Nope\")", list(10), true, null,null,p},
                {"(GET-IN  (HASHMAP \"foo\" 111 )  (LIST \"foo\") \"Nope\")", 111, true, null,null,p},
                {"(GET-IN  (HASHMAP \"foo\" 111 )  (LIST \"boo\") \"Nope\")", "Nope", true, null,null,p},
                {"(GET-IN  (HASHMAP \"foo\" (HASHMAP 11 22 ))  (LIST \"foo\" 11) \"Nope\")", 22, true, null,null,p},
                {"(GET-IN  (HASHMAP \"foo\" (HASHMAP 11 \"AQ\" ))  (LIST \"foo\" 11 1) \"Nope\")", 'Q', true, null,null,p},
                {"(GET  (HASHMAP \"foo\" \"bar\")  \"foo\"  \"Nope\")", "bar", true, null,null,p},
                {"(GET  (HASHMAP \"foo\" \"bar\")  \"mmm\"  \"Nope\")", "Nope", true, null,null,p},
                {"(GET  (HASHMAP \"foo\" \"bar\")  \"mmm\")", null, false, null,null,p},
                {"(GET  testbean \"name\")", testBean.getName(), true, null,beanVars,p},
                {"(GET  testbean \"nosuchkey\")", null, false, null,beanVars,p},
                {"(GET  testbean \"name\" \"Nope\")", testBean.getName(), true, null,beanVars,p},
                {"(GET  testbean \"nosuchkey\" \"Nope\")", "Nope", true, null,beanVars,p},
                {"(GET  (LIST 11 12)  1 \"Nope\")", 12, true, null,beanVars,p},
                {"(GET  (LIST 11 12)  2 \"Nope\")", "Nope", true, null,beanVars,p},
                {"(GET  (LIST 11 12)  2 )", null, false, null,beanVars,p},
                {"(GET  (LIST 11 12)  \"sss\" \"Nope\")", "Nope", true, null,beanVars,p},
                {"(GET  \"ASDF\"  1)", 'S', true, null,null,p},
                {"(GET  \"ASDF\"  4)", null, false, null,null,p},
                {"(GET  \"ASDF\"  4 \"Nope\")", "Nope", true, null,null,p},
                {"(GET  null  4 \"Nope\")", "Nope", true, null,null,p},
                {"(GET  null  4)", null,  false, null,null,p},
                
                {"(ASSOC  (HASHMAP) 1 2)", map(1,2), true, null,null,p},
                {"(ASSOC  (HASHMAP) 1 2 3 4)", map(1,2,3,4), true, null,null,p},
                {"(ASSOC  (HASHMAP) 1 2 3)", map(1,2,3,null), true, null,null,p},
                {"(ASSOC  (HASHMAP 1 2) 3 4 5 6)", map(1,2,3,4,5,6), true, null,null,p},
                {"(PROGN (SETQ m (HASHMAP)) (ASSOC m 2 3) m)", map(), true, null,null,p},

                {"(ASSOC!  (HASHMAP) 1 2)", map(1,2), true, null,null,p},
                {"(ASSOC!  (HASHMAP) 1 2 3 4)", map(1,2,3,4), true, null,null,p},
                {"(ASSOC!  (HASHMAP) 1 2 3)", map(1,2,3,null), true, null,null,p},
                {"(ASSOC!  (HASHMAP 1 2) 3 4 5 6)", map(1,2,3,4,5,6), true, null,null,p},
                {"(PROGN (SETQ m (HASHMAP)) (ASSOC! m 2 3) m)", map(2,3), true, null,null,p},

                
                {"(LET ((a (MAKE-ARRAY 1 :element-type (QUOTE int)))) (ASET a 0 8) (GET-IN a (LIST 0)))"
                 ,8, true,null,null,p},
		
                {"(LET ((M (RE-MATCHER (RE-PATTERN \"(F)([0-9])\") \"F1\"))) (LIST (RE-FIND M) (RE-FIND M)))", list(list("F1","F","1"), null), true, null,null,p},
                {"(LET ((M (RE-MATCHER (RE-PATTERN \"(F)([0-9])\") \"F1\"))) (LIST (RE-FIND M) (RE-GROUPS M) (RE-GROUPS M)))", list(list("F1","F","1"),list("F1","F","1"),list("F1","F","1")), true, null,null,p},
                {"(LET ((S (RE-SEQ (RE-PATTERN \"F[0-9]\") \"F1F2F3\"))) (APPEND () S S))" ,list("F1","F2","F3","F1","F2","F3"), true, null,null,p},
                {"(LET ((S (RE-SEQ (RE-MATCHER (RE-PATTERN \"F[0-9]\") \"F1F2F3\")))) (APPEND () S S))" ,list("F1","F2","F3"), true, null,null,p},
                {"(LET ((M (RE-MATCHER (RE-PATTERN \"F[0-9]\") \"F1F2F3\"))(S (RE-SEQ M)) (A ())) (APPEND! A S) (. M \"reset()\") (APPEND! A S) A)" ,list("F1","F2","F3","F1","F2","F3"), true, null,null,p},

                {"(STR \"FOO \" 5 \" BAR\")", "FOO 5 BAR", true, null, null, p},
                {"(FORMAT \"a=%d b=%s\" 1 \"foo\")", "a=1 b=foo", true, null, null, p},
                {"(FORMAT \"foo\")", "foo", true, null, null, p},
                {"(SEQ (. \"H-E-L-P\" \"split\" (LIST \"-\")))", list("H","E","L","P"), true, null, null, p},
                {"(SYMBOL \"fooo\")", new Symbol("fooo"), true, null, null, p},
                {"(WITH-BINDINGS (HASHMAP \"a\"   1   \"B\"    2  \"c\" 3 \"D\" 4) (+ a B c D))",
                 10 , true, null, null, p},
                {"(WITH-BINDINGS (BEAN testbean) (LIST name surName age accepted children))",
                 list(testBean.getName(),
                      testBean.getSurName(),
                      testBean.getAge(),
                      testBean.isAccepted(),
                      testBean.getChildren()),
                 true, null, beanVars, p},
                {"(WITH-BINDINGS (BEAN testbean \"a-\" \"-b\") (LIST a-name-b a-surName-b a-age-b a-accepted-b a-children-b))",
                 list(testBean.getName(),
                      testBean.getSurName(),
                      testBean.getAge(),
                      testBean.isAccepted(),
                      testBean.getChildren()),
                 true, null, beanVars, p},
                {"(WITH-BINDINGS (BEAN testbean NIL \"-b\") (LIST name-b surName-b age-b accepted-b children-b))",
                 list(testBean.getName(),
                      testBean.getSurName(),
                      testBean.getAge(),
                      testBean.isAccepted(),
                      testBean.getChildren()),
                 true, null, beanVars, p},
                {"(SELECT-KEYS testbean (LIST \"name\" \"surName\"))",
                 Utils.map("name", testBean.getName(), "surName", testBean.getSurName()), true, null, beanVars, p},
                {"(SELECT-KEYS testbean (LIST \"name\" \"surName\" \"nosuchkey\"))",
                 Utils.map("name", testBean.getName(), "surName", testBean.getSurName()), true, null, beanVars, p},
                {"(SELECT-KEYS (HASHMAP \"name\" \"Scott\" \"surName\" \"D.\" \"familyName\" \"Adams\") (LIST \"name\" \"surName\"))",
                 Utils.map("name", "Scott", "surName", "D."), true, null, beanVars, p},
                {"(SELECT-KEYS (HASHMAP \"name\" \"Scott\" \"surName\" \"D.\" \"familyName\" \"Adams\") (LIST \"name\" \"surName\" \"noShuchKey\"))",
                 Utils.map("name", "Scott", "surName", "D."), true, null, beanVars, p},
                                {quine, parsedQuine, true, null, null, p},

                {"((LAMBDA (x y) (SETV x y) x) 1 2)",
                 2, true, null, null, p},
                {"(SETQ)", null, false, null, null, p},
                {"(SETV)", null, false, null, null, p},
                {"(SETV x 1)", 1, true, null, null, p},
                {"(SETQ x 1)", 1, true, null, null, p},
                {"(PROGN (SETQ a 1 b 2) (LIST a b))", list(1,2), true, null, null, p},
                {"(PROGN (SETV a 1 b 2) (LIST a b))", list(1,2), true, null, null, p},
                {"(LET ((x 1)) (SETV x 2) x)", 2, true, null, null, p},
                {"(LET ((x 1)) (SETQ x 2) x)", 2, true, null, null, p},
                {"(LET ((x 1)) ((LAMBDA () (SETQ x 2))) x)", 2, true, null, null, p},
                {"(PROGN (LET () (SETQ x 3)) x)", 3, true, null, null, p},
                {"(PROGN (LET () (SETV x 3)) x)", null, true, new ExecutionException(null, new RuntimeException("variable 'x' does not exist in this context")), null, p},

                {"(SET (QUOTE a) 1)", 1, true, null, null, p},
                {"(PROGN (SET (QUOTE a) 1) a)", 1, true, null, null, p},
                {"(LET () (LET () (SET (QUOTE a) 1)) a)", 1, true, null, null, p},

                {"(SET (QUOTE a) 1 :uplevel 0)", 1, true, null, null, p},
                {"(PROGN (SET (QUOTE a) 1 :uplevel 0) a)", 1, true, null, null, p},
                {"(LET () (LET () (SET (QUOTE a) 1 :uplevel 1)) a)", 1, true, null, null, p},
                {"(LET () (LET ((b 1)) (SET (QUOTE b) 5 :uplevel 1) ) b)", 5, true, null, null, p},
                {"(LET ((b 2)) (LET ((b 1)) (SET (QUOTE b) 5 :uplevel 1) ) b)", 5, true, null, null, p},
                {"(LET ((b 2)) (LET ((b 1)) (SET (QUOTE b) 5 :level 1) ) b)", 5, true, null, null, p},
                {"(LET () (LET ((b 1)) (SET (QUOTE b) 5 :level 1) ) b)", 5, true, null, null, p},


                {"(LET ((a NIL)) (LAZYAND a 1 2))", null, false, null, null, p},
                {"(LET ((a 1)) (LAZYAND a 2 3))", 3, true, null, null, p},
                {"(LET ((a 1)) (LAZYAND a a a))", 1, true, null, null, p},
                {"((LAMBDA (&LAZY x y z) (IF x y z)) 1 2 3)", 2, true, null, null, p},
                {"((LAMBDA (&LAZY x y z) (IF x y z)) 0 2 3)", 3, true, null, null, p},
                {"((LAMBDA (&LAZY x y z) (IF x y z)) 0 2 3)", 3, true, null, null, p},
                {"(LET ((r 0)) ((LAMBDA (&LAZY x y z) (IF x y z)) 0 (SETV r 1) (SETV r 2)) r)", 2, true, null, null, p},
                {"(PROGN (DEFUN LKA (&LAZY &KEY a b c) (IF a (IF b (IF c c false) false) false))  (LET ((d 0)) (LKA :a (+ 1 d) :b (NOT (= d 0)) :c (/ 1 d))))", false, false, null, null, p},
                {"(PROGN (DEFUN LA (&LAZY a b c) (IF a (IF b (IF c c false) false) false))  (LET ((d 0)) (LA (+ 1 d) (NOT (= d 0)) (/ 1 d))))", false, false, null, null, p},
                {"(PROGN (DEFUN LOA (&LAZY &OPTIONAL a b c) (IF a (IF b (IF c c false) false) false))  (LET ((d 0)) (LOA (+ 1 d) (NOT (= d 0)) (/ 1 d))))", false, false, null, null, p},
                {"(PROGN (DEFUN LROA (&LAZY &OPTIONAL a &REQUIRED b c) (IF a (IF b (IF c c false) false) false))  (LET ((d 0)) (LROA (+ 1 d) (NOT (= d 0)) (/ 1 d))))", false, false, null, null, p},
                {"(APPLY (FUNCTION +) ((LAMBDA (&LAZY &REST r) r) 1 2 3))", 6, true, null, null, p},
                // ARRAYS
                {"(MAKE-ARRAY 3)", new Object[3], true, null, null, p},
                {"(MAKE-ARRAY 3 :element-type \"Integer\")", new Integer[3], true, null, null, p},
                // ok as well
                {"(MAKE-ARRAY 3 :element-type \"Integer\")", new Object[3], true, null, null, p},
                {"(MAKE-ARRAY 3 :element-type \"int\")", new int[3], true, null, null, p},
                {"(LET ((a (MAKE-ARRAY 3 :element-type \"int\"))) (ASET a 0 1) a) ",
                 Utils.array(1, 0, 0) , true, null, null, p},
                {"(LET ((a (MAKE-ARRAY 3 :element-type \"short\"))) (ASET a 0 1) a) ",
                 Utils.array((short)1, (short)0, (short)0) , true, null, null, p},
                {"(LET ((a (MAKE-ARRAY 3 :element-type \"Short\"))) (ASET a 0 1) a) ",
                 new Short[] {1, null, null} , true, null, null, p},
                {"(LET ((a (MAKE-ARRAY 3 :element-type \"float\"))) (ASET a 0 1.1) a)  ",
                 Utils.array((float)1.1, (float)0, (float)0) , true, null, null, p},
                {"(LET ((a (MAKE-ARRAY 3 :element-type \"Float\"))) (ASET a 0 1.1) a)  ",
                 new Float[] {1.1F, null, null} , true, null, null, p},
                {"(LET ((a (MAKE-ARRAY 4 :element-type \"byte\"))) (ASET a 0 1.1) (ASET a 1 2) (ASET a 2 (CHAR 97)) (ASET a 3 true) a)",
                 Utils.array((byte)1, (byte)2, (byte)97, (byte)1) , true, null, null, p},
                {"(LET ((a (MAKE-ARRAY 4 :element-type \"Byte\"))) (ASET a 0 1.1) (ASET a 1 2) (ASET a 2 (CHAR 97)) (ASET a 3 true) a)",
                 new Byte[] {(byte)1, (byte)2, (byte)97, (byte)1} , true, null, null, p},
		
                {"(MAKE-ARRAY 1 :element-type \"char\")", new char[1], true, null, null, p},
                {"(MAKE-ARRAY 1 :element-type \"Character\")", new Character[1], true, null, null, p},
                {"(LET ((a (MAKE-ARRAY 3 :element-type \"char\"))) (ASET a 0 (CHAR 83)) a)",
                 Utils.array('S', '\0', '\0') , true, null, null, p},
                {"(LET ((a (MAKE-ARRAY 3 :element-type \"Character\"))) (ASET a 0 (CHAR 83)) a)",
                 new Character[] {'S',null, null} , true, null, null, p},
                {"(LET ((a (MAKE-ARRAY 3 :element-type \"double\"))) (ASET a 0 83.0) a) ",
                 Utils.array(83.0, 0.0, 0.0) , true, null, null, p},
                {"(LET ((a (MAKE-ARRAY 3 :element-type \"Double\"))) (ASET a 0 83.0) a) ",
                 new Double[] {83.0, null, null} , true, null, null, p},
                {"(LET ((a (MAKE-ARRAY 7 :element-type \"boolean\"))) (ASET a 0 true)  (ASET a 1 false)  (ASET a 2 1)  (ASET a 3 0)  (ASET a 4 (CHAR 67))  (ASET a 5 (CHAR 0)) a) ",
                 Utils.array(true, false, true, false, true, false, false) , true, null, null, p},
                {"(LET ((a (MAKE-ARRAY 7 :element-type \"Boolean\"))) (ASET a 0 true)  (ASET a 1 false)  (ASET a 2 1)  (ASET a 3 0)  (ASET a 4 (CHAR 67))  (ASET a 5 (CHAR 0)) a) ",
                 new Boolean[] {true, false, true, false, true, false, null} , true, null, null, p},		
                {"(LET ((a (MAKE-ARRAY 3 :element-type \"int\"))) (ASET a 0 1)  (ASET a 0 false)  (ASET a 1 true) a) ",
                 Utils.array(0, 1, 0) , true, null, null, p},
                {"(LET ((a (MAKE-ARRAY 3 :element-type \"Integer\"))) (ASET a 0 1)  (ASET a 0 false)  (ASET a 1 true) a) ",
                 new Integer[] {0, 1, null} , true, null, null, p},

                {"(SEQUENCEP ())", true , true, null, null, p},
                {"(SEQUENCEP (MAKE-ARRAY 2))", true , true, null, null, p},
                {"(SEQUENCEP \"\")", true , true, null, null, p},
                {"(SEQUENCEP NIL)", false , false, null, null, p},
                {"(SEQUENCEP 1)", false , false, null, null, p},

                //{"(MAKE-ARRAY 1 \"int\")", new int[3], true, null, null, p},
                // THREADS
                {"(LET ((a (MAKE-ARRAY 1)) (t1 (NEW-THREAD (LAMBDA () (ASET a 0 2)))))  (. t1 \"start()\") (. t1 \"join()\") (AREF a 0))",
                 2, true, null, null, p},
                 {"(LET ((a 1) (t1 (NEW-THREAD (LAMBDA () (SETV a 2)))))  (. t1 \"start()\") (. t1 \"join()\") a)",
                        2, true, null, null, p}
            });
    }

    @Arguments(spec={ArgSpec.ARG_LAZY,"a","b","c"})
    public  static class LAZYAND extends Funcs.FuncExp {
        @Override
        public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
            Object andVal = true;
            for (int i =0; i < 3; i++) {
                andVal = eargs.get(i, backtrace);
                if (!Utils.asBoolean(andVal)) {
                    break;
                }
            }
            return andVal;
        }
    }

    
    
    @Test
    public void testExprs() throws Throwable {
        try {
            System.out.println("\n\n TEST #: "+testNum);
            Compiler compiler =new Compiler();
            compiler.setParser(this.parser);
            compiler.addBuiltIn("LAZYAND", LAZYAND.class);
            String inputName = "TEST #"+testNum;
            testNum++;
            System.out.println("Parser: "+compiler.getParser());
            System.out.println("IN:  "+in);
            ASTNList parsed = parser.parse(new ParseCtx(inputName),in);
            System.out.println("AST: " + parsed);

            int i = 0;
            for (ASTN exprObj : parsed) {
                i++;
                System.out.println("EXPRESSION "+i+" OF "+parsed.size());
                Assert.assertEquals(1, parsed.size());
                ICompiled exp = compiler.compile(exprObj);
                Compiler.ICtx ctx = (null!=this.vars ? compiler.newCtx(this.vars)
                                     : compiler.newCtx());
                Object expVal = exp.evaluate(compiler.newBacktrace(),ctx);
                System.out.println("TYP: " + ((null == expVal) ? null :expVal.getClass()));
                System.out.println("OUT: " + Utils.asString(expVal));
                System.out.println("LOG: " + Utils.asBoolean(expVal));
                if (null!=expExc) {
                    Assert.fail("expected exception containing "+expExc);
                }
                System.out.println("XPC: " + Utils.asString(result));
                System.out.println("LXPC:" + logResult);
                if (i==parsed.size()) {
                    if ((result instanceof Double) && (expVal instanceof Double)) {
                        Assert.assertEquals((double)result, (double)expVal, 0.0000000000001);
                    } else if  ((result instanceof Float) && (expVal instanceof Float)) {
                        Assert.assertEquals((float)result, (float)expVal, 0.000001);
                    } else if (null != result &&
                               null != expVal &&
                               expVal.getClass().isArray() &&
                               result.getClass().isArray()) {
                        Assert.assertTrue(Utils.arraysDeepEquals(expVal , result));
                    } else if (((result instanceof Pattern) &&
                                (expVal instanceof Pattern)) ||
                               ((result instanceof Matcher) &&
                                (expVal instanceof Matcher)))
                        {
                            // FIXME: string repr. works, but how to do it properly?
                            Assert.assertEquals(result.toString(),expVal.toString());
                        } else {
                        Assert.assertEquals(result, Utils.asObject(expVal));
                    }
                    Assert.assertEquals(Utils.asBoolean(expVal), logResult);
                }
            }
        } catch (ExecutionException ex) {
            if (null!= expExc) {
                if (areEqual(ex,expExc)) {
                    System.err.println("Got expected exception: "+ex);
                } else {
                    System.err.println("Expected exception: "+expExc+", but got "+ex);
                    ex.printStackTrace(System.err);
                    throw ex;
                }
            } else {
                ex.printStackTrace(System.err);
                throw ex;
            }
        } catch (Throwable ex) {
            ex.printStackTrace(System.err);
            throw ex;
        }
    }

    public boolean areEqual(Throwable a, Throwable b) {
        Assert.assertNotNull(a);
        Assert.assertNotNull(b);
        if (! (a.getClass().equals(b.getClass()))) {
            return false;
        }
        String msgA = a.getMessage();
        String msgB = b.getMessage();
        if (!((null==msgA) ? null == msgB :
              msgA.equals(msgB))) {
            return false;
        }
        Throwable causeA = a.getCause();
        Throwable causeB = b.getCause();
        if (null==causeA && null==causeB) {
            return true;
        }
        return areEqual(causeA, causeB);
    }
    
}
