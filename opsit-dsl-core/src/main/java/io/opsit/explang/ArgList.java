package io.opsit.explang;

import io.opsit.explang.ArgSpec.AF;
import io.opsit.explang.Compiler.ICtx;
import io.opsit.explang.Compiler.Eargs;

import java.util.ArrayList;
import java.util.List;

public class ArgList {
  private ArgSpec spec;
  private ICompiled[] params;
  // FIXME: need flag only for optional args
  protected boolean[] setFlags;
  private int restIdx = Integer.MAX_VALUE;
  private List<ICompiled> rest = null;

  public boolean isSet() {
    return this.params != null;
  }

  public ArgList(ArgSpec argSpec) {
    if (null == argSpec) {
      // FIXME?
      throw new RuntimeException("internal error: argSpec not set");
    }
    this.spec = argSpec;
  }

  public ArgList(ArgSpec argSpec, List <ICompiled> params)
    throws InvalidParametersException{
    this(argSpec);
    setParams(params);
  }

  /**
   * copy required parameters 
   * params -> paramsArray
   * from the left to the r
   * params - array of given arguments, may be shorter or longer than arg. spec
   * paramsArray - array of specified arguments
   */
  private void copyReqParams(List<ICompiled> params,
                             ICompiled[] paramsArray,
                             ParseStat st,
                             boolean[] setFlags)
    throws  InvalidParametersException {
    // loop on arg spec
    while(  st.ldst < paramsArray.length - st.rdst ) {
      final ArgSpec.Arg arg = spec.getArg(st.ldst);
      if (AF.MANDATORY  != arg.getFlag()) {
        break;
      }
      // required argument is missing
      if (st.lsrc >= params.size() - st.rdst ) {
        throw new InvalidParametersException("Insufficient number of arguments given");
      }
      setFlags[st.ldst]=true;
      paramsArray[st.ldst++] = copyParam(arg, params.get(st.lsrc++));
    }
  }

  public ICompiled copyParam(ArgSpec.Arg arg, ICompiled expr) {
    return arg.isLazy() ? new LazyEval (expr) :  expr;
  }

  /**
   * Arguments parse status
   */
  private class ParseStat {
    // all indices are 0-based

    // index (from the right)
    // of the next parameter to be written from the right	
    public int ldst = 0;

    // index (from the left) of the next parameter to be read
    // from the left
    public int lsrc = 0;

    // index (from the right)
    // of the next parameter to be written
    // (read index is same) from the right
    public int rdst = 0;
  }
  /**
   * copy required2 parameters
   * params -> paramsArray
   * from right to the left
   * params - array of given arguments, may be shorter or longer than arg. spec
   * paramsArray - array of specified arguments
   * ldst - number of mandatory arguments that were read from the left
   */
  private void copyReq2Params(List<ICompiled> params, ICompiled[] paramsArray, ParseStat st)
    throws  InvalidParametersException {
    int dstIdx;
    // loop on arg spec
    while ((dstIdx = paramsArray.length - 1 - st.rdst) >= st.ldst) {
      final ArgSpec.Arg arg = spec.getArg(dstIdx);
      if (AF.MANDATORY2  != arg.getFlag()) {
        break;
      }
      final int srcIdx = params.size() - 1 - st.rdst;
      if (srcIdx < st.ldst) {
        throw new  InvalidParametersException("Insufficient number of arguments given");
      }
      paramsArray[dstIdx] = copyParam(arg, params.get(srcIdx));
      st.rdst++;
    }
  }
  /* copy optional arguments
   * from left to right
   * start from index startIdx  */
  private void copyOptionalParams(List<ICompiled> params,
                                  ICompiled[] paramsArray,
                                  ParseStat st,
                                  boolean [] sflags) {
    // loop on specified parameters
    while (st.ldst < paramsArray.length - st.rdst) {
      final ArgSpec.Arg arg = spec.getArg(st.ldst);
      if (arg.getFlag()!=AF.OPTIONAL) {
        break;
      }
      if (st.lsrc >= params.size() - st.rdst) {
        paramsArray[st.ldst] = copyParam(arg,spec.getInitForm(st.ldst));
        st.ldst++;
        //new Funcs.ObjectExp(null);
        continue;
      }
      sflags[st.ldst] = true;
      paramsArray[st.ldst++] = copyParam(arg, params.get(st.lsrc++));
    }
  }

  /* copy KV arguments
   * from left to right
   * start from index startIdx  */
  private void copyKVParams(List<ICompiled> params,
                            ICompiled[] paramsArray,
                            ParseStat st,
                            boolean[] setFlags
                            ) throws InvalidParametersException {
    int i = st.ldst;
    boolean outOfKeyArgs = false;
    while (i < paramsArray.length - st.rdst) {
      final ArgSpec.AF flagsVal = spec.getArg(i).getFlag();
      if (flagsVal!=AF.KEY && flagsVal!= AF.REST_KEY) {
        break;
      }
      if (st.lsrc >= params.size() - st.rdst || outOfKeyArgs) {
        // fill non-resolved key word params with default values
        for (i = st.ldst;
             i<paramsArray.length &&
               (spec.getArg(i).getFlag() == AF.KEY || spec.getArg(i).getFlag() == AF.REST_KEY) ;
             i++) {
          if (null == paramsArray[i]) {
            paramsArray[i] =  copyParam(spec.getArg(i),  spec.getInitForm(i));
          }
        }
        break;
      }
      ICompiled pNameSym = params.get(st.lsrc);
      if (! (pNameSym  instanceof Funcs.ObjectExp)) {
        outOfKeyArgs = true;
        if (flagsVal == AF.KEY && spec.isHasRest()) {
          continue;
        }
        throw new InvalidParametersException("expected keyword parameter name, but got "+pNameSym);
      }
      Object pName = ((Funcs.ObjectExp)pNameSym).getValue();
      if (! (pName instanceof Keyword)) {
        outOfKeyArgs = true;
        if (flagsVal == AF.KEY && spec.isHasRest()) {
          continue;
        }
        throw new  InvalidParametersException("Keyword parameter name must start with ':', but got "+pName);
      }
      ICompiled pValue = params.get(st.lsrc+1);
      //System.out.println(pName+"="+pValue);
      int parIdx = spec.nameToIdx(((Keyword)pName).getName().substring(1));
      if (parIdx >= 0) {
        paramsArray[parIdx]=copyParam(spec.getArg(i), pValue);
        setFlags[parIdx] = true;
        i++;
      } else if (! spec.getArg(i).isAllowOtherKeys()) {
        throw new  InvalidParametersException("Unexpected keyword parameter "+ pName);
      }
      if (null != this.rest) {
        this.rest.add(new Funcs.ValueExpr(pName));
        this.rest.add(params.get(st.lsrc+1));
      }
      st.lsrc+=2;
    }
    st.ldst = i;
  }

  /* copy rest of arguments onto &rest argument*/
  private void copyRestParams(List<ICompiled> params,
                              ICompiled[] paramsArray,
                              ParseStat st)
    throws InvalidParametersException
  {
    if (null == this.rest) {
      this.rest = new ArrayList<ICompiled>();
      Funcs.ValueExpr compiledList =new Funcs.ValueExpr(rest);
      paramsArray[st.ldst] =  compiledList;
      restIdx = st.ldst;
    } else {
      throw new RuntimeException("Internal error: rest parameter encountered twice");
    }
    if (st.ldst + 1 >= spec.size() - st.rdst) {
      // loop on specified parameters
      // only when there are no
      // further parameters (except mandatory2)
      while (st.lsrc < params.size() - st.rdst) {
        this.rest.add(copyParam(spec.getArg(st.ldst), params.get(st.lsrc++)));
      }
    }
    st.ldst++;
  }


  private void setParams(List <ICompiled> params)
    throws InvalidParametersException {
    //final AF flags[] = spec.paramFlags;
    final ICompiled[] paramsArray = new ICompiled[spec.size()];
    final boolean[]  sFlagsArray = new boolean[spec.size()];
    final ParseStat st = new ParseStat();
    copyReq2Params(params, paramsArray, st);
    //int rnum = st.rdst;
    while(st.ldst < spec.size() - st.rdst) {
      final AF flag = spec.getArg(st.ldst).getFlag();
      if (AF.MANDATORY == flag) {
        copyReqParams(params, paramsArray, st,  sFlagsArray);
      } else if (AF.OPTIONAL == flag) {
        copyOptionalParams(params, paramsArray, st, sFlagsArray);
      } else if (AF.REST == flag ) {
        copyRestParams(params, paramsArray, st);
      } else if (AF.KEY == flag ||
                 AF.REST_KEY == flag) {
        copyKVParams(params, paramsArray, st, sFlagsArray);
      } else {
        throw new RuntimeException("Internal error: unknown paranmeter flag value"+flag);
      }
    }
    if ((st.lsrc + st.rdst) < params.size()) {
      throw new InvalidParametersException("Too many arguments given");
    }
    this.params = paramsArray;
    this.setFlags = sFlagsArray;
  }

  public Eargs evaluateArguments(Backtrace backtrace, ICtx ctx) {
    final int size = params.length;
    final Object []  result = new Object[size];
    //final boolean [] needEval = new boolean[size];
    Eargs eargs = ctx.getCompiler().newEargs(result, null /*needEval*/, this, ctx);
    for (int i = 0; i < size; i++) {
      //if (this.spec.getArg(i).isLazy()) {
      //	needEval[i] = true;
      //	result[i]=params[i];
      //} else {
      final Object varValue = evaluateArgument(i,backtrace, ctx, eargs);
      result[i]=varValue;
      //}
    }
    return eargs;
  }

  private Object evaluateArgument(int argIdx, Backtrace backtrace, ICtx ctx, Eargs eargs) {
    final ICompiled varExpr = params[argIdx];
    //Object varValue = null;
    // FIXME!
    final ArgSpec.Arg arg = spec.getArg(argIdx);
    if (arg.getFlag() == ArgSpec.AF.REST) {
      final List <ICompiled>lst = (List)varExpr.evaluate(backtrace, ctx);
      final int size = lst.size();
      final List evList = arg.isLazy() ?
        new RestList(new ArrayList(size), backtrace) : new ArrayList(size);
		
      for (int i = 0; i < size; i++) {
        evList.add(lst.get(i).evaluate(backtrace, ctx));
      }
      return  evList;
    } else {
      final boolean isParamSet = setFlags[argIdx];
      // initParams need to be evaluated in the newly created
      // context (eargs) to see previously defined variables
      return varExpr.evaluate(backtrace, isParamSet ?  ctx : eargs);
    }
  }
    
  public int size() {
    return params.length;
  }

  public int totalSize() {
    return (null == rest) ? params.length : params.length - 1 + rest.size();
  }
    
  //public ICompiled getPositionalArg(int i) {
  //	return i < restIdx ?
  //	    params[i] :
  //	    (ICompiled)rest.get(i - restIdx);
  //}

  public ICompiled get(int i) {
    return params[i];
  }

  public ICompiled[] getParams() {
    return params;
  }

  public String checkParamsList(List <ICompiled> params) {
    if (this.isSet()) {
      return "parameters already set!";
    } else {
      return null;
    }
  }

  public Object restIdx() {
    return restIdx;
  }

  public ArgSpec getSpec() {
    return spec;
  }
}
