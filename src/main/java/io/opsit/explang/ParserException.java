package io.opsit.explang;

import static io.opsit.explang.Utils.list;

import java.util.List;

public class ParserException extends Exception {
  public static final long serialVersionUID = 1L;
  protected ParseCtx pctx;

  public List<String> getMessages() {
    return list(this.getMessage());
  }

  public ParserException(String msg) {
    super(String.format("Parser: %s", msg));
  }

  /**
   * Make parser exception given code location and message.
   */  
  public ParserException(ParseCtx pctx, String msg) {
    super(String.format("%s:%d:%d: Parser: %s", pctx.input, pctx.getLine(), pctx.getPos(), msg));
    this.pctx = pctx;
  }

  /**
   * Make parser exception given code location, message and original exception.
   */
  public ParserException(ParseCtx pctx, String msg, Throwable thr) {
    super(String.format("%s:%d:%d: Parser: %s",
                        pctx.input, pctx.getLine(), pctx.getPos(), msg),  thr);
    this.pctx = pctx;
  }
}
