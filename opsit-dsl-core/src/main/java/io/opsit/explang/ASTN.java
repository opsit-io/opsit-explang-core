package io.opsit.explang;


// FIXME: must be interface?
public abstract class ASTN {
    public ASTN(Object object, ParseCtx pctx) {
        this.object = object;
        this.pctx = pctx;
    }
    public ASTN(Object object, ParseCtx pctx, boolean isComment) {
        this.object = object;
        this.pctx = pctx;
        this.isComment = isComment;
    }

    public ASTN(Object object, ParseCtx pctx, Exception ex) {
        this.object = object;
        this.pctx = pctx;
        this.problem = ex;
    }
    
    protected ParseCtx pctx;
    protected Object object;
    protected boolean isComment = false;
    protected ASTNList parent;
    // FIXME: need accessor functions
    public  Exception problem;

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
    //  if (!isList()) {
    //      return object;
    //  }
    //  throw new CompilationException(pctx,"internal error: object requested on list AST Node");
    // }
    
    //public abstract List<ASTN> getList() ;
    //  if (isList()) {
    //      return (List<ASTN>) object;
    //  }
    //  throw new CompilationException(pctx,"internal error: list requested on non-list AST Node");
    // }

    public ASTN getParent() {
        return this.parent;
    }

    public String toString() {
        return
            (null != problem  ? "<"+problem+">" : "") +
            ((null != object) ? object.toString() : "<null>");
    }

    public String toStringWithPos() {
        return
            "@"+this.getPctx()+"@"+
            (null != problem  ? "<"+problem+">" : "") +
            ((null != object) ? object.toString() : "<null>");
    }

    public abstract interface  Walker {
        public void walk(ASTN node);
    }

    public void dispatchWalker(Walker walker) {
        walker.walk(this);
        if (this.isList()) {
            for(ASTN node : ((ASTNList)this)) {
                if (node != null) {
                    node.dispatchWalker(walker);
                }
            }
        }
    }
    
}
