package org.dudinea.explang;

import org.dudinea.explang.Funcs.ABSTRACT_OP;

// used by numeric operators
public class Promotion {
    public boolean noDouble = true;
    public boolean noFloat  = true;
    public boolean noShort  = true;
    public boolean noInt    = true;
    public boolean noLong   = true;
        
    public void promote(Number arg) {
        if (noDouble) {
            if (noFloat) {
                if (noLong) {
                    if (noInt) {
                        if (noShort) {
                            if (arg instanceof Short) {noShort = false; return;}
                        }
                        if (arg instanceof Integer) {noInt = false; return; }
                    }
                    if (arg instanceof Long) {noLong = false;  return;}
                }
                if (arg instanceof Float) {noFloat = false; return;}
            }
            if (arg instanceof Double) {noDouble = false; return;}
        }
    }
    public Number callOP(ABSTRACT_OP op, Number arg1, Number arg2) {
        final Number result = noDouble 
            ? (noFloat ? op.doIntOp(arg1, arg2) : op.doFloatOp(arg1, arg2)) 
            : op.doDoubleOp(arg1, arg2);
        return result;
    }

    public Number returnResult(Number result) {
        if (noFloat && noDouble) {
            if (noLong) {
                if (noInt) {
                    if (noShort) {
                        return result.byteValue();
                    } else {
                        return result.shortValue();
                    }
                } else {
                    return result.intValue();
                }
            } else {
                return result.longValue();
            }
        } else {
            return result;
        }
    }
}
