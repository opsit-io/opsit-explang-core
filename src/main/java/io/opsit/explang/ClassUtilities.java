package io.opsit.explang;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Utility routines for querying Class objects. */
final class ClassUtilities {
  /** Mapping from primitive wrapper Classes to their corresponding primitive Classes. */
  private static final Map<Class<?>, Class<?>> objectToPrimitiveMap =
      new HashMap<Class<?>, Class<?>>(13);

  static {
    objectToPrimitiveMap.put(Boolean.class, Boolean.TYPE);
    objectToPrimitiveMap.put(Byte.class, Byte.TYPE);
    objectToPrimitiveMap.put(Character.class, Character.TYPE);
    objectToPrimitiveMap.put(Double.class, Double.TYPE);
    objectToPrimitiveMap.put(Float.class, Float.TYPE);
    objectToPrimitiveMap.put(Integer.class, Integer.TYPE);
    objectToPrimitiveMap.put(Long.class, Long.TYPE);
    objectToPrimitiveMap.put(Short.class, Short.TYPE);
  }

  /**
   * Mapping from primitive wrapper Classes to the sets of primitive classes whose instances can be
   * assigned an instance of the first.
   */
  private static final Map<Class<?>, Set<Class<?>>> primitiveWideningsMap =
      new HashMap<Class<?>, Set<Class<?>>>(11);

  static {
    Set<Class<?>> set = new HashSet<Class<?>>();

    set.add(Short.TYPE);
    set.add(Integer.TYPE);
    set.add(Long.TYPE);
    set.add(Float.TYPE);
    set.add(Double.TYPE);
    primitiveWideningsMap.put(Byte.TYPE, set);

    set = new HashSet<Class<?>>();

    set.add(Integer.TYPE);
    set.add(Long.TYPE);
    set.add(Float.TYPE);
    set.add(Double.TYPE);
    primitiveWideningsMap.put(Short.TYPE, set);
    primitiveWideningsMap.put(Character.TYPE, set);

    set = new HashSet<Class<?>>();

    set.add(Long.TYPE);
    set.add(Float.TYPE);
    set.add(Double.TYPE);
    primitiveWideningsMap.put(Integer.TYPE, set);

    set = new HashSet<Class<?>>();

    set.add(Float.TYPE);
    set.add(Double.TYPE);
    primitiveWideningsMap.put(Long.TYPE, set);

    set = new HashSet<Class<?>>();

    set.add(Double.TYPE);
    primitiveWideningsMap.put(Float.TYPE, set);
  }

  /** Do not instantiate. Static methods only. */
  private ClassUtilities() {
  }

  /**
   * Get class by name, if required loading them using the given classloader.
   *
   * @param name FQN of a class, or the name of a primitive type
   * @param loader a ClassLoader
   * @return the Class for the name given. Primitive types are converted to their particular Class
   *     object. null, the empty string, "null", and "void" yield Void.TYPE. If any classes require
   *     loading because of this operation, the loading is done by the given class loader. Such
   *     classes are not initialized, however.
   * @exception ClassNotFoundException if name names an unknown class or primitive
   */
  static Class<?> classForNameOrPrimitive(String name, ClassLoader loader)
      throws ClassNotFoundException {
    if (name == null || name.equals("") || name.equals("null") || name.equals("void")) {
      return Void.TYPE;
    }

    if (name.equals("boolean")) {
      return Boolean.TYPE;
    }

    if (name.equals("byte")) {
      return Byte.TYPE;
    }

    if (name.equals("char")) {
      return Character.TYPE;
    }

    if (name.equals("double")) {
      return Double.TYPE;
    }

    if (name.equals("float")) {
      return Float.TYPE;
    }

    if (name.equals("int")) {
      return Integer.TYPE;
    }

    if (name.equals("long")) {
      return Long.TYPE;
    }

    if (name.equals("short")) {
      return Short.TYPE;
    }

    return Class.forName(name, false, loader);
  }

  /**
   * Check if class is accessioble.
   *
   * @param clz a Class
   * @return true if the class is accessible, false otherwise. Presently returns true if the class
   *     is declared public.
   */
  static boolean classIsAccessible(Class<?> clz) {
    return Modifier.isPublic(clz.getModifiers());
  }

  /**
   * Tells whether instances of the classes in the 'rhs' array could be used as parameters to a
   * reflective method invocation whose parameter list has types denoted by the 'lhs' array.
   *
   * @param lhs Class array representing the types of the formal parameters of a method
   * @param rhs Class array representing the types of the actual parameters of a method. A null
   *     value or Void.TYPE is considered to match a corresponding Object or array class in lhs, but
   *     not a primitive.
   * @return true if compatible, false otherwise
   */
  static boolean compatibleClasses(Class<?>[] lhs, Class<?>[] rhs) {
    if (lhs.length != rhs.length) {
      return false;
    }

    for (int i = 0; i < lhs.length; ++i) {
      if (rhs[i] == null || rhs[i].equals(Void.TYPE)) {
        if (lhs[i].isPrimitive()) {
          return false;
        } else {
          continue;
        }
      }

      if (!lhs[i].isAssignableFrom(rhs[i])) {
        Class<?> lhsPrimEquiv = primitiveEquivalentOf(lhs[i]);
        Class<?> rhsPrimEquiv = primitiveEquivalentOf(rhs[i]);

        if (!primitiveIsAssignableFrom(lhsPrimEquiv, rhsPrimEquiv)) {
          return false;
        }
      }
    }

    return true;
  }

  /**
   * Return accessible method for given class, method name and parameters.
   * 
   * @param clz a Class
   * @param methodName name of a method
   * @param paramTypes Class array representing the types of a method's formal parameters
   * @return the Method with the given name and formal parameter types that is in the nearest
   *     accessible class in the class hierarchy, starting with clz's superclass. The superclass
   *     and implemented interfaces of clz are searched, then their superclasses, etc. until a
   *     method is found. Returns null if there is no such method.
   */
  static Method getAccessibleMethodFrom(
      Class<?> clz, String methodName, Class<?>[] parameterTypes) {
    // Look for overridden method in the superclass.
    Class<?> superclass = clz.getSuperclass();
    Method overriddenMethod = null;

    if (superclass != null && classIsAccessible(superclass)) {
      try {
        overriddenMethod = superclass.getMethod(methodName, parameterTypes);
      } catch (NoSuchMethodException ignored) {
        // ok
      }

      if (overriddenMethod != null) {
        return overriddenMethod;
      }
    }

    // If here, then clz represents Object, or an interface, or
    // the superclass did not have an override.  Check
    // implemented interfaces.

    Class<?>[] interfaces = clz.getInterfaces();

    for (int i = 0; i < interfaces.length; ++i) {
      overriddenMethod = null;

      if (classIsAccessible(interfaces[i])) {
        try {
          overriddenMethod = interfaces[i].getMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException ignored) {
          // ok
        }

        if (overriddenMethod != null) {
          return overriddenMethod;
        }
      }
    }

    overriddenMethod = null;

    // Try superclass's superclass and implemented interfaces.
    if (superclass != null) {
      overriddenMethod = getAccessibleMethodFrom(superclass, methodName, parameterTypes);

      if (overriddenMethod != null) {
        return overriddenMethod;
      }
    }

    // Try implemented interfaces' extended interfaces...
    for (int i = 0; i < interfaces.length; ++i) {
      overriddenMethod = getAccessibleMethodFrom(interfaces[i], methodName, parameterTypes);

      if (overriddenMethod != null) {
        return overriddenMethod;
      }
    }

    // Give up.
    return null;
  }

  /**
   * Return primitive equivalent of given class.
   *
   * @param clz a Class
   * @return the class's primitive equivalent, if aClass is a primitive wrapper. If aClass is
   *     primitive, returns aClass. Otherwise, returns null.
   */
  static Class<?> primitiveEquivalentOf(Class<?> clz) {
    return clz.isPrimitive() ? clz : (Class<?>) objectToPrimitiveMap.get(clz);
  }

  /**
   * Tells whether an instance of the primitive class represented by 'rhs' can be assigned to an
   * instance of the primitive class represented by 'lhs'.
   *
   * @param lhs assignee class
   * @param rhs assigned class
   * @return true if compatible, false otherwise. If either argument is <code>null</code>, or one of
   *     the parameters does not represent a primitive (e.g. Byte.TYPE), returns false.
   */
  static boolean primitiveIsAssignableFrom(Class<?> lhs, Class<?> rhs) {
    if (lhs == null || rhs == null) {
      return false;
    }

    if (!(lhs.isPrimitive() && rhs.isPrimitive())) {
      return false;
    }

    if (lhs.equals(rhs)) {
      return true;
    }

    Set<?> wideningSet = (Set<?>) primitiveWideningsMap.get(rhs);

    if (wideningSet == null) {
      return false;
    }

    return wideningSet.contains(lhs);
  }
}
