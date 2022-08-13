package io.opsit.explang;

public class InvalidParametersException extends Exception {
  public static final long serialVersionUID = 1L;

  public ParseCtx getParseCtx() {
    return pctx;
  }

  protected ParseCtx pctx;

  public InvalidParametersException(String msg) {
    super(String.format("invalid parameters: %s", msg));
  }

  public InvalidParametersException(ParseCtx pctx, String msg) {
    super(msg);
    this.pctx = pctx;
  }
}
