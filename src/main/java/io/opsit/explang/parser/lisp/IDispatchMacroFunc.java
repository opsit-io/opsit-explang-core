package io.opsit.explang.parser.lisp;

import java.io.PushbackReader;
import io.opsit.explang.ParseCtx;
import io.opsit.explang.ASTN;

public interface IDispatchMacroFunc {
  public abstract ASTN execute(PushbackReader is,
                               ParseCtx pctx,
                               ReadTable rt,
                               char c,
                               int numArg);
            
}
