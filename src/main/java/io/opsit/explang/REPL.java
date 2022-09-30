package io.opsit.explang;

import io.opsit.explang.Compiler.ICtx;
import java.io.BufferedReader;
import java.io.Reader;
import java.util.List;

public class REPL implements IREPL {
  protected boolean verbose = false;
  protected boolean lineMode = false;
  protected Compiler compiler;
  protected IParser parser;

  protected IObjectWriter writer =
      new IObjectWriter() {
        @Override
        public String writeObject(Object obj) {
          return Utils.asString(obj);
        }
      };
  
  @Override
  public void setVerbose(boolean val) {
    this.verbose = val;
  }

  @Override
  public boolean getVerbose() {
    return this.verbose;
  }

  public Compiler getCompiler() {
    return this.compiler;
  }

  public void setCompiler(Compiler compiler) {
    this.compiler = compiler;
  }

  @Override
  public void setObjectWriter(IObjectWriter writer) {
    this.writer = writer;
  }

  @Override
  public IObjectWriter getObjectWriter() {
    return this.writer;
  }

  @Override
  public void setParser(IParser parser) {
    this.parser = parser;
  }

  @Override
  public IParser getParser() {
    return this.parser;
  }

  @Override
  public boolean getLineMode() {
    return lineMode;
  }

  @Override
  public void setLineMode(boolean lineMode) {
    this.lineMode = lineMode;
  }

  
  /**
   * Run interactive REPL loop.
   *
   * @param reader input stream
   * @param inputName name of input in REPL UI
   */
  public Integer execute(Reader reader, String inputName)
      throws java.io.IOException {
    getCompiler().setParser(getParser());
    ICtx ctx = getCompiler().newCtx();
    System.out.print(
        "Welcome to the EXPLANG REPL!\n"
            + "Active parser is "
            + parser.getClass().getSimpleName()
            + "\n"
            + "Loaded packages are: "
            + compiler.getPackages()
            + "\n"
            + "Please type an EXPLANG expression "
            + (lineMode ? "terminated by an extra NEWLINE" : "")
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
            if (exprs.size() > 0 && !exprs.hasProblems()) {
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
        if (exprs.hasProblems()) {
          System.out.println("Parser errors:");
          System.out.print(Utils.listParseErrors(exprs));
          if (verbose) {
            System.out.println("\nAST:\n" + exprs + "\n------\n");
          }
          result = null;
        } else {
          for (ASTN exprASTN : exprs) {
            if (verbose) {
              System.out.println("\nAST:\n" + exprASTN + "\n------\n");
            }
            if (exprASTN.hasProblems()) {
              // won't hapen
              System.out.println("Failed to parse expression:");
              System.out.print(Utils.listParseErrors(exprASTN));
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

  
}
