package io.opsit.explang;

import java.io.File;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;


public class Main {
  /** Standalone interpreter entry point. */
  public static void main(String[] argv) throws Exception {
    Main main = new Main();
    main.runWithArgs(argv);
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
    boolean verbose = false;
    boolean lineMode = false;
    File inFile = null;
    int rc = 0;
    Set<String> packages = Compiler.getAllPackages();
    String convName = this.getFuncConverters().get(0);
    String parserName = this.getParsers().get(0);
    List<String> args = Utils.list();
    boolean readArgs = false;
    for (int i = 0; i < argv.length; i++) {
      String val = argv[i];
      if (readArgs) {
        args.add(val);
      } else {
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
        if ("--".equals(val)) {
          readArgs = true;
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
        readArgs = true;
      }
    }

    IParser parser = (IParser) loadModule("io.opsit.explang.parser.",
                                          parserName,
                                          "Parser");
    IStringConverter conv = (IStringConverter) loadModule("io.opsit.explang.strconv.",
                                                          convName,
                                                          "Converter");
    Compiler compiler = new Compiler(conv, packages);
    compiler.setParser(parser);
    compiler.setCommandlineArgs(args);
    if (null != inFile) {
      rc = runfile(inFile, compiler, verbose);
    } else {
      if (!parser.supportREPLStream()) {
        lineMode = true;
      }
      String inputName = "<STDIN%d>";
      IREPL repl = mkREPL(parser);
      repl.setParser(parser);
      repl.setCompiler(compiler);
      repl.setVerbose(verbose);
      repl.setLineMode(lineMode);
      Object result = repl.execute(new InputStreamReader(System.in), inputName);
      if (result instanceof Number) {
        rc = ((Number) result).intValue();
      }
    }
    System.exit(rc);
  }

  protected IREPL mkREPL(IParser parser) {
    return new REPL();
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
            + "explang  [ option .. ] [ file | -- ] [ arg ..]\n"
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
  public int runfile(File file, Compiler compiler, boolean verbose) throws Exception {
    Exception err = null;
    Object result = null;
    Compiler.ICtx ctx = compiler.newCtx();
    int rc = 0;
    try {
      ASTNList asts = new ParserWrapper(compiler.getParser()).parse(file);
      if (verbose) {
        System.err.println("AST(" + file.getName() + "):\n" + asts + "\n------\n");
      }
      final List<ICompiled> exprs = compiler.compileExpList(asts);
      for (ICompiled expr : exprs) {
        if (verbose) {
          System.err.println("EXPR(" + file.getName() + "):\n" + expr + "\n------\n");
        }
        result = expr.evaluate(compiler.newBacktrace(), ctx);
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
}
