package org.dudinea.explang.reader;

import java.util.HashMap;
import java.util.Map;
import java.io.Reader;

import org.dudinea.explang.ICompiled;
import org.dudinea.explang.Keyword;
import org.dudinea.explang.Utils;
import org.dudinea.explang.ParserException;

public class ReadTable {

    protected static class DispatchTable  {
	protected final Map<Character, IDispatchMacroFunc> functions;

	public DispatchTable(Map <Character, IDispatchMacroFunc> functions) {
	    this.functions = functions;
	}

	public DispatchTable() {
	    functions = new HashMap<Character, IDispatchMacroFunc>();
	}

	public DispatchTable(DispatchTable dt)	{
	    functions = new HashMap(dt.functions);
	}
    }

    
    public static final byte SYNTAX_TYPE_CONSTITUENT           = 0;
    public static final byte SYNTAX_TYPE_WHITESPACE            = 1;
    public static final byte SYNTAX_TYPE_TERMINATING_MACRO     = 2;
    public static final byte SYNTAX_TYPE_NON_TERMINATING_MACRO = 3;
    public static final byte SYNTAX_TYPE_SINGLE_ESCAPE         = 4;
    public static final byte SYNTAX_TYPE_MULTIPLE_ESCAPE       = 5;

    //default must be SYNTAX_TYPE_CONSTITUENT;
    protected final Map<Character,Byte> syntax = 
	Utils.map('\t',    SYNTAX_TYPE_WHITESPACE,
		  '\n',   SYNTAX_TYPE_WHITESPACE, // linefeed
		  '\f',   SYNTAX_TYPE_WHITESPACE, // form feed
		  '\r',   SYNTAX_TYPE_WHITESPACE, // return
		  ' ',  SYNTAX_TYPE_WHITESPACE,
		  '"',  SYNTAX_TYPE_TERMINATING_MACRO,
		  '\'', SYNTAX_TYPE_TERMINATING_MACRO,
		  '(',  SYNTAX_TYPE_TERMINATING_MACRO,
		  ')',  SYNTAX_TYPE_TERMINATING_MACRO,
		  ',',  SYNTAX_TYPE_TERMINATING_MACRO,
		  ',',  SYNTAX_TYPE_TERMINATING_MACRO,
		  '`',  SYNTAX_TYPE_TERMINATING_MACRO,
		  '#',  SYNTAX_TYPE_NON_TERMINATING_MACRO,
		  '\\', SYNTAX_TYPE_SINGLE_ESCAPE,
		  '|',  SYNTAX_TYPE_MULTIPLE_ESCAPE);
		  
	
    protected final Map<Character,IReaderMacroFunc> readerMacroFunctions =
	Utils.map(';'  , LispReader.READ_COMMENT,
		  '"'  , LispReader.READ_STRING,
		  '('  , LispReader.READ_LIST,
		  ')'  , LispReader.READ_RIGHT_PAREN,
		  '\'' , LispReader.READ_QUOTE,
		  '#'  , LispReader.READ_DISPATCH_CHAR,
		  '`'  , LispReader.BACKQUOTE_MACRO,
		  ','  , LispReader.COMMA_MACRO);

    // BACKQUOTE-MACRO and COMMA-MACRO are defined in backquote.lisp.
    //readerMacroFunctions['`']  = Symbol.BACKQUOTE_MACRO;
    //readerMacroFunctions[',']  = Symbol.COMMA_MACRO;

    public final IReaderMacroFunc getReaderMacroFunction(char c) {
	return readerMacroFunctions.get(c);
    }

    protected Map<Character, DispatchTable> dispatchTables;
    protected Keyword readtableCase;
    
    public ReadTable()
    {
	initialize();
    }

    protected void initialize()
    {
	DispatchTable dt = new DispatchTable(Utils.map('(',  LispReader.SHARP_LEFT_PAREN,
						       '*', LispReader.SHARP_STAR,
						       '.', LispReader.SHARP_DOT,
						       ':', LispReader.SHARP_COLON,
						       'A', LispReader.SHARP_A,
						       'B', LispReader.SHARP_B,
						       'C', LispReader.SHARP_C,
						       'O', LispReader.SHARP_O,
						       'P', LispReader.SHARP_P,
						       'R', LispReader.SHARP_R,
						       'S', LispReader.SHARP_S,
						       'X', LispReader.SHARP_X,
						       '\'', LispReader.SHARP_QUOTE,
						       '"', LispReader.SHARP_QMARK,
						       '\\', LispReader.SHARP_BACKSLASH,
						       '|', LispReader.SHARP_VERTICAL_BAR,
						       ')', LispReader.SHARP_ILLEGAL,
						       '<', LispReader.SHARP_ILLEGAL,
						       ' ', LispReader.SHARP_ILLEGAL,
						       8, LispReader.SHARP_ILLEGAL, // backspace
						       9, LispReader.SHARP_ILLEGAL, // tab
						       10, LispReader.SHARP_ILLEGAL, // newline, linefeed
						       12, LispReader.SHARP_ILLEGAL, // page
						       13, LispReader.SHARP_ILLEGAL)); // return
	dispatchTables = Utils.map();
	dispatchTables.put('#', dt);
	//readtableCase = Keyword.UPCASE;
	readtableCase = Keyword.PRESERVE;
    }

    public final IDispatchMacroFunc getDispatchMacroCharacter(char dispChar, char subChar)
     throws ParserException {
	DispatchTable dispatchTable = dispatchTables.get(dispChar);
	if (dispatchTable == null) {
	    throw new ParserException(dispChar + " is not a dispatch character.");
	}
	final IDispatchMacroFunc function = dispatchTable.functions.get(subChar);
	//return (function != null) ? function : NIL;
	return function;
    }

    public final boolean isWhitespace(char c)  {
	return getSyntaxType(c) == SYNTAX_TYPE_WHITESPACE;
    }

    public final byte getSyntaxType(char c) {
	final Byte t =  syntax.get(c);
	return null == t ?  SYNTAX_TYPE_CONSTITUENT : t;
    }

    public final boolean isInvalid(char c)
    {
	switch (c)
	    {
	    case 8:
	    case 9:
	    case 10:
	    case 12:
	    case 13:
	    case 32:
	    case 127:
		return true;
	    default:
		return false;
	    }
    }

    public final Keyword getReadtableCase()
    {
	return readtableCase;
    }

    public final void checkInvalid(char c)  throws ParserException {
	// "... no mechanism is provided for changing the constituent trait of a
	// character." (2.1.4.2)
	if (isInvalid(c)) {
	    StringBuilder sb = new StringBuilder("Invalid character");
	    sb.append(" \\");
	    sb.append(c);
	    throw new ParserException(sb.toString());
	}
    }

}
