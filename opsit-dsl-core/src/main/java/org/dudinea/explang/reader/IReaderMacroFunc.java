
package org.dudinea.explang.reader;

import java.io.PushbackReader;

import org.dudinea.explang.ParseCtx;
import org.dudinea.explang.ASTN;

public interface IReaderMacroFunc {
    public ASTN execute(char c,
			PushbackReader is,
			ReadTable rt,
			ParseCtx pctx) ;
}
