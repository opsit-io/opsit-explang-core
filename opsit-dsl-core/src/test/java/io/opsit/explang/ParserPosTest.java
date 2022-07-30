package io.opsit.explang;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class ParserPosTest extends AbstractTest {
    static int testNum = 0;
    static final String INAME = "@";
    
    public static List<Object> list(Object ... objs) {
        return Arrays.asList(objs);
    }

    public static Symbol sym(String str) {
        return new Symbol(str);
    }
    public static Keyword keyword(String str) {
        return new Keyword(str);
    }

    String in;
    ASTN ep;
        
    public ParserPosTest(String in, ASTN ep, Object parsers) {
        this.in = in;
        this.ep = ep;
    }

    protected static ASTNList alist(List obj, int line, int pos, int offset, int len) {
        ParseCtx pctx = new ParseCtx(INAME, line, pos, offset, len);
        return new ASTNList(obj, pctx, false);
    }
    
    @Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { "()", alist(list(), 0, 0, 0, 0), null},
                { "HELLO", alist(list(), 0, 0, 0, 0), null},
                { "  HELLO", alist(list(), 0, 0, 0, 0), null},
                { "123", alist(list(), 0, 0, 0, 0), null},
                { "  123", alist(list(), 0, 0, 0, 0), null},
                { "\"str\"", alist(list(), 0, 0, 0, 0), null},
                { "  \"str\"  ", alist(list(), 0, 0, 0, 0), null},
                { "  \"str\"\"str2\"  ", alist(list(), 0, 0, 0, 0), null},
                { "  ( \"str\" \"str2\" )  ", alist(list(), 0, 0, 0, 0), null},
                { "  ((  123 )    456   )  ", alist(list(), 0, 0, 0, 0), null},
                { "(IF (> a b )\n"+
                  "    a\n"+
                  "    b))", alist(list(), 0, 0, 0, 0), null},		
            });
    }

    @Test
    public void testExprParse() {
        log("\n\n TEST #: "+(testNum++));
        SexpParser parser = new SexpParser();
        
        log("\n\nIN:  "+in);
        ParseCtx pctx = new ParseCtx(INAME);
        ASTNList astnl = parser.parse(pctx, in, 1);
        ASTN astn = astnl.get(0);
        log("OUT: " + astn.toStringWithPos());
	
    }

    private Object stripAST(ASTN obj) {
        if (obj.isList()) {
            List <Object> newLst = new ArrayList<Object>();
            for (ASTN el : ((ASTNList)obj)) {
                newLst.add(stripAST(el));
            }
            return newLst;
        } else  {
            return obj.getObject();
        } 
        //  throw new RuntimeException("Unexpected onject in AST" + obj);
        
    }

}

