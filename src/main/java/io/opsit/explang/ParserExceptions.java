package io.opsit.explang;

import java.util.ArrayList;
import java.util.List;

public class ParserExceptions extends ParserException {
  public static final long serialVersionUID = 1L;
  protected final List<ParserException> errList;

  @Override
  public List<String> getMessages() {
    final List<String> result = new ArrayList<String>(errList.size());
    for (ParserException e : errList) {
      result.add(e.getMessage());
    }
    return result;
  }

  /**
   * Make summary exception given list of parser exceptions.
   */
  public ParserExceptions(ParseCtx pctx, List<ParserException> errList) {
    super(pctx, mkMessage(pctx, errList));
    this.pctx = pctx;
    this.errList = errList;
  }

  private static String mkMessage(ParseCtx pctx, List<ParserException> errList) {
    StringBuilder buf = new StringBuilder((errList.size() + 1) * 64);
    buf.append(String.format("Encountered %d errors while parsing", errList.size()));
    for (ParserException e : errList) {
      buf.append("\n");
      buf.append(null == e ? "<null>" : e.toString());
    }
    return buf.toString();
  }
}
