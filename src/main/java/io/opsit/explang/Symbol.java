package io.opsit.explang;

public class Symbol {
  public final String value;

  /**
   * Construct symbol from its string representation.
   */
  public Symbol(String str) {
    value = str;
    if (null == str || str.length() == 0) {
      throw new ExecutionException("Symbol cannot be null or empty string!");
    }
  }

  public String getName() {
    return value;
  }

  @Override
  public boolean equals(Object other) {
    return null != other && (other instanceof Symbol) && ((Symbol) other).value.equals(this.value);
  }

  public String toString() {
    // return value.toUpperCase();
    return value;
  }

  @Override
  public int hashCode() {
    return value.hashCode();
  }
}
