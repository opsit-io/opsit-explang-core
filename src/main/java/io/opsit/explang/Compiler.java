package io.opsit.explang;

import static io.opsit.explang.DWIM.*;
import static io.opsit.explang.Funcs.*;
import static io.opsit.explang.Seq.*;

import io.opsit.explang.Funcs.ObjectExp;
import io.opsit.explang.Funcs.READ_FROM_STRING;
import io.opsit.explang.parser.sexp.SexpParser;
import io.opsit.explang.strconv.nop.NopConverter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class Compiler {
  protected static Threads threads = new Threads();
  protected IParser parser = new SexpParser();
  // protected IParser parser = new LispReader();
  protected boolean failOnMissingVariables = true;
  protected IStringConverter funcNameConverter;
  protected Set<String> packages =
      Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
  protected boolean enforcePackages = true;
  protected List<String> argv = Utils.list();

  /** Return default list of enabled packages for a Compiler instance. */
  public static Set<String> getDefaultPackages() {
    // FIXME: make configurable
    return Utils.roset(
        Package.BASE_ARITHMENTICS,
        Package.BASE_LOGIC,
        Package.BASE_COERCION,
        Package.BASE_MATH,
        Package.BASE_TYPES,
        Package.BASE_BINDINGS,
        Package.BASE_SEQ,
        Package.BASE_FUNCS,
        Package.BASE_BEANS,
        Package.BASE_CONTROL,
        Package.BASE_REGEX,
        Package.BASE_TEXT,
        Package.BASE_VERSION,
        Package.BASE_DOCS,
        Package.BASE_LANG);
  }

  /** Return list of available packages. */
  public static Set<String> getAllPackages() {
    // FIXME: discover dynamically
    return Utils.roset(
        Package.BASE_ARITHMENTICS,
        Package.BASE_LOGIC,
        Package.BASE_COERCION,
        Package.BASE_MATH,
        Package.BASE_TYPES,
        Package.BASE_BINDINGS,
        Package.BASE_SEQ,
        Package.BASE_FUNCS,
        Package.BASE_BEANS,
        Package.BASE_CONTROL,
        Package.BASE_REGEX,
        Package.BASE_TEXT,
        Package.BASE_VERSION,
        Package.BASE_DOCS,
        Package.BASE_LANG,
        Package.DWIM,
        Package.FFI,
        Package.IO,
        Package.LOOPS,
        Package.RUNTIME,
        Package.THREADS);
  }

  /**
   * Construct compiler with default configuration.
   *
   * <p>The compiler created with NO OP string converter and with default list of enabled packages.
   */
  public Compiler() {
    this(new NopConverter(), getDefaultPackages());
  }

  /**
   * Construct compiler with given list of packages.
   *
   * <p>The compiler created with NO OP string converter and with given list of packages.
   */
  public Compiler(Set<String> packages) {
    this(new NopConverter(), packages);
  }

  /**
   * Construct compiler with given list of packages.
   *
   * <p>The compiler created with given function name converter and default list of packages.
   */
  public Compiler(IStringConverter converter) {
    this(converter, getDefaultPackages());
  }

  /** Construct compiler with given list of packages and with given function name converter. */
  public Compiler(IStringConverter fnameConverter, Set<String> packages) {
    super();
    this.setFnameConverter(fnameConverter);
    this.usePackages(packages);
    initBuiltins(this.getPackages());
  }

  /** Configure function name converter. */
  public void setFnameConverter(IStringConverter converter) {
    this.funcNameConverter = converter;
  }

  /** Configure behaviour on dereference of non-existing variables. */
  public void setFailOnMissingVariables(boolean val) {
    // FIXME: allow configuration on creation
    this.failOnMissingVariables = val;
  }

  /** Get configured parser. */
  public IParser getParser() {
    return parser;
  }

  /** Configure parser. */
  public void setParser(IParser parser) {
    // FIXME: make configurable at compiler creation time
    this.parser = parser;
  }

  /** Get status of package enforcement. */
  public boolean isEnforcePackages() {
    return this.enforcePackages;
  }

  /** Set status of package enforcement. */
  public void setEnforcePackages(boolean val) {
    this.enforcePackages = val;
  }

  /** Add package specifications to the list of used packages. */
  public void usePackages(String... pkgSpecs) {
    Set<String> pkgSet = new HashSet<String>();
    if (null != pkgSpecs) {
      for (String pkgSpec : pkgSpecs) {
        packages.add(pkgSpec);
        pkgSet.add(pkgSpec);
      }
    }
    initBuiltins(pkgSet);
  }

  /** Add package specifications to the list of used packages. */
  public void usePackages(Collection<String> pkgSpecs) {
    if (null != pkgSpecs) {
      for (String pkgSpec : pkgSpecs) {
        packages.add(pkgSpec);
      }
    }
    Set<String> pkgSet = new HashSet<String>();
    pkgSet.addAll(pkgSpecs);
    initBuiltins(pkgSet);
  }

  /** Return set of enabled packages. */
  public Set<String> getPackages() {
    return this.packages;
  }

  /** Configure set of enabled packages. */
  public void setPackages(Set<String> packages) {
    this.packages = packages;
  }

  /** Check if Builtin's package is contaimed in the set of packages. */
  public boolean checkBuiltinPackage(Class<?> cls, Set<String> pkgs) {
    final String name = this.getBuiltinPackage(cls);
    if (null != name) {
      return pkgs.contains(name);
    } else {
      return false;
    }
  }

  /** Get Builtin's package. */
  public String getBuiltinPackage(Class<?> cls) {
    final Package pkg =  cls.getAnnotation(Package.class);
    if (null != pkg) {
      return pkg.name();
    } else {
      return null;
    }
  }

  /**
   * Set command line args.
   */
  public void setCommandlineArgs(List<String> argv) {
    this.argv.clear();
    if (null != argv) {
      this.argv.addAll(argv);
    }
  }

  /**
   * Set command line args.
   */
  public List<String> getCommandlineArgs() {
    return this.argv;
  }
  
  /**
   * Add mapping for a Builtin.
   *
   * <p>Add builtin mapping. If packages are enforced the builtin will be checked against the list
   * of enabled packages.
   */
  public void addBuiltIn(String name, Class<?> cls) {
    if ((!isEnforcePackages()) || checkBuiltinPackage(cls, this.getPackages())) {
      final Builtin builtin =
          IForm.class.isAssignableFrom(cls) ? new BuiltinForm(cls) : new BuiltinFunc(cls);
      functab.put(this.funcNameConverter.convert(name), builtin);
    } else {
      throw new RuntimeException(
          "Won't add builtin with "
              + cls
              + ": class package '"
              + getBuiltinPackage(cls)
              + "' does not belong to any of "
              + "enabled packages");
    }
  }

  /** Abstract class for Builtins. */
  public abstract class Builtin implements ICode {
    protected Class<?> cls;

    public Builtin(Class<?> cls) {
      this.cls = cls;
    }

    @Override
    public boolean isBuiltIn() {
      return true;
    }

    @Override
    public String getDocstring() {
      Docstring ann = cls.getAnnotation(Docstring.class);
      return null == ann ? "Not provided." : ann.text();
    }

    @Override
    public String getArgDescr() {
      Arguments ann = cls.getAnnotation(Arguments.class);
      return null == ann
          ? "args ..."
          : Utils.asStringOrEmpty(
              null == ann.text() || ann.text().trim().isEmpty()
                  ? Utils.arrayContentsAsString(ann.spec())
                  : ann.text());
    }

    @Override
    public String getDefLocation() {
      return cls.toString();
    }

    @Override
    public String getPackageName() {
      Package ann = cls.getAnnotation(Package.class);
      return null == ann
        ? ""
        : Utils.asStringOrEmpty(ann.name());
    }

    @Override
    public ArgSpec getArgSpec() {
      final Arguments ann = cls.getAnnotation(Arguments.class);
      String[] spec = null;
      if (null != ann) {
        spec = ann.spec();
      }
      if (null != spec) {
        try {
          final ArgSpec argSpec = new ArgSpec(spec);
          return argSpec;
        } catch (InvalidParametersException ex) {
          throw new CompilationException("Invalid argument specification for " + this);
        }
      }
      return null;
    }
  }

  public class BuiltinForm extends Builtin {
    public BuiltinForm(Class<?> cls) {
      super(cls);
    }

    @Override
    public ICompiled getInstance() {
      try {
        Constructor<?> constructor = cls.getConstructor(Compiler.this.getClass());
        ICompiled compiled = (ICompiled) constructor.newInstance(Compiler.this);
        return compiled;
      } catch (InstantiationException ex) {
        throw new RuntimeException(ex.getMessage());
      } catch (InvocationTargetException ex) {
        throw new RuntimeException(ex.getMessage());
      } catch (NoSuchMethodException ex) {
        throw new RuntimeException(ex.getMessage());
      } catch (IllegalAccessException ex) {
        throw new RuntimeException(ex.getMessage());
      }
    }

    @Override
    public String getCodeType() {
      return "form";
    }
  }

  public class BuiltinFunc extends Builtin {
    public BuiltinFunc(Class<?> cls) {
      super(cls);
    }

    @Override
    public ICompiled getInstance() {
      try {
        Constructor<?> constructor = cls.getConstructor();
        ICompiled compiled = (ICompiled) constructor.newInstance();
        return compiled;
      } catch (InstantiationException ex) {
        throw new RuntimeException(ex.getMessage());
      } catch (InvocationTargetException ex) {
        throw new RuntimeException(ex.getMessage());
      } catch (NoSuchMethodException ex) {
        throw new RuntimeException(ex.getMessage());
      } catch (IllegalAccessException ex) {
        throw new RuntimeException(ex.getMessage());
      }
    }

    @Override
    public String getCodeType() {
      return "function";
    }
  }

  /**
   * Put function definition into function table.
   */
  public ICode putFun(String name, ICode code) {
    final String key = funcNameConverter.convert(name);
    if (null != code) {
      return functab.put(key, code);
    } else {
      return functab.remove(key);
    }
  }

  public ICode getFun(String name) {
    return functab.get(funcNameConverter.convert(name));
  }

  public Set<String> getFunKeys() {
    return functab.keySet();
  }

  private final Map<String, ICode> functab = new ConcurrentHashMap<String, ICode>();

  private void initBuiltins(Set<String> fromPackages) {
    // FIXME: make list of builtins configurable before initBuiltins.
    // arithmetics
    final List<Object> builtinsInit =
        Utils.list(
            "+", ADDOP.class,
            "-", SUBOP.class,
            "*", MULOP.class,
            "/", DIVOP.class,
            "%", REMOP.class,
            "MOD", MODOP.class,
            "REM", REMOP.class,
            // boolean algrebra
            "NOT", NOT.class,
            "AND", AND.class,
            "OR", OR.class,

            // numeric comparisons
            "=", NUMEQ.class,
            ">", NUMGT.class,
            ">=", NUMGE.class,
            "<=", NUMLE.class,
            "<", NUMLT.class,
            "MAX", MAXOP.class,
            "MIN", MINOP.class,
            // coercion
            "INT", INT.class,
            "SHORT", SHORT.class,
            "BYTE", BYTE.class,
            "LONG", LONG.class,
            "DOUBLE", DOUBLE.class,
            "FLOAT", FLOAT.class,
            "CHAR", CHAR.class,
            "BOOL", BOOL.class,
            "STRING", STRING.class,
            // math
            "SIGNUM", SIGNUM.class,
            "RANDOM", RANDOM.class,
            "SQRT", SQRT.class,
            "LOG", LOG.class,
            "EXP", EXP.class,
            // object comparisons
            // java .equals()
            "EQUAL", EQUAL.class,
            "===", EQ.class,
            "==", SEQUAL.class,
            // variables
            "SETQ", SETQ.class,
            "SETV", SETV.class,
            "SET", SET.class,
            "LET", LET.class,
            "DLET", DLET.class,
            "MAKUNBOUND", MAKUNBOUND.class,
            "BOUNDP", BOUNDP.class,
            "NEW-CTX", NEW_CTX.class,
            "WITH-CTX", WITH_CTX.class,
            "GETPROPS", GETPROPS.class,
            "SETPROPS", SETPROPS.class,
            "GETPROP", GETPROP.class,
            "SETPROP", SETPROP.class,

            // execution control
            "IF", IF.class,
            "COND", COND.class,
            "WHILE", WHILE.class,
            "FOREACH", FOREACH.class,
            "PROGN", PROGN.class,
            "TRY", TRY.class,
            "AS->", TH_AS.class,
            "->", TH_1ST.class,
            "->>", TH_LAST.class,
            "@->", TH_PIPE.class,

            // functional programming
            "LAMBDA", LAMBDA.class,
            "RETURN", RETURN.class,
            "FUNCALL", FUNCALL.class,
            "DEFUN", DEFUN.class,
            "FUNCTION", FUNCTION.class,
            "FSET", FSET.class,
            "SYMBOL-FUNCTION", SYMBOL_FUNCTION.class,
            "APPLY", APPLY.class,
            "EVAL", EVAL.class,
            "READ-FROM-STRING", READ_FROM_STRING.class,
            "FUNCTIONP", FUNCTIONP.class,
            "FUNCTIONS-NAMES", FUNCTIONS_NAMES.class,
            // compilation and evaluation
            "LOAD", LOAD.class,
            "LOADR", LOADR.class,
            "QUOTE", QUOTE.class,
            // threads
            "NEW-THREAD", NEW_THREAD.class,
            // I/O
            "PRINT", PRINT.class,
            "PRINTLN", PRINTLN.class,
            // RUNTIME
            "ARGV", ARGV.class,
            // sequences operations
            "LIST", LIST.class,
            "HASHSET", HASHSET.class,
            "NTH", NTH.class,
            "CONS", CONS.class,
            "APPEND", APPEND.class,
            "APPEND!", NAPPEND.class,
            "FIRST", FIRST.class,
            "REST", REST.class,
            "LENGTH", LENGTH.class,
            "SORT", SORT.class,
            "SORT!", NSORT.class,
            "REVERSE", REVERSE.class,
            "REVERSE!", NREVERSE.class,
            "COLLP", COLLP.class,
            "SEQP", SEQP.class,
            "MAPP", MAPP.class,
            "INDEXEDP", INDEXEDP.class,
            "ASSOCIATIVEP", ASSOCIATIVEP.class,
            "SETP", SETP.class,
            "RANGE", RANGE.class,
            "SUBSEQ", SUBSEQ.class,
            "TAKE", TAKE.class,
            "IN", IN.class,
            "GET-IN", GET_IN.class,
            "GET", GET.class,
            "PUT!", NPUT.class,
            "PUT-IN!", NPUT_IN.class,
            "ASSOC", ASSOC.class,
            "ASSOC!", NASSOC.class,
            "REPLACE", REPLACE.class,
            // java FFI
            ".", DOT.class,
            ".S", DOTS.class,
            ".N", DOTN.class,
            "CLASS", CLASS.class,
            "TYPE-OF", TYPE_OF.class,
            "BEAN", BEAN.class,
            "SELECT-KEYS", SELECT_KEYS.class,
            "DWIM_FIELDS", DWIM_FIELDS.class,
            "TYPEP", TYPEP.class,
            "COPY", COPY.class,
            // mapping functions
            "MAP", MAP.class,
            "MAPPROD", MAPPROD.class,
            "FILTER", FILTER.class,
            "REDUCE", REDUCE.class,
            // string handling
            "RE-PATTERN", RE_PATTERN.class,
            "RE-GLOB", RE_GLOB.class,
            "RE-MATCHER", RE_MATCHER.class,
            "RE-MATCHES", RE_MATCHES.class,
            "RE-GROUPS", RE_GROUPS.class,
            "RE-FIND", RE_FIND.class,
            "RE-SEQ", RE_SEQ.class,
            "DWIM-MATCHES", DWIM_MATCHES.class,
            "SEARCH", SEARCH.class,
            "UPPERCASE", UPPERCASE.class,
            "LOWERCASE", LOWERCASE.class,
            "STR", STR.class,
            "FORMAT", FORMAT.class,
            "STRING-BUFFER", STRINGBUFFER.class,
            "STRING-BUILDER", STRINGBUILDER.class,
            "SEQ", SEQ.class,
            "SYMBOL", SYMBOL.class,
            // hashtables
            "WITH-BINDINGS", WITH_BINDINGS.class,
            "HASHMAP", HASHMAP.class,
            // exception handling
            "BACKTRACE", BACKTRACE.class,
            "THROW", THROW.class,
            // arrays
            "MAKE-ARRAY", MAKE_ARRAY.class,
            "AREF", AREF.class,
            "ASET", ASET.class,
            // help system
            "DESCRIBE-FUNCTION", Funcs.DESCRIBE_FUNCTION.class,
            "DOCUMENTATION", Funcs.DOCUMENTATION.class,
            // versions
            "VERSION", Funcs.VERSION.class);

    for (int i = 0; i < builtinsInit.size(); i += 2) {
      final Class<?> builtinClass = (Class<?>) builtinsInit.get(i + 1);
      if (this.checkBuiltinPackage(builtinClass, fromPackages)) {
        addBuiltIn((String) builtinsInit.get(i), builtinClass);
      }
    }
  }

  /** Compile AST into executable data structures. */
  public ICompiled compile(ASTN ast) {
    if (ast.isList() && ((ASTNList) ast).isLiteralList()) {
      // 1. Compile List literal (may be initializer like [a b c] in some syntaxes)
      // FIXME: DRY with callfunc like AST manipulation
      //       should go in subroutine?
      //       unless we get rid of callfunc usage here (implement label?)
      ASTNList astList = ((ASTNList) ast);
      if (astList.size() == 0) {
        return new EmptyListExp();
      }
      ParseCtx pctx = ast.getPctx();
      ASTNList listfunc = new ASTNList(Utils.list(new ASTNLeaf(new Symbol("LIST"), pctx)), pctx);
      listfunc.addAll((ASTNList) ast);
      IExpr func = (IExpr) compile(listfunc);
      func.setDebugInfo(pctx);
      return func;
    } else if (ast.isList() && !((ASTNList) ast).isLiteralList()) {
      // 2. Usual form execution (form arg ...) or ((lambda) arg ..)

      ASTNList astList = ((ASTNList) ast);

      if (astList.size() == 0) {
        return new EmptyListExp();
      }

      ASTN firstASTN = astList.get(0);
      ParseCtx pctx = firstASTN.getPctx();
      ASTNList restASTN = astList.subList(1, astList.size());
      if (firstASTN.isList()) {
        // inline lambda expr
        LAMBDA compiledLambda = (LAMBDA) compile(firstASTN);
        ICompiled compiled = compiledLambda.getICode().getInstance();
        compiled.setDebugInfo(firstASTN.getPctx());
        try {
          if (compiled instanceof IForm) {
            ((IForm) compiled).setRawParams(restASTN);
          } else {
            List<ICompiled> compiledParams = compileExpList(restASTN);
            // FIXME???
            // compiled = compiled.getInstance();
            compiled.setDebugInfo(firstASTN.getPctx());
            ((IExpr) compiled).setParams(compiledParams);
          }
        } catch (InvalidParametersException ipex) {
          throw new CompilationException(ipex.getParseCtx(), ipex.getMessage());
        }
        return compiled;
      } else if (firstASTN.getObject() instanceof Symbol) {
        Object first = firstASTN.getObject();
        final String fName = ((Symbol) first).toString();
        ICode codeObj = getFun(fName);
        if (null != codeObj && codeObj.isBuiltIn()) {
          try {
            ICompiled compiled = codeObj.getInstance();
            if (compiled instanceof IForm) {
              ((IForm) compiled).setRawParams(restASTN);
            } else {
              List<ICompiled> compiledParams = compileExpList(restASTN);
              ((IExpr) compiled).setParams(compiledParams);
            }
            compiled.setName(fName);
            compiled.setDebugInfo(firstASTN.getPctx());
            return compiled;
          } catch (InvalidParametersException ipex) {
            throw new CompilationException(pctx, ipex.getMessage());
          } catch (RuntimeException rex) {
            // FIXME: specific exception for instantiation troubles?
            throw new CompilationException(
                pctx, "failed to compile list " + astList + ":" + rex.getMessage());
          }
        } else {
          // FIXME: must call it dynamycally, otherwise
          // we won't be able to perform recursive defun
          // so we rewrite expression to use function call

          // // defun
          // final ICompiled compiled = (ICompiled)fobj;
          // if (fobj instanceof IForm) {
          //  ((IForm)compiled).setRawParams(rest);
          // } else {
          //  List<ICompiled> compiledParams = compileParams(rest);
          //  ((IExpr)compiled).setParams(compiledParams);
          // }
          // return compiled;

          ASTNList callfunc =
              new ASTNList(
                  Utils.list(
                      new ASTNLeaf(new Symbol("FUNCALL"), pctx),
                      new ASTNList(
                          Utils.list(
                              new ASTNLeaf(new Symbol("FUNCTION"), pctx),
                              new ASTNLeaf(first, pctx)),
                          pctx)),
                  pctx);
          callfunc.addAll(restASTN);
          IExpr func = (IExpr) compile(callfunc);
          func.setDebugInfo(pctx);
          return func;
        }
      } else {
        throw new CompilationException(
            pctx,
            String.format(
                "failed to compile list " + astList + ": first element is of unsupported type"));
      }
    } else {
      // 3. standalone atom of some kind
      IExpr expr = null;
      final Object astObj = ast.getObject();
      if (astObj instanceof String) {
        expr = new StringExp((String) astObj);
      } else if (astObj instanceof Number) {
        expr = new NumberExp((Number) astObj);
      } else if (astObj instanceof Boolean) {
        expr = new BooleanExp((Boolean) astObj);
      } else if (astObj instanceof Keyword) {
        expr = new ObjectExp(astObj);
      } else if (astObj instanceof Symbol) {
        expr = new VarExp(((Symbol) astObj).toString());
      } else {
        expr = new ObjectExp(astObj);
      }
      expr.setDebugInfo(ast.getPctx());
      return expr;
    }
  }

  /** Base class for special forms and regular functions. */
  public abstract class AbstractForm implements IForm, Runnable {
    protected ParseCtx debugInfo;
    protected String name;

    public void setName(String name) {
      this.name = name;
    }

    protected String getName() {
      return null == name ? this.getClass().getSimpleName() : name;
    }

    public void setDebugInfo(ParseCtx debugInfo) {
      this.debugInfo = debugInfo;
    }

    public ParseCtx getDebugInfo() {
      return debugInfo;
    }

    /** Evaluate expression in new thread. */
    public void run() {
      final Compiler.ICtx ctx = Threads.contexts.get(Thread.currentThread());
      final Object result = this.evaluate(new Backtrace(), ctx);
      Threads.results.put(Thread.currentThread(), result);
    }

    protected abstract Object doEvaluate(Backtrace backtrace, ICtx ctx);

    @Override
    public final Object evaluate(Backtrace backtrace, ICtx ctx) {
      try {
        final StringBuilder b = new StringBuilder(16);
        b.append("(").append(getName()).append(")");
        backtrace.push(b.toString(), this.debugInfo, ctx);
        return doEvaluate(backtrace, ctx);
      } catch (ReturnException rex) {
        throw rex;
      } catch (ExecutionException ex) {
        throw ex;
      } catch (Throwable t) {
        throw new ExecutionException(backtrace, t);
      } finally {
        backtrace.pop();
      }
    }
  }

  @Docstring(text = "Get Function Given it's symbol.")
  @Package(name = Package.BASE_BINDINGS)
  public class FUNCTION extends AbstractForm {
    // FIXME: checks
    protected Symbol fsym;

    @Override
    public void setRawParams(ASTNList params) throws InvalidParametersException {
      if (params.size() != 1) {
        throw new InvalidParametersException(
            debugInfo, "FUNCTION expects one parameter, but got " + params.size());
      }
      Object sym = params.get(0).getObject();
      if (!(sym instanceof Symbol)) {
        throw new InvalidParametersException(
            debugInfo, "FUNCTION expects Symbol parameter, but got " + sym);
      }
      fsym = (Symbol) params.get(0).getObject();
    }

    @Override
    public ICode doEvaluate(Backtrace backtrace, ICtx ctx) {
      ICode code = getFun(fsym.toString());
      if (null == code) {
        throw new RuntimeException("Symbol " + fsym + " function value is NULL");
      }
      // final ICompiled result = code.getInstance();
      return code;
    }
  }

  @Docstring(text = "Evaluate sequence of expressions.")
  @Package(name = Package.BASE_CONTROL)
  public class PROGN extends AbstractForm {
    private List<ICompiled> blocks = null;

    public void setRawParams(ASTNList params) {
      this.blocks = compileExpList(params);
    }

    @Override
    public Object doEvaluate(Backtrace backtrace, ICtx ctx) {
      return evalBlocks(backtrace, blocks, ctx);
    }
  }

  @Docstring(text = "Try-Catch-Final construction.")
  @Package(name = Package.BASE_BINDINGS)
  public class TRY extends AbstractForm {
    private List<ICompiled> blocks = null;
    private List<CatchEntry> catches = null;
    private List<ICompiled> finalBlocks = null;

    private class CatchEntry {
      Class<? extends Throwable> exception;
      String varName;
      List<ICompiled> blocks;
    }

    @SuppressWarnings("unchecked")
    protected CatchEntry compileCatchBlock(ASTNList list) throws InvalidParametersException {
      CatchEntry result = new CatchEntry();
      ASTN exception = list.get(0);

      ASTN astnVar = list.get(1);
      if (!(astnVar.getObject() instanceof Symbol)) {
        throw new InvalidParametersException(
            debugInfo, "second catch parameter must be a variable name");
      }

      if (!(exception.getObject() instanceof Symbol)) {
        throw new InvalidParametersException(
            debugInfo, "first catch parameter must be name of exception class");
      }
      Symbol excSym = (Symbol) exception.getObject();
      try {
        result.exception =
            (Class<? extends Throwable>)
                this.getClass().getClassLoader().loadClass(excSym.getName());
      } catch (ClassNotFoundException clnf) {
        throw new InvalidParametersException(
            debugInfo, "catch: invalid exception class specified: '" + excSym.getName() + "'");
      }
      result.varName = ((Symbol) astnVar.getObject()).getName();
      result.blocks = compileExpList(list.subList(2, list.size()));
      return result;
    }

    @Override
    public void setRawParams(ASTNList params) throws InvalidParametersException {
      for (int i = 0; i < params.size(); i++) {
        ASTN param = params.get(i);
        ASTNList paramsList;
        ASTN first;
        if (param.isList() && ((paramsList = ((ASTNList) param)).size() > 0)) {
          first = paramsList.get(0);
          if (first.getObject() instanceof Symbol) {
            Symbol sym = (Symbol) first.getObject();
            if ("CATCH".equals(sym.getName())) {
              if (null == catches) {
                catches = new ArrayList<CatchEntry>();
              }
              catches.add(compileCatchBlock(paramsList.subList(1, paramsList.size())));
              continue;
            } else if ("FINALLY".equals(sym.getName())) {
              if (null == finalBlocks) {
                finalBlocks = new ArrayList<ICompiled>();
              }
              finalBlocks.addAll(compileExpList(paramsList.subList(1, paramsList.size())));
              continue;
            }
          }
        }
        if (null == blocks) {
          blocks = new ArrayList<ICompiled>();
        }
        blocks.add(compile(param));
      }
    }

    @Override
    public Object doEvaluate(Backtrace backtrace, ICtx ctx) {
      if (null == blocks || blocks.size() == 0) {
        return null;
      }
      if (null == catches || catches.size() == 0) {
        try {
          return evalBlocks(backtrace, blocks, ctx);
        } finally {
          if (null != finalBlocks) {
            evalBlocks(backtrace, finalBlocks, ctx);
          }
        }
      }

      try {
        return evalBlocks(backtrace, blocks, ctx);
      } catch (ReturnException r) {
        throw r;
      } catch (Throwable t) {
        Throwable realT = t;
        if ((t instanceof ExecutionException) && t.getCause() != null) {
          // should always happen
          realT = t.getCause();
        }
        for (int i = 0; i < catches.size(); i++) {
          CatchEntry ce = catches.get(i);
          if (ce.exception.isAssignableFrom(realT.getClass())) {
            ICtx catchCtx = newCtx(ctx);
            catchCtx.put(ce.varName, realT);
            return evalBlocks(backtrace, ce.blocks, catchCtx);
          }
        }

        if (t instanceof ExecutionException) {
          throw t;
        } else {
          // should not happen
          throw new ExecutionException(backtrace, realT);
        }
      } finally {
        if (null != finalBlocks) {
          evalBlocks(backtrace, finalBlocks, ctx);
        }
      }
    }
  }

  @Docstring(
      text =
          "While loop construction. "
              + "Execute sequnce of expressions while the consition is true")
  @Package(name = Package.LOOPS)
  public class WHILE extends AbstractForm {
    private List<ICompiled> blocks = null;
    private ICompiled condition = null;

    @Override
    public void setRawParams(ASTNList params) throws InvalidParametersException {
      if (params.size() < 2) {
        throw new InvalidParametersException(debugInfo, "WHILE expects at least 2 parameters");
      }
      this.condition = compile(params.get(0));
      this.blocks = compileExpList(params.subList(1, params.size()));
    }

    @Override
    public Object doEvaluate(Backtrace backtrace, ICtx ctx) {
      Object rc = null;
      while (Utils.asBoolean(condition.evaluate(backtrace, ctx))) {
        rc = evalBlocks(backtrace, blocks, ctx);
      }
      return rc;
    }
  }

  @Arguments(
      spec = {
        "(",
        "VAR",
        "SEQUENCE",
        ArgSpec.ARG_OPTIONAL,
        "RESULT",
        ")",
        ArgSpec.ARG_REST,
        "body"
      })
  @Docstring(
      text =
          "Foreach Loop over a sequence. \n"
              + "Evaluate body with VAR bound to each element from SEQUENCE, in turn.\n"
              + "Then evaluate RESULT to get return value, default NIL.")
  @Package(name = Package.BASE_SEQ)
  public class FOREACH extends AbstractForm {
    private Symbol loopVar = null;
    private List<ICompiled> blocks = null;
    private ICompiled seqexpr = null;
    private ICompiled resultExpr = null;

    @Override
    public void setRawParams(ASTNList params) throws InvalidParametersException {
      if (params.size() < 2) {
        throw new InvalidParametersException(
            debugInfo,
            "FOREACH expects at least 2 parameters: loop-args and one execution block at least");
      }
      final ASTNList loopPars = ((ASTNList) params.get(0));
      final ASTN loopVarASTN = loopPars.get(0);
      if (loopVarASTN.isList()) {
        throw new InvalidParametersException(
            debugInfo, "FOREACH expects first loop argument cannot be list");
      }
      final Object loopVarObj = loopVarASTN.getObject();
      if (!(loopVarObj instanceof Symbol)) {
        throw new InvalidParametersException(
            debugInfo, "FOREACH expects first loop argument must be loop variable");
      }
      this.loopVar = (Symbol) loopVarObj;
      this.seqexpr = compile(loopPars.get(1));
      this.resultExpr = (loopPars.size() > 2) ? compile(loopPars.get(2)) : null;
      this.blocks = compileExpList(params.subList(1, params.size()));
    }

    @Override
    public Object doEvaluate(Backtrace backtrace, ICtx ctx) {
      final Object seq = seqexpr.evaluate(backtrace, ctx);
      final ICtx loopCtx = ctx.getCompiler().newCtx(ctx);
      final String loopVarName = loopVar.getName();
      loopCtx.replace(loopVarName, null);
      Seq.forEach(
          seq,
          new Seq.Operation() {
            @Override
            public boolean perform(Object item) {
              loopCtx.replace(loopVarName, item);
              evalBlocks(backtrace, blocks, loopCtx);
              return false;
            }
          },
          false);
      loopCtx.replace(loopVarName, null);
      final Object result = null == resultExpr ? null : resultExpr.evaluate(backtrace, loopCtx);
      return result;
    }
  }

  @Docstring(
      text =
          "Conditional switch construct. "
              + "COND allows the execution of forms to be dependent on test-form.\n"
              + "Test-forms are evaluated one at a time in the order in\n"
              + "which they are given in the argument list until a\n"
              + "test-form is found that evaluates to true.  If there\n"
              + "are no forms in that clause, the value of the test-form\n"
              + "is returned by the COND form.  Otherwise, the forms\n"
              + "associated with this test-form are evaluated in order,\n"
              + "left to right and the value returned by the last form\n"
              + "is returned by the COND form.  Once one test-form has\n"
              + "yielded true, no additional test-forms are\n"
              + "evaluated. If no test-form yields true, nil is\n"
              + "returned.")
  @Package(name = Package.BASE_CONTROL)
  @Arguments(text = "{test-form form*}*")
  public class COND extends AbstractForm {
    private List<List<ICompiled>> forms = null;
    private List<ICompiled> testForms = null;

    @Override
    public void setRawParams(ASTNList params) throws InvalidParametersException {
      final int numParms = params.size();
      this.forms = new ArrayList<List<ICompiled>>(numParms);
      this.testForms = new ArrayList<ICompiled>(numParms);
      for (final ASTN clause : params) {
        if (!clause.isList()) {
          throw new InvalidParametersException(debugInfo, "COND expects clauses to be lists.");
        }
        ASTNList entryClauses = ((ASTNList) clause);
        if (null == entryClauses || entryClauses.isEmpty()) {
          throw new InvalidParametersException(
              debugInfo, "COND expects non-empty clauses that contain at least test-form.");
        }
        final ICompiled testClause = compile(entryClauses.get(0));
        this.testForms.add(testClause);
        List<ICompiled> clauseForms = new ArrayList<ICompiled>(entryClauses.size() - 1);
        for (int i = 1; i < entryClauses.size(); i++) {
          clauseForms.add(compile(entryClauses.get(i)));
        }
        this.forms.add(clauseForms);
      }
    }

    @Override
    public Object doEvaluate(Backtrace backtrace, ICtx ctx) {
      final int numClauses = testForms.size();
      for (int i = 0; i < numClauses; i++) {
        final Object testObj = testForms.get(i).evaluate(backtrace, ctx);
        if (Utils.asBoolean(testObj)) {
          final List<ICompiled> blocks = forms.get(i);
          return blocks.isEmpty() ? testObj : evalBlocks(backtrace, blocks, ctx);
        }
      }
      return null;
    }
  }

  @Docstring(text = "Threading form on &PIPE or first argument")
  @Package(name = Package.BASE_CONTROL)
  public class TH_PIPE extends TH_X {
    protected ASTNList insertVar(ASTNList expr) {
      // this.getDebugInfo().
      int idx = 1;
      final List<ASTN> lst = Utils.list();
      lst.addAll(expr.getList());
      final ASTN objASTN = lst.get(0);
      // FIXME: ugly and must not be here
      if (objASTN instanceof ASTNLeaf) {
        Object obj = objASTN.getObject();
        if (obj instanceof Symbol) {
          String name = ((Symbol) obj).getName();
          ICode code = getFun(name);
          if (null != code) {
            ArgSpec argspec = code.getArgSpec();
            if (null != argspec && null != argspec.getArgs()) {
              int argIdx = 0;
              for (ArgSpec.Arg arg : argspec.getArgs()) {
                if (arg.isPipe()) {
                  // list contains function itself
                  if (argIdx + 1 <= lst.size()) {
                    idx = argIdx + 1;
                  }
                  break;
                }
                argIdx++;
              }
            }
          }
        }
      }
      lst.add(idx, new ASTNLeaf(new Symbol(getVarName()), expr.getPctx()));
      final ASTNList result = new ASTNList(lst, expr.getPctx());
      return result;
    }
  }

  @Docstring(text = "Threading form on last argument.")
  @Package(name = Package.BASE_CONTROL)
  public class TH_LAST extends TH_X {
    protected ASTNList insertVar(ASTNList expr) {
      List<ASTN> lst = Utils.list();
      lst.addAll(expr.getList());
      lst.add(new ASTNLeaf(new Symbol(getVarName()), expr.getPctx()));
      ASTNList result = new ASTNList(lst, expr.getPctx());
      return result;
    }
  }

  @Docstring(text = "Threading form on first argument")
  @Package(name = Package.BASE_SEQ)
  public class TH_1ST extends TH_X {
    protected ASTNList insertVar(ASTNList expr) {
      List<ASTN> lst = Utils.list();
      lst.addAll(expr.getList());
      lst.add(1, new ASTNLeaf(new Symbol(getVarName()), expr.getPctx()));
      ASTNList result = new ASTNList(lst, expr.getPctx());
      return result;
    }
  }

  public abstract class TH_BASE extends AbstractForm {
    protected List<ICompiled> blocks = new ArrayList<ICompiled>();
    protected ICompiled startExpr = null;

    protected abstract String getVarName();

    @Override
    public Object doEvaluate(Backtrace backtrace, ICtx ctx) {
      ICtx localCtx = new Ctx(ctx);
      Object result = startExpr.evaluate(backtrace, localCtx);
      for (ICompiled block : blocks) {
        localCtx.getMappings().put(getVarName(), result);
        result = block.evaluate(backtrace, localCtx);
      }
      return result;
    }
  }

  public abstract class TH_X extends TH_BASE {
    // ( -> expr  (expr)*)
    @Override
    protected String getVarName() {
      return "%%";
    }

    @Override
    public void setRawParams(ASTNList params) throws InvalidParametersException {
      if (params.size() < 1) {
        throw new InvalidParametersException(
            debugInfo, "Threading Form:  expects at least 1 parameters");
      }
      this.startExpr = compile(params.get(0));
      for (int i = 1; i < params.size(); i++) {
        ASTN expr = params.get(i);
        if (expr instanceof ASTNList) {
          ASTNList blockExpr = insertVar((ASTNList) expr);
          blocks.add(compile(blockExpr));
        } else {
          throw new InvalidParametersException(
              "Threading form: argument " + i + " must be a function call, but got " + expr);
        }
      }
    }

    protected abstract ASTNList insertVar(ASTNList expr);
  }

  @Docstring(text = "Threading form on named argument")
  @Package(name = Package.BASE_CONTROL)
  public class TH_AS extends TH_BASE {
    // ( as-> expr name  (expr)*)
    protected String varName;

    @Override
    protected String getVarName() {
      return varName;
    }

    @Override
    public void setRawParams(ASTNList params) throws InvalidParametersException {
      if (params.size() < 2) {
        throw new InvalidParametersException(
            debugInfo, "Threading Form: expect at least 2 parameters");
      }
      this.startExpr = compile(params.get(0));

      final ICompiled varExp = compile(params.get(1));
      if (!(varExp instanceof VarExp)) {
        throw new InvalidParametersException(
            debugInfo, "Threading Form: Invalid 2nd parameter, must be a variable name");
      }
      this.varName = ((VarExp) varExp).getName();
      for (int i = 2; i < params.size(); i++) {
        blocks.add(compile(params.get(i)));
      }
    }
  }

  @Package(name = Package.BASE_CONTROL)
  @Docstring(text = "If-else conditional construct.")
  public class IF extends AbstractForm {
    private List<ICompiled> elseBlocks = null;
    private ICompiled condition = null;
    private ICompiled thenBlock = null;

    @Override
    public void setRawParams(ASTNList params) throws InvalidParametersException {
      if (params.size() < 2) {
        throw new InvalidParametersException(debugInfo, "IF expects at least 2 parameters");
      }
      this.condition = compile(params.get(0));
      this.thenBlock = compile(params.get(1));
      this.elseBlocks = compileExpList(params.subList(2, params.size()));
    }

    protected Object evalWithArgs(Backtrace backtrace, List<Object> eargs, ICtx ctx) {
      Object condVal = eargs.get(0);
      if (Utils.asBoolean(condVal)) {
        return thenBlock.evaluate(backtrace, ctx);
      } else {
        return evalBlocks(backtrace, elseBlocks, ctx);
      }
    }

    protected List<Object> evaluateParameters(Backtrace backtrace, ICtx ctx) {
      List<Object> eargs = new ArrayList<Object>(1);
      eargs.add(condition.evaluate(backtrace, ctx));
      return eargs;
    }

    @Override
    public Object doEvaluate(Backtrace backtrace, ICtx ctx) {
      return evalWithArgs(backtrace, evaluateParameters(backtrace, ctx), ctx);
    }
  }

  /** (DEFUN FOO (arg1 arg2...) block block...) */
  @Package(name = Package.BASE_FUNCS)
  @Docstring(text = "Define named function")
  public class DEFUN extends LAMBDA {
    // FIXME: this field is needed/ WHY?
    private String name;

    @Override
    public ICode doEvaluate(Backtrace backtrace, ICtx ctx) {
      final ICode obj = super.doEvaluate(backtrace, ctx);
      // FIXME!
      // obj.setName(name);
      // obj.setDebugInfo(this.debugInfo);
      functab.put(Compiler.this.funcNameConverter.convert(name), obj);
      return obj;
    }

    @Override
    public void setRawParams(ASTNList params) throws InvalidParametersException {
      if (params.size() < 2) {
        throw new InvalidParametersException(
            debugInfo, "DEFUN expects at least 2 parameters, but got " + params.size());
      }
      final ASTNLeaf first = (ASTNLeaf) params.get(0);
      final Object sym = first.getObject();
      if (!(sym instanceof Symbol)) {
        throw new InvalidParametersException(
            debugInfo, "DEFUN expects Symbol as first parameter, but got " + sym);
      }
      this.name = sym.toString();
      final ASTNList second = (ASTNList) params.get(1);
      this.argSpec = new ArgSpec(second, Compiler.this);
      this.blocks = compileExpList(params.subList(2, params.size()));
    }
  }

  @Docstring(text = "Define anonymous function")
  @Package(name = Package.BASE_CONTROL)
  public class LAMBDA extends AbstractForm {
    protected ArgSpec argSpec;
    protected List<ICompiled> blocks;

    @Override
    public void setRawParams(ASTNList params) throws InvalidParametersException {
      if (params.size() < 1) {
        throw new InvalidParametersException(
            debugInfo, "LAMBDA expects at least 1 parameter, but got " + params.size());
      }
      final ASTN first = params.get(0);
      if (!first.isList()) {
        throw new InvalidParametersException(
            debugInfo, "LAMBDA expects first parameter to be a list");
      }
      this.argSpec = new ArgSpec((ASTNList) first, Compiler.this);
      this.blocks = compileExpList(params.subList(1, params.size()));
    }

    @Override
    public ICode doEvaluate(Backtrace backtrace, ICtx ctx) {
      return getICode();
    }

    protected ICode getICode() {
      return new ICode() {
        @Override
        public Funcs.AbstractExpr getInstance() {
          return new Funcs.AbstractExpr() {
            protected ArgList argList;

            @Override
            public Object doEvaluate(Backtrace backtrace, ICtx ctx) {
              Eargs localCtx = argList.evaluateArguments(backtrace, ctx);
              try {
                return evalBlocks(backtrace, blocks, localCtx);
              } catch (ReturnException rex) {
                return rex.getPayload();
              }
            }

            @Override
            public void setParams(List<ICompiled> params) throws InvalidParametersException {
              if (null != this.argList) {
                throw new RuntimeException("internal error: parameters already set");
              }
              this.argList = new ArgList(argSpec, params);
            }
          };
        }

        @Override
        public boolean isBuiltIn() {
          return false;
        }

        @Override
        public String getDocstring() {
          return (null != blocks && blocks.size() > 0 && (blocks.get(0) instanceof Funcs.StringExp))
              ? (String) ((Funcs.StringExp) blocks.get(0)).getValue()
              : "N/A";
        }

        @Override
        public String getArgDescr() {
          return null == argSpec ? "args" : argSpec.asSpecList().toString();
        }

        @Override
        public String getCodeType() {
          return "compiled function";
        }

        @Override
        public String getDefLocation() {
          return "" + LAMBDA.this.getDebugInfo();
        }

        @Override
        public ArgSpec getArgSpec() {
          return argSpec;
        }

        @Override
        public String getPackageName() {
          return "user";
        }
      };
    }
  }

  @Docstring(text = "Destructuring LET construct.")
  @Package(name = Package.BASE_BINDINGS)
  public class DLET extends AbstractForm {
    protected List<ICompiled> blocks = null;
    protected ICompiled listExpr = null;
    protected List<String> varNames = new ArrayList<String>();

    @Override
    public void setRawParams(ASTNList params) throws InvalidParametersException {
      if (params.size() < 2) {
        throw new InvalidParametersException(
            debugInfo, getName() + " expects at least 2 parameters");
      }
      if (!(params.get(0).isList())) {
        throw new InvalidParametersException(
            debugInfo, getName() + " expects first parameter" + "to be list of var names");
      }
      // handle variable mappings
      // @SuppressWarnings("unchecked")
      ASTNList varDefs = (ASTNList) params.get(0);

      for (int i = 0; i < varDefs.size(); i++) {
        ASTN varDef = varDefs.get(i);
        ICompiled varExp = compile(varDef);
        if (!(varExp instanceof VarExp)) {
          throw new InvalidParametersException(
              debugInfo,
              getName() + ": Invalid #" + (i + 1) + "parameter, must be a variable name");
        }
        // locals = new Ctx();
        VarExp var = (VarExp) varExp;
        String varName = var.getName();
        varNames.add(varName);
        listExpr = compile(params.get(1));
      }
      // handle executable blocks
      this.blocks = compileExpList(params.subList(2, params.size()));
    }

    @Override
    public Object doEvaluate(Backtrace backtrace, ICtx ctx) {
      ICtx localCtx = new Ctx(ctx);
      Object listVals = listExpr.evaluate(backtrace, localCtx);
      if (!(listVals instanceof List)) {
        throw new RuntimeException(
            getName() + " second parameter was expected to evaluate to list, but got " + listVals);
      }
      for (int i = 0; i < varNames.size(); i++) {
        String varName = varNames.get(i);
        localCtx.put(varName, ((List<?>) listVals).get(i));
      }
      return evalBlocks(backtrace, blocks, localCtx);
    }
  }

  @Docstring(text = "Return its argument without evaluation.")
  @Package(name = Package.BASE_LANG)
  public class QUOTE extends AbstractForm {
    Object value = null;

    @Override
    public void setRawParams(ASTNList params) throws InvalidParametersException {
      if (params.size() != 1) {
        throw new InvalidParametersException(
            debugInfo, this.getName() + " expects exactly one parameter");
      }
      this.value = Utils.unAstnize(params.get(0));
    }

    @Override
    protected Object doEvaluate(Backtrace backtrace, ICtx ctx) {
      return value;
    }
  }

  @Docstring(text = "Evaluate code with bindings from a Java Map.")
  @Package(name = Package.BASE_BINDINGS)
  public class WITH_BINDINGS extends AbstractForm {
    protected List<ICompiled> blocks = null;
    ICompiled bindExpr = null;

    @Override
    public void setRawParams(ASTNList params) throws InvalidParametersException {
      if (params.size() < 1) {
        throw new InvalidParametersException(
            debugInfo, getName() + " expects at least 2 parameters");
      }
      this.bindExpr = compile(params.get(0));
      this.blocks = compileExpList(params.subList(1, params.size()));
    }

    @Override
    public Object doEvaluate(Backtrace backtrace, ICtx ctx) {
      final Object bindObj = bindExpr.evaluate(backtrace, ctx);
      if (!(bindObj instanceof Map)) {
        throw new ExecutionException(
            backtrace,
            getName() + " expects a java.lang.Map instance as first parameter, but got " + bindObj);
      }
      @SuppressWarnings("unchecked")
      final ICtx localCtx = new Ctx(ctx, (Map<String, Object>) bindObj);
      return evalBlocks(backtrace, blocks, localCtx);
    }
  }

  @Docstring(text = "Evaluate code in given context.")
  @Package(name = Package.BASE_BINDINGS)
  public class WITH_CTX extends AbstractForm {
    protected List<ICompiled> blocks = null;
    ICompiled bindExpr = null;

    @Override
    public void setRawParams(ASTNList params) throws InvalidParametersException {
      if (params.size() < 1) {
        throw new InvalidParametersException(
            debugInfo, getName() + " expects at least 2 parameters");
      }
      this.bindExpr = compile(params.get(0));
      this.blocks = compileExpList(params.subList(1, params.size()));
    }

    @Override
    public Object doEvaluate(Backtrace backtrace, ICtx ctx) {
      final Object bindObj = bindExpr.evaluate(backtrace, ctx);
      if (!(bindObj instanceof ICtx)) {
        throw new ExecutionException(
            backtrace,
            getName() + " expects a ICtx instance as first parameter, but got " + bindObj);
      }
      final ICtx localCtx = (ICtx) bindObj;
      return evalBlocks(backtrace, blocks, localCtx);
    }
  }

  public abstract class ABSTRACT_SET_OP extends AbstractForm {
    VarExp[] variables;
    ICompiled[] values;

    @Override
    public void setRawParams(ASTNList params) throws InvalidParametersException {
      final int numParams = params.size();
      if ((numParams & 0x1) == 1) {
        throw new InvalidParametersException(
            debugInfo, "expected even number of parameters, but got " + numParams);
      }
      final int numPairs = numParams >> 1;
      this.variables = new VarExp[numPairs];
      this.values = new ICompiled[numPairs];
      for (int i = 0; i < numParams; i += 2) {
        final ASTN varDef = params.get(i);
        final ICompiled varExp = compile(varDef);
        if (!(varExp instanceof VarExp)) {
          throw new InvalidParametersException(
              debugInfo, "SETV: Invalid parameter[" + i + "], must be a variable name");
        }
        final int pairPos = i >> 1;
        this.variables[pairPos] = (VarExp) varExp;
        this.values[pairPos] = compile(params.get(i + 1));
      }
    }

    @Override
    public Object doEvaluate(Backtrace backtrace, ICtx ctx) {
      final int numVars = this.variables.length;
      Object val = null;
      for (int i = 0; i < numVars; i++) {
        val = values[i].evaluate(backtrace, ctx);
        final String name = variables[i].getName();
        setvar(name, val, ctx);
      }
      return val;
    }

    protected abstract void setvar(String name, Object val, ICtx ctx);
  }

  @Arguments(text = "{var form}*")
  @Package(name = Package.BASE_BINDINGS)
  @Docstring(
      text =
          "Varianle assignmentm, global if not exists. If variable is already assigned it's value"
              + " will be replaced by result of form evaluation. If not new global binding will be"
              + " created in the global scope\n"
              + "var - a symbol naming a variable.\n"
              + "form - an expression, which evaluation result will be assigned to var\n"
              + "Returns: value of the last form, or nil if no pairs were supplied.")
  public class SETV extends ABSTRACT_SET_OP {
    @Override
    protected void setvar(String name, Object val, ICtx ctx) {
      ctx.replace(name, val);
    }
  }

  @Arguments(text = "{var form}*")
  @Package(name = Package.BASE_BINDINGS)
  @Docstring(
      text =
          "Variable assignment, glocal if not exists. If variable is already assigned it's value"
              + " will be replaced by result of form evaluation. If not new global binding will be"
              + " created in the global scope\n"
              + "var - a symbol naming a variable.\n"
              + "form - an expression, which evaluation result will be assigned to var\n"
              + "Returns: value of the last form, or nil if no pairs were supplied.")
  public class SETQ extends ABSTRACT_SET_OP {
    @Override
    protected void setvar(String name, Object val, ICtx ctx) {
      ctx.greplace(name, val);
    }
  }

  @Docstring(text = "Evaluate code with given var. bindings.")
  @Package(name = Package.BASE_BINDINGS)
  public class LET extends AbstractForm {
    protected List<ICompiled> blocks = null;
    protected List<ICompiled> varExprs = new ArrayList<ICompiled>();
    protected List<String> varNames = new ArrayList<String>();

    @Override
    public void setRawParams(ASTNList params) throws InvalidParametersException {
      if (params.size() < 2) {
        throw new InvalidParametersException(debugInfo, "LET expects at least 2 parameters");
      }
      if (!(params.get(0).isList())) {
        throw new InvalidParametersException(
            debugInfo, "LET expects first parameter" + "to be list of (var value) pairs");
      }
      // handle variable mappings
      // @SuppressWarnings("unchecked")
      ASTNList varDefs = (ASTNList) params.get(0);
      for (int i = 0; i < varDefs.size(); i++) {
        if (!(varDefs.get(i).isList())) {
          throw new InvalidParametersException(
              debugInfo, "LET: Invalid " + (i + 1) + " parameter, expected list");
        }
        // @SuppressWarnings("unchecked")
        ASTNList pair = ((ASTNList) varDefs.get(i));
        if (pair.size() < 1) {
          throw new InvalidParametersException(
              debugInfo,
              "LET: Invalid " + (i + 1) + " parameter, expected at least one value in list");
        }
        ICompiled varExp = compile(pair.get(0));
        if (!(varExp instanceof VarExp)) {
          throw new InvalidParametersException(
              debugInfo,
              "LET: Invalid " + (i + 1) + " parameter, first value must be a variable name");
        }
        // locals = new Ctx();
        VarExp var = (VarExp) varExp;
        ICompiled expr;
        if (pair.size() > 1) {
          expr = compile(pair.get(1));
        } else {
          expr = new ObjectExp(null);
          expr.setDebugInfo(varExp.getDebugInfo());
        }
        String varName = var.getName();
        varNames.add(varName);
        varExprs.add(expr);
      }
      // handle executable blocks
      this.blocks = compileExpList(params.subList(1, params.size()));
    }

    @Override
    public Object doEvaluate(Backtrace backtrace, ICtx ctx) {
      ICtx localCtx = new Ctx(ctx);
      for (int i = 0; i < varNames.size(); i++) {
        String varName = varNames.get(i);
        ICompiled varExpr = varExprs.get(i);
        Object val = varExpr.evaluate(backtrace, localCtx);
        localCtx.put(varName, val);
      }
      return evalBlocks(backtrace, blocks, localCtx);
    }
  }

  // FIXME: implementation not finished
  // TODO: support lambda with normal arguments (like filter)
  //       make useful beans of basic types like  numbers, strings
  //       handle errors (how?)
  //       return same type of sequence
  //       let user name variable
  //       allow search on several sequences (JOIN)
  @Arguments(spec = {"sequence", "test"})
  @Package(name = Package.DWIM)
  @Docstring(text = "Perform DWIM search of an item in a sequence of objects. ")
  public class SEARCH extends AbstractForm {
    protected ICompiled input;
    protected ICompiled predicate;
    protected ASTN testASTN;

    @Override
    public void setRawParams(ASTNList params) throws InvalidParametersException {
      if (params.size() != 2) {
        throw new InvalidParametersException(
            debugInfo, "SEARCH expects 2 parameters: sequence, test");
      }
      input = compile(params.get(0));
      ASTN test = params.get(1);
      testASTN = test;
      if (test instanceof ASTNLeaf) {
        Object testObj = test.getObject();
        if ((testObj instanceof CharSequence)
            || (testObj instanceof Pattern)
            || (testObj instanceof Number)) {
          ParseCtx pctx = test.getPctx();
          test =
              new ASTNList(
                  Utils.list(
                      new ASTNLeaf(new Symbol("DWIM-MATCHES"), pctx),
                      new ASTNLeaf(new Symbol("_"), pctx),
                      test),
                  pctx);
        }
      }
      predicate = compile(test);
    }

    @Override
    public Object doEvaluate(Backtrace backtrace, ICtx ctx) {
      final ICtx localCtx = new Ctx(ctx);
      final Object val = input.evaluate(backtrace, localCtx);
      // FIXME: same sequence
      List<Object> result = Utils.list();
      // if (!isSequence(val)) {
      //    // FIXME: what do we do with it?
      //    return val;
      // }
      forEach(
          val,
          new Operation() {
            @Override
            public boolean perform(Object obj) {
              @SuppressWarnings("unchecked")
              Map<String, Object> objMap =
                  (obj instanceof Map) ? (Map<String, Object>) obj : new Funcs.BeanMap(obj);
              final ICtx checkCtx = new Ctx(ctx, objMap, true);
              checkCtx.getMappings().put("_", obj);
              Object chkResult = null;
              // try {
              chkResult = predicate.evaluate(backtrace, checkCtx);
              // } catch (Exception ex) {
              // ???
              // }
              if (Utils.asBoolean(chkResult)) {
                result.add(obj);
              }
              return false;
            }
          },
          true);
      return result;
    }
  }

  // **** Context

  /**
   * Context - The execution context containing Map of values of dynamic variables, map of variables
   * properties and link to parent context.
   */
  public interface ICtx {
    public ICtx getPrev();

    public ICtx getLevel0();

    public void onMissingVar(String varname);

    // public Object get(String name);
    public Object get(String name, Backtrace bt);

    public Map<Object, Object> getProps(String name, Backtrace bt);

    public Object getProp(String name, Object prop, Backtrace bt);

    public boolean contains(String name);

    // put value in the outer context
    public void put(String name, Object expr);

    // replace value if found at some depth, otherwise
    // perform put()
    public void putProp(String name, Object pkey, Object pval);

    public void putProps(String name, Object... pobjs);

    public void putProps(String name, Map<Object, Object> props);

    public void replace(String name, Object expr);

    // replace value if found at some depth, otherwise
    // place it in global scope
    public void greplace(String name, Object expr);

    public Compiler getCompiler();

    public String toStringSelf();

    public String toStringShort();

    public Map<String, Object> getMappings();

    public Map<String, Map<Object, Object>> getPropsMap();

    public Map<String, Object> findMatches(String pattern);

    public void addMatches(Map<String, Object> matches, String pattern);

    public void remove(String name);

    public List<ICtx> getParentContexts();
  }

  public interface IMissHandler {
    Object handleMiss(ICtx ctx, String key);
  }

  public static class NilMissHandler implements IMissHandler {
    public Object handleMiss(ICtx ctx, String key) {
      return null;
    }
  }

  public static class ErrorMissHandler implements IMissHandler {
    public Object handleMiss(ICtx ctx, String key) {
      throw new RuntimeException("variable '" + key + "' does not exist in this context");
    }
  }

  public class Ctx implements ICtx {
    final Map<String, Object> mappings;
    final Map<String, Map<Object, Object>> propsMap = new HashMap<String, Map<Object, Object>>();
    ICtx prev;
    IMissHandler missHandler = new NilMissHandler();

    protected Map<String, Object> mkMappings() {
      return new HashMap<String, Object>();
    }

    protected void initCtxSettings() {
      setMissHandler(
          Compiler.this.failOnMissingVariables ? new ErrorMissHandler() : new NilMissHandler());
    }

    public Map<String, Map<Object, Object>> getPropsMap() {
      return propsMap;
    }

    @Override
    public Map<Object, Object> getProps(String name, Backtrace bt) {
      ICtx ctx = this.findCtxFor(name);
      return (null == ctx) ? null : ctx.getPropsMap().get(name);
    }

    @Override
    public Object getProp(String name, Object prop, Backtrace bt) {
      ICtx ctx = this.findCtxFor(name);
      if (null != ctx) {
        final Map<Object, Object> pmap = ctx.getPropsMap().get(name);
        if (null != pmap) {
          return pmap.get(prop);
        }
      }
      return null;
    }

    /**
     * Set variable property.
     *
     * @param name variable name
     * @param pkey property key
     * @param pval property value     
     */
    public void putProp(String name, Object pkey, Object pval) {
      getPropsMapForPut(name).put(pkey, pval);
    }

    /**
     * Set variable properties.
     *
     * @param name of variable
     * @param pobjs list of pairs (property name, property value)
     */
    public void putProps(String name, Object... pobjs) {
      Map<Object, Object> pmap = getPropsMapForPut(name);
      for (int i = 0; i < pobjs.length; i += 2) {
        pmap.put(pobjs[i], pobjs[i + 1]);
      }
    }

    public void putProps(String name, Map<Object, Object> newProps) {
      getPropsMapForPut(name).putAll(newProps);
    }

    protected Map<Object, Object> getPropsMapForPut(String name) {
      ICtx ctx = this.findCtxFor(name);
      if (null == ctx) {
        throw new RuntimeException(
            "Failed to put properties: there is no variable mapping for " + name);
      }
      Map<Object, Object> props = ctx.getPropsMap().get(name);
      if (null == props) {
        props = new HashMap<Object, Object>();
        ctx.getPropsMap().put(name, props);
      }
      return props;
    }

    @Override
    public void onMissingVar(String varname) {
      missHandler.handleMiss(this, varname);
    }

    public void setMissHandler(IMissHandler handler) {
      this.missHandler = handler;
    }

    public IMissHandler getMissHandler() {
      return this.missHandler;
    }

    public Ctx(Map<String, Object> vars) {
      this(null, vars, false);
    }

    public Ctx(ICtx prev, Map<String, Object> vars) {
      this(prev, vars, false);
    }

    /**
     * Make context based on parent context and map of value mappings.
     *
     * <p>If roCtx is true will be created context with empty mappings with parentcontext as
     * described above.
     *
     * @param prev Parent context
     * @param vars Variable mappings
     * @param roCtx make extra child context.
     */
    public Ctx(ICtx prev, Map<String, Object> vars, boolean roCtx) {
      super();
      initCtxSettings();
      if (roCtx) {
        this.mappings = mkMappings();
        this.prev = new Ctx(prev, vars, false);
      } else {
        this.mappings = (null == vars) ? mkMappings() : vars;
        this.prev = prev;
      }
    }

    /** Build empty context. */
    public Ctx() {
      this(null, null, false);
    }

    /** Create child context linking to previous context. */
    public Ctx(ICtx prev) {
      this(prev, null, false);
    }

    /**
     * Create child context linking to ctx as previous context and adding all local mapping from the
     * locals context.
     */
    public Ctx(ICtx locals, ICtx ctx) {
      this();
      mappings.putAll(locals.getMappings());
      prev = ctx;
    }

    /** Return variable value. */
    public Object get(String name, Backtrace bt) {
      Object val;
      if (null == (val = mappings.get(name))) {
        return mappings.containsKey(name) ? null : (null == prev ? null : prev.get(name, bt));
      } else {
        return val;
      }
    }

    protected ICtx findCtxFor(String name) {
      ICtx ctx = this;
      while (null != ctx && (!ctx.getMappings().containsKey(name))) {
        ctx = ctx.getPrev();
      }
      return ctx;
    }

    protected ICtx findCtxOrLocal(String name) {
      ICtx ctx = findCtxFor(name);
      return null == ctx ? this : ctx;
    }

    protected ICtx findCtxOrGlobal(String name) {
      ICtx ctx = findCtxFor(name);
      return null == ctx ? this.getLevel0() : ctx;
    }

    protected Map<String, Object> getMappingsOrLocal(String name) {
      return findCtxOrLocal(name).getMappings();
    }

    protected Map<String, Object> getMappingsOrGlobal(String name) {
      return findCtxOrGlobal(name).getMappings();
    }

    /** Check existance of variable mapping with given name. */
    public boolean contains(String name) {
      if (mappings.containsKey(name)) {
        return true;
      } else if (null == prev) {
        return false;
      } else {
        return prev.contains(name);
      }
    }

    /** Create new variable mapping. */
    public void put(String name, Object expr) {
      Object prev = mappings.put(name, expr);
      // FIXME: bug - if var was spreviously set to null
      if (null != prev) {
        throw new RuntimeException("trying to overwrite variable " + name + " in context " + this);
      }
    }

    /** Get compiler that created this context. */
    public Compiler getCompiler() {
      return Compiler.this;
    }

    /** Print out context with its content. */
    public String toString() {
      StringBuffer buf = new StringBuffer();
      buf.append("Ctx<").append(this.hashCode()).append("[");
      for (String var : mappings.keySet()) {
        buf.append("\n").append(var).append(" -> ").append(mappings.get(var));
        if (propsMap.containsKey(var)) {
          buf.append(" (").append(propsMap.get(var));
        }
      }
      buf.append(mappings).append("] \n");
      if (null != prev) {
        buf.append("\n -->").append(prev.toString());
      }
      return buf.toString();
    }

    /** Prunt out string identification of context. */
    public String toStringSelf() {
      StringBuffer buf = new StringBuffer();
      buf.append("Ctx<").append(this.hashCode()).append(">");
      return buf.toString();
    }

    /** Print out context contents. */
    public String toStringShort() {
      StringBuffer buf = new StringBuffer();
      buf.append(this.toStringSelf()).append("=").append(mappings);
      if (null != prev) {
        buf.append("->").append(prev.toStringSelf());
      }
      return buf.toString();
    }

    @Override
    public ICtx getPrev() {
      return this.prev;
    }

    public ICtx getLevel0() {
      final ICtx prev = getPrev();
      return null == prev ? this : prev.getLevel0();
    }

    @Override
    public Map<String, Object> getMappings() {
      return this.mappings;
    }

    @Override
    public void replace(String name, Object value) {
      this.getMappingsOrLocal(name).put(name, value);
    }

    @Override
    public void greplace(String name, Object value) {
      this.getMappingsOrGlobal(name).put(name, value);
    }

    @Override
    public List<ICtx> getParentContexts() {
      final List<ICtx> parents = new ArrayList<ICtx>();
      ICtx parent = this.getPrev();
      while (parent != null) {
        parents.add(parent);
        parent = parent.getPrev();
      }
      return parents;
    }

    @Override
    public Map<String, Object> findMatches(String patternStr) {
      final Map<String, Object> matches = new HashMap<String, Object>();
      this.addMatches(matches, patternStr);
      return matches;
    }

    // FIXME: should not be part of interface?
    @Override
    public void addMatches(Map<String, Object> matches, String pattern) {
      Set<Entry<String, Object>> eset = this.getMappings().entrySet();
      for (Entry<String, Object> e : eset) {
        final String key = e.getKey();
        if (e.getKey().startsWith(pattern)) {
          if (!matches.containsKey(key)) {
            matches.put(key, e.getValue());
          }
        }
      }
      if (null != this.getPrev()) {
        getPrev().addMatches(matches, pattern);
      }
    }

    @Override
    public void remove(String name) {
      final Map<String, Object> mappings = getMappings();
      if (mappings.containsKey(name)) {
        mappings.remove(name);
      }
      ICtx ctx = getPrev();
      if (null != ctx) {
        ctx.remove(name);
      }
    }
  }

  public ICtx newCtx() {
    final Ctx ctx = new Ctx();
    return ctx;
  }

  public ICtx newCtx(ICtx ctx) {
    return new Ctx(ctx);
  }

  public ICtx newCtx(Map<String, Object> vars) {
    return new Ctx(vars);
  }

  public ICtx newCtxFromROMap(Map<String, Object> vars) {
    ICtx ctx = new Ctx(vars);
    return newCtx(ctx);
  }

  public Backtrace newBacktrace() {
    return new Backtrace();
  }

  // ***** Utility functions

  /** Evaluate block of compiled expressions. */
  public Object evalBlocks(Backtrace backtrace, List<ICompiled> blocks, ICtx ctx) {
    Object result = null;
    for (ICompiled block : blocks) {
      result = block.evaluate(backtrace, ctx);
    }
    return result;
  }

  /*public List<ICompiled> compileExpList(ASTNList nodes) {
  List<ICompiled> exprs = new ArrayList<ICompiled>(nodes.size());
  for (ASTN node : nodes) {
    exprs.add(compile(node));
  }
  return exprs;
  }*/

  /** Compile parameters. */
  public List<ICompiled> compileExpList(ASTNList params) {
    List<ICompiled> compiledParams = new ArrayList<ICompiled>(params.size());
    for (ASTN param : params) {
      compiledParams.add(compile(param));
    }
    return compiledParams;
  }

  // ***** Evaluated Arguments
  /** Context adepted for function arguments. */
  public class Eargs extends Ctx {
    private Object[] eargs;
    private ArgList argList;

    /** Create arguments list. */
    public Eargs(Object[] eargs, boolean[] needEval, ArgList argList, ICtx ctx) {
      super(ctx);
      if ((null == argList) || (null == eargs) || (null == ctx)) {
        throw new RuntimeException("Internal error: null constructor parameter when creating Earg");
      }
      this.eargs = eargs;
      // this.needEval = needEval;
      this.argList = argList;
    }

    @Override
    public void replace(final String name, final Object val) {
      if (argList.getSpec().isParameterVar(name)) {
        this.getMappings().put(name, val);
      } else {
        super.replace(name, val);
      }
    }

    @Override
    public boolean contains(String name) {
      final ArgSpec spec = argList.getSpec();
      return (spec.nameToIdx(name) >= 0) || (spec.svarNameToIdx(name) >= 0) || super.contains(name);
    }

    /** Get evaluated argument by its number. */
    public Object get(int argIdx, Backtrace backtrace) {
      final Object val = eargs[argIdx];
      if (val instanceof LazyEval) {
        return ((LazyEval) val).getValue(backtrace);
      } else {
        return val;
      }
    }

    @Override
    public Object get(String name, Backtrace bt) {
      // first hash map (function args may be overridden)
      if (this.getMappings().containsKey(name)) {
        return this.getMappings().get(name);
      }
      // now positionals
      final ArgSpec spec = argList.getSpec();
      final int idx = spec.nameToIdx(name);
      if (idx >= 0) {
        return this.get(idx, bt);
      }
      // now svars
      final int svarIdx = spec.svarNameToIdx(name);
      if (svarIdx >= 0) {
        return argList.setFlags[svarIdx];
      }
      // now default behaviour
      return (null == prev ? null : prev.get(name, bt));
      // return super.get(name, bt);
    }

    public int size() {
      return eargs.length;
    }
  }

  public Eargs newEargs(Object[] result, boolean[] needEval, ArgList argList, ICtx ctx) {
    return new Eargs(result, needEval, argList, ctx);
  }
}
