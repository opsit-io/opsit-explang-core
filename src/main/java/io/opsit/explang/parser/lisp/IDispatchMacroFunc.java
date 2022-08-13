package io.opsit.explang.parser.lisp;

import io.opsit.explang.ASTN;
import io.opsit.explang.ParseCtx;
import java.io.PushbackReader;

public interface IDispatchMacroFunc {
  public abstract ASTN execute(PushbackReader is, ParseCtx pctx, ReadTable rt, char c, int numArg);
}
