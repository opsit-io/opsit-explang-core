package io.opsit.explang;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
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
   * Remove last element of indexed sequence or arbitrary
   * from a set. Returns null if no elements left.
   */
  @SuppressWarnings("unchecked")
  public static Object removeLastElement(Object seq) {
    if (null == seq) {
      return null;
    } else if (seq instanceof Set) {
      // FIXME: atomic operation
      // Ugly, move into adapters
      final Set<Object> set = (Set<Object>)seq;
      if (set.isEmpty()) {
        return null;
      }
      final SeqAdapter adapter = getSeqAdapter(seq);
      final Iterator<Object> iter = set.iterator();
      if (iter.hasNext()) {
        final Object value = iter.next();
        return  Utils.asBoolean(adapter.removeValue(seq, value)) ? value : null;
      } else {
        return null;
      }
    }
    int index = getLength(seq, false) - 1;
    if (index >= 0) {
      // FIXME: must be atomic
      final Object result = removeElementByKeyOrIndex(seq, index);
      return result;
    } else {
      return null;
    }
  }

  /**
   * Non-mutating remove last element.
   */
  @SuppressWarnings("unchecked")
  public static List<Object> roRemoveLastElement(Object seq) {
    Object newSeq = null;
    Object obj = null;
    if (seq instanceof Set) {
      // FIXME: atomic operation
      // Ugly, move into adapters
      final Set<Object> set = (Set<Object>)seq;
      if (set.isEmpty()) {
        return null;
      }
      final SeqAdapter adapter = getSeqAdapter(seq);
      final Iterator<Object> iter = set.iterator();
      if (iter.hasNext()) {
        obj = iter.next();
        newSeq = adapter.roRemoveValue(seq, obj);
      } else {
        newSeq = adapter.shallowClone(seq);
      }
    } else {
      // FIXME: must be atomic
      final SeqAdapter adapter = getSeqAdapter(seq);
      int index = getLength(seq, false) - 1;
      obj = adapter.get(seq, index);
      newSeq = roRemoveElementByKeyOrIndex(seq, index);
    }
    return Utils.list(obj, newSeq);
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

  /**
   * Remove element from  sequence  by key or index.
   */
  public static Object roRemoveElementByKeyOrIndex(Object seq, Object key) {
    SeqAdapter adapter = getSeqAdapter(seq);
    try {
      return adapter.roRemoveByKeyOrIndex(seq, key);
    } catch (IndexOutOfBoundsException ex) {
      return adapter.shallowClone(seq);
    }
  }

  /**
   * Add value to sequence.
   */
  public static Object addValue(Object seq, Object value) {
    SeqAdapter adapter = getSeqAdapter(seq);
    adapter.addValue(seq, value);
    return seq;
  }

  public static Object roAddValue(Object seq, Object value) {
    SeqAdapter adapter = getSeqAdapter(seq);
    return adapter.roAddValue(seq, value);
  }

  protected static interface SeqAdapter {
    Object addValue(Object seq, Object element);

    Object roAddValue(Object seq, Object element);

    Object roPutByKey(Object seq, Object key, Object element);

    Object putByKey(Object seq, Object key, Object element);

    Object set(Object seq, int idx, Object element) throws IndexOutOfBoundsException;

    Object roSet(Object seq, int idx, Object element) throws IndexOutOfBoundsException;

    Object get(Object seq, int idx) throws IndexOutOfBoundsException;

    Object insert(Object seq, int idx, Object element) throws IndexOutOfBoundsException;

    Object roInsert(Object seq, int idx, Object element) throws IndexOutOfBoundsException;

    Object removeByKeyOrIndex(Object seq, Object key) throws IndexOutOfBoundsException;

    Object removeValue(Object seq, Object value);

    Object roRemoveByKeyOrIndex(Object seq, Object keyidx) throws IndexOutOfBoundsException;

    Object roRemoveValue(Object seq, Object value);

    Object shallowClone(Object seq);
  }

  private static RuntimeException invalidOp(Object obj, String op) {
    return new RuntimeException(
        "Cannot do " + op + " object of type " + obj.getClass().getCanonicalName());
  }

  private static RuntimeException cannotChangeError(Object obj) {
    return new RuntimeException(
        "Cannot do mutating change for object of type " + obj.getClass().getCanonicalName());
  }

  static void checkNonMutatingChange(Object a, Object b) {
    if (a == b) {
      throw new RuntimeException(
          "Cannot do non-mutating change for object of type " + a.getClass().getCanonicalName());
    }
  }

  @SuppressWarnings("unchecked")
  protected static final SeqAdapter listAdapter =
      new SeqAdapter() {
        public Object roAddValue(Object seq, Object element) {
          final List<Object> lst = (List<Object>) seq;
          final List<Object> newList = (List<Object>) shallowClone(lst);
          checkNonMutatingChange(newList, seq);
          this.addValue(newList, element);
          return newList;
        }

        public Object addValue(Object seq, Object element) {
          final List<Object> lst = (List<Object>) seq;
          lst.add(element);
          return lst;
        }

        public Object roPutByKey(Object seq, Object key, Object element) {
          throw invalidOp(seq, "non-mutating putByKey");
        }

        public Object putByKey(Object seq, Object key, Object element) {
          throw invalidOp(seq, "putByKey");
        }

        public Object set(Object seq, int idx, Object element) throws IndexOutOfBoundsException {
          final List<Object> lst = (List<Object>) seq;
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

        public Object roSet(Object seq, int idx, Object element) throws IndexOutOfBoundsException {
          final List<Object> lst = (List<Object>) seq;
          try {
            final List<Object> newList = (List<Object>) shallowClone(lst);
            checkNonMutatingChange(newList, seq);
            this.set(newList, idx, element);
            return newList;
          } catch (IndexOutOfBoundsException ex) {
            if (idx == lst.size()) {
              lst.add(element);
              return null;
            } else {
              throw ex;
            }
          }
        }

        public Object roInsert(Object seq, int idx, Object element)
            throws IndexOutOfBoundsException {
          final List<Object> newList = (List<Object>) shallowClone(seq);
          checkNonMutatingChange(newList, seq);
          this.insert(newList, idx, element);
          return newList;
        }

        public Object insert(Object seq, int idx, Object element) throws IndexOutOfBoundsException {
          final List<Object> lst = (List<Object>) seq;
          lst.add(idx, element);
          return lst;
        }

        public Object removeByKeyOrIndex(Object seq, Object keyidx)
            throws IndexOutOfBoundsException {
          final List<Object> lst = (List<Object>) seq;
          return lst.remove((int) Utils.asNumber(keyidx));
        }

        public Object removeValue(Object seq, Object value) throws IndexOutOfBoundsException {
          final List<Object> lst = (List<Object>) seq;
          return lst.remove(value);
        }

        public Object roRemoveByKeyOrIndex(Object seq, Object keyidx)
            throws IndexOutOfBoundsException {
          final List<Object> newList = (List<Object>) shallowClone(seq);
          checkNonMutatingChange(newList, seq);
          this.removeByKeyOrIndex(newList, keyidx);
          return newList;
        }

        public Object roRemoveValue(Object seq, Object value) throws IndexOutOfBoundsException {
          final List<Object> newList = (List<Object>) shallowClone(seq);
          checkNonMutatingChange(newList, seq);
          this.removeValue(newList, value);
          return newList;
        }

        public Object get(Object seq, int idx) throws IndexOutOfBoundsException {
          return ((List<Object>) seq).get(idx);
        }

        public Object shallowClone(Object seq) {
          Class<?> clz = seq.getClass();
          try {
            Constructor<?> constr = clz.getConstructor();
            if (null == constr) {
              throw new RuntimeException("Cannot clone object " + clz + ": constructor not found");
            }
            List<Object> lst = (List<Object>) constr.newInstance();
            lst.addAll((List<Object>) seq);
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
  protected static final SeqAdapter immutableListAdapter =
      new SeqAdapter() {
        public Object roAddValue(Object seq, Object element) {
          final List<Object> lst = (List<Object>) seq;
          List<Object> newList = new ArrayList<Object>(lst.size());
          newList.addAll(lst);
          listAdapter.addValue(newList, element);
          return Collections.unmodifiableList(newList);
        }

        public Object addValue(Object seq, Object element) {
          throw cannotChangeError(seq);
        }

        public Object roPutByKey(Object seq, Object key, Object element) {
          throw invalidOp(seq, "non-mutating putByKey");
        }

        public Object putByKey(Object seq, Object key, Object element) {
          throw invalidOp(seq, "putByKey");
        }

        public Object set(Object seq, int idx, Object element) throws IndexOutOfBoundsException {
          throw cannotChangeError(seq);
        }

        public Object roSet(Object seq, int idx, Object element) throws IndexOutOfBoundsException {
          // FIXME: fixed type of immutable list
          final List<Object> lst = (List<Object>) seq;
          try {
            List<Object> newList = new ArrayList<Object>(lst.size());
            newList.addAll(lst);
            listAdapter.set(newList, idx, element);
            return Collections.unmodifiableList(newList);
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
          throw cannotChangeError(seq);
        }

        public Object roInsert(Object seq, int idx, Object element)
            throws IndexOutOfBoundsException {
          throw new RuntimeException("NOTIMPLEMENTED");
        }

        public Object removeByKeyOrIndex(Object seq, Object keyidx)
            throws IndexOutOfBoundsException {
          throw cannotChangeError(seq);
        }

        public Object roRemoveByKeyOrIndex(Object seq, Object keyidx)
            throws IndexOutOfBoundsException {
          final List<Object> lst = (List<Object>) seq;
          final List<Object> newList = new ArrayList<Object>(lst.size());
          newList.addAll(lst);
          listAdapter.removeByKeyOrIndex(newList, keyidx);
          return Collections.unmodifiableList(newList);
        }

        public Object removeValue(Object seq, Object keyidx) throws IndexOutOfBoundsException {
          throw cannotChangeError(seq);
        }

        public Object roRemoveValue(Object seq, Object keyidx) throws IndexOutOfBoundsException {
          throw new RuntimeException("NOTIMPLEMENTED");
        }

        public Object get(Object seq, int idx) throws IndexOutOfBoundsException {
          return ((List) seq).get(idx);
        }

        public Object shallowClone(Object seq) {
          return seq;
        }
      };

  @SuppressWarnings("unchecked")
  protected static final SeqAdapter immutableSetAdapter =
      new SeqAdapter() {
        public Object roAddValue(Object seq, Object element) {
          final Set<Object> set = (Set<Object>) seq;
          final Set<Object> newSet = new HashSet<Object>();
          newSet.addAll(set);
          setAdapter.addValue(newSet, element);
          return Collections.unmodifiableSet(newSet);
        }

        public Object addValue(Object seq, Object element) {
          throw cannotChangeError(seq);
        }

        public Object roPutByKey(Object seq, Object key, Object element) {
          throw invalidOp(seq, "non-mutating putByKey");
        }

        public Object putByKey(Object seq, Object key, Object element) {
          throw invalidOp(seq, "putByKey");
        }

        public Object roSet(Object seq, int idx, Object element) throws IndexOutOfBoundsException {
          throw new RuntimeException("Set by index not supported for Set objects");
        }

        public Object set(Object seq, int idx, Object element) throws IndexOutOfBoundsException {
          throw new RuntimeException("Set by index not supported for Set objects");
        }

        public Object insert(Object seq, int idx, Object element) throws IndexOutOfBoundsException {
          throw new RuntimeException("Insert by index not supported for Set objects");
        }

        public Object roInsert(Object seq, int idx, Object element)
            throws IndexOutOfBoundsException {
          throw new RuntimeException("Insert by index not supported for Set objects");
        }

        public Object get(Object seq, int idx) throws IndexOutOfBoundsException {
          throw new RuntimeException("Get by index not supported for Set objects");
        }

        public Object removeByKeyOrIndex(Object seq, Object keyidx)
            throws IndexOutOfBoundsException {
          throw invalidOp(seq, "removeByKeyOrIndex");
        }

        public Object removeValue(Object seq, Object value) throws IndexOutOfBoundsException {
          throw invalidOp(seq, "removeByKeyOrIndex");
        }

        public Object roRemoveByKeyOrIndex(Object seq, Object keyidx)
            throws IndexOutOfBoundsException {
          return this.roRemoveValue(seq, keyidx);
        }

        public Object roRemoveValue(Object seq, Object value) throws IndexOutOfBoundsException {
          final Set<Object> set = (Set<Object>) seq;
          final Set<Object> newSet = new HashSet<Object>();
          newSet.addAll(set);
          newSet.remove(value);
          return Collections.unmodifiableSet(newSet);
        }

        public Object shallowClone(Object seq) {
          Class<?> clz = seq.getClass();
          try {
            Constructor<?> constr = clz.getConstructor();
            if (null == constr) {
              throw new RuntimeException("Cannot clone object " + clz + ": constructor not found");
            }
            Set<Object> set = (Set<Object>) constr.newInstance();
            set.addAll((Set) seq);
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

  @SuppressWarnings("unchecked")
  protected static final SeqAdapter setAdapter =
      new SeqAdapter() {
        public Object roAddValue(Object seq, Object element) {
          final Set<Object> set = (Set<Object>) seq;
          final Set<Object> newSet = (Set<Object>) this.shallowClone(seq);
          newSet.add(element);
          return newSet;
        }

        public Object addValue(Object seq, Object element) {
          ((Set<Object>) seq).add(element);
          return seq;
        }

        public Object roPutByKey(Object seq, Object key, Object element) {
          throw invalidOp(seq, "non-mutating putByKey");
        }

        public Object putByKey(Object seq, Object key, Object element) {
          throw invalidOp(seq, "putByKey");
        }

        public Object roSet(Object seq, int idx, Object element) throws IndexOutOfBoundsException {
          throw new RuntimeException("Set by index not supported for Set objects");
        }

        public Object set(Object seq, int idx, Object element) throws IndexOutOfBoundsException {
          throw new RuntimeException("Set by index not supported for Set objects");
        }

        public Object insert(Object seq, int idx, Object element) throws IndexOutOfBoundsException {
          throw new RuntimeException("Insert by index not supported for Set objects");
        }

        public Object roInsert(Object seq, int idx, Object element)
            throws IndexOutOfBoundsException {
          throw new RuntimeException("Insert by index not supported for Set objects");
        }

        public Object get(Object seq, int idx) throws IndexOutOfBoundsException {
          throw new RuntimeException("Get by index not supported for Set objects");
        }

        public Object removeByKeyOrIndex(Object seq, Object keyidx)
            throws IndexOutOfBoundsException {
          throw invalidOp(seq, "removeByKeyOrIndex");
        }

        public Object removeValue(Object seq, Object value) throws IndexOutOfBoundsException {
          final Set<Object> set = (Set<Object>) seq;
          return set.remove(value);
        }

        public Object roRemoveByKeyOrIndex(Object seq, Object keyidx)
            throws IndexOutOfBoundsException {
          return this.roRemoveValue(seq, keyidx);
        }

        public Object roRemoveValue(Object seq, Object value) throws IndexOutOfBoundsException {
          final Set<Object> set = (Set<Object>) seq;
          final Set<Object> newSet = (Set<Object>) this.shallowClone(seq);
          newSet.remove(value);
          return newSet;
        }

        public Object shallowClone(Object seq) {
          Class<?> clz = seq.getClass();
          try {
            Constructor<?> constr = clz.getConstructor();
            if (null == constr) {
              throw new RuntimeException("Cannot clone object " + clz + ": constructor not found");
            }
            Set<Object> set = (Set<Object>) constr.newInstance();
            set.addAll((Set) seq);
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
        public Object roAddValue(Object seq, Object element) {
          StringBuffer csb = new StringBuffer((StringBuffer) seq);
          csb.append(element);
          return csb;
        }

        public Object addValue(Object seq, Object element) {
          ((StringBuffer) seq).append(element);
          return seq;
        }

        public Object roPutByKey(Object seq, Object key, Object element) {
          throw invalidOp(seq, "non-mutating putByKey");
        }

        public Object putByKey(Object seq, Object key, Object element) {
          throw invalidOp(seq, "putByKey");
        }

        public Object roSet(Object seq, int idx, Object element) throws IndexOutOfBoundsException {
          StringBuffer csb = new StringBuffer((StringBuffer) seq);
          this.set(csb, idx, element);
          return csb;
        }

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

        public Object removeByKeyOrIndex(Object seq, Object keyidx)
            throws IndexOutOfBoundsException {
          final StringBuffer buf = (StringBuffer) seq;
          int idx = Utils.asNumber(keyidx).intValue();
          final Character chr = buf.charAt(idx);
          buf.deleteCharAt(idx);
          return chr;
        }

        public Object roRemoveByKeyOrIndex(Object seq, Object keyidx)
            throws IndexOutOfBoundsException {
          StringBuffer csb = new StringBuffer((StringBuffer) seq);
          this.removeByKeyOrIndex(csb, keyidx);
          return csb;
        }

        public Object roRemoveValue(Object seq, Object value) {
          StringBuffer csb = new StringBuffer((StringBuffer) seq);
          this.removeValue(csb, value);
          return csb;
        }

        public Object removeValue(Object seq, Object value) {
          final StringBuffer buf = (StringBuffer) seq;
          final String val = Utils.asStringOrNull(value);
          if (null != val) {
            int idx = buf.indexOf(val);
            if (idx >= 0) {
              buf.delete(idx, idx + val.length());
            }
            return val;
          }
          return null;
        }

        public Object roInsert(Object seq, int idx, Object element)
            throws IndexOutOfBoundsException {
          StringBuffer nb = new StringBuffer((StringBuffer) seq);
          this.insert(nb, idx, element);
          return nb;
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
        public Object roAddValue(Object seq, Object element) {
          StringBuilder csb = new StringBuilder((StringBuffer) seq);
          csb.append(element);
          return csb;
        }

        public Object addValue(Object seq, Object element) {
          ((StringBuilder) seq).append(element);
          return seq;
        }

        public Object roPutByKey(Object seq, Object key, Object element) {
          throw invalidOp(seq, "non-mutating putByKey");
        }

        public Object putByKey(Object seq, Object key, Object element) {
          throw invalidOp(seq, "putByKey");
        }

        public Object roSet(Object seq, int idx, Object element) throws IndexOutOfBoundsException {
          StringBuilder csb = new StringBuilder((StringBuilder) seq);
          this.set(csb, idx, element);
          return csb;
        }

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

        public Object roRemoveByKeyOrIndex(Object seq, Object keyidx)
            throws IndexOutOfBoundsException {
          StringBuilder csb = new StringBuilder((StringBuilder) seq);
          this.removeByKeyOrIndex(csb, keyidx);
          return csb;
        }

        public Object removeByKeyOrIndex(Object seq, Object keyidx)
            throws IndexOutOfBoundsException {
          final StringBuilder buf = (StringBuilder) seq;
          final int idx = Utils.asNumber(keyidx).intValue();
          final Character chr = buf.charAt(idx);
          buf.deleteCharAt(idx);
          return chr;
        }

        public Object roRemoveValue(Object seq, Object value) {
          StringBuilder csb = new StringBuilder((StringBuilder) seq);
          this.removeValue(csb, value);
          return csb;
        }

        public Object removeValue(Object seq, Object value) {
          final StringBuilder buf = (StringBuilder) seq;
          final String val = Utils.asStringOrNull(value);
          if (null != val) {
            int idx = buf.indexOf(val);
            if (idx >= 0) {
              buf.delete(idx, idx + val.length());
            }
            return val;
          }
          return null;
        }

        public Object roInsert(Object seq, int idx, Object element)
            throws IndexOutOfBoundsException {
          StringBuilder nb = new StringBuilder((StringBuilder) seq);
          this.insert(nb, idx, element);
          return nb;
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

  protected static final SeqAdapter stringAdapter =
      new SeqAdapter() {
        public Object roAddValue(Object seq, Object element) {
          StringBuilder b = new StringBuilder((String) seq);
          b.append(element);
          return b.toString();
        }

        public Object addValue(Object seq, Object element) {
          throw cannotChangeError(seq);
        }

        public Object roPutByKey(Object seq, Object key, Object element) {
          throw invalidOp(seq, "non-mutating putByKey");
        }

        public Object putByKey(Object seq, Object key, Object element) {
          throw invalidOp(seq, "putByKey");
        }

        public Object roSet(Object seq, int idx, Object element) {
          StringBuilder b = new StringBuilder((String) seq);
          stringBuilderAdapter.set(b, idx, element);
          return b.toString();
        }

        public Object set(Object seq, int idx, Object element) {
          throw cannotChangeError(seq);
        }

        public Object roRemoveByKeyOrIndex(Object seq, Object keyidx)
            throws IndexOutOfBoundsException {
          StringBuilder b = new StringBuilder((String) seq);
          stringBuilderAdapter.removeByKeyOrIndex(b, keyidx);
          return b.toString();
        }

        public Object removeByKeyOrIndex(Object seq, Object keyidx)
            throws IndexOutOfBoundsException {
          throw cannotChangeError(seq);
        }

        public Object roInsert(Object seq, int idx, Object element)
            throws IndexOutOfBoundsException {
          StringBuilder b = new StringBuilder((String) seq);
          stringBuilderAdapter.insert(b, idx, element);
          return b.toString();
        }

        public Object insert(Object seq, int idx, Object element) {
          throw cannotChangeError(seq);
        }

        public Object get(Object seq, int idx) throws IndexOutOfBoundsException {
          return ((CharSequence) seq).charAt(idx);
        }

        public Object roRemoveValue(Object seq, Object value) {
          StringBuilder csb = new StringBuilder((String) seq);
          stringBuilderAdapter.removeValue(csb, value);
          return csb.toString();
        }

        public Object removeValue(Object seq, Object value) {
          throw cannotChangeError(seq);
        }

        public Object shallowClone(Object seq) {
          return new String((String) seq);
        }
      };

  // FIXME: implement RO operations
  protected static final SeqAdapter charSequenceAdapter =
      new SeqAdapter() {
        public Object roAddValue(Object seq, Object element) {
          throw invalidOp(seq, "non-mutating addValue");
        }

        public Object addValue(Object seq, Object element) {
          throw cannotChangeError(seq);
        }

        public Object roPutByKey(Object seq, Object key, Object element) {
          throw invalidOp(seq, "non-mutating putByKey");
        }

        public Object putByKey(Object seq, Object key, Object element) {
          throw invalidOp(seq, "putByKey");
        }

        public Object roSet(Object seq, int idx, Object element) {
          throw cannotChangeError(seq);
        }

        public Object set(Object seq, int idx, Object element) {
          throw cannotChangeError(seq);
        }

        public Object removeByKeyOrIndex(Object seq, Object keyidx)
            throws IndexOutOfBoundsException {
          throw cannotChangeError(seq);
        }

        public Object roRemoveByKeyOrIndex(Object seq, Object keyidx)
            throws IndexOutOfBoundsException {
          throw cannotChangeError(seq);
        }

        public Object roInsert(Object seq, int idx, Object element)
            throws IndexOutOfBoundsException {
          throw cannotChangeError(seq);
        }

        public Object insert(Object seq, int idx, Object element) {
          throw cannotChangeError(seq);
        }

        public Object get(Object seq, int idx) throws IndexOutOfBoundsException {
          return ((CharSequence) seq).charAt(idx);
        }

        public Object roRemoveValue(Object seq, Object value) {
          throw cannotChangeError(seq);
        }

        public Object removeValue(Object seq, Object value) {
          throw cannotChangeError(seq);
        }

        public Object shallowClone(Object seq) {
          throw cannotChangeError(seq);
        }
      };

  protected static final SeqAdapter arrayAdapter =
      new SeqAdapter() {
        public Object roAddValue(Object seq, Object element) {
          throw invalidOp(seq, "addValue");
        }

        public Object addValue(Object seq, Object element) {
          throw invalidOp(seq, "addValue");
        }

        public Object roPutByKey(Object seq, Object key, Object element) {
          throw invalidOp(seq, "non-mutating putByKey");
        }

        public Object putByKey(Object seq, Object key, Object element) {
          throw invalidOp(seq, "putByKey");
        }

        public Object roSet(Object seq, int idx, Object element) {
          Object ac = this.shallowClone(seq);
          this.set(ac, idx, element);
          return ac;
        }

        @Override
        public Object set(Object seq, int idx, Object element) throws IndexOutOfBoundsException {
          final Object result = Array.get(seq, idx);
          Utils.aset(seq, idx, element);
          return result;
        }

        @Override
        public Object insert(Object seq, int idx, Object element) {
          throw new RuntimeException("Cannot insert into object of type " + seq.getClass());
        }

        @Override
        public Object roInsert(Object seq, int idx, Object element)
            throws IndexOutOfBoundsException {
          throw new RuntimeException(
              "Cannot insert (non-mutating) into  object of type " + seq.getClass());
        }

        @Override
        public Object get(Object seq, int idx) throws IndexOutOfBoundsException {
          return Array.get(seq, idx);
        }

        @Override
        public Object removeByKeyOrIndex(Object seq, Object keyidx)
            throws IndexOutOfBoundsException {
          throw new RuntimeException("Cannot remove element from object of type " + seq.getClass());
        }

        @Override
        public Object roRemoveByKeyOrIndex(Object seq, Object keyidx)
            throws IndexOutOfBoundsException {
          throw new RuntimeException("Cannot remove element from object of type " + seq.getClass());
        }

        public Object roRemoveValue(Object seq, Object value) {
          throw new RuntimeException("Cannot remove element from object of type " + seq.getClass());
        }

        public Object removeValue(Object seq, Object value) {
          throw new RuntimeException("Cannot remove value from object of type " + seq.getClass());
        }

        @Override
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
        public Object roAddValue(Object seq, Object element) {
          Map<Object, Object> cm = (Map<Object, Object>) this.shallowClone(seq);
          checkNonMutatingChange(seq, cm);
          this.addValue(cm, element);
          return cm;
        }

        public Object addValue(Object seq, Object element) {
          if (element instanceof Map.Entry) {
            final Map.Entry<Object, Object> e = (Map.Entry<Object, Object>) element;
            ((Map<Object, Object>) seq).put(e.getKey(), e.getValue());
          } else {
            throw new RuntimeException("Cannot add object of type " + seq.getClass() + " to a Map");
          }
          return seq;
        }

        public Object roPutByKey(Object m, Object key, Object value) {
          Map<Object, Object> cm = (Map<Object, Object>) this.shallowClone(m);
          checkNonMutatingChange(m, cm);
          this.putByKey(cm, key, value);
          return cm;
        }

        public Object putByKey(Object seq, Object key, Object element) {
          return ((Map) seq).put(key, element);
        }

        public Object roSet(Object seq, int idx, Object element) {
          throw new RuntimeException("Cannot modify object of type " + seq.getClass());
        }

        public Object set(Object seq, int idx, Object element) {
          return ((Map<Object, Object>) seq).put(idx, element);
        }

        public Object insert(Object seq, int idx, Object element) {
          return ((Map<Object, Object>) seq).put(idx, element);
        }

        public Object roInsert(Object seq, int idx, Object element)
            throws IndexOutOfBoundsException {
          return this.roPutByKey(seq, idx, element);
        }

        public Object removeByKeyOrIndex(Object seq, Object keyidx)
            throws IndexOutOfBoundsException {
          return ((Map<Object, Object>) seq).remove(keyidx);
        }

        public Object roRemoveByKeyOrIndex(Object seq, Object keyidx)
            throws IndexOutOfBoundsException {
          Map<Object, Object> cm = (Map<Object, Object>) this.shallowClone(seq);
          checkNonMutatingChange(seq, cm);
          this.removeByKeyOrIndex(cm, keyidx);
          return cm;
        }

        public Object get(Object seq, int idx) {
          return ((Map<Object, Object>) seq).get(idx);
        }

        public Object roRemoveValue(Object m, Object value) {
          Map<Object, Object> cm = (Map<Object, Object>) this.shallowClone(m);
          checkNonMutatingChange(m, cm);
          this.removeValue(cm, value);
          return cm;
        }

        public Object removeValue(Object seq, Object value) {
          Map<Object, Object> map = (Map) seq;
          if (map.containsValue(value)) {
            Map.Entry<Object, Object> entry = null;
            for (Map.Entry<Object, Object> e : map.entrySet()) {
              if (Utils.equal(e.getValue(), value)) {
                entry = e;
              }
            }
            map.remove(entry.getKey(), entry.getValue());
            return entry.getValue();
          } else {
            return null;
          }
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

  @SuppressWarnings("unchecked")
  protected static final SeqAdapter immutableMapAdapter =
      new SeqAdapter() {
        public Object roAddValue(Object seq, Object element) {
          Map<Object, Object> cm = new HashMap<Object, Object>((Map<Object, Object>) seq);
          mapAdapter.addValue(cm, element);
          return Collections.unmodifiableMap(cm);
        }

        public Object addValue(Object seq, Object element) {
          throw cannotChangeError(seq);
        }

        public Object roPutByKey(Object m, Object key, Object value) {
          Map<Object, Object> cm = new HashMap<Object, Object>((Map<Object, Object>) m);
          mapAdapter.putByKey(cm, key, value);
          return Collections.unmodifiableMap(cm);
        }

        public Object putByKey(Object seq, Object key, Object element) {
          throw new RuntimeException("Cannot modify object of type " + seq.getClass());
        }

        public Object roSet(Object seq, int idx, Object element) {
          throw new RuntimeException("Cannot modify object of type " + seq.getClass());
        }

        public Object set(Object seq, int idx, Object element) {
          throw cannotChangeError(seq);
        }

        public Object insert(Object seq, int idx, Object element) {
          throw cannotChangeError(seq);
        }

        public Object roInsert(Object seq, int idx, Object element)
            throws IndexOutOfBoundsException {
          return this.roPutByKey(seq, idx, element);
        }

        public Object removeByKeyOrIndex(Object seq, Object keyidx)
            throws IndexOutOfBoundsException {
          throw cannotChangeError(seq);
        }

        public Object roRemoveByKeyOrIndex(Object seq, Object keyidx)
            throws IndexOutOfBoundsException {
          final Map<Object, Object> cm = new HashMap<Object, Object>((Map<Object, Object>) seq);
          mapAdapter.removeByKeyOrIndex(cm, keyidx);
          return Collections.unmodifiableMap(cm);
        }

        public Object removeValue(Object seq, Object value) {
          throw cannotChangeError(seq);
        }

        public Object roRemoveValue(Object seq, Object value) {
          final Map<Object, Object> cm = new HashMap<Object, Object>((Map<Object, Object>) seq);
          mapAdapter.removeValue(cm, value);
          return Collections.unmodifiableMap(cm);
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
        public Object roAddValue(Object seq, Object element) {
          return null;
        }

        public Object addValue(Object seq, Object element) {
          return null;
        }

        public Object roPutByKey(Object seq, Object key, Object element) {
          return null;
        }

        public Object putByKey(Object seq, Object key, Object element) {
          return null;
        }

        public Object roSet(Object seq, int idx, Object element) {
          return null;
        }

        public Object set(Object seq, int idx, Object element) {
          return null;
        }

        public Object insert(Object seq, int idx, Object element) {
          return null;
        }

        public Object roInsert(Object seq, int idx, Object element)
            throws IndexOutOfBoundsException {
          return null;
        }

        public Object removeByKeyOrIndex(Object seq, Object keyidx)
            throws IndexOutOfBoundsException {
          return null;
        }

        public Object roRemoveByKeyOrIndex(Object seq, Object keyidx)
            throws IndexOutOfBoundsException {
          return null;
        }

        public Object roRemoveValue(Object m, Object value) {
          return null;
        }

        public Object removeValue(Object seq, Object value) {
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
    if (seq instanceof Set) {
      return Utils.isKnownImmutable(seq) ? immutableSetAdapter : setAdapter;
    } else {
      return getAssociativeSeqAdapter(seq);
    }
  }

  protected static SeqAdapter getAssociativeSeqAdapter(Object seq) {
    if (null == seq) {
      return nullAdapter;
    } else if (seq instanceof List) {
      return Utils.isKnownImmutable(seq) ? immutableListAdapter : listAdapter;
    } else if (seq instanceof Map) {
      return Utils.isKnownImmutable(seq) ? immutableMapAdapter : mapAdapter;
    } else if (seq.getClass().isArray()) {
      return arrayAdapter;
    } else if (seq instanceof String) {
      return stringAdapter;
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
  * Remove sequence element by index.
  */
  public static Object removeElementByIndex(Object seq, int index) {
    SeqAdapter adapter = getAssociativeSeqAdapter(seq);
    try {
      return adapter.removeByKeyOrIndex(seq, index);
    } catch (IndexOutOfBoundsException ex) {
      return null;
    }
  }

  /**
   * Put element into associative structure by key or index.
   * Returns previous value of the entry.
   * In case of IndexOutOfBoundsException does not modify
   * the structure and returns null.
  */
  @SuppressWarnings("unchecked")
  public static Object putElement(Object seq, Object key, Object element) {
    if (seq instanceof Map) {
      return putToMap((Map)seq, key, element);
    } else {
      try {
        return setElementByIndex(seq, Utils.asNumber(key).intValue(), element);
      } catch (IndexOutOfBoundsException ex) {
        return null;
      }
    }
  }

  
  /**
   * put sequence element by key. Return old value at this index.
   */
  @SuppressWarnings("unchecked")
  public static Object roPutElement(Object seq, Object key, Object element) {
    if (seq instanceof Map) {
      return roPutToMap((Map)seq, key, element);
    } else {
      try {
        return roSetElementByIndex(seq, Utils.asNumber(key).intValue(), element);
      } catch (IndexOutOfBoundsException ex) {
        return null;
      }
    }
  }

  public static Object putToMap(Map<Object,Object> m, Object key, Object  value) {
    SeqAdapter adapter = getAssociativeSeqAdapter(m);
    return adapter.putByKey(m, key, value);
  }

  public static Object roPutToMap(Map<Object,Object> m, Object key, Object  value) {
    SeqAdapter adapter = getAssociativeSeqAdapter(m);
    return adapter.roPutByKey(m, key, value);
  }
  
  public static Object roSetElementByIndex(Object seq, int index, Object element) {
    SeqAdapter adapter = getAssociativeSeqAdapter(seq);
    return adapter.roSet(seq, index, element);
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
   * Put sequence element by index.
   * Return copy of the target object with the change applied.
   */
  public static Object roInsertElementByIndex(Object seq, int index, Object element)
      throws IndexOutOfBoundsException {
    SeqAdapter adapter = getAssociativeSeqAdapter(seq);
    return adapter.roInsert(seq, index, element);
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
              if (buf.length() != 0) {
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
