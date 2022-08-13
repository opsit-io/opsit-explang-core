package io.opsit.explang;

public class CompilationException extends RuntimeException {
  public static final long serialVersionUID = 1L;
  ParseCtx pctx;

  /**
   * Construct compilation exception for a general error that is not
   * associated with specific place in code.
   */
  public CompilationException(String msg) {
    super(String.format("compile error: %s", msg));
  }

  /** Construct compilation exception.
   * @param pctx parse context with error
   * @param msg error message
   */
  public CompilationException(ParseCtx pctx, String msg) {
    super(String.format("%s:%d:%d: compile error: %s",
                        (null == pctx ? "<INPUT>" : pctx.input),
                        (null == pctx ? 0 : pctx.getLine()),
                        (null == pctx ? 0 : pctx.getPos()), msg));
    this.pctx = pctx;
  }
}
