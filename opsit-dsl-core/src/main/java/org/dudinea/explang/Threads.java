package org.dudinea.explang;


import java.lang.Thread;
import java.util.Map;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Collections;
import java.util.WeakHashMap;


public class Threads {
    public  static final Map <Thread, Compiler.ICtx> contexts =
	Collections.synchronizedMap(new WeakHashMap <Thread, Compiler.ICtx>());
    public  static final Map <Thread, Object> results =
	Collections.synchronizedMap(new WeakHashMap<Thread, Object>());
    public static List sizes() {
	return Utils.list(results.size(), contexts.size());
    }
}
