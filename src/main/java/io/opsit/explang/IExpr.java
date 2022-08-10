package io.opsit.explang;

import java.util.List;

public interface IExpr extends ICompiled {
  public void setParams(List<ICompiled> params) throws InvalidParametersException;
}
