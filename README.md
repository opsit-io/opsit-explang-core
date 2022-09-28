Explang Core
============

Introduction
------------

*Explang is a simple and customizable dynamic language for the Java platform.*

The main use case for Explang currently is providing simple
customizable user-facing extension language for JVM based
software. It is being designed to around the following goals and
features:

### Focus on support for Programming in the Small, friendliness to casual users of the language.

- Dynamic typing, implicit conversions to boolean and numeric values when required.
- Dynamic variables: allows for passing values as variables in the
  context and defining user functions without arguments.
- Lisp2 language: different namespaces for variables and functions, so
  there is no potential conflict between user-defined variables and
  built-in functions.
- Batteries included: contains rich set of utility functions for
  operating on collections, regexp and globbing support, etc.
- Support for both imperative and functional style programming.
- Has a rich set of features for defining function arguments (keyword
  args, rest args, optional args, etc) instead of functions overloading.
- Documentation mechanism for user functions and builtins.  

### Pluggable parsers for language syntax

Explang comes with two parsers for LISP like languages:
- lisp - recursive parser that a sub-set of Common Lisp parser
  features.
- sexp - simple S-exp parser that is written imperatively (no depth
  limit because of JVM stack).

There is a parser with a Julia-like Algebraic syntax:
- [opsit-explang-alg-parser](https://github.com/opsit-io/opsit-explang-alg-parser)

### Modularity and adaptability

Language features come in packages so language integrators may select
just the features that are needed for their use cases. For example, to
implement a calculator one may include only arithmetical operations
and variables.  For integrating webhooks one may allow conditionals,
declaration of functions but not loops or FFI for calling arbitrary
Java methods. 

The language strives to provide feature flags and configuration
parameters to allow configuration of language behavious, such as case
sensitivity, use of default values vs. exceptions on access to
undefined variables, etc.


### Simplicity for integration in existing systems

- Explang Core comes with minimum dependencies. 
- No data types of its own: Explang works with built-in Java data
  types: Strings, Numbers, Collections, arrays, Character sequences.
- Abstraction mechanisms to use same functions on different Java
  native types (such as Collections, arrays and character sequences).
- Ability to use Java Beans like objects without writing glue code in
  Java.
- Ability to add easily additional Java based functions and special
  forms to be called from Explang code.
- Explang source strives to be simple and understandable by Java
  programmers. There are currently no language features that defined
  in Explang itself.


Language Documentation
----------------------

See [Explang Language Documentation](https://github.com/opsit/opsit-explang-docs):

- [Language Overview](TBD)
- [Language Functions Reference](TBD)
- [javadoc](TBD) for the *explang-core* module

Code Examples
-------------

- See [Sample code](examples/)


Installation
------------

Download an Explang Core executable JAR jars from Github 
[releases](https://github.com/opsit-io/opsit-explang-core/releases)


Or use maven CLI to fetch the artifacts from maven central:

```
mvn org.apache.maven.plugins:maven-dependency-plugin:2.8:get -Dartifact=io.opsit:opsit-explang-core:0.0.3:jar:runnable   -Dtransitive=false -Ddest=opsit-explang-core-0.0.3-runnable.jar
```

Using REPL
----------

Explang-core contains built-in REPL. 

```
$ java -jar opsit-explang-core-0.0.3-runnable.jar 
Welcome to the EXPLANG REPL!
Active parser is LispParser
Loaded packages are: [base.math, base.text, io, base.bindings, ffi, base.funcs, loops, threads, base.version, base.coercion, base.logic, base.lang, base.arithmetics, base.seq, base.control, base.regex, dwim, base.docs, base.beans, base.types]
Please type an EXPLANG expression 
[0]> (print "hello, world!\n")
hello world

=> hello world

[1]> 
```

This built-in REPL implementation does not support editing of and command history, so one is 
may want to it with some kind of wrapper such as [rlwrap](https://github.com/hanslub42/rlwrap),
VS Code [repeater REPL extension](https://github.com/RegisMelgaco/repeater--repl-tool), 
[Emacs inferior lisp mode](https://www.gnu.org/software/emacs/manual/html_mono/emacs.html#External-Lisp).

There is a REPL implementation with editing support in the separate project 
[Explang JLine REPL](https://github.com/opsit-io/opsit-explang-jline-repl).


Executing Explang Scripts
-------------------------

```shell
$ java -jar opsit-explang-core-0.0.3-runnable.jar ./examples/hello.l
Hello world
```


Quick Start Guide to Using Explang from Java Code
--------------------------------------------------

### Add to the dependencies listin *pom.xml*


```xml
<dependencies>
  <dependency>
    <groupId>io.opsit</groupId>
    <artifactId>opsit-explang-core</artifactId>
    <version>0.0.3</version>
  </dependency>
...
</dependencies>

```

### Parse, compile and evaluate Explang expressions from Java code


```java

import io.opsit.explang.ASTNList;
import io.opsit.explang.Compiler;
import io.opsit.explang.ICompiled;
import io.opsit.explang.IParser;
import io.opsit.explang.ParseCtx;
import io.opsit.explang.parser.lisp.LispParser;
import java.util.HashMap;
import java.util.Map;

...

// code: add two variables
String code = "(+ a b)";
// map with variable values
Map<String,Object> vars = new HashMap<String,Object>();
vars.put("a", 1);
vars.put("b", 2.5);

// Create compiler
Compiler compiler = new Compiler();
// Create a parser and associate it with the compiler
IParser parser = new LispParser();
ParseCtx pctx = new ParseCtx("mycode");
compiler.setParser(parser);
    
// parse code into Abstract Syntax Tree
ASTNList exprs = parser.parse(pctx, code);

// Check that an expression was parsed without errors.
if (exprs.hasProblems()) {
  System.err.println("Parse errors: " + exprs.getProblem());
} else if (exprs.isEmpty()) {
  System.out.println("No expressions were parsed");
} else {
  // Compile an expression into reusable form that can be executed
  ICompiled exp = compiler.compile(exprs.get(0));
  // Create context with user data
  Compiler.ICtx ctx = compiler.newCtx(vars);
  // execute compiled code in given context;
  Object expVal = exp.evaluate(compiler.newBacktrace(), ctx);
  System.out.println("Result: " + expVal);
}

```

# Evaluating Explang expressions via the Java Scripting API (JSR223)

A simpler, but less flexible way of executing Explang is to use it via the
[Java Scripting API](https://jcp.org/aboutJava/communityprocess/final/jsr223/index.html)


```java
package io.opsit.explang.examples;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import io.opsit.explang.jsr223.ExplangScriptEngineFactory;

....
ScriptEngineManager mgr = new ScriptEngineManager();
mgr.registerEngineName("explang", new ExplangScriptEngineFactory());

ScriptEngine engine = mgr.getEngineByName("explang");
// code: add two variables
String code = "(+ a b)";
// map with variable values
engine.put("a", 2);
engine.put("b", 1.5);

//Object result;
Object result = engine.eval("(+ a b)");
System.out.println("eval Result: " + result);

engine.eval(""
+ "(DEFUN fact(x) "
+ "  (IF x (* x (fact (- x 1)))"
+ "        1))"
+ ");
result = ((Invocable) engine).invokeFunction("fact", 5);
System.out.println("invoke function result: " + result);

```


# Adding Java-based builtin function to the language


This example will add builtin function "rect"

```java
import io.opsit.explang.Backtrace;
import io.opsit.explang.Funcs.FuncExp;
import io.opsit.explang.Docstring;
import io.opsit.explang.Arguments;
import io.opsit.explang.Package;
import io.opsit.explang.ArgSpec;
import io.opsit.explang.Compiler.Eargs;
...
@Arguments(spec = {"with", ArgSpec.ARG_OPTIONAL, "height"})
@Docstring(text = "make rectangle width x height. (If height not set return square)")
@Package(name = "user")
public static class Rect extends FuncExp {
  @Override
  public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
    final Object width = eargs.get(0, backtrace);
    final Object height = eargs.get(1, backtrace);
    return "Rectangle(" + width +", " + (null == height ? width : height) +")";
  }
}

...

compiler.usePackages("user");
compiler.addBuiltIn("rect", Rect.class);

```

Language Documentation
----------------------

See [Explang Language Documentation](https://github.com/opsit/opsit-explang-docs):

- [Language Overview](TBD)
- [Language Functions Reference](TBD)
- [javadoc](TBD) for the *explang-core* module
- [Sample code](examples/)


Acknowledgements
----------------

The project contains parts that were derived or adapted from

- [ABCL](https://armedbear.common-lisp.dev)
- [BetterMethodFinder.java](https://adtmag.com/articles/2001/07/24/limitations-of-reflective-method-lookup.aspx) by Paul Haulser


Licenses
--------

Explang is licensed under the [GNU AFFERO GENERAL PUBLIC LICENSE](LICENSE).
