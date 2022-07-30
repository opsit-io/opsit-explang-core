package  io.opsit.explang;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Target(ElementType.TYPE)
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface Package {
    String  name () default "";
    public static String BASE_ARITHMENTICS = "base.arithmetics";
    public static String BASE_LOGIC = "base.logic";
    public static String BASE_COERCION = "base.coercion";
    public static String BASE_MATH = "base.math";
    public static String BASE_TYPES = "base.types";
    public static String BASE_BINDINGS = "base.bindings";
    public static String BASE_SEQ = "base.seq";
    public static String BASE_FUNCS = "base.funcs";
    public static String BASE_BEANS = "base.beans";
    public static String BASE_CONTROL = "base.control";
    public static String BASE_REGEX = "base.regex";
    public static String BASE_TEXT = "base.text";
    public static String BASE_DOCS = "base.docs";
    public static String BASE_LANG = "base.lang";
    public static String BASE_VERSION = "base.version";
    public static String FFI = "ffi";
    public static String DWIM = "dwim";
    public static String IO = "io";
    public static String THREADS = "threads";
    public static String LOOPS = "loops";

}
