package org.dudinea.explang;

import java.util.List;

public interface IForm  extends ICompiled {
	public void setRawParams(ASTNList  params)
	    throws InvalidParametersException;
}
