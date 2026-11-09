package io.github.gg22g2.clp.compressorfrontend;

import java.util.Locale;

/**
 * 1:1 port of CLP core string_utils.hpp and string_utils.cpp
 */
public final class StringUtils {
  private StringUtils() {}

  public static boolean isAlphabet(char c) {
    return ('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z');
  }

  public static boolean isDecimalDigit(char c) {
    return '0' <= c && c <= '9';
  }

  public static boolean isWildcard(char c) {
    return '?' == c || '*' == c;
  }

  public static boolean isWildcard(byte c) {
    return '?' == c || '*' == c;
  }

  /**
   * Cleans wildcard search string:
   * <ul>
   *   <li>Removes consecutive '*'</li>
   *   <li>Removes escaping from non-wildcard characters</li>
   *   <li>Removes dangling escape character from the end of the string</li>
   * </ul>
   */
  public static String cleanUpWildcardSearchString(String str) {
    StringBuilder cleanedStr = new StringBuilder();
    boolean isEscaped = false;
    int strEnd = str.length();

    for (int current = 0; current < strEnd;) {
      char c = str.charAt(current);
      if (isEscaped) {
        isEscaped = false;
        if (isWildcard(c) || '\\' == c) {
          cleanedStr.append('\\');
        }
        cleanedStr.append(c);
        ++current;
      } else if ('*' == c) {
        cleanedStr.append(c);
        do {
          ++current;
        } while (current != strEnd && '*' == str.charAt(current));
      } else {
        if ('\\' == c) {
          isEscaped = true;
        } else {
          cleanedStr.append(c);
        }
        ++current;
      }
    }

    return cleanedStr.toString();
  }

  public static boolean wildcardMatchUnsafe(String tame, String wild) {
    return wildcardMatchUnsafe(tame, wild, true);
  }

  public static boolean wildcardMatchUnsafe(String tame, String wild, boolean caseSensitiveMatch) {
    if (caseSensitiveMatch) {
      return wildcardMatchUnsafeCaseSensitive(tame, wild);
    } else {
      return wildcardMatchUnsafeCaseSensitive(tame.toLowerCase(Locale.ROOT), wild.toLowerCase(Locale.ROOT));
    }
  }

  private static class MatchState {
    int tameCurrent;
    int tameBookmark = -1;
    int wildCurrent;
    int wildBookmark = -1;
  }

  private static boolean advanceTameToNextMatch(MatchState state, String tame, String wild) {
    int tameLen = tame.length();
    char w = wild.charAt(state.wildCurrent);
    if ('?' != w) {
      if ('\\' == w) {
        state.wildCurrent++;
        w = wild.charAt(state.wildCurrent);
      }
      while (true) {
        if (tameLen == state.tameCurrent) {
          return false;
        }
        char t = tame.charAt(state.tameCurrent);
        if (t == w) {
          break;
        }
        state.tameCurrent++;
      }
    }
    state.tameBookmark = state.tameCurrent;
    return true;
  }

  public static boolean wildcardMatchUnsafeCaseSensitive(String tame, String wild) {
    int tameLength = tame.length();
    int wildLength = wild.length();

    if (0 == wildLength) {
      return 0 == tameLength;
    } else {
      if (0 == tameLength) {
        return "*".equals(wild);
      }
    }

    MatchState state = new MatchState();

    while (true) {
      char w = wild.charAt(state.wildCurrent);
      if ('*' == w) {
        ++state.wildCurrent;
        if (wildLength == state.wildCurrent) {
          return true;
        }

        state.wildBookmark = state.wildCurrent;
        if (!advanceTameToNextMatch(state, tame, wild)) {
          return false;
        }
      } else {
        boolean isEscaped = false;
        if ('\\' == w) {
          isEscaped = true;
          ++state.wildCurrent;
          w = wild.charAt(state.wildCurrent);
        }

        char t = tame.charAt(state.tameCurrent);
        if (!((!isEscaped && '?' == w) || t == w)) {
          if (-1 == state.wildBookmark) {
            return false;
          }

          state.wildCurrent = state.wildBookmark;
          state.tameCurrent = state.tameBookmark + 1;
          if (!advanceTameToNextMatch(state, tame, wild)) {
            return false;
          }
        }
      }

      ++state.tameCurrent;
      ++state.wildCurrent;

      if (tameLength == state.tameCurrent) {
        return (wildLength == state.wildCurrent
            || ('*' == wild.charAt(state.wildCurrent) && (state.wildCurrent + 1) == wildLength));
      } else {
        if (wildLength == state.wildCurrent) {
          if (-1 == state.wildBookmark) {
            return false;
          } else {
            state.wildCurrent = state.wildBookmark;
            state.tameCurrent = state.tameBookmark + 1;
            if (!advanceTameToNextMatch(state, tame, wild)) {
              return false;
            }
          }
        }
      }
    }
  }
}
