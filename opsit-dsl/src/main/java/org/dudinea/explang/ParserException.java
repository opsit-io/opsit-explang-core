package org.dudinea.explang;
import org.dudinea.explang.ParseCtx;

public class ParserException
    extends Exception {
    protected ParseCtx pctx;
    public  ParserException (String msg) {
	super(String.format("Parser: %s",msg));
    }

    public  ParserException (ParseCtx pctx, String msg) {
	super(String.format("%s:%d:%d: Parser: %s",
			    pctx.input, pctx.getLine(), pctx.getPos(), msg));
	this.pctx = pctx;
    }

    public  ParserException (ParseCtx pctx, String msg, Throwable t) {
	super(String.format("%s:%d:%d: Parser: %s",
			    pctx.input, pctx.getLine(), pctx.getPos(), msg), t);
	this.pctx = pctx;
    }
}
