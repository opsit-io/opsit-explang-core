package io.opsit.explang;

import io.opsit.explang.Compiler.ICtx;

public class LazyEval implements ICompiled  {
    private ICompiled expr;
    private ICtx ctx;
    public LazyEval(ICompiled expr) {
        this.expr = expr;
    }

    @Override
    public Object evaluate(Backtrace backtrace, ICtx extCtx) {
        ctx = extCtx;
        return this;
    }

    public Object getValue(Backtrace backtrace) {
        return expr.evaluate(backtrace, ctx);
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
