package org.dudinea.explang;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.dudinea.explang.Funcs.ObjectExp;
import org.dudinea.explang.Funcs.READ_FROM_STRING;
import org.dudinea.explang.reader.LispReader;

import static org.dudinea.explang.Funcs.*;

public class Compiler {
    protected static Threads threads = new Threads();
    protected IParser parser = new SexpParser();
    //protected IParser parser = new LispReader();
    protected boolean failOnMissingVariables = true;

    public void setFailOnMissingVariables(boolean val) {
        this.failOnMissingVariables = val;
    }
    
    public IParser getParser() {
        return parser;
    }

    public void setParser(IParser parser) {
	this.parser = parser;
    }

    public void addBuiltIn(String name, Class cls) {
	functab.put(name,
		    IForm.class.isAssignableFrom(cls) ?
		    new BuiltinForm(cls) : new BuiltinFunc(cls));
    }

    public abstract class Builtin implements ICode {
	protected Class cls;
	public Builtin(Class cls) {
	    this.cls = cls;
	}

	@Override
	public boolean isBuiltIn() {
	    return true;
	}
	@Override
	public String getDocstring() {
	    Docstring ann = (Docstring)cls.getAnnotation(Docstring.class);
	    return null == ann ? "Not provided." : ann.text();
	}

	@Override
	public String getArgDescr() {
	    Arguments ann = (Arguments)cls.getAnnotation(Arguments.class);
	    return null == ann ? "args" :
		Utils.asStringOrEmpty(null == ann.text() || ann.text().trim().isEmpty() ?
				      Utils.arrayAsString(ann.spec()) :  ann.text());
	}

	@Override
	public String getDefLocation() {
	    return cls.toString();
	}
	
    }
    
    public class BuiltinForm extends Builtin {
	public BuiltinForm(Class cls) {
	    super(cls);
	}
	
	@Override
	public ICompiled getInstance() {
	    try {
		Constructor<?> constructor = cls.getConstructor(Compiler.this.getClass());
		ICompiled compiled = (ICompiled)constructor.newInstance(Compiler.this);
		return compiled;
	    } catch (InstantiationException ex) {
		throw new RuntimeException(ex.getMessage());
	    } catch (InvocationTargetException ex) {
		throw new RuntimeException(ex.getMessage());
	    } catch (NoSuchMethodException ex) {
		throw new RuntimeException(ex.getMessage());
	    } catch (IllegalAccessException  ex) {
		throw new RuntimeException(ex.getMessage());
	    }
	}

	@Override
	public String getCodeType() {
	    return "form";
	}
    }

    public class BuiltinFunc extends Builtin {
	public BuiltinFunc(Class cls) {
	    super(cls);
	}

	@Override
	public ICompiled getInstance() {
	    try {
		Constructor<?> constructor = cls.getConstructor();
		ICompiled compiled = (ICompiled)constructor.newInstance();
		return compiled;
	    } catch (InstantiationException ex) {
		throw new RuntimeException(ex.getMessage());
	    } catch (InvocationTargetException ex) {
		throw new RuntimeException(ex.getMessage());
	    } catch (NoSuchMethodException ex) {
		throw new RuntimeException(ex.getMessage());
	    } catch (IllegalAccessException  ex) {
		throw new RuntimeException(ex.getMessage());
	    }
	}

	@Override
	public String getCodeType() {
	    return "function";
	}
	
    }

    
    public  final Map <String, ICode>functab = new ConcurrentHashMap <String,ICode>();
    {
        // arithmetics
        addBuiltIn("+", ADDOP.class); 
        addBuiltIn("-", SUBOP.class); 
        addBuiltIn("*", MULOP.class);
        addBuiltIn("/", DIVOP.class);
        addBuiltIn("%", REMOP.class);
        addBuiltIn("MOD", MODOP.class);
        addBuiltIn("REM", REMOP.class);
        // boolean algrebra
        addBuiltIn("NOT", NOT.class);
        addBuiltIn("AND", AND.class);
        addBuiltIn("OR", OR.class);

        // numeric comparisons
        addBuiltIn("=", NUMEQ.class);
        addBuiltIn(">", NUMGT.class);
        addBuiltIn(">=", NUMGE.class);
        addBuiltIn("<=", NUMLE.class);
        addBuiltIn("<", NUMLT.class);
	// coercion
	addBuiltIn("INT", INT.class);
	addBuiltIn("SHORT", SHORT.class);
	addBuiltIn("BYTE", BYTE.class);
	addBuiltIn("LONG", LONG.class);
	addBuiltIn("DOUBLE", DOUBLE.class);	
	addBuiltIn("FLOAT", FLOAT.class);
	addBuiltIn("CHAR", CHAR.class);
	addBuiltIn("BOOL", BOOL.class);
	addBuiltIn("STRING", STRING.class);
	// math
	addBuiltIn("SIGNUM", SIGNUM.class);
	addBuiltIn("RANDOM", RANDOM.class);
        // object comparisons
        // java .equals()
        addBuiltIn("EQUAL", EQUAL.class);
	// variables
	addBuiltIn("SETQ", SETQ.class);	
	addBuiltIn("SETV", SETV.class);
	addBuiltIn("SET", SET.class);
        addBuiltIn("LET", LET.class);
	addBuiltIn("DLET", DLET.class);
	addBuiltIn("MAKUNBOUND", MAKUNBOUND.class);

	addBuiltIn("NEW-CTX", NEW_CTX.class);
	addBuiltIn("WITH-CTX", WITH_CTX.class);
	// TODO: 
	//addBuiltIn("GETPROPS", GETPROPS.class);
	//addBuiltIn("SETPROPS", SETPROPS.class);
	//addBuiltIn("GETPROP",  GETPROP.class);	
	//addBuiltIn("SETPROP",  SETPROP.class);
	
	// execution control
        addBuiltIn("IF", IF.class);
	addBuiltIn("COND", COND.class); 
	addBuiltIn("WHILE", WHILE.class);
	addBuiltIn("FOREACH", FOREACH.class);	
        addBuiltIn("PROGN", PROGN.class);
	addBuiltIn("TRY", TRY.class);
	addBuiltIn("BOUNDP", BOUNDP.class);
        // functional programming
        addBuiltIn("LAMBDA", LAMBDA.class);
        addBuiltIn("FUNCALL", FUNCALL.class);
        addBuiltIn("DEFUN", DEFUN.class);
        addBuiltIn("FUNCTION", FUNCTION.class);
	addBuiltIn("SYMBOL-FUNCTION", SYMBOL_FUNCTION.class);
        addBuiltIn("APPLY", APPLY.class);
        addBuiltIn("EVAL", EVAL.class);
	addBuiltIn("READ-FROM-STRING", READ_FROM_STRING.class);
	addBuiltIn("FUNCTIONP", FUNCTIONP.class);
	addBuiltIn("FUNCTIONS-NAMES", FUNCTIONS_NAMES.class);
	// compilation and evaluation
	addBuiltIn("LOAD", LOAD.class);
	addBuiltIn("LOADR", LOADR.class);
        addBuiltIn("QUOTE", QUOTE.class);
	// threads
	addBuiltIn("NEW-THREAD", NEW_THREAD.class);
        // I/O
        addBuiltIn("PRINT", PRINT.class);
        // sequences operations
        addBuiltIn("LIST", LIST.class);
        addBuiltIn("NTH", NTH.class);  
        addBuiltIn("CONS", CONS.class);
        addBuiltIn("APPEND", APPEND.class);
	addBuiltIn("APPEND!", NAPPEND.class);
        addBuiltIn("FIRST", FIRST.class);
        addBuiltIn("REST", REST.class);
        addBuiltIn("LENGTH", LENGTH.class);
	addBuiltIn("SORT", SORT.class);
	addBuiltIn("SORT!", NSORT.class);
	addBuiltIn("REVERSE", REVERSE.class);
	addBuiltIn("REVERSE!", NREVERSE.class);
	addBuiltIn("SEQUENCEP", SEQUENCEP.class);
	addBuiltIn("RANGE", RANGE.class);
	addBuiltIn("SUBSEQ", SUBSEQ.class);
        // java FFI
        addBuiltIn(".", DOT.class);
        addBuiltIn(".S", DOTS.class);
        addBuiltIn(".N", DOTN.class);
        addBuiltIn("CLASS", CLASS.class);
        addBuiltIn("TYPE-OF", TYPE_OF.class);
        addBuiltIn("BEAN", BEAN.class);
        addBuiltIn("TYPEP", TYPEP.class);
        // mapping functions
        addBuiltIn("MAP", MAP.class);
        addBuiltIn("MAPPROD", MAPPROD.class);
        addBuiltIn("FILTER", FILTER.class);
	addBuiltIn("REDUCE", REDUCE.class);
        // string handling
	addBuiltIn("RE-PATTERN", RE_PATTERN.class);
	addBuiltIn("RE-MATCHER", RE_MATCHER.class);
	addBuiltIn("RE-MATCHES", RE_MATCHES.class);
	addBuiltIn("RE-GROUPS", RE_GROUPS.class);
	addBuiltIn("RE-FIND", RE_FIND.class);
	addBuiltIn("RE-SEQ", RE_SEQ.class);
        addBuiltIn("STR", STR.class);
        addBuiltIn("FORMAT", FORMAT.class);
        addBuiltIn("SEQ", SEQ.class);
        addBuiltIn("SYMBOL", SYMBOL.class);
        // hashtables
        addBuiltIn("WITH-BINDINGS", WITH_BINDINGS.class);
        addBuiltIn("HASHMAP", HASHMAP.class);
        // exception handling
        addBuiltIn("BACKTRACE", BACKTRACE.class);
        addBuiltIn("THROW", THROW.class);
	// arrays
	addBuiltIn("ASET", ASET.class);
	addBuiltIn("MAKE-ARRAY", MAKE_ARRAY.class);
	addBuiltIn("AREF", AREF.class);
	// help system
	addBuiltIn("DESCRIBE-FUNCTION", Funcs.DESCRIBE_FUNCTION.class);
	addBuiltIn("DOCUMENTATION", Funcs.DOCUMENTATION.class);
    }
    
    public  ICompiled compile(ASTN ast) {
        if (ast.isList()) {
            //@SuppressWarnings("unchecked")
            ASTNList astList = ((ASTNList)ast);

            if (astList.size() == 0) {
                return new EmptyListExp();
            }

            ASTN firstASTN = astList.get(0); 
            ParseCtx pctx  = firstASTN.getPctx();
            ASTNList restASTN = astList.subList(1, astList.size());
            if (firstASTN.isList()) {
                // inline lambda expr
		LAMBDA compiledLambda  =  (LAMBDA)compile(firstASTN);
                ICompiled compiled  = compiledLambda.getICode().getInstance();
                compiled.setDebugInfo(firstASTN.getPctx());
                try {
                    if (compiled instanceof IForm) {
                        ((IForm)compiled).setRawParams(restASTN);
                    } else {
                        List<ICompiled> compiledParams = compileParams(restASTN);
			// FIXME???
			//compiled = compiled.getInstance();
			compiled.setDebugInfo(firstASTN.getPctx());
                        ((IExpr)compiled).setParams(compiledParams);
                    }
                } catch (InvalidParametersException ipex) {
                    throw new CompilationException(ipex.getParseCtx(),ipex.getMessage());
                }
                return compiled;
            } else if (firstASTN.getObject() instanceof Symbol) {
                Object first   = firstASTN.getObject();
                final String fName = ((Symbol)first).toString();
                ICode codeObj = functab.get(fName);
		if (null != codeObj && codeObj.isBuiltIn()) {
		    try {
			ICompiled compiled = codeObj.getInstance();
			if (compiled instanceof IForm) {
			    ((IForm)compiled).setRawParams(restASTN);
			} else {
			    List<ICompiled> compiledParams = compileParams(restASTN);
			    ((IExpr)compiled).setParams(compiledParams);
			}
			compiled.setName(fName);
			compiled.setDebugInfo(firstASTN.getPctx());
			return compiled;
		    } catch (InvalidParametersException ipex) {
			throw new CompilationException(pctx, ipex.getMessage());
		    } catch (RuntimeException rex) {
			// FIXME: specific exception for instantiation troubles?
			throw new CompilationException(pctx,
						       "failed to compile list "+
						       astList+":"+
						       rex.getMessage());
						   
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
			new ASTNList(Utils.list(new ASTNLeaf(new Symbol("FUNCALL"), pctx),
					    new ASTNList(Utils.list(new ASTNLeaf(new Symbol("FUNCTION"), pctx),
								new ASTNLeaf(first, pctx)), pctx)), pctx);
                    callfunc.addAll(restASTN);
                    IExpr func = (IExpr)compile(callfunc);
                    func.setDebugInfo(pctx);
		    return func;
		}
            } else {
                throw new CompilationException(pctx, String.format("failed to compile list "+
                                                                   astList+
                                                                   ": first element is of unsupported type"));
            }
        } else {
            IExpr expr = null;
	    final Object astObj = ast.getObject();
            if (astObj instanceof String) {
                expr = new StringExp((String)astObj);
            } else if (astObj instanceof Number) {
                expr = new NumberExp((Number)astObj);
            } else if (astObj instanceof Boolean) {
                expr = new BooleanExp((Boolean)astObj);
	    } else if (astObj instanceof Keyword) {
		expr = new ObjectExp(astObj);
            } else if (astObj instanceof Symbol) {
                expr = new VarExp(((Symbol)astObj).toString());
            } else {
                expr = new ObjectExp(astObj);
            }
            expr.setDebugInfo(ast.getPctx());
            return expr;
        }
        //throw new CompilationException(ast.getPctx(), "This cannot happen: reached end of compile for AST "+ast);
    }

    
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

	public void run () {
	    final Compiler.ICtx ctx =
		Threads.contexts.get(Thread.currentThread());
	    final Object result =
		this.evaluate(new Backtrace(), ctx);
	    Threads.results.put(Thread.currentThread(),result);
	}
	
        protected abstract Object doEvaluate(Backtrace backtrace,ICtx  ctx);
        @Override
        final public Object evaluate(Backtrace backtrace,ICtx  ctx) {
            try {
		final StringBuilder b = new StringBuilder(16);
		b.append("(").append(getName()).append(")");
                backtrace.push(b.toString(),
                               this.debugInfo,
                               ctx);
                return doEvaluate(backtrace, ctx);
            } catch (ExecutionException ex) {
                throw ex;
            } catch (Throwable t) {
                throw new ExecutionException(backtrace,t);
            } finally {
                backtrace.pop();
            } 
        }
    }
    
    public class FUNCTION extends AbstractForm {
        // FIXME: checks
        protected Symbol fsym;

        public void setRawParams(ASTNList params)
            throws InvalidParametersException {
            if (params.size()!=1) {
                throw new InvalidParametersException(debugInfo,
                                                     "FUNCTION expects one parameter, but got "
                                                     +params.size());
            }
            Object sym = params.get(0).getObject();
            if (!(sym instanceof Symbol)) {
                throw new InvalidParametersException(debugInfo,
                                                     "FUNCTION expects Symbol parameter, but got "
                                                     +sym);
            }
            fsym = (Symbol)params.get(0).getObject();
        }

        @Override
        public ICode doEvaluate(Backtrace backtrace,ICtx  ctx) {
            ICode code  = functab.get(fsym.toString());
            if  (null == code) {
                throw new RuntimeException("Symbol "+fsym+" function value is NULL");
	    }
	    //final ICompiled result = code.getInstance();
            return code;
        }
    }
    
    public  class PROGN extends AbstractForm {
        private List<ICompiled> blocks = null;
        public void setRawParams(ASTNList params) {
            this.blocks = compileParams(params);
        }
        
        @Override
        public Object doEvaluate(Backtrace backtrace,ICtx  ctx) {
            return evalBlocks(backtrace, blocks, ctx);
        }
    }



    public  class TRY extends AbstractForm {
	private List<ICompiled> blocks = null;
        private List<CatchEntry> catches = null;
        private List<ICompiled> finalBlocks = null;
	private class CatchEntry {
	    Class exception;
	    String varName;
	    List<ICompiled> blocks;
	}

	public CatchEntry compileCatchBlock(ASTNList list) throws
	    InvalidParametersException {
	    CatchEntry result = new CatchEntry();
	    ASTN exception = list.get(0);

	    ASTN astnVar = list.get(1);
	    if (!(astnVar.getObject() instanceof Symbol)) {
		throw new InvalidParametersException(debugInfo,
						     "second catch parameter must be a variable name" );
	    }

	    if (!(exception.getObject() instanceof Symbol)) {
		throw new InvalidParametersException(debugInfo,
						     "first catch parameter must be name of exception class");
	    }
	    Symbol excSym = (Symbol)exception.getObject();
	    try {
		result.exception = this.getClass()
		    .getClassLoader()
		    .loadClass(excSym.getName());
	    } catch (ClassNotFoundException clnf) {
		throw new InvalidParametersException(debugInfo,
						     "catch: invalid exception class specified: '" + excSym.getName() +"'");
	    }
	    result.varName=((Symbol)astnVar.getObject()).getName();
	    result.blocks = compileParams(list.subList(2, list.size()));
	    return  result;
	}

        public void setRawParams(ASTNList params)
            throws InvalidParametersException {
	    for (int i = 0; i < params.size(); i++) {
		ASTN param = params.get(i);
		ASTNList  pList;
		ASTN first;
		if (param.isList() &&
		    ((pList = ((ASTNList)param)).size() > 0)) {
		    first = pList.get(0);
		    if (first.getObject() instanceof Symbol) {
			Symbol s = (Symbol) first.getObject();
			if ("CATCH".equals(s.getName())) {
			    if (null == catches) {
				catches = new ArrayList();
			    }
			    catches.add(compileCatchBlock(pList
							  .subList(1,pList.size())));
			    continue;
			} else if ("FINALLY".equals(s.getName())) {
			    if (null == finalBlocks) {
				finalBlocks =  new ArrayList();
			    }
			    finalBlocks.
				addAll(compileParams(pList.subList(1, pList.size())));
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
	public Object doEvaluate(Backtrace backtrace,ICtx  ctx) {
	    if (null ==  blocks || blocks.size() == 0) {
		return null;
	    }
	    if (null == catches || catches.size() == 0) {
		try {
		    return evalBlocks(backtrace, blocks, ctx);
		}  finally {
		    if (null != finalBlocks) {
			evalBlocks(backtrace, finalBlocks, ctx);
		    }
		}
	    }
	    
	    try {
		return evalBlocks(backtrace, blocks, ctx);
	    } catch (Throwable t) {
		Throwable realT = t;
		if ((t instanceof ExecutionException) &&
		    t.getCause()!=null) {
		    // should always happen
		    realT = t.getCause();
		}
		for (int i = 0; i < catches.size(); i++) {
		    CatchEntry ce = catches.get(i);
		    if (ce.exception.isAssignableFrom(realT.getClass())) {
			ICtx cCtx = newCtx(ctx);
			cCtx.put(ce.varName, realT);
			return evalBlocks(backtrace, ce.blocks, cCtx);
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

    public  class WHILE extends AbstractForm {
        private List<ICompiled> blocks = null;
        private ICompiled condition = null;
        public void setRawParams(ASTNList params)
            throws InvalidParametersException {
            if (params.size() < 2) {
                throw new InvalidParametersException(debugInfo, "WHILE expects at least 2 parameters");
            }
            this.condition = compile(params.get(0));
            this.blocks = compileParams(params.subList(1, params.size()));
        }
    
        @Override
        public Object doEvaluate(Backtrace backtrace,ICtx  ctx) {
	    Object rc = null;
	    while (Utils.asBoolean(condition.evaluate(backtrace, ctx))) {
		rc = evalBlocks(backtrace, blocks, ctx);
	    }
	    return rc;
        }
    }

    @Arguments(spec={"(", "VAR", "SEQUENCE", ArgSpec.ARG_OPTIONAL,"RESULT",")" ,ArgSpec.ARG_REST,"body"})
    @Docstring(text="Loop over a sequence.\n" +
	       "Evaluate body with VAR bound to each element from SEQUENCE, in turn.\n" +
	       "Then evaluate RESULT to get return value, default NIL.")
    public  class FOREACH extends AbstractForm {
	private Symbol loopVar = null;
        private List<ICompiled> blocks = null;
        private ICompiled seqexpr = null;
	private ICompiled resultExpr = null;
        public void setRawParams(ASTNList params)
            throws InvalidParametersException {
            if (params.size() < 2) {
                throw new InvalidParametersException(debugInfo, "FOREACH expects at least 2 parameters: loop-args and one execution block at least");
            }
	    final ASTNList loopPars = ((ASTNList)params.get(0));
	    final ASTN loopVarASTN = loopPars.get(0);
	    if (loopVarASTN.isList()) {
		throw new InvalidParametersException(debugInfo, "FOREACH expects first loop argument cannot be list");
	    }
	    final Object loopVarObj = loopVarASTN.getObject();
	    if (!(loopVarObj instanceof Symbol)) {
		throw new InvalidParametersException(debugInfo, "FOREACH expects first loop argument must be loop variable");
	    }
	    this.loopVar = (Symbol) loopVarObj;
            this.seqexpr = compile((ASTN)loopPars.get(1));
	    this.resultExpr = (loopPars.size() > 2) ? compile((ASTN)loopPars.get(2)) : null;
	    this.blocks = compileParams(params.subList(1, params.size()));
        }

	@Override
        public Object doEvaluate(Backtrace backtrace,ICtx  ctx) {
	    final Object seq = seqexpr.evaluate(backtrace, ctx);
	    final ICtx loopCtx = ctx.getCompiler().newCtx(ctx);
	    final String loopVarName = loopVar.getName();
	    loopCtx.replace(loopVarName, null);
	    Seq.forEach(seq, new Seq.Operation() {
		    @Override
		    public void perform(Object item) {
			loopCtx.replace(loopVarName, item);
			evalBlocks(backtrace, blocks, loopCtx);
		    }
		}, false);
	    loopCtx.replace(loopVarName, null);
	    final Object result = null == resultExpr ? null :
		resultExpr.evaluate(backtrace, loopCtx);
	    return result;
        }
    }

    @Docstring(text="COND allows the execution of forms to be dependent on test-form.\n"+
	       "Test-forms are evaluated one at a time in the order in\n"+
	       "which they are given in the argument list until a\n"+
	       "test-form is found that evaluates to true.  If there\n"+
	       "are no forms in that clause, the value of the test-form\n"+
	       "is returned by the COND form.  Otherwise, the forms\n"+
	       "associated with this test-form are evaluated in order,\n"+
	       "left to right and the value returned by the last form\n"+
	       "is returned by the COND form.  Once one test-form has\n"+
	       "yielded true, no additional test-forms are\n"+
	       "evaluated. If no test-form yields true, nil is\n"+
	       "returned." ) 
    @Arguments(text="{test-form form*}*")
    public class COND extends AbstractForm {
	private List<List<ICompiled>> forms = null;
	private List<ICompiled> testForms = null;

	public void setRawParams(ASTNList params)
	    throws InvalidParametersException {
	    final int numParms = params.size();
	    this.forms = new ArrayList<List<ICompiled>>(numParms);
	    this.testForms = new ArrayList<ICompiled>(numParms);
	    for (final ASTN clause : params) {
		if (!clause.isList()) {
		    throw new InvalidParametersException(debugInfo, "COND expects clauses to be lists.");
		}
		ASTNList entryClauses = ((ASTNList)clause);
		if (null == entryClauses || entryClauses.isEmpty()) {
		    throw new InvalidParametersException(debugInfo, "COND expects non-empty clauses that contain at least test-form.");
		}
		final ICompiled testClause = compile(entryClauses.get(0));
		this.testForms.add(testClause);
		List <ICompiled> clauseForms = new ArrayList<ICompiled>(entryClauses.size()-1);
		for (int i = 1; i < entryClauses.size() ; i++) {
		    clauseForms.add(compile(entryClauses.get(i)));
		}
		this.forms.add(clauseForms);
	    }
	}
	public Object doEvaluate(Backtrace backtrace,ICtx  ctx) {
	    final int numClauses = testForms.size();
	    for (int i = 0; i < numClauses; i++) {
		final Object testObj = testForms.get(i).evaluate(backtrace, ctx);
		if (Utils.asBoolean(testObj)) {
		    final List<ICompiled> blocks = forms.get(i);
		    return blocks.isEmpty() ? testObj :  evalBlocks(backtrace, blocks, ctx);
		}
	    }
	    return null;
        }
    }
    
    public class IF extends AbstractForm {
        private List<ICompiled> elseBlocks = null;
        private ICompiled condition = null;
        private ICompiled thenBlock = null;

        public void setRawParams(ASTNList params)
            throws InvalidParametersException {
            if (params.size() < 2) {
                throw new InvalidParametersException(debugInfo, "IF expects at least 2 parameters");
            }
            this.condition = compile(params.get(0));
            this.thenBlock = compile(params.get(1));
            this.elseBlocks = compileParams(params.subList(2, params.size()));
        }
    
        public Object evalWithArgs(Backtrace backtrace, List eargs, ICtx ctx) {
            Object condVal = eargs.get(0);
            if (Utils.asBoolean(condVal)) {
                return thenBlock.evaluate(backtrace, ctx);
            } else {
                return evalBlocks(backtrace, elseBlocks, ctx);
            }
        }

        public List<Object> evaluateParameters(Backtrace backtrace, ICtx ctx) {
            List eargs = new ArrayList(1);
            eargs.add(condition.evaluate(backtrace, ctx));
            return eargs;
        }
        
        @Override
        public Object doEvaluate(Backtrace backtrace,ICtx  ctx) {
            return evalWithArgs(backtrace, evaluateParameters(backtrace, ctx),ctx);
        }
    }

    /**
     * (DEFUN FOO (arg1 arg2...) block block...)
     */
    public class DEFUN extends LAMBDA {
	// FIXME: this field is needed/ WHY?
        private String name;
        @Override
        public ICode doEvaluate(Backtrace backtrace,ICtx  ctx) {
            final ICode obj = super.doEvaluate(backtrace, ctx);
	    // FIXME!
            //obj.setName(name);
            //obj.setDebugInfo(this.debugInfo);
            functab.put(name, obj);
            return obj;
        }
    
        @Override
        public void setRawParams(ASTNList params) throws InvalidParametersException {
            if (params.size() < 2) {
                throw new InvalidParametersException(debugInfo,
                                                     "DEFUN expects at least 2 parameters, but got "+
                                                     params.size());
            }
            final ASTNLeaf first = (ASTNLeaf)params.get(0);
            final Object sym = first.getObject();
            if (!(sym instanceof Symbol)) {
                throw new InvalidParametersException(debugInfo,
                                                     "DEFUN expects Symbol as first parameter, but got "
                                                     +sym);
            }
            this.name = sym.toString();	    
            final ASTNList second = (ASTNList)params.get(1);
	    this.argSpec = new ArgSpec(second, Compiler.this);
            this.blocks = compileParams(params.subList(2, params.size()));
        }
    }


    
    public  class LAMBDA extends AbstractForm  {
	protected ArgSpec argSpec;
        protected List<ICompiled> blocks;

        public void setRawParams(ASTNList params)  throws InvalidParametersException {
            if (params.size() < 1) {
                throw new  InvalidParametersException(debugInfo,
                                                      "LAMBDA expects at least 1 parameter, but got "+
                                                      params.size());
            }
            final ASTN first = params.get(0); 
	    if (! first.isList()) {
		throw new  InvalidParametersException(debugInfo,
                                                      "LAMBDA expects first parameter to be a list");
	    }
	    this.argSpec = new ArgSpec((ASTNList)first, Compiler.this);
            this.blocks = compileParams(params.subList(1, params.size()));
        }

        @Override
        public ICode doEvaluate(Backtrace backtrace,ICtx  ctx) {
            return getICode();
        }

        //@Override
        public ICode getICode() {
            return new ICode() {
                @Override
                public Funcs.AbstractExpr getInstance() {
		    return new Funcs.AbstractExpr() {
			protected ArgList argList;
			@Override
			public Object doEvaluate(Backtrace backtrace,ICtx  ctx) {
			    Eargs localCtx = argList.evaluateArguments(backtrace, ctx);
			    return evalBlocks(backtrace, blocks, localCtx);
			}

			@Override
			public void setParams(List<ICompiled> params) throws
			    InvalidParametersException {
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
		    return (null!=blocks
			    && blocks.size() > 0
			    && (blocks.get(0) instanceof Funcs.StringExp))
			? (String)((Funcs.StringExp) blocks.get(0)).getValue()
			: "N/A";
		}

		@Override
		public String getArgDescr() {
		    return null == argSpec ? "args"
			: argSpec.asSpecList().toString();
		}

		@Override
		public String getCodeType() {
		    return "compiled function";
		}

		@Override
		public String getDefLocation() {
		    return ""+LAMBDA.this.getDebugInfo();
		}
		
            };
        }
    }

    public  class DLET extends AbstractForm {
        protected List<ICompiled> blocks = null;
        protected ICompiled listExpr = null;
        protected List<String> varNames = new ArrayList<String>();
    
        public void setRawParams(ASTNList params)
            throws InvalidParametersException {
            if (params.size() < 2) {
                throw new InvalidParametersException(debugInfo, getName() + " expects at least 2 parameters");
            }
            if (!(params.get(0).isList())) {
                throw new InvalidParametersException(debugInfo, getName() + " expects first parameter" + "to be list of var names");
            }
            // handle variable mappings
            //@SuppressWarnings("unchecked")
            ASTNList varDefs = (ASTNList)params.get(0);
	    
            for (int i = 0; i < varDefs.size(); i++) {
                ASTN varDef = varDefs.get(i);
                ICompiled varExp = compile(varDef);
                if (!(varExp instanceof VarExp)) {
                    throw new InvalidParametersException(debugInfo,
                                                         getName()+": Invalid #" + (i + 1) + "parameter, must be a variable name");
                }
                //locals = new Ctx();
                VarExp var = (VarExp) varExp;
                String varName = var.getName();
                varNames.add(varName);
                listExpr = compile(params.get(1));
            }
            // handle executable blocks
            this.blocks  = compileParams(params.subList(2, params.size()));
        }

        @Override
        public Object doEvaluate(Backtrace backtrace,ICtx  ctx) {
            ICtx localCtx = new Ctx(ctx);
            Object listVals = listExpr.evaluate(backtrace, localCtx);
            if (! (listVals instanceof List)) {
                throw new RuntimeException(getName() +
                                           " second parameter was expected to evaluate to list, but got "+
                                           listVals);
            }
            for(int i=0; i<varNames.size();i++) {
                String varName = varNames.get(i);
                localCtx.put(varName, ((List)listVals).get(i));
            }
            return evalBlocks(backtrace, blocks, localCtx);
        }
    

    }
    
    public class QUOTE extends AbstractForm {
        Object value = null;
        @Override
        public void setRawParams(ASTNList params) throws InvalidParametersException {
            if (params.size() != 1) {
                throw new InvalidParametersException(debugInfo, this.getName() +" expects exactly one parameter");
            }
            this.value = Utils.unASTN(params.get(0));
        }
        

        @Override
        protected Object doEvaluate(Backtrace backtrace, ICtx ctx) {
            return value;
        }
    }

    public class WITH_BINDINGS extends AbstractForm  {
        protected List<ICompiled> blocks = null;
        ICompiled bindExpr = null;

        public void setRawParams(ASTNList params)
            throws InvalidParametersException {
            if (params.size() < 1) {
                throw new InvalidParametersException(debugInfo,
                                                     getName() + " expects at least 2 parameters");
            }
            this.bindExpr = compile(params.get(0));
            this.blocks  = compileParams(params.subList(1, params.size()));
        }

        @Override
        public Object doEvaluate(Backtrace backtrace, ICtx ctx) {
            final Object bindObj = bindExpr.evaluate(backtrace, ctx);
            if (! (bindObj instanceof Map)) {
                throw new ExecutionException(backtrace, getName() +
                                             " expects a java.lang.Map instance as first parameter, but got " +
                                             bindObj);
            }
            final ICtx localCtx = new Ctx(ctx, (Map)bindObj);
            return evalBlocks(backtrace, blocks, localCtx);
        }
    }

    public class WITH_CTX extends AbstractForm  {
        protected List<ICompiled> blocks = null;
        ICompiled bindExpr = null;

        public void setRawParams(ASTNList params)
            throws InvalidParametersException {
            if (params.size() < 1) {
                throw new InvalidParametersException(debugInfo,
                                                     getName() + " expects at least 2 parameters");
            }
            this.bindExpr = compile(params.get(0));
            this.blocks  = compileParams(params.subList(1, params.size()));
        }

        @Override
        public Object doEvaluate(Backtrace backtrace, ICtx ctx) {
            final Object bindObj = bindExpr.evaluate(backtrace, ctx);
            if (! (bindObj instanceof ICtx)) {
                throw new ExecutionException(backtrace, getName() +
                                             " expects a ICtx instance as first parameter, but got " +
                                             bindObj);
            }
            final ICtx localCtx = (ICtx)bindObj;
            return evalBlocks(backtrace, blocks, localCtx);
        }
    }

    public  abstract class ABSTRACT_SET_OP extends AbstractForm {
	VarExp[]  variables;
	ICompiled[] values;
	public void setRawParams(ASTNList params)
	    	throws InvalidParametersException {
	    final int numParams = params.size();
	    if ((numParams & 0x1) == 1) {
                throw new InvalidParametersException(debugInfo,
						     "expected even number of parameters, but got "
						     + numParams);
            }
	    final int numPairs = numParams >> 1;
	    this.variables = new VarExp[numPairs];
	    this.values = new ICompiled[numPairs];
	    for (int i = 0; i < numParams; i+=2) {
		final ASTN varDef = params.get(i);
		final ICompiled varExp = compile(varDef);
		if (!(varExp instanceof VarExp)) {
		    throw new InvalidParametersException(debugInfo,
							 "SETV: Invalid parameter["+ i +
							 "], must be a variable name");
		}
		final int pairPos = i >> 1;
		this.variables[pairPos] = (VarExp) varExp;
		this.values[pairPos] = compile(params.get(i+1));
	    }
	}

        @Override
        public Object doEvaluate(Backtrace backtrace,ICtx  ctx) {
	    final int numVars = this.variables.length;
	    Object val = null;
	    for (int i = 0; i < numVars; i++) {
		val = values[i].evaluate(backtrace, ctx);
		final String name = variables[i].getName();
		setvar(name, val, ctx);
	    }
            return val;
        }
	protected abstract void  setvar(String name, Object val, ICtx ctx);
    }

    public  class SETV extends ABSTRACT_SET_OP {
        @Override
	protected  void  setvar(String name, Object val, ICtx ctx) {
	    ctx.replace(name, val);
        }
    }


    @Arguments(text="{var form}*")
    @Docstring(text="Assigns values to variables. If variable is already assigned it's value will be replaced by result of form evaluation. If not new global binding will be created in the global scope\n"+
	       "var - a symbol naming a variable.\n"+
	       "form - an expression, which evaluation result will be assigned to var\n"+
	       "Returns: value of the last form, or nil if no pairs were supplied.") 
    public  class SETQ extends ABSTRACT_SET_OP {
        @Override
	protected  void  setvar(String name, Object val, ICtx ctx) {
	    ctx.greplace(name, val);
        }
    }

    public  class LET extends AbstractForm  {
        protected List<ICompiled> blocks = null;
        protected List<ICompiled> varExprs = new ArrayList<ICompiled>();
        protected List<String> varNames = new ArrayList<String>();
        
        public void setRawParams(ASTNList params)
            throws InvalidParametersException {
            if (params.size() < 2) {
                throw new InvalidParametersException(debugInfo, "LET expects at least 2 parameters");
            }
            if (!(params.get(0).isList())) {
                throw new InvalidParametersException(debugInfo, "LET expects first parameter" + "to be list of (var value) pairs");
            }
            // handle variable mappings
            //@SuppressWarnings("unchecked")
            ASTNList varDefs = (ASTNList)params.get(0);
            for (int i = 0; i < varDefs.size(); i++) {
                if (!(varDefs.get(i).isList())) {
                    throw new InvalidParametersException(debugInfo, "LET: Invalid " + (i + 1) + " parameter, expected list");
                }
                //@SuppressWarnings("unchecked")
                ASTNList pair = ((ASTNList)varDefs.get(i));
                if (pair.size()<1) {
                    throw new InvalidParametersException(debugInfo,
                                                         "LET: Invalid " + (i + 1) + " parameter, expected at least one value in list");
                }
                ICompiled varExp = compile(pair.get(0));
                if (!(varExp instanceof VarExp)) {
                    throw new InvalidParametersException(debugInfo,
                                                         "LET: Invalid " + (i + 1) + " parameter, first value must be a variable name");
                }
                //locals = new Ctx();
                VarExp var = (VarExp) varExp;
                ICompiled expr;
		if (pair.size()>1) {
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
            this.blocks  = compileParams(params.subList(1, params.size()));
        }

        @Override
        public Object doEvaluate(Backtrace backtrace,ICtx  ctx) {
            ICtx localCtx = new Ctx(ctx);
            for(int i=0; i<varNames.size();i++) {
                String varName = varNames.get(i);
                ICompiled varExpr = varExprs.get(i);
                Object val = varExpr.evaluate(backtrace, localCtx);
                localCtx.put(varName, val);
            }
            return evalBlocks(backtrace, blocks, localCtx);
        }        
    }

    /**** Context *****/
    public interface ICtx {
	public ICtx getPrev();
	public ICtx getLevel0();

        public void onMissingVar(String varname);
	//public Object get(String name);
	public Object get(String name, Backtrace bt);
	public Map <Object,Object> getProps(String name, Backtrace bt);
	public Object getProp(String name, Object prop, Backtrace bt);

	public boolean contains(String name);
	// put value in the outer context
	public void put(String name, Object expr);
	// replace value if found at some depth, otherwise
	// perform put()
	public void  putProp(String name, Object pkey, Object pval);
	public void  putProps(String name, Object ... pobjs);
	public void  putProps(String name, Map<Object,Object> props);
	public void replace(String name, Object expr);
	// replace value if found at some depth, otherwise
	// place it in global scope
	public void greplace(String name, Object expr);

	public Compiler getCompiler();

	public String  toStringSelf();
	public String toStringShort();
	
	public Map<String, Object> getMappings();
	public Map<String, Map<Object, Object>> getPropsMap();

	public Map<String, Object> findMatches(String pattern);
	public void addMatches(Map<String, Object> matches,
			       String pattern);
	public void remove(String name);
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
            throw new RuntimeException("variable '"+key+"' does not exist in this context");
        }
    }

    public class Ctx implements ICtx {
	final Map<String, Object> mappings = new HashMap<String, Object>();
	final Map<String, Map<Object,Object>> propsMap =
	    new HashMap<String, Map<Object,Object>>();
	ICtx prev;
        IMissHandler missHandler = new NilMissHandler();

	public Map<String, Map<Object, Object>> getPropsMap() {
	    return propsMap;
	}

	@Override
	public Map<Object, Object> getProps(String name, Backtrace bt) {
	    ICtx ctx = this.findCtxFor(name);
	    return  (null == ctx) ?  null:
		ctx.getPropsMap().get(name);
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

	public void putProp(String name, Object pkey, Object pval) {
	    getPropsMapForPut(name).put(pkey, pval);
	}

	public void putProps(String name, Object... pobjs) {
	    Map<Object, Object> pmap = getPropsMapForPut(name);
	    for (int i = 0; i < pobjs.length; i+=2) {
		pmap.put(pobjs[i], pobjs[i+1]);
	    }
	}

	public void putProps(String name, Map<Object, Object> newProps) {
	    getPropsMapForPut(name).putAll(newProps);
	}

	protected Map<Object, Object> getPropsMapForPut(String name) {
	    ICtx ctx = this.findCtxFor(name);
	    if (null == ctx) {
		throw new RuntimeException("Failed to put properties: there is no variable mapping for "+name);
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
	    super();
	    this.prev = null;
	    if (null != vars) {
		mappings.putAll(vars);
	    }
	}

	public Ctx(ICtx prev, Map<String, Object> vars) {
	    super();
	    this.prev = prev;
	    if (null != vars) {
		mappings.putAll(vars);
	    }
	}

	public Ctx() {
	    super();
	    setMissHandler(Compiler.this.failOnMissingVariables ?
                           new ErrorMissHandler() : new NilMissHandler());
	}

	public Ctx(ICtx prev) {
	    super();
	    this.prev = prev;
	}

	public Ctx(ICtx locals, ICtx ctx) {
	    super();
	    mappings.putAll(locals.getMappings());
	    prev = ctx;
	}

        public Object get(String name, Backtrace bt) {
            Object val;
            if (null == (val = mappings.get(name))) {
                return mappings.containsKey(name) ? null :
		    (null == prev ? null : prev.get(name, bt));
            } else {
                return val;
            }
        }

	protected ICtx findCtxFor(String name) {
	    ICtx ctx = this;
	    while (null!=ctx &&
		   (! ctx.getMappings().containsKey(name))) {
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
        
	protected Map getMappingsOrLocal(String name) {
            return findCtxOrLocal(name).getMappings();
        }

	protected Map getMappingsOrGlobal(String name) {
            return findCtxOrGlobal(name).getMappings();
        }

	
        public boolean contains(String name) {
            if (mappings.containsKey(name)) {
		return true;
	    } else if (null == prev) {
		return false;
	    } else {
                return prev.contains(name);
            }
        }

        public void put(String name, Object expr) {
            Object prev = mappings.put(name, expr);
	    // FIXME: bug - if var was spreviously set to null
            if (null!=prev) {
                throw new RuntimeException("trying to overwrite variable "
                                           +name+" in context " +this);
            }
        }

	public Compiler getCompiler() {
	    return Compiler.this;
	}
	
        public String  toString() {
            StringBuffer buf = new StringBuffer();
            buf.append("Ctx<").append(this.hashCode()).append("[");
	    for (String var : mappings.keySet()) {
		buf.append("\n").append(var).append(" -> ").append(mappings.get(var));
		if (propsMap.containsKey(var)) {
			buf.append(" (").append(propsMap.get(var));
		}
	    }
	    buf.append(mappings).append("] \n");
            if (null!=prev) {
                buf.append("\n -->").append(prev.toString());
            }
            return buf.toString();
        }

        public String  toStringSelf() {
            StringBuffer buf = new StringBuffer();
            buf.append("Ctx<").append(this.hashCode()).append(">");
            return buf.toString();
        }
        
        public String  toStringShort() {
            StringBuffer buf = new StringBuffer();
            buf.append(this.toStringSelf()).append("=").append(mappings);
            if (null!=prev) {
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
	    return null ==  prev ? this :  prev.getLevel0();
	}

	@Override
	public Map<String, Object> getMappings() {
	    return this.mappings;
	}

	@Override
	public void replace(String name, Object value) {
	    this.getMappingsOrLocal(name).put(name,  value);
	}

	@Override
	public void greplace(String name, Object value) {
	    this.getMappingsOrGlobal(name).put(name,  value);
	}

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
	public void addMatches(Map<String, Object> matches,
				  String pattern) {
	    Set<Entry<String, Object>> eset = this.getMappings().entrySet();
	    for (Entry<String, Object> e : eset) {
		final String key = e.getKey();
		if (e.getKey().startsWith(pattern)) {
		    if (! eset.contains(key)) {
			matches.put(e.getKey(), e.getValue());
		    }
		}
	    }
	    if (null != this.getPrev()) {
		getPrev().addMatches(matches, pattern);
	    }

	}
	@Override
	public void remove(String name) {
	    final Map mappings = getMappings();
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

    public ICtx newCtx(Map vars) {
        return new Ctx(vars);
    }

    public Backtrace newBacktrace() {
        return new Backtrace();
    }

    /***** Utility functions ****/
    public  Object evalBlocks(Backtrace backtrace, List<ICompiled> blocks, ICtx ctx) {
        Object result = null;
        for (ICompiled block : blocks) {
            result = block.evaluate(backtrace, ctx);
        }
        return result;
    }

    public  List<ICompiled> compileExpList(ASTNList nodes) {
	List<ICompiled> exprs= new ArrayList<ICompiled>(nodes.size());
	for(ASTN node : nodes) {
	    exprs.add(compile(node));
	}
	return exprs;
    }
    
    public  List<ICompiled> compileParams(ASTNList params) {
        List<ICompiled> compiledParams = new ArrayList<ICompiled>(params.size());
        for (ASTN param: params) {
            compiledParams.add(compile(param));
        }
        return compiledParams;
    }

    public class Eargs extends Ctx {
	private Object[] eargs;
	private ArgList  argList;
	
	// FIXME: not elegant!
	public Eargs(Object [] eargs, boolean needEval[], ArgList argList, ICtx ctx) {
	    super(ctx);
	    if ((null == argList) ||
		(null == eargs)  ||
		//(null == needEval) ||
		(null == ctx)) {
		throw new RuntimeException("Internal error: null constructor parameter when creating Earg");
	    }
	    this.eargs = eargs;
	    //this.needEval = needEval;
	    this.argList = argList;
	}

	public Object get(int i, Backtrace backtrace) {
	    // if (null !=needEval && needEval[i]) {
	    // 	final ICompiled expr = (ICompiled) eargs[i];
	    // 	eargs[i] = expr.evaluate(backtrace, this.getPrev());
	    // 	needEval[i] = false;
	    // }
	    final Object val =  eargs[i];
	    if (val instanceof LazyEval) {
		return ((LazyEval)val).getValue(backtrace);
	    } else {
		return val;
	    }
	}

	@Override
	public void replace(final String name, final Object val) {
	    if (this.isParameterVar(name)) {
		this.getMappings().put(name, val);
	    } else {
		super.replace(name, val);
	    }
	}

	protected boolean isParameterVar(String name) {
	    final ArgSpec spec = argList.getSpec();
	    return (spec.nameToIdx(name) >= 0) ||
		(spec.svarNameToIdx(name) >=0);
	}

	@Override
	public boolean contains(String name) {
	    final ArgSpec spec = argList.getSpec();
	    return (spec.nameToIdx(name) >= 0) ||
		(spec.svarNameToIdx(name) >=0) ||
		super.contains(name);
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
	    //return super.get(name, bt);
	}
    
	public int size() {
	    return eargs.length;
	}


    }

    public Eargs newEargs(Object[] result, boolean[] needEval, ArgList argList, ICtx ctx) {
	return new Eargs(result, needEval, argList, ctx);
    }
    
}
 
