package io.opsit.explang;

import java.io.Reader;

public interface IParser {
  public ASTNList parse(ParseCtx pxt, Reader r, int maxExprs);
  public ASTNList parse(ParseCtx pxt, String input);
  public ASTNList parse(ParseCtx pxt, String input, int maxExprs);
}
