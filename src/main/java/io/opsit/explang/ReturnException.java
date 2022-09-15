package io.opsit.explang;

public class ReturnException extends RuntimeException {
  protected Object payload;
  public ReturnException(Object payload) {
    this.payload = payload;
  }

  public Object getPayload() {
    return this.payload;
  }
}
