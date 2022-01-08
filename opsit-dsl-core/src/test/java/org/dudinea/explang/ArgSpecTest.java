package org.dudinea.explang;

import static org.dudinea.explang.ArgSpec.AF.KEY;
import static org.dudinea.explang.ArgSpec.AF.MANDATORY;
import static org.dudinea.explang.ArgSpec.AF.MANDATORY2;
import static org.dudinea.explang.ArgSpec.AF.OPTIONAL;
import static org.dudinea.explang.ArgSpec.AF.REST;
import static org.dudinea.explang.ArgSpec.AF.REST_KEY;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.dudinea.explang.ArgSpec.AF;
import org.dudinea.explang.ArgSpec.Arg;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class ArgSpecTest {
    Compiler comp = new Compiler();
    protected List<Object> specs;
    protected Throwable exception;
    protected List<Arg> args;
    protected static int testNo = 0;
    
    public ArgSpecTest(List<Object>specs,
                       List<Arg> args,
                       Throwable exception) {
        this.specs = specs;
        this.args = args;
        this.exception = exception;
    }

    protected static List list (Object ... args) {
        return Utils.list(args);
    }

    protected static Arg arg(String name,
                             AF flag) {
        return new Arg(name,flag, null, null, false, false, false);
    }

    protected static Arg arg(String name,
                             AF flag,
                             ICompiled initForm,
                             String svar,
                             boolean allowOtherKeys) {
        return new Arg(name,flag,initForm, svar, allowOtherKeys, false, false);
    }

    protected static Arg arg(String name,
                             AF flag,
                             ICompiled initForm,
                             String svar,
                             boolean allowOtherKeys,
                             boolean isLazy) {
        return new Arg(name,flag,initForm, svar, allowOtherKeys, isLazy, false);
    }

    protected static Arg arg(String name,
                             AF flag,
                             ICompiled initForm,
                             String svar,
                             boolean allowOtherKeys,
                             boolean isLazy,
                             boolean isPipe) {
        return new Arg(name,flag,initForm, svar, allowOtherKeys, isLazy, isPipe);
    }
    
    @Parameters
    public static Collection<Object[]> data() throws Exception {
        List configs =
            Arrays.asList(new Object[][] {
                    { list(),
                      list(),
                      null} ,

                    { list("a"),
                      list(arg("a",MANDATORY)),
                                null},

                    { list("a", "b","c"),
                      list(arg("a", MANDATORY,null,null,false, false,false),
                           arg("b", MANDATORY,null,null,false, false,false),
                           arg("c", MANDATORY,null,null,false, false,false)),
                      null
                    },
                    
                    { list("a", "&PIPE","b","c"),
                      list(arg("a", MANDATORY,null,null,false, false,false),
                           arg("b", MANDATORY,null,null,false, false,true),
                           arg("c", MANDATORY,null,null,false, false,false)),
                      null
                    },

                    { list("&PIPE", "a", "b","c"),
                      list(arg("a", MANDATORY,null,null,false, false,true),
                           arg("b", MANDATORY,null,null,false, false,false),
                           arg("c", MANDATORY,null,null,false, false,false)),
                      null
                    },

                    { list("&PIPE", "a", "b", "&PIPE", "c"),
                      null,
                      new InvalidParametersException("Invalid parameter spec: only one " +
                                                     ArgSpec.ARG_PIPE+ " can be specified")
                    },
                    
                    { list("&LAZY", "a"),
                      list(arg("a",MANDATORY,null,null,false,true)),
                      null},

                    { list("&REQUIRED","a"),
                      list(arg("a",MANDATORY)),
                      null},

                    { list("&REQUIRED","a","&OPTIONAL","b"),
                      list(arg("a",MANDATORY),
                           arg("b", OPTIONAL)),
                      null},
		
                    { list("&REQUIRED","a","&OPTIONAL","b","c"),
                      list(arg("a", MANDATORY),
                           arg("b", OPTIONAL),
                           arg("c", OPTIONAL)),
                      null},

                    { list("&REQUIRED","a",
                           "&OPTIONAL","b",
                           "&REQUIRED","c"),
                      list(arg("a",MANDATORY),
                           arg("b",OPTIONAL),
                           arg("c",MANDATORY2)),
                      null
                    },
		    
                    { list("&REQUIRED","a",
                           "&OPTIONAL","b",
                           "&REST","c"),
                      list(arg("a",MANDATORY),
                           arg("b",OPTIONAL),
                           arg("c",REST)),
                      null
                    },
		    
                    { list("&REQUIRED","&REQUIRED","a",
                           "&OPTIONAL","&OPTIONAL","b",
                           "&REST","&REST","c"),
                      list(arg("a",MANDATORY),
                           arg("b",OPTIONAL),
                           arg("c",REST)),
                      null
                    },
                    { list("&OPTIONAL","a",
                           "&REQUIRED","b"),
                      list(arg("a",OPTIONAL),
                           arg("b",MANDATORY2)),
                      null
                    },
                    { list("&OPTIONAL","a",
                           "&REQUIRED","b",
                           "&OPTIONAL","c"),
                      null,
                      new InvalidParametersException("misplaced argument keyword: " +
                                                     ArgSpec.ARG_OPTIONAL)
                    },
		    
                    { list("&REST","a","&REQUIRED", "b"),
                      list(arg("a",REST),arg("b", MANDATORY2)),
                      null,
                    },

                    { list("a","&OPTIONAL","b","&REST","c","&REQUIRED", "d"),
                      list(arg("a",MANDATORY),
                           arg("b",OPTIONAL),
                           arg("c",REST),
                           arg("d",MANDATORY2)),
                      null
                    },
                    { list("&KEY","a","b"),
                      list(arg("a",KEY),
                           arg("b",KEY)),
                      null
                    },

                    { list("&LAZY","&KEY","a","b"),
                      list(arg("a",KEY,null,null, false,true),
                           arg("b",KEY,null,null, false,true)),
                      null
                    },

                    { list("a", "&KEY","b","c"),
                      list(arg("a", MANDATORY),
                           arg("b", KEY),
                           arg("c", KEY)),
                      null
                    },
		    
                    { list("a", "&OPTIONAL","b","&KEY", "c"),
                      list(arg("a", MANDATORY),
                           arg("b", OPTIONAL),
                           arg("c", KEY)),
                      null
                    },

                    { list("a", "&REST","b","&KEY", "c"),
                      list(arg("a",MANDATORY),
                           arg("b",REST),
                           arg("c",REST_KEY)),
                      null
                    },

                    { list("a", "&REST","b","&KEY", "c","&KEY", "d"),
                      list(arg("a", MANDATORY),
                           arg("b", REST),
                           arg("c", REST_KEY),
                           arg("d", REST_KEY)),
                      null
                    },

                    { list("a", "&REST","b","&KEY", "c","&KEY", "d","&REQUIRED","e"),
                      list(arg("a", MANDATORY),
                           arg("b", REST),
                           arg("c", REST_KEY),
                           arg("d", REST_KEY),
                           arg("e", MANDATORY2)),
                      null
                    },

                    { list("a", "&REST","b","&KEY", "c","d","&ALLOW-OTHER-KEYS","&REQUIRED","e"),
                      list(arg("a", MANDATORY),
                           arg("b", REST),
                           arg("c", REST_KEY,null,null,true),
                           arg("d", REST_KEY,null,null,true),
                           arg("e", MANDATORY2)),
                      null
                    },
		    
                    { list("&OPTIONAL", list("a",new Funcs.NumberExp(33))),
                      list(arg("a", OPTIONAL, new Funcs.NumberExp(33), null, false)),
                      null
                    },
                    { list("&OPTIONAL", list("a",new Funcs.NumberExp(33), "sa")),
                      list(arg("a", OPTIONAL, new Funcs.NumberExp(33), "sa", false)),
                      null
                    },
                    { list("&KEY", list("a",new Funcs.NumberExp(33), "sa")),
                      list(arg("a", KEY, new Funcs.NumberExp(33), "sa", false)),
                      null
                    },
                    { list("&KEY", list("a",new Funcs.NumberExp(33), "sa"), "&ALLOW-OTHER-KEYS"),
                      list(arg("a", KEY, new Funcs.NumberExp(33), "sa", true)),
                      null
                    },
                });
        //return configs.subList(1,2);
        //return configs.subList(18,19 );
        return configs;
    }

    @Test
    public void testArgs() throws Throwable {
        System.err.println("\nTEST " + (testNo++));
        System.err.println("INPUT: " + specs);
        try {
            IParser parser = comp.getParser();
            ParseCtx pctx = new ParseCtx(this.getClass().getSimpleName());
            ASTNList astnSpec = (ASTNList)astnize(specs, parser, pctx);
	    
            ArgSpec spec = new ArgSpec(astnSpec, comp);
            List<Arg> specArgs = Arrays.asList(spec.getArgs());
	    
            System.err.println("OUT_ARGS: " + specArgs);
            System.err.println("EXP_ARGS: " + this.args);
            Assert.assertEquals(this.args, specArgs);
	    

            // System.err.println("EXP_INITFORMS: " + Arrays.toString(this.initForms));
            // Assert.assertEquals(this.flags, spec.paramFlags);
            // Assert.assertEquals(this.names, spec.paramNames);
            // if (null != spec.initForms) {
            // 	// FIXME not sure if I need make equal
            // 	// of self evaluating object return true
            // 	// values match
            // 	Assert.assertNotNull(this.initForms);
            // 	Assert.assertEquals(this.initForms.length, spec.initForms.length);
            // 	for (int i = 0; i < this.initForms.length; i++) {
            // 	    if (null == this.initForms[i]) {
            // 		Assert.assertNull(spec.initForms);
            // 	    } else {
            // 		Funcs.NumberExp expExpr = (Funcs.NumberExp) this.initForms[i];
            // 		Funcs.NumberExp actExpr = (Funcs.NumberExp) spec.initForms[i];

            // 		Assert.assertEquals(expExpr.getValue(), actExpr.getValue());
            // 	    }
            // 	}
            // } else {
            // 	Assert.assertNull(spec.initForms);
            // }

        } catch (Exception ex) {
            System.err.println("OUT_Exc: " + ex);
            System.err.println("EXP_Exc: " + exception);
            try {
                Assert.assertNotNull("Unexpected exception: " + ex, exception);
                Assert.assertEquals(exception.getClass(), ex.getClass());
                Assert.assertEquals(exception.getMessage(), ex.getMessage());
            } catch (Exception ae) {
                ae.printStackTrace();
                throw ae;
            }
        }

    }

    protected static ASTN astnize(Object specObj,
                                  IParser parser,
                                  ParseCtx pctx) {

        if (specObj instanceof List) {
            ASTNList astn = new ASTNList(Utils.list(), pctx);
            for (Object spec : ((List) specObj)) {
                ASTN astnSpec = astnize(spec, parser, pctx);
                astn.add(astnSpec);
            }
            return astn;
        } else {
            ASTN astn = parser.parse(pctx, specObj.toString()).get(0);
            return astn;
        }
    }
   
}


