package org.dudinea.explang.atom;

import  org.dudinea.explang.ParseCtx;

public  interface AtomParser {
    public boolean parse(String str, Object[] holder,  ParseCtx pctx)
	throws AtomParseException ;
}
