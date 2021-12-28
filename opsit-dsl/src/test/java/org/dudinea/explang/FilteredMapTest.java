package org.dudinea.explang;

import static org.dudinea.explang.Utils.map;
import static org.dudinea.explang.Utils.list;
import static org.dudinea.explang.Utils.set;
import org.dudinea.explang.Funcs;
import org.junit.Assert;
import org.junit.Test;
import java.util.Map;
import java.util.Set;


public class FilteredMapTest {
    protected Map mkTestMap() {
        Map map = map("a", "b", "c", "d");
        return map;
    }

    @Test
    public void testListKS() {
        Map m = mkTestMap();
        Map fm = new Funcs.FilteredMap(m, list("a"));
        Assert.assertEquals(1,fm.size());
        Assert.assertEquals(fm.get("a"), "b");
        Assert.assertTrue(fm.containsKey("a"));
        Assert.assertTrue(fm.containsValue("b"));
        Assert.assertNull(fm.get("c"));
        Assert.assertFalse(fm.containsKey("c"));
        Assert.assertFalse(fm.containsValue("d"));
        Assert.assertEquals("{a=b}", fm.toString());
    }

    @Test
    public void testArrayKS() {
        Map m = mkTestMap();
        String[] ks = new String[1];
        ks[0] = "a";
        Map fm = new Funcs.FilteredMap(m, ks);
        Assert.assertEquals(1,fm.size());
        Assert.assertEquals(fm.get("a"), "b");
        Assert.assertTrue(fm.containsKey("a"));
        Assert.assertTrue(fm.containsValue("b"));
        Assert.assertNull(fm.get("c"));
        Assert.assertFalse(fm.containsKey("c"));
        Assert.assertFalse(fm.containsValue("d"));
        Assert.assertEquals("{a=b}", fm.toString());
    }

    @Test
    public void testSetKS() {
        Map m = mkTestMap();
        Set<String> ks = set("a");
        Map fm = new Funcs.FilteredMap(m, ks);
        Assert.assertEquals(1,fm.size());
        Assert.assertEquals(fm.get("a"), "b");
        Assert.assertTrue(fm.containsKey("a"));
        Assert.assertTrue(fm.containsValue("b"));
        Assert.assertNull(fm.get("c"));
        Assert.assertFalse(fm.containsKey("c"));
        Assert.assertFalse(fm.containsValue("d"));
        Assert.assertEquals("{a=b}", fm.toString());
    }

    @Test
    public void testSetString() {
        Map m = map('a', "b", 'c', "d");
        String ks = "a";
        Map fm = new Funcs.FilteredMap(m, ks);
        Assert.assertEquals(1,fm.size());
        Assert.assertEquals(fm.get('a'), "b");
        Assert.assertTrue(fm.containsKey('a'));
        Assert.assertTrue(fm.containsValue("b"));
        Assert.assertNull(fm.get('c'));
        Assert.assertFalse(fm.containsKey('c'));
        Assert.assertFalse(fm.containsValue("d"));
        Assert.assertEquals("{a=b}", fm.toString());
    }
    
}
