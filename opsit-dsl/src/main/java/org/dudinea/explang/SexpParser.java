package org.dudinea.explang;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.dudinea.explang.atom.*;

@SuppressWarnings({"unchecked","rawtypes","serial"})
public class SexpParser implements IParser {

    @Override
    public ASTNList parse(ParseCtx pctx, String input) {
	return parse(pctx, input, Integer.MAX_VALUE);
    }

    public ASTNList parse(ParseCtx pctx, String input, int maxExprs) {
	final InputStream is = new ByteArrayInputStream(input.getBytes());
	return parse(pctx, is, maxExprs);
    }
	
    public ASTNList parse(ParseCtx pctx, InputStream is, int maxExprs) {
	final Reader reader = new InputStreamReader(is);
	return parse(pctx, reader, maxExprs);
    }

    @Override
    public ASTNList parse(ParseCtx pctx, Reader reader, int maxExprs) {
	final List<ASTN> sexp = list(new ASTNList(list(),pctx.clone()));
	boolean inStr = false;
	boolean inComment = false;
	int depth = 0;
	StringBuffer buf = new StringBuffer();
	int lineStart = 0;
	int code;
	ParseCtx spctx = null;
	Exception problem = null;
	try { 
	    for (int i = 0; true  ;i++) {
		if (0 == depth && ((ASTNList)sexp.get(0)).size() >= maxExprs) {
		    break;
		}
		code = reader.read();
		if (code < 0) {
		    break;
		}
		
		pctx.setPos(i-lineStart);
		pctx.setOff(i);
		char chr = (char)code;
		if (inComment) {
		    if (chr=='\n') {
			inComment=false;
		    }
		} else {
		    if (chr=='"') {
			inStr = ! inStr;
			
			if (buf.length() == 0) {
			    spctx = pctx.clone();
			}
			buf.append(chr);
		    } else if ((!inStr) && (chr=='(')) {
			depth++;
			sexp.add(new ASTNList(new ArrayList(), pctx.clone()));
		    } else if ((!inStr) && chr==')') {
			depth--;
			if (depth<0) {
			    sexp.add(new ASTNLeaf(null, pctx, new ParserException(pctx, "Too many right parentheses")));
			}
			if (buf.length()>0) {
			    ((ASTNList)sexp.get(sexp.size() - 1))
				.add(parseAtom(buf.toString(), spctx.clone().upto(pctx)));
			    clearBuf(buf);
			    spctx = null;
			}
			ASTN tmp =  sexp.remove(sexp.size() - 1);
			((ASTNList)sexp.get(sexp.size() - 1)).add(tmp);
		    } else if ((!inStr) && " \n\r\t".indexOf(chr) > -1) {
			if (buf.length() > 0) {
			    ((ASTNList)sexp.get(sexp.size() - 1))
				.add(parseAtom(buf.toString(), spctx.clone().upto(pctx)));
			    clearBuf(buf);
			    spctx = null;
			}
		    } else if ((!inStr) && ';' == chr) {
			inComment = true;
		    } else {
			if (buf.length() == 0) {
			    spctx = pctx.clone();
			}			
			buf.append(chr);
		    }
		}
		if ('\n' == chr) {
		    pctx.setLine(pctx.getLine() + 1);
		    lineStart = i + 1;
		}
	    }

	} catch (IOException ex) {
	    problem = new ParserException(pctx.clone(), "I/O exception", ex);
	}
	if (inStr) {
	    problem = new ParserException(pctx.clone(), "unclosed '\"'");
	}
	if (depth > 0) {
	    problem = new ParserException(pctx.clone(), "unbalanced '('");
	}
	
	if (buf.length() > 0) {
	    ((ASTNList)sexp.get(sexp.size() - 1)).add(parseAtom(buf.toString(), spctx.clone().upto(pctx)));
	}
	ASTNList resultList = (ASTNList)sexp.get(0);
	if (null != problem) {
	    resultList.problem =  problem;
	}
	return resultList;
    }

    
    private ASTN parseAtom(String string, ParseCtx pctx)  {
	final Object[] holder = new Object[1];
	for(AtomParser parser : atomParsers) {
	    try {
		if (parser.parse(string, holder, pctx)) {
		    return new ASTNLeaf(holder[0], pctx);
		}
	    } catch(AtomParseException ex) {
		return new ASTNLeaf(string, pctx, ex);
		//throw new ReaderException(pctx,
		//			  String.format("Failed to parse atom '%s'", string),
		//			  ex);
	    }
	}
	return new ASTNLeaf(string, pctx, new ParserException(String.format("Failed to parse atom '%s'", string)));
	//throw new SexpParserException(pctx,
	//			      String.format("Failed to parse atom '%s'", string));
    }


    protected AtomParser[] atomParsers = new AtomParser[] {
	new NullParser(),
	new BooleanParser(),
	new NumberParser(),
	new StringParser(),
	new KeywordParser(),
	new SymbolParser()
    };
    
    


  

    
    // public ASTN parseExpr(String str) {
    // 	return parseExpr(str, "<EXPRINPUT>");
    // }
    
    // public ASTN parseExpr(String str, String inputName) {
    // 	ParseCtx pctx = new ParseCtx(inputName, 0, 0);
    // 	ASTN exprs = parse(pctx, str);
    // 	List <ASTN> list = exprs.getList();
    // 	if (list.size() != 1) {
    // 	    throw new SexpParserException(new ParseCtx(inputName, 0, 0),
    // 					  String.format("parseExpr: Too many expressions parsed: 1 is expected, by got %d: ",
    // 							list.size()));
    // 	}
    // 	return list.get(0);
    // }

    

    

    
    public String sexpToString(Object obj) {
	if (null == obj) {
	    return "()";
	} else if (obj instanceof List) {
	    StringBuffer str = new StringBuffer();
	    str.append('(');
	    for (Object member : ((List)obj)) {
		if (str.length()>1) {
		    str.append(" ");
		}
		str.append(sexpToString(member));
	    }
	    str.append(')');
	    return str.toString();
	} else if (obj instanceof String) {
	    return String.format("\"%s\"",obj);
	} else {
	    return obj.toString();
	}
    }



    protected void clearBuf(StringBuffer buf) {
	buf.delete(0, buf.length());
    }
    
    protected List list(Object ... objs) {
	List lst = new ArrayList();
	lst.addAll(Arrays.asList(objs));
	return lst;
    }


}

