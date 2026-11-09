package io.github.gg22g2.clp.compressorfrontend;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 1:1 verbatim port of official CLP C++ core Catch2 test: test-string_utils.cpp
 */
public class OfficialCppStringUtilsTest {

  @Test
  void testCleanUpWildcardSearchString() {
    // No wildcards
    assertEquals("test", StringUtils.cleanUpWildcardSearchString("test"));

    // Only '?'
    assertEquals("?est", StringUtils.cleanUpWildcardSearchString("?est"));

    // Normal case with asterisks and escapes
    assertEquals("*t*\\*s\\?t?*", StringUtils.cleanUpWildcardSearchString("***t**\\*s\\?t?**"));

    // Abnormal cases
    assertEquals("*", StringUtils.cleanUpWildcardSearchString("***"));
    assertEquals("*?*", StringUtils.cleanUpWildcardSearchString("*?*"));
    assertEquals("?", StringUtils.cleanUpWildcardSearchString("?"));
    assertEquals("abc", StringUtils.cleanUpWildcardSearchString("a\\bc\\"));
  }

  @Test
  void testWildcardMatchStarCases() {
    String tame = "abcd";
    assertTrue(StringUtils.wildcardMatchUnsafeCaseSensitive(tame, "a*"));
    assertTrue(StringUtils.wildcardMatchUnsafeCaseSensitive(tame, "*d"));
    assertTrue(StringUtils.wildcardMatchUnsafeCaseSensitive(tame, "*a*"));
    assertTrue(StringUtils.wildcardMatchUnsafeCaseSensitive(tame, "*b*"));
    assertTrue(StringUtils.wildcardMatchUnsafeCaseSensitive(tame, "*d*"));
    assertTrue(StringUtils.wildcardMatchUnsafeCaseSensitive(tame, "*"));
    assertFalse(StringUtils.wildcardMatchUnsafeCaseSensitive(tame, "b*"));
    assertFalse(StringUtils.wildcardMatchUnsafeCaseSensitive(tame, "*c"));
  }

  @Test
  void testWildcardMatchQuestionMarkCases() {
    String tame = "abcd";
    assertTrue(StringUtils.wildcardMatchUnsafeCaseSensitive(tame, "a?cd"));
    assertTrue(StringUtils.wildcardMatchUnsafeCaseSensitive(tame, "?bcd"));
    assertTrue(StringUtils.wildcardMatchUnsafeCaseSensitive(tame, "abc?"));
    assertTrue(StringUtils.wildcardMatchUnsafeCaseSensitive(tame, "a??d"));
    assertTrue(StringUtils.wildcardMatchUnsafeCaseSensitive(tame, "??cd"));
    assertTrue(StringUtils.wildcardMatchUnsafeCaseSensitive(tame, "ab??"));
    assertTrue(StringUtils.wildcardMatchUnsafeCaseSensitive(tame, "?bc?"));
    assertTrue(StringUtils.wildcardMatchUnsafeCaseSensitive("abcdef", "a?c?ef"));
    assertTrue(StringUtils.wildcardMatchUnsafeCaseSensitive(tame, "????"));

    assertFalse(StringUtils.wildcardMatchUnsafeCaseSensitive(tame, "???"));
    assertFalse(StringUtils.wildcardMatchUnsafeCaseSensitive(tame, "?????"));
    assertFalse(StringUtils.wildcardMatchUnsafeCaseSensitive(tame, "a?ce"));
  }

  @Test
  void testWildcardMatchMixedStarAndQuestionMarkCases() {
    String tame = "abcd";
    assertTrue(StringUtils.wildcardMatchUnsafeCaseSensitive(tame, "*?bcd"));
    assertTrue(StringUtils.wildcardMatchUnsafeCaseSensitive(tame, "?*bcd"));
    assertTrue(StringUtils.wildcardMatchUnsafeCaseSensitive(tame, "*?*?*?*?*"));
    assertTrue(StringUtils.wildcardMatchUnsafeCaseSensitive("abcde", "a*c?e"));
    assertTrue(StringUtils.wildcardMatchUnsafeCaseSensitive("abcde", "a?c*e"));
  }

  @Test
  void testWildcardCaseInsensitive() {
    assertTrue(StringUtils.wildcardMatchUnsafe("ABCD", "a*d", false));
    assertTrue(StringUtils.wildcardMatchUnsafe("HelloWorld", "h?llo*d", false));
    assertFalse(StringUtils.wildcardMatchUnsafe("ABCD", "a*d", true)); // case sensitive fails
  }
}
