package io.opsit.explang;


public class NotImplementedException extends ExecutionException  {
  public static final long serialVersionUID = 1L;

  public NotImplementedException(Backtrace backtrace, String msg) {
    super(backtrace, msg);
  }

  public NotImplementedException(String msg) {
    super(msg);
  }
}
