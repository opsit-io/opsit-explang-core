package org.dudinea.explang;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Array;

public class Utils {
    public static int safeLen(String str) {
	return (null == str) ? 0 : str.length();
    }

    public static Class defListClz() {
	return ArrayList.class;
    }
    public static <T>List<T> list(T ... objs) {
        List <T>lst = new ArrayList<T>(objs.length);
        lst.addAll(Arrays.asList(objs));
        return lst;
    }

    public static <T>List<T> clist(T ... objs) {
        return Collections.unmodifiableList(list(objs));
    }

    public  static <T,U>Map<T,U> map(Object ... objs) {
        Map <T,U> map  = new HashMap<T,U>(objs.length >> 1);
	for (int i = 0; i < objs.length; i+=2) {
	    map.put((T)objs[i],(U)objs[i+1]);
	}
        return map;
    }

    
    public  static Set<Object> set(Object ... objs) {
	Set set = new HashSet(objs.length);
	for (Object obj: objs) {
	    set.add(obj);
	}
	return set;
    }

    public static Symbol symbol(String name) {
	return null == name ? null : new Symbol(name);
    }
    
    public static Reader str2reader(String str) {
	final ByteArrayInputStream is = new ByteArrayInputStream(str.getBytes());
	final Reader r = new InputStreamReader(is);
	return r;
    }

    public static int max(int a, int b) {
	return a > b ? a : b;
    }

    public static String arrayAsString(Object v) {
	final int len = Array.getLength(v);
	StringBuilder b = new StringBuilder(len << 3);
	b.append("[");
	if (len > 0) {
	    b.append(Array.get(v, 0));
	    for (int i = 1; i < len; i++) {
		b.append(", ");
		b.append(Array.get(v, i));
	    }
	}
	b.append("]");
	return b.toString();
    }

    public static String asString(Object val) {
        return null == val ? "NIL" : 
            ((val instanceof Symbol)  ? ((Symbol)val).getName() :
	     (val.getClass().isArray() ? arrayAsString(val)  :
	      val.toString()));
    }

    public static String asStringOrNull(Object val) {
        return (null == val) ?
	    null : 
            ((val instanceof Symbol)  ? ((Symbol)val).getName() : val.toString());
    }
    public static String asStringOrEmpty(Object val) {
        return (null == val) ?
	    "" : 
            ((val instanceof Symbol)  ? ((Symbol)val).getName() : val.toString());
    }
    
    public static Class tspecToClass(Object tspec) {
	if (tspec instanceof Class) {
	    return (Class)tspec;
	} else {
	    return strToClass(asString(tspec));
	}
    }

    public static List<ICompiled> newPosArgsList(int num) {
	List<ICompiled> lst = new ArrayList<ICompiled>(num);
	for (int i = 1; i <= num ; i++) {
	    lst.add(new Funcs.VarExp("%"+i));
	}
	return lst;
    }

    public static Object copySeq(Object seq) {
	if (null == seq) {
	    return null;
	}
	if (seq instanceof List) {
	    final List lst = (List)seq;
	    final List copy = new ArrayList(lst.size());
	    copy.addAll(lst);
	    return copy;
	} else if (seq.getClass().isArray()) {
	    return Arrays.copyOf((Object[])seq, ((Object[])seq).length);
	} else {
	    return seq;
	}
    }

    
    final private static Map<String,Class> primTypesClasses = new HashMap();
    static {
	for (Class cls :  new Class[] {
		void.class,boolean.class,char.class,byte.class,
		short.class,int.class,long.class,
		double.class,float.class,
	    }) {
	    primTypesClasses.put(cls.getName(), cls);
	}
    }
    
    
    public static Class strToClass(String str) {
	Class result;
	try {
	    result = Utils.class.getClassLoader().loadClass(str);
	} catch (ClassNotFoundException ex) {
	    if (!str.contains(".")) {
		result = primTypesClasses.get(str);
		if (null == result) {
		    str = "java.lang."+str;
		    try {
			result = Utils.class.getClassLoader().loadClass(str);
		    } catch (ClassNotFoundException ex2) {
			throw new RuntimeException(ex);
		    }
		}
	    } else {
		throw new RuntimeException(ex);
	    }
	}
	return result;
    }

    public static Class[] getMethodParamsClasses(List methodParams, List typeSpecs) {
	int listSize = (null == methodParams) ? 0 : methodParams.size();
	final Class [] methodParamClasses = new Class[listSize];
	for (int j=0; j < listSize; j++) {
	    if (null != typeSpecs &&
		typeSpecs.size()>j &&
		typeSpecs.get(j) != null) {
		methodParamClasses[j] =  tspecToClass(typeSpecs.get(j));
	    } else {
		Object paramObj = methodParams.get(j);
		// FIXME: too naive to be useful in most cases
		// need to emulate what compiler does at compile time
		methodParamClasses[j] = (null == paramObj) ? Object.class : paramObj.getClass();
	    }
	} 
	return methodParamClasses; 
    }

    public static Object unASTN(ASTN param)  {
	if (param.isList()) {
	    List<Object> result = new ArrayList<Object>();
	    for (ASTN astn : ((ASTNList)param)) {
		result.add(unASTN(astn));
	    } 
	    return result;
	} else {
	    return param.getObject();
	}
    }

    public static ASTN ASTNize(Object param, ParseCtx ctx) {
	if (param instanceof List) {
	    //;ASTN lst = new ASTN
	    List <ASTN>astnList = new ArrayList<ASTN>(((List)param).size());
	    for (Object obj : (List)param) {
		final ASTN node = ASTNize(obj,ctx);
		astnList.add(node);
	    }
	    return new ASTNList(astnList, ctx);
	} else {
	    return new ASTNLeaf(param, ctx);
	}
    }
    

    public static Object asObject(Object val) {
        return val;
    }

    public static char asChar(Object val) {
	if (null == val) {
	    return (char)0;
	} else if (val instanceof Character) {
	    return ((Character)val).charValue();
	} else if (val instanceof Boolean) {
	    return (Boolean)val ? 'T' : '\0';
	} else if (val instanceof Byte) {
	    return (char)(0xff & ((byte)val));
	} else {
	    return (char)(Utils.asNumberOrParse(val).shortValue());
	}
    }

    // FIXME: make configurable
    // FIXME: strings
    public static Number asNumber(Object val) {
	if (val == null) {
	    return 0;
	} else if (val instanceof Number) {
            return (Number)val;
        } else if (val instanceof Character) {
	    return (short)((Character)val).charValue();
	} else if (val instanceof Boolean) {
	    return ((boolean)val) ? 1 : 0;
	} else if (val instanceof Collection) {
	    return ((Collection) val).size();
	} else if (val.getClass().isArray()) {
	    return java.lang.reflect.Array.getLength(val);
	}
        return 0;
    }


    public static Number parseNumber(String str) {
	if (((str.length() >  1) &&
	     (str.startsWith("+") ||
	      str.startsWith("-") ||
	      str.startsWith("."))) ||
	    ((str.length()>0) &&
	     (str.charAt(0)>='0' && str.charAt(0)<='9'))) {
	    NumberFormat  nf = NumberFormat.getInstance(new Locale("en)","US"));
	    ParsePosition pos = new ParsePosition(str.startsWith("+")?1:0);
	    try {
		Number n = (Number)nf.parseObject(str,pos);
		Character typeSpec = null;
		if (pos.getIndex() + 1 == str.length()) {
		    typeSpec = str.charAt(pos.getIndex());
		} else if (pos.getIndex() < str.length()) {
		    return null;
		}
		if (str.contains(".") &&
		    (n instanceof Long)) {
		    n = n.doubleValue();
		}
		if (null != typeSpec) {
		    switch(typeSpec.charValue()) {
		    case 'l':
		    case 'L':
			n=n.longValue();
			break;
		    case 'b':
		    case 'B':
			n=n.byteValue();
			break;
		    case 's':
		    case 'S':
			n=n.shortValue();
			break;
		    case 'i':
		    case 'I':
			n=n.intValue();
			break;
		    case 'f':
		    case 'F':
			n=n.floatValue();
			break;
		    case 'd':
		    case 'D':
			n=n.doubleValue();
			break;
		    default:
			throw new NumberFormatException(String.format("Failed to parse numeric literal '%s': invalid type modifier %s",
								      str,typeSpec));
		    }
		} else {
		    n = (n instanceof Double) ? n : n.intValue();
		}
		return n;
	    } catch (Exception ex) {
		throw new NumberFormatException(String.format("Failed to parse numeric literal '%s': %s", str, ex.toString()));
	    }
	} else {
	    throw new NumberFormatException(String.format("Invalid numeric literal '%s'", str));
	}
    }

    public static Number asNumberOrParse(Object val) {
	if (null == val) {
	    return 0;
	} else if (val instanceof Boolean) {
	    return ((Boolean) val) ? 1 : 0;
	} else if (val instanceof String) {
	    final String str = ((String)val).trim();
	    try {
		Number n = parseNumber(str);
		return n;
	    } catch (NumberFormatException ex) {
		throw new RuntimeException("String '"+val+"' cannot be coerced to Number", ex);
	    }
	} else if (val instanceof Number) {
            return (Number)val;
        } else if (val instanceof Character) {
	    return (short)((Character)val).charValue();
	} else if (val instanceof Collection) {
	    return ((Collection) val).size();
	} else if (val.getClass().isArray()) {
	    return java.lang.reflect.Array.getLength(val);
	} else {
	    throw new RuntimeException("Object "+val+" cannot be coerced to Number");
	}
    }

    public static boolean isFP(Number num) {
        return ((num instanceof Float) ||
                (num instanceof Double));
    }

    protected static boolean isLast(List<?> list,int idx) {
        return (list.size() - 1) <= idx;
    }

    
    public static Boolean asBoolean(Object val) {
        if (null == val) {
            return false;
        }
        if (val instanceof String) {
            return !((String) val).isEmpty();
        }
        if (val instanceof Boolean) {
            return (Boolean)val;
        }
        if (val instanceof Number) {
            if (isFP((Number)val)) {
                return 0.0D!=((Number)val).doubleValue();
            } else {
                return 0L!=((Number)val).longValue();
            }
        }
        if (val instanceof Collection) {
            return !((Collection<?>)val).isEmpty();
        }
	
	if (val instanceof Character) {
	    final char c = (char)val;
	    return c!='\0';
	}
	if (val.getClass().isArray()) {
	    return java.lang.reflect.Array.getLength(val) > 0;
	}
        return true;
    }

    public static void assertTrue(boolean b) {
	if (!b) {
	    throw new Error("Internal error: assertion failed");
	}
    }

    public static int[]  array (int ...args) {
	return args;
    }
    public static char[]  array (char ...args) {
	return args;
    }
    public static double[]  array (double ...args) {
	return args;
    }

    public static float[]  array (float ...args) {
	return args;
    }
    public static boolean[]  array (boolean ...args) {
	return args;
    }


    public static byte[]  array (byte ...args) {
	return args;
    }
    public static short[]  array (short ...args) {
	return args;
    }

    public static long[]  array (long ...args) {
	return args;
    }


    public static void aset(Object arrayObj, int index, Object obj)  {
	try {
	    Array.set(arrayObj, index, obj);
	} catch (java.lang.IllegalArgumentException ex) {
	    final Class clz = arrayObj.getClass();
	    final Class ct = clz.getComponentType();
	    if (ct == Character.TYPE || ct == java.lang.Character.class) {
		Array.set(arrayObj, index, Utils.asChar(obj));
	    } else if (ct == Byte.TYPE || ct == java.lang.Byte.class) {
		Array.set(arrayObj, index, Utils.asNumber(obj).byteValue());
	    } else if (ct == Short.TYPE || ct == java.lang.Short.class) {
		Array.set(arrayObj, index, Utils.asNumber(obj).shortValue());
	    } else if (ct == Integer.TYPE || ct == java.lang.Integer.class) {
		Array.set(arrayObj, index, Utils.asNumber(obj).intValue());
	    } else if (ct == Long.TYPE || ct == java.lang.Long.class) {
		Array.set(arrayObj, index, Utils.asNumber(obj).longValue());
	    } else if (ct == Float.TYPE || ct == java.lang.Float.class)   {
		Array.set(arrayObj, index, Utils.asNumber(obj).floatValue());
	    } else if (ct == Double.TYPE || ct == java.lang.Double.class) {
		Array.set(arrayObj, index, Utils.asNumber(obj).doubleValue());
	    } else if (ct == Boolean.TYPE || ct == java.lang.Boolean.class) {
		Array.set(arrayObj, index, Utils.asBoolean(obj).booleanValue());
	    } else if (ct == java.lang.String.class || ct==java.lang.CharSequence.class) {
		Array.set(arrayObj, index, Utils.asString(obj));
	    } else {
		throw ex;
	    }
	}
    }

    public static boolean arraysDeepEquals(Object e, Object v) {
	final Class ec = e.getClass();
	final Class vc = v.getClass();
	final Class ect = ec.getComponentType();
	final Class vct = vc.getComponentType();
	if (Object.class.isAssignableFrom(ect) &&
	    Object.class.isAssignableFrom(vct)) {
	    return Arrays.deepEquals( (Object[])e , (Object[])v);
	} else {
	    if (ect != vct) {
		return false;
	    }
	    if (ect == int.class) {
		return Arrays.equals( (int[])e , (int[])v);
	    } else if (ect == char.class) {
		return Arrays.equals( (char[])e , (char[])v);
	    } else if (ect == boolean.class) {
		return Arrays.equals( (boolean[])e , (boolean[])v);
	    } else if (ect == double.class) {
		return Arrays.equals( (double[])e , (double[])v);
	    } else if (ect == byte.class) {
		return Arrays.equals( (byte[])e , (byte[])v);
	    } else if (ect == long.class) {
		return Arrays.equals( (long[])e , (long[])v);
	    } else if (ect == short.class) {
		return Arrays.equals( (short[])e , (short[])v);
	    } else if (ect == float.class) {
		return Arrays.equals( (float[])e , (float[])v);
	    } else {
		// won't happen
		return false;
	    }
 	} 
    }
    
}
