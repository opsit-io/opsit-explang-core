package io.opsit.explang;

import static io.opsit.explang.Funcs.FuncExp;
import static io.opsit.explang.Utils.list;

import io.opsit.explang.Compiler.Eargs;
import io.opsit.explang.Funcs.NUMEQ;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class DWIM {
  @Arguments(
      spec = {"object", ArgSpec.ARG_OPTIONAL, "pattern"},
      text = "{object pattern}")
  @Docstring(
      text =
          "Perform Do What I Mean style search.\n "
              + " When pattern is an Regexp tries to find the regexp in"
              + " String Representation of object.\n"
              + "  When pattern is a String it tries to find its occurences in the String"
              + " Representation of object.\n"
              + " When pattern is a Number then numeric equality check is performed. \n"
              + " When pattern is any other object the equality test is performed. \n"
              + " If there were matches the function returns list of matches. \n"
              + "If there is no match an empty list is returned.")
  public static class DWIM_MATCHES extends NUMEQ {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      List<Object> result = list();
      final Object obj = eargs.get(0, backtrace);
      final Object patObj = eargs.get(1, backtrace);
      if (patObj == null) {
        if (obj == null) {
          result.add(null);
        }
      } else if (obj != null) {
        if (patObj instanceof Pattern) {
          final String objStr = Utils.asString(obj);
          final Pattern pat = (Pattern) patObj;
          final Matcher m = pat.matcher(objStr);
          while (m.find()) {
            result.add(m.group());
          }
        } else if (patObj instanceof Number) {
          try {
            final Number objNum = Utils.asNumber(obj);
            final Number patNum = (Number) patObj;
            final Promotion p = new Promotion();
            p.promote(patNum);
            p.promote(objNum);
            final Integer dif = p.callOP(this, patNum, objNum).intValue();
            final boolean isEqual = compareResult(dif);
            if (isEqual) {
              result.add(objNum);
            }
          } catch (Exception ex) {
            return result;
          }
        } else if (patObj instanceof CharSequence) {
          final String objStr = Utils.asString(obj);
          final String patStr = Utils.asString(patObj);
          int idx = objStr.indexOf(patStr);
          if (idx >= 0) {
            result.add(patStr);
          }
        } else {
          if (obj.equals(patObj)) {
            result.add(patObj);
          }
        }
      }
      return result;
    }
  }

  protected static Map<Object,Object> mkFields(Map<? extends Object,Object> obj,
                                               Map<Object,FieldsMap.Op> fmap) {
    return new FieldsMap(obj, fmap);
  }

  @SuppressWarnings("unchecked")
  protected static FieldsMap.Op mkOp(Object srcSpec) {
    return (srcSpec instanceof List)
      ? mkOpByGetIn((List<Object>) srcSpec)
      : mkOpByGet(srcSpec);
  }
  
  protected static FieldsMap.Op mkOpByGet(Object key) {
    return new FieldsMap.Op() {
      @Override
      public Object get(Map<?, ?> m) {
        return m.get(key);
      }
    };
  }

  protected static FieldsMap.Op mkOpByGetIn(List<Object> keyList) {
    return new FieldsMap.Op() {
      @Override
      public Object get(Map<?, ?> src) {
        return Seq.doGetIn(src, keyList, 0, null /* default */);
      }
    };
  }

  @SuppressWarnings("unchecked")
  protected static void addKsOp(Map<Object, FieldsMap.Op> fmap, Object keyspec, Backtrace bt) {
    Object srcSpec = null;
    Object dstSpec = null;
    Object subKeySpec = null;
    if ((keyspec instanceof Symbol) || (keyspec instanceof CharSequence)) {
      dstSpec = keyspec;
      srcSpec = Utils.asString(keyspec);
    } else if (Seq.isIndexed(keyspec)) {
      int len = Seq.getLength(keyspec, false);
      if (len < 2 || len > 3) {
        throw new ExecutionException(
            "Keyspec '" + keyspec + "' is invalid: must be of length 2 or 3");
      }
      srcSpec = Seq.getElementByIndex(keyspec, 0); // 
      dstSpec = Seq.getElementByIndex(keyspec, 1); // key in the target map
      subKeySpec = Seq.getElementByIndex(keyspec, 2);
    } else {
      throw new ExecutionException(
          "Keyspec '" + keyspec + "' is invalid," + " must be a symbol or a list");
    }

    if ((null != subKeySpec) && !(subKeySpec instanceof List)) {
      throw new ExecutionException(
          "Keyspec '" + keyspec + "' is invalid: subordinate spec must be a list or NIL");
    }
    final Map<Object, FieldsMap.Op> subfmap = (null == subKeySpec)
        ? null
        : mkFmap(subKeySpec, bt);
    
    if (null == dstSpec) {
      if (null == subfmap) {
        throw new ExecutionException("Keyspec '" + keyspec + "' is invalid: "
                                     + "destination spec cannot be null");
      }
      final FieldsMap.Op localOp = mkOp(srcSpec);
      for (Map.Entry<Object, FieldsMap.Op> entry : subfmap.entrySet()) {
        final Object eKey = entry.getKey();
        final FieldsMap.Op eOp = entry.getValue();
        final FieldsMap.Op op = new FieldsMap.Op() {
          @Override
          public Object get(Map<?, ?> src) {
            Object mapObj = localOp.get(src);
            if (mapObj instanceof Map) {
              return eOp.get((Map<?,?>)mapObj);
            } else {
              return null;
            }
          }
        };
        fmap.put(eKey, op);
      }
      //
    } else {
      final Object key = Utils.asString(dstSpec);
      FieldsMap.Op op;
      if (null != subfmap) {
        final FieldsMap.Op localOp = mkOp(srcSpec);
        op = new FieldsMap.Op() {
          @Override
          public Object get(Map<?, ?> src) {
            Object mapObj = localOp.get(src);
            if (mapObj instanceof Map) {
              return mkFields((Map<Object,Object>) mapObj, subfmap);
            } else {
              return new HashMap<Object,Object>();
            }
          }
        };
      } else {
        op = mkOp(srcSpec);
      }
      fmap.put(key, op);
    }
  }

  protected static Map<Object, FieldsMap.Op> mkFmap(Object ksObj, Backtrace backtrace) {
    if (null == ksObj) {
      throw new ExecutionException(backtrace, "Keyseq must be an indexed sequence, but got NIL");
    }
    if (!Seq.isIndexed(ksObj)) {
      throw new ExecutionException(
          backtrace, "Keyseq must be an indexed sequence, but got " + ksObj.getClass());
    }
    final Map<Object, FieldsMap.Op> fmap = new HashMap<Object, FieldsMap.Op>();
    Seq.forEach(
        ksObj,
        new Seq.Operation() {
          @Override
          public boolean perform(Object keySpec) {
            addKsOp(fmap, keySpec, backtrace);
            return false;
          }
        },
        true);
    return fmap;
  }

  @Arguments(spec = {"src", "fieldspecs"})
  @Docstring(lines = {
      "Return a new object with those fields in 'src', which were specified in 'fieldspecs'. ",
      "",
      "  When 'src' is a map or a Java Bean the function will return new map with the specified",
      "fields from the source object.",
      "",
      "  When 'src' is a sequence the above operation will be performed on every object of the ",
      "sequence.",
      "",
      "  When 'src' is a NIL, return NIL.",
      "",
      "Format of 'fieldspecs': list of one or mod field specifications:",
      
      ""
    })
  @Package(name = Package.DWIM)
  @SuppressWarnings("unchecked")
  public static class DWIM_FIELDS extends FuncExp {
    @Override
    public Object evalWithArgs(final Backtrace backtrace, Eargs eargs) {
      final Object obj = eargs.get(0, backtrace);
      final Object ksObj = eargs.get(1, backtrace);
      
      Map<Object, FieldsMap.Op> fmap = mkFmap(ksObj,  backtrace);

      if (null == obj) {
        return null;
      } else if (Seq.isCollection(obj)
                 && !(obj instanceof Map)
                 && !(obj instanceof CharSequence)) {
        final List<Object> result = Utils.list();
        Seq.forEach(
            obj,
            new Seq.Operation() {
              @Override
              public boolean perform(Object item) {
                if (null == item) {
                  result.add(null);
                } else if (item instanceof Map) {
                  result.add(mkFields((Map<Object,Object>)item, fmap));
                } else {
                  result.add(mkFields(new Funcs.BeanMap(item), fmap));
                }
                return false;
              }
            },
            true);
        return result;
      } else if (obj instanceof Map) {
        return mkFields((Map<Object,Object>)obj, fmap);
      } else {
        return mkFields(new Funcs.BeanMap(obj), fmap);
      }
    }
  }
}
