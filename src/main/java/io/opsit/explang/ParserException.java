package io.opsit.explang;

import static io.opsit.explang.Utils.list;

import java.util.List;

public class ParserException extends Exception {
  public static final long serialVersionUID = 1L;
  protected ParseCtx pctx;
  protected String orgMsg;

  public ParseCtx getPctx() {
    return pctx;
  }

  public String getOrgMessage() {
    return orgMsg;
  }
  

  public List<String> getMessages() {
    return list(this.getMessage());
  }

  /**
   * Make parser exception for general parser error w/o specific context.
   */
  public ParserException(String msg) {
    super(String.format("Parser: %s", msg));
    this.orgMsg = msg;
    this.pctx = new ParseCtx("UNKNOWN");
  }

  /**
   * Make parser exception given code location and message.
   */  
  public ParserException(ParseCtx pctx, String msg) {
    super(String.format("%s:%d:%d: Parser: %s", pctx.input, pctx.getLine(), pctx.getPos(), msg));
    this.orgMsg = msg;
    this.pctx = pctx;
  }

  /**
   * Make parser exception given code location, message and original exception.
   */
  public ParserException(ParseCtx pctx, String msg, Throwable thr) {
    super(String.format("%s:%d:%d: Parser: %s",
                        pctx.input, pctx.getLine(), pctx.getPos(), msg),  thr);
    this.orgMsg = msg;
    this.pctx = pctx;
  }
}
