package io.opsit.explang;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Finds methods and constructors that can be invoked reflectively. Attempts to address some of the
 * limitations of the JDK's Class.getMethod() and Class.getConstructor(), and other JDK reflective
 * facilities.
 */
public final class BetterMethodFinder {
  /** The target class to look for methods and constructors in. */
  private final Class<?> clazz;

  /** Mapping from method name to the Methods in the target class with that name. */
  private final Map<String, List<Method>> methodMap = new HashMap<String, List<Method>>();

  /** List of the Constructors in the target class. */
  private final List<Constructor<?>> ctorList = new ArrayList<Constructor<?>>();

  /**
   * Mapping from a Constructor or Method object to the Class objects representing its formal
   * parameters.
   */
  private final Map<Executable, Class<?>[]> paramMap = new HashMap<Executable, Class<?>[]>();

  /**
   * Create instance for given class.
   * 
   * @param clazz Class in which I will look for methods and constructors
   * @exception IllegalArgumentException if clazz is null, or represents a primitive, or represents
   *     an array type
   */
  public BetterMethodFinder(Class<?> clazz) {
    if (clazz == null) {
      throw new IllegalArgumentException("null Class parameter");
    }

    if (clazz.isPrimitive()) {
      throw new IllegalArgumentException("primitive Class parameter");
    }

    if (clazz.isArray()) {
      throw new IllegalArgumentException("array Class parameter");
    }

    this.clazz = clazz;

    loadMethods();
    loadConstructors();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    } else if (obj == null || getClass() != obj.getClass()) {
      return false;
    } else {
      BetterMethodFinder other = (BetterMethodFinder) obj;
      return clazz.equals(other.clazz);
    }
  }

  /**
   * Returns the most specific public constructor in my target class that accepts the number and
   * type of parameters in the given Class array in a reflective invocation.
   *
   * <p>A null value or Void.TYPE in parameterTypes matches a corresponding Object or array
   * reference in a constructor's formal parameter list, but not a primitive formal parameter.
   *
   * @param parameterTypes array representing the number and types of parameters to look for in the
   *     constructor's signature. A null array is treated as a zero-length array.
   * @return Constructor object satisfying the conditions
   * @exception NoSuchMethodException if no constructors match the criteria, or if the reflective
   *     call is ambiguous based on the parameter types
   */
  public Constructor<?> findConstructor(Class<?>[] parameterTypes) throws NoSuchMethodException {
    if (parameterTypes == null) {
      parameterTypes = new Class[0];
    }
    return (Constructor<?>) findMemberIn(ctorList, parameterTypes);
  }

  /**
   * Basis of findConstructor() and findMethod(). The member list fed to this method will be either
   * all Constructor objects or all Method objects.
   */
  private Member findMemberIn(List<? extends Executable> memberList, Class<?>[] parameterTypes)
      throws NoSuchMethodException {
    // For some reason List<Member> is compile time incompatible when
    // calling it with List<Constructor> inspite of Constructor implementing Member!
    // so have to use common supertype as argument type
    // Why?
    List<Member> matchingMembers = new ArrayList<Member>();

    for (Iterator<? extends Executable> it = memberList.iterator(); it.hasNext(); ) {
      // on single member level it handles the type just fine!
      Member member = it.next();
      Class<?>[] methodParamTypes = paramMap.get(member);

      if (Arrays.equals(methodParamTypes, parameterTypes)) {
        return member;
      }

      if (ClassUtilities.compatibleClasses(methodParamTypes, parameterTypes)) {
        matchingMembers.add(member);
      }
    }

    if (matchingMembers.isEmpty()) {
      throw new NoSuchMethodException("no member in " + clazz.getName() + " matching given args");
    }

    if (matchingMembers.size() == 1) {
      return (Member) matchingMembers.get(0);
    }

    return findMostSpecificMemberIn(matchingMembers);
  }

  /**
   * Returns the most specific public method in my target class that has the given name and accepts
   * the number and type of parameters in the given Class array in a reflective invocation.
   *
   * <p>A null value or Void.TYPE in parameterTypes will match a corresponding Object or array
   * reference in a method's formal parameter list, but not a primitive formal parameter.
   *
   * @param methodName name of the method to search for
   * @param parameterTypes array representing the number and types of parameters to look for in the
   *     method's signature. A null array is treated as a zero-length array.
   * @return Method object satisfying the conditions
   * @exception NoSuchMethodException if no methods match the criteria, or if the reflective call is
   *     ambiguous based on the parameter types, or if methodName is null
   */
  public Method findMethod(String methodName, Class<?>[] parameterTypes)
      throws NoSuchMethodException {
    List<Method> methodList = methodMap.get(methodName);

    if (methodList == null) {
      throw new NoSuchMethodException("no method named " + clazz.getName() + "." + methodName);
    }

    if (parameterTypes == null) {
      parameterTypes = new Class[0];
    }

    return (Method) findMemberIn(methodList, parameterTypes);
  }

  /**
   * @param a List of Members (either all Constructors or all Methods).
   * @return the most specific of all Members in the list.
   * @exception NoSuchMethodException if there is an ambiguity as to which is most specific.
   */
  private Member findMostSpecificMemberIn(List<Member> memberList) throws NoSuchMethodException {
    List<Member> mostSpecificMembers = new ArrayList<Member>();

    for (Iterator<Member> memberIt = memberList.iterator(); memberIt.hasNext(); ) {
      Member member = memberIt.next();

      if (mostSpecificMembers.isEmpty()) {
        // First guy in is the most specific so far.
        mostSpecificMembers.add(member);
      } else {
        boolean moreSpecific = true;
        boolean lessSpecific = false;

        // Is member more specific than everyone in the most-specific set?
        for (Iterator<Member> specificIt = mostSpecificMembers.iterator(); specificIt.hasNext(); ) {
          Member moreSpecificMember = specificIt.next();

          if (!memberIsMoreSpecific(member, moreSpecificMember)) {
            /* Can't be more specific than the whole set.  Bail out, and
            mark whether member is less specific than the member
            under consideration.  If it is less specific, it need not be
            added to the ambiguity set.  This is no guarantee of not
            getting added to the ambiguity set...we're just not clever
            enough yet to make that assessment. */

            moreSpecific = false;
            lessSpecific = memberIsMoreSpecific(moreSpecificMember, member);
            break;
          }
        }

        if (moreSpecific) {
          // Member is the most specific now.
          mostSpecificMembers.clear();
          mostSpecificMembers.add(member);
        } else if (!lessSpecific) {
          // Add to ambiguity set if mutually unspecific.
          mostSpecificMembers.add(member);
        }
      }
    }

    if (mostSpecificMembers.size() > 1) {
      throw new NoSuchMethodException(
          "Ambiguous request for member in " + clazz.getName() + " matching given args");
    }

    return (Member) mostSpecificMembers.get(0);
  }

  /**
   * @param args an Object array
   * @return an array of Class objects representing the classes of the objects in the given Object
   *     array. If args is null, a zero-length Class array is returned. If an element in args is
   *     null, then Void.TYPE is the corresponding Class in the return array.
   */
  public static Class<?>[] getParameterTypesFrom(Object[] args) {
    Class<?>[] argTypes = null;

    if (args != null) {
      argTypes = new Class[args.length];

      for (int i = 0; i < args.length; ++i) {
        argTypes[i] = (args[i] == null) ? Void.TYPE : args[i].getClass();
      }
    } else {
      argTypes = new Class[0];
    }

    return argTypes;
  }

  /**
   * @param classNames String array of fully qualified names (FQNs) of classes or primitives.
   *     Represent an array type by using its JVM type descriptor, with dots instead of slashes
   *     (e.g. represent the type int[] with "[I", and Object[][] with "[[Ljava.lang.Object;").
   * @return an array of Class objects representing the classes or primitives named by the FQNs in
   *     the given String array. If the String array is null, a zero-length Class array is returned.
   *     If an element in classNames is null, the empty string, "void", or "null", then Void.TYPE is
   *     the corresponding Class in the return array. If any classes require loading because of this
   *     operation, the loading is done by the ClassLoader that loaded this class. Such classes are
   *     not initialized, however.
   * @exception ClassNotFoundException if any of the FQNs name an unknown class
   */
  public static Class<?>[] getParameterTypesFrom(String[] classNames)
      throws ClassNotFoundException {
    return getParameterTypesFrom(classNames, BetterMethodFinder.class.getClassLoader());
  }

  /**
   * @param classNames String array of fully qualified names (FQNs) of classes or primitives.
   *     Represent an array type by using its JVM type descriptor, with dots instead of slashes
   *     (e.g. represent the type int[] with "[I", and Object[][] with "[[Ljava.lang.Object;").
   * @param loader a ClassLoader
   * @return an array of Class objects representing the classes or primitives named by the FQNs in
   *     the given String array. If the String array is null, a zero-length Class array is returned.
   *     If an element in classNames is null, the empty string, "void", or "null", then Void.TYPE is
   *     the corresponding Class in the return array. If any classes require loading because of this
   *     operation, the loading is done by the given ClassLoader. Such classes are not initialized,
   *     however.
   * @exception ClassNotFoundException if any of the FQNs name an unknown class
   */
  public static Class<?>[] getParameterTypesFrom(String[] classNames, ClassLoader loader)
      throws ClassNotFoundException {
    Class<?>[] types = null;

    if (classNames != null) {
      types = new Class[classNames.length];

      for (int i = 0; i < classNames.length; ++i) {
        types[i] = ClassUtilities.classForNameOrPrimitive(classNames[i], loader);
      }
    } else {
      types = new Class[0];
    }

    return types;
  }

  public int hashCode() {
    return clazz.hashCode();
  }

  /** Loads up the data structures for my target class's constructors. */
  private void loadConstructors() {
    Constructor<?>[] ctors = clazz.getConstructors();

    for (int i = 0; i < ctors.length; ++i) {
      ctorList.add(ctors[i]);
      paramMap.put(ctors[i], ctors[i].getParameterTypes());
    }
  }

  /** Loads up the data structures for my target class's methods. */
  private void loadMethods() {
    Method[] methods = clazz.getMethods();

    for (int i = 0; i < methods.length; ++i) {
      Method method = methods[i];
      String methodName = method.getName();
      Class<?>[] paramTypes = method.getParameterTypes();

      List<Method> list = methodMap.get(methodName);

      if (list == null) {
        list = new ArrayList<Method>();
        methodMap.put(methodName, list);
      }

      if (!ClassUtilities.classIsAccessible(clazz)) {
        method = ClassUtilities.getAccessibleMethodFrom(clazz, methodName, paramTypes);
      }

      if (method != null) {
        list.add(method);
        paramMap.put(method, paramTypes);
      }
    }
  }

  /**
   * @param first a Member
   * @param second a Member
   * @return true if the first Member is more specific than the second, false otherwise. Specificity
   *     is determined according to the procedure in the Java Language Specification, section
   *     15.12.2.
   */
  private boolean memberIsMoreSpecific(Member first, Member second) {
    Class<?>[] firstParamTypes = (Class[]) paramMap.get(first);
    Class<?>[] secondParamTypes = (Class[]) paramMap.get(second);
    return ClassUtilities.compatibleClasses(secondParamTypes, firstParamTypes);
  }
}
