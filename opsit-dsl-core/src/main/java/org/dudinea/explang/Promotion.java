package org.dudinea.explang;

import org.dudinea.explang.Funcs.ABSTRACT_OP;
import org.dudinea.explang.Version;

// used by numeric operators
public class Promotion {
    public boolean noVersion = true;
    public boolean noDouble = true;
    public boolean noFloat  = true;
    public boolean noShort  = true;
    public boolean noInt    = true;
    public boolean noLong   = true;
        
    public void promote(Number arg) {
        if (noVersion) {
            if (noDouble) {
                if (noFloat) {
                    if (noLong) {
                        if (noInt) {
                            if (noShort) {
                                if (arg instanceof Short) {
                                    noShort = false;
                                    return;
                                }
                            }
                            if (arg instanceof Integer) {
                                noInt = false;
                                return;
                            }
                        }
                        if (arg instanceof Long) {
                            noLong = false;
                            return;
                        }
                    }
                    if (arg instanceof Float) {
                        noFloat = false;
                        return;
                    }
                }
                if (arg instanceof Double) {
                    noDouble = false;
                    return;
                }
            }
            if (arg instanceof Version) {
                noVersion = false;
                return;
            }
        }
    }
    public Number callOP(ABSTRACT_OP op, Number arg1, Number arg2) {
        final Number result =
            (noVersion
             ? (noDouble 
                ? (noFloat ? op.doIntOp(arg1, arg2) : op.doFloatOp(arg1, arg2)) 
                : op.doDoubleOp(arg1, arg2))
             : op.doVersionOp(arg1, arg2));
        return result;
    }

    public Number returnResult(Number result) {
        if (noVersion) {
            if (noDouble) {
                if (noFloat) {
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
                    return result.floatValue();
                }
            } else {
                return result.doubleValue();
            }
        } else {
            return result;
        }
    }
}
