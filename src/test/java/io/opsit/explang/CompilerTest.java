package io.opsit.explang;

import static io.opsit.explang.Utils.list;
import static io.opsit.explang.Utils.map;
import static io.opsit.explang.Utils.set;

import io.opsit.explang.Compiler.Eargs;
import io.opsit.explang.parser.lisp.LispParser;
import io.opsit.explang.parser.sexp.SexpParser;
import io.opsit.version.Version;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class CompilerTest extends AbstractTest {
  static int testNum = 0;

  Object result;
  boolean logResult;
  IParser parser;
  String in;
  Throwable expExc;
  Map<String, Object> vars;

  public CompilerTest(
      String in,
      Object result,
      boolean logResult,
      Throwable expExc,
      Map<String, Object> vars,
      IParser parser) {
    this.in = in;
    this.result = result;
    this.expExc = expExc;
    this.logResult = logResult;
    this.vars = vars;
    this.parser = parser;
    this.isVerbose = "true".equalsIgnoreCase(System.getenv("EXPLANG_VERBOSE_TEST"));
  }

  @Parameters
  public static Collection<Object[]> data() throws Exception {
    Collection<Object[]> c = new ArrayList<Object[]>();
    IParser p;
    p = new SexpParser();
    c.addAll(data(p));
    p = new LispParser();
    c.addAll(data(p));
    return c;
  }

  @SuppressWarnings("serial")
  public static Collection<Object[]> data(IParser p) throws Exception {
    TestBean testBean = new TestBean();
    Map<String, Object> beanVars = new HashMap<String, Object>();
    beanVars.put("testbean", testBean);
    String quine =
        "((LAMBDA (X) (LIST X (LIST (QUOTE QUOTE) X))) (QUOTE (LAMBDA (X) (LIST X (LIST (QUOTE"
            + " QUOTE) X)))))";
    @SuppressWarnings("unchecked")
    List<Object> parsedQuine =
        (List<Object>)
            ((List<Object>) Utils.unAstnize((new SexpParser()).parse(new ParseCtx("quine"), quine)))
                .get(0);

    Object[][] tests =
        (new Object[][] {
          // empty list
          {"()", new ArrayList<Object>(0), false, null, null, p},
          // atomic values
          {"1", 1, true, null, null, p},
          {"1.0", 1.0, true, null, null, p},
          {"1.0f", 1.0f, true, null, null, p},
          {"1.0e4", 1.0e4, true, null, null, p},
          {"1.0E4", 1.0E4, true, null, null, p},
          {"1.0e-4", 1.0e-4, true, null, null, p},
          {"1.0E-4", 1.0E-4, true, null, null, p},
          {"1.0e-4f", 1.0e-4f, true, null, null, p},
          {"1.0E-4f", 1.0E-4f, true, null, null, p},
          {"\"foo\"", "foo", true, null, null, p},
          {"\"\"", "", false, null, null, p},
          {"TRUE", Boolean.TRUE, true, null, null, p},
          {"FALSE", Boolean.FALSE, false, null, null, p},
          {"NULL", null, false, null, null, p},

          // arithmetics
          {"(+)", 0, false, null, null, p},
          {"(+ 1b 2b)", (byte) 3, true, null, null, p},
          {"(+ 1s 2s)", (short) 3, true, null, null, p},
          {"(+ 1s 2)", 3, true, null, null, p},
          {"(+ 1 2 3 4)", 10, true, null, null, p},
          {"(+ 1 (+ 2 3)  4)", 10, true, null, null, p},
          {"(/ 2)", 0, false, null, null, p},
          {"(/ 2b)", (byte) 0, false, null, null, p},
          {"(/ 2.0)", 0.5, true, null, null, p},
          {"(/ 20 10.0)", 2.0, true, null, null, p},
          {"(/ 20 10.0f)", (float) 2.0, true, null, null, p},
          {"(/ 1024 2 4 )", 128, true, null, null, p},
          {"(*)", 1, true, null, null, p},
          {"(* 8)", 8, true, null, null, p},
          {"(* 3b 2b)", (byte) 6, true, null, null, p},
          {"(* 3s 2s)", (short) 6, true, null, null, p},
          {"(* 3s 2)", 6, true, null, null, p},
          {"(* 1 2 3 (* 2 2) 5)", 120, true, null, null, p},
          {"(- 1b)", (byte) -1, true, null, null, p},
          {"(- 1s)", (short) -1, true, null, null, p},
          {"(- 1)", -1, true, null, null, p},
          {"(- 1L)", -1L, true, null, null, p},
          {"(- 10 1 2 3 )", 4, true, null, null, p},
          {"(- 10 1 2 3L )", 4L, true, null, null, p},
          {"(% 100 10)", 0, false, null, null, p},
          {"(% 10 3)", 1, true, null, null, p},
          {"(REM -1 5)", -1, true, null, null, p},
          {"(MOD -1 5)", 4, true, null, null, p},
          {"(MOD 13 4)", 1, true, null, null, p},
          {"(REM 13 4)", 1, true, null, null, p},
          {"(MOD -13 4)", 3, true, null, null, p},
          {"(REM -13 4)", -1, true, null, null, p},
          {"(MOD 13 -4)", -3, true, null, null, p},
          {"(REM 13 -4)", 1, true, null, null, p},
          {"(MOD -13 -4)", -1, true, null, null, p},
          {"(REM -13 -4)", -1, true, null, null, p},
          {"(REM -1 5)", -1, true, null, null, p},
          {"(MOD -1 5)", 4, true, null, null, p},
          {"(MOD 13.4 1)", 0.4, true, null, null, p},
          {"(REM 13.4 1)", 0.4, true, null, null, p},
          {"(MOD  -13.4 1)", 0.6, true, null, null, p},
          {"(REM  -13.4 1)", -0.4, true, null, null, p},
          {"(MOD  -13.4 1D)", 0.6, true, null, null, p},
          {"(REM  -13.4 1D)", -0.4, true, null, null, p},
          {"(MOD  -13.4F 1F)", (float) 0.6, true, null, null, p},
          {"(REM  -13.4F 1F)", (float) -0.4, true, null, null, p},
          {"(MOD  -13.4F 1)", (float) 0.6, true, null, null, p},
          {"(REM  -13.4F 1)", (float) -0.4, true, null, null, p},
          {"(MOD  -13.4F 1.1F)", (float) 0.9, true, null, null, p},
          {"(REM  -13.4F 1.1F)", (float) -0.2, true, null, null, p},
          {"(MOD  -13.4 1.1)", 0.9, true, null, null, p},
          {"(REM  -13.4 1.1)", -0.2, true, null, null, p},
          // promotion
          {"(+ 1 2)", Integer.valueOf(3), true, null, null, p},
          {"(+ 1 2.0)", Double.valueOf(3.0), true, null, null, p},
          {"(+ 1.0 2.0)", Double.valueOf(3.0), true, null, null, p},
          {"(+ 1.0 2)", Double.valueOf(3.0), true, null, null, p},
          {"(- 1 2.0)", Double.valueOf(-1.0), true, null, null, p},
          {"(- 1.0 2.0)", Double.valueOf(-1.0), true, null, null, p},
          {"(- 1.0 2)", Double.valueOf(-1.0), true, null, null, p},
          {"(+ 1 2.0f)", Float.valueOf(3.0f), true, null, null, p},
          {"(+ 1.0f 2.0 0)", Double.valueOf(3.0), true, null, null, p},
          {"(+ 1.0f \"1.1f\")", Float.valueOf(2.1f), true, null, null, p},
          {"(OR 0 3L 0)", Long.valueOf(3), true, null, null, p},
          {"(OR 0 \"\" ())", false, false, null, null, p},
          {"(OR)", false, false, null, null, p},
          {
            "(LET ((a NIL)) (LIST (OR (SETV a 0) (SETV a 2) (SETV a 3) ) a))",
            list(2, 2),
            true,
            null,
            null,
            p
          },
          {"(AND)", true, true, null, null, p},
          {"(AND 1 2 3L)", Long.valueOf(3), true, null, null, p},
          {"(AND 1 2 0L)", Long.valueOf(0), false, null, null, p},
          {
            "(PROGN  (SETQ CNTR 0)  (DEFUN GETARG () (SETQ CNTR (+ CNTR 1))) (SETQ ACC ()) "
                + " (FOREACH (X (LIST 1 2 3 ))           (SETQ CNTR 0)           (APPEND! ACC      "
                + "         (LIST (AND (< (GETARG) 2) (< (GETARG) 2) (< (GETARG) 2) (< (GETARG) 2))"
                + "                     CNTR)))  ACC )",
            list(false, 2, false, 2, false, 2),
            true,
            null,
            null,
            p
          },
          {
            "(MAP (LAMBDA (X) (AND (> X 25))) (LIST  25 28))",
            list(false, true),
            true,
            null,
            null,
            p
          },
          {
            "(LET ((a NIL)) (LIST (AND (SETV a 1) (SETV a 0) (SETV a 3) ) a))",
            list(0, 0),
            true,
            null,
            null,
            p
          },
          {"(NOT 0 )", true, true, null, null, p},
          {"(NOT 0b )", true, true, null, null, p},
          {"(NOT 0s )", true, true, null, null, p},
          {"(NOT 0.0f )", true, true, null, null, p},
          {"(NOT 0.0 )", true, true, null, null, p},
          {"(NOT 0.0001 )", false, false, null, null, p},
          {"(NOT -0.0001 )", false, false, null, null, p},
          {"(NOT -0.0001f )", false, false, null, null, p},
          {"(NOT ())", true, true, null, null, p},
          {"(NOT 1)", false, false, null, null, p},
          {"(NOT 1)", false, false, null, null, p},
          {"(= 1)", true, true, null, null, p},
          {"(= 1 1 1)", true, true, null, null, p},
          {"(= 0  \"0\")", true, true, null, null, p},
          {"(= 1.1  \"1.1\")", true, true, null, null, p},
          {"(= 1b  \"1b\")", true, true, null, null, p},
          {"(< 0 1 2)", true, true, null, null, p},
          {"(< 0 1 2 2)", false, false, null, null, p},
          {"(<= 0 1 2)", true, true, null, null, p},
          {"(<= 0 1 2 2 3)", true, true, null, null, p},
          {"(> 2 1 0)", true, true, null, null, p},
          {"(> 4  2 2 -1)", false, false, null, null, p},
          {"(>= 2 1 0 0)", true, true, null, null, p},
          {"(= 0.0 0 0b 0.0f 0.s 0L)", true, true, null, null, p},
          {"(= 0 0.1)", false, false, null, null, p},
          {"(> 0.1 0.0)", true, true, null, null, p},
          {"(> 0.1 0)", true, true, null, null, p},
          // this is not equal in Java
          // { "(= 0.1 0.1F)", true, true, null, null, p},
          {"(= 3.0 3.0F 3L 3 3S 3B)", true, true, null, null, p},

          // object identity
          {"(=== 12345678 12345678)", false, false, null, null, p},
          {"(LET ((A 1) (B A)) (=== A B))", true, true, null, null, p},
          {"(=== NIL  NIL)", true, true, null, null, p},
          // java equal
          {"(EQUAL 1 1)", true, true, null, null, p},
          {"(EQUAL \"a\" \"a\" )", true, true, null, null, p},
          {"(EQUAL NIL NULL )", true, true, null, null, p},
          {"(EQUAL (SYMBOL \"A\") (QUOTE A))", true, true, null, null, p},
          {"(== 1 1b)", true, true, null, null, p},
          {"(== 1 1)", true, true, null, null, p},
          {"(== 1 1.0)", true, true, null, null, p},
          {"(== 12345678 12345678)", true, true, null, null, p},
          {"(== 12345678 12345678L)", true, true, null, null, p},
          {"(== 12345678L 12345678)", true, true, null, null, p},
          {"(== 12345678 12345678.0)", true, true, null, null, p},
          {"(== 12345678.0 12345678.0f)", true, true, null, null, p},
          {"(== (LIST 1) (LIST 1))", true, true, null, null, p},
          {"(== (LIST 1.0) (LIST 1))", true, true, null, null, p},
          {"(== (LIST 1.0 (LIST 2)) (LIST 1 (LIST 2)))", true, true, null, null, p},
          {"(== (LIST 1.0 (LIST 2.0)) (LIST 1 (LIST 2)))", true, true, null, null, p},
          {"(== (LIST 1.0 (LIST 2.0)) (LIST 1 (LIST 2)))", true, true, null, null, p},
          {"(== (LIST 1.0 (LIST 2.0)) (LIST 1 (LIST 2.1)))", false, false, null, null, p},
          {"(== (LIST NIL NIL ) (LIST 1 2))", false, false, null, null, p},
          {"(== (LIST 1 2) (LIST NIL NIL))", false, false, null, null, p},
          // FIXME: maps enum values set

          {"(== (HASHMAP 1 1000.0) (HASHMAP 1 1000.l))", true, true, null, null, p},
          {"(== (HASHMAP 2 1000) (HASHMAP 2 1000.0))", true, true, null, null, p},
          {"(== (HASHMAP 2 1000) (HASHMAP 2 11))", false, false, null, null, p},
          {"(== (HASHMAP 2 1000) (HASHMAP 2 10.1))", false, false, null, null, p},
          {"(== (HASHMAP 3 (LIST 1)) (HASHMAP 3 (LIST 1.0)))", true, true, null, null, p},
          {
            "(LET ((A (MAKE-ARRAY :size 2))) (PUT! A 0 1000) (PUT! A 1 2000) (== (LIST 1000 2000) A))",
            true,
            true,
            null,
            null,
            p
          },
          {
            "(LET ((A (MAKE-ARRAY :size 2))) (PUT! A 0 1000.0) (PUT! A 1 2000) (== (LIST 1000 2000.0)"
                + " A))",
            true,
            true,
            null,
            null,
            p
          },
          {"(PUT! (HASHMAP) 1 2)", null, false, null, null, p},
          {"(PUT! (HASHMAP 1 1) 1 2)", 1, true, null, null, p},
          {"(LET ((m (HASHMAP))) (PUT! m 1 2) m)", map(1,2), true, null, null, p},
          {"(LET ((m (HASHMAP))) (PUT! m \"f\" 1) m)", map("f",1), true, null, null, p},
          
          {"(LET ((m (LIST))) (PUT! m 0 1) m)", list(1), true, null, null, p},
          {"(LET ((m (LIST 1))) (PUT! m 0 2) m)", list(2), true, null, null, p},
          {"(LET ((m (LIST 1))) (PUT! m 1 2) m)", list(1,2), true, null, null, p},
          {"(PUT! (LIST) 0 2)", null, false, null, null, p},
          {"(PUT! (LIST 1) 0 2)", 1, true, null, null, p},
          {"(PUT! (LIST 1) 1 2)", null, false, null, null, p},
          
          {"(PUT! (STRING-BUFFER) 0 33)", null, false, null, null, p},
          {"(PUT! (STRING-BUILDER) 0 33)", null, false, null, null, p},
          {"(PUT! (STRING-BUFFER \"ab\") 0 33)", 'a', true, null, null, p},
          {"(PUT! (STRING-BUILDER \"ab\") 0 33)", 'a', true, null, null, p},

          {"(LET ((m (STRING-BUFFER))) (PUT! m 0 33) m)", new StringBuffer("!"), true, null, null, p},
          {"(LET ((m (STRING-BUILDER))) (PUT! m 0 33) m)", new StringBuilder("!"), true, null, null, p},

          {"(PUT! (MAKE-ARRAY NIL) 0 2)", null, false, null, null, p},
          {"(PUT! (MAKE-ARRAY 1) 0 2)",1, true, null, null, p},
          {"(PUT! (MAKE-ARRAY NIL :elementType \"int\") 0 2)", null, false, null, null, p},
          {"(PUT! (MAKE-ARRAY 1 :elementType \"int\") 0 2)",1, true, null, null, p},
         
          {"(LET ((a (MAKE-ARRAY 1))) (PUT! a 0 2) a)", Utils.arrayOfObjects(2), true, null, null, p},
          {"(LET ((a (MAKE-ARRAY :elementType \"int\" 1))) (PUT! a 0 2) a)", Utils.array(2), true, null, null, p},
          {"(PUT! (MAKE-ARRAY 1) 0 2)",1, true, null, null, p},

          {"(LET ((seq ())) (PUSH! seq 1) (PUSH! seq 2) seq)", list(1, 2), true, null, null, p},
          {"(LET ((seq (HASHSET))) (PUSH! seq 1) (PUSH! seq 2) seq)", set(1, 2), true, null, null, p},
          {"(LET ((seq (STRING-BUFFER))) (PUSH! seq (CHAR 65)) (PUSH! seq (CHAR 66)) seq)",
           new StringBuffer("AB"), true, null, null, p},
          {"(LET ((seq (STRING-BUILDER))) (PUSH! seq (CHAR 65)) (PUSH! seq (CHAR 66)) seq)",
           new StringBuilder("AB"), true, null, null, p},

          {"(LET ((seq \"\")) (PUSH (PUSH seq 65)  66))", "AB", true, null, null, p},
          {"(LET ((seq (STRING-BUFFER \"\"))) (LIST seq (PUSH (PUSH seq 65)  66)))",
           list(new StringBuffer(), new StringBuffer("AB")), true, null, null, p},
          {"(LET ((seq (STRING-BUILDER \"\"))) (LIST seq (PUSH (PUSH seq 65)  66)))",
           list(new StringBuilder(), new StringBuilder("AB")), true, null, null, p},
          {"(LET ((o ()) (t (PUSH (PUSH o 1) 2))) (LIST o t))",list(list(),list(1,2)), true, null, null, p},
          {"(LET ((o (.S \"java.util.Collections\" \"unmodifiableList\" (LIST ()))) (t (PUSH (PUSH o 1) 2))) (LIST o t))",
           list(list(),list(1,2)), true, null, null, p},
          {"(LET ((o (HASHSET)) (t (PUSH (PUSH o 1) 2))) (LIST o t))",list(set(),set(1,2)), true, null, null, p},
          {"(LET ((o (.S \"java.util.Collections\" \"unmodifiableSet\" (LIST (HASHSET)))) (t (PUSH (PUSH o 1) 2))) (LIST o t))",
           list(set(),set(1,2)), true, null, null, p},

          // POP!
          {"(POP! NIL)",
           null, false, new ExecutionException(null,  "POP! from an empty sequence"), null, p},

          {"(LET ((L (LIST 10 20 30))) (LIST (POP! L) (POP! L) (POP! L) L))",
           list(30,20,10,list()), true, null, null, p},
          {"(POP! (LIST))",
           null, false, new ExecutionException(null,  "POP! from an empty sequence"), null, p},
          

          {"(LET ((L (STRING-BUFFER \"ABC\"))) (LIST (POP! L) (POP! L) (POP! L) L))",
           list('C','B','A', new StringBuffer()), true, null, null, p},
          {"(POP! (STRING-BUFFER))",
           null, false, new ExecutionException(null,  "POP! from an empty sequence"), null, p},


          {"(LET ((L (STRING-BUILDER \"ABC\"))) (LIST (POP! L) (POP! L) (POP! L)  L))",
           list('C','B','A', new StringBuilder()), true, null, null, p},
          {"(POP! (STRING-BUILDER))",
           null, false, new ExecutionException(null,  "POP! from an empty sequence"), null, p},

          
          {"(LET ((L (HASHMAP 0 10 1 20 2 30))) (LIST (POP! L) (POP! L) (POP! L)  L))",
           list(30,20,10, map()), true, null, null, p},
          {"(POP! (HASHMAP))",
           null, false, new ExecutionException(null,  "POP! from an empty sequence"), null, p},


          {"(LET ((L (HASHSET 10 20 30))) (LIST (HASHSET (POP! L) (POP! L) (POP! L))  L))",
           list(set(30,20,10), set()), true, null, null, p},
          {"(POP! (HASHSET))",
           null, false, new ExecutionException(null,  "POP! from an empty sequence"), null, p},

          // POP
          {"(POP NIL)",
           null, false, new ExecutionException(null,  "POP from an empty sequence"), null, p},

          {"(POP (LIST))",
           null, false, new ExecutionException(null,  "POP from an empty sequence"), null, p},

          {"(POP (HASHSET))",
           null, false, new ExecutionException(null,  "POP from an empty sequence"), null, p},

          {"(POP (HASHMAP))",
           null, false, new ExecutionException(null,  "POP from an empty sequence"), null, p},

          {"(POP (STRING-BUFFER))",
           null, false, new ExecutionException(null,  "POP from an empty sequence"), null, p},

          {"(POP (STRING-BUILDER))",
           null, false, new ExecutionException(null,  "POP from an empty sequence"), null, p},

                    {"(POP \"\")",
           null, false, new ExecutionException(null,  "POP from an empty sequence"), null, p},

          {"(LET ((L (LIST 10 20 30)))"
           + " (LIST (SETF (LIST V1 L1) (POP L)) "
           + "       (SETF (LIST V2 L2) (POP L1))"
           + "       (SETF (LIST V3 L3) (POP L2))))",
           list(list(30,list(10,20)), list(20,list(10)), list(10,list())), true, null, null, p},

          {"(LET ((L (STRING-BUFFER \"ABC\")))"
           + " (LIST (SETF (LIST V1 L1) (POP L)) "
           + "       (SETF (LIST V2 L2) (POP L1))"
           + "       (SETF (LIST V3 L3) (POP L2))))",
           list(list('C',new StringBuffer("AB")),
                list('B',new StringBuffer("A")),
                list('A',new StringBuffer(""))), true, null, null, p},

          {"(LET ((L (STRING-BUILDER \"ABC\")))"
           + " (LIST (SETF (LIST V1 L1) (POP L)) "
           + "       (SETF (LIST V2 L2) (POP L1))"
           + "       (SETF (LIST V3 L3) (POP L2))))",
           list(list('C',new StringBuilder("AB")),
                list('B',new StringBuilder("A")),
                list('A',new StringBuilder(""))), true, null, null, p},

          {"(LET ((L \"ABC\"))"
           + " (LIST (SETF (LIST V1 L1) (POP L)) "
           + "       (SETF (LIST V2 L2) (POP L1))"
           + "       (SETF (LIST V3 L3) (POP L2))))",
           list(list('C',"AB"),
                list('B',"A"),
                list('A',"")), true, null, null, p},

          {"(LET ((L (HASHMAP 0 10 1 20 2 30)))"
           + " (LIST (SETF (LIST V1 L1) (POP L)) "
           + "       (SETF (LIST V2 L2) (POP L1))"
           + "       (SETF (LIST V3 L3) (POP L2))))",
           list(list(30, map(0,10,1,20)),
                list(20, map(0,10)),
                list(10, map())), true, null, null, p},


          {"(LET ((L (HASHSET 1)))"
            + "(LIST L  (POP L)))",
            list(set(1), list(1, set())), true, null, null, p},

          {"(LET ((L (.S \"java.util.Collections\" \"unmodifiableList\" (LIST (LIST 10 20 30)))))"
           + " (LIST (SETF (LIST V1 L1) (POP L)) "
           + "       (SETF (LIST V2 L2) (POP L1))"
           + "       (SETF (LIST V3 L3) (POP L2))))",
           list(list(30,list(10,20)), list(20,list(10)), list(10,list())), true, null, null, p},

          {"(LET ((L (.S \"java.util.Collections\" \"unmodifiableMap\" (LIST (HASHMAP 0 10 1 20 2 30)))))"
           + " (LIST (SETF (LIST V1 L1) (POP L)) "
           + "       (SETF (LIST V2 L2) (POP L1))"
           + "       (SETF (LIST V3 L3) (POP L2))))",
           list(list(30, map(0,10,1,20)),
                list(20, map(0,10)),
                list(10, map())), true, null, null, p},

          {"(LET ((L (.S \"java.util.Collections\" \"unmodifiableSet\" (LIST (HASHSET 1)))))"
            + "(LIST L  (POP L)))",
            list(set(1), list(1, set())), true, null, null, p},

          

          {"(LET ((L ())) (INSERT! L 0 10) (INSERT! L 0 11))", list(11, 10), true, null, null, p},
          {"(LET ((L ())) (INSERT! L 0 10) (INSERT! L 1 11))", list(10, 11), true, null, null, p},

          {"(LET ((L (STRING-BUILDER \"A\"))) (INSERT! L 0 (CHAR 66)) (INSERT! L 0 (CHAR 67)))",
           new StringBuilder("CBA"), true, null, null, p},
          {"(LET ((L (STRING-BUILDER \"A\"))) (INSERT! L 1 (CHAR 66)) (INSERT! L 2 (CHAR 67)))",
           new StringBuilder("ABC"), true, null, null, p},

          {"(LET ((L (STRING-BUFFER \"A\"))) (INSERT! L 0 (CHAR 66)) (INSERT! L 0 (CHAR 67)))",
           new StringBuffer("CBA"), true, null, null, p},
          {"(LET ((L (STRING-BUFFER \"A\"))) (INSERT! L 1 (CHAR 66)) (INSERT! L 2 (CHAR 67)))",
           new StringBuffer("ABC"), true, null, null, p},

          
          {"(LET ((L ()) (T (INSERT (INSERT L 0 10) 0 11))) (LIST L T))", list(list(), list(11, 10)), true, null, null, p},
          {"(LET ((L ()) (T (INSERT (INSERT L 0 10) 1 11))) (LIST L T))", list(list(), list(10, 11)), true, null, null, p},

          {"(LET ((L (STRING-BUILDER \"A\")) (T (INSERT (INSERT L 0 (CHAR 66))  0 (CHAR 67)))) (IF (=== L T) false T))",
           new StringBuilder("CBA"), true, null, null, p},
          {"(LET ((L (STRING-BUILDER \"A\")) (T (INSERT (INSERT L 1 (CHAR 66))  2 (CHAR 67)))) (IF (=== L T) false T))",
           new StringBuilder("ABC"), true, null, null, p},

          {"(LET ((L (STRING-BUFFER \"A\")) (T (INSERT (INSERT L 0 (CHAR 66))  0 (CHAR 67)))) (IF (=== L T) false T))",
           new StringBuffer("CBA"), true, null, null, p},
          {"(LET ((L (STRING-BUFFER \"A\")) (T (INSERT (INSERT L 1 (CHAR 66))  2 (CHAR 67)))) (IF (=== L T) false T))",
           new StringBuffer("ABC"), true, null, null, p},

          {"(LET ((L \"A\") (T (INSERT (INSERT L 0 (CHAR 66))  0 (CHAR 67)))) (IF (=== L T) false T))",
           "CBA", true, null, null, p},
          {"(LET ((L \"A\") (T (INSERT (INSERT L 1 (CHAR 66))  2 (CHAR 67)))) (IF (=== L T) false T))",
           "ABC", true, null, null, p},
          {"(LET ((o (HASHMAP 1 2 )) (t (INSERT o 3 4))) (LIST o t ))",
           list(map(1,2), map(1,2,3,4)), true, null, null, p},
          {"(LET ((o (.S \"java.util.Collections\" \"unmodifiableMap\" (LIST (HASHMAP 1 2 )))) (t (INSERT o 3 4))) (LIST o t ))",
           list(map(1,2), map(1,2,3,4)), true, null, null, p},

          {"(LET ((L NIL) (R (DELETE! L 0))) (LIST L R))",
           list(null, null), true, null, null, p},
          {"(LET ((L (LIST 10 20 30)) (R (DELETE! L 0))) (LIST L R))",
           list(list(20,30), 10), true, null, null, p},
          {"(LET ((L (LIST 10 20 30))) (DELETE! L 2) L)",
           list(10,20), true, null, null, p},
          {"(LET ((L (LIST 10 20 30)) (R (DELETE! L 3))) (LIST L R))",
           list(list(10,20,30), null), true, null, null, p},

          {"(LET ((M (HASHMAP 1 10 2 20)) (R (DELETE! M 1))) (LIST M R))",
           list(map(2,20),10), true, null, null, p},
          {"(LET ((M (HASHMAP 1 10 2 20)) (R (DELETE! M 3))) (LIST M R))",
           list(map(1,10,2,20), null), true, null, null, p},

          {"(LET ((S (HASHSET 10 20 30)) (R (DELETE! S 20))) (LIST S R))",
           list(set(10,30), 20), true, null, null, p},
          {"(LET ((S (HASHSET 10 20 30)) (R (DELETE! S 200))) (LIST S R))",
           list(set(10,20,30),null), true, null, null, p},

          {"(LET ((S (STRING-BUILDER \"ABC\")) (R (DELETE! S 0))) (LIST S R))",
           list(new StringBuilder("BC"), 'A'), true, null, null, p},
          {"(LET ((S (STRING-BUILDER \"ABC\")) (R (DELETE! S 200))) (LIST S R))",
           list(new StringBuilder("ABC"),null), true, null, null, p},
          
          {"(LET ((S (STRING-BUFFER \"ABC\")) (R (DELETE! S 0))) (LIST S R))",
           list(new StringBuffer("BC"), 'A'), true, null, null, p},
          {"(LET ((S (STRING-BUFFER \"ABC\")) (R (DELETE! S 200))) (LIST S R))",
           list(new StringBuffer("ABC"),null), true, null, null, p},


          {"(LET ((L NIL) (R (DELETE L 0))) (LIST L R))",
           list(null, null), true, null, null, p},
          {"(LET ((L (LIST 10 20 30)) (R (DELETE L 0))) (LIST L R))",
           list(list(10,20,30),list(20,30)), true, null, null, p},
          {"(LET ((L (LIST 10 20 30)) (R (DELETE L 2))) (LIST L R))",
           list(list(10,20,30),list(10,20)), true, null, null, p},
          {"(LET ((L (LIST 10 20 30)) (R (DELETE L 3))) (LIST L R))",
           list(list(10,20,30), list(10,20,30)), true, null, null, p},

          {"(LET ((M (HASHMAP 1 10 2 20)) (R (DELETE M 1))) (LIST M R))",
           list(map(1,10,2,20), map(2,20)), true, null, null, p},
          {"(LET ((M (HASHMAP 1 10 2 20)) (R (DELETE M 3))) (LIST M R))",
           list(map(1,10,2,20), map(1,10,2,20)), true, null, null, p},

          {"(LET ((S (HASHSET 10 20 30)) (R (DELETE S 20))) (LIST S R))",
           list(set(10, 20, 30), set(10,30)), true, null, null, p},
          {"(LET ((S (HASHSET 10 20 30)) (R (DELETE S 200))) (LIST S R))",
           list(set(10, 20,30), set(10,20,30)), true, null, null, p},

          {"(LET ((S (STRING-BUILDER \"ABC\")) (R (DELETE S 0))) (LIST S R))",
           list(new StringBuilder("ABC"), new StringBuilder("BC")), true, null, null, p},
          {"(LET ((S (STRING-BUILDER \"ABC\")) (R (DELETE S 200))) (LIST S R))",
           list(new StringBuilder("ABC"), new StringBuilder("ABC")), true, null, null, p},
          
          {"(LET ((S (STRING-BUFFER \"ABC\")) (R (DELETE S 0))) (LIST S R))",
           list(new StringBuffer("ABC"), new StringBuffer("BC")), true, null, null, p},
          {"(LET ((S (STRING-BUFFER \"ABC\")) (R (DELETE S 200))) (LIST S R))",
           list(new StringBuffer("ABC"), new StringBuffer("ABC")), true, null, null, p},

          {"(LET ((S \"ABC\") (R (DELETE S 0))) (LIST S R))",
           list("ABC", "BC"), true, null, null, p},
          {"(LET ((S  \"ABC\") (R (DELETE S 200))) (LIST S R))",
           list("ABC", "ABC"), true, null, null, p},


          {"(LET ((L (.S \"java.util.Collections\" \"unmodifiableList\" (LIST (LIST 1 2 3))))"
           + "    (R (DELETE L 1))) (LIST L R))",
           list(list(1,2,3),list(1,3)), true, null, null, p},
          {"(LET ((L (.S \"java.util.Collections\" \"unmodifiableMap\" (LIST (HASHMAP 1 2 3 4))))"
           + "    (R (DELETE L 1))) (LIST L R))",
           list(map(1,2,3,4),map(3,4)), true, null, null, p},
          {"(LET ((L (.S \"java.util.Collections\" \"unmodifiableSet\" (LIST (HASHSET 1 2 3 4))))"
           + "    (R (DELETE L 2))) (LIST L R))",
           list(set(1,2,3,4),set(1,3,4)), true, null, null, p},
          
          
          {
            "(== (HASHSET 1 2 3 \"foo\" null) (HASHSET null \"foo\" 1 2 3))",
            true,
            true,
            null,
            null,
            p
          },
          {
            "(== (HASHSET 1 2 3.0 \"foo\" null) (HASHSET null \"foo\" 1 2 3))",
            false,
            false,
            null,
            null,
            p
          },
          {"(== (LIST 1 (HASHSET 1 2 3)) (LIST 1 (HASHSET 1 2 3)))", true, true, null, null, p},
          {"(== \"UNO\" (.S \"io.opsit.explang.TestEnum\" \"UNO\"))", true, true, null, null, p},
          {"(== \"DUO\" (.S \"io.opsit.explang.TestEnum\" \"UNO\"))", false, false, null, null, p},
          {"(== (.S \"io.opsit.explang.TestEnum\" \"UNO\") \"UNO\")", true, true, null, null, p},
          {"(== (.S \"io.opsit.explang.TestEnum\" \"UNO\") \"DUO\")", false, false, null, null, p},
          {"(== \"git\" \"github\")", false, false, null, null, p},
          {"(== \"github\" \"git\")", false, false, null, null, p},
          {"(== \"git\" \"git\")", true, true, null, null, p},
          {"(== \"\" \"git\")", false, false, null, null, p},
          {"(== \"git\" \"\")", false, false, null, null, p},
          {"(== \"\" \"\")", true, true, null, null, p},

          // LOADR
          {
            "(PROGN (SETV *loaded* NIL) (LIST (LOAD"
                + " \"./src/test/resources/io/opsit/explang/resloadtest.lsp\") *loaded*))",
            list(true, "some-result"),
            true,
            null,
            null,
            p
          },
          {
            "(PROGN (SETV *loaded* NIL) (LIST (LOADR \"/io/opsit/explang/resloadtest.lsp\")"
                + " *loaded*))",
            list(true, "some-result"),
            true,
            null,
            null,
            p
          },
          // EVAL
          {"(EVAL (QUOTE (+ 1 2 3 )))", 6, true, null, null, p},
          {"(FUNCTIONS-NAMES \"LOAD\")", list("LOAD", "LOADR"), true, null, null, p},
          {
            "(FUNCTIONS-NAMES (RE-PATTERN \"^LOADR?$\"))",
            list("LOAD", "LOADR"),
            true,
            null,
            null,
            p
          },
          // // degenerate LET cases:
          {"(LET ((v1)) v1)", null, false, null, null, p},
          {"(LET ((v1 1) (v2 (+ 1 2)))  (+ 1 2))", 3, true, null, null, p},
          {"(LET ((v1 1) (v2 (+ 1 2)))  4)", 4, true, null, null, p},
          // just return var value
          {"(LET ((v1 1)) v1)", 1, true, null, null, p},
          {"(LET ((v1 1)) (+ v1 v1))", 2, true, null, null, p},
          {"(LET ((v1 1)(v2 2)) (+ v1 v2))", 3, true, null, null, p},
          // enclosed let
          {"(LET ((v1 1)(v2 2)) (LET ((v3 3 )) (+ v1 v2 v3)))", 6, true, null, null, p},
          {"(LET ((v1 1)(v2 2)) (LET ((v1 4 )) (+ v1 v2)))", 6, true, null, null, p},

          // // // with expression as var value
          {"(LET ((v1 (+ 1 2))) v1)", 3, true, null, null, p},
          {"(LET ((v1 (+ 1 2))) (+ v1 v1))", 6, true, null, null, p},
          {"(LET ((v 1)) (LET ((v NIL)) v))", null, false, null, null, p},
          // var ref in var defs
          {"(LET ((v1 2) (v2 v1) (v3 (+ v1 v2))) (+ v1 v2 v3))", 8, true, null, null, p},
          // BOUNDP
          {"(BOUNDP (QUOTE MU))", false, false, null, null, p},
          {"(BOUNDP \"NU\")", false, false, null, null, p},
          {"(LET ((FOO 1)) (BOUNDP \"FOO\" \"bar\"))", false, false, null, null, p},
          {"(LET ((FOO 1)) (BOUNDP \"FOO\"))", true, true, null, null, p},
          {"(LET ((FOO 1)(BAR null)) (BOUNDP \"FOO\" \"BAR\"))", true, true, null, null, p},
          {
            "(LET ((FOO 1)) (LET ((BAR 2)) (BOUNDP (QUOTE BAR) (QUOTE FOO))))",
            true,
            true,
            null,
            null,
            p
          },

          //  DLET (destructuring-bind
          {"(DLET (a b c d) (LIST 1 2 3 4) (LIST d c b a))", list(4, 3, 2, 1), true, null, null, p},
          {"(DLET (a ) (LIST 1 2 3 4) a)", 1, true, null, null, p},
          {"(DLET (a b c d) (LIST 1 2 3 4) )", null, false, null, null, p},
          // QUOTE
          {"(QUOTE 1)", 1, true, null, null, p},
          {"(QUOTE NIL)", null, false, null, null, p},
          {"(QUOTE ())", list(), false, null, null, p},
          {
            "(QUOTE (FOO \"bar\" 1.0 (1)))",
            list(new Symbol("FOO"), "bar", 1.0, list(1)),
            true,
            null,
            null,
            p
          },
          // COND form
          {
            "(LET ((a 3)(b 3)) (COND ((= a b) (+ a b)) ((> a 0) (* a b)))) ", 6, true, null, null, p
          },
          {"(COND (1) (2 3))", 1, true, null, null, p},
          {"(COND)", null, false, null, null, p},
          {"(COND  (nil 2) (3 4))", 4, true, null, null, p},
          {"(COND  (1 2) (3 4))", 2, true, null, null, p},
          {
            "(LET ((a 2))  (COND ((= (SETV a 1) 1)) ((SETV a 3) (SETV a 5)) ((SETV a 4))) a)",
            1,
            true,
            null,
            null,
            p
          },
          // IF form
          {"(IF  1 2 3)", Integer.valueOf(2), true, null, null, p},
          {"(IF  True 2 3)", Integer.valueOf(2), true, null, null, p},
          {"(IF  False 2 3)", Integer.valueOf(3), true, null, null, p},
          {"(IF  0 2 3)", Integer.valueOf(3), true, null, null, p},
          {"(IF  0 2 3 4 5 6)", Integer.valueOf(6), true, null, null, p},
          {"(IF  1 2 )", Integer.valueOf(2), true, null, null, p},
          {"(IF  0 2 )", null, false, null, null, p},
          {"(IF  1 (IF 0 1 2 ))", Integer.valueOf(2), true, null, null, p},
          {"(PROGN 0 1 2)", Integer.valueOf(2), true, null, null, p},
          {"(PROGN (STR \"HELLO\") (STR \"WORLD\") (STR \"!!!\"))", "!!!", true, null, null, p},
          {"(PROGN)", null, false, null, null, p},
          {"(TRY)", null, false, null, null, p},
          {"(TRY 1 2 3)", 3, true, null, null, p},
          {"(TRY (STR 1) (+ 1 2))", 3, true, null, null, p},
          {"(TRY (STR 1) (+ 1 2) (FINALLY 4) )", 3, true, null, null, p},
          {
            "(TRY (THROW \"Exception\") "
                + "     (CATCH java.lang.Exception ex 3) "
                + "     (FINALLY 4))",
            3,
            true,
            null,
            null,
            p
          },
          {
            "(TRY (THROW \"Exception\") "
                + "     (CATCH Exception ex 3) "
                + "     (FINALLY 4))",
            3,
            true,
            null,
            null,
            p
          },
          {
            "(TRY (THROW (.N \"Exception\" (LIST \"fooo\"))) "
                + "     (CATCH java.lang.Exception ex (. ex \"getMessage()\"))"
                + "     (FINALLY 4))",
            "fooo",
            true,
            null,
            null,
            p
          },
          {
            "(TRY (THROW (.N \"Exception\" (LIST \"fooo\"))) "
                + "     (CATCH java.lang.RuntimeException ex \"oops\")"
                + "     (CATCH java.lang.Throwable ex (. ex \"getMessage()\"))"
                + "     (CATCH java.lang.Exception ex \"nope\")"
                + "     (FINALLY 4))",
            "fooo",
            true,
            null,
            null,
            p
          },
          {
            "(LET ((L (LIST))) "
                + "  (TRY (. L \"add\" (LIST 1) (LIST \"Object\" )) "
                + "     (FINALLY (. L \"add\" (LIST 2) (LIST \"Object\" )))) "
                + "  L)",
            list(1, 2),
            true,
            null,
            null,
            p
          },
          {
            "(LET ((L (LIST))) "
                + " (TRY "
                + "  (TRY (. L \"fooo()\" )"
                + "     (FINALLY (. L \"add\" (LIST 2) (LIST \"Object\" )))) "
                + "  (CATCH java.lang.RuntimeException ex ex))"
                + "  L)",
            list(2),
            true,
            null,
            null,
            p
          },
          // NON-LOCAL EXIT
          {
            "(PROGN "
                + " (DEFUN TESTRET (x) 1 (RETURN x) (SETV x 2)) "
                + " (TESTRET 3))"   ,
            3,
            true,
            null,
            null,
            p
          },
          {
            "(PROGN "
                + " (DEFUN TESTRET (x) (FOREACH (i (RANGE 1 10)) (IF (< i x) i (RETURN i))) NIL)  "
                + " (TESTRET 3))"   ,
            3,
            true,
            null,
            null,
            p
          },
          { //check nested scope
            "(LET ((i 0)) "
            + " (FOREACH (i (RANGE 1 10)) i)  "
            + " i)", 0, false,null,null,p
          },
          { //check nested scope
            "(LET ((i 0) (j 0)) "
            + " (WHILE (< i 10) (SETV i (+ i 1)) (SETL j i))  "
            + " (LIST i j))", list(10,0), true,null,null,p
          },

          {
            "(PROGN "
                + " (DEFUN TESTRET1 (x) (RETURN x) 1) "
                + " (DEFUN TESTRET2 (x) (+ (TESTRET1 x) 2)) "
                + " (TESTRET2 3))"   ,
            5,
            true,
            null,
            null,
            p
          },
          {
            "(PROGN "
                 + "(DEFUN TESTRET (x) "
                 + "(TRY \"aa\" (RETURN x) \"cc\" "
                 + "(CATCH java.lang.Exception ex (RETURN (+ x 1))) "
                 + "(FINALLY (RETURN (+ x 2)))))"
                 + "(TESTRET 3))",
            3,
            true,
            null,
            null,
            p
          },
          {
            "(PROGN "
                 + "(DEFUN TESTRET (x) "
                 + "(TRY \"aa\" (/ 1 0) (RETURN x) \"cc\" "
                 + "(CATCH java.lang.Exception ex (RETURN (+ x 1))) "
                 + "(FINALLY (RETURN (+ x 2)))))"
                 + "(TESTRET 3))",
            4,
            true,
            null,
            null,
            p
          },
          {
            "(PROGN "
                 + "(DEFUN TESTRET (x) "
                 + "(TRY \"aa\" (RETURN x) \"cc\" "
                 + "(CATCH java.lang.Exception ex (RETURN (+ x 1))))) "
                 + "(TESTRET 3))",
            3,
            true,
            null,
            null,
            p
          },
          {
            "(PROGN "
                 + "(DEFUN TESTRET (x) "
                 + "(TRY \"aa\" (/ 1 0) (RETURN x) \"cc\" "
                 + "(CATCH java.lang.Exception ex (RETURN (+ x 1))))) "
                 + "(TESTRET 3))",
            4,
            true,
            null,
            null,
            p
          },
          {
            "(PROGN "
                 + "(DEFUN TESTRET (x) "
                 + "(TRY \"aa\" (RETURN x) \"cc\")) "
                 + "(TESTRET 3))",
            3,
            true,
            null,
            null,
            p
          },
          {
            "(PROGN "
                 + "(DEFUN TESTRET (x) "
                 + "(TRY (/ 1 0) (RETURN x) \"cc\" "
                 + "(CATCH java.lang.Exception ex (RETURN (+ x 1)) 8))) "
                 + "(TESTRET 3))",
            4,
            true,
            null,
            null,
            p
          },

          {
            "(PROGN "
            + "(TRY (/ 1 0) (RETURN 5) \"cc\" "
            + "  (CATCH java.lang.Exception ex (SETV y 1))) "
            + "(LIST y (BOOL ex)))",
            list(1, true),
            true,
            null,
            null,
            p
          },
          {
            "(PROGN "
            + "(TRY (SETV q 4) (/ 1 0) (RETURN 5) \"cc\" "
            + "  (CATCH java.lang.Exception ex (SETV y 1)) "
            + "  (FINALLY (SETV z 2))) "
            + "(LIST q y (BOOL ex) z))",
            list(4,1, true, 2),
            true,
            null,
            null,
            p
          },
          // LOOPS
          {
            "(LET ((r ())) (LIST (FOREACH (a \"ABC\")  (SETV r (APPEND r (LIST 1 a)))) r))",
            list(null, list(1, 'A', 1, 'B', 1, 'C')),
            true,
            null,
            null,
            p
          },
          {
            "(LET ((r ())) (FOREACH (a (LIST 2 3 4))  (SETV r (APPEND r (LIST 1 a)))) r)",
            list(1, 2, 1, 3, 1, 4),
            true,
            null,
            null,
            p
          },
          {
            "(LET ((r ()) (ar (MAKE-ARRAY :size 3))) (PUT! ar 0 2) (PUT! ar 1 3) (PUT! ar 2 4) (FOREACH"
                + " (a ar)  (SETV r (APPEND r (LIST 1 a)))) r)",
            list(1, 2, 1, 3, 1, 4),
            true,
            null,
            null,
            p
          },
          {
            "(LET ((r ()) (ar (MAKE-ARRAY :size 3))) (PUT! ar 0 2) (PUT! ar 1 3) (PUT! ar 2 4) (FOREACH"
                + " (a ar)  (SETV r (APPEND r (LIST 1 a)))))",
            null,
            false,
            null,
            null,
            p
          },
          {
            "(LET ((r ()) (ar (MAKE-ARRAY :size 3))) (PUT! ar 0 2) (PUT! ar 1 3) (PUT! ar 2 4) (FOREACH"
                + " (a ar r)  (SETV r (APPEND r (LIST 1 a)))))",
            list(1, 2, 1, 3, 1, 4),
            true,
            null,
            null,
            p
          },
          {
            "(LET ((r ()) (ar (MAKE-ARRAY :size 3))) (ASET! ar 0 2) (ASET! ar 1 3) (ASET! ar 2 4) (FOREACH"
                + " (a ar r)  (SETV r (APPEND r (LIST 1 a)))))",
            list(1, 2, 1, 3, 1, 4),
            true,
            null,
            null,
            p
          },
          {
            "(LET ((r ()) (ar (LIST NIL NIL NIL))) (ASET! ar 0 2) (ASET! ar 1 3) (ASET! ar 2 4) (FOREACH"
                + " (a ar r)  (SETV r (APPEND r (LIST 1 a)))))",
            list(1, 2, 1, 3, 1, 4),
            true,
            null,
            null,
            p
          },
          {
            "(LET ((ar (MAKE-ARRAY NIL NIL NIL)) (ar2 (ASET (ASET (ASET ar 0 2) 1 3) 2 4))) (LIST ar ar2))",
            list(new Object[] {null,null,null}, new Object[] {2, 3, 4}),
            true,
            null,
            null,
            p
          },
          {
            "(LET ((ar (LIST NIL NIL NIL)) (ar2 (ASET (ASET (ASET ar 0 2) 1 3) 2 4))) (LIST ar ar2))",
            list(list(null,null,null), list(2, 3, 4)),
            true,
            null,
            null,
            p
          },
          {
            "(LET ((ar (.S \"java.util.Collections\" \"unmodifiableList\" (LIST (LIST NIL NIL NIL)))) "
            + "    (ar2 (ASET (ASET (ASET ar 0 2) 1 3) 2 4))) "
            + "  (LIST ar ar2))",
            list(list(null,null,null), list(2, 3, 4)),
            true,
            null,
            null,
            p
          },          
          {"(AS-> (+ 1 0) foo)", 1, true, null, null, p},
          {"(AS-> (+ 1 0) foo (* 10 foo))", 10, true, null, null, p},
          {"(AS-> (+ 1 0) foo (* 10 foo) (* 2 foo))", 20, true, null, null, p},
          {
            "(LET ((foo 100)) (AS-> (+ 1 0) foo (* 10 foo) (* 2 foo)) foo)",
            100,
            true,
            null,
            null,
            p
          },
          {"(->> 10 (- 1) (- 2))", 11, true, null, null, p},
          {"(->  10 (- 1) (- 2))", 7, true, null, null, p},
          {"(->  10 (- 1) (- 2))", 7, true, null, null, p},
          {
            "(->  (LIST 1 2 3 4 5) ((LAMBDA (X Y) (TAKE Y X)) 3))",
            list(1, 2, 3),
            true,
            null,
            null,
            p
          },
          {
            "(PROGN (DEFUN FFF (X Y) (TAKE Y X)) (->  (LIST 1 2 3 4 5) (FFF 3)))",
            list(1, 2, 3),
            true,
            null,
            null,
            p
          },
          {"(LET ((%% 100)) (->  10 (- 1) (- 2)) %%)", 100, true, null, null, p},
          {"(LET ((%% 100)) (->> 10 (- 1) (- 2)) %%)", 100, true, null, null, p},
          {"(@->  (LIST 1 2 3 4 5) (TAKE 3) (SUBSEQ 1))", list(2, 3), true, null, null, p},
          {"(@->  (LIST 1 2 3 4 5) (APPLY (FUNCTION *)))", 120, true, null, null, p},
          {"(@->  (RANGE 1 6) (APPLY (FUNCTION *)))", 120, true, null, null, p},
          // implicit apply with REST_PIPE
          {"(@->  (RANGE 1 6) (*))", 120, true, null, null, p},
          {"(@->  (RANGE 1 6) (+))", 15, true, null, null, p},
          {"(@->  (RANGE 1 6) (+ (+ 1 4)))", 20, true, null, null, p},
          {"(@->  (RANGE 1 6) (MIN))", 1, true, null, null, p},
          {"(@->  (RANGE 1 6) (MAX))", 5, true, null, null, p},
          {"(@->  (RANGE 1 6) (MIN 0))", 0, false, null, null, p},
          {"(@-> (LIST (LIST 1 2) (LIST 3 4)) APPEND +)", 10, true, null, null, p},
          {"(PROGN (EVAL (READ-FROM-STRING \"(DEFUN fff (&REST &PIPE_REST c) c)\")) "
           + "(EVAL (READ-FROM-STRING \"(@-> (LIST 1 2 3 4 5) (fff))\")))",
           list(1,2,3,4,5), true, null, null, p},
          // {"(PROGN (DEFUN FFF (X &PIPE Y) (TAKE X Y)) (@-> (LIST 1 2 3 4 5) (FFF 3)))",
          // list(1,2,3), true, null, null, p},
          // FIXME: does not work when DEFUN is defined in smae expr:
          //        because of the funcall hack
          // (PROGN (DEFUN FFF (X &PIPE Y) (TAKE X Y)) (@-> (LIST 1 2 3 4 5) (FFF 3)))
          // FIXME: support lambda inline
          // {"(@->  (LIST 1 2 3 4 5) ((LAMBDA (X &PIPE Y) (TAKE X Y)) 3))", list(1,2,3), true,
          // null, null, p},

          // RANGE
          {"(APPEND ()  (RANGE 0 0))", list(), false, null, null, p},
          {"(APPEND ()  (RANGE 0 -2))", list(), false, null, null, p},
          {"(APPEND ()  (RANGE 1 1))", list(), false, null, null, p},
          {"(APPEND ()  (RANGE -1 -1))", list(), false, null, null, p},
          {"(APPEND ()  (RANGE 0 1))", list(0), true, null, null, p},
          {"(APPEND ()  (RANGE 0 5))", list(0, 1, 2, 3, 4), true, null, null, p},
          {"(APPEND ()  (RANGE 0 5 1))", list(0, 1, 2, 3, 4), true, null, null, p},
          {
            "(APPEND ()  (RANGE 0b 5b 1b))",
            list((byte) 0, (byte) 1, (byte) 2, (byte) 3, (byte) 4),
            true,
            null,
            null,
            p
          },
          {"(APPEND ()  (RANGE 0 10 2))", list(0, 2, 4, 6, 8), true, null, null, p},
          {"(APPEND ()  (RANGE 0.0 10 2))", list(0.0, 2.0, 4.0, 6.0, 8.0), true, null, null, p},
          {
            "(APPEND ()  (RANGE 0.0 -10 -2))",
            list(0.0, -2.0, -4.0, -6.0, -8.0),
            true,
            null,
            null,
            p
          },
          {"(APPEND () (RANGE 1 -4 -1))", list(1, 0, -1, -2, -3), true, null, null, p},
          // FIXME: test more types of copy
          {"(LET ((o ()) (t (COPY o))) (LIST (== o t) (=== o t)))",
           list(true, false), true, null, null, p},
          {"(LET ((o (LIST 1 2 3)) (t (COPY o))) (LIST (== o t) (=== o t)))",
           list(true, false), true, null, null, p},
          {"(LET ((o (HASHMAP 1 2 3 4)) (t (COPY o))) (LIST (== o t) (=== o t)))",
           list(true, false), true, null, null, p},
          {"(LET ((o (MAKE-ARRAY 1 2 3 4)) (t (COPY o))) (LIST (== o t) (=== o t)))",
           list(true, false), true, null, null, p},
          // FIXME: test more types of PUT
          {"(LET ((o (HASHMAP 1 2 )) (t (PUT o 3 4))) (LIST o t ))",
           list(map(1,2), map(1,2,3,4)), true, null, null, p},
          {"(LET ((o (LIST 1 2 )) (t (PUT o 1 3))) (LIST o t ))",
           list(list(1,2), list(1,3)), true, null, null, p},
          {"(LET ((o (LIST 1 2 )) (t (PUT o 2 3))) (LIST o t ))",
           list(list(1,2), list(1,2,3)), true, null, null, p},
          {"(LET ((o (MAKE-ARRAY 1 2 )) (t (PUT o 1 3))) (IF (=== t o) false t))",
           Utils.arrayOfObjects(1,3), true, null, null, p},
          {"(LET ((o \"ABC\" ) (t (PUT o 1 (CHAR 68)))) (LIST o t))",
           list("ABC","ADC"), true, null, null, p},
          {"(LET ((o (STRING-BUILDER \"ABC\") ) (t (PUT o 1 (CHAR 68)))) (IF (=== t o) false t))",
           new StringBuilder("ADC"), true, null, null, p},
          {"(LET ((o (STRING-BUFFER \"ABC\") ) (t (PUT o 1 (CHAR 68)))) (IF (=== t o) false t))",
           new StringBuffer("ADC"), true, null, null, p},
          {"(LET ((o (.S \"java.util.Collections\" \"unmodifiableList\" (LIST (LIST 1 2 3))))"
           + "    (t (PUT o 1 22))) (LIST o t))",
           list(list(1,2,3),list(1,22,3)), true, null, null, p},
          {"(LET ((o (.S \"java.util.Collections\" \"unmodifiableMap\" (LIST (HASHMAP 1 2 )))) (t (PUT o 3 4))) (LIST o t ))",
           list(map(1,2), map(1,2,3,4)), true, null, null, p},
          {
            "(LET ((R (RANGE 0 1 0.05)) (I (. R \"iterator\" ()))) (. I  \"next\" ()) (. I "
                + " \"next\" ()))",
            0.05,
            true,
            null,
            null,
            p
          },
          {"(LET ((R (RANGE 0 2))) (APPEND () R R))", list(0, 1, 0, 1), true, null, null, p},
          {
            "(LET ((R (RANGE 0 3))) (LIST (LENGTH R) (APPEND () R ) (LENGTH R)))",
            list(3, list(0, 1, 2), 3),
            true,
            null,
            null,
            p
          },
          {
            "(LET ((R (RANGE 0.0 3.0))) (LIST (LENGTH R) (APPEND () R ) (LENGTH R)))",
            list(3, list(0.0, 1.0, 2.0), 3),
            true,
            null,
            null,
            p
          },
          {"(LENGTH (RANGE 1 4 1))", 3, true, null, null, p},
          {"(LENGTH (RANGE 1 -4 -1))", 5, true, null, null, p},
          {"(LENGTH (RANGE 1 4 -1))", 0, false, null, null, p},
          {"(LENGTH (RANGE 0 0))", 0, false, null, null, p},
          {"(LENGTH (RANGE 0 0 3))", 0, false, null, null, p},
          {"(LENGTH (RANGE 2 2 3))", 0, false, null, null, p},
          {"(LENGTH (RANGE 0 10))", 10, true, null, null, p},
          {"(LENGTH (RANGE 2 17 3))", 5, true, null, null, p},
          {"(LENGTH (RANGE 2 18 3))", 6, true, null, null, p},
          {"(LENGTH (RANGE 0.0 0))", 0, false, null, null, p},
          {"(LENGTH (RANGE 0.0 0 3))", 0, false, null, null, p},
          {"(LENGTH (RANGE 2.0 2 3))", 0, false, null, null, p},
          {"(LENGTH (RANGE 0.0 10))", 10, true, null, null, p},
          {"(LENGTH (RANGE 2.0 17 3))", 5, true, null, null, p},
          {"(LENGTH (RANGE 2.0 18 3))", 6, true, null, null, p},
          // // // // LAMBDA
          {"((LAMBDA ()))", null, false, null, null, p},
          {"((LAMBDA (a b)) 1 2)", null, false, null, null, p},
          {"((LAMBDA (A B C) (+ A A B B C C)) 1 2 3)", 12, true, null, null, p},
          {"((LAMBDA (x y) (LIST x y)) 1 2)", list(1, 2), true, null, null, p},
          {"((LAMBDA (x &OPTIONAL y) (LIST x y)) 1 2)", list(1, 2), true, null, null, p},
          {"((LAMBDA (x &OPTIONAL y) (LIST x y)) 1)", list(1, null), true, null, null, p},
          {"((LAMBDA (&REQUIRED x &OPTIONAL y) (LIST x y)) 1)", list(1, null), true, null, null, p},
          {
            "((LAMBDA (&REQUIRED x &OPTIONAL y &REQUIRED z) (LIST x y z)) 1 2)",
            list(1, null, 2),
            true,
            null,
            null,
            p
          },
          {
            "((LAMBDA (&OPTIONAL x &REQUIRED y z) (LIST x y z)) 1 2)",
            list(null, 1, 2),
            true,
            null,
            null,
            p
          },
          {
            "((LAMBDA (x &REST y) (LIST x y )) 1 2 3 4 5)",
            list(1, list(2, 3, 4, 5)),
            true,
            null,
            null,
            p
          },
          {"((LAMBDA (x &REST y) (LIST x y )) 1 )", list(1, list()), true, null, null, p},
          {
            "((LAMBDA (x &REST y) (LIST x y )) 1 (+ 1 2) (+ 4 5))",
            list(1, list(3, 9)),
            true,
            null,
            null,
            p
          },
          {
            "((LAMBDA (x &OPTIONAL y &REST z) (LIST x y z)) 1 2 3 4)",
            list(1, 2, list(3, 4)),
            true,
            null,
            null,
            p
          },
          {
            "((LAMBDA (x &OPTIONAL y &REST z) (LIST x y z)) 1 )",
            list(1, null, list()),
            true,
            null,
            null,
            p
          },
          {"((LAMBDA (&REST x) x))", list(), false, null, null, p},
          {
            "((LAMBDA (a &REST x &REQUIRED y z) (LIST a x y z)) 1 2 3 4 5)",
            list(1, list(2, 3), 4, 5),
            true,
            null,
            null,
            p
          },
          // keyword args
          {"((LAMBDA (&KEY a) (LIST a)) :a 1)", list(1), true, null, null, p},
          {
            "((LAMBDA (&KEY a b c) (LIST a b c)) :a 1 :b 2 :c 3)",
            list(1, 2, 3),
            true,
            null,
            null,
            p
          },
          {
            "((LAMBDA (&KEY a b c) (LIST a b c)) :a 1 :b 2 )", list(1, 2, null), true, null, null, p
          },
          {"((LAMBDA (&KEY a b c) (LIST a b c)) :c 3 )", list(null, null, 3), true, null, null, p},
          {"((LAMBDA (&KEY (a)) (LIST a )) :a 3 )", list(3), true, null, null, p},
          {"((LAMBDA (&KEY (a (+ 5 2))) (LIST a )) :a 3 )", list(3), true, null, null, p},
          {"((LAMBDA (&KEY (a (+ 5 2))) (LIST a )))", list(7), true, null, null, p},
          {
            "((LAMBDA (&KEY (a (+ 5 2) sa)) (LIST a sa)) :a 3 )", list(3, true), true, null, null, p
          },
          {"((LAMBDA (&KEY (a (+ 5 2) sa)) (LIST a sa)) )", list(7, false), true, null, null, p},
          {"((LAMBDA (&KEY (a NIL sa)) (LIST a sa)) )", list(null, false), true, null, null, p},
          {
            "((LAMBDA (&KEY (a NIL sa)) (LIST a sa)) :a NIL)", list(null, true), true, null, null, p
          },
          {
            "((LAMBDA (x &REST &KEY a b c &ALLOW-OTHER-KEYS) (LIST x a b c)) 10 :c 3 :b 2 :z 8 :a 9)",
            list(10,list(new Keyword(":z"), 8, new Keyword(":a"), 9), 2, 3),
            true,
            null,
            null,
            p
          },
          {
            "((LAMBDA (&OPTIONAL (a (+ 5 2) sa)) (LIST a sa)) 3 )",
            list(3, true),
            true,
            null,
            null,
            p
          },
          {
            "((LAMBDA (&OPTIONAL (a (+ 5 2) sa)) (LIST a sa)) )",
            list(7, false),
            true,
            null,
            null,
            p
          },
          {
            "((LAMBDA (&OPTIONAL (a NIL sa)) (LIST a sa)) )", list(null, false), true, null, null, p
          },
          {
            "((LAMBDA (&OPTIONAL (a NIL sa)) (LIST a sa)) NIL)",
            list(null, true),
            true,
            null,
            null,
            p
          },
          {
            "((LAMBDA (x &OPTIONAL (a (+ 1 x) sa)) (LIST x a sa)) 3)",
            list(3, 4, false),
            true,
            null,
            null,
            p
          },
          {
            "((LAMBDA (x &OPTIONAL (a (+ 1 x) sa)) (LIST x a sa)) 3 5)",
            list(3, 5, true),
            true,
            null,
            null,
            p
          },
          {"((LAMBDA (&LAZY a b c) (LIST a b c)) 1 2 3)", list(1, 2, 3), true, null, null, p},
          {
            "(PROGN (DEFUN tkwf (&KEY a b c) (LIST a b c)) (tkwf :a 1 :b 2 ))",
            list(1, 2, null),
            true,
            null,
            null,
            p
          },
          {
            "(PROGN (DEFUN tkwf (&KEY a b c) (LIST a b c)) (FUNCALL (FUNCTION tkwf) :a 1 :b 2 ))",
            list(1, 2, null),
            true,
            null,
            null,
            p
          },
          {
            "(LET ((f (LAMBDA (A B C) (* A A B B C C)))) (FUNCALL f 1 2 3))",
            36,
            true,
            null,
            null,
            p
          },
          {
            "(LET ((f (LAMBDA (x) (IF x (LET ((y (- x 1))) (FUNCALL f y)) 0)))) (FUNCALL f 1))",
            0,
            false,
            null,
            null,
            p
          },
          {
            "(LET ((f (LAMBDA (x) (IF x (* x (FUNCALL f (- x 1))) 1)))) (FUNCALL f 5))",
            120,
            true,
            null,
            null,
            p
          },
          {"(LIST)", list(), false, null, null, p},
          {"(LIST 1 2 3 4)", list(1, 2, 3, 4), true, null, null, p},
          {"(LIST (+ 1 2) (+ 1 1) 3 4)", list(3, 2, 3, 4), true, null, null, p},
          {"(NTH 2 (LIST 1 2 3 4))", Integer.valueOf(3), true, null, null, p},
          {"(NTH 5 (LIST 1 2 3 4))", null, false, null, null, p},
          {"(NTH 5 NIL)", null, false, null, null, p},
          {"(NTH 2 (. (LIST 1 2 3 4) \"toArray()\" ))", Integer.valueOf(3), true, null, null, p},
          {"(NTH 5 (. (LIST 1 2 3 4) \"toArray()\" ))", null, false, null, null, p},
          {
            "(LET ((s (MAKE-ARRAY :size 2 :elementType (QUOTE int)))) (NTH 0 s))",
            0,
            false,
            null,
            null,
            p
          },
          {
            "(LET ((s (MAKE-ARRAY :size 2 :elementType (QUOTE int)))) (NTH 2 s))",
            null,
            false,
            null,
            null,
            p
          },
          {
            "(LET ((s (.N \"java.lang.StringBuilder\")))  (. s \"append\" (LIST \"abc\")) (NTH 2"
                + " s))",
            'c',
            true,
            null,
            null,
            p
          },
          {
            "(LET ((s (.N \"java.lang.StringBuilder\")))  (. s \"append\" (LIST \"abc\")) (NTH 3"
                + " s))",
            null,
            false,
            null,
            null,
            p
          },
          {"(CONS 1 (LIST 2 3 4))", list(1, 2, 3, 4), true, null, null, p},
          {"(CONS 1 2)", list(1, 2), true, null, null, p},
          {"(APPEND (STRING-BUFFER \"foo\") \"bar\")", new StringBuffer("foobar"), true, null, null, p},
          {"(APPEND (STRING-BUILDER \"foo\") \"bar\")", new StringBuilder("foobar"), true, null, null, p},
          {"(LET ((B (STRING-BUILDER \"foo\"))) (APPEND! B \"bar\") B)", new StringBuilder("foobar"), true, null, null, p},
          {"(LET ((B (STRING-BUFFER  \"foo\"))) (APPEND! B \"bar\") B)", new  StringBuffer("foobar"), true, null, null, p},
          {"(APPEND \"foo\" \"bar\")", "foobar", true, null, null, p},
          {"(APPEND (LIST 1 2) (LIST 3) 4)", list(1, 2, 3, 4), true, null, null, p},
          {"(APPEND)", list(), false, null, null, p},
          {"(APPEND () 1 2 3)", list(1, 2, 3), true, null, null, p},
          {
            "(APPEND (MAKE-ARRAY :size 0 :elementType \"int\") 1 2 3)",
            Utils.array(1, 2, 3),
            true,
            null,
            null,
            p
          },
          {
            "(APPEND (MAKE-ARRAY :size 1 :elementType \"int\") 1 (LIST 2 3) (MAKE-ARRAY :size 2) \"AB\")",
            Utils.array(0, 1, 2, 3, 0, 0, 65, 66),
            true,
            null,
            null,
            p
          },
          {
            "(APPEND (MAKE-ARRAY :size 1 :elementType \"char\") \"aa\" (LIST (CHAR 113) 65))",
            Utils.array('\0', 'a', 'a', 'q', 'A'),
            true,
            null,
            null,
            p
          },
          {
            "(APPEND (MAKE-ARRAY :size 1 :elementType \"byte\") 1 (LIST 2 3) (MAKE-ARRAY :size 2) \"AB\")",
            Utils.array(
                (byte) 0, (byte) 1, (byte) 2, (byte) 3, (byte) 0, (byte) 0, (byte) 65, (byte) 66),
            true,
            null,
            null,
            p
          },
          {
            "(APPEND (MAKE-ARRAY :size 1 :elementType \"short\") 1 (LIST 2 3) (MAKE-ARRAY :size 2) \"AB\")",
            Utils.array(
                (short) 0,
                (short) 1,
                (short) 2,
                (short) 3,
                (short) 0,
                (short) 0,
                (short) 65,
                (short) 66),
            true,
            null,
            null,
            p
          },
          {"(FIRST (LIST 1 2 3))", 1, true, null, null, p},
          {"(REST (LIST 1 2 3))", list(2, 3), true, null, null, p},
          {"(REST (LIST 1))", list(), false, null, null, p},
          {"(LENGTH \"1234\")", 4, true, null, null, p},
          {"(LENGTH (LIST 1 2 3 (LIST 5 6)))", 4, true, null, null, p},
          {"(LENGTH (MAKE-ARRAY :size 2 :elementType (QUOTE int)))", 2, true, null, null, p},
          {"(LENGTH (MAKE-ARRAY :size 2))", 2, true, null, null, p},
          {"(LENGTH NIL)", 0, false, null, null, p},
          {"(LENGTH \"\")", 0, false, null, null, p},
          {"(LENGTH 222)", 1, true, null, null, p},
          {
            "(SUBSEQ (APPEND (MAKE-ARRAY :size 0) (LIST 1 2 3 4)) 0)",
            new Object[] {1, 2, 3, 4},
            true,
            null,
            null,
            p
          },
          {
            "(SUBSEQ (APPEND (MAKE-ARRAY :size 0) (LIST 1 2 3 4)) 1)",
            new Object[] {2, 3, 4},
            true,
            null,
            null,
            p
          },
          {
            "(SUBSEQ (APPEND (MAKE-ARRAY :size 0) (LIST 1 2 3 4)) 4)",
            new Object[] {},
            false,
            null,
            null,
            p
          },
          {
            "(SUBSEQ (APPEND (MAKE-ARRAY :size 0) (LIST 1 2 3 4)) 0 3)",
            new Object[] {1, 2, 3},
            true,
            null,
            null,
            p
          },
          {
            "(SUBSEQ (APPEND (MAKE-ARRAY :size 0) (LIST 1 2 3 4)) 0 4)",
            new Object[] {1, 2, 3, 4},
            true,
            null,
            null,
            p
          },
          {
            "(SUBSEQ (APPEND (MAKE-ARRAY :size 0) (LIST 1 2 3 4)) 0 5)",
            new Object[] {1, 2, 3, 4},
            true,
            null,
            null,
            p
          },
          {
            "(SUBSEQ (APPEND (MAKE-ARRAY :size 0) (LIST 1 2 3 4)) 1)",
            new Object[] {2, 3, 4},
            true,
            null,
            null,
            p
          },
          {
            "(SUBSEQ (APPEND (MAKE-ARRAY :size 0) (LIST 1 2 3 4)) 4 4)",
            new Object[] {},
            false,
            null,
            null,
            p
          },
          {"(SUBSEQ (LIST 1 2 3 4) 0)", list(1, 2, 3, 4), true, null, null, p},
          {"(SUBSEQ (LIST 1 2 3 4) 1)", list(2, 3, 4), true, null, null, p},
          {"(SUBSEQ (LIST 1 2 3 4) 4)", list(), false, null, null, p},
          {"(SUBSEQ (LIST 1 2 3 4) 0 3)", list(1, 2, 3), true, null, null, p},
          {"(SUBSEQ (LIST 1 2 3 4) 0 4)", list(1, 2, 3, 4), true, null, null, p},
          {"(SUBSEQ (LIST 1 2 3 4) 0 5)", list(1, 2, 3, 4), true, null, null, p},
          {"(SUBSEQ (LIST 1 2 3 4) 1)", list(2, 3, 4), true, null, null, p},
          {"(SUBSEQ (LIST 1 2 3 4) 4 4)", list(), false, null, null, p},
          {"(SUBSEQ \"1234\" 0)", "1234", true, null, null, p},
          {"(SUBSEQ \"1234\" 1)", "234", true, null, null, p},
          {"(SUBSEQ \"1234\" 4)", "", false, null, null, p},
          {"(SUBSEQ \"1234\" 0 3)", "123", true, null, null, p},
          {"(SUBSEQ \"1234\" 0 4)", "1234", true, null, null, p},
          {"(SUBSEQ \"1234\" 0 5)", "1234", true, null, null, p},
          {"(SUBSEQ \"1234\" 1)", "234", true, null, null, p},
          {"(SUBSEQ \"1234\" 4 4)", "", false, null, null, p},
          {
            "(TAKE 0 (APPEND (MAKE-ARRAY :size 0) (LIST 1 2 3 4)))", new Object[] {}, false, null, null, p
          },
          {
            "(TAKE 1 (APPEND (MAKE-ARRAY :size 0) (LIST 1 2 3 4)))", new Object[] {1}, true, null, null, p
          },
          {
            "(TAKE 4 (APPEND (MAKE-ARRAY :size 0) (LIST 1 2 3 4)))",
            new Object[] {1, 2, 3, 4},
            true,
            null,
            null,
            p
          },
          {
            "(TAKE 5 (APPEND (MAKE-ARRAY :size 0) (LIST 1 2 3 4)))",
            new Object[] {1, 2, 3, 4},
            true,
            null,
            null,
            p
          },
          {"(TAKE 0 (LIST 1 2 3 4))", list(), false, null, null, p},
          {"(TAKE 1 (LIST 1 2 3 4))", list(1), true, null, null, p},
          {"(TAKE 4 (LIST 1 2 3 4))", list(1, 2, 3, 4), true, null, null, p},
          {"(TAKE 5 (LIST 1 2 3 4))", list(1, 2, 3, 4), true, null, null, p},
          {"(TAKE 0 \"1234\")", "", false, null, null, p},
          {"(TAKE 1 \"1234\")", "1", true, null, null, p},
          {"(TAKE 4 \"1234\")", "1234", true, null, null, p},
          {"(TAKE 5 \"1234\")", "1234", true, null, null, p},
          {"(IN  (CHAR 81)  \"Quogga\")", true, true, null, null, p},
          {"(IN  (CHAR 97)  \"Quogga\")", true, true, null, null, p},
          {"(IN  (CHAR 97)  \"Quagga\")", true, true, null, null, p},
          {"(IN  (CHAR 122)  \"Quagga\")", false, false, null, null, p},
          {"(IN  NIL  \"Quagga\")", false, false, null, null, p},
          {"(IN \"FOO\"   (LIST \"BAR\" \"BAZ\"))", false, false, null, null, p},
          {"(IN \"QQQ\"   (LIST \"BAR\" \"FOO\" \"QQQ\"))", true, true, null, null, p},
          {
            "(LET ((A (MAKE-ARRAY :size 4))) (PUT! A 0 1) (PUT! A 1 2) (PUT! A 2 3) (PUT! A 3 4)  (SUBSEQ"
                + " A 0))",
            new Object[] {1, 2, 3, 4},
            true,
            null,
            null,
            p
          },
          {
            "(LET ((A (MAKE-ARRAY :size 4))) (PUT! A 0 1) (PUT! A 1 2) (PUT! A 2 3) (PUT! A 3 4)  (SUBSEQ"
                + " A 1))",
            new Object[] {2, 3, 4},
            true,
            null,
            null,
            p
          },
          {
            "(LET ((A (MAKE-ARRAY :size 4))) (PUT! A 0 1) (PUT! A 1 2) (PUT! A 2 3) (PUT! A 3 4)  (SUBSEQ"
                + " A 4))",
            new Object[] {},
            false,
            null,
            null,
            p
          },
          {
            "(LET ((A (MAKE-ARRAY :size 4 :elementType \"int\"))) (PUT! A 0 1) (PUT! A 1 2) (PUT! A 2 3)"
                + " (PUT! A 3 4)  (SUBSEQ A 0))",
            new int[] {1, 2, 3, 4},
            true,
            null,
            null,
            p
          },
          {
            "(LET ((A (MAKE-ARRAY :size 4 :elementType \"int\"))) (PUT! A 0 1) (PUT! A 1 2) (PUT! A 2 3)"
                + " (PUT! A 3 4)  (SUBSEQ A 1))",
            new int[] {2, 3, 4},
            true,
            null,
            null,
            p
          },
          {
            "(LET ((A (MAKE-ARRAY :size 4  :elementType \"int\"))) (PUT! A 0 1) (PUT! A 1 2) (PUT! A 2"
                + " 3) (PUT! A 3 4)  (SUBSEQ A 4))",
            new int[] {},
            false,
            null,
            null,
            p
          },

          // COERCION
          {"(STRING 1)", "1", true, null, null, p},
          {"(STRING \"a\")", "a", true, null, null, p},
          {"(STRING true)", "true", true, null, null, p},
          {"(STRING (CHAR 97))", "a", true, null, null, p},
          {"(STRING NIL)", "NIL", true, null, null, p},
          {"(BOOL 1)", true, true, null, null, p},
          {"(BOOL 1)", true, true, null, null, p},
          {"(BOOL 0)", false, false, null, null, p},
          {"(BOOL \"\")", false, false, null, null, p},
          {"(BOOL \"foo\")", true, true, null, null, p},
          {"(BOOL true)", true, true, null, null, p},
          {"(BOOL false)", false, false, null, null, p},
          {"(BOOL ())", false, false, null, null, p},
          {"(BOOL (LIST NIL))", true, true, null, null, p},
          {"(BOOL (MAKE-ARRAY :size 0))", false, false, null, null, p},
          {"(BOOL (MAKE-ARRAY :size 1))", true, true, null, null, p},
          {"(INT 1)", 1, true, null, null, p},
          {"(INT 1.0)", 1, true, null, null, p},
          {"(INT 0)", 0, false, null, null, p},
          {"(INT -1)", -1, true, null, null, p},
          {"(INT (CHAR 67))", 67, true, null, null, p},
          {"(INT (CHAR 0))", 0, false, null, null, p},
          {"(INT \"22\")", 22, true, null, null, p},
          {"(INT \" 22 \")", 22, true, null, null, p},
          {"(INT \" 22.1 \")", 22, true, null, null, p},
          {"(INT (LIST))", 0, false, null, null, p},
          {"(INT (LIST 1 2))", 2, true, null, null, p},
          {
            "(INT \"\")",
            null,
            false,
            new ExecutionException(
                null,
                new RuntimeException(
                    "String '' cannot be coerced to Number",
                    new NumberFormatException("Invalid numeric literal ''"))),
            null,
            p
          },
          {
            "(INT \" QQ\")",
            null,
            false,
            new ExecutionException(
                null,
                new RuntimeException(
                    "String ' QQ' cannot be coerced to Number",
                    new NumberFormatException("Invalid numeric literal 'QQ'"))),
            null,
            p
          },
          {"(LONG 1)", 1L, true, null, null, p},
          {"(LONG 1.0)", 1L, true, null, null, p},
          {"(LONG 0)", 0L, false, null, null, p},
          {"(LONG -1)", -1L, true, null, null, p},
          {"(LONG \"22\")", 22L, true, null, null, p},
          {"(LONG \" 22 \")", 22L, true, null, null, p},
          {"(LONG \" 22.1 \")", 22L, true, null, null, p},
          {
            "(LONG \"\")",
            null,
            false,
            new ExecutionException(
                null,
                new RuntimeException(
                    "String '' cannot be coerced to Number",
                    new NumberFormatException("Invalid numeric literal ''"))),
            null,
            p
          },
          {
            "(LONG \" QQ\")",
            null,
            false,
            new ExecutionException(
                null,
                new RuntimeException(
                    "String ' QQ' cannot be coerced to Number",
                    new NumberFormatException("Invalid numeric literal 'QQ'"))),
            null,
            p
          },
          {"(DOUBLE 1)", 1.0, true, null, null, p},
          {"(DOUBLE 1.0)", 1.0, true, null, null, p},
          {"(DOUBLE 0)", 0.0, false, null, null, p},
          {"(DOUBLE -1)", -1.0, true, null, null, p},
          {"(DOUBLE \"22\")", 22.0, true, null, null, p},
          {"(DOUBLE \" 22 \")", 22.0, true, null, null, p},
          {"(DOUBLE \" 22.1 \")", 22.1, true, null, null, p},
          {"(DOUBLE \"NaN\")", Double.NaN, true, null, null, p},
          {"(DOUBLE (STRING-BUFFER \"1.1\"))", 1.1, true, null, null, p},
          {"(DOUBLE (STRING-BUILDER \"1.1\"))", 1.1, true, null, null, p},
          {"(DOUBLE \"1.1e4\")", 1.1e+4, true, null, null, p},          
          {"(DOUBLE \"1.1E4\")", 1.1E+4, true, null, null, p},
          {"(FLOAT \"1.1e4\")", 1.1e+4f, true, null, null, p},          
          {"(FLOAT \"1.1E4\")", 1.1E+4f, true, null, null, p},
          {"(FLOAT \"NaN\")", Float.NaN, true, null, null, p},          
          {"(FLOAT (STRING-BUFFER \"1.1\"))", 1.1f, true, null, null, p},
          {"(FLOAT (STRING-BUILDER \"1.1\"))", 1.1f, true, null, null, p},

          {
            "(DOUBLE \"\")",
            null,
            false,
            new ExecutionException(
                null,
                new RuntimeException(
                    "String '' cannot be coerced to Number",
                    new NumberFormatException("Invalid numeric literal ''"))),
            null,
            p
          },
          {
            "(DOUBLE \" QQ\")",
            null,
            false,
            new ExecutionException(
                null,
                new RuntimeException(
                    "String ' QQ' cannot be coerced to Number",
                    new NumberFormatException("Invalid numeric literal 'QQ'"))),
            null,
            p
          },
          {"(FLOAT 1)", (float) 1.0, true, null, null, p},
          {"(FLOAT 1.0)", (float) 1.0, true, null, null, p},
          {"(FLOAT 0)", (float) 0.0, false, null, null, p},
          {"(FLOAT -1)", (float) -1.0, true, null, null, p},
          {"(FLOAT \"22\")", (float) 22.0, true, null, null, p},
          {"(FLOAT \" 22 \")", (float) 22.0, true, null, null, p},
          {"(FLOAT \" 22.1 \")", (float) 22.1, true, null, null, p},
          {
            "(FLOAT \"\")",
            null,
            false,
            new ExecutionException(
                null,
                new RuntimeException(
                    "String '' cannot be coerced to Number",
                    new NumberFormatException("Invalid numeric literal ''"))),
            null,
            p
          },
          {
            "(FLOAT \" QQ\")",
            null,
            false,
            new ExecutionException(
                null,
                new RuntimeException(
                    "String ' QQ' cannot be coerced to Number",
                    new NumberFormatException("Invalid numeric literal 'QQ'"))),
            null,
            p
          },
          {"(SHORT 1)", (short) 1, true, null, null, p},
          {"(SHORT 1.0)", (short) 1, true, null, null, p},
          {"(SHORT 0)", (short) 0, false, null, null, p},
          {"(SHORT -1)", (short) -1, true, null, null, p},
          {"(SHORT \"22\")", (short) 22, true, null, null, p},
          {"(SHORT \" 22 \")", (short) 22, true, null, null, p},
          {"(SHORT \" 22.1 \")", (short) 22, true, null, null, p},
          {
            "(SHORT \"\")",
            null,
            false,
            new ExecutionException(
                null,
                new RuntimeException(
                    "String '' cannot be coerced to Number",
                    new NumberFormatException("Invalid numeric literal ''"))),
            null,
            p
          },
          {
            "(SHORT \" QQ\")",
            null,
            false,
            new ExecutionException(
                null,
                new RuntimeException(
                    "String ' QQ' cannot be coerced to Number",
                    new NumberFormatException("Invalid numeric literal 'QQ'"))),
            null,
            p
          },
          {"(BYTE 1)", (byte) 1, true, null, null, p},
          {"(BYTE 1.0)", (byte) 1, true, null, null, p},
          {"(BYTE 0)", (byte) 0, false, null, null, p},
          {"(BYTE -1)", (byte) -1, true, null, null, p},
          {"(BYTE \"22\")", (byte) 22, true, null, null, p},
          {"(BYTE \" 22 \")", (byte) 22, true, null, null, p},
          {"(BYTE \" 22.1 \")", (byte) 22, true, null, null, p},
          {
            "(BYTE \"\")",
            null,
            false,
            new ExecutionException(
                null,
                new RuntimeException(
                    "String '' cannot be coerced to Number",
                    new NumberFormatException("Invalid numeric literal ''"))),
            null,
            p
          },
          {
            "(BYTE \" QQ\")",
            null,
            false,
            new ExecutionException(
                null,
                new RuntimeException(
                    "String ' QQ' cannot be coerced to Number",
                    new NumberFormatException("Invalid numeric literal 'QQ'"))),
            null,
            p
          },
          {"(CHAR NIL)", '\0', false, null, null, p},
          {"(CHAR 48)", '0', true, null, null, p},
          {"(CHAR (CHAR 48))", '0', true, null, null, p},
          {"(CHAR 198)", '\u00c6', true, null, null, p}, // AE ligature
          {"(CHAR -58b)", '\u00c6', true, null, null, p}, // AE ligature
          {"(CHAR \"198\")", '\u00c6', true, null, null, p}, // AE ligature
          {"(CHAR true)", 'T', true, null, null, p},
          {"(CHAR false)", '\0', false, null, null, p},
          {"(SIGNUM 2)", 1, true, null, null, p},
          {"(SIGNUM -2)", -1, true, null, null, p},
          {"(SIGNUM 0)", 0, false, null, null, p},
          {"(SIGNUM 2L)", 1L, true, null, null, p},
          {"(SIGNUM -2L)", -1L, true, null, null, p},
          {"(SIGNUM 0L)", 0L, false, null, null, p},
          {"(SIGNUM -63072000000L)", -1L, true, null, null, p},
          {"(SIGNUM 2.0)", 1.0, true, null, null, p},
          {"(SIGNUM -2.0)", -1.0, true, null, null, p},
          {"(SIGNUM 0.0)", 0.0, false, null, null, p},
          {"(SIGNUM 2.0f)", (float) 1.0, true, null, null, p},
          {"(SIGNUM -2.0f)", (float) -1.0, true, null, null, p},
          {"(SIGNUM 0.0f)", (float) 0.0, false, null, null, p},
          {"(SIGNUM 2s)", (short) 1.0, true, null, null, p},
          {"(SIGNUM -2s)", (short) -1.0, true, null, null, p},
          {"(SIGNUM 0s)", (short) 0.0, false, null, null, p},
          {"(SIGNUM 2b)", (byte) 1, true, null, null, p},
          {"(SIGNUM -2b)", (byte) -1, true, null, null, p},
          {"(SIGNUM 0b)", (byte) 0, false, null, null, p},

          // {"( 4)",Math.sqrt(4), true, null, null, p},

          {"(SQRT 4)", Math.sqrt(4), true, null, null, p},
          {"(SQRT 0)", Math.sqrt(0), false, null, null, p},
          {"(SQRT 99)", Math.sqrt(99), true, null, null, p},
          {"(SQRT 99.9)", Math.sqrt(99.9), true, null, null, p},
          {"(LOG 100 10)", Math.log10(100), true, null, null, p},
          {"(LOG 100)", Math.log(100), true, null, null, p},
          {"(MAX 3 2 1 9.2 -9 (VERSION \"1.1.0\"))", 9.2, true, null, null, p},
          {"(MIN 3 2 1 9.2 -9 (VERSION \"1.1.0\"))", -9, true, null, null, p},
          {
            "(MAX 3 2 1 9.2 -9 (VERSION \"10.1.0\"))",
            Version.mkSemVersion(10L, 1L, 0L, null, null),
            true,
            null,
            null,
            p
          },

          // FFI
          // medhod w/o parameters
          {"(. \"FOO\" \"length\" (LIST ) )", Integer.valueOf(3), true, null, null, p},
          {"(. \"FOO\" \"length\" () )", Integer.valueOf(3), true, null, null, p},
          {"(. \"FOO\" \"length()\" )", Integer.valueOf(3), true, null, null, p},
          // field of object
          {"(. 3 \"MAX_VALUE\" )", Integer.MAX_VALUE, true, null, null, p},
          {"(. 3 \"MAX_VALUE\" \"MIN_VALUE\" )", Integer.MIN_VALUE, true, null, null, p},
          // combinations
          {"(. \"FOO\" \"length\" () \"MAX_VALUE\" )", Integer.MAX_VALUE, true, null, null, p},
          {"(. \"FOO\" \"length().MIN_VALUE.MAX_VALUE\" )", Integer.MAX_VALUE, true, null, null, p},
          // arguments
          {
            "(. \"\" \"format\" (LIST \"foo=%d m=%s\" (. (LIST 1 \"qqq\") \"toArray()\")))",
            "foo=1 m=qqq",
            true,
            null,
            null,
            p
          },
          // type specs
          {
            "(LET ((M (HASHMAP))(OT \"Object\")) (. M \"put\" (LIST 2 3L) (LIST OT OT) ) M)",
            new HashMap<Object, Object>() {
              {
                this.put(2, 3L);
              }
            },
            true,
            null,
            null,
            p
          },
          {
            "(.S \"java.lang.Runtime\" \"getRuntime()\")", Runtime.getRuntime(), true, null, null, p
          },
          {"(. \"foo\" \"charAt\" (LIST 1) (LIST \"int\"))", 'o', true, null, null, p},
          {
            "(.S \"java.lang.Runtime\" \"getRuntime().availableProcessors()\")",
            Runtime.getRuntime().availableProcessors(),
            true,
            null,
            null,
            p
          },
          {"(.S \"Math\" \"abs\" (LIST -1.0) (LIST \"double\"))", 1.0, true, null, null, p},
          {"(.N \"java.util.LinkedList\")", new LinkedList<Object>(), false, null, null, p},
          {
            "(.N \"java.util.Locale\" (LIST \"en\" \"US\"))",
            java.util.Locale.forLanguageTag("en-US"),
            true,
            null,
            null,
            p
          },
          {
            "(.N \"java.util.GregorianCalendar\" (LIST 1976 01 02) "
                + "(LIST \"int\" \"int\" \"int\"))",
            new java.util.GregorianCalendar(1976, 01, 02),
            true,
            null,
            null,
            p
          },
          {"(CLASS \"java.lang.String\")", String.class, true, null, null, p},
          {"(CLASS (QUOTE java.lang.String))", String.class, true, null, null, p},
          {"(TYPE-OF \"FOO\")", String.class, true, null, null, p},
          {"(TYPEP 1 (CLASS (QUOTE java.lang.Integer)))", true, true, null, null, p},
          {"(TYPEP 1 (CLASS (QUOTE Number)))", true, true, null, null, p},
          {"(TYPEP () (QUOTE java.util.List))", true, true, null, null, p},
          {"(TYPEP \"\" \"String\")", true, true, null, null, p},
          {"(THROW \"bla\")", null, true, new ExecutionException(null, "bla"), null, p},
          {
            "(THROW (.N \"java.lang.RuntimeException\" (LIST \"bla\")))",
            null,
            true,
            new ExecutionException(null, new RuntimeException("bla")),
            null,
            p
          },

          // {"(THROW \"foo\" (.N \"java.lang.RuntimeException\" (LIST \"bla\")))",  null, true,
          //  new ExecutionException(null, "foo", new RuntimeException("bla")), null, p},

          {
            "(MAP (LAMBDA (x) (+ x 1)) )",
            null,
            true,
            new ExecutionException(
                null, new RuntimeException("At least one sequence must be provided")),
            null,
            p
          },
          {
            "(MAPPROD (LAMBDA (x) (+ x 1)) )",
            null,
            true,
            new ExecutionException(
                null, new RuntimeException("At least one sequence must be provided")),
            null,
            p
          },
          {"(MAP (LAMBDA (x) (+ x 1)) (LIST 1 2 3))", list(2, 3, 4), true, null, null, p},
          {
            "(MAP (LAMBDA (x y) (+ x (* 2 y))) (LIST 1 2 3) (LIST 4 5 6))",
            list(9, 12, 15),
            true,
            null,
            null,
            p
          },
          {
            "(MAP (LAMBDA (x y) (+ x (* 2 y))) (LIST 1 2 3 4 5) (LIST 4 5 6))",
            list(9, 12, 15),
            true,
            null,
            null,
            p
          },
          {"(MAP (LAMBDA (x y) (+ x (* 2 y))) (LIST) (LIST 4 5 6))", list(), false, null, null, p},
          {
            "(LET ((f (LAMBDA (x) (+ x 1))) (l (LIST 1 2 3))) (MAP f l))",
            list(2, 3, 4),
            true,
            null,
            null,
            p
          },
          {
            "(MAP (LAMBDA (x y z) (+ x y z))  (LIST 1 2 3) (LIST 0 10) (LIST))",
            list(),
            false,
            null,
            null,
            p
          },
          {
            "(MAP (FUNCTION +)  (LIST 1 2 3) (LIST 4 5 6) (LIST 7 8 9))",
            list(12, 15, 18),
            true,
            null,
            null,
            p
          },
          {"(MAPPROD (LAMBDA (x) (+ x 1)) (LIST 1 2 3))", list(2, 3, 4), true, null, null, p},
          {
            "(MAPPROD (LAMBDA (x y) (+ x y))  (LIST 1 2 3) (LIST 0 10 20 30))",
            list(1, 2, 3, 11, 12, 13, 21, 22, 23, 31, 32, 33),
            true,
            null,
            null,
            p
          },
          {
            "(MAPPROD (LAMBDA (x y z) (+ x y z))  (LIST 1 2 3) (LIST 0 10) (LIST 100))",
            list(101, 102, 103, 111, 112, 113),
            true,
            null,
            null,
            p
          },
          {
            "(MAPPROD (LAMBDA (x y z) (+ x y z))  (LIST 1 2 3) (LIST 0 10) (LIST))",
            list(),
            false,
            null,
            null,
            p
          },
          {
            "(MAPPROD (LAMBDA (x y z) (+ x y z))  (LIST 1 2 3) (LIST 0 10) (LIST))",
            list(),
            false,
            null,
            null,
            p
          },
          {
            "(MAPPROD (LAMBDA (x y z) (+ x y z))  (LIST) (LIST 0 10) (LIST 1))",
            list(),
            false,
            null,
            null,
            p
          },
          {"(MAPPROD (LAMBDA (x ) (+ 1 x))  (LIST))", list(), false, null, null, p},
          {"(MAPPROD (LAMBDA (x ) (+ 1 x))  NIL)", list(), false, null, null, p},
          {"(MAPPROD (LAMBDA (x y) (+ x y))  (LIST 1) (LIST 1))", list(2), true, null, null, p},
          {
            "(MAPPROD (LAMBDA (X Y Z) (STR X Y Z)) \"ABC\"  (LIST 1 2 3) (APPEND (MAKE-ARRAY :size 0)"
                + " (LIST \"X\" \"Y\" \"Z\")))",
            list(
                "A1X", "B1X", "C1X", "A2X", "B2X", "C2X", "A3X", "B3X", "C3X", "A1Y", "B1Y", "C1Y",
                "A2Y", "B2Y", "C2Y", "A3Y", "B3Y", "C3Y", "A1Z", "B1Z", "C1Z", "A2Z", "B2Z", "C2Z",
                "A3Z", "B3Z", "C3Z"),
            true,
            null,
            null,
            p
          },
          {"(PROGN (FSET (QUOTE p3) (LAMBDA (x) (+ 3 x)))  (p3 1))", 4, true, null, null, p},
          {"(FILTER (LAMBDA (x) (> x 0))  (LIST -2 -1 0 1 2))", list(1, 2), true, null, null, p},
          {"(PROGN (DEFUN INCR (x) (+ 1 x))  (INCR 1))", 2, true, null, null, p},
          {"(PROGN (DEFUN SELF (x) x)  (SELF 1))", 1, true, null, null, p},
          {"(PROGN (DEFUN SELF (x) x)  (SELF NIL))", null, false, null, null, p},
          {"(PROGN (DEFUN INCR (x) (+ 1 x))  (FUNCALL (FUNCTION INCR) 5))", 6, true, null, null, p},
          {"(FUNCALL (FUNCTION +) 1 2 3)", 6, true, null, null, p},
          {"(FUNCALL (FUNCTION +) 1 (+ 1 1)  3)", 6, true, null, null, p},
          {"(APPLY (FUNCTION +) 1 2 3 4)", 10, true, null, null, p},
          {"(APPLY (FUNCTION +) 1 (LIST 2 3 4))", 10, true, null, null, p},
          {"(APPLY (FUNCTION +) (LIST 1 2 3 4))", 10, true, null, null, p},
          {"(APPLY (FUNCTION +) )", 0, false, null, null, p},
          {"(FUNCALL (SYMBOL-FUNCTION \"+\") 1 2 3)", 6, true, null, null, p},
          {"(APPLY (SYMBOL-FUNCTION \"+\") 1 2 3 4)", 10, true, null, null, p},
          {
            "(APPLY (LAMBDA (&KEY a b c) (LIST a b c)) :a 1 :b 2 ())",
            list(1, 2, null),
            true,
            null,
            null,
            p
          },
          {
            "(APPLY (LAMBDA (&KEY a b &REST r) (LIST a b r)) :a 1 2 3 4)",
            list(1, null, list(2, 3, 4)),
            true,
            null,
            null,
            p
          },

          // call must be dynamic
          {
            "(PROGN (DEFUN zqq () 1) (DEFUN zpp () (zqq)) (DEFUN zqq () 2) (zpp))",
            2,
            true,
            null,
            null,
            p
          },
          {"(REVERSE (LIST 7 5 2 3 4 -1 0))", list(0, -1, 4, 3, 2, 5, 7), true, null, null, p},
          {"(REVERSE (LIST 2))", list(2), true, null, null, p},
          {"(REVERSE (LIST NIL))", list((Object) null), true, null, null, p},
          {"(REVERSE (LIST))", list(), false, null, null, p},
          {"(REVERSE NIL)", null, false, null, null, p},
          {
            "(LET ((s (LIST 1 2 3))) (LIST (REVERSE s) s))",
            list(list(3, 2, 1), list(1, 2, 3)),
            true,
            null,
            null,
            p
          },
          {
            "(LET ((s (LIST 1 2 3))) (LIST (REVERSE! s) s))",
            list(list(3, 2, 1), list(3, 2, 1)),
            true,
            null,
            null,
            p
          },
          {"(SORT (LIST 7 5 2 3 4 -1 0))", list(-1, 0, 2, 3, 4, 5, 7), true, null, null, p},
          {
            "(SORT (LAMBDA (a b) (- a b)) (LIST 7 5 2 3 4 -1 0))",
            list(-1, 0, 2, 3, 4, 5, 7),
            true,
            null,
            null,
            p
          },
          {"(REDUCE (FUNCTION +) (LIST))", 0, false, null, null, p},
          {"(REDUCE (FUNCTION +) (LIST 1))", 1, true, null, null, p},
          {"(REDUCE (FUNCTION +) (LIST 1 2))", 3, true, null, null, p},
          {"(REDUCE (FUNCTION +) 1 (LIST))", 1, true, null, null, p},
          {"(REDUCE (FUNCTION +) 1 (LIST 2 3))", 6, true, null, null, p},
          {"(REDUCE (FUNCTION +) 1 (LIST 2 3))", 6, true, null, null, p},
          {"(REDUCE (FUNCTION +) (LIST 1 2 3 4 5))", 15, true, null, null, p},
          {"(REDUCE (FUNCTION +) 1 (LIST 1 2 3 4 5))", 16, true, null, null, p},
          {
            "(LET ((l (LIST 7 5 1))) (LIST l (SORT l)))",
            list(list(7, 5, 1), list(1, 5, 7)),
            true,
            null,
            null,
            p
          },
          {
            "(LET ((l (LIST 7 5 1))) (LIST l (SORT! l)))",
            list(list(1, 5, 7), list(1, 5, 7)),
            true,
            null,
            null,
            p
          },
          // FIXME: tests for other seq. types sort
          // comments
          {";; (\r\n 3 ; 1 2 \n ;; )", 3, true, null, null, p},

          // recursive defun
          {
            "(PROGN (DEFUN fact (x) (IF x (* x (fact (- x 1))) 1)) (fact 5))",
            120,
            true,
            null,
            null,
            p
          },
          // mutually recursive defun
          //ONLY,
          { "(PROGN "
            + "(DEFUN isodd (x) (IF (= 0 x) FALSE (iseven (- x 1))))"
            + "(DEFUN iseven (x) (IF (= 0 x) TRUE (isodd (- x 1))))"
            + "(LIST "
            + "    (iseven 0) (iseven 1) (iseven 2) (iseven 3)"
            + "    (isodd 0)  (isodd  1) (isodd  2) (isodd  3)))" ,
            list(true, false, true, false, false, true, false, true),
            true,
            null,
            null,
            p},
          {"(RE-PATTERN \"^F[0-9]$\")", Pattern.compile("^F[0-9]$"), true, null, null, p},
          {"(RE-GLOB \"g*b\")", Pattern.compile("g.*b"), true, null, null, p},
          {
            "(RE-MATCHER (RE-PATTERN \"^F[0-9]$\") \"BLA\")",
            Pattern.compile("^F[0-9]$").matcher("BLA"),
            true,
            null,
            null,
            p
          },
          {"(RE-MATCHES (RE-MATCHER (RE-PATTERN \"^F[0-9]$\") \"F1\"))", "F1", true, null, null, p},
          {
            "(RE-MATCHES (RE-MATCHER (RE-PATTERN \"^(F)([0-9])$\") \"F1\"))",
            list("F1", "F", "1"),
            true,
            null,
            null,
            p
          },
          {
            "(RE-MATCHES (RE-MATCHER (RE-PATTERN \"^(F)([0-9])$\") \"BLA\"))",
            null,
            false,
            null,
            null,
            p
          },
          {"(RE-MATCHES (RE-PATTERN \"^F[0-9]$\") \"F1\")", "F1", true, null, null, p},
          {
            "(RE-MATCHES (RE-PATTERN \"^(F)([0-9])$\") \"F1\")",
            list("F1", "F", "1"),
            true,
            null,
            null,
            p
          },
          {"(RE-MATCHES (RE-PATTERN \"^(F)([0-9])$\") \"BLA\")", null, false, null, null, p},
          {"(RE-MATCHES (RE-PATTERN \"^bla$\") \"BLA\")", null, false, null, null, p},
          {"(RE-MATCHES (RE-PATTERN \"^bla$\" \"i\") \"BLA\")", "BLA", true, null, null, p},
          {"(RE-FIND (RE-PATTERN \"(F)([0-9])\") \"BLA\")", null, false, null, null, p},
          {
            "(RE-FIND (RE-PATTERN \"(F)([0-9])\") \"F1\")",
            list("F1", "F", "1"),
            true,
            null,
            null,
            p
          },
          {
            "(DWIM-MATCHES \"a1231b\" (RE-PATTERN \"[0-9]\"))",
            list("1", "2", "3", "1"),
            true,
            null,
            null,
            p
          },
          {"(DWIM-MATCHES \"a1231b\" \"1\")", list("1"), true, null, null, p},
          {"(DWIM-MATCHES NIL NIL)", list((Object) null), true, null, null, p},
          {"(DWIM-MATCHES 1 1)", list(1), true, null, null, p},
          {"(DWIM-MATCHES 1 1.0)", list(1), true, null, null, p},
          {"(DWIM-MATCHES 1.0 1)", list(1.0), true, null, null, p},
          {
            "(SEARCH (LIST \"foo\" \"bar\" \"baz\" \"boog\") (EQUAL _ \"foo\"))",
            list("foo"),
            true,
            null,
            null,
            p
          },
          {
            "(SEARCH (LIST \"foo\" \"bar\" \"baz\" \"boog\") \"foo\")",
            list("foo"),
            true,
            null,
            null,
            p
          },
          // FIXME: no common regex syntax for sexp and lisp reader
          {
            "(SEARCH (LIST (HASHMAP \"foo\" \"bar\" \"baz\" \"boog\") (HASHMAP \"foo\""
                + " \"bar2\" \"baz\" \"boog2\")) (EQUAL foo \"bar\") )",
            list(map("foo", "bar", "baz", "boog")),
            true,
            null,
            null,
            p
          },
          {"(SEARCH 5 (> _ 2))", list(5), true, null, null, p},
          {"(SEARCH 2 (> _ 2))", list(), false, null, null, p},
          {"(SEARCH NIL (> _ 2))", list(), false, null, null, p},
          // {"(SEARCH (LIST \"foo\" \"bar\" \"baz\" \"boog\") \"foo\")", list("foo"), true,
          // null,null,p},

          {"(GET-IN  NIL        NIL)", null, false, null, null, p},
          {"(GET-IN  NIL        NIL \"Nope\")", null, false, null, null, p},
          {"(GET-IN  (LIST 1 2) NIL \"Nope\")", list(1, 2), true, null, null, p},
          {
            "(GET-IN  (LIST)     0)",
            null,
            false,
            new ExecutionException(
                "Cannot use the provided class java.lang.Integer value as list of indexes"),
            null,
            p
          },
          {
            "(GET-IN  (LIST)     0 \"Nope\")",
            null,
            false,
            new ExecutionException(
                "Cannot use the provided class java.lang.Integer value as list of indexes"),
            null,
            p
          },
          {"(GET-IN  (LIST 10)  (LIST 0))", 10, true, null, null, p},
          {"(GET-IN  (LIST 10)  (LIST 0) \"Nope\")", 10, true, null, null, p},
          {"(GET-IN  (LIST 10)  (LIST 1) \"Nope\")", "Nope", true, null, null, p},
          {"(GET-IN  (LIST 10)  () \"Nope\")", list(10), true, null, null, p},
          {"(GET-IN  (HASHMAP \"foo\" 111 )  (LIST \"foo\") \"Nope\")", 111, true, null, null, p},
          {
            "(GET-IN  (HASHMAP \"foo\" 111 )  (LIST \"boo\") \"Nope\")", "Nope", true, null, null, p
          },
          {
            "(GET-IN  (HASHMAP \"foo\" (HASHMAP 11 22 ))  (LIST \"foo\" 11) \"Nope\")",
            22,
            true,
            null,
            null,
            p
          },
          {
            "(GET-IN  (HASHMAP \"foo\" (HASHMAP 11 \"AQ\" ))  (LIST \"foo\" 11 1) \"Nope\")",
            'Q',
            true,
            null,
            null,
            p
          },
          // PUT-IN!
          {
            "(LET ((m (HASHMAP \"a\" 1 \"b\" 2 \"c\" (HASHMAP \"d\" 1 \"e\" 2)))) "+
            "       (PUT-IN! m (LIST \"f\") 3) m)",
            map("a",1,"b",2,"f",3,"c",(map("d",1,"e",2))),
            true,
            null,
            null,
            p
          },
          {
            "(LET ((m (HASHMAP \"a\" 1 \"b\" 2 \"c\" (HASHMAP \"d\" 1 \"e\" 2)))) "+
            "       (PUT-IN! m (LIST \"c\" \"f\") 3) m)",
            map("a",1,"b",2,"c",(map("d",1,"e",2, "f", 3))),
            true,
            null,
            null,
            p
          },
          {
            "(LET ((m (LIST 1  2 (LIST 3 4)))) "+
            "       (PUT-IN! m (LIST 2 1) 5) m)",
            list(1,2,(list(3,5))),
            true,
            null,
            null,
            p
          },
          {
            "(LET ((m (LIST))) "+
            "       (PUT-IN! m (LIST 0) 1) m)",
            list(1),
            true,
            null,
            null,
            p
          },
          {
            "(LET ((m (HASHMAP))) (PUT-IN! m (LIST 1 2 3) 1 (HASHMAP)) m)",
            map(1,map(2,map(3,1))),
            true,
            null,
            null,
            p
          },
          {
            "(LET ((L (LIST))) (PUT-IN! L (LIST 0 0 0) 1 (LIST)) L)",
            list(list(list(1))),
            true,
            null,
            null,
            p
          },
          {
            "(LET ((L (MAKE-ARRAY :size 1))) (PUT-IN! L (LIST 0 0) 1 (MAKE-ARRAY :size 1)) L)",
            Utils.arrayOfObjects((Object)Utils.arrayOfObjects(1)),
            true,
            null,
            null,
            p
          },
          // PUT-IN
          {
            "(LET ((m (HASHMAP \"a\" 1 \"b\" 2 \"c\" (HASHMAP \"d\" 1 \"e\" 2))) "+
            "      (n (PUT-IN m (LIST \"f\") 3)))  (LIST m n))",
            list(map("a",1,"b",2,"c",(map("d",1,"e",2))),
                 map("a",1,"b",2,"f",3,"c",(map("d",1,"e",2)))),
            true,
            null,
            null,
            p
          },
          {
            "(LET ((m (HASHMAP \"a\" 1 \"b\" 2 \"c\" (HASHMAP \"d\" 1 \"e\" 2))) "+
            "      (n (PUT-IN m (LIST \"c\" \"f\") 3))) (LIST m n))",
            list(map("a",1,"b",2,"c",(map("d",1,"e",2))),
                 map("a",1,"b",2,"c",(map("d",1,"e",2, "f", 3)))),
            true,
            null,
            null,
            p
          },
          {
            "(LET ((m (LIST 1  2 (LIST 3 4))) "+
            "      (n (PUT-IN m (LIST 2 1) 5))) (LIST m n))",
            list(list(1,2,list(3,4)) , list(1,2,(list(3,5)))),
            true,
            null,
            null,
            p
          },
          {
            "(LET ((m (LIST)) "+
            "      (n (PUT-IN m (LIST 0) 1))) (LIST m n))",
            list(list(),list(1)),
            true,
            null,
            null,
            p
          },
          {
            "(LET ((m (HASHMAP)) "
            + "    (n (PUT-IN m (LIST 1 2 3) 1 (HASHMAP)))) (LIST m n))",
            list(map(),  map(1,map(2,map(3,1)))),
            true,
            null,
            null,
            p
          },
          {
            "(LET ((L (LIST)) "
            +"     (n (PUT-IN L (LIST 0 0 0) 1 (LIST)))) (LIST L n))",
            list(list(),  list(list(list(1)) )),
            true,
            null,
            null,
            p
          },
          {
            "(LET ((L (MAKE-ARRAY :size 1))"
            +"    (n (PUT-IN L (LIST 0 0) 1 (MAKE-ARRAY :size 1)))) (LIST L n))",
            list(Utils.arrayOfObjects((Object)null),
                 Utils.arrayOfObjects((Object)Utils.arrayOfObjects(1))),
            true,
            null,
            null,
            p
          },
          {
            "(LET ((L (MAKE-ARRAY (HASHMAP 1 2)))"
            +"     (n (PUT-IN L (LIST 0 1) 3))) (LIST L n))",
            list(Utils.arrayOfObjects(map(1,2)),
                 Utils.arrayOfObjects(map(1,3))),
            true,
            null,
            null,
            p
          },
          
          {"(GET  (HASHMAP \"foo\" \"bar\")  \"foo\"  \"Nope\")", "bar", true, null, null, p},
          {"(GET  (HASHMAP \"foo\" \"bar\")  \"mmm\"  \"Nope\")", "Nope", true, null, null, p},
          {"(GET  (HASHMAP \"foo\" \"bar\")  \"mmm\")", null, false, null, null, p},
          {"(GET  testbean \"name\")", testBean.getName(), true, null, beanVars, p},
          {"(GET  testbean \"nosuchkey\")", null, false, null, beanVars, p},
          {"(GET  testbean \"name\" \"Nope\")", testBean.getName(), true, null, beanVars, p},
          {"(GET  testbean \"nosuchkey\" \"Nope\")", "Nope", true, null, beanVars, p},
          {"(GET  (LIST 11 12)  1 \"Nope\")", 12, true, null, beanVars, p},
          {"(GET  (LIST 11 12)  2 \"Nope\")", "Nope", true, null, beanVars, p},
          {"(GET  (LIST 11 12)  2 )", null, false, null, beanVars, p},
          {"(GET  (LIST 11 12)  \"sss\" \"Nope\")", "Nope", true, null, beanVars, p},
          {"(GET  \"ASDF\"  1)", 'S', true, null, null, p},
          {"(GET  \"ASDF\"  4)", null, false, null, null, p},
          {"(GET  \"ASDF\"  4 \"Nope\")", "Nope", true, null, null, p},
          {"(GET  null  4 \"Nope\")", "Nope", true, null, null, p},
          {"(GET  null  4)", null, false, null, null, p},
          {"(ASSOC  (HASHMAP) 1 2)", map(1, 2), true, null, null, p},
          {"(ASSOC  (HASHMAP) 1 2 3 4)", map(1, 2, 3, 4), true, null, null, p},
          {"(ASSOC  (HASHMAP) 1 2 3)", map(1, 2, 3, null), true, null, null, p},
          {"(ASSOC  (HASHMAP 1 2) 3 4 5 6)", map(1, 2, 3, 4, 5, 6), true, null, null, p},
          {"(PROGN (SETQ m (HASHMAP)) (ASSOC m 2 3) m)", map(), true, null, null, p},
          {"(ASSOC!  (HASHMAP) 1 2)", map(1, 2), true, null, null, p},
          {"(ASSOC!  (HASHMAP) 1 2 3 4)", map(1, 2, 3, 4), true, null, null, p},
          {"(ASSOC!  (HASHMAP) 1 2 3)", map(1, 2, 3, null), true, null, null, p},
          {"(ASSOC!  (HASHMAP 1 2) 3 4 5 6)", map(1, 2, 3, 4, 5, 6), true, null, null, p},
          {"(PROGN (SETQ m (HASHMAP)) (ASSOC! m 2 3) m)", map(2, 3), true, null, null, p},
          {
            "(LET ((a (MAKE-ARRAY :size 1 :elementType (QUOTE int)))) (PUT! a 0 8) (GET-IN a (LIST 0)))",
            8,
            true,
            null,
            null,
            p
          },
          {
            "(LET ((M (RE-MATCHER (RE-PATTERN \"(F)([0-9])\") \"F1\"))) (LIST (RE-FIND M) (RE-FIND"
                + " M)))",
            list(list("F1", "F", "1"), null),
            true,
            null,
            null,
            p
          },
          {
            "(LET ((M (RE-MATCHER (RE-PATTERN \"(F)([0-9])\") \"F1\"))) (LIST (RE-FIND M)"
                + " (RE-GROUPS M) (RE-GROUPS M)))",
            list(list("F1", "F", "1"), list("F1", "F", "1"), list("F1", "F", "1")),
            true,
            null,
            null,
            p
          },
          {
            "(LET ((S (RE-SEQ (RE-PATTERN \"F[0-9]\") \"F1F2F3\"))) (APPEND () S S))",
            list("F1", "F2", "F3", "F1", "F2", "F3"),
            true,
            null,
            null,
            p
          },
          {
            "(LET ((S (RE-SEQ (RE-MATCHER (RE-PATTERN \"F[0-9]\") \"F1F2F3\")))) (APPEND () S S))",
            list("F1", "F2", "F3"),
            true,
            null,
            null,
            p
          },
          {
            "(LET ((M (RE-MATCHER (RE-PATTERN \"F[0-9]\") \"F1F2F3\"))(S (RE-SEQ M)) (A ()))"
                + " (APPEND! A S) (. M \"reset()\") (APPEND! A S) A)",
            list("F1", "F2", "F3", "F1", "F2", "F3"),
            true,
            null,
            null,
            p
          },
          {"(STR \"FOO \" 5 \" BAR\")", "FOO 5 BAR", true, null, null, p},
          {"(STR \"FOO \" 5 \" BAR\" NIL)", "FOO 5 BAR", true, null, null, p},
          {"(STR)", "", false, null, null, p},
          {"(STR NIL)", "", false, null, null, p},
          {"(FORMAT \"a=%d b=%s\" 1 \"foo\")", "a=1 b=foo", true, null, null, p},
          {"(FORMAT \"foo\")", "foo", true, null, null, p},
          {"(STRING-BUILDER)", new StringBuilder(), false, null, null, p},
          {"(STRING-BUILDER \"foo\")", new StringBuilder("foo"), true, null, null, p},
          {"(STRING-BUILDER \"foo\" \"bar\" )", new StringBuilder("foobar"), true, null, null, p}, 
          {"(STRING-BUFFER)", new StringBuffer(), false, null, null, p},
          {"(STRING-BUFFER \"foo\")", new StringBuffer("foo"), true, null, null, p},
          {"(STRING-BUFFER \"foo\" \"bar\")", new StringBuffer("foobar"), true, null, null, p},
          {"(LET ((M (HASHMAP 333 222)) (l (APPEND (LIST) M)) (E (GET l 0))) (GET E \"key\"))", 333, true, null, null, p},
          {"(LET ((M (HASHMAP 333 222)) (l (APPEND (LIST) M)) (E (GET l 0))) (GET E \"value\"))", 222, true, null, null, p},
          {"(SYMBOL \"fooo\")", new Symbol("fooo"), true, null, null, p},
          {
            "(WITH-BINDINGS (HASHMAP \"a\"   1   \"B\"    2  \"c\" 3 \"D\" 4) (+ a B c D))",
            10,
            true,
            null,
            null,
            p
          },
          {
            "(WITH-BINDINGS (BEAN testbean) (LIST name surName age accepted children))",
            list(
                testBean.getName(),
                testBean.getSurName(),
                testBean.getAge(),
                testBean.isAccepted(),
                testBean.getChildren()),
            true,
            null,
            beanVars,
            p
          },
          {
            "(WITH-BINDINGS (BEAN testbean \"a-\" \"-b\") (LIST a-name-b a-surName-b a-age-b"
                + " a-accepted-b a-children-b))",
            list(
                testBean.getName(),
                testBean.getSurName(),
                testBean.getAge(),
                testBean.isAccepted(),
                testBean.getChildren()),
            true,
            null,
            beanVars,
            p
          },
          {
            "(WITH-BINDINGS (BEAN testbean NIL \"-b\") (LIST name-b surName-b age-b accepted-b"
                + " children-b))",
            list(
                testBean.getName(),
                testBean.getSurName(),
                testBean.getAge(),
                testBean.isAccepted(),
                testBean.getChildren()),
            true,
            null,
            beanVars,
            p
          },
          {
            "(SELECT-KEYS testbean (LIST \"name\" \"surName\"))",
            Utils.map("name", testBean.getName(), "surName", testBean.getSurName()),
            true,
            null,
            beanVars,
            p
          },
          {
            "(SELECT-KEYS testbean (LIST \"name\" \"surName\" \"nosuchkey\"))",
            Utils.map("name", testBean.getName(), "surName", testBean.getSurName()),
            true,
            null,
            beanVars,
            p
          },
          {
            "(SELECT-KEYS (HASHMAP \"name\" \"Scott\" \"surName\" \"D.\" \"familyName\" \"Adams\")"
                + " (LIST \"name\" \"surName\"))",
            Utils.map("name", "Scott", "surName", "D."),
            true,
            null,
            beanVars,
            p
          },
          {
            "(SELECT-KEYS (HASHMAP \"name\" \"Scott\" \"surName\" \"D.\" \"familyName\" \"Adams\")"
                + " (LIST \"name\" \"surName\" \"noShuchKey\"))",
            Utils.map("name", "Scott", "surName", "D."),
            true,
            null,
            beanVars,
            p
          },
          {
            "(SELECT-KEYS (LIST \"zero\" \"one\" \"two\" \"three\") (LIST 0 2))",
            Utils.map(0, "zero", 2, "two"),
            true,
            null,
            null,
            p
          },
          
          {quine, parsedQuine, true, null, null, p},
          {"((LAMBDA (x y) (SETV x y) x) 1 2)", 2, true, null, null, p},
          {"(SETQ)", null, false, null, null, p},
          {"(SETV)", null, false, null, null, p},
          {"(SETV x 1)", 1, true, null, null, p},
          {"(SETQ x 1)", 1, true, null, null, p},
          {"(PROGN (SETQ a 1 b 2) (LIST a b))", list(1, 2), true, null, null, p},
          {"(PROGN (SETV a 1 b 2) (LIST a b))", list(1, 2), true, null, null, p},
          {"(LET ((x 1)) (SETV x 2) x)", 2, true, null, null, p},
          {"(LET ((x 1)) (SETQ x 2) x)", 2, true, null, null, p},
          {"(LET ((x 1)) ((LAMBDA () (SETQ x 2))) x)", 2, true, null, null, p},
          {"(PROGN (LET () (SETQ x 3)) x)", 3, true, null, null, p},
          {
            "(PROGN (LET () (SETV x 3)) x)",
            null,
            true,
            new ExecutionException(
                null, new RuntimeException("variable 'x' does not exist in this context")),
            null,
            p
          },
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
          {
            "(LET ((r 0)) ((LAMBDA (&LAZY x y z) (IF x y z)) 0 (SETV r 1) (SETV r 2)) r)",
            2,
            true,
            null,
            null,
            p
          },
          {
            "(PROGN (DEFUN LKA (&LAZY &KEY a b c) (IF a (IF b (IF c c false) false) false))  (LET"
                + " ((d 0)) (LKA :a (+ 1 d) :b (NOT (= d 0)) :c (/ 1 d))))",
            false,
            false,
            null,
            null,
            p
          },
          {
            "(PROGN (DEFUN LA (&LAZY a b c) (IF a (IF b (IF c c false) false) false))  (LET ((d 0))"
                + " (LA (+ 1 d) (NOT (= d 0)) (/ 1 d))))",
            false,
            false,
            null,
            null,
            p
          },
          {
            "(PROGN (DEFUN LOA (&LAZY &OPTIONAL a b c) (IF a (IF b (IF c c false) false) false)) "
                + " (LET ((d 0)) (LOA (+ 1 d) (NOT (= d 0)) (/ 1 d))))",
            false,
            false,
            null,
            null,
            p
          },
          {
            "(PROGN (DEFUN LROA (&LAZY &OPTIONAL a &REQUIRED b c) (IF a (IF b (IF c c false) false)"
                + " false))  (LET ((d 0)) (LROA (+ 1 d) (NOT (= d 0)) (/ 1 d))))",
            false,
            false,
            null,
            null,
            p
          },
          {"(APPLY (FUNCTION +) ((LAMBDA (&LAZY &REST r) r) 1 2 3))", 6, true, null, null, p},
          // ARRAYS
          {"(MAKE-ARRAY :size 3)", new Object[3], true, null, null, p},
          {"(MAKE-ARRAY :size 3 :elementType \"Integer\")", new Integer[3], true, null, null, p},
          // ok as well
          {"(MAKE-ARRAY :size 3 :elementType \"Integer\")", new Object[3], true, null, null, p},
          {"(MAKE-ARRAY :size 3 :elementType \"int\")", new int[3], true, null, null, p},
          {
            "(LET ((a (MAKE-ARRAY :size 3 :elementType \"int\"))) (PUT! a 0 1) a) ",
            Utils.array(1, 0, 0),
            true,
            null,
            null,
            p
          },
          {
            "(LET ((a (MAKE-ARRAY :size 3 :elementType \"short\"))) (PUT! a 0 1) a) ",
            Utils.array((short) 1, (short) 0, (short) 0),
            true,
            null,
            null,
            p
          },
          {
            "(LET ((a (MAKE-ARRAY :size 3 :elementType \"Short\"))) (PUT! a 0 1) a) ",
            new Short[] {1, null, null},
            true,
            null,
            null,
            p
          },
          {
            "(LET ((a (MAKE-ARRAY :size 3 :elementType \"float\"))) (PUT! a 0 1.1) a)  ",
            Utils.array((float) 1.1, (float) 0, (float) 0),
            true,
            null,
            null,
            p
          },
          {
            "(LET ((a (MAKE-ARRAY :size 3 :elementType \"Float\"))) (PUT! a 0 1.1) a)  ",
            new Float[] {1.1F, null, null},
            true,
            null,
            null,
            p
          },
          {
            "(LET ((a (MAKE-ARRAY :size 4 :elementType \"byte\"))) (PUT! a 0 1.1) (PUT! a 1 2) (PUT! a 2"
                + " (CHAR 97)) (PUT! a 3 true) a)",
            Utils.array((byte) 1, (byte) 2, (byte) 97, (byte) 1),
            true,
            null,
            null,
            p
          },
          {
            "(LET ((a (MAKE-ARRAY :size 4 :elementType \"Byte\"))) (PUT! a 0 1.1) (PUT! a 1 2) (PUT! a 2"
                + " (CHAR 97)) (PUT! a 3 true) a)",
            new Byte[] {(byte) 1, (byte) 2, (byte) 97, (byte) 1},
            true,
            null,
            null,
            p
          },
          {"(MAKE-ARRAY :size 1 :elementType \"char\")", new char[1], true, null, null, p},
          {"(MAKE-ARRAY :size 1 :elementType \"Character\")", new Character[1], true, null, null, p},
          {
            "(LET ((a (MAKE-ARRAY :size 3 :elementType \"char\"))) (PUT! a 0 (CHAR 83)) a)",
            Utils.array('S', '\0', '\0'),
            true,
            null,
            null,
            p
          },
          {
            "(LET ((a (MAKE-ARRAY :size 3 :elementType \"Character\"))) (PUT! a 0 (CHAR 83)) a)",
            new Character[] {'S', null, null},
            true,
            null,
            null,
            p
          },
          {
            "(LET ((a (MAKE-ARRAY :size 3 :elementType \"double\"))) (PUT! a 0 83.0) a) ",
            Utils.array(83.0, 0.0, 0.0),
            true,
            null,
            null,
            p
          },
          {
            "(LET ((a (MAKE-ARRAY :size 3 :elementType \"Double\"))) (PUT! a 0 83.0) a) ",
            new Double[] {83.0, null, null},
            true,
            null,
            null,
            p
          },
          {
            "(LET ((a (MAKE-ARRAY :size 7 :elementType \"boolean\"))) (PUT! a 0 true)  (PUT! a 1 false) "
                + " (PUT! a 2 1)  (PUT! a 3 0)  (PUT! a 4 (CHAR 67))  (PUT! a 5 (CHAR 0)) a) ",
            Utils.array(true, false, true, false, true, false, false),
            true,
            null,
            null,
            p
          },
          {
            "(LET ((a (MAKE-ARRAY :size 7 :elementType \"Boolean\"))) (PUT! a 0 true)  (PUT! a 1 false) "
                + " (PUT! a 2 1)  (PUT! a 3 0)  (PUT! a 4 (CHAR 67))  (PUT! a 5 (CHAR 0)) a) ",
            new Boolean[] {true, false, true, false, true, false, null},
            true,
            null,
            null,
            p
          },
          {
            "(LET ((a (MAKE-ARRAY :size 3 :elementType \"int\"))) (PUT! a 0 1)  (PUT! a 0 false)  (PUT!"
                + " a 1 true) a) ",
            Utils.array(0, 1, 0),
            true,
            null,
            null,
            p
          },
          {
            "(LET ((a (MAKE-ARRAY :size 3 :elementType \"Integer\"))) (PUT! a 0 1)  (PUT! a 0 false) "
                + " (PUT! a 1 true) a) ",
            new Integer[] {0, 1, null},
            true,
            null,
            null,
            p
          },
          {"(SEQP ())", true, true, null, null, p},
          {"(SEQP (MAKE-ARRAY :size 2))", true, true, null, null, p},
          {"(SEQP \"\")", true, true, null, null, p},
          {"(SEQP NIL)", false, false, null, null, p},
          {"(SEQP 1)", false, false, null, null, p},

          // {"(MAKE-ARRAY 1 \"int\")", new int[3], true, null, null, p},
          // THREADS
          {
            "(LET ((a (MAKE-ARRAY :size 1)) (t1 (NEW-THREAD (LAMBDA () (PUT! a 0 2)))))  (. t1"
                + " \"start()\") (. t1 \"join()\") (AREF a 0))",
            2,
            true,
            null,
            null,
            p
          },
          {
            "(LET ((a 1) (t1 (NEW-THREAD (LAMBDA () (SETV a 2)))))  (. t1 \"start()\") (. t1"
                + " \"join()\") a)",
            2,
            true,
            null,
            null,
            p
          },
          {"(PROGN (SETF a 11) a)", 11, true, null, null, p},
          {"(PROGN (SETF a (+ 10 1)) a)", 11, true, null, null, p},
          {"(PROGN (SETF a (LIST 1 2)) (SETF (AREF a 1) 11) a)", list(1,11), true, null, null, p},
          {"(PROGN (SETF a (LIST 1 2)) (SETF (AREF a 1) (+ 1 10)) a)", list(1,11), true, null, null, p},
          {"(PROGN (SETF (LIST a b c) (LIST 1 2 3)) (LIST a b c))", list(1,2,3), true, null, null, p},
          {"(PROGN (SETF (LIST a b c d) (LIST 1 2 3)) (LIST a b c d))", list(1,2,3,null), true, null, null, p},
          {"(PROGN (SETF (LIST a b c d) ()) (LIST a b c d))", list(null,null,null,null), true, null, null, p},
          {"(PROGN (SETF (LIST a b c d) NIL) (LIST a b c d))", list(null,null,null,null), true, null, null, p},
          {"(PROGN (SETF (LIST a b c) (LIST 1 2 3 4)) (LIST a b c))", list(1,2,3), true, null, null, p},
          {
            "(PROGN (SETF (LIST a b (LIST c d) e) (LIST 1 2 (LIST 3 4) 5)) (LIST a b c d e))",
            list(1,2,3,4,5), true, null, null, p
          },
          {
            "(PROGN (SETF (LIST a b (LIST c d) e) (LIST 1 2 NIL 5)) (LIST a b c d e))",
            list(1,2,null,null,5), true, null, null, p
          },
          {
            "(PROGN (SETF (LIST a b (LIST c d) e) (LIST 1 2 (LIST 3) 5)) (LIST a b c d e))",
            list(1,2,3,null,5), true, null, null, p
          },
          {
            "(PROGN (SETF (HASHMAP _foo \"foo\" _qaz \"qaz\") (HASHMAP \"foo\" 1 \"qaz\" 2)) (LIST _foo _qaz))",
            list(1,2), true, null, null, p
          },
          {
            "(PROGN (SETF (HASHMAP _foo \"foo\" _qaz \"qaz\") (HASHMAP \"foo\" 1)) (LIST _foo _qaz))",
            list(1,null), true, null, null, p
          },
          {
            "(PROGN (SETF (HASHMAP _foo \"foo\" _qaz \"qaz\") NIL) (LIST _foo _qaz))",
            list(null,null), true, null, null, p
          },

          {
            "(PROGN (SETF (HASHMAP name \"name\" (HASHMAP mother \"mom\" father \"dad\") \"parents\") "
            + "           (HASHMAP \"name\" \"John\" "
            + "              \"parents\" (HASHMAP \"mom\" \"Ann\" \"dad\" \"Jack\"))) "
            + "     (LIST name mother father))",
            list("John","Ann","Jack"), true, null, null, p
          },
          {
            "(PROGN (SETF (LIST (HASHMAP a \"k1\") (HASHMAP b \"k2\")) "
            + "           (LIST (HASHMAP \"k1\" \"v1\") (HASHMAP  \"k2\" \"v2\"))) "
            + "     (LIST a b))",
            list("v1","v2"), true, null, null, p
          },
          {
            "(PROGN (SETF (HASHMAP name :name (LIST book1 book2) :books) "
            + "           (HASHMAP :name \"Jane Austin\" "
            + "                    :books (LIST \"Sense and Sensibility\" \"Pride and Prejustice\" ))) "
            + "     (LIST name book1 book2))",
            list("Jane Austin", "Sense and Sensibility", "Pride and Prejustice"), true, null, null, p
          },
          // FIELDS
          {
            "(DWIM_FIELDS (LIST (HASHMAP \"a\" \"1\" \"b\" \"2\") (HASHMAP \"a\" \"22\")) (LIST (QUOTE a) (QUOTE b)))",
            list(map("a","1", "b", "2"), map("a","22", "b", null)),
            true, null,null, p
          },
          {
            "(DWIM_FIELDS (LIST (HASHMAP \"a\" \"1\" \"b\" \"2\") (HASHMAP \"a\" \"22\")) (LIST \"a\" \"b\"))",
            list(map("a","1", "b", "2"), map("a","22", "b", null)),
            true, null,null, p
          },
          {
            "(DWIM_FIELDS (LIST (HASHMAP \"a\" \"11\" \"b\" \"22\") (HASHMAP \"a\" \"22\")) "
            + "             (LIST (LIST (LIST \"a\" 0) \"aa\") "
            + "                   (LIST (LIST \"b\" 0) \"bb\")))",
            list(map("aa",'1', "bb", '2'), map("aa",'2', "bb", null)),
            true, null,null, p
          },
          {
            "(DWIM_FIELDS (LIST (HASHMAP \"a\" \"11\" \"b\" \"22\") (HASHMAP \"a\" \"22\")) "
            + "       (LIST (LIST (LIST \"a\") \"aa\") "
            + "             (LIST (LIST \"b\") \"bb\")))",
            list(map("aa","11", "bb", "22"), map("aa","22", "bb", null)),
            true, null,null, p
          },
          {
            "(DWIM_FIELDS (LIST (HASHMAP \"a\" (HASHMAP \"x\" \"XX\" \"y\" \"YY\") \"b\" \"22\") "
            + "             (HASHMAP \"a\" (HASHMAP \"x\" \"2XX\" \"y\" \"2YY\"))) "
            + "       (LIST (LIST \"a\" \"aa\" (LIST  \"x\" \"y\"))))",
            list(map("aa",map("y","YY","x","XX")), map("aa",map("y","2YY","x","2XX"))),
            true, null,null, p
          },
          {
            "(DWIM_FIELDS (LIST (HASHMAP \"a\" (HASHMAP \"x\" \"XX\" \"y\" \"YY\") \"b\" \"22\") "
            + "             (HASHMAP \"a\" (HASHMAP \"x\" \"2XX\" \"y\" \"2YY\"))) "
            + "       (LIST (LIST \"a\" NIL (LIST  \"x\" \"y\")) \"b\"))",
            list(map("y","YY","x","XX","b","22"), map("y","2YY","x","2XX","b", null)),
            true, null,null, p
          },
          {
            "(DWIM_FIELDS NIL (LIST \"A\" \"B\"))", null, false, null,null, p
          },
          {
            "(DWIM_FIELDS (LIST NULL) (LIST \"A\" \"B\"))", list((Object)null), true, null,null, p
          },
          {
            "(DWIM_FIELDS (LIST) (LIST \"A\" \"B\"))", list(), false, null,null, p
          },
          {
            "(VERSION \"1.2.3\")", Version.mkSemVersion(1L, 2L, 3L, null, null), true, null, null, p
          },
          {
            "(+ (VERSION \"1.2.3\") (VERSION \"2.3.4\")) ",
            Version.mkSemVersion(3L, 5L, 7L, null, null),
            true,
            null,
            null,
            p
          },
          {
            "(- (VERSION \"1.2.3\") (VERSION \"1.2.3\")) ",
            Version.mkSemVersion(0L, 0L, 0L, null, null),
            false,
            null,
            null,
            p
          },
          {
            "(+  (VERSION \"1.2.3\")  (VERSION \"a.b.c\")) ",
            Version.parseVersion("1a.2b.3c"),
            false,
            null,
            null,
            p
          },
          {"(>  (VERSION \"1.2.3\") (VERSION \"1.2.2\")) ", true, true, null, null, p},
          {"(>= (VERSION \"1.2.3\") (VERSION \"1.2.2\")) ", true, true, null, null, p},
          {"(<  (VERSION \"1.2.3\") (VERSION \"1.2.2\")) ", false, false, null, null, p},
          {"(<= (VERSION \"1.2.3\") (VERSION \"1.2.2\")) ", false, false, null, null, p},
          {"(=  (VERSION \"1.2.3\") (VERSION \"1.2.2\")) ", false, false, null, null, p},
          {"(<  (VERSION \"1.2.2\") (VERSION \"1.2.3\")) ", true, true, null, null, p},
          {"(<= (VERSION \"1.2.2\") (VERSION \"1.2.3\")) ", true, true, null, null, p},
          {"(=  (VERSION \"1.2.2\")  (VERSION \"1.2.3\")) ", false, false, null, null, p},
          {"(>= (VERSION \"1.2.2\")  (VERSION \"1.2.3\")) ", false, false, null, null, p},
          {"(>  (VERSION \"1.2.2\")  (VERSION \"1.2.3\")) ", false, false, null, null, p},
          {
            "(+  (VERSION \"1.2.3-1+2\")  (VERSION \"0.0.0-1+1\")) ",
            Version.mkSemVersion(1L, 2L, 3L, list("2"), list("3")),
            true,
            null,
            null,
            p
          },
          {
            "(+  (VERSION \"1.2.3-1+2\")  (VERSION \"0.0.0-1.8+1.9\")) ",
            Version.mkSemVersion(1L, 2L, 3L, list("2", "8"), list("3", "9")),
            true,
            null,
            null,
            p
          },
          {
            "(< (VERSION \"0.0.8\" ) "
                + "   (VERSION \"0.0.9\") "
                + "   (VERSION \"0.1.0\") "
                + "   (VERSION \"0.1.1\") "
                + "   (VERSION \"0.1.1a\") "
                + "   (VERSION \"1.0.0-alpha\") "
                + "   (VERSION \"1.0.0-alpha.1\") "
                + "   (VERSION \"1.0.0-alpha.beta\") "
                + "   (VERSION \"1.0.0-beta\") "
                + "   (VERSION \"1.0.0-beta.2\") "
                + "   (VERSION \"1.0.0-beta.11\") "
                + "   (VERSION \"1.0.0-rc.1\" ) "
                + "   (VERSION \"1.0.0\"))",
            true,
            true,
            null,
            null,
            p
          },
          {
            "(<= (VERSION \"0.0.8\" ) "
                + "   (VERSION \"0.0.9\") "
                + "   (VERSION \"0.1.0\") "
                + "   (VERSION \"0.1.0+2\") "
                + "   (VERSION \"0.1.1\") "
                + "   (VERSION \"0.1.1a\") "
                + "   (VERSION \"1.0.0-alpha\") "
                + "   (VERSION \"1.0.0-alpha.1\") "
                + "   (VERSION \"1.0.0-alpha.beta\") "
                + "   (VERSION \"1.0.0-beta\") "
                + "   (VERSION \"1.0.0-beta+1\") "
                + "   (VERSION \"1.0.0-beta+2.2\") "
                + "   (VERSION \"1.0.0-beta.2\") "
                + "   (VERSION \"1.0.0-beta.11\") "
                + "   (VERSION \"1.0.0-rc.1\" ) "
                + "   (VERSION \"1.0.0\"))",
            true,
            true,
            null,
            null,
            p
          },
          {
            "(>= (VERSION \"1.0.0\")"
                + "   (VERSION \"1.0.0-rc.1\" ) "
                + "   (VERSION \"1.0.0-beta.11\") "
                + "   (VERSION \"1.0.0-beta.2\") "
                + "   (VERSION \"1.0.0-beta+2.2\") "
                + "   (VERSION \"1.0.0-beta+1\") "
                + "   (VERSION \"1.0.0-beta\") "
                + "   (VERSION \"1.0.0-alpha.beta\") "
                + "   (VERSION \"1.0.0-alpha.1\") "
                + "   (VERSION \"1.0.0-alpha\") "
                + "   (VERSION \"0.1.1a\") "
                + "   (VERSION \"0.1.1\") "
                + "   (VERSION \"0.1.0+2\") "
                + "   (VERSION \"0.1.0\") "
                + "   (VERSION \"0.0.9\") "
                + "  (VERSION \"0.0.8\" )) ",
            true,
            true,
            null,
            null,
            p
          }
        });
    return filterTests(tests);
  }

  @Arguments(spec = {ArgSpec.ARG_LAZY, "a", "b", "c"})
  @Package(name = "tests")
  public static class LAZYAND extends Funcs.FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      Object andVal = true;
      for (int i = 0; i < 3; i++) {
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
    clearLog();
    try {
      log("\n\n TEST #: " + testNum);
      Compiler compiler = new Compiler();
      compiler.setParser(this.parser);
      compiler.usePackages(
          Package.DWIM, Package.FFI, Package.IO, Package.LOOPS, Package.THREADS, "tests");
      compiler.setEnforcePackages(true);
      compiler.addBuiltIn("LAZYAND", LAZYAND.class);
      String inputName = "TEST #" + testNum;
      testNum++;
      log("Parser: " + compiler.getParser());
      log("IN:  " + in);
      ASTNList parsed = parser.parse(new ParseCtx(inputName), in);
      log("AST: " + parsed);

      int i = 0;
      for (ASTN exprObj : parsed) {
        i++;
        log("EXPRESSION " + i + " OF " + parsed.size());
        Assert.assertEquals(1, parsed.size());
        ICompiled exp = compiler.compile(exprObj);
        Compiler.ICtx ctx = (null != this.vars ? compiler.newCtx(this.vars) : compiler.newCtx());
        Object expVal = exp.evaluate(compiler.newBacktrace(), ctx);
        log("TYP: " + ((null == expVal) ? null : expVal.getClass()));
        log("OUT: " + Utils.asString(expVal));
        log("LOG: " + Utils.asBoolean(expVal));
        if (null != expExc) {
          Assert.fail("expected exception containing " + expExc);
        }
        log("XPC: " + Utils.asString(result));
        log("LXPC:" + logResult);
        if (i == parsed.size()) {
          checkResult(expVal, result, 0, null);
          Assert.assertEquals(Utils.asBoolean(expVal), logResult);
        }
      }
    } catch (ExecutionException ex) {

      if (null != expExc) {
        if (areEqual(ex, expExc)) {
          log("Got expected exception: " + ex);
        } else {
          log("Expected exception: " + expExc + ", but got " + ex);
          flushLog();
          ex.printStackTrace(System.out);
          throw ex;
        }
      } else {
        flushLog();
        log("Got unexpected exception: " + ex);
        ex.printStackTrace(System.out);
        throw ex;
      }
    } catch (Throwable ex) {
      flushLog();
      ex.printStackTrace(System.err);
      throw ex;
    }
  }

  @SuppressWarnings("unchecked")
  public void checkResult(Object expVal, Object result, int recLevel, Integer checkIdx) {
    final String msg = "checkResult: rec_level=" + recLevel + ", index=" + checkIdx;
    if ((result instanceof Double) && (expVal instanceof Double)) {
      Assert.assertEquals(msg, (double) result, (double) expVal, 0.0000000000001);
    } else if ((result instanceof Float) && (expVal instanceof Float)) {
      Assert.assertEquals(msg, (float) result, (float) expVal, 0.000001);
    } else if (null != result && null != expVal && expVal.getClass().isArray()
               && result.getClass().isArray()) {
      Assert.assertTrue(msg, Utils.arraysDeepEquals(expVal, result));
    } else if (((result instanceof Pattern) && (expVal instanceof Pattern))
               || ((result instanceof Matcher) && (expVal instanceof Matcher))) {
      // FIXME: string repr. works, but how to do it properly?
      Assert.assertEquals(msg, result.toString(), expVal.toString());
    } else if ((result instanceof StringBuilder) && (expVal instanceof StringBuilder)) {
      Assert.assertEquals(msg, result.toString(), expVal.toString());
    } else if ((result instanceof StringBuffer) && (expVal instanceof StringBuffer)) {
      Assert.assertEquals(msg, result.toString(), expVal.toString());
    } else if ((result instanceof List) && (expVal instanceof List)) {
      final List<Object> expList = (List<Object>)expVal;
      final List<Object> resultList = (List<Object>)result;
      
      Assert.assertEquals(msg, (Object)expList.size(), (Object)resultList.size());
      for (int idx = 0; idx < expList.size(); idx++) {
        checkResult(expList.get(idx), resultList.get(idx), recLevel + 1, idx);
      }
    } else {
      Assert.assertEquals(msg, result, Utils.asObject(expVal));
    }
  
  }
    
  public boolean areEqual(Throwable a, Throwable b) {
    Assert.assertNotNull(a);
    Assert.assertNotNull(b);
    if (!(a.getClass().equals(b.getClass()))) {
      return false;
    }
    String msgA = a.getMessage();
    String msgB = b.getMessage();
    if (!((null == msgA) ? null == msgB : msgA.equals(msgB))) {
      return false;
    }
    Throwable causeA = a.getCause();
    Throwable causeB = b.getCause();
    if (null == causeA && null == causeB) {
      return true;
    }
    return areEqual(causeA, causeB);
  }
}
