package io.opsit.explang;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;


public class Threads {
  public  static final Map <Thread, Compiler.ICtx> contexts =
	Collections.synchronizedMap(new WeakHashMap <Thread, Compiler.ICtx>());
  public  static final Map <Thread, Object> results =
	Collections.synchronizedMap(new WeakHashMap<Thread, Object>());
  public static List<Integer> sizes() {
	return Utils.list(results.size(), contexts.size());
  }
}
