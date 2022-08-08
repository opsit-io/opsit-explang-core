
package io.opsit.explang.parser.lisp;

import java.io.PushbackReader;

import io.opsit.explang.ParseCtx;
import io.opsit.explang.ASTN;

public interface IReaderMacroFunc {
  public ASTN execute(char c,
                      PushbackReader is,
                      ReadTable rt,
                      ParseCtx pctx) ;
}
