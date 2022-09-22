package io.opsit.explang;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ArgSpec {
  public static final String ARG_REST = "&REST";

  public static final String ARG_MANDATORY = "&REQUIRED";
  public static final String ARG_OPTIONAL = "&OPTIONAL";

  public static final String ARG_KEY = "&KEY";
  public static final String ARG_ALLOW_OTHER_KEYS = "&ALLOW-OTHER-KEYS";

  public static final String ARG_EAGER = "&EAGER";
  public static final String ARG_LAZY = "&LAZY";
  public static final String ARG_PIPE = "&PIPE";

  private static final Set<String> argSyms =
      Utils.set(ARG_REST, ARG_OPTIONAL, ARG_KEY, ARG_MANDATORY);

  public static boolean isArgsym(Symbol arg) {
    return arg != null && argSyms.contains(arg.getName());
  }

  public static enum AF {
    MANDATORY,
    OPTIONAL,
    MANDATORY2,
    REST,
    KEY,
    REST_KEY;
  }

  protected static class Arg {
    private AF flag;
    private String name;
    /**
     * Form that initializes default arg. value
     */
    private ICompiled initForm;
    /**
     * Status var - true if optional argument was set.
     */
    private String svar;
    private boolean allowOtherKeys = false;
    private boolean lazy = false;
    private boolean pipe = false;

    public AF getFlag() {
      return flag;
    }

    public boolean isLazy() {
      return lazy;
    }

    public boolean isPipe() {
      return this.pipe;
    }

    public boolean isAllowOtherKeys() {
      return allowOtherKeys;
    }

    protected Arg() {
      super();
    }

    protected Arg(
        String name,
        AF flag,
        ICompiled initForm,
        String svar,
        boolean allowOtherKeys,
        boolean isLazy,
        boolean pipe) {
      super();
      this.name = name;
      this.flag = flag;
      this.initForm = initForm;
      this.svar = svar;
      this.allowOtherKeys = allowOtherKeys;
      this.lazy = isLazy;
      this.pipe = pipe;
    }

    @Override
    public int hashCode() {
      return (null == name ? 0 : name.hashCode()) + (null == flag ? 0 : flag.hashCode())
          + (null == svar ? 0 : svar.hashCode()) + (allowOtherKeys ? 0 : 0x01) + (lazy ? 0 : 0x02)
          + (pipe ? 0 : 0x04);
    }

    @Override
    public String toString() {
      return "("
          + flag
          + " "
          + (this.isAllowOtherKeys() ? ARG_ALLOW_OTHER_KEYS : "")
          + " "
          + (this.lazy ? ARG_LAZY : "")
          + " "
          + (this.pipe ? ARG_PIPE : "")
          + " "
          + name
          + " "
          + initForm
          + " "
          + svar
          + ")";
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof Arg) {
        final Arg a = (Arg) obj;
        return ((null == this.name && null == a.name)
                || (null != this.name && this.name.equals(a.name)))
            && ((null == this.flag && null == a.flag)
                || (null != this.flag && this.flag.equals(a.flag)))
            && (this.isAllowOtherKeys() == a.isAllowOtherKeys())
            && (this.isLazy() == a.isLazy())
            && (this.isPipe() == a.isPipe())
            && ((null == this.initForm && null == a.initForm)
                || (null != this.initForm && this.initForm.equals(a.initForm)))
            && ((null == this.svar && null == a.svar)
                || (null != this.svar && this.svar.equals(a.svar)));
      } else {
        return false;
      }
    }
  }

  /**
   * Return list of argument specifiers.
   */
  public List<String> asSpecList() {
    final Arg[] args = this.getArgs();
    // final StringBuilder buf = new StringBuilder(args.length<<3);
    List<String> specList = Utils.list();
    boolean optional = false;
    boolean m2 = false;
    boolean rest = false;
    boolean eager = true;
    boolean aok = false;
    boolean key = false;
    for (int i = 0; i < args.length; i++) {
      Arg arg = args[i];
      AF flag = arg.getFlag();
      if (!optional && flag == AF.OPTIONAL) {
        specList.add(ARG_OPTIONAL);
        optional = true;
      }
      if (!m2 && flag == AF.MANDATORY2) {
        specList.add(ARG_MANDATORY);
        m2 = true;
        optional = false;
      }
      if (!key && (flag == AF.KEY || flag == AF.REST_KEY)) {
        key = true;
        specList.add(ARG_KEY);
      }
      if (!rest && (flag == AF.REST || flag == AF.REST_KEY)) {
        specList.add(ARG_REST);
        rest = true;
      }
      if (eager) {
        if (arg.isLazy()) {
          specList.add(ARG_LAZY);
          eager = false;
        }
      } else {
        if (!arg.isLazy()) {
          specList.add(ARG_EAGER);
          eager = true;
        }
      }
      if (!aok) {
        if (arg.isAllowOtherKeys()) {
          specList.add(ARG_ALLOW_OTHER_KEYS);
          aok = true;
        }
      } else {
        if (!arg.isAllowOtherKeys()) {
          // specList.add("ERROR: "+ARG_ALLOW_OTHER_KEYS);
          aok = false;
        }
      }
      specList.add(arg.name);
    }
    return specList;
  }

  public Arg getArg(int idx) {
    return args[idx];
  }

  public Arg[] getArgs() {
    return args;
  }

  private Arg []args;
  private Object[] argSpecs;
  private boolean hasRest = false;

  /* &required               &opt
   *+---+                         +----+
   *|   V         &opt            V    | &mnd
   *+-mandatory  ----------> optional -+----->mandatory2
   *  |      |                  |                       ^
   *  |      | &rest            V &rest <-----+         |
   *  |      +--------------> rest -----------+ &rest   |
   *  |                        | &key                   |
   *  |&key                   rest_key------------------+
   *  |                                 &required       |
   *  +----------------------- key----------------------+
   *                                    &required
   */
  static final Map<Object, Map<Object, Object>> xmap =
      Utils.map(
          AF.MANDATORY,
              Utils.map(
                  ARG_MANDATORY, AF.MANDATORY,
                  ARG_REST, AF.REST,
                  ARG_OPTIONAL, AF.OPTIONAL,
                  ARG_KEY, AF.KEY),
          AF.OPTIONAL,
              Utils.map(
                  ARG_OPTIONAL, AF.OPTIONAL,
                  ARG_MANDATORY, AF.MANDATORY2,
                  ARG_REST, AF.REST,
                  ARG_KEY, AF.KEY),
          AF.MANDATORY2, Utils.map(ARG_MANDATORY, AF.MANDATORY2),
          AF.REST,
              Utils.map(
                  ARG_REST, AF.REST,
                  ARG_KEY, AF.REST_KEY,
                  ARG_MANDATORY, AF.MANDATORY2),
          AF.KEY,
              Utils.map(
                  ARG_KEY, AF.KEY,
                  ARG_MANDATORY, AF.MANDATORY2,
                  ARG_REST, AF.REST),
          AF.REST_KEY,
              Utils.map(
                  ARG_KEY, AF.REST_KEY,
                  ARG_MANDATORY, AF.MANDATORY2));

  public boolean isHasRest() {
    return hasRest;
  }

  private AF flagTrans(AF current, String spec) throws InvalidParametersException {
    Map<Object, Object> transitions = xmap.get(current);
    if (null == transitions) {
      throw new RuntimeException("Internal error: unknown current arg state: " + current);
    }
    AF result = (AF) transitions.get(spec);
    if (null == result) {
      throw new InvalidParametersException("misplaced argument keyword: " + spec);
    }
    if (result.equals(current)) {
      result = current;
    }
    return result;
  }

  private static Object[] stringsToArray(String[] strArgs) throws InvalidParametersException {
    Object []args = new Object[strArgs.length];
    for (int i = 0; i < strArgs.length; i++) {
      args[i] = new Symbol(strArgs[i]);
    }
    return args;
  }

  private static Object[] astnToArray(ASTNList astnList, Compiler comp)
      throws InvalidParametersException {
    // if (! argSpecs.isList()) {
    //    throw new InvalidParametersException("Argument Specification must be a list, but got "+
    // argSpecs.getObject());
    // }
    // final List astnList = argSpecs.getList();

    Object[] specs = new Object[astnList.size()];
    for (int i = 0; i < astnList.size(); i++) {
      ASTN astn = astnList.get(i);
      if (!astn.isList()) {
        specs[i] = astn.getObject();
      } else {
        ASTNList lst = (ASTNList) astn;
        List<Object> listSpec = new ArrayList<Object>(lst.size());
        for (int j = 0; j < lst.size(); j++) {

          ASTN el = lst.get(j);
          if (!el.isList() && (el.getObject() instanceof Symbol)) {
            listSpec.add(el.getObject());
          } else {
            listSpec.add(comp.compile(el));
          }
        }
        specs[i] = listSpec;
      }
    }
    return specs;
  }

  public ArgSpec(String[] argspecs) throws InvalidParametersException {
    this(stringsToArray(argspecs), null);
  }

  public ArgSpec(String[] argspecs, Compiler comp) throws InvalidParametersException {
    this(stringsToArray(argspecs), comp);
  }

  public ArgSpec(ASTNList argSpecs, Compiler comp) throws InvalidParametersException {
    this(astnToArray(argSpecs, comp), comp);
  }

  /**
   * Build ArgSpec from array of arguments specifications.
   */
  public ArgSpec(Object []argSpecs, Compiler comp) throws InvalidParametersException {
    this.argSpecs = argSpecs;
    AF flag = AF.MANDATORY;
    Arg []args = new Arg[argSpecs.length];
    boolean otherKeys = false;
    boolean lazy = false;
    boolean pipe = false;
    boolean hadPipe = false;
    int argsIdx = 0;
    Arg arg;
    for (int specsIdx = 0; specsIdx < argSpecs.length; specsIdx++) {
      Object spec = this.argSpecs[specsIdx];
      if (spec instanceof Symbol) {
        Symbol specSym = (Symbol) spec;
        if (isArgsym(specSym)) {
          flag = flagTrans(flag, specSym.getName());
          continue;
        }

        if (ARG_ALLOW_OTHER_KEYS.equalsIgnoreCase(specSym.getName())) {
          if (AF.KEY == flag || AF.REST_KEY == flag) {
            otherKeys = true;
            for (int k = argsIdx - 1;
                k >= 0 && (args[k].flag == AF.KEY || args[k].flag == AF.REST_KEY);
                k--) {
              args[k].allowOtherKeys = true;
            }
          } else {
            throw new InvalidParametersException(ARG_ALLOW_OTHER_KEYS + " allowed only after &key");
          }
          continue;
        } else if (!(AF.KEY == flag || AF.REST_KEY == flag)) {
          // ??? WHY?
          otherKeys = false;
        }
        if (ARG_LAZY.equalsIgnoreCase(specSym.getName())) {
          lazy = true;
          continue;
        } else if (ARG_EAGER.equalsIgnoreCase(specSym.getName())) {
          lazy = false;
          continue;
        } else if (ARG_PIPE.equalsIgnoreCase(specSym.getName())) {
          pipe = true;
          continue;
        }
        arg = new Arg();
        arg.name = specSym.getName();
      } else if (spec instanceof List) {
        arg = new Arg();
        @SuppressWarnings("unchecked")
        List<Object> listSpec = (List<Object>) spec;
        if (AF.MANDATORY == flag || AF.MANDATORY2 == flag) {
          throw new InvalidParametersException(
              "Invalid parameter spec: must be Symbol for required argument");
        }
        if (listSpec.isEmpty()) {
          throw new InvalidParametersException("Invalid parameter spec: cannot be empty list");
        }
        if (listSpec.size() > 3) {
          throw new InvalidParametersException("Invalid parameter spec: list is too long");
        }
        if (!(listSpec.get(0) instanceof Symbol)) {
          throw new InvalidParametersException(
              "Invalid parameter spec: first list element must be symbol");
        }
        arg.name = ((Symbol) listSpec.get(0)).getName();
        if (listSpec.size() > 1) {
          arg.initForm = (ICompiled) listSpec.get(1);
        }
        if (listSpec.size() > 2) {
          arg.svar = ((Symbol) listSpec.get(2)).getName();
        }
      } else {
        throw new InvalidParametersException(
            "Invalid parameter spec, must be Symbol or List " + spec);
      }
      arg.flag = flag;
      arg.allowOtherKeys = otherKeys;
      arg.lazy = lazy;
      if (pipe && hadPipe) {
        throw new InvalidParametersException(
            "Invalid parameter spec: only one " + ArgSpec.ARG_PIPE + " can be specified");
      }
      arg.pipe = pipe;
      if (pipe) {
        pipe = false;
        hadPipe = true;
      }
      hasRest |= (AF.REST == flag);
      args[argsIdx] = arg;
      argsIdx++;
    }
    this.args = Arrays.copyOf(args, argsIdx);
  }

  /**
   * Find argument index by name.
   */
  public int nameToIdx(String name) {
    // FIXME: use map!
    for (int i = 0; i < this.args.length; i++) {
      if (name.equals(args[i].name)) {
        return i;
      }
    }
    return -1;
  }

  public ICompiled getInitForm(int idx) {
    ICompiled result = this.args[idx].initForm;
    return null == result ? new Funcs.ObjectExp(null) : result;
  }

  public int size() {
    return args.length;
  }

  /**
   * Convert svar variable name to argument index.
   */
  public int svarNameToIdx(String name) {
    for (int i = 0; i < this.args.length; i++) {
      if (name.equals(args[i].svar)) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Check if name is a parameter variable.
   */
  public boolean isParameterVar(String name) {
    return (this.nameToIdx(name) >= 0) || (this.svarNameToIdx(name) >= 0);
  }
}
