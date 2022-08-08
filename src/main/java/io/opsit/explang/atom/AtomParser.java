package io.opsit.explang.atom;

import  io.opsit.explang.ParseCtx;

public  interface AtomParser {
  public boolean parse(String str, Object[] holder,  ParseCtx pctx)
    throws AtomParseException ;
}
