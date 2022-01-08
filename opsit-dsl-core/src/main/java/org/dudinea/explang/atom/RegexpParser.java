package org.dudinea.explang.atom;

import org.dudinea.explang.ParseCtx;
import java.util.regex.Pattern;

public  class RegexpParser implements AtomParser {
    public boolean parse(String str, Object[]holder, ParseCtx pctx) {
	if ((str.length() < 3) ||
	    (!str.startsWith("r\""))) {
	    return false;
	}
	StringBuilder sb = new StringBuilder();
	int flags = 0;
	//Exception problem = null;
	boolean inQuote = false;
	for (int i =1; i < str.length(); i++) {
	    char c = str.charAt(i);
	    if (c=='"') {
		inQuote = !inQuote;
		continue;
	    }
	    if (!inQuote) {
		switch (c) {
		case 'd':
			flags |= Pattern.UNIX_LINES;
			break;
		case 'i':
			flags |= Pattern.CASE_INSENSITIVE;
			break;
		case 'x':
			flags |= Pattern.COMMENTS;
			break;
		case 'm':
			flags |= Pattern.MULTILINE;
			break;
		case 'l':
			flags |= Pattern.LITERAL;
			break;
		case 's':
			flags |= Pattern.DOTALL;
			break;
		case 'u':
			flags |= Pattern.UNICODE_CASE;
			break;
		case 'c':
			flags |= Pattern.CANON_EQ;
			break;
		case 'U':
			flags |= Pattern.UNICODE_CHARACTER_CLASS;
			break;			
		default:
		    return false;
		    
		}
		continue;
	    }
	    if (c== '\\') {
		i++;
		if (i < str.length()) {
		    c = str.charAt(i);
		    switch (c) {
		    case '\"':
			break;
		    default:
			sb.append('\\'); 
			break;
		    }
		} else {
		    return false;
		}
	    }
	    sb.append(c); 
	    continue;
	}
	if (inQuote) {
	    return false;
	}
	//System.out.println("RegexpParser: str='" + sb.toString() + "', flags=" + flags); 
	holder[0] = Pattern.compile(sb.toString(), flags);
	return true;
    }
}
