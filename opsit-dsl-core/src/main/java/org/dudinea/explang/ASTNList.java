package org.dudinea.explang;

import java.util.Iterator;
import java.util.List;

public class ASTNList extends ASTN implements Iterable<ASTN> {
    protected boolean isMultiExpr = false;
    protected boolean isLiteralList = false;

    public ASTNList(List object, ParseCtx pctx) {
        super(object, pctx);
    }
    public ASTNList(List object, ParseCtx pctx, boolean isComment) {
        super(object, pctx, isComment);
    }

    public boolean isMultiExpr() {
        return isMultiExpr;
    }

    public void setMultiExpr(boolean multiExpr) {
        this.isMultiExpr = multiExpr;
    }
    
    @Override
    public boolean isList() {
        return true;
    }

    @Override
    public Object getObject() {
        throw new CompilationException(pctx,"internal error: object requested on list AST Node");
    }

    @Override
    public boolean hasProblems() {
        if (null != getProblem()) {
            return true;
        }
        for( ASTN astn: (List<ASTN>)object) {
            if (astn.hasProblems()) {
                return true;
            }
        }
        return false;
    }
    
    //@Override
    public List<ASTN> getList() {
        return (List<ASTN>)object;
    }

    @Override
    public Iterator<ASTN> iterator() {
        return ((List)object).iterator();
    }

    public ASTN get(int i) {
        return (ASTN)((List)object).get(i);
    }

    public void addAll(ASTNList astns) {
        for (ASTN astn : astns) {
            add(astn);
        }
    }
    
    public void add(ASTN astn) {
        //if (null == astn.parent) {
        astn.parent = this;
        ((List<ASTN>)object).add(astn);
        //} else {
        //throw new RuntimeException("ASTN node already has parent");
        // }
    }

    public int size() {
        return ((List<ASTN>)object).size();
    }

    public ASTNList subList(int start, int end) {
        final List<ASTN> subList = ((List<ASTN>)object).subList(start, end);
        ASTNList subASTNlist = new ASTNList(subList, pctx, isComment);
        return subASTNlist;
    }

    public boolean isEmpty() {
        return ((List<ASTN>)object).isEmpty();
    }

    @Override
    public String toStringWithPos() {
        StringBuilder b = new StringBuilder();
        b.append("@").append(this.getPctx())
            .append("@");
        if (null != problem) {
            b.append("<"+problem+">");
        }
        b.append("(");
        final int len = getList().size();
        for (int i = 0; i < len ; i++) {
            if (i > 0) {
                b.append(" ");
            }
            b.append(getList().get(i).toStringWithPos());
        }
        b.append(")");
        return b.toString();
    }

    @Override
    public String toString() {
        return
            (null != problem  ? "<"+problem+">" : "") +
            ((null != object) ? astnlistToString((List<ASTN>)object)  : "<null>");
    }

    protected String astnlistToString(List<ASTN> lst) {
        final StringBuilder b = new StringBuilder();
        b.append(this.isLiteralList() ? "[" : "(");
        for (Object obj : lst) {
            if (b.length() > 1) {
                b.append(", ");
            }
            b.append(obj.toString());
        }
        b.append(this.isLiteralList ? "]" : ")");
        return b.toString();
    }

    public void setIsLiteralList(boolean val) {
        this.isLiteralList = val;
    }

    public boolean isLiteralList() {
        return this.isLiteralList;
    }
}
