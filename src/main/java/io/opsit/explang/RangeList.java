package io.opsit.explang;

import io.opsit.version.Version;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

// import io.opsit.explang.Funcs.AbstractOp;

public class RangeList implements List<Number> {
  private final AbstractOp addop =
      new AbstractOp() {
        @Override
        public Number doIntOp(Number result, Number arg) {
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

        @Override
        public Version doVersionOp(Number result, Number arg) {
          if (result instanceof Version) {
            Version verResult = (Version) result;
            if (arg instanceof Version) {
              return verResult.add((Version) arg);
            }
            return verResult.add(arg);
          } else if (arg instanceof Version) {
            return ((Version) arg).add(result);
          } else {
            return Version.fromDouble(result.doubleValue() + arg.doubleValue());
          }
        }
      };

  protected final Promotion prom = new Promotion();
  protected Number start;
  protected Number to;
  protected Number step;

  /**
   * Construct RangeList from interval of numbers with step.
   */
  public RangeList(final Number start, final Number to, final Number step) {
    this.step = step;
    this.to = to;
    this.start = start;
    prom.promote(start);
    prom.promote(step);
  }

  @Override
  public Iterator<Number> iterator() {
    return new Iterator<Number>() {
      final Number []from = new Number[] {start};
      final boolean inv = step.doubleValue() < 0;

      @Override
      public boolean hasNext() {
        final double f = from[0].doubleValue();
        final double t = to.doubleValue();
        return (inv ^ (f < t)) && (f != t);
      }

      @Override
      public Number next() {
        final Number result = prom.returnResult(from[0]);
        final Number r = prom.callOP(addop, from[0], step);
        from[0] = prom.returnResult(r);
        return result;
      }
    };
  }

  @Override
  public int size() {
    final double dIntervals = ((to.doubleValue() - start.doubleValue()) / step.doubleValue());
    final int intervals = (int) dIntervals;
    final int length = dIntervals > intervals ? intervals + 1 : intervals;
    return length > 0 ? length : 0;
  }

  @Override
  public boolean isEmpty() {
    return 0 == size();
  }

  @Override
  public boolean contains(Object obj) {
    return indexOf(obj) >= 0 ? true : false;
  }

  @Override
  public Object[] toArray() {
    final Object obj = prom.returnResult(0);
    final Object array = Array.newInstance(obj.getClass(), 0);
    return toArray((Object[]) array);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T[] toArray(T[] arr) {
    final int length = size();
    final Object array = Array.newInstance(arr.getClass().getComponentType(), length);
    final Iterator<Number> iter = this.iterator();
    for (int i = 0; i < length; i++) {
      Array.set(array, i, iter.next());
    }
    return (T[]) array;
  }

  @Override
  public Number remove(int index) {
    throw new UnsupportedOperationException("Cannot use remove on RANGE");
  }

  @Override
  public boolean remove(Object obj) {
    throw new UnsupportedOperationException("Cannot use remove on RANGE");
  }

  @Override
  public boolean containsAll(Collection<?> col) {
    if (null == col) {
      throw new NullPointerException("containsAll called with null argument");
    }
    for (Object obj : col) {
      if (!contains(obj)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean addAll(Collection<? extends Number> col) {
    throw new UnsupportedOperationException("Cannot use addAll on RANGE");
  }

  @Override
  public boolean addAll(int index, Collection<? extends Number> col) {
    throw new UnsupportedOperationException("Cannot use addAll on RANGE");
  }

  @Override
  public boolean removeAll(Collection<?> col) {
    throw new UnsupportedOperationException("Cannot use addAll on RANGE");
  }

  @Override
  public boolean retainAll(Collection<?> col) {
    throw new UnsupportedOperationException("Cannot use addAll on RANGE");
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException("Cannot use addAll on RANGE");
  }


  @Override
  public Number get(int index) {
    if (index < 0 || index >= size()) {
      throw new IndexOutOfBoundsException();
    }
    return unlimGet(index);
  }

  protected Number unlimGet(int index) {
    return prom.returnResult(start.doubleValue() + step.doubleValue() * index);
  }

  @Override
  public Number set(int index, Number element) {
    throw new UnsupportedOperationException("Cannot use set on RANGE");
  }

  @Override
  public boolean add(Number num) {
    throw new UnsupportedOperationException("Cannot use add on RANGE");
  }

  
  @Override
  public void add(int index, Number element) {
    throw new UnsupportedOperationException("Cannot use add on RANGE");
  }

  
  @Override
  public int indexOf(Object obj) {
    // FIXME: won't reliably work on floating point numbers
    // FIXME: call different implementations for integers and floats
    if (null == obj) {
      // strict behaviour would be
      // throw new NullPointerException("contains called with null argument");
      return -1;
    }
    if (!(obj instanceof Number)) {
      throw new ClassCastException(
          "contains called with unsupported class " + obj.getClass() + ", must be a Number");
    }
    final double val = ((Number) obj).doubleValue();
    final long pos = Math.round((val - start.doubleValue()) / step.doubleValue());
    if (pos < 0 || pos >= size()) {
      return -1;
    }
    final double checkVal = step.doubleValue() * pos + start.doubleValue();
    final Number checkNum = prom.returnResult(checkVal);
    // FIXME: overflow
    return checkNum.equals(obj) ? (int) pos : -1;
  }

  @Override
  public int lastIndexOf(Object obj) {
    return indexOf(obj);
  }

  @Override
  public ListIterator<Number> listIterator() {
    // FIXME
    throw new NotImplementedException("listIterator");
  }

  @Override
  public ListIterator<Number> listIterator(int index) {
    // FIXME
    throw new NotImplementedException("listIterator");
  }

  @Override
  public List<Number> subList(int fromIndex, int toIndex) {
    if (fromIndex < 0) {
      throw new IndexOutOfBoundsException("fromIndex=" + fromIndex);
    }
    if (toIndex > size()) {
      throw new IndexOutOfBoundsException("toIndex=" + toIndex);
    }
    if (fromIndex > toIndex) {
      throw new IllegalArgumentException("fromIndex > toIndex");
    }
    final Number startNum = get(fromIndex);
    final Number toNum = unlimGet(toIndex);
    return new RangeList(startNum, toNum, step);
  }
}
