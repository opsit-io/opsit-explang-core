package io.opsit.explang;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;



@Target(ElementType.TYPE)
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface Docstring {
  /**
   * Represent docstring for Builtin functions and forms.
   */  
  String text() default "Not documented.";

  /**
   * Represent docstring for Builtin functions and forms.  This is
   * preferred representation for docstring.  Each array entry will be
   * joined using newline as separator".
   */
  String[] lines() default {};
}
