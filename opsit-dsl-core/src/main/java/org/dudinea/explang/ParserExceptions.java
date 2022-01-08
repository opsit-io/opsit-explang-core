package org.dudinea.explang;

import java.util.List;
import java.util.ArrayList;


public class ParserExceptions extends ParserException {
    final protected List<ParserException> errList;

    @Override
    public List<String> getMessages() {
	final List<String> result = new ArrayList(errList.size());
	for (ParserException e : errList) {
	    result.add(e.getMessage());
	}
	return result;
    }
    
    public ParserExceptions(ParseCtx pctx,
			    List<ParserException> errList) {
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


