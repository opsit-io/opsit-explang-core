package io.opsit.explang;

import java.util.List;

public class ASTNLeaf extends ASTN {
    public ASTNLeaf(Object object, ParseCtx pctx) {
        super(object, pctx);
    }
    public ASTNLeaf(Object object, ParseCtx pctx, boolean isComment) {
        super(object, pctx, isComment);
    }
    
    public ASTNLeaf(Object object, ParseCtx pctx, Exception ex) {
        super(object, pctx, ex);
    }
    
    @Override
    public boolean isList() {
        return false;
    }

    @Override
    public Object getObject() {
        return object;
    }

    public boolean hasProblems() {
    	return null != getProblem();
    }

    // @Override
    // public List<ASTN> getList() {
    // 	throw new CompilationException(pctx,"internal error: list requested on non-list AST Node");		
    // }
}
