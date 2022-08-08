package io.opsit.explang;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;


public class GlobPatternTest {
  @Test
  public void star_becomes_dot_star() throws Exception {
    assertTrue(GlobPattern.compile("gl*b") instanceof java.util.regex.Pattern);
    assertEquals("gl.*b", GlobPattern.compile("gl*b").toString());
  }

  @Test
  public void escaped_star_is_unchanged() throws Exception {
    assertEquals("gl\\*b", GlobPattern.compile("gl\\*b").toString());
  }

  @Test
  public void question_mark_becomes_dot() throws Exception {
    assertEquals("gl.b", GlobPattern.compile("gl?b").toString());
  }

  @Test
  public void escaped_question_mark_is_unchanged() throws Exception {
    assertEquals("gl\\?b", GlobPattern.compile("gl\\?b").toString());
  }

  @Test
  public void character_classes_dont_need_conversion() throws Exception {
    assertEquals("gl[-o]b", GlobPattern.compile("gl[-o]b").toString());
  }

  @Test
  public void escaped_classes_are_unchanged() throws Exception {
    assertEquals("gl\\[-o\\]b", GlobPattern.compile("gl\\[-o\\]b").toString());
  }

  @Test
  public void negation_in_character_classes() throws Exception {
    assertEquals("gl[^a-n!p-z]b", GlobPattern.compile("gl[!a-n!p-z]b").toString());
  }

  @Test
  public void nested_negation_in_character_classes() throws Exception {
    assertEquals("gl[[^a-n]!p-z]b", GlobPattern.compile("gl[[!a-n]!p-z]b").toString());
  }

  @Test
  public void escape_carat_if_it_is_the_first_char_in_a_character_class() throws Exception {
    assertEquals("gl[\\^o]b", GlobPattern.compile("gl[^o]b").toString());
  }

  @Test
  public void metachars_are_escaped() throws Exception {
    assertEquals("gl..*\\.\\(\\)\\+\\|\\^\\$\\@\\%b", GlobPattern.compile("gl?*.()+|^$@%b").toString());
  }

  @Test
  public void metachars_in_character_classes_dont_need_escaping() throws Exception {
    assertEquals("gl[?*.()+|^$@%]b", GlobPattern.compile("gl[?*.()+|^$@%]b").toString());
  }

  @Test
  public void escaped_backslash_is_unchanged() throws Exception {
    assertEquals("gl\\\\b", GlobPattern.compile("gl\\\\b").toString());
  }

  @Test
  public void slashQ_and_slashE_are_escaped() throws Exception {
    assertEquals("\\\\Qglob\\\\E", GlobPattern.compile("\\Qglob\\E").toString());
  }

  @Test
  public void braces_are_turned_into_groups() throws Exception {
    assertEquals("(glob|regex)", GlobPattern.compile("{glob,regex}").toString());
  }

  @Test
  public void escaped_braces_are_unchanged() throws Exception {
    assertEquals("\\{glob\\}", GlobPattern.compile("\\{glob\\}").toString());
  }
 
  @Test
  public void commas_dont_need_escaping() throws Exception {
    assertEquals("(glob,regex),", GlobPattern.compile("{glob\\,regex},").toString());
  }   
}
