package io.opsit.explang.jsr223;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;
import java.util.Map;
import java.util.Arrays;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.Invocable;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import io.opsit.explang.Utils;
import io.opsit.explang.reader.LispReader;
import io.opsit.explang.Compiler;
import io.opsit.explang.Funcs;
import io.opsit.explang.ICode;
import io.opsit.explang.IParser;
import io.opsit.explang.InvalidParametersException;
import io.opsit.explang.Backtrace;
import io.opsit.explang.ParseCtx;
import io.opsit.explang.ASTN;
import io.opsit.explang.ASTNList;
import io.opsit.explang.ICompiled;
import io.opsit.explang.IExpr;

public class ExplangScriptEngine
    implements ScriptEngine, Invocable  {
    final protected ScriptEngineFactory factory;
    protected ScriptContext defScriptContext;
    protected Compiler comp;

    protected ExplangScriptEngine(Bindings n) {
	this();
	if (n == null) {
            throw new NullPointerException("n is null");
        }
        defScriptContext.setBindings(n, ScriptContext.ENGINE_SCOPE);
    }

    
    protected ExplangScriptEngine() {
	this(new ExplangScriptEngineFactory());
    }
    
    protected ExplangScriptEngine(ScriptEngineFactory f) {
	this.factory = f;
	defScriptContext = new ExplangScriptContext();
	this.comp = new Compiler();
	comp.setParser(new LispReader());
    }

    protected Compiler.ICtx makeCtx(ScriptContext sc) {
	return comp.newCtx();
    }
    
    @Override
    public Object eval(String script, ScriptContext context) throws ScriptException {
	InputStream is = new ByteArrayInputStream(script.getBytes());
	Reader r = new InputStreamReader(is);
	return eval(r, context);
    }

    @Override
    public Object eval(Reader reader, ScriptContext context) throws ScriptException {
	IParser parser = comp.getParser();
	Backtrace bt = comp.newBacktrace();
	ParseCtx pctx = new ParseCtx("<JSR223>");
	Object result = null;
	Compiler.ICtx ctx = makeCtx(context);
	ASTNList astns = null;
	do {
	    astns = parser.parse(pctx, reader,1);
	    if (astns.isEmpty()) {
		break;
	    }
	    for (ASTN astn : astns) {
		ICompiled expr = comp.compile(astn);
		result = expr.evaluate(bt, ctx);
	    }
	} while (true);
	return result;
    }

    @Override
    public Object eval(String script) throws ScriptException {
	return this.eval(script, this.defScriptContext);
    }

    @Override
    public Object eval(Reader reader) throws ScriptException {
	return this.eval(reader, this.defScriptContext);
    }

    @Override
    public Object eval(String script, Bindings n) throws ScriptException {
	// TODO Auto-generated method stub
	return null;
    }


        /**
     * <code>eval(Reader, Bindings)</code> calls the abstract
     * <code>eval(Reader, ScriptContext)</code> method, passing it a <code>ScriptContext</code>
     * whose Reader, Writers and Bindings for scopes other that <code>ENGINE_SCOPE</code>
     * are identical to those members of the protected <code>context</code> field.  The specified
     * <code>Bindings</code> is used instead of the <code>ENGINE_SCOPE</code>
     *
     * <code>Bindings</code> of the <code>context</code> field.
     *
     * @param reader A <code>Reader</code> containing the source of the script.
     * @param bindings A <code>Bindings</code> to use for the <code>ENGINE_SCOPE</code>
     * while the script executes.
     *
     * @return The return value from <code>eval(Reader, ScriptContext)</code>
     * @throws ScriptException if an error occurs in script.
     * @throws NullPointerException if any of the parameters is null.
     */
    public Object eval(Reader reader, Bindings bindings ) throws ScriptException {

        ScriptContext ctxt = getScriptContext(bindings);

        return eval(reader, ctxt);
    }


    @Override
    public void put(String key, Object value) {
	Bindings n = getBindings(ScriptContext.ENGINE_SCOPE);
        if (n != null) {
            n.put(key, value);
        }
    }

    /**
     * Gets the value for the specified key in the <code>ENGINE_SCOPE</code> of the
     * protected <code>context</code> field.
     *
     * @return The value for the specified key.
     *
     * @throws NullPointerException if key is null.
     * @throws IllegalArgumentException if key is empty.
     */
    public Object get(String key) {

        Bindings nn = getBindings(ScriptContext.ENGINE_SCOPE);
        if (nn != null) {
            return nn.get(key);
        }

        return null;
    }


    /**
     * Returns the <code>Bindings</code> with the specified scope value in
     * the protected <code>context</code> field.
     *
     * @param scope The specified scope
     *
     * @return The corresponding <code>Bindings</code>.
     *
     * @throws IllegalArgumentException if the value of scope is
     * invalid for the type the protected <code>context</code> field.
     */
    public Bindings getBindings(int scope) {

        if (scope == ScriptContext.GLOBAL_SCOPE) {
            return defScriptContext.getBindings(ScriptContext.GLOBAL_SCOPE);
        } else if (scope == ScriptContext.ENGINE_SCOPE) {
            return defScriptContext.getBindings(ScriptContext.ENGINE_SCOPE);
        } else {
            throw new IllegalArgumentException("Invalid scope value.");
        }
    }

    
    @Override
    public void setBindings(Bindings bindings, int scope) {
        if (scope == ScriptContext.GLOBAL_SCOPE) {
            defScriptContext.setBindings(bindings, ScriptContext.GLOBAL_SCOPE);;
        } else if (scope == ScriptContext.ENGINE_SCOPE) {
            defScriptContext.setBindings(bindings, ScriptContext.ENGINE_SCOPE);;
        } else {
            throw new IllegalArgumentException("Invalid scope value.");
        }
    }

    @Override
    public Bindings createBindings() {
	return new SimpleBindings();
    }

    @Override
    public ScriptContext getContext() {
	return defScriptContext;
    }

    /**
     * Sets the value of the protected <code>context</code> field to the specified
     * <code>ScriptContext</code>.
     *
     * @param ctxt The specified <code>ScriptContext</code>.
     * @throws NullPointerException if ctxt is null.
     */
    public void setContext(ScriptContext ctxt) {
        if (ctxt == null) {
            throw new NullPointerException("null context");
        }
        defScriptContext = ctxt;
    }

    
    @Override
    public ScriptEngineFactory getFactory() {
	return factory;
    }


        /**
     * Returns a <code>SimpleScriptContext</code>.  The <code>SimpleScriptContext</code>:
     *<br><br>
     * <ul>
     * <li>Uses the specified <code>Bindings</code> for its <code>ENGINE_SCOPE</code>
     * </li>
     * <li>Uses the <code>Bindings</code> returned by the abstract <code>getGlobalScope</code>
     * method as its <code>GLOBAL_SCOPE</code>
     * </li>
     * <li>Uses the Reader and Writer in the default <code>ScriptContext</code> of this
     * <code>ScriptEngine</code>
     * </li>
     * </ul>
     * <br><br>
     * A <code>SimpleScriptContext</code> returned by this method is used to implement eval methods
     * using the abstract <code>eval(Reader,Bindings)</code> and <code>eval(String,Bindings)</code>
     * versions.
     *
     * @param nn Bindings to use for the <code>ENGINE_SCOPE</code>
     * @return The <code>SimpleScriptContext</code>
     */
    protected ScriptContext getScriptContext(Bindings nn) {
        ExplangScriptContext ctxt = new ExplangScriptContext();
        Bindings gs = getBindings(ScriptContext.GLOBAL_SCOPE);

        if (gs != null) {
            ctxt.setBindings(gs, ScriptContext.GLOBAL_SCOPE);
        }

        if (nn != null) {
            ctxt.setBindings(nn,
                    ScriptContext.ENGINE_SCOPE);
        } else {
            throw new NullPointerException("Engine scope Bindings may not be null.");
        }

        ctxt.setReader(defScriptContext.getReader());
        ctxt.setWriter(defScriptContext.getWriter());
        ctxt.setErrorWriter(defScriptContext.getErrorWriter());

        return ctxt;

	}

	@Override
	public Object invokeMethod(Object thiz, String name, Object... args) throws ScriptException, NoSuchMethodException {
	    throw new ScriptException("invokeMethod not implemented");
	}

	@Override
	public Object invokeFunction(String name, Object... args) throws ScriptException, NoSuchMethodException {
	    // FIXME: move to utils?
	    ICode code = comp.getFun(name);
	    ICompiled compiled = code.getInstance();
	    if (!(compiled  instanceof IExpr)) {
		throw new ScriptException("Not a function: "+compiled.getClass());
	    }
	    try {
		IExpr func = (IExpr) compiled;
		List paramsList = Arrays.asList(args);
		ParseCtx pctx = new ParseCtx("InvokeFunction");
		ASTNList astnizedParams = (ASTNList)Utils.ASTNize(paramsList, pctx);
		List<ICompiled> compiledParams = comp.compileParams(astnizedParams);
		func.setParams(compiledParams);
		Backtrace bt = comp.newBacktrace();
		Compiler.ICtx ctx = makeCtx(this.defScriptContext);
		Object result = func.evaluate(bt, ctx);
		return result;
	    } catch (InvalidParametersException ex) {
		throw new ScriptException(ex);
	    }
	}

	@Override
	public <T> T getInterface(Class<T> clasz) {
	    return null;
	}

	@Override
	public <T> T getInterface(Object thiz, Class<T> clasz) {
	    return null;
	}
}
