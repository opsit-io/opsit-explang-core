package org.dudinea.explang.reader;

import java.io.PushbackReader;
import org.dudinea.explang.ParseCtx;
import org.dudinea.explang.ASTN;

public interface IDispatchMacroFunc {
    public abstract ASTN execute(PushbackReader is,
				 ParseCtx pctx,
				 ReadTable rt,
				 char c,
				 int numArg);
			
}
