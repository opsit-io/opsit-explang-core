package org.dudinea.explang;


import java.util.List;
import java.util.ArrayList;

public class Backtrace {
    //protected int size = 1024;
    protected List<Frame> frames;
    public Backtrace() {
	frames = new ArrayList<Frame>(1024);
    }

    public Backtrace copy() {
	Backtrace newCallChain = new Backtrace();
	newCallChain.frames.addAll(frames);
	return newCallChain;
    }
    
    public List<Frame> getFrames() {
	return frames;
    }

    public Frame pop() {
	return frames.remove(frames.size() - 1);
	//return null;
    }
	
    private void push(Frame f) {
    	frames.add(f);
    }

    public void push(String fName, ParseCtx pctx, Compiler.ICtx ctx) {
	push(new Frame(fName, pctx, ctx));
	//push(null);
    }

    public String toString() {
	final List<Frame> frames =  this.getFrames();
	final int length = frames.size();
	int maxFun=0; int maxPos = 0;
	boolean needCtx [] = new boolean[length];
	Compiler.ICtx lastCtx = null;
	for (int i = 0; i < length ; i++) {
	    Frame f = frames.get(i);
	    maxFun = Utils.max(Utils.safeLen(f.fName), maxFun);
	    maxPos = Utils.max(Utils.safeLen(""+f.pctx), maxPos);
	    needCtx[i] = (f.ctx != lastCtx);
	    lastCtx = f.ctx;
	}
	final int nWidth = Integer.toString(length).length();
	final StringBuilder fb = new StringBuilder();
	fb.append("%-").append(nWidth).append("d: ");
	fb.append("%-").append(maxFun+2).append("s ");
	fb.append("%-").append(maxPos).append("s%s");
	fb.append("%s\n");
	final String frameFmt = fb.toString();
	final StringBuilder b = new StringBuilder();
	for (int i = length - 1; i >= 0 ; i--) {
	    Frame f = frames.get(i);
	    b.append(String.format(frameFmt,
				   (length - i),
				   f.fName,
				   f.pctx,
				   needCtx[i] ? " " : "",
				   needCtx[i] ? f.ctx.toStringShort() : ""));
	}
	return b.toString();
    }

    public static class Frame {
	protected ParseCtx pctx;
	protected Compiler.ICtx ctx;
	protected String fName;
	public Frame(String fName, ParseCtx pctx, Compiler.ICtx ctx) {
	    this.fName = fName;
	    this.pctx = pctx;
	    this.ctx = ctx;
	}

	public String toString() {
	    return String.format("%15s [%12s] %s", fName, pctx, ctx);
	}
	public String toStringShort() {
	    return String.format("%14s [%10s]",fName, pctx);
	}
    }


}
