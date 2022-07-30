package io.opsit.explang;


public class ExecutionException extends RuntimeException {
    public Backtrace backtrace;
    public ExecutionException(Throwable cause) {
	super(cause);
    }

    public ExecutionException(Backtrace backtrace, Throwable cause) {
	super(cause);
	this.backtrace = null == backtrace ? null : backtrace.copy();
    }

    public ExecutionException(String msg) {
	super(msg);
    }

    public ExecutionException(Backtrace backtrace, String msg) {
	super(msg);
	this.backtrace = null == backtrace ? null : backtrace.copy();
    }

    public ExecutionException(Backtrace backtrace, String msg, Throwable t) {
	super(msg, t);
	this.backtrace = null == backtrace ? null : backtrace.copy();
    }

    public Backtrace getBacktrace() {
	return backtrace;
    }
    public void  setBacktrace(Backtrace bt) {
	this.backtrace =  bt;
    }
}
