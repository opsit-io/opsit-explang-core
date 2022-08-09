package io.opsit.explang.parser.lisp;

import io.opsit.explang.ASTN;
import io.opsit.explang.ParseCtx;

import java.io.PushbackReader;

public interface IReaderMacroFunc {
  public ASTN execute(char c, PushbackReader is, ReadTable rt, ParseCtx pctx);
}
