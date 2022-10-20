package io.opsit.explang;

public class OperatorDesc {
  protected String name;
  protected String []usages;
  protected String function;

  /**
   * Create OperatorDesc given it components.
   */
  public OperatorDesc(String name, String function, String... usages) {
    this.name = name;
    this.usages = usages;
    this.function = function;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String[] getUsages() {
    return usages;
  }

  public void setUsages(String[] usages) {
    this.usages = usages;
  }

  public String getFunction() {
    return function;
  }

  public void setFunction(String val) {
    this.function = val;
  }

  @Override
  public String toString() {
    final StringBuilder buf = new StringBuilder(64);
    buf.append(this.name);
    if (null != usages && usages.length > 0) {
      buf.append(": ");
      if (usages[0].contains("\n")) {
        buf.append("\n");
      }
      buf.append(String.join("; ", usages));
    }
    return buf.toString();
  }
}
