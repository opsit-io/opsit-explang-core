package org.dudinea.explang.atom;

import org.dudinea.explang.ParseCtx;
import static org.dudinea.explang.Utils.symbol;
import static org.dudinea.explang.Utils.list;
import org.dudinea.explang.Version;
 
public  class VersionParser implements AtomParser {
    static private EscStringParser sp = new EscStringParser();
    public boolean parse(String str, Object[]holder, ParseCtx pctx) {
        if (str.length() < 4) {
            return false;
        }
        if (!str.startsWith("v\"")) {
            return false;
        }
        final String [] stringHolder = new String[1];
        boolean result = sp.parse(str.substring(1), stringHolder, pctx); 
        if (result) {
            try {
                holder[0] = Version.parseVersion(stringHolder[0]);
                return true;
            } catch ( Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        return false;
    }
}