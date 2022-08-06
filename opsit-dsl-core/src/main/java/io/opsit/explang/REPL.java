package io.opsit.explang;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;
import java.util.Arrays;
import java.util.Set;
import io.opsit.explang.Compiler.ICtx;
import io.opsit.explang.algparser.AlgReader;
import io.opsit.explang.algparser.UCStringConverter;
import io.opsit.explang.reader.LispReader;

public class REPL {
  public static void main(String argv[])
    throws Exception {
    boolean verbose = false;
    boolean lineMode = false;
    int rc = 0;

    Set<String> packages = Compiler.getAllPackages();
    //Compiler compiler = null;
    IParser parser = new LispReader();
    Compiler.IStringConverter conv = new Compiler.NOPConverter();

    ICtx ctx = null;
    File f = null;
    for (int i = 0; i < argv.length; i++) {
      String val = argv[i];
      if ("-v".equals(val)) {
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
      if ("-a".equals(val)) {
        conv  = new UCStringConverter();
        parser = new AlgReader();
        // FIXME: make ANTLR work interactively
        lineMode = true;
        continue;
      }
      if ("-s".equals(val)) {
        parser = new SexpParser();
        continue;
      }
      f = new java.io.File(val);
    }
    Compiler compiler = new Compiler(conv, packages);
    compiler.setParser(parser);
    ctx = compiler.newCtx();
    if (null != f) {
      rc = runfile(f, verbose, ctx);
    } else {
      String inputName = "<STDIN%d>";
      InputStream is = System.in;
      Reader reader = new InputStreamReader(is);
      rc = repl(reader, verbose, inputName, ctx, lineMode);
    }
    System.exit(rc);
  }


  public static Set<String> parsePackages(String [] argv, int idx) {
    Set<String> packages = Utils.set();
    if (idx < argv.length) {
      String val = argv[idx];
      packages.addAll(Arrays.asList(val.split("\\s*,\\s*")));
    }
    return packages;
  }
    
  public static int runfile(File file, boolean verbose, ICtx ctx)
    throws Exception {
    Exception err = null;
    Object result = null;
    int rc = 0;
    try {
      ASTNList asts = new ParserWrapper(ctx.getCompiler().getParser()).parse(file);
      if (verbose) {
        System.err.println("AST(" + file.getName() + "):\n" + asts +"\n------\n");
      }
      final List<ICompiled> exprs = ctx.getCompiler().compileExpList(asts);
      for (ICompiled expr : exprs) {
        if (verbose) {
          System.err.println("EXPR(" + file.getName() + "):\n" + expr +"\n------\n");
        }   
        result = expr.evaluate(ctx.getCompiler().newBacktrace(), ctx);
        if (verbose) {
          System.err.println("RESULT(" + file.getName() + "):\n" + result +"\n------\n");
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
      System.err.print("RUNTIME ERROR: "+ex.getMessage());
      err = ex;
      rc = 4;
      if (null!=ex.getBacktrace()) {
        System.err.println(" at:\n" + ex.getBacktrace());
      } else {
        System.err.println();
      }
    } catch (Exception ex) {
      rc = 5;
      err = ex;
      System.err.println("ERROR: "+ex);
    } finally {
      if (null!=err && verbose) {
        err.printStackTrace(System.err);
      }
    }
    return rc;
  }

  public static int repl(Reader reader, boolean verbose, String inputName, ICtx ctx, boolean lineMode)
    throws java.io.IOException {
    Compiler compiler = ctx.getCompiler();
    IParser parser = compiler.getParser();
    System.out.print("Welcome to the EXPLANG REPL!\n"+
                     "Active parser is " +
                     parser.getClass().getSimpleName() +"\n" +
                     "Loaded packages are: " + compiler.getPackages() +"\n" +
                     "Please type an EXPLANG expression "+
                     (lineMode ? "followed by an extra NEWLINE" : "") + "\n");
    int inputNo = 0;
    Throwable err = null;
    Object result = null;
    Backtrace bt = compiler.newBacktrace();
    if (lineMode) {
      reader = new  BufferedReader(reader);
    }
    while (true) {
      try {
        ctx.getMappings().put("***", ctx.get("**", bt));
        ctx.getMappings().put("**", ctx.get("*", bt));
        ctx.getMappings().put("*", result);
        
        System.out.print("["+inputNo+"]> ");
        System.out.flush();
        
        err = null;

        ParseCtx pctx = new ParseCtx("INPUT"+(inputNo++));
        ASTNList exprs = null;
        if (lineMode) {
          StringBuilder buf = new StringBuilder();
          String line = null;
          while (null != (line = ((BufferedReader) reader).readLine())) {
            if (line.length() == 0) {
              break;
            }
            buf.append(line).append("\n");
            exprs = parser.parse( pctx, buf.toString(), Integer.MAX_VALUE);
            if (verbose) {
              System.out.println("\nPARSER RETURN: " +exprs);
            }
            if (null!=exprs && exprs.size() > 0 &&
                // we can parse the last expression =>
                // we accept list of all expressions.
                // FIXME: there should be a way to distinguish
                //        expressions that are incomplete
                //        and those that had errors but are
                //        known to be complete
                !exprs.get(exprs.size()-1).hasProblems()) {
              if (verbose) {
                System.out.println("\nLINE READER: read complete " +exprs.size()+ "expressions\n");
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
            System.out.println("\nINPUT:" + buf.toString()+"\n");
          }
        } else {
          exprs = parser.parse( pctx, reader, 1);
        }
        if (null == exprs) {
          continue;
        }
        for (ASTN exprASTN : exprs) {
          if (verbose) {
            System.out.println("\nAST:\n"+exprASTN+"\n------\n");
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
        System.err.println("COMPILATION ERROR: "+ ex.getMessage());
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

  private static String listParseErrors(ASTN exprASTN) {
    final StringBuilder buf = new StringBuilder();
    ASTN.Walker errCollector = new ASTN.Walker (){
        public void walk(ASTN node) {
          final Exception ex =  node.getProblem();
          if (null != ex) {
            buf.append(ex.getMessage()).append("\n");
          }
        }
      };
    exprASTN.dispatchWalker(errCollector);
    return buf.toString();
  }
}
