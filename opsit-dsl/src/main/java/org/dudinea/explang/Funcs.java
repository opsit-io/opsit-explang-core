package org.dudinea.explang;

import static org.dudinea.explang.ArgSpec.ARG_OPTIONAL;
import static org.dudinea.explang.ArgSpec.ARG_REST;

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

import org.dudinea.explang.Compiler.Eargs;
import org.dudinea.explang.Compiler.ICtx;
import org.dudinea.explang.Seq.Operation;

public class Funcs {
    public static abstract class AbstractExpr implements IExpr, Runnable {
        protected ParseCtx debugInfo;
        protected String name = null;
    

        public void run () {
            final Compiler.ICtx ctx =
                Threads.contexts.remove(Thread.currentThread());
            final Object result =
                this.evaluate(new Backtrace(), ctx);
            Threads.results.put(Thread.currentThread(),result);
        }
        
        abstract protected Object doEvaluate(Backtrace backtrace,ICtx  ctx);

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
        final public Object evaluate(final Backtrace backtrace,
                                     final ICtx  ctx) {
            try {
                backtrace.push(getTraceName(),
                               this.debugInfo,
                               ctx);
                return doEvaluate(backtrace, ctx);
            } catch (ExecutionException ex) {
                throw ex;
            } catch (Throwable t) {
                throw new ExecutionException(backtrace,t);
            } finally {
                backtrace.pop();
            }
            
        }
    }

    public static abstract class FuncExp extends AbstractExpr {
        protected  ArgList argList;
        @Override
        protected String getTraceName() {
            //final StringBuilder b = new StringBuilder(16);
            //b.append("(").append(getName()).append(")");
            return getName();
        }
        
        @Override
        public Object doEvaluate(Backtrace backtrace, ICtx ctx) {
            return evalWithArgs(backtrace, this.evaluateParameters(backtrace, ctx));
        }
    
        protected abstract Object evalWithArgs(Backtrace backtrace, Eargs eargs);
    
        //@Override
        public Eargs evaluateParameters(Backtrace backtrace, ICtx ctx) {
            return argList.evaluateArguments(backtrace, ctx);
        }


        public void checkParamsList(List <ICompiled> params)
            throws InvalidParametersException{
            if (null!=argList) {
                throw new InvalidParametersException(this.getDebugInfo(),
                                                     "internal exception: parameters already set");
            }
        }
            

        
        @Override
        public void setParams(List<ICompiled>  params)
            throws InvalidParametersException {

            Arguments args = this.getClass().getAnnotation(Arguments.class);
            if (null == args) {
                throw new RuntimeException("argument list not specified for function "
                                           +this.getClass());
            }
            String specArray[]= args.spec();
            // FIXME: no compiler - no initForm!
            ArgSpec spec = new ArgSpec(specArray, null);
            this.checkParamsList(params);
            this.argList = new ArgList(spec, params);
        }
    }

    /**** ARITHMETIC FUNCTIONS ****/
    public static interface ABSTRACT_OP  {
        Number doIntOp(Number arg1, Number arg2);
        Number doDoubleOp(Number arg1, Number arg2);
        Number doFloatOp(Number arg1, Number arg2);
    }    


    @Arguments(spec={ArgSpec.ARG_REST,"args"})
    public static abstract class ABSTRACT_ADD extends FuncExp implements ABSTRACT_OP {
        protected abstract Number getNeutral();
        @Override
        public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
            Number result = getNeutral();
            Promotion p = new Promotion();
            List rest = (List) eargs.get(0, backtrace);
            if (rest.size() == 0) {
                p.promote(result);
            } else {
                for (Object earg : rest) {
                    Number arg = Utils.asNumber(earg);
                    p.promote(arg);
                    result = p.callOP(this, result, arg);
                }
            }
            return p.returnResult(result);
        }
    }

    @Docstring(text="Compute Sum. "+
               "Returns the sum of numeric values of it's arguments, " +
               "performing any necessary type conversions in the process. " +
               "If no numbers are supplied, 0 is returned.")
    public static class ADDOP extends ABSTRACT_ADD {
        @Override
        protected Number getNeutral() {
            return new Integer(0);
        }
        @Override
        public  Number doIntOp(Number result, Number arg) {
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
    }

    @Docstring(text="Compute Product. "+
               "Returns the product of it's arguments , "+
               "performing any necessary type conversions in the process. "+
               "If no numbers are supplied, 1 is returned.")
    public static class MULOP extends ABSTRACT_ADD {
        @Override
        protected Number getNeutral() {
            return  new Integer(1);
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
    }
    

    @Arguments(spec={ArgSpec.ARG_REST,"args"})
    public abstract static class ABSTRACT_SUB extends FuncExp implements  ABSTRACT_OP {
        protected abstract Number getNeutral();
        @Override
        public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
            List rest = (List) eargs.get(0, backtrace);
            Number num = Utils.asNumber(rest.get(0));
            Promotion p = new Promotion();
            p.promote(num);
            if (rest.size() == 1) {
                num = p.callOP(this, getNeutral(), num);
            } else {
                for (int i = 1; i < rest.size(); i++) {
                    Number arg = Utils.asNumber(rest.get(i));
                    p.promote(arg);
                    num = p.callOP(this, num, arg);
                }
            }
            return p.returnResult(num);
        }
    }
    
    @Docstring(text="Performs subtraction or negation. "+
               "If only one number is supplied, the negation of that "+
               "number is returned. If more than one argument is given, "+
               "it subtracts rest of the arguments from the first one "+
               "and returns the result. The function performs necessary "+
               "type conversions.")
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
        protected Number getNeutral() {
            return new Integer(0);
        }
    }

    @Docstring(text="Performs Division or Reciprocation. "+
               "If no denominators are supplied, the function / returns "+
               "the reciprocal of number. " +
               "If at least one denominator is supplied, the function / "+
               "divides the numerator by all of the denominators and returns"+
               " the resulting quotient. If each argument is either an integer"+
               " or a ratio, and the result is not an integer, then it is a ratio."+
               " The function / performs necessary type conversions. ")
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
        public Float doFloatOp(Number arg1, Number arg2) {
            return arg1.floatValue() / arg2.floatValue();
        }
        
        @Override
        protected Number getNeutral() {
            return new Integer(1);
        }

    }


    @Arguments(spec={"x","y"})
    @Docstring(text="Compute Remainder. "+
               "Generalizations of the remainder function. When both operands are integer "+
               "returns result of the remainder operation . If one of them is floating point "+
               "returns result of \n\t number - truncate_to_zero (number / divisor) * divisor "+
               "(same semantic as for the Java % operator.")
    public  static class REMOP extends FuncExp implements  ABSTRACT_OP {
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
        public  Number doIntOp(Number arg1, Number arg2) {
            return arg1.longValue() % arg2.longValue();
        }

        @Override
        public Double doDoubleOp(Number arg1, Number arg2) {
            return arg1.doubleValue() % arg2.doubleValue();
        }

        @Override
        public  Float doFloatOp(Number arg1, Number arg2) {
            return arg1.floatValue() % arg2.floatValue();
        }
    }

    @Arguments(spec={"number","divisor"})
    @Docstring(text="Compute Modulus. "+
               "Generalizations of the modulus function. When both operands are integer "+
               "returns result of the modulus operation. If one of them is floating point "+
               "returns result of \n\t number - ⌊ (number / divisor) ⌋ * divisor ")
    public  static class MODOP extends FuncExp implements  ABSTRACT_OP {
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
            return arg1.longValue() - Math.floor(arg1.doubleValue() / arg2.doubleValue()) * arg2.longValue();
        }
        
        @Override
        public  Double doDoubleOp(Number arg1, Number arg2) {
            return arg1.doubleValue() - Math.floor(arg1.doubleValue() / arg2.doubleValue()) * arg2.doubleValue();
        }

        @Override
        public Float doFloatOp(Number arg1, Number arg2) {
            return arg1.floatValue() - ((float)Math.floor(arg1.doubleValue() / arg2.doubleValue())) * arg2.floatValue();
        }
    }

    
    
    /**** BOOLEAN FUNCTIONS ****/
    @Arguments(spec={ArgSpec.ARG_LAZY, ArgSpec.ARG_REST, "forms"})
    @Docstring(text="Logical AND. "+
               "Function AND lazily evaluates each argument form, "+
               "one at a time from left to right. " +
               "As soon as any form evaluates to NIL, "+
               "AND returns NIL without evaluating the remaining forms. " +
               "If all forms but the last evaluate to true values, "+
               "AND returns the results " +
               "produced by evaluating the last form. " +
               "If no forms are supplied, (AND) returns true.")
    public static class AND extends FuncExp {
        @Override
        public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
            Object andVal = true;
            for (Object  val : (List)eargs.get(0, backtrace)) {
                andVal = val;
                if (!Utils.asBoolean(val)) {
                    break;
                }
            }
            return andVal;
        }
    }

    @Arguments(spec={ArgSpec.ARG_LAZY, ArgSpec.ARG_REST, "args"})
    @Docstring(text ="Logical OR. "+
               "Function OR lazily evaluates each form, "+
               "one at a time, from left to right. "+
               "The evaluation of all forms terminates when a form evaluates to true "+
               "(i.e., something other than nil) "+
               "and OR immediately returns that value "+
               "without evaluating the remaining forms.")
    public static class OR extends FuncExp {
        @Override
        public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
            Object orVal = false;
            for (Object val : (List)eargs.get(0, backtrace)) {
                if (Utils.asBoolean(val)) {
                    orVal = val;
                    break;
                }
            }
            return orVal;
        }
    }

    @Arguments(spec={"x"})
    @Docstring(text="Logical Negation. "+
               "Returns True if x has false logical value; otherwise, returns False."+
               "Parameter x can be any object. Only NIL, the empty list (), "+
               "the empty String \"\", 0  and FALSE have false logical value. "+
               "All other objects have true logical value")
    public static class NOT extends FuncExp {
        @Override
        public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
            Object val = eargs.get(0, backtrace);
            return !Utils.asBoolean(val).booleanValue();
        }
    }

    /**** COMPARISON ****/
    @Arguments(spec={"x","y"})
    @Docstring(text="Check Object Equality. "+
               "Returns true if x equal to y according to call to Java method "+
               "x.equals(y) or if both objects are NIL.")
    public static class EQUAL extends FuncExp {
        @Override
        public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
            final Object v1 = eargs.get(0, backtrace);
            final Object v2 = eargs.get(1, backtrace);
            return (v1==null) ? (v2==null) : v1.equals(v2);
        }
    }

    @Arguments(spec={"x",ARG_REST,"args"})
    public static abstract class NUMCOMP extends FuncExp implements ABSTRACT_OP    {
        @Override
        public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
            boolean result = true;
            Promotion p = new Promotion();
            Number prevVal = Utils.asNumber(eargs.get(0, backtrace));
            List rest = (List)eargs.get(1, backtrace);
            p.promote(prevVal);
            for (int i=0; i< rest.size() ; i++) {
                Number val = Utils.asNumber(rest.get(i));
                p.promote(val);
                Integer dif = p.callOP(this, prevVal, val).intValue();
                result &= compareResult(dif);
                prevVal = val;
            }
            return result;
        }
        @Override
        public Number doIntOp(Number arg1, Number arg2) {
            return  arg1.longValue() - arg2.longValue();
        }
        @Override
        public Number doDoubleOp(Number arg1, Number arg2) {
            final Double compRes = arg1.doubleValue() - arg2.doubleValue();
            return compRes < 0.0 ? -1 : (compRes>0.0 ? 1 : 0);
        }
        @Override
        public Number doFloatOp(Number arg1, Number arg2) {
            final float compRes = arg1.floatValue() - arg2.floatValue();
            return compRes < 0.0f ? -1 : (compRes>0.0f ? 1 : 0);
        }
        protected abstract boolean compareResult(int res);
    }

    @Arguments(spec={"x",ARG_REST,"args"})
    @Docstring(text="Test numeric equality. Returns True if all arguments are numerically equal.  Returns True if only one argument is given")
    public  static class NUMEQ extends NUMCOMP {
        @Override
        protected boolean compareResult(int res) {
            return 0 == res;
        }
        
    }

    @Arguments(spec={"x",ARG_REST,"args"})
    @Docstring(text="Greater Than - Numeric comparison. Returns True if all arguments are monotonically decreasing order.  Returns True if only one argument is given")
    public static class NUMGT extends NUMCOMP {
        @Override
        protected boolean compareResult(int res) {
            return res > 0;
        }
    }

    @Arguments(spec={"x",ARG_REST,"args"})
    @Docstring(text="Greater or Equal - Numeric comparison. Returns True if all arguments are monotonically non-increasing order.  Returns True if only one argument is given")
    public static class NUMGE extends NUMCOMP {
        @Override
        protected boolean compareResult(int res) {
            return res >= 0;
        }
    }

    @Arguments(spec={"x",ARG_REST,"args"})
    @Docstring(text="Less Than - Numeric Comparison. Returns True if all arguments are monotonically increasing order.  Returns True if only one argument is given")
    public static  class NUMLT extends NUMCOMP {
        @Override
        protected boolean compareResult(int res) {
            return res < 0;
        }
    }

    @Arguments(spec={"x",ARG_REST,"args"})
    @Docstring(text="Less or Equal - Numeric comparison. Returns True if all arguments are monotonically non-decreasing order.  Returns True if only one argument is given")
    public static  class NUMLE extends NUMCOMP {
        @Override
        protected boolean compareResult(int res) {
            return res <= 0;
        }
    }

    @Arguments(spec={"x"})
    @Docstring(text="Return Number Sign. Determines a numerical value that indicates whether number is negative, zero, or positive. "+
               "Returns one of -1, 0, or 1 according to whether number is negative, zero, or positive. The type of "+
               "the result is of the same numeric type as x")
    public static class SIGNUM extends FuncExp {
        @Override
        public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
            final Number val = Utils.asNumber(eargs.get(0, backtrace));

            if (val instanceof Double) {
                final double dv = val.doubleValue();
                return  dv > 0.0 ? 1.0 : (dv < 0.0 ?  -1.0 : 0);
            } else if (val instanceof Float) {
                final float fv = val.floatValue();
                return  fv > (float)0.0
                    ? (float) 1.0
                    : (fv < (float)0.0 ?  (float)-1.0 : (float)0);
            } else {
                final Promotion p = new Promotion();
                p.promote(val);
                final long lv = val.longValue();
                return p.returnResult(lv > 0 ? (byte) 1
                                      : (lv < 0 ? (byte) -1 : (byte) 0));
            }
        }
    }

    /**** COERCION *****/
    @Arguments(spec={"value"})
    @Docstring(text="Coerce Value to Boolean. Value may be a Character, a Number, a Boolean, a Byte, a String, any object or NIL:\n" +
               "* Boolean value will be returned as is\n"+
               "* NIL is false\n"+
               "* Character  \u0000 is false.\n"+
               "* any Number which is equal to zero is false\n"+
               "* an empty String is false\n"+
               "* An empty collection is false \n"+
               "* Any other object is true.\n")
    public static class BOOL extends FuncExp {
        @Override
        public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
            final Object val = eargs.get(0, backtrace);
            return Utils.asBoolean(val);
        }
    }
    

    
    @Arguments(spec={"value"})
    @Docstring(text="Coerce Value to Character. Value may be a Character, a Number, a Boolean, a Byte, a Stringor NIL:\n" +
               "* Character value will be returned as is.\n"+
               "* NIL will be converted to unicode value #\u0000.\n"+
               "* a Boolean True value will be returned as character 'T', False as '\0'.\n"+
               "* a Number (other than Byte) will be truncated to short (if needed) and the character at corresponding Unicode code unit will be returned.\n"+
               "* a Byte value will be treated as unsigned integer value and processed as described above.\n"+
               "* a String will be parsed as number using same rules as numeric literals and the resulting value will be used as described above. Conversion to number may fail.\n"+
               "* Any other object will cause conversion error.\n")
    public static class CHAR extends FuncExp {
        @Override
        public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
            final Object val = eargs.get(0, backtrace);
            return Utils.asChar(val);
        }
    }

    
    @Arguments(spec={"value"})
    @Docstring(text="Coerce value to Integer. Value may be a Number, String, any object or NIL." +
               "String will be parsed as number using same rules as numeric literals. "+
               "The floating point value will be truncated.")
    public static class INT extends FuncExp {
        @Override
        public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
            final Object val = eargs.get(0, backtrace);
            return Utils.asNumberOrParse(val).intValue();
        }
    }

    @Arguments(spec={"value"})
    @Docstring(text="Coerce Value to String. Value may be any object or NIL: "+
               "NIL is converted to String \"NIL\", any other object converted "+
               "using it's toString() method")
    public static class STRING extends FuncExp {
        @Override
        public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
            final Object obj = eargs.get(0, backtrace);
            return Utils.asString(obj);
        }
    }

    @Arguments(spec={"value"})
    @Docstring(text="Coerce Value to Long. Value may be a Number, String, any object or NIL." +
               "String will be parsed as number using same rules as numeric literals. "+
               "The floating point values will be truncated.")
    public static class LONG extends FuncExp {
        @Override
        public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
            final Object val = eargs.get(0, backtrace);
            return  Utils.asNumberOrParse(val).longValue();
        }
    }

    @Arguments(spec={"value"})
    @Docstring(text="Coerce Value to Short. Value may be a Number, String, any object or NIL." +
               "String will be parsed as number using same rules as numeric literals. "+
               "The floating point values will be truncated.")
    public static class SHORT extends FuncExp {
        @Override
        public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
            final Object val = eargs.get(0, backtrace);
            return Utils.asNumberOrParse(val).shortValue();
        }
    }

    @Arguments(spec={"value"})
    @Docstring(text="Coerce Value to Byte. Value may be a Number, String, any object or NIL." +
               "String will be parsed as number using same rules as numeric literals. "+
               "The floating point values will be truncated.")
    public static class BYTE extends FuncExp {
        @Override
        public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
            final Object val = eargs.get(0, backtrace);
            return Utils.asNumberOrParse(val).byteValue();
        }
    }
    @Arguments(spec={"value"})
    @Docstring(text="Coerce Value to Double. Value may be a Number, String, any object or NIL." +
               "String will be parsed as number using same rules as numeric literals. "+
               "The floating point values will be truncated.")
    public static class DOUBLE extends FuncExp {
        @Override
        public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
            final Object val = eargs.get(0, backtrace);
            return Utils.asNumberOrParse(val).doubleValue();
        }
    }
    @Arguments(spec={"value"})
    @Docstring(text="Coerce Value to Float. Value may be a Number, String, any object or NIL." +
               "String will be parsed as number using same rules as numeric literals. "+
               "The floating point values will be truncated.")
    public static class FLOAT extends FuncExp {
        @Override
        public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
            final Object val = eargs.get(0, backtrace);
            return Utils.asNumberOrParse(val).floatValue();
        }
    }

    @Arguments(spec={"limit"})
    @Docstring(text="Produce Pseudo-Random Number. Returns a pseudo-random number that is a non-negative number less than limit and of the same numeric type as limit. Implemented uding Java Math.random()")
    public static class RANDOM extends FuncExp {
        @Override
        public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
            final Object numberObj = eargs.get(0, backtrace);
            if (! (numberObj  instanceof Number)) {
                throw new ExecutionException(backtrace, getName() + " argument must be a number");
            }
            final double val = java.lang.Math.random();
            final Number number = (Number) numberObj;
            if (number instanceof Integer) {
                return  (int) (val * number.intValue());
            } else if (numberObj instanceof Long) {
                return  (long) (val * number.longValue());
            } else if (numberObj instanceof Short) {
                return  (short) (val * number.shortValue());
            } else if (numberObj instanceof Float) {
                return  (float) (val * number.floatValue());
            } else if (numberObj instanceof Double) {
                return  (double) (val * number.doubleValue());
            } else if (numberObj instanceof Byte) {
                return  (byte) (val * number.byteValue());
            } else {
                throw new ExecutionException(backtrace, "Unsupported argument numeric type: "+ numberObj.getClass());
            }
        }
    }

    
    /**** VARIABLES, DATATYPES AND FUNCTIONS ****/
    // FIXME: are array types supported?
    @Arguments(spec={"object","type-specifier"})
    @Docstring(text="Check if Object is of Specified Type. Returns True if object is of the specified type. Type specifier may be a Class object or string or symbol which is a valid type-specifier.")
    public static class TYPEP extends FuncExp {
        @Override
        public Object evalWithArgs(Backtrace backtrace,Eargs eargs) {
            final Object val = eargs.get(0, backtrace);
            final Object tspec = eargs.get(1, backtrace);
            if (null == tspec) {
                return null == val;
            }
            final Class tclass = Utils.tspecToClass(tspec);
            return tclass.isInstance(val);          
        }
    }

    @Arguments(spec={"object"})
    @Docstring(text="Return Object Type. Returns type (as class) of the given object. For NIL argument return NIL.")
    public static class TYPE_OF extends FuncExp {
        @Override
        public Object evalWithArgs(Backtrace backtrace,Eargs eargs) {
            Object val = eargs.get(0, backtrace);
            if (null != val) {
                return val.getClass();
            } else {
                return null;
            }
        }
    }
    
    @Arguments(spec={ARG_REST,"symbols"})
    @Docstring(text="Check if Symbols are Bound. Returns True if all the arguments are bound symbols or names of bound symbols; otherwise, returns False.") 
    public static class BOUNDP extends FuncExp {
        @Override
        public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
            for (Object arg : (List)eargs.get(0, backtrace)) {
                if ((null == arg) ||
                    (! eargs.contains(Utils.asString(arg)))) {
                    return false;
                }
            }
            return true;
        }
    }

    @Arguments(spec={ArgSpec.ARG_REST,"pairs"})
    @Docstring(text="Create a HashMap. Returns new HashMap filled with given keys and values. "
               + "Throws InvalidParametersException if non-even number of arguments is given.")
    public static class HASHMAP extends FuncExp {
        @Override
        public void checkParamsList(List <ICompiled> params)
            throws InvalidParametersException {
            super.checkParamsList(params);
            if (0 != (params.size() % 2 )) {
                throw new InvalidParametersException(String.format("%s expects even number of parameters, but got %d",
                                                                   getName(), params.size()));
            }
        }
        @Override
        public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
            Map map = new HashMap();
            List rest = (List)eargs.get(0, backtrace);
            for (int i = 0; i < rest.size(); i+=2) {
                map.put(rest.get(i), rest.get(i+1));
            }
            return map;
        }
    }
    

    // FIXME: allow function be symbol (or function name?)
    @Arguments(spec={"function", ArgSpec.ARG_REST,"arguments"})
    @Docstring(text="Apply Arguments to a Function. Function must be a function object")
    public static class FUNCALL extends AbstractExpr {
        private List<ICompiled> params = null;
        //private ICode  code = null;
        //private ICompiled func = null;
        
        @Override
        public void setParams(List<ICompiled> params)
            throws InvalidParametersException {
            if (params.size()==0) {
                throw new InvalidParametersException(debugInfo, this.getName() +
                                                     " requires at least one argument");
            }
            if (null != this.params) {
                throw new InvalidParametersException(this.getDebugInfo(),
                                                     "internal exception: parameters already set");
            }       
            this.params = params;
        }

    
        // public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
        //     Object val = eargs.get(0, backtrace);
        //     if (!(val  instanceof ICode)) {
        //         throw new RuntimeException("Expected ICode object, but got "+val);
        //     }
        //     ICode code = (ICode) Utils.asObject(val);
        //     if (code != this.code) {
        //      IExpr instance = (IExpr)code.getInstance();
        //      try {
        //          instance.setParams(params.subList(1, params.size()));
        //      } catch (InvalidParametersException e) {
        //          throw new RuntimeException(String.format("%s: called function at %s: does not take parameter: %s",
        //                                                   this.getName(),
        //                                                   (null == e.getParseCtx() ? "?" : e.getParseCtx().toString()),
        //                                                   e.getMessage()));
        //      }
        //      synchronized (this) {
        //          this.code = code;
        //          this.func = instance;
        //      }
        //     }
        //     return this.func.evaluate(backtrace, eargs);
        // }

        //@Override
        //public Eargs evaluateParameters(Backtrace backtrace, ICtx ctx) {
        //    Object eargs[] = new Object[1];
        //    eargs[0]=params.get(0).evaluate(backtrace, ctx);
        //    return ctx.getCompiler().newEargs(eargs, ctx);
        //}

        @Override
        public Object doEvaluate(Backtrace backtrace,ICtx  ctx) {
            final Object functionObj  = params.get(0).evaluate(backtrace, ctx);
            // if (functionObj instanceof Symbol) {
            //  functionObj = params.get(0)eargs.getCompiler().functab.get(fname);
            // } else if (functionObj instanceof String) {
            // }
            
            if (!(functionObj  instanceof ICode)) {
                throw new RuntimeException("Expected ICode object, but got " + functionObj);
            } 
            ICode function = (ICode) Utils.asObject(functionObj);
            //if (code != this.code) {
            IExpr instance = (IExpr)function.getInstance();
            try {
                instance.setParams(params.subList(1, params.size()));
            } catch (InvalidParametersException e) {
                throw new RuntimeException(String.format("%s: called function at %s: does not take parameter: %s",
                                                         this.getName(),
                                                         (null == e.getParseCtx() ? "?" : e.getParseCtx().toString()),
                                                         e.getMessage()));
            }
            // FIXME: put back  optimization!
            
            //synchronized (this) {
            //    this.code = code;
            //    this.func = instance;
            //}
            //}
            //return this.func.evaluate(backtrace, ctx);
            return instance.evaluate(backtrace, ctx);
            //return evalWithArgs(backtrace, evaluateParameters(backtrace, ctx));
        }

    }


    
    /****** MAPPING OPERATIONS ******/
    // args is a spreadable list designator
    @Arguments(spec={"f", ArgSpec.ARG_REST,"arguments"})
    @Docstring(text="Apply function to arguments. arguments must be a spreadable list designator, i.e. if the last argument is a list, it contents will be appended to the list of arguments.")
    public static class APPLY extends FuncExp  {
        @Override
        public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
            Object val = eargs.get(0, backtrace);
            ICode lambda = (ICode) Utils.asObject(val);
            IExpr instance = (IExpr)lambda.getInstance();
            List fArgs = (List) eargs.get(1, backtrace);

            ICtx applyCtx = eargs.getCompiler().newCtx(eargs);


            List rest = null;
            int restSize;
            int headSize;  
            if ((fArgs.size()>0) && ((fArgs.get(fArgs.size()-1)) instanceof List)) {
                rest = (List) fArgs.get(fArgs.size()-1);
                restSize = rest.size();
                headSize = fArgs.size() - 1;
            } else {
                restSize = 0;
                headSize = fArgs.size();
            }
            List<ICompiled> callParams = new ArrayList(restSize + headSize);
            for (int i=0; i < headSize; i++) {
                callParams.add(new ObjectExp(fArgs.get(i)));
            }
            for (int i = 0; i < restSize ; i++) {
                callParams.add(new ObjectExp(rest.get(i)));
            }
            try {
                instance.setParams(callParams);
            } catch (InvalidParametersException e) {
                throw new RuntimeException(String.format("%s: called function at %s: does not take provided parameters: %s",
                                                         this.getName(),
                                                         (null == e.getParseCtx() ? "?" : e.getParseCtx().toString()),
                                                         e.getMessage()));
            }
            return instance.evaluate(backtrace, applyCtx);
        }
    }


    protected static List<ICompiled>  setFuncPosParams(IExpr instance, int cnt) {
        final List<ICompiled> callParams = Utils.newPosArgsList(cnt);
        try {
            instance.setParams((List<ICompiled>) callParams);
        } catch (InvalidParametersException e) {
            throw new RuntimeException(String.format("lambda at %s: does not take parameter: %s",
                                                     (null == e.getParseCtx() ? "?" : e.getParseCtx().toString()),
                                                     e.getMessage()));
        }
        return callParams;
    }
    
    @Docstring(text="Reduce operation.\n func is a function of 2 arguments, "+
               "value - optional starting value, seq is input sequence.\n"+
               "When val is not given:  apply func to the first 2 items in the seq, "+
               "then to the result and 3rd, etc. "+
               "If seq contains no items, func must accept no arguments, return (func)."+
               "If seq has 1 item, return it without calling func;\n"+
               "If value is supplied, apply func on value and the first seq element, then "+ 
               "on the result and the second element, etc. If there is no elements - return val;")
    @Arguments(spec={"func",ArgSpec.ARG_OPTIONAL, "val",ArgSpec.ARG_MANDATORY, "seq"})
    public static  class REDUCE extends FuncExp {
        @Override
        public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
            Object val = eargs.get(0, backtrace);
            ICode lambda = (ICode) Utils.asObject(val);
            IExpr instance = (IExpr)lambda.getInstance();

            List list = (List)eargs.get(2, backtrace);
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
            int i = 0;
            Object result;
            if (haveStartVal) {
                result = startVal;
            } else {
                result = list.get(i++);
            }
            List<ICompiled> vars = setFuncPosParams(instance, 2);
            for (; i < list.size(); i++) {
                ICtx newCtx = eargs.getCompiler().newCtx(eargs);
                newCtx.getMappings().put("%1",  result);
                newCtx.getMappings().put("%2",  list.get(i));
                result = instance.evaluate(backtrace, newCtx);
            }
            return result;
        }
    }

    @Docstring(text="Filter operation. test is a function of one argument that returns boolean, seq is input sequence. "+
               "Return a sequence from which the elements that do not satisfy the test have been removed.")
    @Arguments(spec={"test", "sequence"})
    public static  class FILTER extends FuncExp {
        @Override
        public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
            Object val = eargs.get(0, backtrace);
            ICode lambda = (ICode) Utils.asObject(val);
            IExpr instance = (IExpr)lambda.getInstance();
            List list = (List) eargs.get(1, backtrace);
            final String argname = "arg#0";
            final List<ICompiled> callParams = new ArrayList<ICompiled>(1);
            callParams.add(new VarExp(argname));
            try {
                instance.setParams((List<ICompiled>) callParams);
            } catch (InvalidParametersException e) {
                throw new RuntimeException(String.format("FILTER: lambda at %s: does not take parameter: %s",
                                                         (null == e.getParseCtx() ? "?" : e.getParseCtx().toString()),
                                                         e.getMessage()));
            }
            List results = new ArrayList();
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
    public static abstract class ABSTRACTMAPOP extends FuncExp {
        @Override
        public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
            Object val = eargs.get(0, backtrace);
            ICode lambda = (ICode) Utils.asObject(val);
            IExpr instance = (IExpr)lambda.getInstance();

            List rest = (List) eargs.get(1, backtrace);
            final int numLists = rest.size();
            if (numLists == 0) {
                throw new RuntimeException("At least one sequence must be provided");
            }
            final List<ICompiled> callParams = new ArrayList<ICompiled>(numLists);
            // evaluated lists that were given as parameters
            List<Object> lists[] = new List[numLists];
            for (int i=0; i < lists.length; i++) {
                final List<Object> list = (List<Object>) rest.get(i);
                lists[i] = null == list ? new ArrayList() : list;
                callParams.add(new VarExp("arg#"+i));
            }
            try {
                instance.setParams((List<ICompiled>)callParams);
            } catch (InvalidParametersException e) {
                throw new RuntimeException(String.format("%s: lambda at %s: does not take parameter: %s",
                                                         this.getName(),
                                                         (null == e.getParseCtx() ? "?" : e.getParseCtx().toString()),
                                                         e.getMessage()));
            }
        

            List results = new ArrayList();
            callfuncs(backtrace, results, lists, instance, callParams, eargs);
            return results;
        }

        abstract protected void callfuncs(Backtrace backtrace, List results, List lists[], 
                                          IExpr instance, List<ICompiled> callParams, ICtx ctx);
        
        protected int callfunc(Backtrace backtrace, List results, List lists[], int indices[],
                               IExpr instance, List<ICompiled> callParams, ICtx ctx) {
            ICtx loopCtx = ctx.getCompiler().newCtx(ctx);
            for (int listNo = 0; listNo < lists.length; listNo++) {
                List list = lists[listNo];
                if (indices[listNo] < list.size()) {
                    loopCtx.put(((VarExp)callParams.get(listNo)).getName(), list.get(indices[listNo]));
                } else {
                    return listNo;
                }
            }

            final Object result = instance.evaluate(backtrace, loopCtx);
            results.add(result);
            return -1;
        }
    }

    @Arguments(spec={"func", ArgSpec.ARG_REST,"lists"})
    @Docstring(text="Apply function on elements of collections. "+
               "Returns a sequence consisting of the result of applying func to "+
               "the set of first items of each list, followed by applying func to the "+
               "set of second items in each list, until any one of the lists is "+
               "exhausted.  Any remaining items in other lists are ignored. Function "+
               "func should accept number arguments that is equal to number of lists.")
    public static class MAP extends ABSTRACTMAPOP {
        protected void callfuncs(Backtrace backtrace, List results, List lists[], 
                                 IExpr instance, List<ICompiled> callParams, ICtx ctx) {
            int indices[] = new int[lists.length];
            int overflow=-1;
            while (true) {
                overflow = callfunc(backtrace, results, lists, indices, instance, callParams, ctx);
                if (overflow >=0) {
                    break;
                }
                for (int i=0; i<indices.length; i++) {
                    indices[i]++;
                }
            }
        }
    }

    @Arguments(spec={"f", ArgSpec.ARG_REST,"lists"})
    @Docstring(text="Apply function on cartesioan product of lists. Returns a sequence consisting of the result of applying func to "+
               "the cartesian product of the lists. Function func should accept number "+
               "arguments that is equal to number of lists.")
    public static class MAPPROD extends ABSTRACTMAPOP {
        protected void callfuncs(Backtrace backtrace, List results, List lists[], 
                                 IExpr instance, List<ICompiled> callParams, ICtx ctx) {
            int indices[] = new int[lists.length];
            int overflow=-1;
            while (true) {
                overflow = callfunc(backtrace, results, lists, indices, instance, callParams, ctx);
                if (overflow < 0) {
                    indices[0]++;
                    continue;
                }
                if (overflow >= indices.length-1) {
                    //overflow in eldest list, END
                    return;
                }
                if (lists[overflow].size() == 0) {
                    // one of lists is empty
                    return;
                }
                indices[overflow+1]++;
                for (int i =0; i <= overflow; i++) { 
                    indices[i]=0;
                }
            }
        }
    }
    /***** VARIABLE PROPERTIES HANDLING ******/
    @Arguments(spec = {"symbol","property-key"})
    @Docstring(text = "Get Variable Property. Returns value of a property from variable property map")
    public static class GETPROP extends FuncExp {
        @Override
        public Object evalWithArgs(final Backtrace backtrace, Eargs eargs) {
            final Object symbolObj = eargs.get(0, backtrace);
            final Object propKey = eargs.get(1, backtrace);
            if (null == symbolObj) {
                return null;
            }
            return eargs.getProp(Utils.asString(symbolObj),propKey, backtrace);
        }
    }
    @Arguments(spec = {"symbol","property-key","property-value"})
    @Docstring(text = "Set variable property. Sets property value in variable property map")
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
    @Arguments(spec = {"symbol","properties-map"})
    @Docstring(text = "Set Properties Map for a Variable")
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
            eargs.putProps(Utils.asString(symbolObj), (Map)propsObj);
            return null;
        }
    }
    
    /***** CONTEXT HANDLING ******/
    @Arguments(spec = {})
    @Docstring(text = "Create New Empty Context")
    public static class NEW_CTX extends FuncExp {
        @Override
        public Object evalWithArgs(final Backtrace backtrace, Eargs eargs) {
            return eargs.getCompiler().newCtx();
        }
    }

    /***** JAVA INTEROP *****/
    public static class FilteredMap implements Map {
        protected Map src;
        protected Set<Object> filterSet;

        public FilteredMap(Map src, Object filter) {
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
        public void putAll(Map m) {
            src.putAll(m);
        }

        @Override
        public void clear() {
            src.clear();
        }

        @Override
        public Set keySet() {
            return Utils.intersectSets(filterSet, src.keySet());
        }

        @Override
        public Collection values() {
            Set<Object> values = new HashSet<Object>();
            for (Object key : this.keySet()) {
                values.add(get(key));
            }
            return values;
        }

        @Override
        public Set entrySet() {
            Set<Map.Entry> entries = new HashSet<Map.Entry>();
            for (Object key : this.keySet()) {
                final Object entryKey = key;
                entries.add(new Map.Entry() {
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
            Iterator<Entry> i = entrySet().iterator();
            if (!i.hasNext())
                return "{}";

            StringBuilder sb = new StringBuilder();
            sb.append('{');
            for (;;) {
                Entry e = i.next();
                Object key = e.getKey();
                Object value = e.getValue();
                sb.append(key == this ? "(this Map)" : key);
                sb.append('=');
                sb.append(value == this ? "(this Map)" : value);
                if (!i.hasNext())
                    return sb.append('}').toString();
                sb.append(',').append(' ');
            }
        }
    }
    
    public static class BeanMap implements Map {
        protected Object obj;
        //protected Backtrace backtrace;
        protected String prefix;
        protected String suffix;
        // collection of fields names
        protected Object fields;

        public BeanMap(Object obj) {
            this(obj, null, null);
        }

        public BeanMap(Object obj, String prefix, String suffix) {
            this.obj = obj;
            //this.backtrace = backtrace;
            this.prefix = Utils.asStringOrEmpty(prefix);
            this.suffix = Utils.asStringOrEmpty(suffix);
            this.getters = getGettersMap();
        }

        public BeanMap(Object obj, String prefix, String suffix, Object  fields) {
            this.obj = obj;
            //this.backtrace = backtrace;
            this.prefix = Utils.asStringOrEmpty(prefix);
            this.suffix = Utils.asStringOrEmpty(suffix);
            this.fields = fields;
            this.getters = getGettersMap();
        }

        protected Map<String,Method> getGettersMap() {
            Map<String,Method> result = new HashMap<String,Method>();
            Method[] methods = obj.getClass().getMethods();
            for(int i = 0; i < methods.length; i++) {
                Method m = methods[i];
                //if (m.getParameterCount()>0) {
                if (m.getParameterTypes().length > 0) {
                    continue;
                }
                final String methodName = m.getName();
                if (null==methodName || null == m.getReturnType()) {
                    continue;
                }
                StringBuilder kb =  new StringBuilder();
                kb.append(prefix);
                String fieldName = null;
                if (methodName.startsWith("get") && (methodName.length()>=4)) {
                    fieldName = methodName.substring(3, 4).toLowerCase() + methodName.substring(4);
                } else if (methodName.startsWith("is") && (methodName.length()>=3)) {
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
                result.put(kb.toString(),m);
            }
            return result;
        }
                
        protected Map<String,Method> getters;

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
        public Object put(Object key, Object value) {
            throw new RuntimeException("put is not implemented");
        }

        @Override
        public Object remove(Object key) {
            throw new RuntimeException("remove is not implemented");
        }

        @Override
        public void putAll(Map m) {
            throw new RuntimeException("putAll is not implemented");
        }

        @Override
        public void clear() {
            throw new RuntimeException("clear is not implemented");
        }

        @Override
        public Set keySet() {
            return getters.keySet();
        }

        @Override
        public Collection values() {
            Set<Object> values = new HashSet<Object>();
            for (Object key : getters.keySet()) {
                values.add(get(key));
            }
            return values;
        }

        @Override
        public Set entrySet() {
            Set<Map.Entry> entries = new HashSet<Map.Entry>();
            for (Object key : getters.keySet()) {
                final Object entryKey = key;
                entries.add(new Map.Entry() {
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
            Iterator<Entry> i = entrySet().iterator();
            if (!i.hasNext())
                return "{}";
            StringBuilder sb = new StringBuilder();
            sb.append('{');
            for (;;) {
                Entry e = i.next();
                Object key = e.getKey();
                Object value = e.getValue();
                sb.append(key == this ? "(this Map)" : key);
                sb.append('=');
                sb.append(value == this ? "(this Map)" : value);
                if (!i.hasNext())
                    return sb.append('}').toString();
                sb.append(',').append(' ');
            }
            //return sb.toString();
        }
    }

    protected static Object doSelectKeys(Object obj, Object ksObj) {
        if (obj == null || ksObj== null) {
            return Utils.map();
        } else if (obj instanceof Map) {
            return new FilteredMap((Map)obj, ksObj);
        } else {
            return new BeanMap(obj,
                               null,
                               null,
                               ksObj);
        }
    }

    
    @Arguments(spec = {"object",  "keyseq"})
    @Docstring(text = "Returns a map containing only those entries in map whose key is in keys. ")
    public static class SELECT_KEYS extends FuncExp {
        @Override
        public Object evalWithArgs(final Backtrace backtrace, Eargs eargs) {
            final Object obj = eargs.get(0, backtrace);
            final Object ksObj = eargs.get(1, backtrace);
            return doSelectKeys(obj, ksObj);
        }        
    }

    @Arguments(spec = {"object",  "keyseq"})
    @Docstring(text = "Returns a map containing only those entries in map whose key is in keys. ")
    public static class DWIM_FIELDS extends FuncExp {
        @Override
        public Object evalWithArgs(final Backtrace backtrace, Eargs eargs) {
            final Object obj = eargs.get(0, backtrace);
            final Object ksObj = eargs.get(1, backtrace);
            if (Seq.isSequence(obj) && !(obj instanceof Map)) {
                final List result = Utils.list();
                Seq.forEach(obj, new Seq.Operation() {
                        @Override
                        public boolean perform(Object obj) {
                            result.add(doSelectKeys(obj, ksObj));
                            return false;
                        }
                    }
                    , true);
                return result;
            } else {
                return doSelectKeys(obj, ksObj);
            }
        }        
    }

    
    @Arguments(spec = {"object", ARG_OPTIONAL, "prefix", "suffix"})
    @Docstring(text = "Convert Java Bean to a Map. "
               + "Returns a Map based on getters in the passed java object. "
               + "Accepts optional prefix and suffics arguments that are used "
               + "to modify the generated keys.")
    public static class BEAN extends FuncExp {
        @Override
        public Object evalWithArgs(final Backtrace backtrace, Eargs eargs) {
            final Object obj = eargs.get(0, backtrace);
            return new BeanMap(obj, 
                               (String) ((eargs.size() > 1) ? eargs.get(1, backtrace) : null),
                               (String) ((eargs.size() > 2) ? eargs.get(2, backtrace) : null));
        }
    }

    @Arguments(spec={"class-spec"})
    @Docstring(text="Return Class by Class Name."+
               "Return class object according to it's fully qualified class name. " +
               "class-spec may be string, symbol or any object,"+
               "which string representation will be used")
    public static class CLASS extends FuncExp {
        @Override
        public Object evalWithArgs(Backtrace backtrace,Eargs eargs) {
            return Utils.strToClass(Utils.asString(eargs.get(0, backtrace)));
        }
    
    }
    
    @Arguments(spec={"class", ArgSpec.ARG_OPTIONAL, "arglist", "typeslist"})
    @Docstring(text="Return New Class Instance. Optional arglist and typeslist "+
               "parameters specify parameters to be passed to cosnstructor "+
               "and their types. When typelist not given it tries to find "+
               "most narrowly matching constructor on the basis of types of "+
               "the arguments in arglist. If typeslist is provided exactly "+
               "matching constructor will be used.")
    public static class DOTN extends FuncExp {
        @Override
        public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
            final Class cls = Utils.tspecToClass(eargs.get(0, backtrace));
            final List<Object> constrArgs = (eargs.size() > 1) ? (List) eargs.get(1, backtrace) : null;
            final List<Object> tspecs = (eargs.size() > 2) ? (List) eargs.get(2, backtrace) : null;
            final Class[] paramsClasses = Utils.getMethodParamsClasses(constrArgs, tspecs);
            Constructor constr;
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
                Object obj = (null == constrArgs)
                    ? constr.newInstance() 
                    : constr.newInstance(constrArgs.toArray());
                return obj;
            } catch (InstantiationException ex) {
                throw new RuntimeException(ex);
            }  catch (InvocationTargetException ex) {
                throw new RuntimeException(ex);
            }  catch (IllegalAccessException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
    
    public static abstract class FFI extends FuncExp {
        protected Object javaCall(Object object, List parts) {
            try {
                for(int i=0; i<parts.size(); i++) {
                    String partname = Utils.asString(parts.get(i));
                    boolean isMethod = false;
                    List methodParams = null;
                    List paramsTypesSpec = null;
                    //List<?> methodParams = null;
                    if (partname.endsWith("()")) {
                        isMethod = true;
                        partname = partname.substring(0, partname.length()-2);
                        //methodParams = new ArrayList<Object>();
                    } else if (!Utils.isLast(parts, i) &&
                               (parts.get(i+1) instanceof List)) {
                        isMethod = true;
                        methodParams = (List<?>)parts.get(i+1);
                        i++;
                        if ((parts.size()>i+1) &&
                            (parts.get(i+1) instanceof List)) {
                            paramsTypesSpec = (List)parts.get(i+1);
                            i++;
                        }
                    }
                    Class cls;
                    if (object instanceof Class) {
                        cls = (Class)object;
                        object = null;
                    } else {
                        cls = object.getClass();
                    }
                    
                    if (isMethod) {
                        if (null != methodParams) {
                            final Class methodParamClasses[] = Utils.getMethodParamsClasses(methodParams, paramsTypesSpec);
                            Method m;
                            if (null != paramsTypesSpec) {
                                m = cls.getMethod(partname, methodParamClasses);
                            } else {
                                BetterMethodFinder finder = new BetterMethodFinder(cls);
                                m = finder.findMethod(partname, methodParamClasses);
                            }
                            object = m.invoke(object, methodParams.toArray());
                        } else {
                            Method m = cls.getMethod(partname, null);
                            object = m.invoke(object);
                        }
                    } else {
                        Field f = cls.getField(partname);
                        object = f.get(object);
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
            }
        }
        
        protected List<Object> getCallParts(List<Object>  specArgs) {
            final List<Object> parts = new ArrayList<Object>(specArgs.size());
            for(Object val : specArgs) {
                if (val instanceof List) {
                    parts.add(val);
                } else {
                    String spec = (val instanceof Symbol) 
                        ? ((Symbol)val).getName() 
                        : Utils.asString(val);
                    String [] subparts = spec.split("\\.");
                    for (String subpart: subparts) {
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
    @Arguments(spec={"object", ArgSpec.ARG_REST, "call-args"})
    @Docstring(text="Call Java Object Method/Read Field"+
               "Call method of java object or read contend of object field. ")
    public static class DOT extends FFI {
        @Override
        public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
            final Object object = eargs.get(0, backtrace);
            final List<Object> parts = super.getCallParts((List)eargs.get(1, backtrace));
            final Object result = javaCall(object, parts);
            return result;
        }
    }

    @Arguments(spec={"class", ArgSpec.ARG_REST, "call-args"})
    @Docstring(text="Call Static Java Method/Read Static Field"+
               "Call method of java object or read contend of object field. ")
    public static class DOTS extends FFI {
        @Override
        public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
            final List rest = (List)eargs.get(1, backtrace);
            final Object tspec = eargs.get(0, backtrace);
            final Class cls = Utils.tspecToClass(tspec);
            final StringBuilder trail = new StringBuilder();
            final List<Object> parts = super.getCallParts(rest);
            final Object result = javaCall(cls, parts);          
            return result;
        }
    }


    /***** EXCEPTION HANDLING AND DEBUGGING *****/
    @Arguments(spec={})
    @Docstring(text="Return callstack backtrace. "+
               "Returns string representation of current stack frame.")
    public static class BACKTRACE extends FuncExp {
        @Override
        public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
            return (null == backtrace) ?
                "* BACKTRACE: Call backtrace is not available, please call evaluate() with the backtrace parameter *\n":
                backtrace.toString();
        }
    }

    @Arguments(spec={"exception"})
    @Docstring(text="Throw Java Exception. The exception may be a java Throwable object or String. In the latter case a new ExecutionException with given message will be created and thrown.")
    public static class THROW extends FuncExp {
        @Override
        public Object evalWithArgs(Backtrace backtrace,Eargs eargs) {
            Object val = eargs.get(0, backtrace);
            ExecutionException ex;
            if (val instanceof ExecutionException) {
                ex = (ExecutionException) val;
                if (null == ex.getBacktrace()) {
                    ex.setBacktrace(backtrace);
                }
            } else if (val instanceof Throwable) {
                ex = new ExecutionException(backtrace, (Throwable)val);
            } else {
                ex = new ExecutionException(backtrace, Utils. asString(val));
            }
            throw ex;
        }
    }
    
    /**** STRING HANDLING ****/
    @Arguments(spec={"pattern"})
    @Docstring(text="Compile A Regexp Pattern. On success returns a java.util.regex.Pattern objec. On error raises exception.")
    public static  class RE_PATTERN extends FuncExp {
        @Override
        public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
            final String patternStr = Utils.asString(eargs.get(0, backtrace));
            final Pattern pattern = Pattern.compile(patternStr);
            return pattern;
        }
    }

    public static Matcher getMatcher(Eargs eargs, Backtrace backtrace) {
        final Object obj0 = eargs.get(0, backtrace);
        final Object obj1 = eargs.get(1, backtrace);
        if (null == obj1) {
            return (Matcher)obj0;
        } else {
            return  ((Pattern)obj0).matcher((CharSequence)obj1);
        }
    }

    public static Object returnGroups(Matcher m) {
        if (m.groupCount()>0) {
            final List glist = new ArrayList(m.groupCount() + 1);
            glist.add (m.group());
            for (int i = 1; i <= m.groupCount(); i++) {
                glist.add(m.group(i));
            }
            return glist;
        } else {
            return m.group();
        }
    }

    @Arguments(spec={"matcher"})
    @Docstring(text="Return Groups for a Regexp Match. "+
               "Returns the groups from the most recent match/find.\n"+
               "If there are no nested groups, returns a string of the entire\n"+
               "match. If there are nested groups, returns a list of the groups,\n"+
               "the first element being the entire match.")
    public static  class RE_GROUPS extends FuncExp {
        @Override
        public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
            return returnGroups((Matcher)eargs.get(0, backtrace));
        }
    }

    @Arguments(spec={"pattern","char-seq"})
    @Docstring(text="Return Regexp Matcher. "+
               "Returns an instance of java.util.regex.Matcher, "+
               "for use, e.g. in RE-FIND.")
    public static  class RE_MATCHER extends FuncExp {
        @Override
        public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
            final Pattern pattern = (Pattern)eargs.get(0, backtrace);
            final CharSequence charSeq = (CharSequence)eargs.get(1, backtrace);
            return pattern.matcher(charSeq);
        }
    }

    @Arguments(spec={"arg0",ArgSpec.ARG_OPTIONAL,"arg2"}, text="{pattern schar-seq | matcher}")
    @Docstring(text="Perform regexp match. When called With two arguments created java.util.regex.Matcher using pattern and char-seq.\n  "+
               "When called with one arguments it uses given Matcher. \n "+
               "Returns the match, if any, of string to pattern, using Matcher.matches(). \n "+
               "if no groups were defined it returns the matched string.\n "+
               "If groups were defined it returns a list consisting of the full match and matched groups\n "+
               "If there is no match NIL is returned")
    public static  class RE_MATCHES extends FuncExp {
        @Override
        public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
            final Matcher m = getMatcher(eargs,  backtrace);
            if (m.matches()) {
                return returnGroups(m);
            } else {
                return null;
            }
        }
    }

    
    @Arguments(spec={"arg0",ArgSpec.ARG_OPTIONAL,"arg2"}, text="{pattern schar-seq | matcher}")
    @Docstring(text="Perform Regexp Find. "+
               "When called With two arguments creates java.util.regex.Matcher using pattern and char-seq.\n  "+
               "When called with one arguments it uses given Matcher. \n "+
               "Returns the next ,match, if any, of string to pattern, using Matcher.find(). \n "+
               "if no groups were defined it returns the matched string.\n "+
               "If groups were defined it returns a list consisting of the full match and matched groups\n "+
               "If there is no match NIL is returned")
    public static  class RE_FIND extends FuncExp {
        @Override
        public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
            final Matcher m = getMatcher(eargs,  backtrace);
            if (m.find()) {
                return returnGroups(m);
            } else {
                return null;
            }
        }
    }

    
    @Arguments(spec={"arg0",ArgSpec.ARG_OPTIONAL,"arg2"}, text="{pattern schar-seq | matcher}")
    @Docstring(text="Return Results of Regexp Find as a Lazy Sequence. "+
               "When called With two arguments created java.util.regex.Matcher using pattern and char-seq.\n  "+
               "Returns lazy iterable sequence (instance of Iterable) of matches of string to pattern, using Matcher.find(). \n "+
               "When called with one arguments it uses given Matcher. \n "+
               "if no groups were defined the elements of the sequence are the matched string.\n "+
               "If groups were defined it returns a list consisting of the full match and matched groups\n "+
               "If there is no match empty sequence is returned")
    public static class RE_SEQ extends FuncExp {
        @Override
        protected Iterable evalWithArgs(final Backtrace backtrace, final Eargs eargs) {
            return new Iterable() {
                @Override
                public Iterator iterator() {

                    return new Iterator() {
                        final Matcher m = getMatcher(eargs,  backtrace);
                        boolean findResult = m.find();

                        @Override
                        public boolean hasNext() {
                            synchronized (m) {
                                return findResult;
                            }
                        }
                        @Override
                        public synchronized Object next() {
                            synchronized (m) {
                                final Object result = returnGroups(m);
                                // FIXME: call only if requested
                                findResult = m.find();
                                return result;
                            }
                        }
                    };
                }
            };
        }
    }

    @Arguments(spec={"elt", "sequence"})
    @Docstring(text = "Check if an element is contained in a sequence. ")
    public static  class IN extends FuncExp {
        @Override
        public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
            if (2 != eargs.size()) {
                throw new ExecutionException(backtrace,
                                             "Unexpected number of arguments: expected 2 but got "
                                             +eargs.size());
            }
            Object elt = eargs.get(0, backtrace);
            Object seq = eargs.get(1, backtrace);
            final boolean[] holder = new boolean[1];
            Seq.forEach(seq, new Operation() {
                    public boolean perform(Object obj) {
                        if (null != obj && obj.equals(elt)) {
                            return (holder[0]=true);
                        } else if (null == obj && null == elt) {
                            return (holder[0]=true);
                        }
                        return false;
                    }
                }, false);
            return holder[0];
        }
    }


    // FIXME: produce specific retriever before starting the iterations
    // no need to check object type ons each level
    // FIXME: allow use other sequences, including lazy ones
    protected static Object getKeyByIndex(Object ksObj, int ksIdx) {
        if (ksObj instanceof List) {
            if (ksIdx < 0 || ksIdx >= ((List)ksObj).size()) {
                return null;
            }
            return ((List) ksObj).get(ksIdx);
        }
        if (null == ksObj) {
            return null;
        }
        if (ksObj.getClass().isArray()) {
            if (ksIdx < 0 || ksIdx >= Array.getLength(ksObj)) {
                return null;
            }
            return Array.get(ksObj,ksIdx);
        }
        throw new ExecutionException("Cannot use the provided "+
                                     ksObj.getClass()+
                                     " value as list of indexes");
    }

    protected static int getIntIdx(Object key) {
        try {
            return Utils.asNumberOrParse(key).intValue();
        } catch (Exception ex) {
            return -1;
        }
    }

    protected static boolean doGet(final Object obj,
                                   final Object[] result,
                                   final Object keyObj
                                   /*final Backtrace bt*/) {
        result[0] = null;
        if (obj instanceof Map) {
            final Map<Object,Object> map = (Map<Object,Object>) obj;
            result[0] = map.get(keyObj);
            if (null == result[0]) {
                if (!map.containsKey(keyObj)) {
                    return false;
                }
            }
        } else if (obj instanceof List) {
            final List<Object> lst = (List<Object>) obj;
            try {
                result[0] = lst.get(getIntIdx(keyObj));
            } catch (IndexOutOfBoundsException ex) {
                return false;
            }
        } else if (obj instanceof Set) {
            final Set<Object> s = (Set<Object>) obj;
            if (! s.contains(keyObj) ) {
                return false;
            }
            result[0] = keyObj;
        } else if (obj == null) {
            return false;
        } else if (obj instanceof CharSequence) {
            final CharSequence s = (CharSequence) obj;
            try {
                result[0] = s.charAt(getIntIdx(keyObj));
            } catch (IndexOutOfBoundsException ex) {
                return false;
            }
        } else if (obj.getClass().isArray()) {
            try {
                result[0] = Array.get(obj, getIntIdx(keyObj));
            } catch (ArrayIndexOutOfBoundsException ex) {
                return false;
            }
        } else {
            final String keyStr=Utils.asStringOrEmpty(keyObj).trim();
            if (keyStr.length() == 0) {
                return false;
            }
            // FIXME: better check for method name?
            try {
                final String getName = Utils.concat("get",keyStr.substring(0,1).toUpperCase(),keyStr.substring(1));
                final Method m = obj.getClass().getMethod(getName);
                if (null == m.getReturnType()) {
                    return false;
                }
                result[0] = m.invoke(obj);
            } catch (Exception ex) {
                return false;
            }
            // FIXME: if ICtx object
        }
        return true;
    }
    
    @SuppressWarnings("unchecked")
    protected static Object doGetIn(final Object obj,
                                    final Object ksObj,
                                    final int ksIdx,
                                    final Object notDefined,
                                    final Backtrace bt) {
        Object result[] = new Object[1];
        final Object key = getKeyByIndex(ksObj, ksIdx);
        if (null == key) {
            return obj;
        }
        if (doGet(obj, result, key)) {
            return doGetIn(result[0], ksObj, ksIdx + 1, notDefined, bt);
        }
        return notDefined;
        /*
        if (obj instanceof Map) {
            final Map<Object,Object> map = (Map<Object,Object>) obj;
            result = map.get(key);
            if (null == result) {
                if (!map.containsKey(key)) {
                    return notDefined;
                }
            }
        } else if (obj instanceof List) {
            final List<Object> lst = (List<Object>) obj;
            try {
                result = lst.get(getIntIdx(key));
            } catch (IndexOutOfBoundsException ex) {
                return notDefined;
            }
        } else if (obj instanceof Set) {
            final Set<Object> s = (Set<Object>) obj;
            if (! s.contains(key) ) {
                return notDefined;
            }
            result = key;
        } else if (obj == null) {
            return notDefined;
        } else if (obj instanceof CharSequence) {
            final CharSequence s = (CharSequence) obj;
            try {
                result = s.charAt(getIntIdx(key));
            } catch (IndexOutOfBoundsException ex) {
                return notDefined;
            }
        } else if (obj.getClass().isArray()) {
            try {
                result = Array.get(obj, getIntIdx(key));
            } catch (ArrayIndexOutOfBoundsException ex) {
                return notDefined;
            }
        } else {
            final String keyStr=Utils.asStringOrEmpty(key).trim();
            if (keyStr.length() == 0) {
                return notDefined;
            }
            // FIXME: better check for method name?
            try {
                final String getName = Utils.concat("get",keyStr.substring(0,1).toUpperCase(),keyStr.substring(1));
                final Method m = obj.getClass().getMethod(getName);
                if (null == m.getReturnType()) {
                    return notDefined;
                }
                result = m.invoke(obj);
            } catch (Exception ex) {
                return notDefined;
            }
            // FIXME: if ICtx object
            }*/
        //return doGetIn(result[0], ksObj, ksIdx + 1, notDefined, bt);
    }

    @Arguments(spec={"structure", "ks", "&OPTIONAL", "not-found"})
    @Docstring(text = "Returns the value from an associative structure. \n" +
               "Return value from an associative structure struct, \n" + 
               "where ks is a sequence of keys. Returns NIL if the key\n " +
               "is not present, or the not-found value if supplied.")
    public static  class GET_IN extends FuncExp {
        @Override
        public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
            final int argsnum = eargs.size();
            if (argsnum != 2 && argsnum != 3) {
                throw new ExecutionException(backtrace,
                                             "Unexpected number of arguments: expected 2 or 3, but got "
                                             +eargs.size());
            }
            final Object obj = eargs.get(0, backtrace);
            final Object ksObj = eargs.get(1, backtrace);
            final Object notDefined = argsnum == 2 ? null : eargs.get(2, backtrace);
            final Object result = doGetIn(obj, ksObj, 0, notDefined, backtrace);
            return result;
        }
    }

    @Arguments(spec={"structure", "key", "&OPTIONAL", "not-found"})
    @Docstring(text = "Returns the value from an associative structure. \n" +
               "Return value from an associative structure struct, \n" + 
               " Returns NIL if the key is not present, or the not-found value if supplied.")
    public static  class GET extends FuncExp {
        @Override
        public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
            final int argsnum = eargs.size();
            if (argsnum != 2 && argsnum != 3) {
                throw new ExecutionException(backtrace,
                                             "Unexpected number of arguments: expected 2 or 3, but got "
                                             +eargs.size());
            }
            final Object obj = eargs.get(0, backtrace);
            final Object keyObj = eargs.get(1, backtrace);
            final Object notDefined = argsnum > 2 ? eargs.get(2, backtrace) : null;
            final Object result[] = new Object[1];
            return doGet(obj, result, keyObj) ? result[0] : notDefined;
        }
    }
    
    @Arguments(text = "map {key val}+", spec = {"map", "key", "val",ArgSpec.ARG_REST, "kvpairs"})
    @Docstring(text = "Associates value with key in an map structure. \n" +
               "Return new instance of the structure, the original is left unchanged.")
    public static  class ASSOC extends FuncExp {
        @Override
        public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
            Map result = null;
            final int argsnum = eargs.size();
            if (argsnum < 1) {
                throw new ExecutionException(backtrace,
                                             "Unexpected number of arguments: expected at least one argument, but got "
                                             +eargs.size());
            }
            final Object obj = eargs.get(0, backtrace);
            // FIXME: use same type?
            // FIXME: data sharing?
            result = new HashMap();
            if (null != obj) {
                result.putAll((Map)obj);
            }
            doMapAssoc(result, eargs, backtrace);
            return result;
        }
    }

   
    @Arguments(text = "map {key val}+", spec = {"map", "key", "val",ArgSpec.ARG_REST, "kvpairs"})
    @Docstring(text = "Associates value with key in an map structure. \n" +
               "Modifies the object and returns it as the result.")
    public static  class NASSOC extends FuncExp {
        @Override
        public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
            Map result = null;
            if (eargs.size() < 1) {
                throw new ExecutionException(backtrace,
                                             "Unexpected number of arguments: expected at least one argument, but got "
                                             +eargs.size());
            }
            final Object obj = eargs.get(0, backtrace);
            result = null == obj ? new HashMap() : (Map) obj;
            doMapAssoc(result, eargs, backtrace);
            return result;
        }
    }

    protected static void doMapAssoc(Map target, Eargs eargs,Backtrace backtrace) {
        final int argsnum = eargs.size();
        target.put(eargs.get(1,backtrace), eargs.get(2,backtrace));
        if (argsnum > 3) {
            List rest = (List)eargs.get(3,backtrace);
            for (int i = 0; i < rest.size(); i += 2) {
                final Object k = rest.get(i);
                final Object v = i + 1 < rest.size() ? rest.get(i + 1) : null;
                target.put(k, v);
            }
        }
    }

    @Arguments(spec={"item", "sequence"})
    @Docstring(text = "Perform DWIM search of an item in a sequence of objects. ")
    public static  class SEARCH extends FuncExp {
        @Override
        public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
            if (2 != eargs.size()) {
                throw new ExecutionException(backtrace,
                                             "Unexpected number of arguments: expected 2 but got "
                                             +eargs.size());
            }
            Object elt = eargs.get(0, backtrace);
            Object seq = eargs.get(1, backtrace);
            final boolean[] holder = new boolean[1];
            Operation op = null;
            Seq.forEach(seq, op, false);
            return holder[0];
        }
    }


    
    
    @Arguments(spec={"format", ArgSpec.ARG_REST, "values"})
    @Docstring(text="Format String. "+
               "Returns a formatted string using the specified format string (in the "+
               "format of java.util.Formatter) and arguments. Arguments referenced by " +
               "the format specifiers in the format string. If there are more " +
               "arguments than format specifiers, the extra arguments are " +
               "ignored. Throws IllegalFormatException - If a format string contains " +
               "an illegal syntax, a format specifier that is incompatible with the " +
               "given arguments, insufficient arguments given the format string, or "+
               "other illegal conditions.")
    public static  class FORMAT extends FuncExp {
        @Override
        public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
            String format = (String) eargs.get(0, backtrace);
            return String.format(format, ((List)eargs.get(1, backtrace)).toArray());
        }
    }

    @Arguments(spec={ArgSpec.ARG_REST, "args"})
    @Docstring(text="Print Arguments on standard output.")
    public static class PRINT extends FuncExp {
        @Override
        public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
            StringBuffer buf=new StringBuffer();
            for (Object val : (List)eargs.get(0, backtrace)) {
                buf.append(Utils.asString(val));
            }
            System.out.print(buf.toString());
            return buf.toString();
        }
    }
    
    @Arguments(spec={ArgSpec.ARG_REST,"values"})
    @Docstring(text="Concatenate Strings. Returns concatenation "+
               "of string representationx of the function arguments")
    public static class STR extends FuncExp {
        @Override
        public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
            StringBuffer buf=new StringBuffer();
            for (Object val : (List)eargs.get(0, backtrace)) {
                buf.append(Utils.asString(val));
            }
            return buf.toString();
        }
    }

    /****** SEQUENCES ******/
    @Arguments(spec={ArgSpec.ARG_REST,"sequences"})
    @Docstring(text="Concatenate sequences (destructive). "+
               "Adds to the first given sequence (target sequence) all the elements of" +
               "all of the following sequences and return the target sequence.  If no " +
               "sequences were given an empty list will be returned. Target sequence " +
               "must be extendable, that means that objects like Arrays or String " +
               "cannot be target of this operation")
    public static class NAPPEND extends APPEND {
        public NAPPEND() {
            super();
            isDestructive = true;
        }
    }
    
    private static Seq.Operation mkArraySetter(final Object arr,
                                               final int[] counter,
                                               Class componentType) {
        if (componentType.isPrimitive()) {
            if (componentType.equals(Character.TYPE)) {
                return new Seq.Operation() {
                    @Override
                    public boolean perform(Object obj) {
                        Utils.aset(arr,
                                   counter[0]++,
                                   Utils.asChar(obj));
                        return false;
                    }
                };
            } else if (componentType.equals(Boolean.TYPE)) {
                return new Seq.Operation() {
                    @Override
                    public boolean perform(Object obj) {
                        Utils.aset(arr,
                                   counter[0]++,
                                   Utils.asBoolean(obj));
                        return false;
                    }
                };
            } else {
                // six numeric types
                return new Seq.Operation() {
                    @Override
                    public boolean perform(Object obj) {
                        Utils.aset(arr,
                                   counter[0]++,
                                   Utils.asNumber(obj));
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


    @Arguments(spec={"sequence", "start", ArgSpec.ARG_OPTIONAL, "end"})
    @Docstring(text="Return subsequnce of a sequence. "+
               "subseq creates a sequence that is a copy of the subsequence of " +
               "sequence bounded by start and end. Start specifies an offset into the " +
               "original sequence and marks the beginning position of the " +
               "subsequence. end marks the position following the last element of the " +
               "subsequence. subseq always allocates a new sequence for a result; it " +
               "never shares storage with an old sequence. The result subsequence is " +
               "of the same kind as sequence.")
    public static class SUBSEQ extends FuncExp {
        @Override
        @SuppressWarnings("unchecked")
        public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
            Object result = null;
            final int size = eargs.size();
            if (3 != size) {
                throw new ExecutionException(backtrace, "Unexpected number of arguments: "+size);
            }
            final Object seqObj  =  eargs.get(0, backtrace);
            if (null == seqObj) {
                throw new ExecutionException("sequence parameter cannot be NIL");
            }
            final int start = Utils.asNumber(eargs.get(1, backtrace)).intValue();
            final Object endObj  = eargs.get(2, backtrace);
            final Integer end = (null == endObj) ? null : Utils.asNumber(endObj).intValue();
            Class clz = null == seqObj ? Utils.defListClz() : seqObj.getClass();
            if (clz.isArray()) {
                return arraySubseq(clz, backtrace, seqObj, start, end);
            } else if (CharSequence.class.isAssignableFrom(clz)) {
                return charSeqSubseq(clz, backtrace, (CharSequence) seqObj, start, end);
            } else if (List.class.isAssignableFrom(clz)) {
                return listSubseq(clz, backtrace, (List)seqObj, start, end);
            } else {
                throw new ExecutionException(backtrace, "SUBSEQ: Unsupported sequence type: " + clz);
            }
        }

        private Object charSeqSubseq(Class clz, Backtrace bt,
                                     CharSequence seq,
                                     int start,
                                     Integer end) {
            final int siz = seq.length();
            return seq.subSequence(start, null == end ? siz : (end >= siz ? siz : end));
        }

        private Object arraySubseq(Class clz, Backtrace bt, Object arrayObj, int start, Integer end) {
            final Class componentType = clz.getComponentType();
            final int siz = Array.getLength(arrayObj);
            final int endPos = null == end ? siz : (end >= siz ? siz : end);
            final Object result = Array.newInstance(componentType, endPos - start);
            final int[] counter = new int[1];
            final Operation setter = mkArraySetter(result, counter, componentType);
            for (int i = start; i < endPos ; i++) {
                setter.perform(Array.get(arrayObj, i));
            }
            return result;
        }

        protected List  listSubseq(Class clz, Backtrace bt, List lst, int start, Integer end) {
            final int siz = lst.size();
            return lst.subList(start,  null == end ? siz : (end >= siz ? siz : end));
        }
    }
    
    @Arguments(spec={ArgSpec.ARG_REST,"sequences"})
    @Docstring(text="Concatenate sequences (non-destructive). " +
               "append returns a new sequence that is the concatenation of the " +
               "elements of the arguments. All the argument remain unchanged. The " +
               "resulting sequence is of the same type as the first argument. In no " +
               "arguments were given an empty list is returned. If target sequence is " +
               "an array necessary coercions will be performed automatically.")
    public static class APPEND extends FuncExp {
        protected boolean isDestructive;
        public APPEND() {
            isDestructive = false;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
            Object result = null;
            final int size = eargs.size();
            if (1 != size) {
                throw new ExecutionException(backtrace, "Unexpected number of arguments: "+size);
            }
            List seqs = (List)eargs.get(0, backtrace);
            if (seqs.size() == 0) {
                return Utils.list();
            }
            Object firstSeq = seqs.get(0);
            Class clz = null == firstSeq ? Utils.defListClz() : firstSeq.getClass();
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

        private Object charSeqAppend(Class clz, Backtrace bt, List seqs) {
            // FIXME: support other types but StringBuilder
            Object target = seqs.get(0);
            if (isDestructive && !(target instanceof StringBuilder)) {
                throw new ExecutionException(bt, "Unsupported sequence type " + clz +" for being target of destructive operation");
            }
            final StringBuilder result = isDestructive ?
                (StringBuilder)target: new StringBuilder();
            final int numArgs = seqs.size();
            for (int i = isDestructive ? 1 : 0; i < numArgs; i++) {
                Object seq = seqs.get(i);
                Seq.forEach(seq, new Seq.Operation() {
                        @Override
                        public boolean perform(Object obj) {
                            result.append(Utils.asChar(obj));
                            return false;
                        }
                    }, true);
            }
            final CharSequence cs =  StringBuilder.class.isAssignableFrom(clz) ? result :
                (StringBuffer.class.isAssignableFrom(clz) ? new StringBuffer(result) :
                 ((String.class == clz ) ? result.toString() : null));
            if (null == cs) {
                throw new ExecutionException(bt,"Unsupported CharSequence type: "+clz+" only String, StringBuilder, StringBuffer are supported");
            }
            return cs;
        }

        private Object arrayAppend(Class clz, Backtrace bt, List seqs) {
            if (isDestructive) {
                throw new ExecutionException(bt, "Unsupported sequence type " + clz +" for being target of sequence extension");
            }
            int totalLength = 0;
            final int numArgs = seqs.size();
            // FIXME: support for sequences that cannot return their size
            for (int i = 0; i < numArgs; i++) {
                final Object seq = seqs.get(i);
                totalLength += Seq.getLength(seq, true);
            }
            final Class componentType = clz.getComponentType();
            Object result = Array.newInstance(componentType, totalLength);
            final int[] counter = new int[1];

            for (int i = 0; i < numArgs; i++) {
                final Object seq = seqs.get(i);
                Seq.forEach(seq, mkArraySetter(result, counter, componentType),
                            true);
            }
            return result;
        }

        protected Collection colAppend(Class clz, Backtrace bt, List seqs) {
            Object result = null;
            try {
                result = isDestructive ? seqs.get(0) : clz.newInstance();
            } catch (InstantiationException ex) {
                throw new ExecutionException(bt, ex);
            } catch (IllegalAccessException ex) {
                throw new ExecutionException(bt, ex);
            }
            final Collection resultCol = (Collection)result;
            final int numArgs = seqs.size();
            for (int i = isDestructive ? 1 : 0; i < numArgs; i++) {
                Object seq = seqs.get(i);
                Seq.forEach(seq, new Seq.Operation() {
                        @Override
                        public boolean perform(Object obj) {
                            resultCol.add(obj);
                            return false;
                        }
                    }, true);
            }
            return resultCol;
        }
    }
    
    @Arguments(spec={"object-1", "object-2"})
    @Docstring(text="Prepend element to a sequence.")
    public static class CONS extends FuncExp {
        @Override
        @SuppressWarnings("unchecked")
        public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
            final Object  v1 = eargs.get(0, backtrace);
            final Object  v2 = eargs.get(1, backtrace);
            List<Object> resultList;
            if (Utils.asObject(v2) instanceof List) {
                final List<Object> l2 = (List<Object>)Utils.asObject(v2);
                resultList = new ArrayList<Object>(l2.size()+1);
                resultList.add(0, v1);
                resultList.addAll(1, (List<Object>)Utils.asObject(v2));
            } else {
                resultList = new ArrayList<Object>(2);
                resultList.add(0, v1);
                resultList.add(1, v2);
            }
            return resultList;
        }
    }

    @Arguments(spec={"sequence"})
    @Docstring(text="Returns the first element of the sequence. Returns NIL when " +
               "sequence is NIL or empty")
    public static class FIRST extends  NTH {
        @Override
        @SuppressWarnings("unchecked")
        public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
            Object seq = eargs.get(0, backtrace);
            return Seq.getElement(seq, 0);
        }
    }

    @Arguments(spec={"sequence"})
    @Docstring(text="Return length of a sequence. Parameter may be any supported sequence (collection, array, character sequence) or NIL (0 will be returned).")
    public static class LENGTH extends FuncExp {
        @Override
        @SuppressWarnings("unchecked")
        public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
            return Seq.getLength(eargs.get(0, backtrace), true);
        }
    }

    @Arguments(spec={ArgSpec.ARG_REST,"args"})
    @Docstring(text="Create a list. Returns a list containing the supplied objects. ")
    public static class LIST extends FuncExp {
        @Override
        public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
            List rest = (List)eargs.get(0, backtrace);
            final List<Object> lst = new ArrayList<Object>(rest.size());
            for (Object val : rest) {
                lst.add(val);
            }
            return lst;
        }
    }


    
    @Arguments(spec={"n","sequence"})
    @Docstring(text="Locates the nth element of a sequence. n may be any non-negative number. Returns NIL when sequence is NIL or n is out of bounds")
    public static class NTH extends FuncExp {
        @Override
        public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
            final Object seq = Utils.asObject(eargs.get(1, backtrace));
            final int index = Utils.asNumber(eargs.get(0, backtrace)).intValue();
            if (index < 0) {
                throw new RuntimeException(getName() +
                                           "expected non-negative index index, but got"
                                           + index);
            }
            return Seq.getElement(seq, index);
        }
    }

    @Arguments(spec={"sequence"})
    @Docstring(text="Return 2nd and further elements of sqeuence.")
    public static  class REST extends FuncExp {
        @Override
        @SuppressWarnings("unchecked")
        public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
            Object val = eargs.get(0, backtrace);
            final List<Object> list = (List<Object>)Utils.asObject(val);
            return list.subList(1, list.size());
        }
    }

    @Arguments(spec={"object"})
    @Docstring(text="Check if an object is a sequence.")
    public static  class SEQUENCEP extends FuncExp {
        @Override
        @SuppressWarnings("unchecked")
        public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
            final Object val = eargs.get(0, backtrace);
            return Seq.isSequence(val);
        }
    }


    @Arguments(spec={"sequence"})
    @Docstring(text="Reverse a sequence (destructive).")
    public static  class NREVERSE extends FuncExp {
        @Override
        @SuppressWarnings("unchecked")
        public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
            Object val = eargs.get(0, backtrace);
            if (null == val) return null;
            return reverseList((List<Object>) val);
        }

        private Object reverseList(List<Object> list) {
            Collections.reverse(list);
            return list;
        }
    }
    @Arguments(spec={"sequence"})
    @Docstring(text="Reverse a sequence (non-destructive).")
    public static  class REVERSE extends NREVERSE {
        @Override
        @SuppressWarnings("unchecked")
        public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
            final Object val = eargs.get(0, backtrace);
            if (null == val) return null;
            return super.reverseList((List)Utils.copySeq((List<Object>)val));
        }
    }


    @Arguments(spec={"start",  "stop", ArgSpec.ARG_OPTIONAL,  "step"})
    @Docstring(text="Reverse a sequence (destructive).")
    public static class RANGE extends FuncExp {
        @Override
        public Object evalWithArgs(Backtrace bt, Eargs eargs) {
            //final Promotion prom = new Promotion();

            // 0 is default
            final Number start = Utils.asNumber(eargs.get(0, bt));
            //prom.promote(start);

            final Number to =  Utils.asNumber((Number)eargs.get(1, bt));

            // 1 is default
            final Number step = (null == eargs.get(2, bt)) ?
                1 : Utils.asNumber(eargs.get(2, bt)) ;
            //prom.promote(step);

            
            final Collection resultSeq = new RangeList(start, to, step);
            return resultSeq;
        }
    }
    
    @Arguments(spec={"object"})
    @Docstring(text="Coerce object into a sequence.")
    public static class SEQ extends FuncExp {
        @Override
        public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
            Object val = eargs.get(0, backtrace);
            List result = null;
            if ((null == val) || (val instanceof List)) {
                result = (List)val;
            } else if (val instanceof String) {
                // FIXME: split to chars
                result =  Arrays.asList(((String)val).split(""));
            } else if (val.getClass().isArray()) {
                result =  Arrays.asList((Object[])val);
            } else {
                throw new ExecutionException(backtrace, "Do not know how to sequence "+val);
            }
            return result;
        }
    }

    
    public static class SORT extends NSORT {
        protected Object doSort(Object seq, ICode lambda, Backtrace bt, ICtx ctx) {
            return super.doSort(Utils.copySeq(seq), lambda, bt, ctx);
        }
    }

    @Arguments(spec={ArgSpec.ARG_OPTIONAL, "f", ArgSpec.ARG_MANDATORY, "sequence"})
    @Docstring(text="Sort a sequence (destructively).")
    public static class NSORT extends FuncExp {
        @Override
        public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
            final Object seq =     (eargs.size() == 1 ? eargs.get(0, backtrace) : eargs.get(1, backtrace));
            final ICode lambda = (ICode) (eargs.size() == 1 ? null         : eargs.get(0, backtrace)) ;
            return doSort(seq, lambda, backtrace, eargs);
        }
        protected Object doSort(final Object seq,
                                final ICode lambda,
                                final Backtrace backtrace,
                                final ICtx ctx) {
            Comparator comparator = null;
            if (null != lambda) {
                final ICtx localCtx = ctx.getCompiler().newCtx(ctx);
                final IExpr compf = (IExpr)lambda.getInstance();
                try {
                    compf.setParams(Utils.newPosArgsList(2));
                } catch (InvalidParametersException ex) {
                    throw new RuntimeException(String.format("%s: lambda at %s: does not take parameter: %s",
                                                             this.getName(),
                                                             (null == ex.getParseCtx() ? "?" : ex.getParseCtx().toString()),
                                                             ex.getMessage()));
                }
                comparator = new Comparator() {
                        @Override
                        public int compare(Object o1, Object o2) {
                            localCtx.getMappings().put("%1", o1);
                            localCtx.getMappings().put("%2", o2);
                            return (Integer)compf.evaluate(backtrace, localCtx);
                        }
                    };
            }
            if (seq instanceof List)  {
                List col = (List) seq;
                if (null==comparator) {
                    Collections.sort(col);
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
            } else if (null == seq) {
                return seq;
            } else {
                throw new RuntimeException("No idea how to sort object of type "+seq.getClass());
            }
        }
    }

    
    /***** LANGUAGE ****/
    @Arguments(spec={"fn", ARG_OPTIONAL,"name"})
    @Docstring(text="Create new Java thread. "+
               "Creates new Java thread and prepare it for execution of given function fn."+
               "fn must not require parameters for it's execution. The created thread is not started." )
    public static class NEW_THREAD extends FuncExp {
        @Override
        protected Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
            final IExpr instance = (IExpr) ((ICode)eargs.get(0, backtrace)).getInstance();
            try {
                instance.setParams(new ArrayList());
            } catch (InvalidParametersException iex) {
                throw new RuntimeException(iex);
            }
            Runnable r = (Runnable) instance;
            final Thread t = null == eargs.get(1, backtrace) ?
                new Thread(r) :
                new Thread(r,Utils.asString(eargs.get(1, backtrace)));
            Threads.contexts.put(t,eargs);
            return t;
        }
    }

    
    @Arguments(spec={"symbol"})
    @Docstring(text="Returns function bound to given symbol. If no function bound raises an error. The returned object may be a built-in function, compiled function or built-in special form.")
    public static class SYMBOL_FUNCTION extends FuncExp {
        @Override
        public Object evalWithArgs(Backtrace backtrace,Eargs eargs) {
            String fname = Utils.asString(eargs.get(0, backtrace));
            Object funcObj = eargs.getCompiler().getFun(fname);
            Object result = null;
            if  (null == funcObj) {
                throw new RuntimeException("Symbol "+fname+" function value is NULL");
            } else if (funcObj instanceof ICode) {
                return funcObj;
            } else {
                throw new RuntimeException("Symbol "+fname+" function value is of unsupported type: "+funcObj);
            }
        }
    }

    @Arguments(spec={"file-spec"})
    @Docstring(text="Execute program from a file/stream. "+
               "Sequentially executes each form it encounters in the input file/or stream named by resource-spec. Returns exception if input could not be read or there were exceptions while compiling or executing forms an exception will be raised. file-spec may be a java.io.File object, file path as String or opened InputStream.")
    public static class LOAD extends FuncExp {
        protected InputStream openInput(Object loadObj, Backtrace bt) {
            File f = null;
            String srcName = null;
            if (loadObj instanceof String) {
                f = new File((String) loadObj);
                srcName = (String) loadObj;
            } else if (loadObj instanceof File) {
                f = (File) loadObj;
                srcName = f.getPath();
            } else if (loadObj instanceof InputStream) {
                return (InputStream) loadObj;
            }
            try {
                return new FileInputStream(f);
            } catch (IOException ex) {
                throw new ExecutionException(bt, "I/O opening stream", ex);
            }
        }

        protected Boolean load(Backtrace bt, ICtx ctx, Object loadObj) {
            if (loadObj == null) {
                return false;
            }
            final String inputName = Utils.asString(loadObj);
            final ParseCtx pctx = new ParseCtx(inputName);
            InputStream is = null;
            try {
                is = openInput(loadObj, bt);
                ASTNList astns = new ParserWrapper(ctx.getCompiler().getParser()).parse(is);
                for(ASTN astn : astns) {
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
        public Object evalWithArgs(Backtrace backtrace,Eargs eargs) {
            return load(backtrace, eargs, eargs.get(0, backtrace));
        }
    }

    @Arguments(spec={"resource-spec"})
    @Docstring(text="Execute program from Java resource. Sequentially executes each form it encounters in the java resource file named by resource-spec. Returns exception if file could not be read or there were exceptions while compiling or executing forms an exception will be raised.")
    public static class LOADR extends LOAD {
        @Override
        protected InputStream openInput(Object loadObj, Backtrace bt) {
            if (! (loadObj instanceof CharSequence)) {
                throw new ExecutionException(bt,
                                             "Invalid resource path " + loadObj);
            }
            final String srcName = Utils.asString(loadObj);
            final InputStream is = this.getClass().getResourceAsStream(srcName);
            if (null == is) {
                throw new ExecutionException(bt, "Failed to load '"+srcName+"' as resource");
            }
            return is;
        }
    }

    @Docstring(text="Get list of names of defined functions. If names are given use them as filter expressions:  only those which match at least one of filter expressions will be returned. Filters may be strings (substring match) or regular expressions (java.util.regex.Pattern objects).")
    @Arguments(spec={ARG_REST,"names"})
    public static class FUNCTIONS_NAMES extends FuncExp {
        @Override
        public Object evalWithArgs(Backtrace backtrace,Eargs eargs) {
            List results = new ArrayList();
            List rest = (List)eargs.get(0, backtrace);
            for (String key : eargs.getCompiler().getFunKeys()) {
                if (rest.size() > 0) {
                    for (Object obj : rest) {
                        if (obj instanceof Pattern) {
                            final Matcher m = ((Pattern)obj).matcher(key);
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

    @Arguments(spec={"object"})
    @Docstring(text="Check if object is a function. Returns true if object is a function (built-in or user defined); otherwise, returns false. "+
               "A function is an object that represents code to be executed when an appropriate number of arguments "+
               "is supplied. A function can be directly invoked by using it as the first argument to funcall, apply.")
    public static class FUNCTIONP extends FuncExp {
        @Override
        public Object evalWithArgs(Backtrace backtrace,Eargs eargs) {
            final Object obj  = eargs.get(0, backtrace);
            if (null!=obj
                && ICode.class.isAssignableFrom(obj.getClass())) {
                final String codeType = ((ICode)obj).getCodeType();
                return  "function".equals(codeType) || "compiled function".equals(codeType);
            }
            return false;
        }
    }

    @Docstring(text="Evaluate a Parsed Expression. Evaluates parsed form in the current dynamic context and return result of evaluation'")
    @Arguments(spec={"form"})
    public static class EVAL extends FuncExp {
        @Override
        public Object evalWithArgs(Backtrace backtrace,Eargs eargs) {
            final Object obj = eargs.get(0, backtrace);
            final ASTN astn = (obj instanceof ASTN) ?
                (ASTN) obj : Utils.ASTNize(obj, new ParseCtx("<EVAL>"));
            final ICompiled expr = eargs.getCompiler().compile(astn);
            final Object result = expr.evaluate(backtrace, eargs);
            return result;
        }
    }

    @Arguments(spec={"string"})
    @Docstring(text="Parse expression from string. "+
               "Reads expression from string using default parser. Returns expression or NIL if no expression has been read")
    public static class READ_FROM_STRING extends FuncExp {
        @Override
        public Object evalWithArgs(Backtrace bt,Eargs eargs) {
            String str = (String)eargs.get(0, bt);
            ParseCtx pctx = new ParseCtx("<READ_FROM_STRING>");
            ASTNList  astns = eargs.getCompiler().getParser()
                .parse(pctx, str);
            if (null == astns || astns.size() ==0) {
                return null;
            }
            return Utils.unASTN(astns.get(0));
        }
    }

    
    @Docstring(text="Makes new Symbol for a string")
    @Arguments(spec={"symbol-name"})
    public static class SYMBOL extends FuncExp {
        @Override
        public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
            return new Symbol(Utils.asString(eargs.get(0, backtrace)));
        }
    }

    // TODO: support level and uplevel
    @Docstring(text="Unbind variable given by symbol. Always returns symbol.")
    @Arguments(spec={"symbol"})
    public static class MAKUNBOUND extends FuncExp {
        @Override
        public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
            final Symbol sym = (Symbol) eargs.get(0, backtrace);
            final String name = sym.getName();
            eargs.remove(name);
            //ICtx ctx = eargs;
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

    
    @Docstring(text="Change variable value in specified context. "+
               "Set changes the contents of variable symbol in the dynamic context to the given value. If uplevel is set the value will be set in the uplevel-ths previous context. If level is set the value will be changed in the level-th context from the level0 ")
    @Arguments(spec={"symbol","value",ArgSpec.ARG_KEY,"uplevel", "level"}, text="symbol value { level | uplevel }?")
    public static class SET extends FuncExp {
        @Override
        public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
            final Symbol sym = (Symbol) eargs.get(0, backtrace);
            final Object obj = eargs.get(1, backtrace);
            final Object uplevelObj =  eargs.get(2, backtrace);
            final Object  levelObj =  eargs.get(3, backtrace);
            if (null!=levelObj) {
                // absolute level
                if (null != uplevelObj) {
                    throw new ExecutionException(backtrace, getName() + " only one of two must be set: level | uplevel");
                }
                final int level = Utils.asNumber(levelObj).intValue();
                final List<ICtx> parents = eargs.getParentContexts();
                final int lastIdx = parents.size()-1;
                ICtx ctx = parents.get(lastIdx - level);
                ctx.getMappings().put(sym.getName(), obj);
            } else if (null != uplevelObj && null == levelObj) {
                if (null != levelObj) {
                    throw new ExecutionException(backtrace,
                                                 getName() + " only one of two must be set: level | uplevel");
                }
                final int uplevel = Utils.asNumber(uplevelObj).intValue();
                ICtx ctx = eargs.getPrev();
                for (int i = 0; i < uplevel && null!=ctx; i++) {
                    ctx = ctx.getPrev();
                }
                if (null != ctx) {
                    ctx.getMappings().put(sym.getName(), obj);
                } else {
                    throw new ExecutionException(backtrace,
                                                 getName() + "uplevel value exceeds context depth");
                }
            } else {
                eargs.getPrev().greplace(sym.getName(), obj);
            }
            return obj;
        }
    }

    @Docstring(text="Sat array element value. Set value of array element at index to object. If java array is typed (i.e. not array of java.lang.Objects) and object type does not match this function will attempt to perform necessary coercion operations. The coercions work in the same way as INT, FLOAT, STRING and rest of the built-in coercion functions.")
    @Arguments(spec={"array","index","object"})
    public static class ASET extends FuncExp {
        @Override
        public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
            final Object arrayObj =  Utils.asObject(eargs.get(0, backtrace));
            final int index = Utils.asNumber(eargs.get(1, backtrace)).intValue();
            final Object obj = Utils.asObject(eargs.get(2, backtrace));
            Utils.aset(arrayObj, index, obj);
            return obj;
        }
    }


    
    
    @Arguments(spec={"array","index"})
    @Docstring(text="Get Array element value. Return array element at specified index. Throws ArrayOutOfBoundsException if index is invalid")
    public static class AREF  extends FuncExp {
        @Override
        public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
            final Object arrayObj =  Utils.asObject(eargs.get(0, backtrace));
            final int index = Utils.asNumber(eargs.get(1, backtrace)).intValue();
            return Array.get(arrayObj, index);
        }
    }

    @Arguments(spec={"size", ArgSpec.ARG_KEY, "element-type"})
    @Docstring(text="Ceate an Array. "+
               "Creates array of objects of specified size. Optional :element-type argument specifies type of array elements. The default is java.lang.Object")
    public static class MAKE_ARRAY extends FuncExp {
        @Override
        public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
            final int size = Utils.asNumber(eargs.get(0, backtrace)).intValue();
            final Object et = eargs.get("element-type", backtrace);
            if (null != et) {
                final Class tclass = Utils.tspecToClass(et);
                return Array.newInstance(tclass, size);
            } else {
                return  new Object[size];
            }
        }
    }

    protected static Object findFuncObject(Object fobj, Eargs eargs) {
        if ((fobj instanceof Symbol) || (fobj instanceof CharSequence)) {
            String fName = Utils.asString(fobj);
            if (null != fName) {
                return eargs.getCompiler().getFun(fName);
            }
        } else {
            return fobj;
        }
        return null;
    }
    
    
    @Arguments(spec={"function"})
    @Docstring(text="Describe function. "+
               "Return textual description of given function or "+
               "built-in form. function is a symbol or function name or a lambda")
    public static class DESCRIBE_FUNCTION extends FuncExp {
        @Override
        public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
            final Object fobj = Utils.asObject(eargs.get(0, backtrace));
            StringBuilder buf = new StringBuilder(127);
            Object funcObj = findFuncObject(eargs.get(0, backtrace), eargs);
            buf.append("Object " + Utils.asString(fobj));
            if (null != funcObj) {
                if (funcObj instanceof ICode) {
                    buf.append(" is a ");
                    ICode codeObj = (ICode) funcObj;
                    if (codeObj.isBuiltIn()) {
                        buf.append("built-in ");
                    } 
                    buf.append(codeObj.getCodeType());
                    buf.append(" defined at ");
                    buf.append(codeObj.getDefLocation());
                    buf.append("\n");
                    buf.append("Arguments: \n    " + codeObj.getArgDescr()+"\n");
                    buf.append("Documentation: \n    " + codeObj.getDocstring() +"\n");
                } else {
                    buf.append(" is an unknown object of type "+funcObj.getClass());
                }
            } else {
                buf.append(" is not defined");
            }

            return buf.toString();
        }

    }
    
    @Arguments(spec = { "function-name" })
    @Docstring(text = "Get Function Docstring. "+
               "Return documentation string of given function or "
               + "built-in form. function is a symbol or function name or a lambda")
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

    public static class VarExp extends AbstractExpr  {
        private final String varname;

        public String getName() {
            return varname;
        }
        
        public VarExp(String str) {
            this.varname = str;
        }

        @Override
        protected Object doEvaluate(Backtrace backtrace,ICtx  ctx) {
            final Object obj = ctx.get(varname, backtrace);
            if (null == obj) {
                if (ctx.contains(varname)) {
                    return null;
                }
                ctx.onMissingVar(varname);
                //throw new RuntimeException("variable '"+varname+"' does not exist in this context");
            }
            //if (obj instanceof LazyEval) {
            //  return ((LazyEval)obj).getValue(backtrace);
            //}
            return obj;
        }

        @Override
        public void setParams(List<ICompiled> params)
            throws InvalidParametersException {
            throw new InvalidParametersException(debugInfo,
                                                 "setParams called for variable "+varname+
                                                 ": this cannot happen");
        }
    }

    /****** SELF-EVALUATING OBJECTS ******/
    public static class ValueExpr extends AbstractExpr {
        private Object value;

        public ValueExpr(Object value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof ValueExpr) {
                final ValueExpr v = (ValueExpr) o;
                return v.getClass().equals(v.getClass()) &&
                    ((null == this.value && null == v.value) ||
                     (null != this.value && this.value.equals(v.value)));
            } 
            return false;
        }

        @Override
        public void setParams(List<ICompiled> params)
            throws  InvalidParametersException{
            throw new InvalidParametersException(debugInfo,
                                                 "setParams called for "+
                                                 this.getName()+
                                                 ", this cannot happen!");
        }

        @Override 
        public String toString() {
            return null == value ? "NIL" : value.toString();
        }

        @Override
        public Object doEvaluate(Backtrace backtrace,ICtx  ctx) {
            return value;
        }

        // FIXME: do we really need this??
        public Object getValue() {
            return value;
        }
    }


    public static class StringExp extends ValueExpr {
        public StringExp (String str) {
            super(str);
        }
    }

    public static class EmptyListExp extends ValueExpr {
        public EmptyListExp() {
            super(new ArrayList());
        }
        
    }
    
    public static class NumberExp extends  ValueExpr{
        public NumberExp(Number value) {
            super(value);
        }

        @Override
        public boolean equals(Object o) {
            return super.equals(o);
        }
        
    }
    
    public static class BooleanExp extends  ValueExpr{
        public BooleanExp(Boolean value) {
            super(value);
        }
    }

    public static class ObjectExp extends  ValueExpr{
        public ObjectExp(Object value) {
            super(value);
        }
    }
    
    
}
