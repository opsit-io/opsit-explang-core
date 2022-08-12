package io.opsit.explang;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;


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
  public boolean contains(Object obj) {
    return lst.contains(obj);
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
          return ((LazyEval) obj).getValue(bt);
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
  public Object[] toArray(Object[] array) {
    return lst.toArray(array);
  }

  @Override
  public boolean add(Object obj) {
    return lst.add(obj);
  }

  @Override
  public void add(int index, Object element) {
    lst.add(index, element);
  }

  @Override
  public boolean remove(Object obj) {
    return lst.remove(obj);
  }

  @Override
  public Object remove(int index) {
    return lst.remove(index);
  }

  @Override
  public boolean containsAll(Collection<?> col) {
    return lst.containsAll(col);
  }

  @Override
  public boolean addAll(Collection<? extends Object> col) {
    return lst.addAll(col);
  }

  @Override
  public boolean addAll(int index, Collection<? extends Object> col) {
    return lst.addAll(index, col);
  }

  @Override
  public boolean removeAll(Collection<? extends Object> col) {
    return lst.removeAll(col);
  }

  @Override
  public boolean retainAll(Collection<? extends Object> col) {
    return lst.retainAll(col);
  }

  @Override
  public void clear() {
    lst.clear();
  }

  @Override
  public Object get(int index) {
    final Object obj = lst.get(index);
    if (obj instanceof LazyEval) {
      return ((LazyEval) obj).getValue(bt);
    } else {
      return obj;
    }
  }

  @Override
  public Object set(int index, Object element) {
    return lst.set(index, element);
  }

  @Override
  public int indexOf(Object obj) {
    return lst.indexOf(obj);
  }

  @Override
  public int lastIndexOf(Object obj) {
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
