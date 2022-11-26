package io.opsit.explang;

import static io.opsit.explang.ArgSpec.ARG_OPTIONAL;
import static io.opsit.explang.ArgSpec.ARG_REST;

import io.opsit.explang.Compiler.Eargs;
import io.opsit.explang.Compiler.ICtx;
import io.opsit.explang.Seq.Operation;
import io.opsit.version.Version;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Funcs {
  public abstract static class AbstractExpr implements IExpr, Runnable {
    protected ParseCtx debugInfo;
    protected String name = null;

    /** Run expression in separate Thread. */
    public void run() {
      final Compiler.ICtx ctx = Threads.contexts.remove(Thread.currentThread());
      final Object result = this.evaluate(new Backtrace(), ctx);
      Threads.results.put(Thread.currentThread(), result);
    }

    protected abstract Object doEvaluate(Backtrace backtrace, ICtx ctx);

    public void setName(String name) {
      this.name = name;
    }

    public void setDebugInfo(ParseCtx debugInfo) {
      this.debugInfo = debugInfo;
    }

    public ParseCtx getDebugInfo() {
      return debugInfo;
    }

    protected String getTraceName() {
      return getName();
    }

    protected String getName() {
      if (null == this.name) {
        this.name = this.getClass().getSimpleName();
      }
      return this.name;
    }

    @Override
    public final Object evaluate(final Backtrace backtrace, final ICtx ctx) {
      try {
        backtrace.push(getTraceName(), this.debugInfo, ctx);
        return doEvaluate(backtrace, ctx);
      } catch (ReturnException ex) {
        throw ex;
      } catch (ExecutionException ex) {
        throw ex;
      } catch (Throwable t) {
        throw new ExecutionException(backtrace, t);
      } finally {
        backtrace.pop();
      }
    }
  }

  public abstract static class FuncExp extends AbstractExpr {
    protected ArgList argList;

    @Override
    protected String getTraceName() {
      // final StringBuilder b = new StringBuilder(16);
      // b.append("(").append(getName()).append(")");
      return getName();
    }

    @Override
    public Object doEvaluate(Backtrace backtrace, ICtx ctx) {
      return evalWithArgs(backtrace, this.evaluateParameters(backtrace, ctx));
    }

    protected abstract Object evalWithArgs(Backtrace backtrace, Eargs eargs);

    /** Evaluate function parameters in the execution context. */
    public Eargs evaluateParameters(Backtrace backtrace, ICtx ctx) {
      return argList.evaluateArguments(backtrace, ctx);
    }

    /** Ensure that parametes are valid and can be set for the object. */
    public void checkParamsList(List<ICompiled> params) throws InvalidParametersException {
      // FIXME: what to do with parameters?
      if (null != argList) {
        throw new InvalidParametersException(
            this.getDebugInfo(), "internal exception: parameters already set");
      }
    }

    /** Set parameters for function on compilation stage. */
    @Override
    public void setParams(List<ICompiled> params) throws InvalidParametersException {

      Arguments args = this.getClass().getAnnotation(Arguments.class);
      if (null == args) {
        throw new RuntimeException("argument list not specified for function " + this.getClass());
      }
      String[] specArray = args.spec();
      // FIXME: no compiler - no initForm!
      ArgSpec spec = new ArgSpec(specArray, null);
      this.checkParamsList(params);
      this.argList = new ArgList(spec, params);
    }
  }

  // **** ARITHMETIC FUNCTIONS

  /** Abstract class for addition type arithmetic functions. */
  @Arguments(spec = {ArgSpec.ARG_REST, "args"})
  public abstract static class ABSTRACTADDOP extends FuncExp implements AbstractOp {
    protected abstract Number getNeutral();

    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      Number result = getNeutral();
      Promotion promo = new Promotion();
      List<?> rest = (List<?>) eargs.get(0, backtrace);
      if (rest.size() == 0) {
        promo.promote(result);
      } else {
        for (Object earg : rest) {
          Number arg = Utils.asNumber(earg);
          promo.promote(arg);
          result = promo.callOP(this, result, arg);
        }
      }
      return promo.returnResult(result);
    }
  }

  @Docstring(
             lines = {
               "Compute Sum. ",
               "Computes sum of function arguments performing any necessary type conversions",
               "in the process. If no numbers are supplied, 0 is returned."})
  @Package(name = Package.BASE_ARITHMENTICS)
  public static class ADDOP extends ABSTRACTADDOP {
    @Override
    protected Number getNeutral() {
      return Integer.valueOf(0);
    }

    @Override
    public Number doIntOp(Number result, Number arg) {
      return result.longValue() + arg.longValue();
    }

    @Override
    public Float doFloatOp(Number result, Number arg) {
      return result.floatValue() + arg.floatValue();
    }

    @Override
    public Double doDoubleOp(Number result, Number arg) {
      return result.doubleValue() + arg.doubleValue();
    }

    @Override
    public Version doVersionOp(Number result, Number arg) {
      if (result instanceof Version) {
        Version verResult = (Version) result;
        if (arg instanceof Version) {
          return verResult.add((Version) arg);
        }
        return verResult.add(arg);
      } else if (arg instanceof Version) {
        return ((Version) arg).add(result);
      } else {
        return Version.fromDouble(result.doubleValue() + arg.doubleValue());
      }
    }
  }

  @Docstring(lines = {"Compute Product.",
                      "",
                      "Returns the product of all its arguments performing any necessary type",
                      "conversions in the process. If no numbers are supplied, 1 is returned."})
  @Package(name = Package.BASE_ARITHMENTICS)
  public static class MULOP extends ABSTRACTADDOP {
    @Override
    protected Number getNeutral() {
      return Integer.valueOf(1);
    }

    @Override
    public Number doIntOp(Number result, Number arg) {
      return result.longValue() * arg.longValue();
    }

    @Override
    public Double doDoubleOp(Number result, Number arg) {
      return result.doubleValue() * arg.doubleValue();
    }

    @Override
    public Float doFloatOp(Number result, Number arg) {
      return result.floatValue() * arg.floatValue();
    }

    public Double doVersionOp(Number result, Number arg) {
      return result.doubleValue() * arg.doubleValue();
    }
  }

  @Arguments(spec = {ArgSpec.ARG_REST, "args"})
  public abstract static class ABSTRACT_SUB extends FuncExp implements AbstractOp {
    protected abstract Number getNeutral();

    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      List<?> rest = (List<?>) eargs.get(0, backtrace);
      Number num = Utils.asNumber(rest.get(0));
      Promotion promo = new Promotion();
      promo.promote(num);
      if (rest.size() == 1) {
        num = promo.callOP(this, getNeutral(), num);
      } else {
        for (int i = 1; i < rest.size(); i++) {
          Number arg = Utils.asNumber(rest.get(i));
          promo.promote(arg);
          num = promo.callOP(this, num, arg);
        }
      }
      return promo.returnResult(num);
    }
  }

  @Docstring(lines = {
      "Performs subtraction or negation. ",
      "If only one number is supplied, the negation of that ",
      "number is returned. If more than one argument is given, ",
      "it subtracts rest of the arguments from the first one ",
      "and returns the result. The function performs necessary ",
      "type conversions."})
  @Package(name = Package.BASE_ARITHMENTICS)
  public static class SUBOP extends ABSTRACT_SUB {
    @Override
    public Number doIntOp(Number arg1, Number arg2) {
      return arg1.longValue() - arg2.longValue();
    }

    @Override
    public Float doFloatOp(Number arg1, Number arg2) {
      return arg1.floatValue() - arg2.floatValue();
    }

    @Override
    public Double doDoubleOp(Number arg1, Number arg2) {
      return arg1.doubleValue() - arg2.doubleValue();
    }

    @Override
    public Version doVersionOp(Number arg1, Number arg2) {
      if (arg1 instanceof Version) {
        Version verResult = (Version) arg1;
        if (arg2 instanceof Version) {
          return verResult.sub((Version) arg2);
        }
        return verResult.sub(arg2);
      } else if (arg2 instanceof Version) {
        return Version.fromDouble(arg1.doubleValue()).sub((Version) arg2);
      } else {
        return Version.fromDouble(arg1.doubleValue() - arg2.doubleValue());
      }
    }

    @Override
    protected Number getNeutral() {
      return Integer.valueOf(0);
    }
  }

  @Docstring(
             lines = {
               "Performs Division or Reciprocation. ",
               "- If no denominators are supplied, the function '/' returns the reciprocal",
               "  of the argument.",
               "- If at least one denominator is supplied, the function '/' divides the",
               "  numerator by all of the denominators and returns the resulting quotient.",
               "The function / performs necessary type conversions. "})
  @Package(name = Package.BASE_ARITHMENTICS)
  public static class DIVOP extends ABSTRACT_SUB {
    @Override
    public Number doIntOp(Number arg1, Number arg2) {
      return arg1.longValue() / arg2.longValue();
    }

    @Override
    public Double doDoubleOp(Number arg1, Number arg2) {
      return arg1.doubleValue() / arg2.doubleValue();
    }

    @Override
    public Double doVersionOp(Number arg1, Number arg2) {
      return arg1.doubleValue() / arg2.doubleValue();
    }

    @Override
    public Float doFloatOp(Number arg1, Number arg2) {
      return arg1.floatValue() / arg2.floatValue();
    }

    @Override
    protected Number getNeutral() {
      return Integer.valueOf(1);
    }
  }

  @Arguments(spec = {"x", ArgSpec.ARG_REST, "args"})
  @Docstring(lines = {
      "Find maximum. ",
      "Returns the maximum of numeric values of it's arguments,",
      "performing any necessary type conversions in the process."})
  @Package(name = Package.BASE_ARITHMENTICS)
  public static class MAXOP extends NUMGE {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      Promotion promo = new Promotion();
      Number result = Utils.asNumber(eargs.get(0, backtrace));
      List<?> rest = (List<?>) eargs.get(1, backtrace);
      promo.promote(result);
      for (int i = 0; i < rest.size(); i++) {
        Number val = Utils.asNumber(rest.get(i));
        promo.promote(val);
        Integer dif = promo.callOP(this, result, val).intValue();
        result = compareResult(dif) ? result : val;
      }
      return result;
    }
  }

  @Arguments(spec = {"x", ArgSpec.ARG_REST, "args"})
  @Docstring(lines = {"Find minimum. ",
                      "Returns the maximum of numeric values of it's arguments,",
                      "performing any necessary type conversions in the process."})
  @Package(name = Package.BASE_ARITHMENTICS)
  public static class MINOP extends NUMLE {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      Promotion promo = new Promotion();
      Number result = Utils.asNumber(eargs.get(0, backtrace));
      List<?> rest = (List<?>) eargs.get(1, backtrace);
      promo.promote(result);
      for (int i = 0; i < rest.size(); i++) {
        Number val = Utils.asNumber(rest.get(i));
        promo.promote(val);
        Integer dif = promo.callOP(this, result, val).intValue();
        result = compareResult(dif) ? result : val;
      }
      return result;
    }
  }

  @Arguments(spec = {"x", "y"})
  @Docstring(
      text =
          "Compute Remainder. "
              + "Generalizations of the remainder function. When both operands are integer "
              + "returns result of the remainder operation . If one of them is floating point "
              + "returns result of \n\t number - truncate_to_zero (number / divisor) * divisor "
              + "(same semantic as for the Java % operator.")
  @Package(name = Package.BASE_ARITHMENTICS)
  public static class REMOP extends FuncExp implements AbstractOp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      final Number first = Utils.asNumber(eargs.get(0, backtrace));
      final Number second = Utils.asNumber(eargs.get(1, backtrace));
      final Promotion p = new Promotion();
      p.promote(first);
      p.promote(second);
      return p.returnResult(p.callOP(this, first, second));
    }

    @Override
    public Number doIntOp(Number arg1, Number arg2) {
      return arg1.longValue() % arg2.longValue();
    }

    @Override
    public Double doDoubleOp(Number arg1, Number arg2) {
      return arg1.doubleValue() % arg2.doubleValue();
    }

    @Override
    public Double doVersionOp(Number arg1, Number arg2) {
      return arg1.doubleValue() % arg2.doubleValue();
    }

    @Override
    public Float doFloatOp(Number arg1, Number arg2) {
      return arg1.floatValue() % arg2.floatValue();
    }
  }

  @Arguments(spec = {"number", "divisor"})
  @Docstring(
      text =
          "Compute Modulus. "
              + "Generalizations of the modulus function. When both operands are integer "
              + "returns result of the modulus operation. If one of them is floating point "
              + "returns result of \n\t number - ⌊ (number / divisor) ⌋ * divisor ")
  @Package(name = Package.BASE_ARITHMENTICS)
  public static class MODOP extends FuncExp implements AbstractOp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      final Number first = Utils.asNumber(eargs.get(0, backtrace));
      final Number second = Utils.asNumber(eargs.get(1, backtrace));
      final Promotion p = new Promotion();
      p.promote(first);
      p.promote(second);
      return p.returnResult(p.callOP(this, first, second));
    }

    @Override
    public Number doIntOp(Number arg1, Number arg2) {
      return arg1.longValue()
          - Math.floor(arg1.doubleValue() / arg2.doubleValue()) * arg2.longValue();
    }

    @Override
    public Double doDoubleOp(Number arg1, Number arg2) {
      return arg1.doubleValue()
          - Math.floor(arg1.doubleValue() / arg2.doubleValue()) * arg2.doubleValue();
    }

    public Double doVersionOp(Number arg1, Number arg2) {
      return arg1.doubleValue()
          - Math.floor(arg1.doubleValue() / arg2.doubleValue()) * arg2.doubleValue();
    }

    @Override
    public Float doFloatOp(Number arg1, Number arg2) {
      return arg1.floatValue()
          - ((float) Math.floor(arg1.doubleValue() / arg2.doubleValue())) * arg2.floatValue();
    }
  }

  // **** BOOLEAN FUNCTIONS
  @Arguments(spec = {ArgSpec.ARG_LAZY, ArgSpec.ARG_REST, "forms"})
  @Docstring(lines = {
      "Logical `AND` operation.  ",
      "",
      "Function `AND` lazily evaluates each argument expression, one at a time from",
      "left to right. As soon as any of them evaluates to implicit logical `false`,",
      "`AND` returns this value without evaluating the remaining expressions (if any).",
      "If all evaluate to true values, `AND` returns the result produced by",
      "evaluating the last expression. If no arguments supplied, `AND` returns `true`."})
  @Package(name = Package.BASE_LOGIC)
  public static class AND extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      Object andVal = true;
      for (Object val : (List<?>) eargs.get(0, backtrace)) {
        andVal = val;
        if (!Utils.asBoolean(val)) {
          break;
        }
      }
      return andVal;
    }
  }

  @Arguments(spec = {ArgSpec.ARG_LAZY, ArgSpec.ARG_REST, "args"})
  @Docstring(lines = {
               "Logical `OR` operation.",
               "",
               "Function `OR` lazily evaluates each argument expression, one at a time, from",
               "left to right. The evaluation of the argument expressions terminates when one",
               "of them evaluates to an implicit logical `true` and `OR` immediately returns",
               "that value without evaluating the remaining expressions (if any). If all",
               "evaluate to logical false `OR` returns the result of the last expression.",
               "If no arguments were supplied, it returns logical false."})
  @Package(name = Package.BASE_LOGIC)
  public static class OR extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      Object orVal = false;
      for (Object val : (List<?>) eargs.get(0, backtrace)) {
        if (Utils.asBoolean(val)) {
          orVal = val;
          break;
        }
      }
      return orVal;
    }
  }

  @Arguments(spec = {"x"})
  @Docstring(
      text =
          "Logical Negation. "
              + "Returns True if x has false logical value; otherwise, returns False."
              + "Parameter x can be any object. Only NIL, the empty list (), "
              + "the empty String \"\", 0  and FALSE have false logical value. "
              + "All other objects have true logical value")
  @Package(name = Package.BASE_LOGIC)
  public static class NOT extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      Object val = eargs.get(0, backtrace);
      return !Utils.asBoolean(val).booleanValue();
    }
  }

  // **** COMPARISON
  @Arguments(spec = {"x", "y"})
  @Docstring(
      text =
          "Check Object Equality. "
              + "Returns true if x equal to y according to call to Java method "
              + "x.equals(y) or if both objects are NIL.")
  @Package(name = Package.BASE_LOGIC)
  public static class EQUAL extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      final Object v1 = eargs.get(0, backtrace);
      final Object v2 = eargs.get(1, backtrace);
      return (v1 == null) ? (v2 == null) : v1.equals(v2);
    }
  }

  @Arguments(spec = {"x", "y"})
  @Docstring(
      text =
          "Check Value Equality. Returns true if x equal to y according to call to Java method"
              + " x.equals(y) or if both objects are NIL. If they are not, it  returns true if  thy"
              + " are equal numerically or structurally.")
  @Package(name = Package.BASE_LOGIC)
  public static class SEQUAL extends FuncExp {
    NumCompOp nc = new NumCompOp();

    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      final Object v1 = eargs.get(0, backtrace);
      final Object v2 = eargs.get(1, backtrace);
      return Seq.sequal(v1, v2);
    }
  }

  // **** COMPARISON
  @Arguments(spec = {"x", "y"})
  @Docstring(
      text =
          "Check Object Equality. "
              + "Objects identity check: returns true if Object x is same as Object y. "
              + "Uses java operator == to check objects identity")
  @Package(name = Package.BASE_LOGIC)
  public static class EQ extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      final Object v1 = eargs.get(0, backtrace);
      final Object v2 = eargs.get(1, backtrace);
      return v1 == v2;
    }
  }

  @Arguments(spec = {"x", ARG_REST, "args"})
  public abstract static class NUMCOMP extends FuncExp implements AbstractOp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      boolean result = true;
      Promotion promo = new Promotion();
      Number prevVal = Utils.asNumber(eargs.get(0, backtrace));
      List<?> rest = (List<?>) eargs.get(1, backtrace);
      promo.promote(prevVal);
      for (int i = 0; i < rest.size(); i++) {
        Number val = Utils.asNumber(rest.get(i));
        promo.promote(val);
        Integer dif = promo.callOP(this, prevVal, val).intValue();
        result &= compareResult(dif);
        prevVal = val;
      }
      return result;
    }

    @Override
    public Number doIntOp(Number arg1, Number arg2) {
      return arg1.longValue() - arg2.longValue();
    }

    @Override
    public Number doDoubleOp(Number arg1, Number arg2) {
      final Double compRes = arg1.doubleValue() - arg2.doubleValue();
      return compRes < 0.0 ? -1 : (compRes > 0.0 ? 1 : 0);
    }

    @Override
    public Number doVersionOp(Number arg1, Number arg2) {
      if (arg1 instanceof Version) {
        return ((Version) arg1).compareTo(Version.fromNumber(arg2));
      } else if (arg2 instanceof Version) {
        return -((Version) arg2).compareTo(Version.fromNumber(arg1));
      } else {
        final Double compRes = arg1.doubleValue() - arg2.doubleValue();
        return compRes < 0.0 ? -1 : (compRes > 0.0 ? 1 : 0);
      }
    }

    @Override
    public Number doFloatOp(Number arg1, Number arg2) {
      final float compRes = arg1.floatValue() - arg2.floatValue();
      return compRes < 0.0f ? -1 : (compRes > 0.0f ? 1 : 0);
    }

    protected abstract boolean compareResult(int res);
  }

  @Arguments(spec = {"x", ARG_REST, "args"})
  @Docstring(
      text =
          "Test numeric equality. Returns True if all arguments are numerically equal. "
              + "Returns True if only one argument is given")
  @Package(name = Package.BASE_LOGIC)
  public static class NUMEQ extends NUMCOMP {
    @Override
    protected boolean compareResult(int res) {
      return 0 == res;
    }
  }

  @Arguments(spec = {"x", ARG_REST, "args"})
  @Docstring(
      text =
          "Greater Than - Numeric comparison. Returns True if all arguments are monotonically "
              + "decreasing order.  Returns True if only one argument is given")
  @Package(name = Package.BASE_LOGIC)
  public static class NUMGT extends NUMCOMP {
    @Override
    protected boolean compareResult(int res) {
      return res > 0;
    }
  }

  @Arguments(spec = {"x", ARG_REST, "args"})
  @Docstring(
      text =
          "Greater or Equal - Numeric comparison. "
              + "Returns True if all arguments are monotonically non-increasing order. "
              + "Returns True if only one argument is given")
  @Package(name = Package.BASE_LOGIC)
  public static class NUMGE extends NUMCOMP {
    @Override
    protected boolean compareResult(int res) {
      return res >= 0;
    }
  }

  @Arguments(spec = {"x", ARG_REST, "args"})
  @Package(name = Package.BASE_LOGIC)
  @Docstring(
      text =
          "Less Than - Numeric Comparison. "
              + "Returns True if all arguments are monotonically increasing order.  "
              + "Returns True if only one argument is given")
  public static class NUMLT extends NUMCOMP {
    @Override
    protected boolean compareResult(int res) {
      return res < 0;
    }
  }

  @Arguments(spec = {"x", ARG_REST, "args"})
  @Package(name = Package.BASE_LOGIC)
  @Docstring(
      text =
          "Less or Equal - Numeric comparison. "
              + "Returns True if all arguments are monotonically non-decreasing order.  "
              + "Returns True if only one argument is given")
  public static class NUMLE extends NUMCOMP {
    @Override
    protected boolean compareResult(int res) {
      return res <= 0;
    }
  }

  @Arguments(spec = {"x"})
  @Docstring(
      text =
          "Return Number Sign. Determines a numerical value that indicates whether number is"
              + " negative, zero, or positive. Returns one of -1, 0, or 1 according to whether"
              + " number is negative, zero, or positive. The type of the result is of the same"
              + " numeric type as x")
  @Package(name = Package.BASE_ARITHMENTICS)
  public static class SIGNUM extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      final Number val = Utils.asNumber(eargs.get(0, backtrace));

      if (val instanceof Double) {
        final double dv = val.doubleValue();
        return dv > 0.0 ? 1.0 : (dv < 0.0 ? -1.0 : 0);
      } else if (val instanceof Float) {
        final float fv = val.floatValue();
        return fv > (float) 0.0 ? (float) 1.0 : (fv < (float) 0.0 ? (float) -1.0 : (float) 0);
      } else {
        final Promotion p = new Promotion();
        p.promote(val);
        final long lv = val.longValue();
        return p.returnResult(lv > 0 ? (byte) 1 : (lv < 0 ? (byte) -1 : (byte) 0));
      }
    }
  }

  // **** COERCION
  @Arguments(spec = {"value"})
  @Docstring(
      text =
          "Coerce Value to Boolean. Value may be a Character, a Number, a Boolean, a Byte, a"
              + " String, any object or NIL:\n"
              + "* Boolean value will be returned as is\n"
              + "* NIL is false\n"
              + "* Character  \u0000 is false.\n"
              + "* any Number which is equal to zero is false\n"
              + "* an empty String is false\n"
              + "* An empty collection is false \n"
              + "* Any other object is true.\n")
  @Package(name = Package.BASE_COERCION)
  public static class BOOL extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      final Object val = eargs.get(0, backtrace);
      return Utils.asBoolean(val);
    }
  }

  @Arguments(spec = {"value"})
  @Docstring(
      text =
          "Coerce Value to Character. Value may be a Character, a Number, a Boolean, a Byte, a"
              + " Stringor NIL:\n"
              + "* Character value will be returned as is.\n"
              + "* NIL will be converted to unicode value #\u0000.\n"
              + "* a Boolean True value will be returned as character 'T', False as '\0'.\n"
              + "* a Number (other than Byte) will be truncated to short (if needed) and the"
              + " character at corresponding Unicode code unit will be returned.\n"
              + "* a Byte value will be treated as unsigned integer value and processed as"
              + " described above.\n"
              + "* a String will be parsed as number using same rules as numeric literals and the"
              + " resulting value will be used as described above. Conversion to number may fail.\n"
              + "* Any other object will cause conversion error.\n")
  @Package(name = Package.BASE_COERCION)
  public static class CHAR extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      final Object val = eargs.get(0, backtrace);
      return Utils.asChar(val);
    }
  }

  @Arguments(spec = {"value"})
  @Docstring(
      text =
          "Coerce value to Integer. Value may be a Number, String, any object or NIL."
              + "String will be parsed as number using same rules as numeric literals. "
              + "The floating point value will be truncated.")
  @Package(name = Package.BASE_COERCION)
  public static class INT extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      final Object val = eargs.get(0, backtrace);
      return Utils.asNumber(val).intValue();
    }
  }

  @Arguments(spec = {"value"})
  @Docstring(
      text =
          "Coerce Value to String. Value may be any object or NIL: "
              + "NIL is converted to String \"NIL\", any other object converted "
              + "using it's toString() method")
  @Package(name = Package.BASE_COERCION)
  public static class STRING extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      final Object obj = eargs.get(0, backtrace);
      return Utils.asString(obj);
    }
  }

  @Arguments(spec = {"value"})
  @Docstring(
      text =
          "Coerce Value to Long. Value may be a Number, String, any object or NIL."
              + "String will be parsed as number using same rules as numeric literals. "
              + "The floating point values will be truncated.")
  @Package(name = Package.BASE_COERCION)
  public static class LONG extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      final Object val = eargs.get(0, backtrace);
      return Utils.asNumber(val).longValue();
    }
  }

  @Arguments(spec = {"value"})
  @Docstring(
      text =
          "Coerce Value to Short. Value may be a Number, String, any object or NIL."
              + "String will be parsed as number using same rules as numeric literals. "
              + "The floating point values will be truncated.")
  @Package(name = Package.BASE_COERCION)
  public static class SHORT extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      final Object val = eargs.get(0, backtrace);
      return Utils.asNumber(val).shortValue();
    }
  }

  @Arguments(spec = {"value"})
  @Docstring(
      text =
          "Coerce Value to Byte. Value may be a Number, String, any object or NIL."
              + "String will be parsed as number using same rules as numeric literals. "
              + "The floating point values will be truncated.")
  @Package(name = Package.BASE_COERCION)
  public static class BYTE extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      final Object val = eargs.get(0, backtrace);
      return Utils.asNumber(val).byteValue();
    }
  }

  @Arguments(spec = {"value"})
  @Docstring(
      text =
          "Coerce Value to Double. Value may be a Number, String, any object or NIL."
              + "String will be parsed as number using same rules as numeric literals. "
              + "The floating point values will be truncated.")
  @Package(name = Package.BASE_COERCION)
  public static class DOUBLE extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      final Object val = eargs.get(0, backtrace);
      return Utils.asNumber(val).doubleValue();
    }
  }

  @Arguments(spec = {"value"})
  @Docstring(
      text =
          "Coerce Value to Float. Value may be a Number, String, any object or NIL."
              + "String will be parsed as number using same rules as numeric literals. "
              + "The floating point values will be truncated.")
  @Package(name = Package.BASE_COERCION)
  public static class FLOAT extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      final Object val = eargs.get(0, backtrace);
      return Utils.asNumber(val).floatValue();
    }
  }

  @Arguments(spec = {"limit"})
  @Docstring(
      text =
          "Produce Pseudo-Random Number. Returns a pseudo-random number that is a non-negative"
              + " number less than limit and of the same numeric type as limit. Implemented uding"
              + " Java Math.random()")
  @Package(name = Package.BASE_MATH)
  public static class RANDOM extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      final Object numberObj = eargs.get(0, backtrace);
      final double val = java.lang.Math.random();
      final Number number = Utils.asNumber(numberObj);
      if (number instanceof Integer) {
        return (int) (val * number.intValue());
      } else if (numberObj instanceof Long) {
        return (long) (val * number.longValue());
      } else if (numberObj instanceof Short) {
        return (short) (val * number.shortValue());
      } else if (numberObj instanceof Float) {
        return (float) (val * number.floatValue());
      } else if (numberObj instanceof Double) {
        return val * number.doubleValue();
      } else if (numberObj instanceof Byte) {
        return (byte) (val * number.byteValue());
      } else {
        throw new ExecutionException(
            backtrace, "Unsupported argument numeric type: " + numberObj.getClass());
      }
    }
  }

  @Arguments(spec = {"x"})
  @Docstring(text = "Computes square root of the argument. Returns double value.")
  @Package(name = Package.BASE_MATH)
  public static class SQRT extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      final Object numberObj = eargs.get(0, backtrace);
      final double val = java.lang.Math.sqrt(Utils.asNumber(numberObj).doubleValue());
      return val;
    }
  }

  @Arguments(spec = {"x", "&OPTIONAL", "base"})
  @Docstring(
      text =
          "Computes logarithm. "
              + "If base is not given it computes natural logarithm. Returns a Double"
              + " value.")
  @Package(name = Package.BASE_MATH)
  public static class LOG extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      final Object numberObj = eargs.get(0, backtrace);
      if (!(numberObj instanceof Number)) {
        throw new ExecutionException(backtrace, getName() + " argument must be a number");
      }
      double val = java.lang.Math.log(((Number) numberObj).doubleValue());
      if (eargs.size() > 1) {
        final Object baseObj = eargs.get(1, backtrace);
        if (null != baseObj) {
          if (!(baseObj instanceof Number)) {
            throw new ExecutionException(
                backtrace, getName() + " logarithmm base must be a number, but got " + baseObj);
          }
          val = val / java.lang.Math.log(((Number) baseObj).doubleValue());
        }
      }
      return val;
    }
  }

  @Arguments(spec = {"x", "&OPTIONAL", "base"})
  @Docstring(
      text =
          "Perform exponentiation. If base is not given it returns e raised to power x. Returns a"
              + " Double value.")
  @Package(name = Package.BASE_MATH)
  public static class EXP extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      final Object numberObj = eargs.get(0, backtrace);
      if (!(numberObj instanceof Number)) {
        throw new ExecutionException(backtrace, getName() + " argument must be a number");
      }
      double base = Math.E;
      if (eargs.size() > 1) {
        final Object baseObj = eargs.get(1, backtrace);
        if (null != baseObj) {
          if (!(baseObj instanceof Number)) {
            throw new ExecutionException(
                backtrace, getName() + " logarithmm base must be a number, but got " + baseObj);
          }
          base = ((Number) baseObj).doubleValue();
        }
      }
      return Math.pow(((Number) numberObj).doubleValue(), base);
    }
  }

  // **** VARIABLES, DATATYPES AND FUNCTIONS
  // FIXME: are array types supported?
  @Arguments(spec = {"object", "type-specifier"})
  @Docstring(
      text =
          "Check if Object is of Specified Type. Returns True if object is of the specified type."
              + " Type specifier may be a Class object or string or symbol which is a valid"
              + " type-specifier.")
  @Package(name = Package.BASE_TYPES)
  public static class TYPEP extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      final Object val = eargs.get(0, backtrace);
      final Object tspec = eargs.get(1, backtrace);
      if (null == tspec) {
        return null == val;
      }
      try {
        final Class<?> tclass = Utils.tspecToClass(tspec);
        return tclass.isInstance(val);
      } catch (ClassNotFoundException ex) {
        throw new ExecutionException(ex);
      }

    }
  }

  @Arguments(spec = {"object"})
  @Docstring(
      text =
          "Return Object Type. Returns type (as class) of the given object. For NIL argument return"
              + " NIL.")
  @Package(name = Package.BASE_TYPES)
  public static class TYPE_OF extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      Object val = eargs.get(0, backtrace);
      if (null != val) {
        return val.getClass();
      } else {
        return null;
      }
    }
  }

  @Arguments(spec = {ARG_REST, "symbols"})
  @Docstring(
      text =
          "Check if Symbols are Bound. Returns True if all the arguments are bound symbols or names"
              + " of bound symbols; otherwise, returns False.")
  @Package(name = Package.BASE_BINDINGS)
  public static class BOUNDP extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      for (Object arg : (List<?>) eargs.get(0, backtrace)) {
        if ((null == arg) || (!eargs.contains(Utils.asString(arg)))) {
          return false;
        }
      }
      return true;
    }
  }

  @Arguments(spec = {ArgSpec.ARG_REST, "pairs"})
  @Docstring(
      text =
          "Create a HashMap. Returns new HashMap filled with given keys and values. "
              + "Throws InvalidParametersException if non-even number of arguments is given.")
  @Package(name = Package.BASE_SEQ)
  public static class HASHMAP extends FuncExp  implements LValue {
    @Override
    public void checkParamsList(List<ICompiled> params) throws InvalidParametersException {
      super.checkParamsList(params);
      if (0 != (params.size() % 2)) {
        throw new InvalidParametersException(
            String.format(
                "%s expects even number of parameters, but got %d", getName(), params.size()));
      }
    }

    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      Map<Object, Object> map = new HashMap<Object, Object>();
      List<?> rest = (List<?>) eargs.get(0, backtrace);
      for (int i = 0; i < rest.size(); i += 2) {
        map.put(rest.get(i), rest.get(i + 1));
      }
      return map;
    }

    @Override
    public Object doSet(Backtrace backtrace, ICtx ctx, Object value) {
      // rest parameter
      ICompiled args = this.argList.get(0);
      if (args instanceof ValueExpr) {
        // list of target objects
        final List<?> targetList = (List<?>) ((ValueExpr) args).value;
        if (null == value) { // treat NIL as empty map
          value = new HashMap<Object,Object>();
        } else if (!(value instanceof Map)) {
          throw new ExecutionException(backtrace,
                                       "Source for map destructuring must be a Map or NIL,"
                                       + " but got "
                                       + value.getClass());
        }
        Map<?,?> valueMap = (Map<?,?>)value;
        for (int idx = 0; idx < targetList.size(); idx += 2) {
          Object lvalObj = targetList.get(idx);
          Object srcKeyExpr = targetList.get(idx + 1);
          if (lvalObj instanceof LValue) {
            Object valueKey = ((ICompiled) srcKeyExpr).evaluate(backtrace, ctx);
            Object val = valueMap.get(valueKey);
            ((LValue) lvalObj).doSet(backtrace, ctx, val);
          }
        }
      } else {
        throw new RuntimeException("Internal error: list value is not instance of ValueExpr!");
      }
      return null;
    }
  }

  @Arguments(spec = {"function", ArgSpec.ARG_PIPE, ArgSpec.ARG_REST, "arguments"})
  @Docstring(lines =
             {"Call function `f` with `arguments`. ",
              "`funcall` calls function `f` with given arguments."})
  @Package(name = Package.BASE_FUNCS)
  public static class FUNCALL extends AbstractExpr {
    private List<ICompiled> params = null;

    @Override
    public void setParams(List<ICompiled> params) throws InvalidParametersException {
      if (params.size() == 0) {
        throw new InvalidParametersException(
            debugInfo, this.getName() + " requires at least one argument");
      }
      if (null != this.params) {
        throw new InvalidParametersException(
            this.getDebugInfo(), "internal exception: parameters already set");
      }
      this.params = params;
    }

    @Override
    public Object doEvaluate(Backtrace backtrace, ICtx ctx) {
      final Object functionObj = params.get(0).evaluate(backtrace, ctx);
      if (!(functionObj instanceof ICode)) {
        throw new RuntimeException("Expected ICode object, but got " + functionObj);
      }
      ICode function = (ICode) Utils.asObject(functionObj);
      IExpr instance = (IExpr) function.getInstance();
      if (null != backtrace) {
        final Backtrace.Frame frame = backtrace.last();
        if (null != frame) {
          instance.setDebugInfo(frame.pctx);
        }
      }
      try {
        instance.setParams(params.subList(1, params.size()));
      } catch (InvalidParametersException e) {
        throw new RuntimeException(
            String.format(
                "%s: called function at %s: does not take parameter: %s",
                this.getName(),
                (null == e.getParseCtx() ? "?" : e.getParseCtx().toString()),
                e.getMessage()));
      }
      return instance.evaluate(backtrace, ctx);
    }
  }

  // ****** MAPPING OPERATIONS
  // args is a spreadable list designator
  @Arguments(spec = {"f", ArgSpec.ARG_PIPE, ArgSpec.ARG_REST, "arguments"})
  @Docstring(lines =
             {"Call function `f` with `arguments` expanding the last one. ",
              "`apply` calls function `f` with given arguments. If the last of the arguments",
              "is a list, its contents will be appended to the list of arguments of `f`."})
  @Package(name = Package.BASE_FUNCS)
  public static class APPLY extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      final Object val = eargs.get(0, backtrace);
      final ICode lambda = (ICode) Utils.asObject(val);
      final IExpr instance = (IExpr) lambda.getInstance();
      final List<?> funcArgs = (List<?>) eargs.get(1, backtrace);

      final ICtx applyCtx = eargs.getCompiler().newCtx(eargs);

      List<?> rest = null;
      int restSize;
      int headSize;
      if ((funcArgs.size() > 0) && ((funcArgs.get(funcArgs.size() - 1)) instanceof List)) {
        rest = (List<?>) funcArgs.get(funcArgs.size() - 1);
        restSize = rest.size();
        headSize = funcArgs.size() - 1;
      } else {
        restSize = 0;
        headSize = funcArgs.size();
      }
      List<ICompiled> callParams = new ArrayList<ICompiled>(restSize + headSize);
      for (int i = 0; i < headSize; i++) {
        callParams.add(new ObjectExp(funcArgs.get(i)));
      }
      for (int i = 0; i < restSize; i++) {
        callParams.add(new ObjectExp(rest.get(i)));
      }
      if (null != backtrace) {
        final Backtrace.Frame frame = backtrace.last();
        if (null != frame) {
          instance.setDebugInfo(frame.pctx);
        }
      }
      try {
        instance.setParams(callParams);
      } catch (InvalidParametersException e) {
        throw new RuntimeException(
            String.format(
                "%s: called function at %s: does not take provided parameters: %s",
                this.getName(),
                (null == e.getParseCtx() ? "?" : e.getParseCtx().toString()),
                e.getMessage()));
      }
      return instance.evaluate(backtrace, applyCtx);
    }
  }

  protected static List<ICompiled> setFuncPosParams(IExpr instance, int cnt) {
    final List<ICompiled> callParams = Utils.newPosArgsList(cnt);
    try {
      instance.setParams(callParams);
    } catch (InvalidParametersException e) {
      throw new RuntimeException(
          String.format(
              "lambda at %s: does not take parameter: %s",
              (null == e.getParseCtx() ? "?" : e.getParseCtx().toString()), e.getMessage()));
    }
    return callParams;
  }

  @Docstring(
      text =
          "Reduce operation.\n func is a function of 2 arguments, "
              + "value - optional starting value, seq is input sequence.\n"
              + "When val is not given:  apply func to the first 2 items in the seq, "
              + "then to the result and 3rd, etc. "
              + "If seq contains no items, func must accept no arguments, return (func)."
              + "If seq has 1 item, return it without calling func;\n"
              + "If value is supplied, apply func on value and the first seq element, then "
              + "on the result and the second element, etc. If there is no elements - return val;")
  @Arguments(
      spec = {"func", ArgSpec.ARG_OPTIONAL, "val", ArgSpec.ARG_PIPE, ArgSpec.ARG_MANDATORY, "seq"})
  @Package(name = Package.BASE_SEQ)
  public static class REDUCE extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      Object val = eargs.get(0, backtrace);
      ICode lambda = (ICode) Utils.asObject(val);
      IExpr instance = (IExpr) lambda.getInstance();

      List<?> list = (List<?>) eargs.get(2, backtrace);
      Object startVal = eargs.get(1, backtrace);
      // FIXME
      boolean haveStartVal = null != startVal;

      if (null == list || 0 == list.size()) {
        if (haveStartVal) {
          return startVal;
        } else {
          setFuncPosParams(instance, 0);
          ICtx newCtx = eargs.getCompiler().newCtx(eargs);
          return instance.evaluate(backtrace, newCtx);
        }
      }
      int idx = 0;
      Object result;
      if (haveStartVal) {
        result = startVal;
      } else {
        result = list.get(idx++);
      }
      setFuncPosParams(instance, 2);
      for (; idx < list.size(); idx++) {
        ICtx newCtx = eargs.getCompiler().newCtx(eargs);
        newCtx.getMappings().put("%1", result);
        newCtx.getMappings().put("%2", list.get(idx));
        result = instance.evaluate(backtrace, newCtx);
      }
      return result;
    }
  }

  @Docstring(
      text =
          "Filter operation. test is a function of one argument that returns boolean, seq is input"
              + " sequence. Return a sequence from which the elements that do not satisfy the test"
              + " have been removed.")
  @Arguments(spec = {"test", ArgSpec.ARG_PIPE, "sequence"})
  @Package(name = Package.BASE_SEQ)
  public static class FILTER extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      Object val = eargs.get(0, backtrace);
      ICode lambda = (ICode) Utils.asObject(val);
      IExpr instance = (IExpr) lambda.getInstance();
      List<?> list = (List<?>) eargs.get(1, backtrace);
      final String argname = "arg#0";
      final List<ICompiled> callParams = new ArrayList<ICompiled>(1);
      callParams.add(new VarExp(argname));
      try {
        instance.setParams(callParams);
      } catch (InvalidParametersException e) {
        throw new RuntimeException(
            String.format(
                "FILTER: lambda at %s: does not take parameter: %s",
                (null == e.getParseCtx() ? "?" : e.getParseCtx().toString()), e.getMessage()));
      }
      List<Object> results = new ArrayList<Object>();
      for (int i = 0; i < list.size(); i++) {
        ICtx loopCtx = eargs.getCompiler().newCtx(eargs);
        loopCtx.put(argname, list.get(i));
        final Object result = instance.evaluate(backtrace, loopCtx);
        if (Utils.asBoolean(result)) {
          results.add(list.get(i));
        }
      }
      return results;
    }
  }

  // f &rest args
  public abstract static class ABSTRACTMAPOP extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      Object val = eargs.get(0, backtrace);
      ICode lambda = (ICode) Utils.asObject(val);
      IExpr instance = (IExpr) lambda.getInstance();

      List<?> rest = (List<?>) eargs.get(1, backtrace);
      final int numLists = rest.size();
      if (numLists == 0) {
        throw new RuntimeException("At least one sequence must be provided");
      }
      final List<ICompiled> callParams = new ArrayList<ICompiled>(numLists);
      // evaluated lists that were given as parameters
      Object[] seqs = new Object[numLists];
      for (int i = 0; i < seqs.length; i++) {
        Object seq = rest.get(i);
        if (seq instanceof Map) {
          seq = Seq.valuesList(seq);
        }
        seqs[i] = null == seq ? new ArrayList<Object>() : seq;
        callParams.add(new VarExp("arg#" + i));
      }
      try {
        instance.setParams(callParams);
      } catch (InvalidParametersException e) {
        throw new RuntimeException(
            String.format(
                "%s: lambda at %s: does not take parameter: %s",
                this.getName(),
                (null == e.getParseCtx() ? "?" : e.getParseCtx().toString()),
                e.getMessage()));
      }

      List<Object> results = new ArrayList<Object>();
      callfuncs(backtrace, results, seqs, instance, callParams, eargs);
      return results;
    }

    protected abstract void callfuncs(
        Backtrace backtrace,
        List<Object> results,
        Object[] seqs,
        IExpr instance,
        List<ICompiled> callParams,
        ICtx ctx);

    protected int callfunc(
        Backtrace backtrace,
        List<Object> results,
        Object[] seqs,
        int[] indices,
        IExpr instance,
        List<ICompiled> callParams,
        ICtx ctx) {
      ICtx loopCtx = ctx.getCompiler().newCtx(ctx);
      for (int seqNo = 0; seqNo < seqs.length; seqNo++) {
        Object seq = seqs[seqNo];
        int seqSize = Seq.getLength(seq, false);
        if (indices[seqNo] < seqSize) {
          loopCtx.put(
              ((VarExp) callParams.get(seqNo)).getName(),
              // seq.get(indices[seqNo])
              Seq.getElementByIndex(seq, indices[seqNo]));
        } else {
          return seqNo;
        }
      }

      final Object result = instance.evaluate(backtrace, loopCtx);
      results.add(result);
      return -1;
    }
  }

  @Arguments(spec = {"func", ArgSpec.ARG_PIPE, ArgSpec.ARG_REST, "lists"})
  @Docstring(
      text =
          "Apply function on elements of collections. "
              + "Returns a sequence consisting of the result of applying func to "
              + "the set of first items of each list, followed by applying func to the "
              + "set of second items in each list, until any one of the lists is "
              + "exhausted.  Any remaining items in other lists are ignored. Function "
              + "func should accept number arguments that is equal to number of lists.")
  @Package(name = Package.BASE_SEQ)
  public static class MAP extends ABSTRACTMAPOP {
    protected void callfuncs(
        Backtrace backtrace,
        List<Object> results,
        Object[] seqs,
        IExpr instance,
        List<ICompiled> callParams,
        ICtx ctx) {
      int[] indices = new int[seqs.length];
      int overflow = -1;
      while (true) {
        overflow = callfunc(backtrace, results, seqs, indices, instance, callParams, ctx);
        if (overflow >= 0) {
          break;
        }
        for (int i = 0; i < indices.length; i++) {
          indices[i]++;
        }
      }
    }
  }

  @Arguments(spec = {"f", ArgSpec.ARG_PIPE, ArgSpec.ARG_REST, "lists"})
  @Docstring(
      text =
          "Apply function on cartesioan product of lists. Returns a sequence consisting of the"
              + " result of applying func to the cartesian product of the lists. Function func"
              + " should accept number arguments that is equal to number of lists.")
  @Package(name = Package.BASE_SEQ)
  public static class MAPPROD extends ABSTRACTMAPOP {
    protected void callfuncs(
        Backtrace backtrace,
        List<Object> results,
        Object[] seqs,
        IExpr instance,
        List<ICompiled> callParams,
        ICtx ctx) {
      int[] indices = new int[seqs.length];
      int overflow = -1;
      while (true) {
        overflow = callfunc(backtrace, results, seqs, indices, instance, callParams, ctx);
        if (overflow < 0) {
          indices[0]++;
          continue;
        }
        if (overflow >= indices.length - 1) {
          // overflow in eldest list, END
          return;
        }
        if (Seq.getLength(seqs[overflow], false) == 0) {
          // one of lists is empty
          return;
        }
        indices[overflow + 1]++;
        for (int i = 0; i <= overflow; i++) {
          indices[i] = 0;
        }
      }
    }
  }

  // ***** VARIABLE PROPERTIES HANDLING
  @Arguments(spec = {"symbol", "property-key"})
  @Docstring(
      text = "Get Variable Property. " + "Returns value of a property from variable property map")
  @Package(name = Package.BASE_BINDINGS)
  public static class GETPROP extends FuncExp {
    @Override
    public Object evalWithArgs(final Backtrace backtrace, Eargs eargs) {
      final Object symbolObj = eargs.get(0, backtrace);
      final Object propKey = eargs.get(1, backtrace);
      if (null == symbolObj) {
        return null;
      }
      return eargs.getProp(Utils.asString(symbolObj), propKey, backtrace);
    }
  }

  @Arguments(spec = {"symbol", "property-key", "property-value"})
  @Docstring(text = "Set variable property. Sets property value in variable property map")
  @Package(name = Package.BASE_BINDINGS)
  public static class SETPROP extends FuncExp {
    @Override
    public Object evalWithArgs(final Backtrace backtrace, Eargs eargs) {
      final Object symbolObj = eargs.get(0, backtrace);
      final Object propKey = eargs.get(1, backtrace);
      final Object propVal = eargs.get(2, backtrace);
      if (null == symbolObj) {
        return null;
      }
      eargs.putProp(Utils.asString(symbolObj), propKey, propVal);
      return null;
    }
  }

  @Arguments(spec = {"symbol"})
  @Docstring(text = "Get Properties Map for a Variable.")
  @Package(name = Package.BASE_BINDINGS)
  public static class GETPROPS extends FuncExp {
    @Override
    public Object evalWithArgs(final Backtrace backtrace, Eargs eargs) {
      final Object symbolObj = eargs.get(0, backtrace);
      if (null == symbolObj) {
        return null;
      }
      return eargs.getProps(Utils.asString(symbolObj), backtrace);
    }
  }

  @Arguments(spec = {"symbol", "properties-map"})
  @Docstring(text = "Set Properties Map for a Variable")
  @Package(name = Package.BASE_BINDINGS)
  @SuppressWarnings("unchecked")
  public static class SETPROPS extends FuncExp {
    @Override
    public Object evalWithArgs(final Backtrace backtrace, Eargs eargs) {
      final Object symbolObj = eargs.get(0, backtrace);
      final Object propsObj = eargs.get(1, backtrace);
      if (null == symbolObj) {
        return null;
      }
      if (null != propsObj && !(propsObj instanceof Map)) {
        throw new RuntimeException("properties-map must be a java.util.Map");
      }
      eargs.putProps(Utils.asString(symbolObj), (Map<Object, Object>) propsObj);
      return null;
    }
  }

  // ***** CONTEXT HANDLING
  @Arguments(spec = {})
  @Docstring(text = "Create New Empty Context")
  @Package(name = Package.BASE_BINDINGS)
  public static class NEW_CTX extends FuncExp {
    @Override
    public Object evalWithArgs(final Backtrace backtrace, Eargs eargs) {
      return eargs.getCompiler().newCtx();
    }
  }

  // ***** JAVA INTEROP
  public static class FilteredMap implements Map<Object, Object> {
    protected Map<Object, Object> src;
    protected Set<?> filterSet;

    public FilteredMap(Map<Object, Object> src, Object filter) {
      this.src = src;
      this.filterSet = Seq.asSet(filter);
    }

    @Override
    public int size() {
      return keySet().size();
    }

    @Override
    public boolean isEmpty() {
      return this.keySet().isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
      return this.keySet().contains(key);
    }

    @Override
    public boolean containsValue(Object value) {
      if (!src.containsValue(value)) {
        return false;
      } else {
        for (Object key : filterSet) {
          if (src.containsKey(key)) {
            Object val = src.get(key);
            if (null == val) {
              if (null == value) {
                return true;
              }
            } else {
              if (val.equals(value)) {
                return true;
              }
            }
          }
        }
        return false;
      }
    }

    @Override
    public Object get(Object key) {
      if (containsKey(key)) {
        return src.get(key);
      } else {
        return null;
      }
    }

    @Override
    public Object put(Object key, Object value) {
      // FIXME: do we need write methods
      return src.put(key, value);
    }

    @Override
    public Object remove(Object key) {
      return src.remove(key);
    }

    @Override
    public void putAll(Map<?, ?> map) {
      src.putAll(map);
    }

    @Override
    public void clear() {
      src.clear();
    }

    @Override
    public Set<Object> keySet() {
      return Utils.intersectSets(filterSet, src.keySet());
    }

    @Override
    public Collection<Object> values() {
      Set<Object> values = new HashSet<Object>();
      for (Object key : this.keySet()) {
        values.add(get(key));
      }
      return values;
    }

    @Override
    public Set<Map.Entry<Object, Object>> entrySet() {
      Set<Map.Entry<Object, Object>> entries = new HashSet<Map.Entry<Object, Object>>();
      for (Object key : this.keySet()) {
        final Object entryKey = key;
        entries.add(
            new Map.Entry<Object, Object>() {
              @Override
              public Object getKey() {
                return entryKey;
              }

              @Override
              public Object getValue() {
                return get(entryKey);
              }

              @Override
              public Object setValue(Object value) {
                return put(entryKey, value);
              }
            });
      }
      return entries;
    }

    @Override
    public String toString() {
      Iterator<Entry<Object, Object>> iter = entrySet().iterator();
      if (!iter.hasNext()) {
        return "{}";
      }

      StringBuilder sb = new StringBuilder();
      sb.append('{');
      for (; ; ) {
        Entry<Object, Object> entry = iter.next();
        Object key = entry.getKey();
        Object value = entry.getValue();
        sb.append(key == this ? "(this Map)" : key);
        sb.append('=');
        sb.append(value == this ? "(this Map)" : value);
        if (!iter.hasNext()) {
          return sb.append('}').toString();
        }
        sb.append(',').append(' ');
      }
    }
  }


  public static class IndexMap implements Map<Object, Object> {
    protected Object src;
    protected Set<?> filterSet;

    public IndexMap(Object src, Object filter) {
      this.src = src;
      this.filterSet = Seq.asSet(filter);
    }

    @Override
    public int size() {
      return keySet().size();
    }

    @Override
    public boolean isEmpty() {
      return this.keySet().isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
      return this.keySet().contains(key);
    }

    @Override
    public boolean containsValue(Object value) {
      for (Object key : filterSet) {
        if (containsKey(key)) {
          Object val = get(key);
          if (null == val) {
            if (null == value) {
              return true;
            }
          } else {
            if (val.equals(value)) {
              return true;
            }
          }
        }
      }
      return false;
    }

    @Override
    public Object get(Object key) {
      if (containsKey(key)) {
        return Seq.getElementByIndex(src, Utils.asNumber(key).intValue());
      } else {
        return null;
      }
    }

    @Override
    public Object put(Object key, Object value) {
      throw new RuntimeException("not implemented for IndexMap");
    }

    @Override
    public Object remove(Object key) {
      throw new RuntimeException("not implemented for IndexMap");
    }

    @Override
    public void putAll(Map<?, ?> map) {
      throw new RuntimeException("not implemented for IndexMap");
    }

    @Override
    public void clear() {
      throw new RuntimeException("not implemented for IndexMap");
    }

    @Override
    public Set<Object> keySet() {
      final Set<Object> set = new HashSet<Object>();
      int length = Seq.getLength(src, false);
      for (Object key : filterSet) {
        final Number idxNum = Utils.asNumber(key);
        if (null != idxNum) {
          int idx = idxNum.intValue();
          if (idx >= 0 && idx < length) {
            set.add(idx);
          }
        }
      }
      return set;
    }

    @Override
    public Collection<Object> values() {
      Set<Object> values = new HashSet<Object>();
      for (Object key : this.keySet()) {
        values.add(get(key));
      }
      return values;
    }

    @Override
    public Set<Map.Entry<Object, Object>> entrySet() {
      Set<Map.Entry<Object, Object>> entries = new HashSet<Map.Entry<Object, Object>>();
      for (Object key : this.keySet()) {
        final Object entryKey = key;
        entries.add(new Map.Entry<Object, Object>() {
          @Override
          public Object getKey() {
            return entryKey;
          }

          @Override
          public Object getValue() {
            return get(entryKey);
          }

          @Override
          public Object setValue(Object value) {
            return put(entryKey, value);
          }
        });
      }
      return entries;
    }

    @Override
    public String toString() {
      Iterator<Entry<Object, Object>> iter = entrySet().iterator();
      if (!iter.hasNext()) {
        return "{}";
      }

      StringBuilder sb = new StringBuilder();
      sb.append('{');
      for (;;) {
        Entry<Object, Object> entry = iter.next();
        Object key = entry.getKey();
        Object value = entry.getValue();
        sb.append(key == this ? "(this Map)" : key);
        sb.append('=');
        sb.append(value == this ? "(this Map)" : value);
        if (!iter.hasNext()) {
          return sb.append('}').toString();
        }
        sb.append(',').append(' ');
      }
    }
  }

  public static class BeanMap implements Map<String, Object> {
    protected Object obj;
    // protected Backtrace backtrace;
    protected String prefix;
    protected String suffix;
    // collection of fields names
    protected Object fields;

    public BeanMap(Object obj) {
      this(obj, null, null);
    }

    /**
     * Construct bean map from bean object.
     *
     * @param obj base object
     * @param prefix map keys prefix
     * @param suffix map keys suffix
     */
    public BeanMap(Object obj, String prefix, String suffix) {
      this.obj = obj;
      // this.backtrace = backtrace;
      this.prefix = Utils.asStringOrEmpty(prefix);
      this.suffix = Utils.asStringOrEmpty(suffix);
      this.getters = getGettersMap();
    }

    /**
     * Construct bean map from bean object.
     *
     * @param obj base object
     * @param prefix map keys prefix
     * @param suffix map keys suffix
     * @param fields sequence of allowed fields
     */
    public BeanMap(Object obj, String prefix, String suffix, Object fields) {
      this.obj = obj;
      // this.backtrace = backtrace;
      this.prefix = Utils.asStringOrEmpty(prefix);
      this.suffix = Utils.asStringOrEmpty(suffix);
      this.fields = fields;
      this.getters = getGettersMap();
    }

    protected Map<String, Method> getGettersMap() {
      Map<String, Method> result = new HashMap<String, Method>();
      Method[] methods = null == obj ? new Method[0] : obj.getClass().getMethods();
      for (int i = 0; i < methods.length; i++) {
        Method method = methods[i];
        // if (m.getParameterCount()>0) {
        if (method.getParameterTypes().length > 0) {
          continue;
        }
        final String methodName = method.getName();
        if (null == methodName || null == method.getReturnType()) {
          continue;
        }
        StringBuilder kb = new StringBuilder();
        kb.append(prefix);
        String fieldName = null;
        if (methodName.startsWith("get") && (methodName.length() >= 4)) {
          fieldName = methodName.substring(3, 4).toLowerCase() + methodName.substring(4);
        } else if (methodName.startsWith("is") && (methodName.length() >= 3)) {
          fieldName = methodName.substring(2, 3).toLowerCase() + methodName.substring(3);
        } else {
          continue;
        }
        if (null != this.fields) {
          if (!Seq.containsElement(fields, fieldName)) {
            continue;
          }
        }
        kb.append(fieldName);
        kb.append(suffix);
        result.put(kb.toString(), method);
      }
      return result;
    }

    protected Map<String, Method> getters;

    @Override
    public int size() {
      return getters.size();
    }

    @Override
    public boolean isEmpty() {
      return 0 == this.getters.size();
    }

    @Override
    public boolean containsKey(Object key) {
      return getters.containsKey(Utils.asString(key));
    }

    @Override
    public boolean containsValue(Object value) {
      throw new RuntimeException("containsValue is not implemented");
    }

    @Override
    public Object get(Object key) {
      Method getter = getters.get(Utils.asString(key));
      if (null != getter) {
        try {
          return getter.invoke(obj);
        } catch (IllegalAccessException ex) {
          throw new RuntimeException(ex);
        } catch (InvocationTargetException ex) {
          throw new RuntimeException(ex);
        }
      }
      return null;
    }

    @Override
    public Object put(String key, Object value) {
      throw new RuntimeException("put is not implemented");
    }

    @Override
    public Object remove(Object key) {
      throw new RuntimeException("remove is not implemented");
    }

    @Override
    public void putAll(Map<? extends String, ? extends Object> methods) {
      throw new RuntimeException("putAll is not implemented");
    }

    @Override
    public void clear() {
      throw new RuntimeException("clear is not implemented");
    }

    @Override
    public Set<String> keySet() {
      return getters.keySet();
    }

    @Override
    public Collection<Object> values() {
      Set<Object> values = new HashSet<Object>();
      for (Object key : getters.keySet()) {
        values.add(get(key));
      }
      return values;
    }

    @Override
    public Set<Map.Entry<String, Object>> entrySet() {
      Set<Map.Entry<String, Object>> entries = new HashSet<Map.Entry<String, Object>>();
      for (String key : getters.keySet()) {
        final String entryKey = key;
        entries.add(
            new Map.Entry<String, Object>() {
              @Override
              public String getKey() {
                return entryKey;
              }

              @Override
              public Object getValue() {
                return get(entryKey);
              }

              @Override
              public Object setValue(Object value) {
                return put(entryKey, value);
              }
            });
      }
      return entries;
    }

    @Override
    public String toString() {
      Iterator<Entry<String, Object>> iter = entrySet().iterator();
      if (!iter.hasNext()) {
        return "{}";
      }
      StringBuilder sb = new StringBuilder();
      sb.append('{');
      for (; ; ) {
        Entry<String, Object> entry = iter.next();
        Object key = entry.getKey();
        Object value = entry.getValue();
        sb.append(key == this ? "(this Map)" : key);
        sb.append('=');
        sb.append(value == this ? "(this Map)" : value);
        if (!iter.hasNext()) {
          return sb.append('}').toString();
        }
        sb.append(',').append(' ');
      }
      // return sb.toString();
    }
  }

  @SuppressWarnings("unchecked")
  protected static Object doSelectKeys(Object obj, Object ksObj) {
    if (obj == null || ksObj == null) {
      return Utils.map();
    } else if (obj instanceof Map) {
      return new FilteredMap((Map<Object, Object>) obj, ksObj);
    } else if (Seq.isCollection(obj)) {
      return new IndexMap(obj, ksObj);
    } else {
      return new BeanMap(obj, null, null, ksObj);
    }
  }

  @Arguments(spec = {"object", "keyseq"})
  @Docstring(text = "Returns a map containing only those entries in map whose key is in keys. ")
  @Package(name = Package.BASE_SEQ)
  public static class SELECT_KEYS extends FuncExp {
    @Override
    public Object evalWithArgs(final Backtrace backtrace, Eargs eargs) {
      final Object obj = eargs.get(0, backtrace);
      final Object ksObj = eargs.get(1, backtrace);
      return doSelectKeys(obj, ksObj);
    }
  }


  
  @Arguments(spec = {"object", ARG_OPTIONAL, "prefix", "suffix"})
  @Docstring(
      text =
          "Convert Java Bean to a Map. "
              + "Returns a Map based on getters in the passed java object. "
              + "Accepts optional prefix and suffics arguments that are used "
              + "to modify the generated keys.")
  @Package(name = Package.BASE_BINDINGS)
  public static class BEAN extends FuncExp {
    @Override
    public Object evalWithArgs(final Backtrace backtrace, Eargs eargs) {
      final Object obj = eargs.get(0, backtrace);
      return new BeanMap(
          obj,
          (String) ((eargs.size() > 1) ? eargs.get(1, backtrace) : null),
          (String) ((eargs.size() > 2) ? eargs.get(2, backtrace) : null));
    }
  }

  @Arguments(spec = {"class-spec"})
  @Docstring(
      text =
          "Return Class by Class Name."
              + "Return class object according to it's fully qualified class name. "
              + "class-spec may be string, symbol or any object,"
              + "which string representation will be used")
  @Package(name = Package.FFI)
  public static class CLASS extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      try {
        return Utils.strToClass(Utils.asString(eargs.get(0, backtrace)));
      } catch (ClassNotFoundException ex) {
        throw new ExecutionException(backtrace, ex);
      }
    }
  }

  @Arguments(spec = {"class", ArgSpec.ARG_OPTIONAL, "arglist", "typeslist"})
  @Docstring(
      text =
          "Return New Class Instance. Optional arglist and typeslist "
              + "parameters specify parameters to be passed to cosnstructor "
              + "and their types. When typelist not given it tries to find "
              + "most narrowly matching constructor on the basis of types of "
              + "the arguments in arglist. If typeslist is provided exactly "
              + "matching constructor will be used.")
  @Package(name = Package.FFI)
  public static class DOTN extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      Class<?> cls;
      try {
        cls = Utils.tspecToClass(eargs.get(0, backtrace));
      } catch (ClassNotFoundException ex) {
        throw new ExecutionException(backtrace, ex);
      }
      final List<?> constrArgs = (eargs.size() > 1) ? (List<?>) eargs.get(1, backtrace) : null;
      final List<?> tspecs = (eargs.size() > 2) ? (List<?>) eargs.get(2, backtrace) : null;
      Class<?>[] paramsClasses;
      try {
        paramsClasses = Utils.getMethodParamsClasses(constrArgs, tspecs);
      } catch (ClassNotFoundException clnf) {
        throw new RuntimeException(clnf);
      }
      Constructor<?> constr;
      try {
        if (null != tspecs) {
          constr = cls.getConstructor(paramsClasses);
        } else {
          BetterMethodFinder finder = new BetterMethodFinder(cls);
          constr = finder.findConstructor(paramsClasses);
        }
      } catch (NoSuchMethodException ex) {
        throw new RuntimeException(ex);
      }
      try {
        Object obj =
            (null == constrArgs) ? constr.newInstance() : constr.newInstance(constrArgs.toArray());
        return obj;
      } catch (InstantiationException ex) {
        throw new RuntimeException(ex);
      } catch (InvocationTargetException ex) {
        throw new RuntimeException(ex);
      } catch (IllegalAccessException ex) {
        throw new RuntimeException(ex);
      }
    }
  }

  public abstract static class FFI extends FuncExp {
    protected Object javaCall(Object object, List<?> parts) {
      try {
        for (int i = 0; i < parts.size(); i++) {
          String partname = Utils.asString(parts.get(i));
          boolean isMethod = false;
          List<?> methodParams = null;
          List<?> paramsTypesSpec = null;
          // List<?> methodParams = null;
          if (partname.endsWith("()")) {
            isMethod = true;
            partname = partname.substring(0, partname.length() - 2);
            // methodParams = new ArrayList<Object>();
          } else if (!Utils.isLast(parts, i) && (parts.get(i + 1) instanceof List)) {
            isMethod = true;
            methodParams = (List<?>) parts.get(i + 1);
            i++;
            if ((parts.size() > i + 1) && (parts.get(i + 1) instanceof List)) {
              paramsTypesSpec = (List<?>) parts.get(i + 1);
              i++;
            }
          }
          Class<?> cls;
          if (object instanceof Class) {
            cls = (Class<?>) object;
            object = null;
          } else {
            cls = object.getClass();
          }

          if (isMethod) {
            if (null != methodParams) {
              final Class<?>[] methodParamClasses =
                  Utils.getMethodParamsClasses(methodParams, paramsTypesSpec);
              Method method;
              if (null != paramsTypesSpec) {
                method = cls.getMethod(partname, methodParamClasses);
              } else {
                BetterMethodFinder finder = new BetterMethodFinder(cls);
                method = finder.findMethod(partname, methodParamClasses);
              }
              object = method.invoke(object, methodParams.toArray());
            } else {
              Method method = cls.getMethod(partname);
              object = method.invoke(object);
            }
          } else {
            Field field = cls.getField(partname);
            object = field.get(object);
          }
          if (null == object /*&& isLast(params,i)*/) {
            break;
          }
        }
        return object;
      } catch (NoSuchMethodException nfex) {
        throw new RuntimeException(nfex);
      } catch (IllegalAccessException nfex) {
        throw new RuntimeException(nfex);
      } catch (InvocationTargetException nfex) {
        throw new RuntimeException(nfex.getTargetException());
      } catch (NoSuchFieldException nfex) {
        throw new RuntimeException(nfex);
      } catch (ClassNotFoundException clnf) {
        throw new RuntimeException(clnf);
      }

    }

    protected List<Object> getCallParts(List<Object> specArgs) {
      final List<Object> parts = new ArrayList<Object>(specArgs.size());
      for (Object val : specArgs) {
        if (val instanceof List) {
          parts.add(val);
        } else {
          String spec = (val instanceof Symbol) ? ((Symbol) val).getName() : Utils.asString(val);
          String[] subparts = spec.split("\\.");
          for (String subpart : subparts) {
            parts.add(subpart);
          }
        }
      }
      return parts;
    }
  }

  // /** Usage:
  //  * (. obj "getList" () "get" (0) "field1" )
  //  * (. obj "get(0).toString()" )
  //  *
  //  */
  // FIXME: proper docstring
  @Arguments(spec = {"object", ArgSpec.ARG_REST, "call-args"})
  @Docstring(
      text =
          "Call Java Object Method/Read Field"
              + "Call method of java object or read contend of object field. ")
  @Package(name = Package.FFI)
  @SuppressWarnings("unchecked")
  public static class DOT extends FFI {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      final Object object = eargs.get(0, backtrace);
      final List<Object> parts = super.getCallParts((List<Object>) eargs.get(1, backtrace));
      final Object result = javaCall(object, parts);
      return result;
    }
  }

  @Arguments(spec = {"class", ArgSpec.ARG_REST, "call-args"})
  @Docstring(
      text =
          "Call Static Java Method/Read Static Field"
              + "Call method of java object or read contend of object field. ")
  @Package(name = Package.FFI)
  @SuppressWarnings("unchecked")
  public static class DOTS extends FFI {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      final List<Object> rest = (List<Object>) eargs.get(1, backtrace);
      final Object tspec = eargs.get(0, backtrace);
      final Class<?> cls;
      try {
        cls = Utils.tspecToClass(tspec);
      } catch (ClassNotFoundException ex) {
        throw new ExecutionException(backtrace, ex);
      }
      // final StringBuilder trail = new StringBuilder();
      final List<Object> parts = super.getCallParts(rest);
      final Object result = javaCall(cls, parts);
      return result;
    }
  }

  // ***** EXCEPTION HANDLING AND DEBUGGING
  @Package(name = Package.BASE_FUNCS)
  @Arguments(spec = {ArgSpec.ARG_OPTIONAL, "vars"})
  @Docstring(
      text = "Return callstack backtrace. "
             + "Returns string representation of current stack frame."
             + "If vars is a sequence print  bindings that are contained in a sequence."
             + "If vars is not a sequence print all bindings only if vars boolean value is true.")
  public static class BACKTRACE extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      return (null == backtrace)
          ? "* BACKTRACE: Call backtrace is not available, please call evaluate() with the"
              + " backtrace parameter *\n"
          : backtrace.toString(eargs.get(0, backtrace));
    }
  }

  @Arguments(spec = {"exception"})
  @Docstring(
      text =
          "Throw Java Exception. The exception may be a java Throwable object or String. In the"
              + " latter case a new ExecutionException with given message will be created and"
              + " thrown.")
  @Package(name = Package.BASE_CONTROL)
  public static class THROW extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      Object val = eargs.get(0, backtrace);
      ExecutionException ex;
      if (val instanceof ExecutionException) {
        ex = (ExecutionException) val;
        if (null == ex.getBacktrace()) {
          ex.setBacktrace(backtrace);
        }
      } else if (val instanceof Throwable) {
        ex = new ExecutionException(backtrace, (Throwable) val);
      } else {
        ex = new ExecutionException(backtrace, Utils.asString(val));
      }
      throw ex;
    }
  }


  @Arguments(spec = {ArgSpec.ARG_KEY, "message"})
  @Docstring(lines = {
      "Creates an Exception Object. ",
      "Returns new ExecutionException with given message."})
  @Package(name = Package.BASE_CONTROL)
  public static class EXCEPTION extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      Object val = eargs.get(0, backtrace);
      return new ExecutionException(Utils.asStringOrNull(val));
    }
  }

  
  @Arguments(spec = {"value"})
  @Docstring(text = "Return value from function")
  @Package(name = Package.BASE_CONTROL)
  public static class RETURN extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      Object val = eargs.get(0, backtrace);
      throw new ReturnException(val);
    }
  }
  
  // **** STRING HANDLING
  @Arguments(spec = {"pattern", ArgSpec.ARG_OPTIONAL, "flags"})
  @Docstring(
      text =
          "Compile A Regexp Pattern. On success returns a java.util.regex.Pattern objec. On error"
              + " raises exception.")
  @Package(name = Package.BASE_REGEX)
  public static class RE_PATTERN extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      final String patternStr = Utils.asString(eargs.get(0, backtrace));
      final String flagsStr = Utils.asStringOrNull(eargs.get(1, backtrace));
      if (null != flagsStr) {
        return Pattern.compile(patternStr, Utils.parseRegexpFlags(flagsStr));
      } else {
        return Pattern.compile(patternStr);
      }
    }
  }

  @Arguments(spec = {"pattern", ArgSpec.ARG_OPTIONAL, "flags"})
  @Docstring(
      text =
          "Compile a Globbing Pattern. "
              + "On success returns a java.util.regex.Pattern object. "
              + "On error raises exception.")
  @Package(name = Package.BASE_REGEX)
  public static class RE_GLOB extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      final String patternStr = Utils.asString(eargs.get(0, backtrace));
      final String flagsStr = Utils.asStringOrNull(eargs.get(1, backtrace));
      if (null != flagsStr) {
        return GlobPattern.compile(patternStr, Utils.parseRegexpFlags(flagsStr));
      } else {
        return GlobPattern.compile(patternStr);
      }
    }
  }

  protected static Matcher getMatcher(Eargs eargs, Backtrace backtrace) {
    final Object obj0 = eargs.get(0, backtrace);
    final Object obj1 = eargs.get(1, backtrace);
    if (null == obj1) {
      return (Matcher) obj0;
    } else {
      return ((Pattern) obj0).matcher((CharSequence) obj1);
    }
  }

  protected static Object returnGroups(Matcher matcher) {
    if (matcher.groupCount() > 0) {
      final List<Object> glist = new ArrayList<Object>(matcher.groupCount() + 1);
      glist.add(matcher.group());
      for (int i = 1; i <= matcher.groupCount(); i++) {
        glist.add(matcher.group(i));
      }
      return glist;
    } else {
      return matcher.group();
    }
  }

  @Arguments(spec = {"matcher"})
  @Docstring(
      text =
          "Return Groups for a Regexp Match. "
              + "Returns the groups from the most recent match/find.\n"
              + "If there are no nested groups, returns a string of the entire\n"
              + "match. If there are nested groups, returns a list of the groups,\n"
              + "the first element being the entire match.")
  @Package(name = Package.BASE_REGEX)
  public static class RE_GROUPS extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      return returnGroups((Matcher) eargs.get(0, backtrace));
    }
  }

  @Arguments(spec = {"pattern", ArgSpec.ARG_PIPE, "char-seq"})
  @Docstring(
      text =
          "Return Regexp Matcher. "
              + "Returns an instance of java.util.regex.Matcher, "
              + "for use, e.g. in RE-FIND.")
  @Package(name = Package.BASE_REGEX)
  public static class RE_MATCHER extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      final Pattern pattern = (Pattern) eargs.get(0, backtrace);
      final CharSequence charSeq = (CharSequence) eargs.get(1, backtrace);
      return pattern.matcher(charSeq);
    }
  }

  @Arguments(
      spec = {"arg0", ArgSpec.ARG_PIPE, ArgSpec.ARG_OPTIONAL, "arg2"},
      text = "{pattern schar-seq | matcher}")
  @Docstring(
      text =
          "Perform regexp match. When called With two arguments created java.util.regex.Matcher"
              + " using pattern and char-seq.\n"
              + "  When called with one arguments it uses given Matcher. \n"
              + " Returns the match, if any, of string to pattern, using Matcher.matches(). \n"
              + " if no groups were defined it returns the matched string.\n"
              + " If groups were defined it returns a list consisting of the full match and matched"
              + " groups\n"
              + " If there is no match NIL is returned")
  @Package(name = Package.BASE_REGEX)
  public static class RE_MATCHES extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      final Matcher m = getMatcher(eargs, backtrace);
      if (m.matches()) {
        return returnGroups(m);
      } else {
        return null;
      }
    }
  }

  @Arguments(
      spec = {"arg0", ArgSpec.ARG_PIPE, ArgSpec.ARG_OPTIONAL, "arg2"},
      text = "{pattern schar-seq | matcher}")
  @Docstring(
      text =
          "Perform Regexp Find. When called With two arguments creates java.util.regex.Matcher"
              + " using pattern and char-seq.\n"
              + "  When called with one arguments it uses given Matcher. \n"
              + " Returns the next ,match, if any, of string to pattern, using Matcher.find(). \n"
              + " if no groups were defined it returns the matched string.\n"
              + " If groups were defined it returns a list consisting of the full match and matched"
              + " groups\n"
              + " If there is no match NIL is returned")
  @Package(name = Package.BASE_REGEX)
  public static class RE_FIND extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      final Matcher m = getMatcher(eargs, backtrace);
      if (m.find()) {
        return returnGroups(m);
      } else {
        return null;
      }
    }
  }

  @Arguments(
      spec = {"arg0", ArgSpec.ARG_PIPE, ArgSpec.ARG_OPTIONAL, "arg2"},
      text = "{pattern schar-seq | matcher}")
  @Docstring(
      text =
          "Return Results of Regexp Find as a Lazy Sequence. When called With two arguments created"
              + " java.util.regex.Matcher using pattern and char-seq.\n"
              + "  Returns lazy iterable sequence (instance of Iterable) of matches of string to"
              + " pattern, using Matcher.find(). \n"
              + " When called with one arguments it uses given Matcher. \n"
              + " if no groups were defined the elements of the sequence are the matched string.\n"
              + " If groups were defined it returns a list consisting of the full match and matched"
              + " groups\n"
              + " If there is no match empty sequence is returned")
  @Package(name = Package.BASE_REGEX)
  public static class RE_SEQ extends FuncExp {
    @Override
    protected Iterable<Object> evalWithArgs(final Backtrace backtrace, final Eargs eargs) {
      return new Iterable<Object>() {
        @Override
        public Iterator<Object> iterator() {

          return new Iterator<Object>() {
            final Matcher matcher = getMatcher(eargs, backtrace);
            boolean findResult = matcher.find();

            @Override
            public boolean hasNext() {
              synchronized (matcher) {
                return findResult;
              }
            }

            @Override
            public synchronized Object next() {
              synchronized (matcher) {
                final Object result = returnGroups(matcher);
                // FIXME: call only if requested
                findResult = matcher.find();
                return result;
              }
            }
          };
        }
      };
    }
  }

  @Arguments(spec = {"elt", ArgSpec.ARG_PIPE, "col"})
  @Docstring(text = "Check if an element is contained in a collection. ")
  @Package(name = Package.BASE_SEQ)
  public static class IN extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      if (2 != eargs.size()) {
        throw new ExecutionException(
            backtrace, "Unexpected number of arguments: expected 2 but got " + eargs.size());
      }
      Object elt = eargs.get(0, backtrace);
      Object seq = eargs.get(1, backtrace);
      if (seq instanceof Map) {
        return ((Map<?,?>) seq).containsValue(elt);
      }
      final boolean[] holder = new boolean[1];
      // FIXME: optimize for specigic types
      Seq.forEach(
          seq,
          new Operation() {
            public boolean perform(Object obj) {
              if (null != obj && obj.equals(elt)) {
                return (holder[0] = true);
              } else if (null == obj && null == elt) {
                return (holder[0] = true);
              }
              return false;
            }
          },
          false);
      return holder[0];
    }
  }

  @Arguments(spec = {"structure", "ks", "&OPTIONAL", "not-found"})
  @Docstring(
      text =
          "Returns the value from an hierarchy of associative structures. \n"
              + "Return value from an associative structure struct, \n"
              + "where ks is a sequence of keys. Returns NIL if the key\n "
              + "is not present, or the not-found value if supplied.")
  @Package(name = Package.BASE_SEQ)
  public static class GET_IN extends FuncExp implements LValue {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      final int argsnum = eargs.size();
      if (argsnum != 2 && argsnum != 3) {
        throw new ExecutionException(
            backtrace, "Unexpected number of arguments: expected 2 or 3, but got " + eargs.size());
      }
      final Object obj = eargs.get(0, backtrace);
      final Object ksObj = eargs.get(1, backtrace);
      final Object notDefined = argsnum == 2 ? null : eargs.get(2, backtrace);
      final Object result = Seq.doGetIn(obj, ksObj, 0, notDefined);
      return result;
    }

    @Override
    public Object doSet(Backtrace backtrace, ICtx ctx, Object value) {
      Eargs eargs = this.evaluateParameters(backtrace, ctx);
      final int argsnum = eargs.size();
      if (argsnum != 2 && argsnum != 3) {
        throw new ExecutionException(
            backtrace, "Unexpected number of arguments: expected 2 or 3, but got " + eargs.size());
      }
      final Object obj = eargs.get(0, backtrace);
      final Object ksObj = eargs.get(1, backtrace);
      // FIXME: should be any sequence probably
      // FIXME: check type and give normal error
      setIn(obj, (List<?>)ksObj, value, null);
      return value;
    }
  }

  protected static Object setIn(final Object target,
                                final List<?> path,
                                Object value,
                                Object newCol) {
    return setIn(target, path, value, 0, newCol);
  }

  protected static Object setIn(final Object target,
                                final List<?> path,
                                Object value,
                                int index,
                                Object newCol) {
    try {
      final int size = path.size();
      if (size == 0) {
        return value;
      }
      //Map<Object,Object> updateMap = (Map) target;
      if (size - index == 1) {
        Seq.setElement(target, path.get(index), value);
        return target;
      }
      Object key = path.get(index);
      Object subTarget = Seq.getElementByKeyOrIndex(target, key);
      boolean cont = false;
      if (Seq.isAssociative(subTarget)) {
        cont = true;
      } else {
        if (subTarget == null) {
          // FIXME: must be configurable by key args
          if (null == newCol) {
            throw new RuntimeException("Cannot set key at level "
                                       + index + " because current item is NIL");
          }
          subTarget = Seq.shallowClone(newCol);
          Seq.setElement(target, key, subTarget);
          cont = true;
        } else {
          throw new RuntimeException("Cannot set key at level "
                                     + index + " because current item is " + subTarget.getClass());
        }
      }
      if (cont) {
        setIn(subTarget, path, value, index + 1, newCol);
      }
      //setIn(subTarget, path, value, index + 1, newCol);    
      return target;
    } catch (IndexOutOfBoundsException ex) {
      throw new ExecutionException(ex);
    }
  }


  @Arguments(spec = {"structure", "keys", "value","&OPTIONAL", "new-col"})
  @Docstring(lines = {"Put value into an hierarchy of associative structures according list of keys ks. ",
                      "Returns copy of the structure with the required modification."})
  @Package(name = Package.BASE_SEQ)
  @SuppressWarnings("unchecked")
  public static class PUT_IN extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      final int argsnum = eargs.size();
      if (argsnum != 4) {
        throw new ExecutionException(
            backtrace, "Unexpected number of arguments: expected 4, but got " + eargs.size());
      }
      final Object src = eargs.get(0, backtrace);
      final List<Object> ksObj = (List<Object>)eargs.get(1, backtrace);
      final Object object = eargs.get(2, backtrace);
      final Object nf = eargs.get(3, backtrace);
      final Object target = deepCopy(src, backtrace);
      final Object result = setIn(target, ksObj, object, nf);
      return result;
    }
  }
  
  @Arguments(spec = {"structure", "ks", "value","&OPTIONAL", "new-col"})
  @Docstring(text = "Put value into an hierarchy of associative structures "
             + "according list of keys ks.")
  @Package(name = Package.BASE_SEQ)
  @SuppressWarnings("unchecked")
  public static class NPUT_IN extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      final int argsnum = eargs.size();
      if (argsnum != 4) {
        throw new ExecutionException(
            backtrace, "Unexpected number of arguments: expected 4, but got " + eargs.size());
      }
      final Object target = eargs.get(0, backtrace);
      final List<Object> ksObj = (List<Object>)eargs.get(1, backtrace);
      final Object object = eargs.get(2, backtrace);
      final Object nf = eargs.get(3, backtrace);
      final Object result = setIn(target, ksObj, object, nf);
      return result;
    }
  }

  @Arguments(spec = {"structure", "key", "&OPTIONAL", "not-found"})
  @Docstring(
      text =
          "Returns the value from an associative structure. \n"
              + "Return value from an associative structure struct, \n"
              + " Returns NIL if the key is not present, or the not-found value if supplied.")
  @Package(name = Package.BASE_SEQ)
  public static class GET extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      final int argsnum = eargs.size();
      if (argsnum != 2 && argsnum != 3) {
        throw new ExecutionException(
            backtrace, "Unexpected number of arguments: expected 2 or 3, but got " + eargs.size());
      }
      final Object obj = eargs.get(0, backtrace);
      final Object keyObj = eargs.get(1, backtrace);
      final Object notDefined = argsnum > 2 ? eargs.get(2, backtrace) : null;
      final Object[] result = new Object[1];
      return Seq.doGet(obj, result, keyObj) ? result[0] : notDefined;
    }
  }

  @Arguments(spec = {"seq", "keyidx"})
  @Docstring(text = "Check whether Map or indexed sequence has given key or index.")
  @Package(name = Package.BASE_SEQ)
  public static class HASKEY extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      return Seq.containsKey(eargs.get(0, backtrace), eargs.get(1, backtrace));
    }
  }

  @Arguments(
      text = "map {key val}+",
      spec = {"map", "key", "val", ArgSpec.ARG_REST, "kvpairs"})
  @Docstring(
      text =
          "Associates values with keys in an map structure. \n"
              + "Return new instance of the structure, the original is left unchanged.")
  @Package(name = Package.BASE_SEQ)
  @SuppressWarnings("unchecked")
  public static class ASSOC extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      Map<Object, Object> result = null;
      final int argsnum = eargs.size();
      if (argsnum < 1) {
        throw new ExecutionException(
            backtrace,
            "Unexpected number of arguments: expected at least one argument, but got "
                + eargs.size());
      }
      final Object obj = eargs.get(0, backtrace);
      // FIXME: use same type?
      // FIXME: data sharing?
      result = new HashMap<Object, Object>();
      if (null != obj) {
        result.putAll((Map<Object, Object>) obj);
      }
      doMapAssoc(result, eargs, backtrace);
      return result;
    }
  }

  @Arguments(
      text = "map {key val}+",
      spec = {"map", "key", "val", ArgSpec.ARG_REST, "kvpairs"})
  @Docstring(
      text =
          "Associates values with keys in an map structure. \n"
              + "Modifies the object and returns it as the result.")
  @Package(name = Package.BASE_SEQ)
  @SuppressWarnings("unchecked")
  public static class NASSOC extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      Map<Object, Object> result = null;
      if (eargs.size() < 1) {
        throw new ExecutionException(
            backtrace,
            "Unexpected number of arguments: expected at least one argument, but got "
                + eargs.size());
      }
      final Object obj = eargs.get(0, backtrace);
      result = null == obj ? new HashMap<Object, Object>() : (Map<Object, Object>) obj;
      doMapAssoc(result, eargs, backtrace);
      return result;
    }
  }

  protected static void doMapAssoc(Map<Object, Object> target, Eargs eargs, Backtrace backtrace) {
    final int argsnum = eargs.size();
    target.put(eargs.get(1, backtrace), eargs.get(2, backtrace));
    if (argsnum > 3) {
      List<?> rest = (List<?>) eargs.get(3, backtrace);
      for (int i = 0; i < rest.size(); i += 2) {
        final Object k = rest.get(i);
        final Object v = i + 1 < rest.size() ? rest.get(i + 1) : null;
        target.put(k, v);
      }
    }
  }


  @Arguments(spec = {"value"})
  @Docstring(
      text =
      "Convert character, string or character sequence to upper case.")
  @Package(name = Package.BASE_TEXT)
  public static class UPPERCASE extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      final Object obj =  eargs.get(0, backtrace);
      if (obj == null) {
        return null;
      } else if (obj instanceof String) {
        return ((String) obj).toUpperCase();
      } else if (obj instanceof Character) {
        return Character.toUpperCase((Character) obj);
      } else if (obj instanceof StringBuilder) {
        return new StringBuilder(((StringBuilder) obj).toString().toUpperCase());
      } else if (obj instanceof StringBuffer) {
        return new StringBuffer(((StringBuffer) obj).toString().toUpperCase());
      } else {
        return obj;
      }
    }
  }

  @Arguments(spec = {"value"})
  @Docstring(
      text =
      "Convert character, string or character sequence to lower case.")
  @Package(name = Package.BASE_TEXT)
  public static class LOWERCASE extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      final Object obj =  eargs.get(0, backtrace);
      if (obj == null) {
        return null;
      } else if (obj instanceof String) {
        return ((String) obj).toLowerCase();
      } else if (obj instanceof Character) {
        return Character.toLowerCase((Character) obj);
      } else if (obj instanceof StringBuilder) {
        return new StringBuilder(((StringBuilder) obj).toString().toLowerCase());
      } else if (obj instanceof StringBuffer) {
        return new StringBuffer(((StringBuffer) obj).toString().toLowerCase());
      } else {
        return obj;
      }
    }
  }

  @Arguments(spec = {"format", ArgSpec.ARG_REST, "values"})
  @Docstring(
      text =
          "Format String. "
              + "Returns a formatted string using the specified format string (in the "
              + "format of java.util.Formatter) and arguments. Arguments referenced by "
              + "the format specifiers in the format string. If there are more "
              + "arguments than format specifiers, the extra arguments are "
              + "ignored. Throws IllegalFormatException - If a format string contains "
              + "an illegal syntax, a format specifier that is incompatible with the "
              + "given arguments, insufficient arguments given the format string, or "
              + "other illegal conditions.")
  @Package(name = Package.BASE_TEXT)
  public static class FORMAT extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      String format = (String) eargs.get(0, backtrace);
      return String.format(format, ((List<?>) eargs.get(1, backtrace)).toArray());
    }
  }

  @Arguments(spec = {ArgSpec.ARG_REST, "args"})
  @Docstring(text = "Print Arguments on standard output.")
  @Package(name = Package.IO)
  public static class PRINT extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      StringBuffer buf = new StringBuffer();
      for (Object val : (List<?>) eargs.get(0, backtrace)) {
        buf.append(Utils.asString(val));
      }
      System.out.print(buf.toString());
      return buf.toString();
    }
  }

  @Arguments(spec = {ArgSpec.ARG_REST, "args"})
  @Docstring(text = "Print Arguments on standard output and print newline.")
  @Package(name = Package.IO)
  public static class PRINTLN extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      StringBuffer buf = new StringBuffer();
      for (Object val : (List<?>) eargs.get(0, backtrace)) {
        buf.append(Utils.asString(val));
      }
      System.out.println(buf.toString());
      return buf.toString();
    }
  }

  @Arguments(spec = {ArgSpec.ARG_REST, "args"})
  @Docstring(text = "Create and initialize a StringBuilder object. "
             + "Return stringbuilder with all the arguments concatenated.")
  @Package(name = Package.BASE_TEXT)
  public static class STRINGBUILDER extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      List<?> rest = (List<?>) eargs.get(0, backtrace);
      final StringBuilder b  = new StringBuilder();
      for (Object val : rest) {
        if (null != val) {
          b.append(Utils.asStringOrNull(val));
        }
      }
      return b;
    }
  }


  @Arguments(spec = {ArgSpec.ARG_REST, "args"})
  @Docstring(text = "Create and initialize a StringBuffer object. "
             + "Return stringbuilder with all the arguments concatenated.")
  @Package(name = Package.BASE_TEXT)
  public static class STRINGBUFFER extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      List<?> rest = (List<?>) eargs.get(0, backtrace);
      final StringBuffer b  = new StringBuffer();
      for (Object val : rest) {
        if (null != val) {
          b.append(Utils.asStringOrNull(val));
        }
      }
      return b;
    }
  }

  @Arguments(spec = {ArgSpec.ARG_OPTIONAL, "n"})
  @Docstring(
      text =
          "Access command line arguments. "
              + "When n is provided return nth argument as String, "
              + "when not -- return list of command line arguments. "
              + "If n is out of range return NIL.")
  @Package(name = Package.RUNTIME)
  public static class ARGV extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      List<String> args = eargs.getCompiler().getCommandlineArgs();
      Object argObj = eargs.get(0, backtrace);
      int n = Utils.asNumber(argObj).intValue();
      if (null == argObj) {
        return Utils.copySeq(args);
      } else if (args.size() <= n) {
        return null;
      } else {
        return args.get(n);
      }
    }
  }

  @Arguments(spec = {ArgSpec.ARG_REST, "values"})
  @Docstring(
      text =
          "Concatenate Strings. Returns concatenation "
              + "of string representationx of the function arguments. NIL arguments are ignored.")
  @Package(name = Package.BASE_TEXT)
  public static class STR extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      StringBuffer buf = new StringBuffer();
      for (Object val : (List<?>) eargs.get(0, backtrace)) {
        if (null != val) {
          buf.append(Utils.asString(val));
        }
      }
      return buf.toString();
    }
  }

  // ****** SEQUENCES
  @Arguments(spec = {ArgSpec.ARG_REST, "sequences"})
  @Docstring(
      text =
          "Concatenate sequences (destructive). "
              + "Adds to the first given sequence (target sequence) all the elements of "
              + "all of the following sequences and return the target sequence.  If no "
              + "sequences were given an empty list will be returned. Target sequence "
              + "must be extendable, that means that objects like Arrays or String "
              + "cannot be target of this operation")
  @Package(name = Package.BASE_SEQ)
  public static class NAPPEND extends APPEND {
    public NAPPEND() {
      super();
      isDestructive = true;
    }
  }

  private static Seq.Operation mkArraySetter(
      final Object arr, final int[] counter, Class<?> componentType) {
    if (componentType.isPrimitive()) {
      if (componentType.equals(Character.TYPE)) {
        return new Seq.Operation() {
          @Override
          public boolean perform(Object obj) {
            Utils.aset(arr, counter[0]++, Utils.asChar(obj));
            return false;
          }
        };
      } else if (componentType.equals(Boolean.TYPE)) {
        return new Seq.Operation() {

          @Override
          public boolean perform(Object obj) {
            Utils.aset(arr, counter[0]++, Utils.asBoolean(obj));
            return false;
          }
        };
      } else {
        // six numeric types
        return new Seq.Operation() {
          @Override
          public boolean perform(Object obj) {
            Utils.aset(arr, counter[0]++, Utils.asNumber(obj));
            return false;
          }
        };
      }
    } else {
      return new Seq.Operation() {

        @Override
        public boolean perform(Object obj) {
          Utils.aset(arr, counter[0]++, obj);
          return false;
        }
      };
    }
  }

  private static Object charSeqSubseq(
      Class<?> clz, Backtrace bt, CharSequence seq, int start, Integer end) {
    final int siz = seq.length();
    return seq.subSequence(start, null == end ? siz : (end >= siz ? siz : end));
  }

  private static Object arraySubseq(
      Class<?> clz, Backtrace bt, Object arrayObj, int start, Integer end) {
    final Class<?> componentType = clz.getComponentType();
    final int siz = Array.getLength(arrayObj);
    final int endPos = null == end ? siz : (end >= siz ? siz : end);
    final Object result = Array.newInstance(componentType, endPos - start);
    final int[] counter = new int[1];
    final Operation setter = mkArraySetter(result, counter, componentType);
    for (int i = start; i < endPos; i++) {
      setter.perform(Array.get(arrayObj, i));
    }
    return result;
  }

  protected static List<?> listSubseq(
      Class<?> clz, Backtrace bt, List<?> lst, int start, Integer end) {
    final int siz = lst.size();
    return lst.subList(start, null == end ? siz : (end >= siz ? siz : end));
  }

  protected static Object seqSubseq(Object seqObj, Backtrace backtrace, int start, Integer end) {
    Class<?> clz = null == seqObj ? Utils.defListClz() : seqObj.getClass();
    if (clz.isArray()) {
      return arraySubseq(clz, backtrace, seqObj, start, end);
    } else if (CharSequence.class.isAssignableFrom(clz)) {
      return charSeqSubseq(clz, backtrace, (CharSequence) seqObj, start, end);
    } else if (List.class.isAssignableFrom(clz)) {
      return listSubseq(clz, backtrace, (List<?>) seqObj, start, end);
    } else {
      throw new ExecutionException(backtrace, "SUBSEQ: Unsupported sequence type: " + clz);
    }
  }

  @Arguments(spec = {"n", ArgSpec.ARG_PIPE, "seq"})
  @Docstring(
      text =
          "Return  first n elements of a sequence. "
              + "take creates new sequence with first n elements of seq. "
              + "If n is bigger than length of the sequence all the elements"
              + "are returned. The result subsequence is of the same kind as sequence.")
  @Package(name = Package.BASE_SEQ)
  public static class TAKE extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      final int size = eargs.size();
      if (2 != size) {
        throw new ExecutionException(backtrace, "Unexpected number of arguments: " + size);
      }
      final Object seqObj = eargs.get(1, backtrace);
      if (null == seqObj) {
        throw new ExecutionException("sequence parameter cannot be NIL");
      }
      final Object endObj = eargs.get(0, backtrace);
      final int end = (null == endObj) ? 0 : Utils.asNumber(endObj).intValue();
      return seqSubseq(seqObj, backtrace, 0, end);
    }
  }

  @Arguments(spec = {"sequence", "start", ArgSpec.ARG_OPTIONAL, "end"})
  @Docstring(
      text =
          "Return subsequence of a sequence. "
              + "subseq creates a sequence that is a copy of the subsequence of "
              + "sequence bounded by start and end. Start specifies an offset into the "
              + "original sequence and marks the beginning position of the "
              + "subsequence. end marks the position following the last element of the "
              + "subsequence. subseq always allocates a new sequence for a result; it "
              + "never shares storage with an old sequence. The result subsequence is "
              + "of the same kind as sequence.")
  @Package(name = Package.BASE_SEQ)
  public static class SUBSEQ extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      final int size = eargs.size();
      if (3 != size) {
        throw new ExecutionException(backtrace, "Unexpected number of arguments: " + size);
      }
      final Object seqObj = eargs.get(0, backtrace);
      if (null == seqObj) {
        throw new ExecutionException("sequence parameter cannot be NIL");
      }
      final int start = Utils.asNumber(eargs.get(1, backtrace)).intValue();
      final Object endObj = eargs.get(2, backtrace);
      final Integer end = (null == endObj) ? null : Utils.asNumber(endObj).intValue();
      return seqSubseq(seqObj, backtrace, start, end);
    }
  }

  // FIXME: list types
  protected static Object deepCopy(List lst, Backtrace backtrace) throws ExecutionException {
    final int size = lst.size();
    List copy = new ArrayList(size);
    for (int idx = 0; idx < size; idx++) {
      final Object item = lst.get(idx);
      final Object itemCopy = deepCopy(item, backtrace);
      copy.add(itemCopy);
    }
    return copy;
  }

  // FIXME: Set types
  protected static Object deepCopy(Set set, Backtrace backtrace) throws ExecutionException {
    final int size = set.size();
    final Set copy = new HashSet(size);
    for (Object item : set) {
      final Object itemCopy = deepCopy(item, backtrace);
      copy.add(itemCopy);
    }
    return copy;
  }

  // FIXME: Set types
  protected static Map deepCopy(Map<?, ?> map, Backtrace backtrace) throws ExecutionException {
    final int size = map.size();
    final HashMap copy = new HashMap(size);
    for (Map.Entry<?, ?> item : map.entrySet()) {
      final Object key = item.getKey();
      final Object value = item.getValue();
      final Object keyCopy = deepCopy(key, backtrace);
      final Object valueCopy = deepCopy(value, backtrace);
      copy.put(keyCopy, valueCopy);
    }
    return copy;
  }

  // FIXME: very crude, need to care for arrays, collection types
  protected static Object deepCopy(Object obj, Backtrace backtrace) throws ExecutionException {
    Object copy;
    if (obj == null) {
      copy =  null;
    } else if (obj instanceof List) {
      copy = deepCopy((List)obj, backtrace);
    } else if (obj instanceof Set) {
      copy = deepCopy((Set)obj, backtrace);
    } else if (obj instanceof Map) {
      copy = deepCopy((Map) obj, backtrace);
      /*} FIXME: else if (obj.getClass().isArray()) {
        copy = deepCopyArray(obj, backtrace);*/
    } else {
      copy = shallowCopy(obj, backtrace);
    }
    return copy;
  }

  protected static Object shallowCopy(Object obj, Backtrace backtrace) throws ExecutionException {
    if (Utils.isKnownImmutable(obj)) {
      return obj;
    }
    Exception seqError = null;
    if (Seq.isSeq(obj)) {
      try {
        return Seq.shallowClone(obj);
      } catch (Exception ex) {
        seqError = ex;
      }
    }
    Exception cloneError = null;
    if (obj instanceof Cloneable) {
      try {
        return Utils.cloneObjectByClone((Cloneable) obj);
      } catch (Exception ex) {
        cloneError = ex;
      }
    }
    Exception constrError = null;
    try {
      return Utils.copyObjectByCopyConstructor(obj);
    } catch (Exception ex) {
      constrError = ex;
    }
    throw new
      ExecutionException(backtrace,
                         "Failed to copy object:"
                         + (null != seqError
                            ? " Seq.shallowClone: " + seqError.getMessage() + ";"
                            : "")
                         + (null != cloneError
                            ? " clone: " + cloneError.getMessage() + ";"
                            : "")
                         + (null != constrError
                            ? " copy constr uctor: " + constrError.getMessage() + ";"
                            : ""));
  }
  
  @Arguments(spec = {"object"})
  @Docstring(text = "Perform shallow copy of an object.")
  @Package(name = Package.BASE_SEQ)
  public static class COPY extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      final int size = eargs.size();
      if (1 != size) {
        throw new ExecutionException(backtrace, "Unexpected number of arguments: " + size);
      }
      final Object obj = eargs.get(0, backtrace);
      return shallowCopy(obj, backtrace);
    }
  }

  @Arguments(spec = {"object"})
  @Docstring(text = "Perform deep copy of an object.")
  @Package(name = Package.BASE_SEQ)
  public static class DEEP_COPY extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      final Object obj = eargs.get(0, backtrace);
      return deepCopy(obj, backtrace);
    }
  }

    
  @Arguments(spec = {ArgSpec.ARG_REST, "sequences"})
  @Docstring(lines = {
      "Concatenate sequences (non-destructive). ",
      "`append` returns a new sequence that is the concatenation of the ",
      "elements of the arguments. All the arguments remain unchanged. The ",
      "resulting sequence is of the same type as the first argument. In no ",
      "arguments were given, an empty list is returned. If target sequence is",
      "an array, necessary coercions according to the type of array elements",
      "will be performed automatically."})
  @Package(name = Package.BASE_SEQ)
  public static class APPEND extends FuncExp {
    protected boolean isDestructive;

    public APPEND() {
      isDestructive = false;
    }

    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      final int size = eargs.size();
      if (1 != size) {
        throw new ExecutionException(backtrace, "Unexpected number of arguments: " + size);
      }
      List<?> seqs = (List<?>) eargs.get(0, backtrace);
      if (seqs.size() == 0) {
        return Utils.list();
      }
      Object firstSeq = seqs.get(0);
      Class<?> clz = null == firstSeq ? Utils.defListClz() : firstSeq.getClass();
      if (clz.isArray()) {
        return arrayAppend(clz, backtrace, seqs);
      } else if (CharSequence.class.isAssignableFrom(clz)) {
        return charSeqAppend(clz, backtrace, seqs);
      } else if (Collection.class.isAssignableFrom(clz)) {
        return colAppend(clz, backtrace, seqs);
      } else {
        throw new ExecutionException(backtrace, "Unsupported sequence type: " + clz);
      }
    }

    private Object charSeqAppend(Class<?> clz, Backtrace bt, List<?> seqs) {
      // FIXME: support other types but StringBuilder
      Object target = seqs.get(0);
      if (isDestructive && !
          (target instanceof Appendable)) {
        throw new ExecutionException(
            bt, "Unsupported sequence type " + clz + " for being target of destructive operation");
      }
      Appendable result;
      Object resultObj = null;
      boolean subst = false;
      if (isDestructive) {
        result = (Appendable) target;
      } else {
        try {
          final Constructor<?> constr = clz.getConstructor();
          resultObj = constr.newInstance();
        } catch (Exception ex) {
          subst = true;
          resultObj = new StringBuilder();
        }
        if (resultObj instanceof Appendable) {
          result = (Appendable) resultObj;
        } else {
          subst = true;
          result = new StringBuilder();
        }
      }
      final Appendable appendable = result;
      final int numArgs = seqs.size();
      for (int i = isDestructive ? 1 : 0; i < numArgs; i++) {
        Object seq = seqs.get(i);
        Seq.forEach(
            seq,
            new Seq.Operation() {
              @Override
              public boolean perform(Object obj) {
                try {
                  appendable.append(Utils.asChar(obj));
                } catch (IOException ioex) {
                  throw new ExecutionException(bt, ioex.getMessage());
                }
                return false;
              }
            },
            true);
      }
      if (subst) {
        // FIXME: use constructor(string)?
        return appendable.toString();
      }
      return appendable;
    }

    private Object arrayAppend(Class<?> clz, Backtrace bt, List<?> seqs) {
      if (isDestructive) {
        throw new ExecutionException(
            bt, "Unsupported sequence type " + clz + " for being target of sequence extension");
      }
      int totalLength = 0;
      final int numArgs = seqs.size();
      // FIXME: support for sequences that cannot return their size
      for (int i = 0; i < numArgs; i++) {
        final Object seq = seqs.get(i);
        totalLength += Seq.getLength(seq, true);
      }
      final Class<?> componentType = clz.getComponentType();
      Object result = Array.newInstance(componentType, totalLength);
      final int[] counter = new int[1];

      for (int i = 0; i < numArgs; i++) {
        final Object seq = seqs.get(i);
        Seq.forEach(seq, mkArraySetter(result, counter, componentType), true);
      }
      return result;
    }

    protected Collection<?> colAppend(Class<?> clz, Backtrace bt, List<?> seqs) {
      Object result = null;
      try {
        // result = isDestructive ? seqs.get(0) : clz.newInstance();
        result = isDestructive ? seqs.get(0) : clz.getConstructor().newInstance();
      } catch (InstantiationException ex) {
        throw new ExecutionException(bt, ex);
      } catch (IllegalAccessException ex) {
        throw new ExecutionException(bt, ex);
      } catch (InvocationTargetException ex) {
        throw new ExecutionException(bt, ex);
      } catch (NoSuchMethodException ex) {
        throw new ExecutionException(bt, ex);
      }

      @SuppressWarnings("unchecked")
      final Collection<Object> resultCol = (Collection<Object>) result;
      final int numArgs = seqs.size();
      for (int i = isDestructive ? 1 : 0; i < numArgs; i++) {
        Object seq = seqs.get(i);
        Seq.forEach(
            seq,
            new Seq.Operation() {
              @Override
              public boolean perform(Object obj) {
                resultCol.add(obj);
                return false;
              }
            },
            true);
      }
      return resultCol;
    }
  }

  @Arguments(spec = {"object-1", "object-2"})
  @Docstring(text = "Prepend element to a list.")
  @Package(name = Package.BASE_SEQ)
  public static class CONS extends FuncExp {
    @Override
    @SuppressWarnings("unchecked")
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      final Object v1 = eargs.get(0, backtrace);
      final Object v2 = eargs.get(1, backtrace);
      List<Object> resultList;
      // FIXME:  use same list type, handle immutable lists
      if (Utils.asObject(v2) instanceof List) {
        final List<Object> l2 = (List<Object>) Utils.asObject(v2);
        resultList = new ArrayList<Object>(l2.size() + 1);
        resultList.add(0, v1);
        resultList.addAll(1, (List<Object>) Utils.asObject(v2));
      } else {
        resultList = new ArrayList<Object>(2);
        resultList.add(0, v1);
        resultList.add(1, v2);
      }
      return resultList;
    }
  }

  @Arguments(spec = {"sequence"})
  @Docstring(
      text =
          "Returns the first element of the sequence. Returns NIL when "
              + "sequence is NIL or empty")
  @Package(name = Package.BASE_SEQ)
  public static class FIRST extends NTH {
    @Override
    // @SuppressWarnings("unchecked")
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      Object seq = eargs.get(0, backtrace);
      return Seq.getElementByIndex(seq, 0);
    }
  }

  @Arguments(spec = {"sequence"})
  @Docstring(
      text =
          "Return length of a sequence. Parameter may be any supported sequence (collection, array,"
              + " character sequence) or NIL (0 will be returned).")
  @Package(name = Package.BASE_SEQ)
  public static class LENGTH extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      return Seq.getLength(eargs.get(0, backtrace), true);
    }
  }

  @Arguments(spec = {ArgSpec.ARG_REST, "args"})
  @Docstring(text = "Create a list. Returns a list containing the supplied objects. ")
  @Package(name = Package.BASE_SEQ)
  public static class LIST extends FuncExp implements LValue {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      List<?> rest = (List<?>) eargs.get(0, backtrace);
      final List<Object> lst = new ArrayList<Object>(rest.size());
      for (Object val : rest) {
        lst.add(val);
      }
      return lst;
    }

    @Override
    public Object doSet(Backtrace backtrace, ICtx ctx, Object value) {
      // rest parameter
      ICompiled args = this.argList.get(0);
      if (args instanceof ValueExpr) {
        // list of target objects
        final List<?> lst = (List<?>) ((ValueExpr) args).value;
        final int siz = lst.size();
        int cnt = 0;
        if (null != value) { // treat NIL as empty list
          // for each member of  source seq
          cnt += Seq.forEach(value, new Seq.Operation() {
              protected int idx = 0;

              @Override
              public boolean perform(Object obj) {
                if (idx < siz) {
                  Object arg = lst.get(idx);
                  if (arg instanceof LValue) {
                    ((LValue) arg).doSet(backtrace, ctx, obj);
                  }
                  idx++;
                  return false;
                } else {
                  return true;
                }
              }
            }, false);
        }
        // assign NIL to the rest of unassigned objects;
        for (; cnt < siz; cnt++) {
          Object arg = lst.get(cnt);
          if (arg instanceof LValue) {
            ((LValue) arg).doSet(backtrace, ctx, null);
          }
        }
      } else {
        throw new RuntimeException("Internal error: list value is not instance of ValueExpr!");
      }
      return null;

      /*Eargs eargs = this.evaluateParameters(backtrace, ctx);
      final Object arrayObj = Utils.asObject(eargs.get(0, backtrace));
      final int index = Utils.asNumber(eargs.get(1, backtrace)).intValue();
      try {
        return Seq.setElementByIndex(arrayObj, index, value);
      } catch (IndexOutOfBoundsException ex) {
        throw new ExecutionException(ex);
        }*/
      //return true;
    }
  }

  @Arguments(spec = {ArgSpec.ARG_REST, "args"})
  @Docstring(text = "Create a HashSet. Returns a set containing the supplied objects. ")
  @Package(name = Package.BASE_SEQ)
  public static class HASHSET extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      List<?> rest = (List<?>) eargs.get(0, backtrace);
      final Set<Object> set = new HashSet<Object>(rest.size());
      for (Object val : rest) {
        set.add(val);
      }
      return set;
    }
  }

  @Arguments(spec = {"n", ArgSpec.ARG_PIPE, "sequence"})
  @Docstring(
      text =
          "Locates the nth element of a sequence. n may be any non-negative number. Returns NIL"
              + " when sequence is NIL or n is out of bounds")
  @Package(name = Package.BASE_SEQ)
  public static class NTH extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      final Object seq = Utils.asObject(eargs.get(1, backtrace));
      final int index = Utils.asNumber(eargs.get(0, backtrace)).intValue();
      if (index < 0) {
        throw new RuntimeException(
            getName() + "expected non-negative index index, but got" + index);
      }
      return Seq.getElementByIndex(seq, index);
    }
  }

  @Arguments(spec = {"sequence"})
  @Docstring(text = "Return 2nd and further elements of sqeuence.")
  @Package(name = Package.BASE_SEQ)
  public static class REST extends FuncExp {
    @Override
    @SuppressWarnings("unchecked")
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      Object val = eargs.get(0, backtrace);
      final List<Object> list = (List<Object>) Utils.asObject(val);
      return list.subList(1, list.size());
    }
  }

  @Arguments(spec = {"object"})
  @Docstring(text = "Check if an object is a sequence.")
  @Package(name = Package.BASE_SEQ)
  public static class COLLP extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      final Object val = eargs.get(0, backtrace);
      return Seq.isCollection(val);
    }
  }


  
  @Arguments(spec = {"object"})
  @Docstring(text = "Check if an object is a sequence.")
  @Package(name = Package.BASE_SEQ)
  public static class SEQP extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      final Object val = eargs.get(0, backtrace);
      return Seq.isSeq(val);
    }
  }

  @Arguments(spec = {"object"})
  @Docstring(text = "Check if a value is a NIL.")
  @Package(name = Package.BASE_LOGIC)
  public static class NILP extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      final Object val = eargs.get(0, backtrace);
      return val == null;
    }
  }

  @Arguments(spec = {"object"})
  @Docstring(text = "Check if a value is not a NIL.")
  @Package(name = Package.BASE_LOGIC)
  public static class NOTNILP extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      final Object val = eargs.get(0, backtrace);
      return val != null;
    }
  }

  @Arguments(spec = {"object"})
  @Docstring(text = "Check if an object is a Map.")
  @Package(name = Package.BASE_SEQ)
  public static class MAPP extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      final Object val = eargs.get(0, backtrace);
      return Seq.isMap(val);
    }
  }


  @Arguments(spec = {"object"})
  @Docstring(text = "Check if an object is a Set.")
  @Package(name = Package.BASE_SEQ)
  public static class SETP extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      final Object val = eargs.get(0, backtrace);
      return Seq.isSet(val);
    }
  }

  @Arguments(spec = {"object"})
  @Docstring(text = "Check if an object is an associative collection.")
  @Package(name = Package.BASE_SEQ)
  public static class ASSOCIATIVEP extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      final Object val = eargs.get(0, backtrace);
      return Seq.isAssociative(val);
    }
  }

  @Arguments(spec = {"object"})
  @Docstring(text = "Check if an object is an indexed collection.")
  @Package(name = Package.BASE_SEQ)
  public static class INDEXEDP extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      final Object val = eargs.get(0, backtrace);
      return Seq.isIndexed(val);
    }
  }
  
  
  @Arguments(spec = {"sequence"})
  @Docstring(text = "Reverse a sequence (destructive).")
  @Package(name = Package.BASE_SEQ)
  public static class NREVERSE extends FuncExp {
    @Override
    @SuppressWarnings("unchecked")
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      Object val = eargs.get(0, backtrace);
      if (null == val) {
        return null;
      }
      return reverseList((List<Object>) val);
    }

    private Object reverseList(List<Object> list) {
      Collections.reverse(list);
      return list;
    }
  }

  @Arguments(spec = {"sequence"})
  @Docstring(text = "Reverse a sequence (non-destructive).")
  @Package(name = Package.BASE_SEQ)
  public static class REVERSE extends NREVERSE {
    @Override
    @SuppressWarnings("unchecked")
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      final Object val = eargs.get(0, backtrace);
      if (null == val) {
        return null;
      }
      return super.reverseList((List<Object>) Utils.copySeq((List<Object>) val));
    }
  }

  @Arguments(spec = {"start", "stop", ArgSpec.ARG_OPTIONAL, "step"})
  @Docstring(
      text =
          "Return sequence of numbers."
              + " Returns sequence of numbers  from start to stop (inclusively) with .")
  @Package(name = Package.BASE_SEQ)
  public static class RANGE extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace bt, Eargs eargs) {
      // final Promotion prom = new Promotion();

      // 0 is default
      final Number start = Utils.asNumber(eargs.get(0, bt));
      // prom.promote(start);

      final Number to = Utils.asNumber(eargs.get(1, bt));

      // 1 is default
      final Number step = (null == eargs.get(2, bt)) ? 1 : Utils.asNumber(eargs.get(2, bt));
      // prom.promote(step);

      final Collection<?> resultSeq = new RangeList(start, to, step);
      return resultSeq;
    }
  }

  @Arguments(
      spec = {ArgSpec.ARG_OPTIONAL, "f", ArgSpec.ARG_PIPE, ArgSpec.ARG_MANDATORY, "sequence"})
  @Docstring(text = "Sort a sequence (non destructively).")
  @Package(name = Package.BASE_SEQ)
  public static class SORT extends NSORT {
    protected Object doSort(Object seq, ICode lambda, Backtrace bt, ICtx ctx) {
      return super.doSort(Utils.copySeq(seq), lambda, bt, ctx);
    }
  }

  @Arguments(
      spec = {ArgSpec.ARG_OPTIONAL, "f", ArgSpec.ARG_PIPE, ArgSpec.ARG_MANDATORY, "sequence"})
  @Docstring(text = "Sort a sequence (destructively).")
  @Package(name = Package.BASE_SEQ)
  public static class NSORT extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      final Object seq = (eargs.size() == 1 ? eargs.get(0, backtrace) : eargs.get(1, backtrace));
      final ICode lambda = (ICode) (eargs.size() == 1 ? null : eargs.get(0, backtrace));
      return doSort(seq, lambda, backtrace, eargs);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    protected Object doSort(
        final Object seq, final ICode lambda, final Backtrace backtrace, final ICtx ctx) {
      Comparator<Object> comparator = null;
      if (null != lambda) {
        final ICtx localCtx = ctx.getCompiler().newCtx(ctx);
        final IExpr compf = (IExpr) lambda.getInstance();
        try {
          compf.setParams(Utils.newPosArgsList(2));
        } catch (InvalidParametersException ex) {
          throw new RuntimeException(
              String.format(
                  "%s: lambda at %s: does not take parameter: %s",
                  this.getName(),
                  (null == ex.getParseCtx() ? "?" : ex.getParseCtx().toString()),
                  ex.getMessage()));
        }
        comparator =
            new Comparator<Object>() {
              @Override
              public int compare(Object o1, Object o2) {
                localCtx.getMappings().put("%1", o1);
                localCtx.getMappings().put("%2", o2);
                return (Integer) compf.evaluate(backtrace, localCtx);
              }
            };
      }
      if (null == seq) {
        return seq;
      } else if (seq instanceof List) {
        List<?> col = (List<?>) seq;
        // FIXME: what if values are not actually Comparable, exceptions will happen
        // need to catch exceptions here
        if (null == comparator) {
          Collections.sort((List) col);
        } else {
          Collections.sort(col, comparator);
        }
        return col;
      } else if (seq.getClass().isArray()) {
        Object[] ar = (Object[]) seq;
        if (null == comparator) {
          Arrays.sort(ar);
        } else {
          Arrays.sort(ar, comparator);
        }
        return ar;
      } else {
        throw new RuntimeException("No idea how to sort object of type " + seq.getClass());
      }
    }
  }

  // ***** LANGUAGE
  @Arguments(spec = {"fn", ARG_OPTIONAL, "name"})
  @Docstring(
      text =
          "Create new Java thread. Creates new Java thread and prepare it for execution of given"
              + " function fn.fn must not require parameters for it's execution. The created thread"
              + " is not started.")
  @Package(name = Package.THREADS)
  public static class NEW_THREAD extends FuncExp {
    @Override
    protected Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      final IExpr instance = (IExpr) ((ICode) eargs.get(0, backtrace)).getInstance();
      try {
        instance.setParams(new ArrayList<ICompiled>());
      } catch (InvalidParametersException iex) {
        throw new RuntimeException(iex);
      }
      Runnable runnable = (Runnable) instance;
      final Thread t =
          null == eargs.get(1, backtrace)
              ? new Thread(runnable)
              : new Thread(runnable, Utils.asString(eargs.get(1, backtrace)));
      Threads.contexts.put(t, eargs);
      return t;
    }
  }

  @Arguments(spec = {"symbol"})
  @Docstring(
      text =
          "Returns function bound to given symbol. If no function bound raises an error. The"
              + " returned object may be a built-in function, compiled function or built-in special"
              + " form.")
  @Package(name = Package.BASE_FUNCS)
  public static class SYMBOL_FUNCTION extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      String fname = Utils.asString(eargs.get(0, backtrace));
      Object funcObj = eargs.getCompiler().getFun(fname);
      if (null == funcObj) {
        throw new RuntimeException("Symbol " + fname + " function value is NULL");
      } else if (funcObj instanceof ICode) {
        return funcObj;
      } else {
        throw new RuntimeException(
            "Symbol " + fname + " function value is of unsupported type: " + funcObj);
      }
    }
  }

  @Arguments(spec = {"file-spec"})
  @Docstring(
      text =
          "Execute program from a file/stream. Sequentially executes each form it encounters in the"
              + " input file/or stream named by resource-spec. Returns exception if input could not"
              + " be read or there were exceptions while compiling or executing forms an exception"
              + " will be raised. file-spec may be a java.io.File object, file path as String or"
              + " opened InputStream.")
  @Package(name = Package.IO)
  public static class LOAD extends FuncExp {
    protected InputStream openInput(Object loadObj, Backtrace bt) {
      File file = null;
      if (loadObj instanceof String) {
        file = new File((String) loadObj);
      } else if (loadObj instanceof File) {
        file = (File) loadObj;
      } else if (loadObj instanceof InputStream) {
        return (InputStream) loadObj;
      }
      try {
        return new FileInputStream(file);
      } catch (IOException ex) {
        throw new ExecutionException(bt, "I/O opening stream", ex);
      }
    }

    protected Boolean load(Backtrace bt, ICtx ctx, Object loadObj) {
      if (loadObj == null) {
        return false;
      }
      InputStream is = null;
      try {
        is = openInput(loadObj, bt);
        ASTNList astns = new ParserWrapper(ctx.getCompiler().getParser()).parse(is);
        for (ASTN astn : astns) {
          ICompiled expr = ctx.getCompiler().compile(astn);
          expr.evaluate(bt, ctx);
        }
        return true;
      } finally {
        if (null != is) {
          try {
            is.close();
          } catch (IOException ex) {
            throw new ExecutionException(bt, "I/O exception at stream close", ex);
          }
        }
      }
    }

    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      return load(backtrace, eargs, eargs.get(0, backtrace));
    }
  }

  @Arguments(spec = {"resource-spec"})
  @Docstring(
      text =
          "Execute program from Java resource. Sequentially executes each form it encounters in the"
              + " java resource file named by resource-spec. Returns exception if file could not be"
              + " read or there were exceptions while compiling or executing forms an exception"
              + " will be raised.")
  @Package(name = Package.IO)
  public static class LOADR extends LOAD {
    @Override
    protected InputStream openInput(Object loadObj, Backtrace bt) {
      if (!(loadObj instanceof CharSequence)) {
        throw new ExecutionException(bt, "Invalid resource path " + loadObj);
      }
      final String srcName = Utils.asString(loadObj);
      final InputStream is = this.getClass().getResourceAsStream(srcName);
      if (null == is) {
        throw new ExecutionException(bt, "Failed to load '" + srcName + "' as resource");
      }
      return is;
    }
  }

  @Docstring(
      text =
          "Get list of names of defined functions. If names are given use them as filter"
              + " expressions:  only those which match at least one of filter expressions will be"
              + " returned. Filters may be strings (substring match) or regular expressions"
              + " (java.util.regex.Pattern objects).")
  @Arguments(spec = {ARG_REST, "names"})
  @Package(name = Package.BASE_DOCS)
  public static class FUNCTIONS_NAMES extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      List<String> results = new ArrayList<String>();
      List<?> rest = (List<?>) eargs.get(0, backtrace);
      for (String key : eargs.getCompiler().getFunKeys()) {
        final Object fun = eargs.getCompiler().getFun(key);
        if (null == fun) {
          // stub
          continue;
        }
        if (rest.size() > 0) {
          for (Object obj : rest) {
            if (obj instanceof Pattern) {
              final Matcher m = ((Pattern) obj).matcher(key);
              if (m.matches()) {
                results.add(key);
              }
            } else {
              String str = Utils.asString(obj);
              if (null == str) {
                continue;
              }
              if (key.contains(str)) {
                results.add(key);
              }
            }
          }
        } else {
          results.add(key);
        }
      }
      Collections.sort(results);
      return results;
    }
  }

  @Arguments(spec = {"object"})
  @Docstring(
      text =
          "Check if object is a function. Returns true if object is a function (built-in or user"
              + " defined); otherwise, returns false. A function is an object that represents code"
              + " to be executed when an appropriate number of arguments is supplied. A function"
              + " can be directly invoked by using it as the first argument to funcall, apply.")
  @Package(name = Package.BASE_FUNCS)
  public static class FUNCTIONP extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      final Object obj = eargs.get(0, backtrace);
      if (null != obj && ICode.class.isAssignableFrom(obj.getClass())) {
        final String codeType = ((ICode) obj).getCodeType();
        return "function".equals(codeType) || "compiled function".equals(codeType);
      }
      return false;
    }
  }

  @Docstring(
      text =
          "Evaluate a Parsed Expression. Evaluates parsed form in the current dynamic context and"
              + " return result of evaluation'")
  @Arguments(spec = {"form"})
  @Package(name = Package.BASE_LANG)
  public static class EVAL extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      final Object obj = eargs.get(0, backtrace);
      final ASTN astn =
          (obj instanceof ASTN) ? (ASTN) obj : Utils.astnize(obj, new ParseCtx("<EVAL>"));
      final ICompiled expr = eargs.getCompiler().compile(astn);
      final Object result = expr.evaluate(backtrace, eargs);
      return result;
    }
  }

  @Arguments(spec = {"string"})
  @Docstring(
      text =
          "Parse expression from string. Reads expression from string using default parser. Returns"
              + " expression or NIL if no expression has been read")
  @Package(name = Package.IO)
  public static class READ_FROM_STRING extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace bt, Eargs eargs) {
      String str = (String) eargs.get(0, bt);
      ParseCtx pctx = new ParseCtx("<READ_FROM_STRING>");
      ASTNList astns = eargs.getCompiler().getParser().parse(pctx, str);
      if (null == astns || astns.size() == 0) {
        return null;
      }
      return Utils.unAstnize(astns.get(0));
    }
  }

  @Docstring(text = "Makes new Symbol for a string")
  @Arguments(spec = {"symbol-name"})
  @Package(name = Package.BASE_BINDINGS)
  public static class SYMBOL extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      return new Symbol(Utils.asString(eargs.get(0, backtrace)));
    }
  }

  // TODO: support level and uplevel
  @Docstring(text = "Unbind variable given by symbol. Always returns symbol.")
  @Arguments(spec = {"symbol"})
  @Package(name = Package.BASE_BINDINGS)
  public static class MAKUNBOUND extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      final Symbol sym = (Symbol) eargs.get(0, backtrace);
      final String name = sym.getName();
      eargs.remove(name);
      // ICtx ctx = eargs;
      // while (null != ctx) {
      //  final Map mappings = ctx.getMappings();
      //  if (mappings.containsKey(name)) {
      //      mappings.remove(name);
      //  }
      //  ctx = ctx.getPrev();
      // }

      return sym;
    }
  }

  @Docstring(
      text =
          "Change variable value in specified context. Set changes the contents of variable symbol"
              + " in the dynamic context to the given value. If uplevel is set the value will be"
              + " set in the uplevel-ths previous context. If level is set the value will be"
              + " changed in the level-th context from the level0 ")
  @Arguments(
      spec = {"symbol", "value", ArgSpec.ARG_KEY, "uplevel", "level"},
      text = "symbol value { level | uplevel }?")
  @Package(name = Package.BASE_BINDINGS)
  public static class SET extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      final Symbol sym = (Symbol) eargs.get(0, backtrace);
      final Object obj = eargs.get(1, backtrace);
      final Object uplevelObj = eargs.get(2, backtrace);
      final Object levelObj = eargs.get(3, backtrace);
      if (null != levelObj) {
        // absolute level
        if (null != uplevelObj) {
          throw new ExecutionException(
              backtrace, getName() + " only one of two must be set: level | uplevel");
        }
        final int level = Utils.asNumber(levelObj).intValue();
        final List<ICtx> parents = eargs.getParentContexts();
        final int lastIdx = parents.size() - 1;
        ICtx ctx = parents.get(lastIdx - level);
        ctx.getMappings().put(sym.getName(), obj);
      } else if (null != uplevelObj) {
        final int uplevel = Utils.asNumber(uplevelObj).intValue();
        ICtx ctx = eargs.getPrev();
        for (int i = 0; i < uplevel && null != ctx; i++) {
          ctx = ctx.getPrev();
        }
        if (null != ctx) {
          ctx.getMappings().put(sym.getName(), obj);
        } else {
          throw new ExecutionException(
              backtrace, getName() + " uplevel value exceeds context depth");
        }
      } else {
        eargs.getPrev().greplace(sym.getName(), obj);
      }
      return obj;
    }
  }

  @Docstring(text = "Set symbol's function value to value and return previous value or NIL.")
  @Arguments(spec = {"symbol", "value"})
  @Package(name = Package.BASE_BINDINGS)
  public static class FSET extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      final Object symObj = eargs.get(0, backtrace);
      if (!(symObj instanceof Symbol)) {
        throw new ExecutionException(
            backtrace, getName() + " first argument must be symbol, but got " + symObj);
      }
      final String funcName = ((Symbol) symObj).getName();
      final Object obj = eargs.get(1, backtrace);
      ICode code = eargs.getCompiler().getFunOrStub(funcName);
      if (code instanceof Compiler.CodeProxy) {
        final Compiler.CodeProxy proxy = (Compiler.CodeProxy) code;
        Object prev = proxy.code;
        proxy.setCode((ICode)obj);
        return prev;
      } else {
        // FIXME
        throw new ExecutionException(backtrace, "Cannot override builtin function " + funcName);
      }
      //return eargs.getCompiler().putFun(((Symbol) symObj).getName(), (ICode) obj);
    }
  }

  @Docstring(lines =
             {"Put element value into an associative structure.",
              "Set value of element at index/key in the target structure to object. ",
              "If target ibject is a Java array and the object type does not match the type",
              "of this array this function will attempt to perform necessary coercion",
              "operations. The coercions  work in the same way as INT, FLOAT, STRING",
              "and rest of the built-in coercion functions.",
              "",
              "If target object is a list or and array and happens out of bound exception",
              "the function returns normally without any change to the target structure",
              "",
              "The function returns previous value of the element or NIL if it did not exist",
              "or no change has been made."})
  @Arguments(spec = {"target", "key", "object"})
  @Package(name = Package.BASE_SEQ)
  public static class NPUT extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      final Object targetObj = Utils.asObject(eargs.get(0, backtrace));
      final Object index = Utils.asObject(eargs.get(1, backtrace));
      final Object obj = Utils.asObject(eargs.get(2, backtrace));
      return Seq.putElement(targetObj, index, obj);
    }
  }

  @Docstring(lines =
             {"Put element value into an associative structure (non-mutating).",
              "Set value of element at index/key in the target structure to object. ",
              "If target ibject is a Java array and the object type does not match the type",
              "of this array this function will attempt to perform necessary coercion",
              "operations. The coercions  work in the same way as INT, FLOAT, STRING",
              "and rest of the built-in coercion functions.",
              "",
              "If target object is a list or and array and happens out of bound exception",
              "the function returns normally without any change to the target structure",
              "",
              "The function returns copy of the target object with the requested change."})
  @Arguments(spec = {"target", "key", "object"})
  @Package(name = Package.BASE_SEQ)
  public static class PUT extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      final Object srcObj = Utils.asObject(eargs.get(0, backtrace));
      final Object index = Utils.asObject(eargs.get(1, backtrace));
      final Object obj = Utils.asObject(eargs.get(2, backtrace));
      return Seq.roPutElement(srcObj, index, obj);
    }
  }

  @Docstring(text = "Remove an element from the end of a sequence modifying the sequence. "
             + "Returns the removed element.")
  @Arguments(spec = {"seq"})
  @Package(name = Package.BASE_SEQ)
  public static class NPOP extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      final Object list = Utils.asObject(eargs.get(0, backtrace));
      // FIXME: must be atomic
      if (null == list || Seq.getLength(list, false) < 1) {
        throw new ExecutionException(backtrace, this.getName() + " from an empty sequence");
      }
      return Seq.removeLastElement(list);
    }
  }


  @Docstring(text = "Remove an element from the end of a sequence. "
             + "Returns list with the removed element and a copy of the sequence with this element removed.")
  @Arguments(spec = {"seq"})
  @Package(name = Package.BASE_SEQ)
  public static class POP extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      final Object list = Utils.asObject(eargs.get(0, backtrace));
      // FIXME: must be atomic
      if (null == list || Seq.getLength(list, false) < 1) {
        throw new ExecutionException(backtrace, this.getName() + " from an empty sequence");
      }
      return Seq.roRemoveLastElement(list);
    }
  }

  
  @Docstring(text = "Append element to the end of a seqence modifying the sequence. "
             + "Returns the sequence.")
  @Arguments(spec = {"seq", "object"})
  @Package(name = Package.BASE_SEQ)
  public static class NPUSH extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      final Object seq = Utils.asObject(eargs.get(0, backtrace));
      final Object obj = Utils.asObject(eargs.get(1, backtrace));
      // FIXME: atomic push must be supported
      if (seq instanceof Set) {
        Seq.addValue(seq, obj);
      } else {
        Seq.putElement(seq, Seq.getLength(seq, false), obj);
      }
      return seq;
    }
  }

  @Docstring(text = "Append element to the end of a seqence. "
             + "Returns copy of the target object with the requested change.")
  @Arguments(spec = {"seq", "object"})
  @Package(name = Package.BASE_SEQ)
  public static class PUSH extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      final Object seq = Utils.asObject(eargs.get(0, backtrace));
      final Object obj = Utils.asObject(eargs.get(1, backtrace));
      if (seq instanceof Set) {
        return Seq.roAddValue(seq, obj);
      } else {
        return Seq.roPutElement(seq, Seq.getLength(seq, false), obj);
      }
    }
  }

  
  @Docstring(text = "Insert element into given position in the indexed sequence."
             + " Returns the sequence")
  @Arguments(spec = {"seq", "index","object"})
  @Package(name = Package.BASE_SEQ)
  public static class NINSERT extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      final Object listObject = Utils.asObject(eargs.get(0, backtrace));
      final int index = Utils.asNumber(eargs.get(1, backtrace)).intValue();
      final Object object = Utils.asObject(eargs.get(2, backtrace));
      return Seq.insertElementByIndex(listObject, index, object);
    }
  }


  @Docstring(text = "Insert element into given position in the indexed sequence."
             + " Returns a copy of the target sequence with the requested modification.")
  @Arguments(spec = {"seq", "index","object"})
  @Package(name = Package.BASE_SEQ)
  public static class INSERT extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      final Object listObject = Utils.asObject(eargs.get(0, backtrace));
      final int index = Utils.asNumber(eargs.get(1, backtrace)).intValue();
      final Object object = Utils.asObject(eargs.get(2, backtrace));
      return Seq.roInsertElementByIndex(listObject, index, object);
    }
  }

  @Docstring(text = "Delete an element from a sequence by key or index."
             + " Returns the removed element.")
  @Arguments(spec = {"seq", "key"})
  @Package(name = Package.BASE_SEQ)
  public static class NDELETE extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      final Object listObject = Utils.asObject(eargs.get(0, backtrace));
      final Object keyidx = eargs.get(1, backtrace);
      return Seq.removeElementByKeyOrIndex(listObject, keyidx);
    }
  }

  @Docstring(text = "Delete an element from a sequence by key or index."
             + " Returns a copy of the target object with the requested element removed.")
  @Arguments(spec = {"seq", "key"})
  @Package(name = Package.BASE_SEQ)
  public static class DELETE extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      final Object listObject = Utils.asObject(eargs.get(0, backtrace));
      final Object keyidx = eargs.get(1, backtrace);
      return Seq.roRemoveElementByKeyOrIndex(listObject, keyidx);
    }
  }

  
  
  @Docstring(lines = {"Set value of element at index to object.",
                      "",
                      "Set indexed sequence (array, list, character sequence) element value.",
                      "",
                      "If target ibject is a Java array and object type does not match type of this",
                      "array this function will attempt to perform necessary coercion operations. ",
                      "The coercions  work in the same way as INT, FLOAT, STRING and rest of the ",
                      "built-in coercion functions. May fail with index out of bound exception.",
                      "",
                      "The function returns previous value of the element."})
  @Arguments(spec = {"obj", "key", "object"})
  @Package(name = Package.BASE_SEQ)
  public static class NASET extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      final Object arrayObj = Utils.asObject(eargs.get(0, backtrace));
      final int index = Utils.asNumber(eargs.get(1, backtrace)).intValue();
      final Object obj = Utils.asObject(eargs.get(2, backtrace));
      try {
        return Seq.setElementByIndex(arrayObj, index, obj);
      } catch (IndexOutOfBoundsException ex) {
        throw new ExecutionException(ex);
      }
    }
  }

  @Docstring(lines = {"Set value of element at index to object. ",
                      "",
                      "Set indexed sequence (array, list, character sequence) element value.",
                      "May fail with index out of bound exception.",
                      "",
                      "If target ibject is a Java array and object type does not match type of this",
                      "array this function will attempt to perform necessary coercion operations. ",
                      "The coercions  work in the same way as INT, FLOAT, STRING and rest of the ",
                      "built-in coercion functions. ",
                      "",
                      "The function returns new sequence with the requested change, the original",
                      "object is not modified"})  
  @Arguments(spec = {"obj", "key", "object"})
  @Package(name = Package.BASE_SEQ)
  public static class ASET extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      final Object arrayObj = Utils.asObject(eargs.get(0, backtrace));
      final int index = Utils.asNumber(eargs.get(1, backtrace)).intValue();
      final Object obj = Utils.asObject(eargs.get(2, backtrace));
      try {
        return Seq.roSetElementByIndex(arrayObj, Utils.asNumber(index).intValue(), obj);
      } catch (IndexOutOfBoundsException ex) {
        throw new ExecutionException(ex);
      }
    }
  }
  
  
  @Arguments(spec = {"array", "index"})
  @Docstring(
      text =
          "Get Array element value. Return array element at specified index. Throws"
              + " ArrayOutOfBoundsException if index is invalid")
  @Package(name = Package.BASE_SEQ)
  public static class AREF extends FuncExp implements LValue {
    @Override
    public Object doSet(Backtrace backtrace, ICtx ctx, Object value) {
      Eargs eargs = this.evaluateParameters(backtrace, ctx);
      final Object arrayObj = Utils.asObject(eargs.get(0, backtrace));
      final int index = Utils.asNumber(eargs.get(1, backtrace)).intValue();
      try {
        return Seq.setElementByIndex(arrayObj, index, value);
      } catch (IndexOutOfBoundsException ex) {
        throw new ExecutionException(ex);
      }
    }
    
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      final Object arrayObj = Utils.asObject(eargs.get(0, backtrace));
      final int index = Utils.asNumber(eargs.get(1, backtrace)).intValue();
      return Seq.refElementByIndex(arrayObj, index);
    }
  }


  @Arguments(spec = {ArgSpec.ARG_PIPE, "seq", ArgSpec.ARG_REST, "sep"})
  @Docstring(text = "Return sequence elements of seq separated by elements in sep. "
             + "Currently only lists are supported.")
  @Package(name = Package.BASE_SEQ)
  public static class INTERPOSE extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      final Object seqObj = Utils.asObject(eargs.get(0, backtrace));
      final List<?>  seps = (List<?>)eargs.get(1, backtrace);
      //FIXME: support more seq. types, list kinds
      if (seqObj instanceof List) {
        final List<?> seqLst = (List<?>)seqObj;
        final List<Object> result = new ArrayList<Object>(seqLst.size() * (1 + seps.size()));
        for (int i = 0; i < seqLst.size(); i++) {
          if (i > 0) {
            for (int j = 0; j < seps.size(); j++) {
              result.add(seps.get(j));
            }
          }
          result.add(seqLst.get(i));
        }
        return result;
      } else if (null == seqObj) {
        return null;
      }  else {
        throw new ExecutionException(backtrace, getName()
                                     + " not implemented for sequence of type "
                                     + seqObj.getClass());
      }
    }
  }


  @Arguments(spec = {ArgSpec.ARG_OPTIONAL, "sep", ArgSpec.ARG_MANDATORY, ArgSpec.ARG_PIPE, "seqs"})
  @Docstring(text = "Return sequence elements of seq joined by elements in sep. "
             + " Currently only strings are supported.")
  @Package(name = Package.BASE_SEQ)
  public static class JOIN extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      final String  sep = Utils.asStringOrEmpty(eargs.get(0, backtrace));
      final Object seqObj = Utils.asObject(eargs.get(1, backtrace));
      try {
        return Seq.joinWithString(seqObj, sep);
      } catch (RuntimeException ex) {
        return new ExecutionException(backtrace, ex.getMessage());
      }
    }
  }


  @Arguments(spec = {"seq", "target", ArgSpec.ARG_OPTIONAL, "replacement"})
  @Docstring(text = "Replace eache subsequence in seq that equals to "
             + "target with replacement sequence. "
             + "Return resulting sequence. "
             + " The original sequence is not modified. "
             + "If replacement is not provided or is NIL "
             + " the target sequences will be deleted.")
  @Package(name = Package.BASE_SEQ)
  public static class REPLACE extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      final Object seqObj = Utils.asObject(eargs.get(0, backtrace));
      if (seqObj instanceof String) {
        final CharSequence targetCs = Utils.asCharSequenceOrNull(eargs.get(1, backtrace));
        CharSequence replacementCs = (eargs.size() > 2)
            ? Utils.asCharSequenceOrNull(eargs.get(2, backtrace)) : null;
        if (null == replacementCs) {
          replacementCs = "";
        }
        if (null != targetCs) {
          return ((String) seqObj).replace(targetCs, replacementCs);
        } else {
          return seqObj;
        }
      } else if (null == seqObj) {
        return null;
      }  else {
        throw new ExecutionException(backtrace, getName()
                                     + " not implemented for objects of type "
                                     + seqObj.getClass());
      }
    }
  }

  @Arguments(spec = {ArgSpec.ARG_KEY, "size", "elementType", ArgSpec.ARG_REST, "elements"})
  @Docstring(
      text =
          "Create an Array. Creates array of objects of specified size. Optional :elementType"
              + " argument specifies type of array elements. The default is java.lang.Object")
  @Package(name = Package.BASE_SEQ)
  public static class MAKE_ARRAY extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      int size = 0;
      final Object sizeObj = eargs.get("size", backtrace);
      final Object eltsObj = eargs.get("elements", backtrace);
      if (null != sizeObj) {
        size =  Utils.asNumber(sizeObj).intValue();
      } else if (null != eltsObj) {
        size = Seq.getLength(eltsObj, false);
      }
      final Object et = eargs.get("elementType", backtrace);
      Object result = null;
      if (null != et) {
        Class<?> tclass = null;;
        try {
          tclass = Utils.tspecToClass(et);
        } catch (ClassNotFoundException ex) {
          throw new ExecutionException(backtrace, ex);
        }
        result = Array.newInstance(tclass, size);
      } else {
        result = new Object[size];
      }
      if (null != eltsObj) {
        int eltSize = Seq.getLength(eltsObj, false);
        List<?> elements = (List<?>) eltsObj;
        for (int i = 0; i < size && i < eltSize; i++) {
          Array.set(result, i, elements.get(i));
        }
      }
      return result;
    }
  }

  protected static Object findFuncObject(Object fobj, Eargs eargs) {
    if ((fobj instanceof Symbol) || (fobj instanceof CharSequence)) {
      String funcName = Utils.asString(fobj);
      if (null != funcName) {
        return eargs.getCompiler().getFun(funcName);
      }
    } else {
      return fobj;
    }
    return null;
  }

  @Arguments(spec = {"spec"})
  @Docstring(text = "Create Version from text specification. ")
  @Package(name = Package.BASE_VERSION)
  public static class VERSION extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      final String spec = Utils.asString(eargs.get(0, backtrace));
      try {
        return Version.parseVersion(spec);
      } catch (IllegalArgumentException ex) {
        throw new ExecutionException(backtrace, "Failed to parse version spec: " + ex);
      }
    }
  }

  public static class FDesc extends BeanMap {
    protected String name;
    protected IParser parser;
    protected Compiler compiler;

    /**
     * Construct FSesc from ICompiled object.
     */
    public FDesc(Object obj, String name, IParser parser, Compiler compiler) {
      super(obj);
      this.name = name;
      this.parser = parser;
      this.compiler = compiler;
    }

    @Override
    protected Map<String, Method> getGettersMap() {
      Map<String, Method> methods = super.getGettersMap();
      methods.put("name", null);
      methods.put("operatorDescs", null);
      return methods;
    }

    @Override
    public Object get(Object key) {
      final String keyStr = Utils.asString(key);
      if ("name".equalsIgnoreCase(keyStr)) {
        return this.name;
      } else if ("argDescr".equalsIgnoreCase(keyStr)) {
        return formatArgs();
      } else if ("operatorDescr".equalsIgnoreCase(keyStr)) {
        return getOperatorDescr();
      }
      return super.get(key);
    }


    /**
     * Get formatted description of operators that are implemented
     * using this function.
     */
    public String getOperatorDescr() {
      final List<OperatorDesc> operators = getOperatorDescs();
      if (null != operators) {
        final StringBuilder buf = new StringBuilder();
        for (OperatorDesc op : operators) {
          buf.append("Operator: ").append(op.name).append("\n");
          for (int idx = 0; idx <  op.getUsages().length; idx++) {
            String usage = op.getUsages()[idx];
            for (String line : usage.split("\n")) {
              buf.append("   ").append(line).append("\n");
            }
          }
        }
        return buf.toString();
      }
      return null;
    }

    /**
     * Get list of descriptios of operators that are implemented
     * using this function.
     */
    public List<OperatorDesc> getOperatorDescs() {
      final List<OperatorDesc> descs = new ArrayList<OperatorDesc>();
      for (OperatorDesc desc : this.parser.getOperatorDescs()) {
        if (null != desc.getFunction() && desc.getFunction().equalsIgnoreCase(this.name)) {
          descs.add(desc);
        }
      }
      return descs;
    }
    
    
    protected String formatArgs() {
      ICode codeObj = (ICode) this.obj;
      if (null != parser && null != codeObj.getArgSpec()) {
        return parser.formatArgSpec(codeObj.getArgSpec());
      } else {
        return codeObj.getArgDescr();
      }
    }
    
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(127);
      if (null != this.obj && (this.obj instanceof ICode)) {
        ICode codeObj = (ICode) this.obj;
        ;
        buf.append(this.name);
        buf.append(" is a ");
        if (codeObj.isBuiltIn()) {
          buf.append("built-in ");
        }
        buf.append(codeObj.getCodeType());
        buf.append(" defined at ");
        buf.append(codeObj.getDefLocation());
        buf.append("\n\n");
        buf.append("Arguments: ").append(formatArgs()).append("\n");

        String operatorDescr = this.getOperatorDescr();
        if (null != operatorDescr) {
          buf.append(operatorDescr);
        }
        buf.append("Documentation: \n    ").append(codeObj.getDocstring()).append("\n\n");
        buf.append("Package: ").append(codeObj.getPackageName()).append("\n");
      } else if (null != this.obj) {
        buf.append(" Object of type ").append(this.obj.getClass()).append("\n");
      } else {
        buf.append(" is not defined\n");
      }
      return buf.toString();
    }
  }

  @Arguments(spec = {"function"})
  @Docstring(
      text =
          "Describe function. "
              + "Return textual description of given function or "
              + "built-in form. function is a symbol or function name or a lambda")
  @Package(name = Package.BASE_DOCS)
  public static class DESCRIBE_FUNCTION extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      final Object target = eargs.get(0, backtrace);
      final Object funcObj = findFuncObject(target, eargs);
      FDesc fdesc = new FDesc((ICode) funcObj,
                              Utils.asString(target),
                              eargs.getCompiler().getParser(),
                              eargs.getCompiler());
      return fdesc;
    }
  }

  @Arguments(spec = {"function-name"})
  @Docstring(
      text =
          "Get Function Docstring. "
              + "Return documentation string of given function or "
              + "built-in form. function is a symbol or function name or a lambda")
  @Package(name = Package.BASE_DOCS)
  public static class DOCUMENTATION extends FuncExp {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      Object funcObj = findFuncObject(eargs.get(0, backtrace), eargs);
      if (null != funcObj) {
        if (funcObj instanceof ICode) {
          ICode codeObj = (ICode) funcObj;
          return codeObj.getDocstring();
        } else {
          return "unknown object of type " + funcObj.getClass();
        }
      } else {
        return null;
      }
    }
  }


  public interface LValue {
    Object doSet(Backtrace backtrace, ICtx ctx, Object value);
  }
  
  public static class VarExp extends AbstractExpr implements LValue {
    private final String varname;

    public String getName() {
      return varname;
    }

    public VarExp(String str) {
      this.varname = str;
    }

    @Override
    public Object doSet(Backtrace backtrace, ICtx ctx, Object value) {
      ctx.replace(varname, value);
      return value;
    }

    @Override
    protected Object doEvaluate(Backtrace backtrace, ICtx ctx) {
      final Object obj = ctx.get(varname, backtrace);
      if (null == obj && !ctx.contains(varname)) {
        ctx.onMissingVar(varname);
      }
      return obj;
    }

    @Override
    public void setParams(List<ICompiled> params) throws InvalidParametersException {
      throw new InvalidParametersException(
          debugInfo, "setParams called for variable " + varname + ": this cannot happen");
    }
  }

  // ****** SELF-EVALUATING OBJECTS
  public static class ValueExpr extends AbstractExpr {
    private Object value;

    public ValueExpr(Object value) {
      this.value = value;
    }

    @Override
    public int hashCode() {
      return this.getClass().hashCode() + (null == this.value ? 0 : value.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof ValueExpr) {
        final ValueExpr v = (ValueExpr) obj;
        return v.getClass().equals(v.getClass())
            && ((null == this.value && null == v.value)
                || (null != this.value && this.value.equals(v.value)));
      }
      return false;
    }

    @Override
    public void setParams(List<ICompiled> params) throws InvalidParametersException {
      throw new InvalidParametersException(
          debugInfo, "setParams called for " + this.getName() + ", this cannot happen!");
    }

    @Override
    public String toString() {
      return null == value ? "NIL" : value.toString();
    }

    @Override
    public Object doEvaluate(Backtrace backtrace, ICtx ctx) {
      return value;
    }

    // FIXME: do we really need this??
    public Object getValue() {
      return value;
    }
  }

  public static class StringExp extends ValueExpr {
    public StringExp(String str) {
      super(str);
    }
  }

  public static class EmptyListExp extends ValueExpr {
    public EmptyListExp() {
      super(new ArrayList<Object>());
    }
  }

  public static class NumberExp extends ValueExpr {
    public NumberExp(Number value) {
      super(value);
    }

    @Override
    public boolean equals(Object obj) {
      return super.equals(obj);
    }
  }

  public static class BooleanExp extends ValueExpr {
    public BooleanExp(Boolean value) {
      super(value);
    }
  }

  public static class ObjectExp extends ValueExpr {
    public ObjectExp(Object value) {
      super(value);
    }
  }
}
