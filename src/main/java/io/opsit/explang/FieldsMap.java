package io.opsit.explang;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class FieldsMap implements Map<Object, Object> {
  public static interface Op {
    Object get(Map<? extends Object, ? extends Object> src);
  }

  protected Map<? extends Object, ? extends Object> src;
  protected Map<Object, Op> fmap;

  public FieldsMap(Map<? extends Object, ? extends Object> src, Map<Object, Op> fmap) {
    this.src = src;
    this.fmap = fmap;
  }

  /**
   * Make new instance out of another FieldsMap.
   */
  public FieldsMap(FieldsMap fm) {
    initFromFieldsMap(fm);
    /*this.src = fm.src;
    this.fmap = new HashMap<Object,Op>();
    fmap.putAll(fm.fmap);*/
  }

  /**
   * Make new instance out of any Map.
   */
  public FieldsMap(Map<?,?> o) {
    if (o instanceof FieldsMap) {
      initFromFieldsMap((FieldsMap) o);
    } else {
      this.src = o;
      this.fmap = new HashMap<Object, Op>();
    }
  }

  protected void initFromFieldsMap(FieldsMap fm) {
    this.src = fm.src;
    this.fmap = new HashMap<Object,Op>();
    fmap.putAll(fm.fmap);
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
    for (Object key : this.keySet()) {
      final Op op = fmap.get(key);
      if (null != op) {
        Object val = op.get(src);
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
    final Op op = fmap.get(key);
    if (null != op) {
      return op.get(src);
    } else {
      return null;
    }
  }

  @Override
  public Object put(final Object key, final Object value) {
    Object result = this.get(key);
    this.fmap.put(key, new Op() {
          public Object get(Map<? extends Object, ? extends Object> srcMap) {
            return value;
          }
    });
    return result;
  }

  @Override
  public Object remove(Object key) {
    throw new RuntimeException("remove implemented for " + this.getClass().getSimpleName());
  }

  @Override
  public void putAll(Map<?, ?> map) {
    throw new RuntimeException("putAll not implemented for " + this.getClass().getSimpleName());
  }

  @Override
  public void clear() {
    throw new RuntimeException("clear not implemented for " + this.getClass().getSimpleName());
  }

  @Override
  public Set<Object> keySet() {
    return fmap.keySet();
  }

  @Override
  public Collection<Object> values() {
    Set<Object> values = new HashSet<Object>();
    for (Object key : this.keySet()) {
      final Op op = fmap.get(key);
      if (null != op) {
        final Object val = op.get(src);
        values.add(val);
      }
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
