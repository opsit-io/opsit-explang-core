package io.opsit.explang.jsr223;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import org.junit.Assert;
import org.junit.Test;

import io.opsit.explang.Utils;

public class JSR223Test {

  protected ScriptEngine getEngine() {
    ScriptEngineManager mgr = new ScriptEngineManager();
    mgr.registerEngineName("explang", new ExplangScriptEngineFactory());
    return  mgr.getEngineByName("explang");
  }
    
  @Test
  public  void testGetEngine() {
    ScriptEngine engine = getEngine();
    Assert.assertNotNull(engine);
  }

  @Test
  public void testPutGet() {
    ScriptEngine engine = getEngine();
    Object someValue = new Object();
    engine.put("some-var", someValue);
    Object retrieved = engine.get("some-var");
    Assert.assertEquals(someValue, retrieved);
  }

  @Test
  public void testEvalStringExpr() throws Exception {
    ScriptEngine engine = getEngine();
    Object result = engine.eval("(+ 1 2)");
    Assert.assertEquals(3, result);
  }


  @Test
  public void testEvalStringExprs() throws Exception {
    ScriptEngine engine = getEngine();
    Object result = engine.eval("(SETV A 2) (SETV B 3) (+ A B)");
    Assert.assertEquals(5, result);
  }


  @Test
  public void testEvalReaderExprs() throws Exception {
    ScriptEngine engine = getEngine();
    String str = "(SETV A 2)\n"+
      "(SETV B 3)\n"+
      "(+ A B)\n";
    Object result = engine.eval(Utils.str2reader(str));
    Assert.assertEquals(5, result);
  }

  @Test
  public void testInvokeBuiltin() throws ScriptException, NoSuchMethodException {
    ScriptEngine engine = getEngine();
    Object result = ((Invocable) engine).invokeFunction("+", 1,2,3);
    Assert.assertEquals(6, result);
  }

  @Test
  public void testInvokeLambda() throws ScriptException, NoSuchMethodException {
    ScriptEngine engine = getEngine();
    engine.eval("(DEFUN FOO (x y z) (+ x (* 2 y) (* 4 z)))");
    Object result = ((Invocable) engine).invokeFunction("FOO", 1,2,3);
    Assert.assertEquals(17, result);
  }
    
  //        //Implementing a Java interface in Lisp
  //        //lispEngine.eval("(defun compare-to (&rest args) 42)");
  //        //Comparable c = ((Invocable) lispEngine).getInterface(java.lang.Comparable.class);
  //        //System.out.println("compareTo: " + c.compareTo(null));
            
  //        //Compilation!
  //        engine.eval("(defmacro slow-compiling-macro (arg) (dotimes (i 1000000) (incf i)) `(print ,arg))");
            
  //        long millis = System.currentTimeMillis();
  //        engine.eval("(slow-compiling-macro 42)");
  //        millis = System.currentTimeMillis() - millis;
  //        System.out.println("interpretation took " + millis);
            
  //        millis = System.currentTimeMillis();
  //        CompiledScript cs = ((Compilable) engine).compile("(slow-compiling-macro 42)");
  //        millis = System.currentTimeMillis() - millis;
  //        System.out.println("compilation took " + millis);
            
  //        millis = System.currentTimeMillis();
  //        cs.eval();
  //        millis = System.currentTimeMillis() - millis;
  //        System.out.println("evaluation took " + millis);

  //        millis = System.currentTimeMillis();
  //        cs.eval();
  //        millis = System.currentTimeMillis() - millis;
  //        System.out.println("evaluation took " + millis);

  //        //Ecc. ecc.
  //    } catch (NoSuchMethodException e) {
  //        e.printStackTrace();
  //    } catch (ScriptException e) {
  //        e.printStackTrace();
  //    }
  // }
    
}


