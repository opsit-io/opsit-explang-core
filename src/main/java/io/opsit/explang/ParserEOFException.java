package io.opsit.explang;

public class ParserEOFException extends ParserException {
  public static final long serialVersionUID = 1L;

  public ParserEOFException(String msg) {
    super(msg);
  }

  public ParserEOFException(ParseCtx pctx, String msg) {
    super(pctx, msg);
  }

  public ParserEOFException(ParseCtx pctx, String msg, Throwable throwable) {
    super(pctx, msg, throwable);
  }
}
