package io.opsit.explang;

import io.opsit.explang.ParseCtx;

public class ParserEOFException
    extends  ParserException {

    public  ParserEOFException (String msg) {
	super(msg);
    }

    public  ParserEOFException (ParseCtx pctx, String msg) {
	super(pctx, msg);
    }

    public  ParserEOFException (ParseCtx pctx, String msg, Throwable t) {
	super(pctx, msg, t);
    }
}
