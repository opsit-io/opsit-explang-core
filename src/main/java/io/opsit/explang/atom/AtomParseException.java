package io.opsit.explang.atom;

import io.opsit.explang.ParseCtx;

public class AtomParseException extends Exception {
  ParseCtx pctx;

  public AtomParseException(String msg) {
    super(String.format("atom parser error: %s", msg));
  }

  /**
   * Create parse exception given source code location and message.
   */
  public AtomParseException(ParseCtx pctx, String msg) {
    super(
        String.format(
            "%s:%d:%d: atom parser error: %s", pctx.input, pctx.getLine(), pctx.getPos(), msg));
    this.pctx = pctx;
  }

  /**
   * Create parse exception given source code location, message and throwable.
   */
  public AtomParseException(ParseCtx pctx, String msg, Throwable t) {
    super(
        String.format(
            "%s:%d:%d: atom parser got exception: %s",
            pctx.input, pctx.getLine(), pctx.getPos(), msg),
        t);
    this.pctx = pctx;
  }
}
