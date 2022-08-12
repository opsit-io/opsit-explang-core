package io.opsit.explang;

import static io.opsit.explang.Utils.list;

import io.opsit.explang.Compiler.Eargs;
import io.opsit.explang.Funcs.NUMEQ;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DWIM {
  @Arguments(
      spec = {"arg0", ArgSpec.ARG_OPTIONAL, "arg2"},
      text = "{object pattern}")
  @Docstring(
      text =
          "Perform DWIM find operation. When pattern is an Regexp tries to find the regexp in"
              + " String Representation of object.\n"
              + "  When pattern is a String it tries to find its occurences in the String"
              + " Representation of object.\n"
              + " When pattern is a Number then numeric equality check is performed. \n"
              + " When pattern is any other object the equality test is performed. \n"
              + " If there were matches the function returns list of matches. \n"
              + "If there is no match an empty list is returned.")
  public static class DWIM_MATCHES extends NUMEQ {
    @Override
    public Object evalWithArgs(Backtrace backtrace, Eargs eargs) {
      List<Object> result = list();
      final Object obj = eargs.get(0, backtrace);
      final Object patObj = eargs.get(1, backtrace);
      if (patObj == null) {
        if (obj == null) {
          result.add(null);
        }
      } else if (obj != null) {
        if (patObj instanceof Pattern) {
          final String objStr = Utils.asString(obj);
          final Pattern pat = (Pattern) patObj;
          final Matcher m = pat.matcher(objStr);
          while (m.find()) {
            result.add(m.group());
          }
        } else if (patObj instanceof Number) {
          try {
            final Number objNum = Utils.asNumberOrParse(obj);
            final Number patNum = (Number) patObj;
            final Promotion p = new Promotion();
            p.promote(patNum);
            p.promote(objNum);
            final Integer dif = p.callOP(this, patNum, objNum).intValue();
            final boolean isEqual = compareResult(dif);
            if (isEqual) {
              result.add(objNum);
            }
          } catch (Exception ex) {
            return result;
          }
        } else if (patObj instanceof CharSequence) {
          final String objStr = Utils.asString(obj);
          final String patStr = Utils.asString(patObj);
          int idx = objStr.indexOf(patStr);
          if (idx >= 0) {
            result.add(patStr);
          }
        } else {
          if (obj.equals(patObj)) {
            result.add(patObj);
          }
        }
      }
      return result;
    }
  }
}
