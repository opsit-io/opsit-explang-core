package org.dudinea.explang;

import java.util.List;

public interface IExpr extends ICompiled {
    public void setParams(List <ICompiled> params) throws InvalidParametersException;
    //public List<Object> evaluateParameters(Compiler.CallChain chain, Compiler.Ctx ctx);
    // FIXME: do we need this in interface?
    //public Object evaluate(Compiler.CallChain chain, List<Object> eargs, Compiler.Ctx ctx);
}
