package org.dudinea.explang;


import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.function.Consumer;


public class Seq {

    public static interface Operation {
        public boolean perform(Object obj);
    }

    public static boolean isSequence(Object obj) {
        return (null!= obj) &&
            ((obj instanceof Map) ||
             (obj instanceof Iterable) ||
             (obj instanceof CharSequence) ||
             (obj instanceof Enumeration) ||
             (obj.getClass().isArray()));
    }
    
    public static void forEach(Object seq, Operation op, boolean allowNonSeq) {
        if (seq instanceof Map) {
            seq = ((Map)seq).entrySet();
        }
        if (null == seq) {
            if (allowNonSeq) {
                op.perform(seq);
            } else {
                throw new RuntimeException("NIL is not a supported sequence.");
            }
        } else if (seq instanceof Iterable) {
            final Iterator iter = ((Iterable)seq).iterator();
            boolean brk = false;;
            while (iter.hasNext()) {
                if (op.perform(iter.next())) {
                    break;
                }
            }
        } else if (seq instanceof CharSequence) {
            final CharSequence cs = ((CharSequence)seq);
            final int numChars = cs.length();
            for (int j = 0; j < numChars; j++) {
                if (op.perform(cs.charAt(j))) {
                    break;
                }
            }
        } else if (seq.getClass().isArray()) {
            final int len = Array.getLength(seq);
            for (int j = 0; j < len; j++) {
                if (op.perform(Array.get(seq, j))) {
                    break;
                }
            }
        } else if (seq instanceof Enumeration) {
            final Enumeration en = (Enumeration) seq;
            while (en.hasMoreElements()) {
                if (op.perform(en.nextElement())) {
                    break;
                }
            }
        } else if (seq instanceof Map) {
            final Map map = (Map) seq;
            Iterable col = map.values();
            Iterator iter = col.iterator();
            while(iter.hasNext()) {
                if (op.perform(iter.next())) {
                    break;
                }
            }
       	} else {
            if (allowNonSeq) {
                op.perform(seq);
            } else {
                throw new RuntimeException("Do not know how to iterate sequence of type "+seq.getClass());
            }
        }
    }

    public static boolean containsElement(Object seq, Object obj) {
	    if (null == seq) {
            return false;
	    } else if (seq instanceof Collection) {
            return ((Collection)seq).contains(obj);
	    } else if (seq.getClass().isArray()) {
            final int len = Array.getLength(seq);
            for (int j = 0; j < len; j++) {
                Object elt = Array.get(seq, j);
                if (null == obj) {
                    if (null == elt) {
                        return true;
                    }
                } else {
                    return elt.equals(obj);
                }
            }
            return false;
	    } else if (seq instanceof CharSequence) {
            if (! (obj instanceof Character)) {
                return false;
            }
            final CharSequence cs = ((CharSequence) seq);
            final Character chr = (Character) obj;
            final int len = cs.length();
            for (int i = 0; i < len; i++) {
                if (cs.charAt(i) == chr) {
                    return true;
                }
            }
            return false;
        } else if (seq instanceof Map) {
            return ((Map) seq).containsValue(obj);
        } else {
            return false;
	    }
    }
    
    public static Object getElement(Object seq, int index) {
	    if (null == seq) {
            return null;
	    } else if (seq instanceof List) {
            try {
                final List<Object> list = (List<Object>)seq;
                return list.get(index);
            } catch (IndexOutOfBoundsException bex) {
                return null;
            }
	    } else if (seq.getClass().isArray()) {
            try {
                return Array.get(seq, index);
            } catch (ArrayIndexOutOfBoundsException bex) {
                return null;
            }
	    } else if (seq instanceof CharSequence) {
            try {
                final CharSequence cs = (CharSequence)seq;
                return cs.charAt(index);
            } catch (IndexOutOfBoundsException bex) {
                return null;
            }
	    } else {
            throw new RuntimeException("Unupported sequence type "+
                                       seq.getClass().getName());
	    }
    }
    
    public static int getLength(Object val,
                                boolean allowNonSeq) {
        if (null == val) {
            if (allowNonSeq) {
                return 0;
            } else {
                throw new RuntimeException("NIL not a sequence: ");
            }
        } else if (val instanceof Collection) {
            return ((Collection)val).size();
        } else if (val instanceof CharSequence) {
            return ((CharSequence)val).length();
        } else if (val.getClass().isArray()) {
            return Array.getLength(val);
        } else if (val instanceof Map) {
            return ((Map)val).size();
        } else {
            if (allowNonSeq) {
                return 1;
            } else {
                throw new RuntimeException("Given object type is not a sequence: "+val.getClass());
            }
        }
    }

    public static Set asSet(Object obj) {
        final Set result = new HashSet();
        if (null!=obj) {
            forEach(obj, new Operation () {
                    @Override
                    public boolean perform(Object obj) {
                        result.add(obj);
                        return false;
                    }
                
            }, false);
        }
        return result;
    }

    public static List valuesList(Object seq) {
        final List result = Utils.list();
        if (null != seq) {
            forEach(seq, new Operation () {
                    @Override
                    public boolean perform(Object obj) {
                        result.add(obj);
                        return false;
                    }
                }, false);
        }
        return result;
    }

    public static int maxLength(Object... seqs) {
        int result = 0;
        for (int i = 0; i < seqs.length; i++) {
            int len = getLength(seqs[i], false);
            if (len > result) {
                result = len;
            }
        }
        return result;
    }

    public static int minLength(Object... seqs) {
        if (seqs.length > 0) {
            int result = Integer.MAX_VALUE;
            for (int i = 0; i < seqs.length; i++) {
                int len = getLength(seqs[i], false);
                if (len < result) {
                    result = len;
                }
            }
            return result;
        } else {
            return 0;
        }
    }

    
    public static interface Multiop{
        public Object perform(Object... objs);
    }

    public static List mapall(Multiop op, Object... seqs) {
        int maxlen = maxLength(seqs);
        List result = new ArrayList<Object>(maxlen);
        Object[] args = new Object[seqs.length];
        for (int i = 0; i < maxlen; i++) {
            for (int j = 0; j < seqs.length; j++) {
                args[j] = getElement(seqs[j], i);
            }
            result.add(i, op.perform(args));
        }
        return result;
    }


    public static boolean sequal(Map m1, Map m2) {
        // not sure that this is right
        // probably need to check keys according to the same rules?
        // but it could get messy
        // unless we use same equality rules for getting keys from maps
        final Set k1 = m1.keySet();
        final Set k2 = m2.keySet();
        if (!k1.equals(k2)) {
            return false;
        }
        for (Object k : m1.keySet()) {
            final Object el1 = m1.get(k);
            final Object el2 = m2.get(k);
            if (Utils.sequal(el1, el2)) {
                continue;
            }
            if (isSequence(el1) && isSequence(el2)) {
                if (sequal(el1, el2)) {
                    continue;
                }
            }
            return false;
        }
        return true;
    }
    
    public static boolean sequal(Object o1, Object o2) {
        int l1 = Seq.getLength(o2, false);
        int l2 = Seq.getLength(o2, false);
        if (l1 != l2) {
            return false;
        }
        if ((o1 instanceof Map) && (o2 instanceof Map)) {
            return sequal((Map) o1, (Map) o2);
        }
        if ((o1 instanceof Set) && (o2 instanceof Set)) {
            return o1.equals(o2);
        }
        
        for (int i = 0; i < l1; i++) {
            final Object el1 = getElement(o1, i);
            final Object el2 = getElement(o2, i);
            // compare non-sequence objects
            // if both of them is sequnces and x.equals(y) == true
            //  it's ok as well.
            if (Utils.sequal(el1, el2)) {
                continue;
            }
            if (isSequence(el1) && isSequence(el2)) {
                if (sequal(el1, el2)) {
                    continue;
                }
            }
            return false;
        }
        return true;
    }
}
