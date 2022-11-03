package io.opsit.explang;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

public class Utils {
  public static int safeLen(String str) {
    return (null == str) ? 0 : str.length();
  }

  public static Class<?> defListClz() {
    return ArrayList.class;
  }

  /** Make List of positionals args. */
  @SuppressWarnings("varargs")
  @SafeVarargs
  public static <T> List<T> list(T... objs) {
    List<T> lst = new ArrayList<T>(objs.length);
    lst.addAll(Arrays.asList(objs));
    return lst;
  }

  /** Make unmodifiableList list of positionals args. */
  @SuppressWarnings("varargs")
  @SafeVarargs
  public static <T> List<T> clist(T... objs) {
    return Collections.unmodifiableList(list(objs));
  }

  /** Make Map of (key value) pairs of positionals args. */
  @SuppressWarnings("unchecked")
  public static <T, U> Map<T, U> map(Object... objs) {
    Map<T, U> map = new HashMap<T, U>(objs.length >> 1);
    for (int i = 0; i < objs.length; i += 2) {
      map.put((T) objs[i], (U) objs[i + 1]);
    }
    return map;
  }

  /** Make Set of positionals args. */
  @SafeVarargs
  public static <T> Set<T> set(T... objs) {
    Set<T> set = new HashSet<T>(objs.length);
    for (T obj : objs) {
      set.add(obj);
    }
    return set;
  }

  /** Make Read-only Set of positionals args. */
  @SafeVarargs
  public static <T> Set<T> roset(T... objs) {
    Set<T> set = new HashSet<T>(objs.length);
    for (T obj : objs) {
      set.add(obj);
    }
    return Collections.unmodifiableSet(set);
  }

  /** Make Symbol with given name. */
  public static Symbol symbol(String name) {
    return null == name ? null : new Symbol(name);
  }

  /** Make Reader from string content. */
  public static Reader str2reader(String str) {
    final ByteArrayInputStream is = new ByteArrayInputStream(str.getBytes());
    final Reader r = new InputStreamReader(is);
    return r;
  }

  /** Return maximum of two values. */
  public static int max(int intA, int intB) {
    return intA > intB ? intA : intB;
  }

  /** Convert an array to String for printout. */
  public static String arrayAsString(Object val) {
    final int len = Array.getLength(val);
    StringBuilder buf = new StringBuilder(len << 3);
    buf.append("[");
    if (len > 0) {
      buf.append(Array.get(val, 0));
      for (int i = 1; i < len; i++) {
        buf.append(", ");
        buf.append(Array.get(val, i));
      }
    }
    buf.append("]");
    return buf.toString();
  }

  /** Convert an array to String for printout. */
  public static String arrayContentsAsString(Object val) {
    final int len = Array.getLength(val);
    StringBuilder buf = new StringBuilder(len << 3);
    if (len > 0) {
      buf.append(Array.get(val, 0));
      for (int i = 1; i < len; i++) {
        buf.append(", ");
        buf.append(Array.get(val, i));
      }
    }
    return buf.toString();
  }

  /**
   * Convert an object to its String representation.
   *
   * <p>Symbols will be printed as their names, Arrays - using arrayAsString, null as NIL, other
   * objects using their toString() method.
   */
  public static String asString(Object val) {
    return null == val ? "NIL"
        : ((val instanceof Symbol) ? ((Symbol) val).getName()
            : (val.getClass().isArray() ? arrayAsString(val) : val.toString()));
  }

  /** Convert an object to its String representation,
   * return  CharSequences as is or return null if value is null.
   */
  public static CharSequence asCharSequenceOrNull(Object val) {
    return null == val
        ? null
      : ((val instanceof CharSequence)
         ? (CharSequence)val
         : ((val instanceof Symbol)
             ? ((Symbol) val).getName()
             : (val.getClass().isArray() ? arrayAsString(val) : val.toString())));
  }

  /** Convert an object to its String representation or return null if value is null. */
  public static String asStringOrNull(Object val) {
    return (null == val)
        ? null
        : ((val instanceof Symbol) ? ((Symbol) val).getName() : val.toString());
  }

  /** Convert an object to its String representation or return empty String if value is null. */
  public static String asStringOrEmpty(Object val) {
    return (null == val)
        ? ""
        : ((val instanceof Symbol) ? ((Symbol) val).getName() : val.toString());
  }

  /**
   * Convert type specification to Class a object it is representing. Argument may be a Class
   * instance, in this case it will be returned as it is.
   */
  public static Class<?> tspecToClass(Object tspec) throws ClassNotFoundException {
    if (tspec instanceof Class) {
      return (Class<?>) tspec;
    } else {
      return strToClass(asString(tspec));
    }
  }

  /** Create list of positional arguments. */
  public static List<ICompiled> newPosArgsList(int num) {
    List<ICompiled> lst = new ArrayList<ICompiled>(num);
    for (int i = 1; i <= num; i++) {
      lst.add(new Funcs.VarExp("%" + i));
    }
    return lst;
  }

  /**
   * Shallow copy sequence.
   *
   * <p>Creates new instance of sequence of the same type with the same content. Only Lists and
   * Arrays are currently supported.
   */
  public static Object copySeq(Object seq) {
    if (null == seq) {
      return null;
    }
    if (seq instanceof List) {
      final List<?> lst = (List<?>) seq;
      // FIXME: type of created target List
      final List<Object> copy = new ArrayList<Object>(lst.size());
      copy.addAll(lst);
      return copy;
    } else if (seq.getClass().isArray()) {
      return Arrays.copyOf((Object[]) seq, ((Object[]) seq).length);
    } else {
      return seq;
    }
  }

  private static final Map<String, Class<?>> primTypesClasses = new HashMap<String, Class<?>>();

  static {
    for (Class<?> cls :
        new Class<?>[] {
          void.class,
          boolean.class,
          char.class,
          byte.class,
          short.class,
          int.class,
          long.class,
          double.class,
          float.class,
        }) {
      primTypesClasses.put(cls.getName(), cls);
    }
  }

  /**
   * Convert class name to a class object.
   *
   * <p>java.lang classes do not require full path.
   */
  public static Class<?> strToClass(String str) throws ClassNotFoundException {
    if (!str.contains(".")) {
      Class<?> result = primTypesClasses.get(str);
      if (null != result) {
        return result;
      }
      try {
        return Utils.class.getClassLoader().loadClass("java.lang." + str);
      } catch (ClassNotFoundException ex) {
        return Utils.class.getClassLoader().loadClass(str);
      }
    } else {
      return Utils.class.getClassLoader().loadClass(str);
    }
  }

  /**
   * Get array of classes of method parameters given optional array of argument type specifications.
   */
  public static Class<?>[] getMethodParamsClasses(List<?> methodParams, List<?> typeSpecs)
    throws ClassNotFoundException {
    int listSize = (null == methodParams) ? 0 : methodParams.size();
    final Class<?>[] methodParamClasses = new Class<?>[listSize];
    for (int j = 0; j < listSize; j++) {
      if (null != typeSpecs && typeSpecs.size() > j && typeSpecs.get(j) != null) {
        methodParamClasses[j] = tspecToClass(typeSpecs.get(j));
      } else {
        Object paramObj = methodParams.get(j);
        // FIXME: too naive to be useful in most cases
        // need to emulate what compiler does at compile time
        methodParamClasses[j] = (null == paramObj) ? Object.class : paramObj.getClass();
      }
    }
    return methodParamClasses;
  }

  /** Strip objects out of an AST node. */
  public static Object unAstnize(ASTN param) {
    if (param.isList()) {
      List<Object> result = new ArrayList<Object>();
      for (ASTN astn : ((ASTNList) param)) {
        result.add(unAstnize(astn));
      }
      return result;
    } else {
      return param.getObject();
    }
  }

  /** Make AST node of objects (honoring lists). */
  public static ASTN astnize(Object param, ParseCtx ctx) {
    if (param instanceof ASTN) {
      return (ASTN) param;
    } else if (param instanceof List) {
      // ;ASTN lst = new ASTN
      List<ASTN> astnList = new ArrayList<ASTN>(((List<?>) param).size());
      for (Object obj : (List<?>) param) {
        final ASTN node = astnize(obj, ctx);
        astnList.add(node);
      }
      return new ASTNList(astnList, ctx);
    } else {
      return new ASTNLeaf(param, ctx);
    }
  }

  /** Print contents details with selected context bindings
   *  if vars is a collection of variable names. If it is not a sequence
   *  print all vars if vars has implicit boolean value of true,
   *  no variables if false;
   */
  public static String listCtxVars(Compiler.ICtx ctx, Object vars, IObjectWriter w) {
    if (null == vars || !asBoolean(vars)) {
      return ctx.toStringSelf();
    }
    if (Seq.isSeq(vars)) {
      final IObjectWriter writer = null != w
          ? w
          : new IObjectWriter() {
              @Override
              public String writeObject(Object obj) {
                return Utils.asString(obj);
              }
            };

      final StringBuffer buf = new StringBuffer();
      buf.append(ctx.toStringSelf()).append("{");
      final int sz = buf.length();
      Seq.forEach(vars,
          new Seq.Operation() {
            @Override
            public boolean perform(Object o) {
              final String var = Utils.asStringOrNull(o);
              if (ctx.getMappings().containsKey(var)) {
                if (buf.length() > sz) {
                  buf.append(", ");
                }
                final String val = Utils.asString(ctx.getMappings().get(var));
                buf.append(var)
                  .append(":=")
                  .append(writer.writeObject(val));
              }
              return false;
            }
          },
                  false);
      buf.append("}");
      if (null != ctx.getPrev()) {
        buf.append("->").append(ctx.getPrev().toStringSelf());
      }
      return buf.toString();
    }
    return ctx.toStringShort();
  }
  
  
  /** Print list of all parse errors for an AST subtree. */
  public static String listParseErrors(ASTN exprASTN) {
    final StringBuilder buf = new StringBuilder();
    ASTN.Walker errCollector =
        new ASTN.Walker() {
          private void addMsg(ParseCtx pctx, String msg) {
            buf.append(pctx);
            buf.append(": ");
            buf.append(msg).append("\n");
          }
          
          public void walk(ASTN node) {
            final Exception ex = node.getProblem();
            if (null != ex) {
              if (ex instanceof ParserExceptions) {
                List<ParserException> lst = ((ParserExceptions) ex).getExceptions();
                for (ParserException pex : lst) {
                  addMsg(pex.getPctx(), pex.getOrgMessage());
                }
              } else if (ex instanceof ParserException) {
                addMsg(((ParserException)ex).getPctx(),
                       ((ParserException)ex).getOrgMessage());
              } else {
                addMsg(node.getPctx(), ex.getMessage());
              }  
            }
          }
        };
    exprASTN.dispatchWalker(errCollector);
    return buf.toString();
  }
  
  /** Return argument as object. FIXME: do we need this? why it was added? */
  public static Object asObject(Object val) {
    return val;
  }

  /**
   * Return object as char.
   *
   * <p>Character object returned as is, Boolean as 'T' or '\0', Byte as corresponding ASCII
   * character, otherwise ASCII character of numeric value
   */
  public static char asChar(Object val) {
    if (null == val) {
      return (char) 0;
    } else if (val instanceof Character) {
      return ((Character) val).charValue();
    } else if (val instanceof Boolean) {
      return (Boolean) val ? 'T' : '\0';
    } else if (val instanceof Byte) {
      return (char) (0xff & ((byte) val));
    } else {
      return (char) (Utils.asNumber(val).shortValue());
    }
  }


  /** Parse string as Number. FIXME: exc. handling is ugly */
  public static Number parseNumber(String str) {
    if (((str.length() > 1) && (str.startsWith("+") || str.startsWith("-") || str.startsWith(".")))
        || ((str.length() > 0) && (str.charAt(0) >= '0' && str.charAt(0) <= '9'))) {
      NumberFormat nf = NumberFormat.getInstance(new Locale("en)", "US"));
      ParsePosition pos = new ParsePosition(str.startsWith("+") ? 1 : 0);
      try {
        Number num = (Number) nf.parseObject(str.toUpperCase(), pos);
        Character typeSpec = null;
        if (pos.getIndex() + 1 == str.length()) {
          typeSpec = str.charAt(pos.getIndex());
        } else if (pos.getIndex() < str.length()) {
          return null;
        }
        if (str.contains(".") && (num instanceof Long)) {
          num = num.doubleValue();
        }
        if (null != typeSpec) {
          switch (typeSpec.charValue()) {
            case 'l':
            case 'L':
              num = num.longValue();
              break;
            case 'b':
            case 'B':
              num = num.byteValue();
              break;
            case 's':
            case 'S':
              num = num.shortValue();
              break;
            case 'i':
            case 'I':
              num = num.intValue();
              break;
            case 'f':
            case 'F':
              num = num.floatValue();
              break;
            case 'd':
            case 'D':
              num = num.doubleValue();
              break;
            default:
              throw new NumberFormatException(
                  String.format(
                      "Failed to parse numeric literal '%s': invalid type modifier %s",
                      str, typeSpec));
          }
        } else {
          num = (num instanceof Double) ? num : num.intValue();
        }
        return num;
      } catch (Exception ex) {
        throw new NumberFormatException(
            String.format("Failed to parse numeric literal '%s': %s", str, ex.toString()));
      }
    } else {
      throw new NumberFormatException(String.format("Invalid numeric literal '%s'", str));
    }
  }

  /**
   * Coerse object to Number.
   *
   * <p>FIXME: describe conversion FIXME: make configurable
   */
  public static Number asNumber(Object val) {
    if (val instanceof Number) {
      return (Number) val;
    } else if (null == val) {
      return 0;
    } else if (val instanceof Boolean) {
      return ((Boolean) val) ? 1 : 0;
    } else if (val instanceof String) {
      final String str = ((String) val).trim();
      try {
        Number num = parseNumber(str);
        return num;
      } catch (NumberFormatException ex) {
        throw new RuntimeException("String '" + val + "' cannot be coerced to Number", ex);
      }
    }  else if (val instanceof Character) {
      return (short) ((Character) val).charValue();
    } else if (val instanceof Collection) {
      return ((Collection<?>) val).size();
    } else if (val.getClass().isArray()) {
      return java.lang.reflect.Array.getLength(val);
    } else {
      throw new RuntimeException("Object " + val + " cannot be coerced to Number");
    }
  }

  /** Check if the argument is a floating point number. */
  public static boolean isFP(Number num) {
    return ((num instanceof Float) || (num instanceof Double));
  }

  /** Check if index is the last of the list. */
  protected static boolean isLast(List<?> list, int idx) {
    return (list.size() - 1) <= idx;
  }

  /** Coerce object to boolean. */
  public static Boolean asBoolean(Object val) {
    if (null == val) {
      return false;
    }
    if (val instanceof CharSequence) {
      return ((CharSequence) val).length() > 0;
    }
    if (val instanceof Boolean) {
      return (Boolean) val;
    }
    if (val instanceof Number) {
      if (isFP((Number) val)) {
        return 0.0D != ((Number) val).doubleValue();
      } else {
        return 0L != ((Number) val).longValue();
      }
    }
    if (val instanceof Collection) {
      return !((Collection<?>) val).isEmpty();
    }

    if (val instanceof Character) {
      final char c = (char) val;
      return c != '\0';
    }
    if (val.getClass().isArray()) {
      return java.lang.reflect.Array.getLength(val) > 0;
    }
    return true;
  }

  /** Assert true value. */
  public static void assertTrue(boolean val) {
    if (!val) {
      throw new Error("Internal error: assertion failed");
    }
  }

  /** Make array of int. */
  public static int[] array(int... args) {
    return args;
  }

  /** Make array of char. */
  public static char[] array(char... args) {
    return args;
  }

  /** Make array of double. */
  public static double[] array(double... args) {
    return args;
  }

  /** Make array of float. */
  public static float[] array(float... args) {
    return args;
  }

  /** Make array of boolean. */
  public static boolean[] array(boolean... args) {
    return args;
  }

  /** Make array of byte. */
  public static byte[] array(byte... args) {
    return args;
  }

  /** Make array of short. */
  public static short[] array(short... args) {
    return args;
  }

  /** Make array of long. */
  public static long[] array(long... args) {
    return args;
  }

  /** Make array of Objects. */
  public static Object[] arrayOfObjects(Object... args) {
    return args;
  }

  
  /** Set array element. */
  public static void aset(Object arrayObj, int index, Object obj) {
    try {
      Array.set(arrayObj, index, obj);
    } catch (java.lang.IllegalArgumentException ex) {
      final Class<?> clz = arrayObj.getClass();
      final Class<?> ct = clz.getComponentType();
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
      } else if (ct == Float.TYPE || ct == java.lang.Float.class) {
        Array.set(arrayObj, index, Utils.asNumber(obj).floatValue());
      } else if (ct == Double.TYPE || ct == java.lang.Double.class) {
        Array.set(arrayObj, index, Utils.asNumber(obj).doubleValue());
      } else if (ct == Boolean.TYPE || ct == java.lang.Boolean.class) {
        Array.set(arrayObj, index, Utils.asBoolean(obj).booleanValue());
      } else if (ct == java.lang.String.class || ct == java.lang.CharSequence.class) {
        Array.set(arrayObj, index, Utils.asString(obj));
      } else {
        throw ex;
      }
    }
  }

  /** Array deep comparison. */
  public static boolean arraysDeepEquals(Object arrayE, Object arrayV) {
    final Class<?> ec = arrayE.getClass();
    final Class<?> vc = arrayV.getClass();
    final Class<?> ect = ec.getComponentType();
    final Class<?> vct = vc.getComponentType();
    if (Object.class.isAssignableFrom(ect) && Object.class.isAssignableFrom(vct)) {
      return Arrays.deepEquals((Object[]) arrayE, (Object[]) arrayV);
    } else {
      if (ect != vct) {
        return false;
      }
      if (ect == int.class) {
        return Arrays.equals((int[]) arrayE, (int[]) arrayV);
      } else if (ect == char.class) {
        return Arrays.equals((char[]) arrayE, (char[]) arrayV);
      } else if (ect == boolean.class) {
        return Arrays.equals((boolean[]) arrayE, (boolean[]) arrayV);
      } else if (ect == double.class) {
        return Arrays.equals((double[]) arrayE, (double[]) arrayV);
      } else if (ect == byte.class) {
        return Arrays.equals((byte[]) arrayE, (byte[]) arrayV);
      } else if (ect == long.class) {
        return Arrays.equals((long[]) arrayE, (long[]) arrayV);
      } else if (ect == short.class) {
        return Arrays.equals((short[]) arrayE, (short[]) arrayV);
      } else if (ect == float.class) {
        return Arrays.equals((float[]) arrayE, (float[]) arrayV);
      } else {
        // won't happen
        return false;
      }
    }
  }

  /** Check if String is empty. */
  public static boolean isEmpty(String str) {
    return null == str || str.length() == 0;
  }

  /** Return first not null not-empty argument. */
  public static String coalesce(String... strings) {
    for (String s : strings) {
      if (!isEmpty(s)) {
        return s;
      }
    }
    return null;
  }

  /** return first non-null argument. */
  @SafeVarargs
  public static <T> T nvl(T... objects) {
    for (T object : objects) {
      if (null != object) {
        return object;
      }
    }
    return null;
  }

  /** Concatenate string arguments. */
  public static String concat(String... strs) {
    if (null != strs) {
      final StringBuilder b = new StringBuilder(strs.length << 4);
      for (String str : strs) {
        b.append(str);
      }
      return b.toString();
    } else {
      return "";
    }
  }

  /** Compute intersection of sets. */
  public static Set<Object> intersectSets(Set<?>... sets) {
    final Set<Object> result = new HashSet<Object>();
    if (sets.length > 0) {
      result.addAll(sets[0]);
      for (int i = 1; i < sets.length; i++) {
        result.retainAll(sets[i]);
      }
    }
    return result;
  }

  /** Make union of Sets. */
  public static Set<Object> unionSets(Set<?>... sets) {
    final Set<Object> result = new HashSet<Object>();
    for (int i = 0; i < sets.length; i++) {
      result.addAll(sets[i]);
    }
    return result;
  }

  /** Check if Number is equal. */
  public static boolean isRoundNumber(Number val) {
    if (null == val) {
      return false;
    }
    final double d = val.doubleValue();
    return d % 1 == 0;
  }

  /** Check if objects are equal using Object.equals(). */
  public static boolean equal(Object objA, Object objB) {
    if (null == objA) {
      return null == objB;
    }
    return objA.equals(objB);
  }

  /** Check if equal to enum according to the name of the enum value. */
  public static boolean enumEqual(Enum<?> enumE, Object obj) {
    return Seq.sequal(enumE.name(), obj);
  }

  public static final NumCompOp nc = new NumCompOp();

  /** Return true if object are equeal as Objects or Numerically. */
  public static boolean objequal(Object v1, Object v2) {
    if (v1 == null) {
      if (v2 == null) {
        return true;
      } else {
        return false;
      }
    }
    if (v2 == null) {
      return false;
    }
    if (v1.equals(v2)) {
      return true;
    }
    if ((v1 instanceof Number) && (v2 instanceof Number)) {
      return nc.compare((Number) v1, (Number) v2) == 0;
    }
    if (v1.getClass().isEnum()) {
      return enumEqual((Enum<?>) v1, v2);
    }
    if (v2.getClass().isEnum()) {
      return enumEqual((Enum<?>) v2, v1);
    }
    return false;
  }

  /**
   * Tries to copy objects using its copy constructor.
   */
  public static Object copyObjectByCopyConstructor(Object obj) {
    if (null != obj) {
      try {
        final Constructor<?> c = obj.getClass().getConstructor(obj.getClass());
        return c.newInstance(obj);
      } catch (Exception ex) {
        throw new RuntimeException(ex);
      }
    } else {
      return null;
    }
  }

  /**
   * Tries to copy a Cloneable object using its clone() method.
   */
  public static Object cloneObjectByClone(Cloneable obj) {
    if (null != obj) {
      try {
        Method m = obj.getClass().getMethod("clone");
        return m.invoke(obj);
      } catch (Exception ex) {
        throw new RuntimeException(ex);
      }
    } else {
      return null;
    }
  }

  /**
   * Convert character regexp flag to its integer constant.
   * Returns code or 0 if character flag is invalid.
   */
  public static int parseRegexpFlag(char c) {
    switch (c) {
      case 'd':
        return Pattern.UNIX_LINES;
      case 'i':
        return Pattern.CASE_INSENSITIVE;
      case 'x':
        return Pattern.COMMENTS;
      case 'm':
        return Pattern.MULTILINE;
      case 'l':
        return Pattern.LITERAL;
      case 's':
        return Pattern.DOTALL;
      case 'u':
        return Pattern.UNICODE_CASE;
      case 'c':
        return Pattern.CANON_EQ;
      case 'U':
        return Pattern.UNICODE_CHARACTER_CLASS;
      default:
        return 0;
    }
  }

  /**
   * Parse string of regexp flag characters.
   * Returns flag value or throws a runtime exception if
   * flag is invalid.
   */
  public static int parseRegexpFlags(String flagsStr) {
    int flags = 0;
    if (null != flagsStr) {
      for (int i = 0; i < flagsStr.length(); i++) {
        char c = flagsStr.charAt(i);
        int f = parseRegexpFlag(c);
        if (f == 0) {
          throw new RuntimeException("Unknown regex flag character '" + c +  "'");
        }
        flags |= f;
      }
    }
    return flags;
  }

  // FIXME: make configurable
  //        to enable user to add handling of
  //        various kinds of @Immutable annotations
  /**
   * Return true if Object is known to be immutable.
   *
   * <p>Currently using list of types that known to be Immutable.
   */
  public static boolean isKnownImmutable(Object obj) {
    return null == obj 
      || obj instanceof Character
      || obj instanceof String
      || obj instanceof Number
      || obj instanceof Boolean
      || obj instanceof Enum 
      || obj instanceof Class
      || obj instanceof Void
      || obj instanceof java.util.regex.Pattern 
      // FIXME: kluge?
      || obj.getClass().toString().startsWith("java.util.ImmutableCollections$");
  }
  
  public static final String MAVEN_PROPS =
      "META-INF/maven/io.opsit/opsit-explang-core/pom.properties";
  
  /**
   * Return explang core jar version.
   */
  public static String getExplangCoreVersionStr() {
    final ClassLoader loader = Utils.class.getClassLoader();
    InputStream is = loader.getResourceAsStream(MAVEN_PROPS);
    if (null != is) {
      final Properties props = new Properties();
      try {
        props.load(is);
        return props.getProperty("version");
      } catch (IOException iex) {
        return null;
      } finally {
        try {
          is.close();
        } catch (IOException ex) {
          // won't happen hoppefully
        }
      }
    }
    return null;
  }
}
