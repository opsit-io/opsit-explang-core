package io.opsit.explang;

import io.opsit.version.Version;
import java.util.Comparator;

public class NumCompOp implements AbstractOp, Comparator<Number> {
  @Override
  public Number doIntOp(Number arg1, Number arg2) {
    return arg1.longValue() - arg2.longValue();
  }

  @Override
  public Number doDoubleOp(Number arg1, Number arg2) {
    final Double compRes = arg1.doubleValue() - arg2.doubleValue();
    return compRes < 0.0 ? -1 : (compRes > 0.0 ? 1 : 0);
  }

  @Override
  public Number doVersionOp(Number arg1, Number arg2) {
    if (arg1 instanceof Version) {
      return ((Version) arg1).compareTo(Version.fromNumber(arg2));
    } else if (arg2 instanceof Version) {
      return -((Version) arg2).compareTo(Version.fromNumber(arg1));
    } else {
      final Double compRes = arg1.doubleValue() - arg2.doubleValue();
      return compRes < 0.0 ? -1 : (compRes > 0.0 ? 1 : 0);
    }
  }

  @Override
  public Number doFloatOp(Number arg1, Number arg2) {
    final float compRes = arg1.floatValue() - arg2.floatValue();
    return compRes < 0.0f ? -1 : (compRes > 0.0f ? 1 : 0);
  }

  @Override
  public int compare(Number v1, Number v2) {
    final Promotion p = new Promotion();
    final Number n1 = (Number) v1;
    p.promote(n1);
    final Number n2 = (Number) v2;
    p.promote(n2);
    final Integer dif = p.callOP(this, n1, n2).intValue();
    return dif;
  }
}
