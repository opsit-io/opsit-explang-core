package io.opsit.explang;

/**
 * Perform operation on two numeric operands.
 *
 * <p>Part of the numeric promotion mechanism.
 *
 */
public interface AbstractOp {
  /**
   * Do operation on integer type operands (Long, Integer, Short,
   * Byte).
   *
   */
  Number doIntOp(Number arg1, Number arg2);

  /**
   * Do operation on Double type operands.
   *
   * <p>At least one of operands must be Double, rest may be integer
   * types or Float.
   */  
  Number doDoubleOp(Number arg1, Number arg2);

  /**
   * Do operation on Float type operands.
   *
   * <p>At least one of operands must be Float, the rest may be integer types.
   */    
  Number doFloatOp(Number arg1, Number arg2);

  /**
   * Do operation on Version types operands.
   *
   * <p>At least one of operands must be Version, others may be other numeric types. 
   */    
  Number doVersionOp(Number arg1, Number arg2);
}
