package io.opsit.explang;

import io.opsit.explang.Compiler.ICtx;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;


public class REPL {
  protected boolean verbose = false;
  protected boolean lineMode = false;
  protected ICtx ctx = null;
  protected File inFile = null;

  /** REPL entry point. */
  public static void main(String[] argv) throws Exception {
    REPL repl = new REPL();
    repl.runWithArgs(argv);
  }

  protected List<String> parsers = Utils.list("lisp", "sexp");
  protected List<String> funcConverters = Utils.list("uc", "nop");

  public List<String> getParsers() {
    return parsers;
  }

  public void setParsers(List<String> parsers) {
    this.parsers = parsers;
  }

  public List<String> getFuncConverters() {
    return funcConverters;
  }

  public void setFuncConverters(List<String> funcConverters) {
    this.funcConverters = funcConverters;
  }

  
  protected void runWithArgs(String[] argv) throws Exception {
    int rc = 0;
    Set<String> packages = Compiler.getAllPackages();
    String convName = this.getFuncConverters().get(0);
    String parserName = this.getParsers().get(0);
    for (int i = 0; i < argv.length; i++) {
      String val = argv[i];
      if ("-d".equals(val)) {
        verbose = true;
        continue;
      }
      if ("-l".equals(val)) {
        lineMode = true;
        continue;
      }
      if ("-p".equals(val)) {
        packages = parsePackages(argv, ++i);
        continue;
      }
      if ("-r".equals(val)) {
        parserName = argv[++i];
        continue;
      }
      
      if ("-f".equals(val)) {
        convName = argv[++i];
        continue;
      }

      if ("-v".equals(val)) {
        final String version = Utils.getExplangCoreVersionStr();
        if (null != version) {
          System.err.println(version);
          System.exit(0);
        } else {
          System.err.println("Failed to determine version");
          System.exit(1);
        }
      }

      if (val.startsWith("-h") || "-?".equals(val)) {
        usage();
        System.exit(0);
      }
      if (val.startsWith("-")) {
        System.err.println("Unknown option: '" + val + "'\n");
        usage();
        System.exit(1);
      }
      inFile = new java.io.File(val);
    }

    IParser parser = (IParser) loadModule("io.opsit.explang.parser.",
                                          parserName,
                                          "Parser");
    IStringConverter conv = (IStringConverter) loadModule("io.opsit.explang.strconv.",
                                                          convName,
                                                          "Converter");
    Compiler compiler = new Compiler(conv, packages);
    compiler.setParser(parser);
    ctx = compiler.newCtx();
    if (null != inFile) {
      rc = runfile(inFile);
    } else {
      if (!parser.supportREPLStream()) {
        lineMode = true;
      }
      String inputName = "<STDIN%d>";
      InputStream is = System.in;
      Reader reader = new InputStreamReader(is);
      rc = repl(reader, inputName);
    }
    System.exit(rc);
  }

  protected static final String MODULE_NAME_REGEX = "^[a-zA-Z_][a-zA-Z_0-9]*$";
  
  protected Object loadModule(String classPrefix, String moduleName, String classSuffix) {
    if (! moduleName.matches(MODULE_NAME_REGEX)) {
      System.err.println("Invalid module name specified '" + moduleName
                         + "', must match regex "  + MODULE_NAME_REGEX);
      System.exit(1);
    }
    final String moduleClassStr = classPrefix
        + moduleName.toLowerCase() + "."
        + moduleName.substring(0,1).toUpperCase() + moduleName.substring(1) + classSuffix;
    try {
      Class<?> clz = Utils.strToClass(moduleClassStr);
      Constructor<?> constr = clz.getConstructor();
      Object result =  constr.newInstance();
      return result;
    } catch (Exception ex) {
      System.err.println("Failed to load module '" + moduleName + "': " + ex);
      System.exit(2);
      return null;
    }
  }
  
  protected String listItems(Collection<String> items) {
    final List<String> lst = new ArrayList<String>(items);
    lst.sort(null);
    StringBuilder buf = new StringBuilder();
    for (String item : lst) {
      buf.append("                  ").append(item).append("\n");
    }
    return buf.toString();
  }

  protected void usage() {
    final String msg =
        ""
            + "Explang REPL usage:\n"
            + "explang  [ option .. ] [ file ... ]\n"
            + "  -d            Enable verbose diagnostics\n"
            + "  -p  packages  Comma separated list of enabled packages\n"
            + "                By default all the packages are enabled.\n"
            + "                The available packages:\n"
            + listItems(Compiler.getAllPackages())
            + "  -r  parser    Specify parser. The default is " + getParsers().get(0)
            +   ", available parsers are:\n"
            + listItems(getParsers())
            + "  -f  converter Specify function name converter. The default is  "
            +    getFuncConverters().get(0) + ", available converters are:\n"
            + listItems(getFuncConverters())      
            + "  -l            Enable line mode\n"
            + "  -h            Print help message\n"
            + "  -v            Print software version\n";
    System.err.print(msg);
  }

  protected static Set<String> parsePackages(String[] argv, int idx) {
    Set<String> packages = Utils.set();
    if (idx < argv.length) {
      String val = argv[idx];
      packages.addAll(Arrays.asList(val.split("\\s*,\\s*")));
    }
    return packages;
  }

  /** Compile and execute file as explang code. */
  public int runfile(File file) throws Exception {
    Exception err = null;
    Object result = null;
    int rc = 0;
    try {
      ASTNList asts = new ParserWrapper(ctx.getCompiler().getParser()).parse(file);
      if (verbose) {
        System.err.println("AST(" + file.getName() + "):\n" + asts + "\n------\n");
      }
      final List<ICompiled> exprs = ctx.getCompiler().compileExpList(asts);
      for (ICompiled expr : exprs) {
        if (verbose) {
          System.err.println("EXPR(" + file.getName() + "):\n" + expr + "\n------\n");
        }
        result = expr.evaluate(ctx.getCompiler().newBacktrace(), ctx);
        if (verbose) {
          System.err.println("RESULT(" + file.getName() + "):\n" + result + "\n------\n");
        }
      }
    } catch (ParserException ex) {
      System.err.println(ex.getMessage());
      err = ex;
      rc = 2;
    } catch (CompilationException ex) {
      System.err.println(ex.getMessage());
      err = ex;
      rc = 3;
    } catch (ExecutionException ex) {
      System.err.print("RUNTIME ERROR: " + ex.getMessage());
      err = ex;
      rc = 4;
      if (null != ex.getBacktrace()) {
        System.err.println(" at:\n" + ex.getBacktrace());
      } else {
        System.err.println();
      }
    } catch (Exception ex) {
      rc = 5;
      err = ex;
      System.err.println("ERROR: " + ex);
    } finally {
      if (null != err && verbose) {
        err.printStackTrace(System.err);
      }
    }
    return rc;
  }

  /**
   * Run interactive REPL loop.
   *
   * @param reader input stream
   * @param inputName name of input in REPL UI
   */
  public int repl(Reader reader, String inputName)
      throws java.io.IOException {
    Compiler compiler = ctx.getCompiler();
    IParser parser = compiler.getParser();
    System.out.print(
        "Welcome to the EXPLANG REPL!\n"
            + "Active parser is "
            + parser.getClass().getSimpleName()
            + "\n"
            + "Loaded packages are: "
            + compiler.getPackages()
            + "\n"
            + "Please type an EXPLANG expression "
            + (lineMode ? "followed by an extra NEWLINE" : "")
            + "\n");
    int inputNo = 0;
    Throwable err = null;
    Object result = null;
    Backtrace bt = compiler.newBacktrace();
    if (lineMode) {
      reader = new BufferedReader(reader);
    }
    while (true) {
      try {
        ctx.getMappings().put("***", ctx.get("**", bt));
        ctx.getMappings().put("**", ctx.get("*", bt));
        ctx.getMappings().put("*", result);

        System.out.print("[" + inputNo + "]> ");
        System.out.flush();

        err = null;

        ParseCtx pctx = new ParseCtx("INPUT" + (inputNo++));
        ASTNList exprs = null;
        if (lineMode) {
          StringBuilder buf = new StringBuilder();
          String line = null;
          while (null != (line = ((BufferedReader) reader).readLine())) {
            if (line.length() == 0) {
              break;
            }
            buf.append(line).append("\n");
            exprs = parser.parse(pctx, buf.toString(), Integer.MAX_VALUE);
            if (verbose) {
              System.out.println("\nPARSER RETURN: " + exprs);
            }
            if (null != exprs
                && exprs.size() > 0
                &&
                // we can parse the last expression =>
                // we accept list of all expressions.
                // FIXME: there should be a way to distinguish
                //        expressions that are incomplete
                //        and those that had errors but are
                //        known to be complete
                !exprs.get(exprs.size() - 1).hasProblems()) {
              if (verbose) {
                System.out.println(
                    "\nLINE READER: read complete " + exprs.size() + "expressions\n");
              }
              break;
            }
          }
          if (null == line) {
            if (verbose) {
              System.out.println("\nEOF in line mode\n");
            }
            break;
          }
          if (verbose) {
            System.out.println("\nINPUT:" + buf.toString() + "\n");
          }
        } else {
          exprs = parser.parse(pctx, reader, 1);
        }
        if (null == exprs) {
          continue;
        }
        for (ASTN exprASTN : exprs) {
          if (verbose) {
            System.out.println("\nAST:\n" + exprASTN + "\n------\n");
          }
          if (exprASTN.hasProblems()) {
            System.out.println("Failed to parse expression:");
            System.out.print(listParseErrors(exprASTN));
            break;
          }
          ICompiled expr = compiler.compile(exprASTN);
          if (verbose) {
            System.out.println("compiled:\n" + expr + "\n------\n");
          }
          result = expr.evaluate(bt, ctx);
          if (verbose) {
            System.out.println("evaluation result:\n");
          }
          System.out.print("\n=> ");
          System.out.print(Utils.asString(result));
          System.out.println();
        }
      } catch (CompilationException ex) {
        System.err.println("COMPILATION ERROR: " + ex.getMessage());
        err = ex;
      } catch (ExecutionException ex) {
        System.err.print("EXECUTION ERROR: " + ex.getMessage());
        err = ex;
        if (null != ex.getBacktrace()) {
          System.err.println(" at:\n" + ex.getBacktrace());
        } else {
          System.err.println();
        }
      } catch (RuntimeException ex) {
        err = ex;
        System.err.println("RUNTIME EXCEPTION: " + ex);
      } catch (Exception ex) {
        err = ex;
        System.err.println("EXCEPTION: " + ex);
      }
      if ((null != err) && verbose) {
        err.printStackTrace(System.err);
      }
    }
    return null == err ? 0 : 1;
  }

  private String listParseErrors(ASTN exprASTN) {
    final StringBuilder buf = new StringBuilder();
    ASTN.Walker errCollector =
        new ASTN.Walker() {
          public void walk(ASTN node) {
            final Exception ex = node.getProblem();
            if (null != ex) {
              buf.append(ex.getMessage()).append("\n");
            }
          }
        };
    exprASTN.dispatchWalker(errCollector);
    return buf.toString();
  }
}
