package org.dudinea.explang.atom;

import org.dudinea.explang.ParseCtx;

public  class BooleanParser implements AtomParser {
    public static final String FALSE_LITERAL = "false";
    public static final String TRUE_LITERAL = "true";

    @Override
    public boolean parse(String str, Object[] holder,  ParseCtx pctx) {
	if (str.equalsIgnoreCase(FALSE_LITERAL)) {
	    holder[0]= Boolean.FALSE;
	    return true;
	} else if (str.equalsIgnoreCase(TRUE_LITERAL)) {
	    holder[0]= Boolean.TRUE;
	    return true; 
	} else {
	    return false;
	}
    }
}
