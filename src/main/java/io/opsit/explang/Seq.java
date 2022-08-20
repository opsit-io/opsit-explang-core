package io.opsit.explang;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Utility class for operations on sequences of various types. */
public class Seq {

  public static interface Operation {
    public boolean perform(Object obj);
  }

  /** Check if object is a supported sequence. */
  public static boolean isSequence(Object obj) {
    return (null != obj)
        && ((obj instanceof Map)
            || (obj instanceof Iterable)
            || (obj instanceof CharSequence)
            || (obj instanceof Enumeration)
            || (obj.getClass().isArray()));
  }

  /**
   * Run operation op on all objects in sequence.
   *
   * <p>If allowNonSeq is true then run operation on object that is not a sequence.
   */
  public static void forEach(Object seq, Operation op, boolean allowNonSeq) {
    if (seq instanceof Map) {
      seq = ((Map<?, ?>) seq).entrySet();
    }
    if (null == seq) {
      if (allowNonSeq) {
        op.perform(seq);
      } else {
        throw new RuntimeException("NIL is not a supported sequence.");
      }
    } else if (seq instanceof Iterable) {
      final Iterator<?> iter = ((Iterable<?>) seq).iterator();
      // boolean brk = false;;
      while (iter.hasNext()) {
        if (op.perform(iter.next())) {
          break;
        }
      }
    } else if (seq instanceof CharSequence) {
      final CharSequence cs = ((CharSequence) seq);
      final int numChars = cs.length();
      for (int j = 0; j < numChars; j++) {
        if (op.perform(cs.charAt(j))) {
          break;
        }
      }
    } else if (seq.getClass().isArray()) {
      final int len = Array.getLength(seq);
      for (int j = 0; j < len; j++) {
        if (op.perform(Array.get(seq, j))) {
          break;
        }
      }
    } else if (seq instanceof Enumeration) {
      final Enumeration<?> en = (Enumeration<?>) seq;
      while (en.hasMoreElements()) {
        if (op.perform(en.nextElement())) {
          break;
        }
      }
    } else if (seq instanceof Map) {
      // FIXME: dead code? Why?
      final Map<?, ?> map = (Map<?, ?>) seq;
      Iterable<?> col = map.values();
      Iterator<?> iter = col.iterator();
      while (iter.hasNext()) {
        if (op.perform(iter.next())) {
          break;
        }
      }
    } else {
      if (allowNonSeq) {
        op.perform(seq);
      } else {
        throw new RuntimeException("Do not know how to iterate sequence of type " + seq.getClass());
      }
    }
  }

  /**
   * Check if sequence contains object.
   */
  public static boolean containsElement(Object seq, Object obj) {
    if (null == seq) {
      return false;
    } else if (seq instanceof Collection) {
      return ((Collection<?>) seq).contains(obj);
    } else if (seq.getClass().isArray()) {
      final int len = Array.getLength(seq);
      for (int j = 0; j < len; j++) {
        Object elt = Array.get(seq, j);
        if (null == obj) {
          if (null == elt) {
            return true;
          }
        } else {
          return elt.equals(obj);
        }
      }
      return false;
    } else if (seq instanceof CharSequence) {
      if (!(obj instanceof Character)) {
        return false;
      }
      final CharSequence cs = ((CharSequence) seq);
      final Character chr = (Character) obj;
      final int len = cs.length();
      for (int i = 0; i < len; i++) {
        if (cs.charAt(i) == chr) {
          return true;
        }
      }
      return false;
    } else if (seq instanceof Map) {
      return ((Map<?, ?>) seq).containsValue(obj);
    } else {
      return false;
    }
  }

  /**
   * Get sequence element by index.
   */
  public static Object getElement(Object seq, int index) {
    if (null == seq) {
      return null;
    } else if (seq instanceof List) {
      try {
        final List<?> list = (List<?>) seq;
        return list.get(index);
      } catch (IndexOutOfBoundsException bex) {
        return null;
      }
    } else if (seq.getClass().isArray()) {
      try {
        return Array.get(seq, index);
      } catch (ArrayIndexOutOfBoundsException bex) {
        return null;
      }
    } else if (seq instanceof CharSequence) {
      try {
        final CharSequence cs = (CharSequence) seq;
        return cs.charAt(index);
      } catch (IndexOutOfBoundsException bex) {
        return null;
      }
    } else {
      throw new RuntimeException("Unupported sequence type " + seq.getClass().getName());
    }
  }

  /**
   * put sequence element by index. Return old value at this index.
   */
  @SuppressWarnings("unchecked")
  public static Object putElement(Object seq, int index, Object element) {
    if (null == seq) {
      return null;
    } else if (seq instanceof List) {
      try {
        final List<Object> list = (List<Object>) seq;
        return list.set(index, element);
      } catch (IndexOutOfBoundsException bex) {
        return null;
      }
    } else if (seq.getClass().isArray()) {
      try {
        Object result = Array.get(seq, index);
        Utils.aset(seq, index, element);
        return result;
      } catch (ArrayIndexOutOfBoundsException bex) {
        return null;
      }
    } else if (seq instanceof StringBuffer) {
      try {
        final StringBuffer buf = (StringBuffer) seq;
        final Character result = buf.charAt(index);
        buf.setCharAt(index, Utils.asChar(element));
        return result;
      } catch (IndexOutOfBoundsException bex) {
        return null;
      }
    } else if (seq instanceof StringBuilder) {
      try {
        final StringBuffer buf = (StringBuffer) seq;
        final Character result = buf.charAt(index);
        buf.setCharAt(index, Utils.asChar(element));
        return result;
      } catch (IndexOutOfBoundsException bex) {
        return null;
      }
    } else {
      throw new RuntimeException("Unupported sequence type " + seq.getClass().getName());
    }
  }


  /**
   * Get length of sequence.
   */
  public static int getLength(Object val, boolean allowNonSeq) {
    if (null == val) {
      if (allowNonSeq) {
        return 0;
      } else {
        throw new RuntimeException("NIL not a sequence: ");
      }
    } else if (val instanceof Collection) {
      return ((Collection<?>) val).size();
    } else if (val instanceof CharSequence) {
      return ((CharSequence) val).length();
    } else if (val.getClass().isArray()) {
      return Array.getLength(val);
    } else if (val instanceof Map) {
      return ((Map<?, ?>) val).size();
    } else {
      if (allowNonSeq) {
        return 1;
      } else {
        throw new RuntimeException("Given object type is not a sequence: " + val.getClass());
      }
    }
  }

  /**
   * Return sequence objects as Set.
   */
  public static Set<?> asSet(Object obj) {
    final Set<Object> result = new HashSet<Object>();
    if (null != obj) {
      forEach(
          obj,
          new Operation() {
            @Override
            public boolean perform(Object obj) {
              result.add(obj);
              return false;
            }
          },
          false);
    }
    return result;
  }

  /**
   * Return new list with all elements of given sequence.
   */
  public static List<Object> valuesList(Object seq) {
    final List<Object> result = Utils.list();
    if (null != seq) {
      forEach(
          seq,
          new Operation() {
            @Override
            public boolean perform(Object obj) {
              result.add(obj);
              return false;
            }
          },
          false);
    }
    return result;
  }

  /**
   * Get maximal length of given sequences.
   */
  public static int maxLength(Object... seqs) {
    int result = 0;
    for (int i = 0; i < seqs.length; i++) {
      int len = getLength(seqs[i], false);
      if (len > result) {
        result = len;
      }
    }
    return result;
  }

  /**
   * Get minimal length of given sequences.
   */
  public static int minLength(Object... seqs) {
    if (seqs.length > 0) {
      int result = Integer.MAX_VALUE;
      for (int i = 0; i < seqs.length; i++) {
        int len = getLength(seqs[i], false);
        if (len < result) {
          result = len;
        }
      }
      return result;
    } else {
      return 0;
    }
  }

  public static interface Multiop {
    public Object perform(Object... objs);
  }

  /**
   * Perform operation on all objects of the given sequences.
   */
  public static List<Object> mapall(Multiop op, Object... seqs) {
    int maxlen = maxLength(seqs);
    List<Object> result = new ArrayList<Object>(maxlen);
    Object[] args = new Object[seqs.length];
    for (int i = 0; i < maxlen; i++) {
      for (int j = 0; j < seqs.length; j++) {
        args[j] = getElement(seqs[j], i);
      }
      result.add(i, op.perform(args));
    }
    return result;
  }

  /**
   * Compare maps contents.
   */
  public static boolean sequal(Map<?, ?> m1, Map<?, ?> m2) {
    // not sure that this is right
    // probably need to check keys according to the same rules?
    // but it could get messy
    // unless we use same equality rules for getting keys from maps
    final Set<?> k1 = m1.keySet();
    final Set<?> k2 = m2.keySet();
    if (!k1.equals(k2)) {
      return false;
    }
    for (Object k : m1.keySet()) {
      final Object el1 = m1.get(k);
      final Object el2 = m2.get(k);
      if (sequal(el1, el2)) {
        continue;
      }
      return false;
    }
    return true;
  }

  // FIXME: identify circular refs?
  /**
   * Compare objects, for sequences compare sequence contents.
   */
  public static boolean sequal(Object o1, Object o2) {
    if (Utils.objequal(o1, o2)) {
      return true;
    } else if ((o1 instanceof Map) && (o2 instanceof Map)) {
      return sequal((Map<?, ?>) o1, (Map<?, ?>) o2);
    } else if ((o1 instanceof Set) && (o2 instanceof Set)) {
      return o1.equals(o2);
    } else if (Seq.isSequence(o1) && Seq.isSequence(o2)) {
      int l1 = Seq.getLength(o1, false);
      int l2 = Seq.getLength(o2, false);
      if (l1 != l2) {
        return false;
      }
      for (int i = 0; i < l1; i++) {
        final Object el1 = getElement(o1, i);
        final Object el2 = getElement(o2, i);
        // compare non-sequence objects
        // if both of them is sequnces and x.equals(y) == true
        //  it's ok as well.
        if (sequal(el1, el2)) {
          continue;
        }
        return false;
      }
      return true;
    } else {
      return false;
    }
  }
}
