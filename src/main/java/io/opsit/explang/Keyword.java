package io.opsit.explang;

public class Keyword extends Symbol {

  public static Keyword UPCASE = new Keyword(":UPCASE");
  public static Keyword DOWNCASE = new Keyword(":DOWNCASE");
  public static Keyword PRESERVE = new Keyword(":PRESERVE");

  /**
   * Construct Keyword from its string  representation starting with ':'.
   */
  public Keyword(String val) {
    super(val);
    if (null == val || val.length() < 2 || (!val.startsWith(":"))) {
      throw new RuntimeException("Keyword cannot have value " + val);
    }
  }
}
