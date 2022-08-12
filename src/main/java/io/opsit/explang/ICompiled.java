package io.opsit.explang;

public interface ICompiled {
  public Object evaluate(Backtrace backtrace, Compiler.ICtx ctx);

  public void setDebugInfo(ParseCtx pctx);

  public void setName(String str);

  public ParseCtx getDebugInfo();
}
