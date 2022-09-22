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
import io.opsit.explang.Compiler.ICtx;
import junit.framework.AssertionFailedError;

@RunWith(Parameterized.class)
public class ArgTest extends AbstractTest {
  static int testNum = 0;
    
  public static List<Object> list(Object ... objs) {
    return Utils.list(objs);
  }

  List<Object> argSpec;
  int size;
  int restIdx;
  Throwable exception;
  List  <Object>params;
  List  <Object>readParams;
    
    
  public ArgTest(List<Object> argSpec,
                 // params to be passed
                 List<Object>  params,
                 // expected params to be read, null if exception expected
                 List<Object> readParams,
                 // expected size of list of parameter list
                 // (total,  including parameters in rest list )
                 int size,
                 // index of the rest parameter
                 int restIdx,
                 // expected exception
                 Throwable exception) {
    this.argSpec = argSpec;
    this.params = params;
    this.size = size;
    this.restIdx = restIdx;
    this.readParams = readParams;
    this.exception = exception;
    this.isVerbose = "true".equalsIgnoreCase(System.getenv("EXPLANG_VERBOSE_TEST"));
  }

    
  @Parameters
  public static Collection<Object[]> data() throws Exception {
    Exception tooManyArgs = new InvalidParametersException("Too many arguments given");
    List<Object[]> list= new ArrayList<Object[]>();
    list.addAll(Arrays.asList(new Object[][] {
          { list(),
            list(),
            list(),
            0, Integer.MAX_VALUE, null},
          { list(),
            list(1),
            null,
            0, Integer.MAX_VALUE,
            tooManyArgs},           
          { list("a"),
            list(1),
            list(1),
            1, Integer.MAX_VALUE, null},
          { list("a"),
            list(1, 2),
            null,
            1, Integer.MAX_VALUE,
            tooManyArgs},           
          { list("&OPTIONAL","a"),
            list(1),
            list(1), 1,  Integer.MAX_VALUE,  null},
          { list("&OPTIONAL",list("a")),
            list(1),
            list(1), 1,  Integer.MAX_VALUE,  null},
          { list("&OPTIONAL",list("a",1)),
            list(1),
            list(1), 1,  Integer.MAX_VALUE,  null},

          { list("&OPTIONAL",list("a",1,"sa")),
            list(1),
            list(1), 1,  Integer.MAX_VALUE,  null},  
          { list("&REQUIRED","a"),
            list(1),
            list(1),          
            1,   Integer.MAX_VALUE, null},
          { list("&REQUIRED","a"),
            list(1,2,3),
            list(1),          
            1,   Integer.MAX_VALUE, new InvalidParametersException("Too many arguments given")},            
          { list("a","b"),
            list(1),
            null,
            -1,  Integer.MAX_VALUE,
            new  InvalidParametersException("Insufficient number of arguments given")},
          { list("a","&OPTIONAL","b"),
            list(1),
            list(1, null), 
            2,  Integer.MAX_VALUE, null},
        
          { list("a","&OPTIONAL",
                 "b","&REQUIRED","c"),
            list(1),
            null,
            -1,  Integer.MAX_VALUE,
            new  InvalidParametersException("Insufficient number of arguments given")},
          { list("a",
                 "&OPTIONAL","b",
                 "&REQUIRED","c"),
            list(1,2,3),
            list(1,2,3),
            3,  Integer.MAX_VALUE, null},
          { list("a",
                 "&OPTIONAL","b",
                 "&REQUIRED","c"),
            list(1,3),
            list(1,null, 3),
            3,  Integer.MAX_VALUE, null},
          { list("&OPTIONAL","a","b","c"),
            list(),
            list(null,null,null),
            3,  Integer.MAX_VALUE, null},
          { list("&REST","a"),
            list(),
            list(list()),
            0,  0, null},
          { list("&REST","a"),
            list(1),
            list(list(1)),
            1,  0, null},
          { list("&REST","a"),
            list(1,2,3),
            list(list(1,2,3)),
            3,  0, null},
          { list("a", "&REST","b"),
            list(1),
            list(1, list()),
            1,  1, null},
          { list("a", "&OPTIONAL", "b", "&REST","c"),
            list(1),
            list(1, null, list()),
            2,  2, null},
          { list("a", "&OPTIONAL", "b", "&REST","c"),
            list(1,2,3,4),
            list(1, 2, list(3,4)),
            4,  2, null},
          { list("a", "&REST", "b", "&REQUIRED","c"),
            list(1,2,3,4),
            list(1, list(2,3), 4),
            4,  1, null},
          { list("a", "&REST", "b", "&REQUIRED","c"),
            list(1,2),
            list(1, list(), 2),
            2,  1, null},
          { list("&KEY", "a"),
            list(":a",1),
            list(1),
            1,  Integer.MAX_VALUE, null},
          { list("&KEY", "a","b","c"),
            list(":a",1,":b",2,":c",3),
            list(1,2,3),
            3,  Integer.MAX_VALUE, null},
          { list("&KEY", "a","b","c"),
            list(":c",3,":b",2,":a",1),
            list(1,2,3),
            3,  Integer.MAX_VALUE, null},
          { list("x", "&KEY", "a","b","c"),
            list(10, ":c",3,":b",2,":a",1),
            list(10,1,2,3),
            4,  Integer.MAX_VALUE, null},
            
          { list("&KEY", "a","b","c"),
            list(10, ":d",4,":c",3,":b",2,":a",1),
            list(10,1,2,3),
            4,  Integer.MAX_VALUE, new  InvalidParametersException("expected keyword parameter name, but got 10")},
          { list("x", "&REST", "&KEY", "a","b","c"),
            list(10, ":c",3,":b",2,":a",1),
            list( 10,1,2,3),
            4,  Integer.MAX_VALUE, new  InvalidParametersException("Unexpected keyword parameter :a")}
          ,
          
          { list("x", "&REST","r", "&KEY", "a","b","c"),
            list(10, ":c",3,":b",2,":a",1),
            list(10,list(),1,2,3),
            4,  1, null},
          { list("x", "&REST","r", "&KEY", "a","b","c"),
            list(10, ":c",3,":b",2),
            list(10,list(),null,2,3),
            4,  1, null},
          { list("x", "&REST","r", "&KEY", "a","b","c"),
            list(10, ":c",3,":b",2),
            list(10,list(),null,2,3),
            4,  1, null},
          { list("x", "&REST","r", "&KEY", "a","b","c"),
            list(10, ":c",3,":b",2,":d",4),
            list(10,list(":c",3,":b",2),null,2,3),
            8,  Integer.MAX_VALUE, new  InvalidParametersException("Unexpected keyword parameter :d")},
          { list("x", "&REST","r", "&KEY", "a","b","c"),
            list(10),
            list(10,list(),null,null,null),
            4,  1, null},
          { list("x", "&REST","r", "&KEY"),
            list(10),
            list(10,list()),
            1,  1, null},
          { list("x","&OPTIONAL","y", "&REST","r", "&KEY","k"),
            list(10,9,":k",8),
            list(10,9,list(),8),
            3,  2, null},           
          { list("x","&OPTIONAL","y", "&REST","r", "&KEY","k","&ALLOW-OTHER-KEYS"),
            list(10,9,":o",2,":k",8),
            list(10,9,list(":o",2),8),
            5,  2, null},
          { list("x","&KEY","k","l","&REST","r"),
            list(10,":l",2,":k",8,1,2,3),
            list(10,8,2,list(1,2,3)),
            6,  3, null},
          { list("x","&KEY","k","l","&REST","r"),
            list(10,":l",2,":k",8,1,2,3),
            list(10,8,2,list(1,2,3)),
            6,  3, null},
          { list("x","&KEY","k","l","&REST","r"),
            list(10,":l",2,":k",8),
            list(10,8,2,list()),
            3,  3, null},
          { list("x","&KEY","k","l"),
            list(10,":l",2),
            list(10,null,2),
            3,  Integer.MAX_VALUE, null},
          { list("x","&KEY","k","l","&REST","r"),
            list(10,":l",2,1,2,3),
            list(10,null,2,list(1,2,3)),
            6,  3, null},
          { list("&KEY",list("k",new Funcs.NumberExp(33))),
            list(":k",2),
            list(2),
            1,  Integer.MAX_VALUE, null},
            }
          ));
    return list;
  }

    
  @Test
  public void testArgs() throws Throwable {
    clearLog();
    try {
      log("\n\n TEST #: " + testNum);
      Compiler comp = new Compiler();
      IParser parser = comp.getParser();
      ParseCtx pctx = new ParseCtx(this.getClass().getSimpleName(),
                                   testNum,0,testNum,0);
      ArgSpec spec = new ArgSpec((ASTNList)ArgSpecTest.astnize(argSpec,parser,pctx),
                                 comp);
      log("ArgSpec:  " + argSpec);
      List<ICompiled> testParams = new ArrayList<ICompiled>();
      for (Object param : params) {
        if (param instanceof Integer) {
          testParams.add(new Funcs.NumberExp(((Integer)param).intValue()));
        } else if (param instanceof String) {
          String strParam = (String)param;
          if (strParam.length()>=2 &&
              strParam.startsWith(":")) {
            testParams.add(new Funcs.ObjectExp(new Keyword(strParam)));
          } else {
            testParams.add(new Funcs.VarExp(strParam));
          }
        } else {
          throw new RuntimeException("unsupported type of test parameter "+param);
        }
      }
      ArgList argList = new ArgList(spec, testParams);
      log("ArgList:  " + argList);
      log("readParams:  " + readParams);
      Assert.assertEquals(this.size, argList.totalSize());
      Assert.assertEquals(this.restIdx, argList.restIdx());
      List <Object> parsedParams = new ArrayList<Object>();
      for (ICompiled expr: argList.getParams()) {
        Object obj = recursiveEval(expr, comp.newBacktrace(), comp.newCtx());
        parsedParams.add(obj);
      }
      log("parsedParams:  " + parsedParams);
      Assert.assertEquals(this.readParams, parsedParams);

    } catch (Exception e) {
      log("Exception:  " + e);
      try {
        Assert.assertNotNull(exception);
        Assert.assertEquals(exception.getClass(), e.getClass());
        Assert.assertEquals(exception.getMessage(), e.getMessage());
      } catch (AssertionFailedError ae) {
        flushLog();
        e.printStackTrace(System.err);
        throw ae;
      }
    } catch (Throwable t) {
      throw t;
    } finally {
      flushLog();
    }
  }

  private Object recursiveEval(Object obj, Backtrace backtrace, ICtx ctx) {
    Assert.assertNotNull(obj);
    final Object value =  ((IExpr)obj).evaluate(backtrace, ctx);
    if (value instanceof List) {
      @SuppressWarnings("unchecked")
        final List<Object> listValue = (List<Object>)value;
      final List <Object>valuesList = new ArrayList<Object>();
      for(Object el: listValue) {
        valuesList.add(recursiveEval(el, backtrace, ctx));
      }
      return valuesList;        
    } else if (value instanceof Keyword) {
      return ((Keyword)value).getName();
    } else {
      return value;
    }
  }
}


