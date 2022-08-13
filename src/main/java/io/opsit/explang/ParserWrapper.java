package io.opsit.explang;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

public class ParserWrapper {
  IParser parser;
  public static String DEFAULT_INPUT_NAME = "<INPUT>";

  public ParserWrapper(IParser parser) {
    this.parser = parser;
  }

  public ASTNList parse(String txt) throws ParserException {
    return parse(txt, DEFAULT_INPUT_NAME);
  }

  public ASTNList parse(String txt, String inputName) throws ParserException {
    ParseCtx pctx = new ParseCtx(inputName);
    return (ASTNList) parser.parse(pctx, txt);
  }

  public ASTNList parse(InputStream is) {
    return (ASTNList) parse(is, DEFAULT_INPUT_NAME);
  }

  /**
   * Parse input file with given name as name of input.
   */
  public ASTNList parse(File file, String inputName) throws ParserException {
    ParseCtx pctx = new ParseCtx(inputName);
    InputStream is = null;
    try {
      is = new FileInputStream(file);
      InputStreamReader reader = new InputStreamReader(is);
      return (ASTNList) parser.parse(pctx, reader, Integer.MAX_VALUE);
    } catch (FileNotFoundException ex) {
      throw new ParserException(pctx, "I/O exception", ex);
    } finally {
      if (null != is) {
        try {
          is.close();
        } catch (IOException ex) {
          throw new ParserException(pctx, "I/O exception at stream close", ex);
        }
      }
    }
  }

  public ASTNList parse(File file) throws ParserException {
    return parse(file, file.getName());
  }

  public ASTNList parse(InputStream is, String inputName) {
    Reader reader = new InputStreamReader(is);
    return (ASTNList) parser.parse(new ParseCtx(inputName), reader, Integer.MAX_VALUE);
  }
}
