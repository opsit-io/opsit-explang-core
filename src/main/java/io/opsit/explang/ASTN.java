package io.opsit.explang;

// FIXME: must be interface?
/**
 * Abstract syntax tree node containing some code object.
 *
 * <p>Represents node in abstract syntax tree. A node may be a
 * leaf node containing a or a list node containing several ASTN nodes.
 */
public abstract class ASTN {
  protected ParseCtx pctx;
  protected Object object;
  protected boolean isComment = false;
  // link to parent ASTN list
  protected ASTNList parent;
  // FIXME: need accessor functions
  public Exception problem;

  /**
   * Make an ASTN node for a code node.
   */
  public ASTN(Object object, ParseCtx pctx) {
    this.object = object;
    this.pctx = pctx;
  }

  /**
   * Make an ASTN node, possible for a comment node.
   */
  public ASTN(Object object, ParseCtx pctx, boolean isComment) {
    this.object = object;
    this.pctx = pctx;
    this.isComment = isComment;
  }

  /**
   * Make an ASTN node for an invalid expression.
   */
  public ASTN(Object object, ParseCtx pctx, Exception ex) {
    this.object = object;
    this.pctx = pctx;
    this.problem = ex;
  }

  public abstract boolean hasProblems();

  public Exception getProblem() {
    return problem;
  }

  public ParseCtx getPctx() {
    return pctx;
  }

  public abstract boolean isList();

  public boolean isComment() {
    return isComment;
  }

  public abstract Object getObject();

  public ASTN getParent() {
    return this.parent;
  }

  @Override
  public String toString() {
    return (null != problem ? "<" + problem + ">" : "")
        + ((null != object) ? object.toString() : "<null>");
  }

  /**
   * Print out a node with source code location and error messages.
   */
  public String toStringWithPos() {
    return "@"
        + this.getPctx()
        + "@"
        + (null != problem ? "<" + problem + ">" : "")
        + ((null != object) ? object.toString() : "<null>");
  }

  public abstract interface Walker {
    public void walk(ASTN node);
  }

  /**
   * dispatch walker run on ASTN tree.
   */
  public void dispatchWalker(Walker walker) {
    walker.walk(this);
    if (this.isList()) {
      for (ASTN node : ((ASTNList) this)) {
        if (node != null) {
          node.dispatchWalker(walker);
        }
      }
    }
  }
}
