package io.opsit.explang;

import java.io.Reader;

public interface IREPL {
  public Object execute(Reader reader, String inputName) throws Exception;

  public void setParser(IParser parser);

  public IParser getParser();

  public void setVerbose(boolean val);

  public boolean getVerbose();

  public io.opsit.explang.Compiler getCompiler();

  public void setCompiler(io.opsit.explang.Compiler compiler);

  public void setLineMode(boolean val);

  public boolean getLineMode();

  public void setObjectWriter(IObjectWriter ow);

  public IObjectWriter getObjectWriter();
}
