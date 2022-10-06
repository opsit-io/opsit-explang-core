package io.opsit.explang.parser.lisp;

import io.opsit.explang.Keyword;
import io.opsit.explang.ParserException;
import io.opsit.explang.Utils;
import java.util.HashMap;
import java.util.Map;

public class ReadTable {

  protected static class DispatchTable {
    protected final Map<Character, IDispatchMacroFunc> functions;

    public DispatchTable(Map<Character, IDispatchMacroFunc> functions) {
      this.functions = functions;
    }

    public DispatchTable() {
      this.functions = new HashMap<Character, IDispatchMacroFunc>();
    }

    public DispatchTable(DispatchTable dt) {
      this.functions = new HashMap<Character, IDispatchMacroFunc>(dt.functions);
    }
  }

  public static final byte SYNTAX_TYPE_CONSTITUENT = 0;
  public static final byte SYNTAX_TYPE_WHITESPACE = 1;
  public static final byte SYNTAX_TYPE_TERMINATING_MACRO = 2;
  public static final byte SYNTAX_TYPE_NON_TERMINATING_MACRO = 3;
  public static final byte SYNTAX_TYPE_SINGLE_ESCAPE = 4;
  public static final byte SYNTAX_TYPE_MULTIPLE_ESCAPE = 5;

  // default must be SYNTAX_TYPE_CONSTITUENT;
  protected final Map<Character, Byte> syntax =
      Utils.map(
          '\t', SYNTAX_TYPE_WHITESPACE,
          '\n', SYNTAX_TYPE_WHITESPACE, // linefeed
          '\f', SYNTAX_TYPE_WHITESPACE, // form feed
          '\r', SYNTAX_TYPE_WHITESPACE, // return
          ' ', SYNTAX_TYPE_WHITESPACE,
          '"', SYNTAX_TYPE_TERMINATING_MACRO,
          '\'', SYNTAX_TYPE_TERMINATING_MACRO,
          '(', SYNTAX_TYPE_TERMINATING_MACRO,
          ')', SYNTAX_TYPE_TERMINATING_MACRO,
          ',', SYNTAX_TYPE_TERMINATING_MACRO,
          ',', SYNTAX_TYPE_TERMINATING_MACRO,
          '`', SYNTAX_TYPE_TERMINATING_MACRO,
          '#', SYNTAX_TYPE_NON_TERMINATING_MACRO,
          '\\', SYNTAX_TYPE_SINGLE_ESCAPE,
          '|', SYNTAX_TYPE_MULTIPLE_ESCAPE);

  protected final Map<Character, IReaderMacroFunc> readerMacroFunctions =
      Utils.map(
          ';', LispParser.READ_COMMENT,
          '"', LispParser.READ_STRING,
          '(', LispParser.READ_LIST,
          ')', LispParser.READ_RIGHT_PAREN,
          '\'', LispParser.READ_QUOTE,
          '#', LispParser.READ_DISPATCH_CHAR,
          '`', LispParser.BACKQUOTE_MACRO,
          ',', LispParser.COMMA_MACRO);

  // BACKQUOTE-MACRO and COMMA-MACRO are defined in backquote.lisp.
  // readerMacroFunctions['`']  = Symbol.BACKQUOTE_MACRO;
  // readerMacroFunctions[',']  = Symbol.COMMA_MACRO;

  public final IReaderMacroFunc getReaderMacroFunction(char c) {
    return readerMacroFunctions.get(c);
  }

  protected Map<Character, DispatchTable> dispatchTables;
  protected Keyword readtableCase;

  public ReadTable() {
    initialize();
  }

  protected void initialize() {
    DispatchTable dt =
        new DispatchTable(
            Utils.map(
                '(',
                LispParser.SHARP_LEFT_PAREN,
                '*',
                LispParser.SHARP_STAR,
                '.',
                LispParser.SHARP_DOT,
                ':',
                LispParser.SHARP_COLON,
                'A',
                LispParser.SHARP_A,
                'B',
                LispParser.SHARP_B,
                'C',
                LispParser.SHARP_C,
                'G',
                LispParser.SHARP_G,
                'O',
                LispParser.SHARP_O,
                'P',
                LispParser.SHARP_P,
                'R',
                LispParser.SHARP_R,
                'S',
                LispParser.SHARP_S,
                'X',
                LispParser.SHARP_X,
                '\'',
                LispParser.SHARP_QUOTE,
                '"',
                LispParser.SHARP_QMARK,
                '\\',
                LispParser.SHARP_BACKSLASH,
                '|',
                LispParser.SHARP_VERTICAL_BAR,
                ')',
                LispParser.SHARP_ILLEGAL,
                '<',
                LispParser.SHARP_ILLEGAL,
                ' ',
                LispParser.SHARP_ILLEGAL,
                8,
                LispParser.SHARP_ILLEGAL, // backspace
                9,
                LispParser.SHARP_ILLEGAL, // tab
                10,
                LispParser.SHARP_ILLEGAL, // newline, linefeed
                12,
                LispParser.SHARP_ILLEGAL, // page
                13,
                LispParser.SHARP_ILLEGAL)); // return
    dispatchTables = Utils.map();
    dispatchTables.put('#', dt);
    //readtableCase = Keyword.UPCASE;
    readtableCase = Keyword.PRESERVE;
  }

  /**
   * return dispatch function for a dispatch characters pair.
   */
  public final IDispatchMacroFunc getDispatchMacroCharacter(char dispChar, char subChar)
      throws ParserException {
    DispatchTable dispatchTable = dispatchTables.get(dispChar);
    if (dispatchTable == null) {
      throw new ParserException(dispChar + " is not a dispatch character.");
    }
    final IDispatchMacroFunc function = dispatchTable.functions.get(subChar);
    // return (function != null) ? function : NIL;
    return function;
  }

  public final boolean isWhitespace(char c) {
    return getSyntaxType(c) == SYNTAX_TYPE_WHITESPACE;
  }

  public final byte getSyntaxType(char c) {
    final Byte t = syntax.get(c);
    return null == t ? SYNTAX_TYPE_CONSTITUENT : t;
  }

  /**
   * Return true if character belongs to the invalid character class.
   */
  public final boolean isInvalid(char c) {
    switch (c) {
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

  public final Keyword getReadtableCase() {
    return readtableCase;
  }

  /**
   * Ensure that character is not invalid.
   */
  public final void checkInvalid(char c) throws ParserException {
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
