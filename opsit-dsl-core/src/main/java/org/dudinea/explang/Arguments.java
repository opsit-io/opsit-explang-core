package  org.dudinea.explang;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.dudinea.explang.ArgSpec;

//@Documented
@Target(ElementType.TYPE)
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface Arguments {
    String[] spec () default 	{ArgSpec.ARG_REST, "args"};
    String   text () default "";
}
