package io.opsit.explang;

public class ParseCtx {
    public String input = "<INPUT>";
    private int line = -1;
    private int pos = -1;
    private int off = -1;
    private int len = 0;

    public ParseCtx(String input, int line, int pos, int off, int len) {
	this.input = input;
	this.line = line;
	this.pos = pos;
	this.off = off;
	this.len = len;
    }

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
    
    /**
     * @return the line
     */
    public int getLine() {
	return line;
    }

    /**
     * @param line the line to set
     */
    public void setLine(int line) {
	this.line = line;
    }

    /**
     * @return the pos
     */
    public int getPos() {
	return pos;
    }

    /**
     * @param pos the pos to set
     */
    public void setPos(int pos) {
	this.pos = pos;
    }

    public ParseCtx upto(ParseCtx pctx) {
	this.len = pctx.off + 1 - this.off;
	return this;
    }
}
