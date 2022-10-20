package io.opsit.explang;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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

  /** Check if object is a supported collection. */
  public static boolean isCollection(Object obj) {
    return (null != obj)
        && ((obj instanceof Map)
            || (obj instanceof Iterable)
            || (obj instanceof CharSequence)
            || (obj instanceof Enumeration)
            || (obj.getClass().isArray()));
  }

  /** Check if object is a supported sequence. */
  public static boolean isSeq(Object obj) {
    return (null != obj)
      &&  ((obj instanceof Iterable)
           || (obj instanceof CharSequence)
           || (obj instanceof Enumeration)
           || (obj.getClass().isArray()));
  }


  /** Check if object is an indexed sequence. */
  public static boolean isIndexed(Object obj) {
    return (null != obj)
      && ((obj instanceof List)  
          || (obj instanceof CharSequence)
          || (obj.getClass().isArray()));
  }

  /** Check if object is a set. */
  public static boolean isSet(Object obj) {
    return obj instanceof Set;

  }

  /** Check if object is a set. */
  public static boolean isMap(Object obj) {
    return obj instanceof Map;

  }

  /** Check if object supports associative addressing (indices or keys). */
  public static boolean isAssociative(Object obj) {
    return isMap(obj) || isIndexed(obj);
  }

  
  /**
   * Run operation op on all objects in sequence.
   *
   * <p>If allowNonSeq is true then run operation on object that is not a sequence.
   */
  public static int forEach(Object seq, Operation op, boolean allowNonSeq) {
    if (seq instanceof Map) {
      seq = ((Map<?, ?>) seq).entrySet();
    }
    int cnt = 0;
    if (null == seq) {
      if (allowNonSeq) {
        op.perform(seq);
        cnt++;
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
        cnt++;
      }
    } else if (seq instanceof CharSequence) {
      final CharSequence cs = ((CharSequence) seq);
      final int numChars = cs.length();
      for (int j = 0; j < numChars; j++) {
        if (op.perform(cs.charAt(j))) {
          break;
        }
        cnt++;
      }
    } else if (seq.getClass().isArray()) {
      final int len = Array.getLength(seq);
      for (int j = 0; j < len; j++) {
        if (op.perform(Array.get(seq, j))) {
          break;
        }
        cnt++;
      }
    } else if (seq instanceof Enumeration) {
      final Enumeration<?> en = (Enumeration<?>) seq;
      while (en.hasMoreElements()) {
        if (op.perform(en.nextElement())) {
          break;
        }
        cnt++;
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
        cnt++;
      }
    } else {
      if (allowNonSeq) {
        op.perform(seq);
        cnt++;
      } else {
        throw new RuntimeException("Do not know how to iterate sequence of type " + seq.getClass());
      }
    }
    return cnt;
  }


  /**
   * Check if an associative collection contains key.
   */
  public static boolean containsKey(Object seq, Object key) {
    if (isMap(seq)) {
      return ((Map<?,?>) seq).containsKey(key);
    } else if (null == seq) {
      return false;
    } else if (isIndexed(seq)) {
      final int idx = Utils.asNumber(key).intValue();
      return idx >= 0 && idx < getLength(seq, false);
    } else if (isSet(seq)) {
      return ((Set<?>) seq).contains(key);
    } else {
      return false;
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
   * Get sequence element by key.
   */
  public static Object getElementByKeyOrIndex(Object seq, Object key) {
    if (null == seq) {
      return null;
    } else if (seq instanceof Map) {
      return ((Map) seq).get(key);
    } else {
      return getElementByIndex(seq, Utils.asNumber(key).intValue());
    }
  }

  /**
   * Remove element from  sequence  by key or index.
   */
  public static Object removeElementByKeyOrIndex(Object seq, Object key) {
    if (null == seq) {
      return null;
    } else if (seq instanceof Map) {
      return ((Map<?,?>) seq).remove(key);
    } else if (seq instanceof Set) {
      boolean removed =  ((Set<?>) seq).remove(key);
      return removed ? key : null;
    } else {
      return removeElementByIndex(seq, Utils.asNumber(key).intValue());
    }
  }

  protected static interface SeqAdapter {
    Object set(Object seq, int idx, Object element) throws IndexOutOfBoundsException;

    Object get(Object seq, int idx) throws IndexOutOfBoundsException;

    Object insert(Object seq, int idx, Object element) throws IndexOutOfBoundsException;

    Object remove(Object seq, int idx) throws IndexOutOfBoundsException;

    Object shallowClone(Object seq);
  }

  @SuppressWarnings("unchecked")
  protected static final SeqAdapter listAdapter = new SeqAdapter() {
    public Object set(Object seq, int idx, Object element) throws IndexOutOfBoundsException {
      final List<Object> lst = (List<Object>)seq;
      try {
        return lst.set(idx, element);
      } catch (IndexOutOfBoundsException ex) {
        if (idx == lst.size()) {
          lst.add(element);
          return null;
        } else {
          throw ex;
        }
      }
    }

    public Object insert(Object seq, int idx, Object element) throws IndexOutOfBoundsException {
      final List<Object> lst = (List<Object>)seq;
      lst.add(idx, element);
      return lst;
    }

    public Object remove(Object seq, int idx) throws IndexOutOfBoundsException {
      final List<Object> lst = (List<Object>)seq;
      return lst.remove(idx);
    }
      
    public Object get(Object seq, int idx) throws IndexOutOfBoundsException {
      return ((List) seq).get(idx);
    }
      
    public Object shallowClone(Object seq) {
      Class<?> clz = seq.getClass();
      try {
        Constructor<?> constr = clz.getConstructor();
        if (null == constr) {
          throw new RuntimeException("Cannot clone object " + clz + ": constructor not found");
        }
        List<Object> lst = (List<Object>)constr.newInstance();
        lst.addAll((List<Object>)seq);
        return lst;
      } catch (InstantiationException ex) {
        throw new RuntimeException(ex);
      } catch (InvocationTargetException ex) {
        throw new RuntimeException(ex);
      } catch (IllegalAccessException ex) {
        throw new RuntimeException(ex);
      } catch (NoSuchMethodException ex) {
        throw new RuntimeException(ex);
      }

    }
  };

  @SuppressWarnings("unchecked")
  protected static final SeqAdapter setAdapter =
      new SeqAdapter() {
        public Object set(Object seq, int idx, Object element) throws IndexOutOfBoundsException {
          throw new RuntimeException("Set by index not supported for Set objects");
        }

        public Object insert(Object seq, int idx, Object element) throws IndexOutOfBoundsException {
          throw new RuntimeException("Insert by index not supported for Set objects");
        }

        public Object get(Object seq, int idx) throws IndexOutOfBoundsException {
          throw new RuntimeException("Get by index not supported for Set objects");
        }

        public Object remove(Object seq, int idx) throws IndexOutOfBoundsException {
          final Set<Object> set = (Set<Object>) seq;
          return set.remove(idx);
        }

        public Object shallowClone(Object seq) {
          Class<?> clz = seq.getClass();
          try {
            Constructor<?> constr = clz.getConstructor();
            if (null == constr) {
              throw new RuntimeException("Cannot clone object " + clz + ": constructor not found");
            }
            Set<Object> set = (Set<Object>) constr.newInstance();
            set.addAll((List<Object>) seq);
            return set;
          } catch (InstantiationException ex) {
            throw new RuntimeException(ex);
          } catch (InvocationTargetException ex) {
            throw new RuntimeException(ex);
          } catch (IllegalAccessException ex) {
            throw new RuntimeException(ex);
          } catch (NoSuchMethodException ex) {
            throw new RuntimeException(ex);
          }
        }
      };

  protected static final SeqAdapter stringBufferAdapter =
      new SeqAdapter() {
        public Object set(Object seq, int idx, Object element) throws IndexOutOfBoundsException {
          final StringBuffer buf = (StringBuffer) seq;
          try {
            final Character result = buf.charAt(idx);
            buf.setCharAt(idx, Utils.asChar(element));
            return result;
          } catch (IndexOutOfBoundsException ex) {
            if (idx == buf.length()) {
              buf.append(Utils.asChar(element));
              return null;
            } else {
              throw ex;
            }
          }
        }

        public Object remove(Object seq, int idx) throws IndexOutOfBoundsException {
          final StringBuffer buf = (StringBuffer) seq;
          final Character chr = buf.charAt(idx);
          buf.deleteCharAt(idx);
          return chr;
        }

        public Object insert(Object seq, int idx, Object element) throws IndexOutOfBoundsException {
          final StringBuffer buf = (StringBuffer) seq;
          if (element instanceof CharSequence) {
            buf.insert(idx, (CharSequence) element);
          } else if (element instanceof Character) {
            buf.insert(idx, (Character) element);
          } else if (null == element) {
            buf.insert(idx, (Character) element);
          } else if (element.getClass().isArray()
              && element.getClass().getComponentType() == Character.TYPE) {
            buf.insert(idx, (char[]) element);
          } else if (element.getClass().isArray()
              && element.getClass().getComponentType() == Byte.TYPE) {
            buf.insert(idx, (byte[]) element);
          } else {
            buf.insert(idx, element);
          }
          return seq;
        }

        public Object get(Object seq, int idx) throws IndexOutOfBoundsException {
          return ((StringBuffer) seq).charAt(idx);
        }

        public Object shallowClone(Object seq) {
          return new StringBuffer((StringBuffer) seq);
        }
      };

  protected static final SeqAdapter stringBuilderAdapter =
      new SeqAdapter() {
        public Object set(Object seq, int idx, Object element) throws IndexOutOfBoundsException {
          final StringBuilder buf = (StringBuilder) seq;
          try {
            final Character result = buf.charAt(idx);
            buf.setCharAt(idx, Utils.asChar(element));
            return result;
          } catch (IndexOutOfBoundsException ex) {
            if (idx == buf.length()) {
              buf.append(Utils.asChar(element));
              return null;
            } else {
              throw ex;
            }
          }
        }

        public Object remove(Object seq, int idx) throws IndexOutOfBoundsException {
          final StringBuilder buf = (StringBuilder) seq;
          final Character chr = buf.charAt(idx);
          buf.deleteCharAt(idx);
          return chr;
        }

        public Object insert(Object seq, int idx, Object element) throws IndexOutOfBoundsException {
          final StringBuilder buf = (StringBuilder) seq;
          if (element instanceof CharSequence) {
            buf.insert(idx, (CharSequence) element);
          } else if (element instanceof Character) {
            buf.insert(idx, (Character) element);
          } else if (null == element) {
            buf.insert(idx, (Character) element);
          } else if (element.getClass().isArray()
              && element.getClass().getComponentType() == Character.TYPE) {
            buf.insert(idx, (char[]) element);
          } else if (element.getClass().isArray()
              && element.getClass().getComponentType() == Byte.TYPE) {
            buf.insert(idx, (byte[]) element);
          } else {
            buf.insert(idx, element);
          }
          return seq;
        }

        public Object get(Object seq, int idx) throws IndexOutOfBoundsException {
          return ((StringBuilder) seq).charAt(idx);
        }

        public Object shallowClone(Object seq) {
          return new StringBuilder((StringBuilder) seq);
        }
      };

  protected static final SeqAdapter charSequenceAdapter =
      new SeqAdapter() {
        public Object set(Object seq, int idx, Object element) {
          throw new RuntimeException("Cannot modify object of type " + seq.getClass());
        }

        public Object remove(Object seq, int idx) throws IndexOutOfBoundsException {
          throw new RuntimeException("Cannot remove element from object of type " + seq.getClass());
        }

        public Object insert(Object seq, int idx, Object element) {
          throw new RuntimeException("Cannot insert into  object of type " + seq.getClass());
        }

        public Object get(Object seq, int idx) throws IndexOutOfBoundsException {
          return ((CharSequence) seq).charAt(idx);
        }

        public Object shallowClone(Object seq) {
          throw new RuntimeException("Cannot shallow clone object of type " + seq.getClass());
        }
      };

  protected static final SeqAdapter arrayAdapter =
      new SeqAdapter() {
        public Object set(Object seq, int idx, Object element) throws IndexOutOfBoundsException {
          final Object result = Array.get(seq, idx);
          Utils.aset(seq, idx, element);
          return result;
        }

        public Object insert(Object seq, int idx, Object element) {
          throw new RuntimeException("Cannot insert into object of type " + seq.getClass());
        }

        public Object get(Object seq, int idx) throws IndexOutOfBoundsException {
          return Array.get(seq, idx);
        }

        public Object remove(Object seq, int idx) throws IndexOutOfBoundsException {
          throw new RuntimeException("Cannot remove element from object of type " + seq.getClass());
        }

        public Object shallowClone(Object seq) {
          final int length = Array.getLength(seq);
          final Object copy = Array.newInstance(seq.getClass().getComponentType(), length);
          for (int i = 0; i < length; i++) {
            Array.set(copy, i, Array.get(seq, i));
          }
          return copy;
        }
      };

  @SuppressWarnings("unchecked")
  protected static final SeqAdapter mapAdapter =
      new SeqAdapter() {
        public Object set(Object seq, int idx, Object element) {
          return ((Map<Object, Object>) seq).put(idx, element);
        }

        public Object insert(Object seq, int idx, Object element) {
          return ((Map<Object, Object>) seq).put(idx, element);
        }

        public Object remove(Object seq, int idx) throws IndexOutOfBoundsException {
          return ((Map<Object, Object>) seq).remove(idx);
        }

        public Object get(Object seq, int idx) {
          return ((Map<Object, Object>) seq).get(idx);
        }

        public Object shallowClone(Object seq) {
          Class<?> clz = seq.getClass();
          try {
            Constructor<?> constr = clz.getConstructor(Map.class);
            if (null == constr) {
              throw new RuntimeException("Cannot clone object " + clz + ": constructor not found");
            }
            return constr.newInstance((Map) seq);
          } catch (InstantiationException ex) {
            throw new RuntimeException(ex);
          } catch (InvocationTargetException ex) {
            throw new RuntimeException(ex);
          } catch (IllegalAccessException ex) {
            throw new RuntimeException(ex);
          } catch (NoSuchMethodException ex) {
            throw new RuntimeException(ex);
          }
        }
      };

  protected static final SeqAdapter nullAdapter =
      new SeqAdapter() {
        public Object set(Object seq, int idx, Object element) {
          return null;
        }

        public Object insert(Object seq, int idx, Object element) {
          return null;
        }

        public Object remove(Object seq, int idx) throws IndexOutOfBoundsException {
          return null;
        }

        public Object get(Object seq, int idx) {
          return null;
        }

        public Object shallowClone(Object seq) {
          return null;
        }
      };

  protected static SeqAdapter getSeqAdapter(Object seq) {
    if (null == seq) {
      return nullAdapter;
    } else if (seq instanceof List) {
      return listAdapter;
    } else if (seq instanceof Map) {
      return mapAdapter;
    } else if (seq instanceof Set) {
      return setAdapter;
    } else if (seq.getClass().isArray()) {
      return arrayAdapter;
    } else if (seq instanceof StringBuffer) {
      return stringBufferAdapter;
    } else if (seq instanceof StringBuilder) {
      return stringBuilderAdapter;
    } else if (seq instanceof CharSequence) {
      return charSequenceAdapter;
    } else {
      throw new RuntimeException("Unupported sequence type " + seq.getClass().getName());
    }
  }
  
  protected static SeqAdapter getAssociativeSeqAdapter(Object seq) {
    if (null == seq) {
      return nullAdapter;
    } else if (seq instanceof List) {
      return listAdapter;
    } else if (seq instanceof Map) {
      return mapAdapter;
    } else if (seq.getClass().isArray()) {
      return arrayAdapter;
    } else if (seq instanceof StringBuffer) {
      return stringBufferAdapter;
    } else if (seq instanceof StringBuilder) {
      return stringBuilderAdapter;
    } else if (seq instanceof CharSequence) {
      return charSequenceAdapter;
    } else {
      throw new RuntimeException("Unupported sequence type " + seq.getClass().getName());
    }
  }

  public static Object shallowClone(Object seq) {
    SeqAdapter adapter = getSeqAdapter(seq);
    return adapter.shallowClone(seq);
  }

  
  /**
   * Get sequence element by index. Ignore index errors.
   */
  public static Object getElementByIndex(Object seq, int index) {
    try {
      return refElementByIndex(seq, index);
    } catch (IndexOutOfBoundsException ex) {
      return null;
    }
  }

    
  /**
   * Get sequence element by index. Throws IndexOutOfBounds exception
   * if index is invalid.
   */
  public static Object refElementByIndex(Object seq, int index) {
    SeqAdapter adapter = getAssociativeSeqAdapter(seq);
    return adapter.get(seq, index);
  }
  
  /**
  * Get sequence element by index.
  */
  public static Object removeElementByIndex(Object seq, int index) {
    SeqAdapter adapter = getAssociativeSeqAdapter(seq);
    try {
      return adapter.remove(seq, index);
    } catch (IndexOutOfBoundsException ex) {
      return null;
    }
  }
  
  /**
   * put sequence element by key. Return old value at this index.
   */
  @SuppressWarnings("unchecked")
  public static Object putElement(Object seq, Object key, Object element) {
    if (seq instanceof Map) {
      return ((Map)seq).put(key, element);
    } else {
      try {
        return setElementByIndex(seq, Utils.asNumber(key).intValue(), element);
      } catch (IndexOutOfBoundsException ex) {
        return null;
      }
    }
  }


  /**
   * put sequence element by index. Return old value at this index.
   */
  public static Object setElementByIndex(Object seq, int index, Object element)
      throws IndexOutOfBoundsException {
    SeqAdapter adapter = getAssociativeSeqAdapter(seq);
    return adapter.set(seq, index, element);
  }

  /**
   * put sequence element by index. Return old value at this index.
   */
  public static Object insertElementByIndex(Object seq, int index, Object element)
      throws IndexOutOfBoundsException {
    SeqAdapter adapter = getAssociativeSeqAdapter(seq);
    return adapter.insert(seq, index, element);
  }

  /**
   * put sequence element by key. Return old value at this index.
   */
  @SuppressWarnings("unchecked")
  public static Object setElement(Object seq, Object key, Object element)
      throws IndexOutOfBoundsException {
    if (seq instanceof Map) {
      return ((Map)seq).put(key, element);
    } else {
      return setElementByIndex(seq, Utils.asNumber(key).intValue(), element);
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
        args[j] = getElementByIndex(seqs[j], i);
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
    } else if (Seq.isCollection(o1) && Seq.isCollection(o2)) {
      int l1 = Seq.getLength(o1, false);
      int l2 = Seq.getLength(o2, false);
      if (l1 != l2) {
        return false;
      }
      for (int i = 0; i < l1; i++) {
        final Object el1 = getElementByIndex(o1, i);
        final Object el2 = getElementByIndex(o2, i);
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

  /**
   * Get data object from nested data structures.
   */
  @SuppressWarnings("unchecked")
  public static boolean doGet(final Object obj, final Object[] result, final Object keyObj) {
    result[0] = null;
    int idx = -1;
    if (obj instanceof Map) {
      final Map<Object, Object> map = (Map<Object, Object>) obj;
      result[0] = map.get(keyObj);
      if (null == result[0]) {
        if (!map.containsKey(keyObj)) {
          return false;
        }
      }
    } else if (obj instanceof Set) {
      final Set<Object> s = (Set<Object>) obj;
      if (!s.contains(keyObj)) {
        return false;
      }
      result[0] = keyObj;
    } else if (obj == null) {
      return false;
      
    } else if ((obj instanceof List) && (idx = getIntIdx(keyObj)) >= 0) {
      final List<Object> lst = (List<Object>) obj;
      try {
        result[0] = lst.get(idx);
      } catch (IndexOutOfBoundsException ex) {
        return false;
      }
    } else if ((obj instanceof CharSequence) && (idx = getIntIdx(keyObj)) >= 0) {
      final CharSequence s = (CharSequence) obj;
      try {
        result[0] = s.charAt(idx);
      } catch (IndexOutOfBoundsException ex) {
        return false;
      }
    } else if (obj.getClass().isArray() && (idx = getIntIdx(keyObj)) >= 0) {
      try {
        result[0] = Array.get(obj, idx);
      } catch (ArrayIndexOutOfBoundsException ex) {
        return false;
      }

    } else if (java.util.Map.Entry.class.isAssignableFrom(obj.getClass())) {
      if ("key".equalsIgnoreCase(Utils.asStringOrNull(keyObj))) {
        result[0] = ((Map.Entry<?,?>) obj).getKey();
      } else if ("value".equalsIgnoreCase(Utils.asStringOrNull(keyObj))) {
        result[0] = ((Map.Entry<?,?>) obj).getValue();
      } else {
        return false;
      }
    } else {
      final String keyStr = Utils.asStringOrEmpty(keyObj).trim();
      if (keyStr.length() == 0) {
        return false;
      }
      // FIXME: better check for method name?
      try {
        final String getName =
            Utils.concat("get", keyStr.substring(0, 1).toUpperCase(), keyStr.substring(1));
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

  /**
   * Get data object from nested data structures.
   */
  public static Object doGetIn(
      final Object obj,
      final Object ksObj,
      final int ksIdx,
      final Object notDefined) {
    Object[] result = new Object[1];
    final Object key = getKeyByIndex(ksObj, ksIdx);
    if (null == key) {
      return obj;
    }
    if (doGet(obj, result, key)) {
      return doGetIn(result[0], ksObj, ksIdx + 1, notDefined);
    }
    return notDefined;
  }

  
  // FIXME: produce specific retriever before starting the iterations
  // no need to check object type ons each level
  // FIXME: allow use other sequences, including lazy ones
  
  protected static Object getKeyByIndex(Object ksObj, int ksIdx) {
    if (ksObj instanceof List) {
      if (ksIdx < 0 || ksIdx >= ((List<?>) ksObj).size()) {
        return null;
      }
      return ((List<?>) ksObj).get(ksIdx);
    }
    if (null == ksObj) {
      return null;
    }
    if (ksObj.getClass().isArray()) {
      if (ksIdx < 0 || ksIdx >= Array.getLength(ksObj)) {
        return null;
      }
      return Array.get(ksObj, ksIdx);
    }
    throw new ExecutionException(
        "Cannot use the provided " + ksObj.getClass() + " value as list of indexes");
  }

  protected static int getIntIdx(Object key) {
    try {
      return Utils.asNumber(key).intValue();
    } catch (Exception ex) {
      return -1;
    }
  }

  /**
   * Join sequence with string separator.
   */
  public static String joinWithString(Object seqObj, String sep) {
    if (isCollection(seqObj)) {
      final StringBuilder buf = new StringBuilder();
      forEach(seqObj, new Operation() {
          @Override
          public boolean perform(Object obj) {
              if (!buf.isEmpty()) {
                buf.append(sep);
              }
              buf.append(Utils.asStringOrEmpty(obj));
              return false;
            }
          },
          false);
      return buf.toString();
    } else if (null == seqObj) {
      return null;
    } else {
      throw new RuntimeException("join by string separator not implemented for sequence of type "
                                 + seqObj.getClass());
    }
  }
}
