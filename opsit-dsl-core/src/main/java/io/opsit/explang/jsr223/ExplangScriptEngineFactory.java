package io.opsit.explang.jsr223;

import java.util.List;
import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.SimpleBindings;
import javax.script.Bindings;
import javax.script.ScriptContext;

import io.opsit.explang.Utils;

public class ExplangScriptEngineFactory implements ScriptEngineFactory {
    // FIXME: define threading parameters
    final protected Map <String,String> engineParameters =
        Utils.map(ScriptEngine.ENGINE,           "ExplangSE",
		  ScriptEngine.ENGINE_VERSION,   "0.0.1",
		  ScriptEngine.NAME,             "ExplangSE",
		  ScriptEngine.LANGUAGE,         "explang",
		  ScriptEngine.LANGUAGE_VERSION, "0.0.1");

    protected Bindings globalBindings = new SimpleBindings();
    
    @Override
    public String getEngineName() {
	return engineParameters.get(ScriptEngine.ENGINE);
    }

    @Override
    public String getEngineVersion() {
	return engineParameters.get(ScriptEngine.ENGINE_VERSION);
    }

    @Override
    public List<String> getExtensions() {
	return Utils.clist("l","lsp","lisp","exl");
    }

    @Override
    public List<String> getMimeTypes() {
	return Utils.clist("text/x-script.lisp");
    }

    @Override
    public List<String> getNames() {
	return Utils.clist(engineParameters.get(ScriptEngine.NAME));
    }

    @Override
    public String getLanguageName() {
	return engineParameters.get(ScriptEngine.LANGUAGE);
    }

    @Override
    public String getLanguageVersion() {
	return engineParameters.get(ScriptEngine.LANGUAGE_VERSION);
    }

    @Override
    public Object getParameter(String key) {
	return engineParameters.get(key);
    }

    @Override
    public String getMethodCallSyntax(String obj, String m, String... args) {
	final StringBuilder buf = new StringBuilder();
	buf.append("(.").append(obj).append(" \"").append(m).append("\" ");
	buf.append("(LIST ");
	for (int i =0; i < args.length; i++)  {
	    if (i > 0) {
		buf.append(" ");
	    }
	    buf.append(args[i]);
	}
	buf.append("))");
	return buf.toString();
    }

    @Override
    public String getOutputStatement(String toDisplay) {
	final StringBuilder buf = new StringBuilder();
	buf.append("(PRINT ").append(toDisplay).append(" \"\n\")");
	return buf.toString();
    }

    @Override
    public String getProgram(String... statements) {
	// FIXME: PROGN?
	return String.join("\n", statements);
    }

    @Override
    public ScriptEngine getScriptEngine() {
	final ScriptEngine engine =  new ExplangScriptEngine(this);
	engine.setBindings(this.globalBindings,
			   ScriptContext.GLOBAL_SCOPE);
	return engine;
    }
    
}
