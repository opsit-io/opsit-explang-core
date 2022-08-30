package io.opsit.explang;

import java.io.Reader;
import java.util.List;

public interface IParser {
  public ASTNList parse(ParseCtx pxt, Reader reader, int maxExprs);

  public ASTNList parse(ParseCtx pxt, String input);

  public ASTNList parse(ParseCtx pxt, String input, int maxExprs);

  public boolean supportREPLStream();

  public String formatArgSpec(List<String> spec);
}
