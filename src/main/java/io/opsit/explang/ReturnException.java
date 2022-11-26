package io.opsit.explang;

public class ReturnException extends RuntimeException {
  public static final long serialVersionUID = 1;
  protected transient Object payload;

  public ReturnException(Object payload) {
    this.payload = payload;
  }

  public Object getPayload() {
    return this.payload;
  }
}
