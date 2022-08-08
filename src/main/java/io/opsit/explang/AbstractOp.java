package io.opsit.explang;

public interface AbstractOp {
  Number doIntOp(Number arg1, Number arg2);

  Number doDoubleOp(Number arg1, Number arg2);

  Number doFloatOp(Number arg1, Number arg2);

  Number doVersionOp(Number arg1, Number arg2);
}
