package io.opsit.explang;

public interface IForm  extends ICompiled {
  public void setRawParams(ASTNList  params)
    throws InvalidParametersException;
}
