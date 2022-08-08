package io.opsit.explang;

public class Symbol {
  public final String value;
  public Symbol(String str) {
    value = str;
    if (null==str || str.length()==0) {
      throw new ExecutionException("Symbol cannot be null or empty string!");
    }
  }

  public String getName() {
    return value;
  }
    
  @Override
  public boolean equals(Object other) {
    return null!=other &&
      (other instanceof Symbol) &&
      ((Symbol)other).value.equals(this.value);
  }

  public String toString() {
    //return value.toUpperCase();
    return value;
  }
}
