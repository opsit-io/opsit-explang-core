package org.dudinea.explang;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;


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
        forEach(obj, new Operation () {
                @Override
                public boolean perform(Object obj) {
                    result.add(obj);
                    return false;
                }
                
            }, false);
        return result;
    }
}
