package io.opsit.explang;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for specifying arguments for builtin functions/forms.
 */
@Target(ElementType.TYPE)
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface Arguments {
  /**
   * Default argument spec "&amp;REST args".
   */
  String[] spec() default {ArgSpec.ARG_REST, "args"};

  /**
   * Default argument desccription (empty).
   */
  String text() default "";
}
