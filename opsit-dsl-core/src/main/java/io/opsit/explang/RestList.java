package io.opsit.explang;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

//@SuppressWarnings("rawtypes")
public class RestList implements List<Object> {
  private List<Object> lst;
  Backtrace bt;
    
  public RestList(List<Object> lst, Backtrace bt) {
    this.lst = lst;
    this.bt = bt;
  }
    
  @Override
  public int size() {
    return lst.size();
  }

  @Override
  public boolean isEmpty() {
    return lst.isEmpty();
  }

  @Override
  public boolean contains(Object o) {
    return lst.contains(o);
  }

  @Override
  public Iterator<Object> iterator() {
    final Iterator<Object> lstIter = lst.iterator();
    return new Iterator<Object>() {
        
      @Override
      public boolean hasNext() {
        return lstIter.hasNext();
      }

      @Override
      public Object next() {
        final Object obj = lstIter.next();
        if (obj instanceof LazyEval) {
          return ((LazyEval)obj).getValue(bt);
        } else {
          return obj;
        }
      }
    };

  }

  @Override
  public Object[] toArray() {
    return lst.toArray();
  }

  @Override
  @SuppressWarnings("unchecked")
  public Object[] toArray(Object[] a) {
    return lst.toArray(a);
  }

  @Override
  public boolean add(Object e) {
    return lst.add(e);
  }

  @Override
  public boolean remove(Object o) {
    return lst.remove(o);
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    return lst.containsAll(c);
  }

  @Override
  public boolean addAll(Collection<? extends Object> c) {
    return lst.addAll(c);
  }

  @Override
  public boolean addAll(int index, Collection<? extends Object> c) {
    return lst.addAll(index, c);
  }

  @Override
  public boolean removeAll(Collection<? extends Object> c) {
    return lst.removeAll(c);
  }

  @Override
  public boolean retainAll(Collection<? extends Object> c) {
    return lst.retainAll(c);
  }

  @Override
  public void clear() {
    lst.clear();
  }

  @Override
  public Object get(int index) {
    final Object obj = lst.get(index);
    if (obj instanceof LazyEval) {
      return ((LazyEval)obj).getValue(bt);
    } else {
      return obj;
    }
  }

  @Override
  public Object set(int index, Object element) {
    return lst.set(index, element);
  }

  @Override
  public void add(int index, Object element) {
    lst.add(index, element);
  }

  @Override
  public Object remove(int index) {
    return lst.remove(index);
  }

  @Override
  public int indexOf(Object o) {
    return lst.indexOf(o);
  }

  @Override
  public int lastIndexOf(Object o) {
    return lst.lastIndexOf(0);
  }

  @Override
  public ListIterator<Object> listIterator() {
    return lst.listIterator();
  }

  @Override
  public ListIterator<Object> listIterator(int index) {
    return lst.listIterator(index);
  }

  @Override
  public List<Object> subList(int fromIndex, int toIndex) {
    return new RestList(lst.subList(fromIndex, toIndex), bt);
  }

  @Override
  public String toString() {
    return lst.toString();
  }
}
