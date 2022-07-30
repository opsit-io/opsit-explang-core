package io.opsit.explang;

import java.util.List;

public interface ICode {
    //public Object evaluate(Backtrace backtrace, Compiler.Ctx ctx);
    public ICompiled getInstance();
    public boolean isBuiltIn();
    public String getDocstring();
    public String getArgDescr();
    public ArgSpec getArgSpec();
    public String getCodeType();
    public String getDefLocation();
    //public void setDebugInfo(ParseCtx pctx);
    //public void setName(String str);
    //public ParseCtx getDebugInfo();
}
