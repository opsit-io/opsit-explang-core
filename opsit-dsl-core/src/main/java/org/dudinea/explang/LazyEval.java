package org.dudinea.explang;

import org.dudinea.explang.Compiler.ICtx;

public class LazyEval implements ICompiled  {
    private ICompiled expr;
    private ICtx ctx;
    private Object value = null;
    boolean hasValue = false;
    public LazyEval(ICompiled expr) {
	this.expr = expr;
	//this.ctx = ctx;
    }

    //@Override
    public Object evaluate(Backtrace backtrace, ICtx extCtx) {
	ctx = extCtx;
	return this;
    }

    public Object getValue(Backtrace backtrace) {
	if (!hasValue) {
	    value = expr.evaluate(backtrace, ctx);
	    hasValue = true;
	}
	return value;
    }

    @Override
    public void setDebugInfo(ParseCtx pctx) {

    }

    @Override
    public void setName(String str) {

    }

    @Override
    public ParseCtx getDebugInfo() {
	return expr.getDebugInfo();
    }
}
