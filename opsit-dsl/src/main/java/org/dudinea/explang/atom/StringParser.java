package org.dudinea.explang.atom;

import org.dudinea.explang.ParseCtx;


public  class StringParser implements AtomParser {
	// FIXME: support escapes
	@Override
	public boolean parse(String str, Object[] holder,  ParseCtx pctx) {
	    if (str.length() >=2 &&
		str.startsWith("\"") &&
		str.endsWith("\"")) {
		holder[0]= str.substring(1,str.length()-1);
		return true;
	    } else {
		return false;
	    }
	}
    }
