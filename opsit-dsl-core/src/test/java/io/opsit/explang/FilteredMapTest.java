package io.opsit.explang;

import static io.opsit.explang.Utils.map;
import static io.opsit.explang.Utils.list;
import static io.opsit.explang.Utils.set;
import org.junit.Assert;
import org.junit.Test;
import java.util.Map;
import java.util.Set;


public class FilteredMapTest {
  protected Map<Object,Object> mkTestMap() {
    Map<Object,Object> map = map("a", "b", "c", "d");
    return map;
  }

  @Test
  public void testListKS() {
    Map<Object,Object> m = mkTestMap();
    Map<Object,Object> fm = new Funcs.FilteredMap(m, list("a"));
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
    Map<Object,Object> m = mkTestMap();
    String[] ks = new String[1];
    ks[0] = "a";
    Map<Object,Object> fm = new Funcs.FilteredMap(m, ks);
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
    Map<Object,Object> m = mkTestMap();
    Set<String> ks = set("a");
    Map<Object,Object> fm = new Funcs.FilteredMap(m, ks);
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
    Map<Object,Object> m = map('a', "b", 'c', "d");
    String ks = "a";
    Map<Object,Object> fm = new Funcs.FilteredMap(m, ks);
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
