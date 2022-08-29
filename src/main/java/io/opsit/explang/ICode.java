package io.opsit.explang;

public interface ICode {
  public ICompiled getInstance();

  public boolean isBuiltIn();

  public String getDocstring();

  public String getArgDescr();

  public ArgSpec getArgSpec();

  public String getCodeType();

  public String getDefLocation();

  public String getPackageName();
}
