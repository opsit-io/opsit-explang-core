package io.opsit.explang;

/**
 * Parse Contexts represents location of parsed source code.
 *
 */
public class ParseCtx {
  public String input = "<INPUT>";
  private int line = -1;
  private int pos = -1;
  private int off = -1;
  private int len = 0;

  /**
   * Create Parse Context.
   *
   * <p>Unknown locations pareameters must be set to -1.
   * 
   * @param input name of source file or stream
   * @line line number
   * @pos position in the line
   * @off offest from start of the file/stream
   * @len length of the fragment
   */
  public ParseCtx(String input, int line, int pos, int off, int len) {
    this.input = input;
    this.line = line;
    this.pos = pos;
    this.off = off;
    this.len = len;
  }

  @Override
  public ParseCtx clone() {
    return new ParseCtx(input, line, pos,off, len);
  }
  
  public ParseCtx(String input) {
    this(input, 0, -1, -1, 0);
  }
    
  public String toString() {
    return String.format("%s:line=%d:pos=%d:o=%d:len=%d",input,line,pos,off,len);
  }

  public int getOff() {
    return off;
  }
    
  public int setOff(int off) {
    return this.off = off;
  }
    
  public int getLine() {
    return line;
  }

  public void setLine(int line) {
    this.line = line;
  }

  public int getPos() {
    return pos;
  }

  public void setPos(int pos) {
    this.pos = pos;
  }

  public ParseCtx upto(ParseCtx pctx) {
    this.len = pctx.off + 1 - this.off;
    return this;
  }
}
