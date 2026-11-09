package io.github.gg22g2.clp.compressorfrontend;

import java.io.ByteArrayOutputStream;

/**
 * 1:1 port of CLP core ir/parsing.hpp and ir/parsing.cpp
 */
public final class Parsing {
  private Parsing() {}

  public static boolean isDelim(byte c) {
    return !('+' == c
        || ('-' <= c && c <= '.')
        || ('0' <= c && c <= '9')
        || ('A' <= c && c <= 'Z')
        || '\\' == c
        || '_' == c
        || ('a' <= c && c <= 'z'));
  }

  public static boolean isDelim(char c) {
    return !('+' == c
        || ('-' <= c && c <= '.')
        || ('0' <= c && c <= '9')
        || ('A' <= c && c <= 'Z')
        || '\\' == c
        || '_' == c
        || ('a' <= c && c <= 'z'));
  }

  public static boolean isVariablePlaceholder(byte c) {
    return VariablePlaceholder.Integer == c
        || VariablePlaceholder.Dictionary == c
        || VariablePlaceholder.Float == c;
  }

  public static boolean isVariablePlaceholder(char c) {
    return VariablePlaceholder.Integer == c
        || VariablePlaceholder.Dictionary == c
        || VariablePlaceholder.Float == c;
  }

  public static boolean isDecimalDigit(byte c) {
    return '0' <= c && c <= '9';
  }

  public static boolean isDecimalDigit(char c) {
    return '0' <= c && c <= '9';
  }

  public static boolean isAlphabet(byte c) {
    return ('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z');
  }

  public static boolean isAlphabet(char c) {
    return ('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z');
  }

  public static boolean couldBeMultiDigitHexValue(byte[] str, int begin, int end) {
    if (end - begin < 2) {
      return false;
    }
    for (int i = begin; i < end; ++i) {
      byte c = str[i];
      if (!(('a' <= c && c <= 'f') || ('A' <= c && c <= 'F') || ('0' <= c && c <= '9'))) {
        return false;
      }
    }
    return true;
  }

  public static boolean couldBeMultiDigitHexValue(String str) {
    int length = str.length();
    if (length < 2) {
      return false;
    }
    for (int i = 0; i < length; ++i) {
      char c = str.charAt(i);
      if (!(('a' <= c && c <= 'f') || ('A' <= c && c <= 'F') || ('0' <= c && c <= '9'))) {
        return false;
      }
    }
    return true;
  }

  public static boolean couldBeMultiDigitHexValue(String str, int begin, int end) {
    if (end - begin < 2) {
      return false;
    }
    for (int i = begin; i < end; ++i) {
      char c = str.charAt(i);
      if (!(('a' <= c && c <= 'f') || ('A' <= c && c <= 'F') || ('0' <= c && c <= '9'))) {
        return false;
      }
    }
    return true;
  }

  public static boolean isVar(byte[] value, int offset, int length) {
    int[] bounds = new int[]{0, 0};
    if (getBoundsOfNextVar(value, offset, length, bounds)) {
      return bounds[0] == 0 && bounds[1] == length;
    }
    return false;
  }

  public static boolean isVar(String value) {
    int[] bounds = new int[]{0, 0};
    if (getBoundsOfNextVar(value, bounds)) {
      return bounds[0] == 0 && bounds[1] == value.length();
    }
    return false;
  }

  /**
   * Gets the bounds of the next variable in the given byte array.
   * bounds[0] is in-out begin_pos, bounds[1] is in-out end_pos.
   *
   * @param str message byte array
   * @param offset offset into str
   * @param msgLength length of slice in str
   * @param bounds [0] = begin_pos, [1] = end_pos (relative to slice)
   * @return true if a variable was found, false otherwise
   */
  public static boolean getBoundsOfNextVar(byte[] str, int offset, int msgLength, int[] bounds) {
    int endPos = bounds[1];
    if (msgLength <= endPos) {
      return false;
    }

    int beginPos;
    while (true) {
      beginPos = endPos;

      // Find next non-delimiter
      for (; beginPos < msgLength; ++beginPos) {
        byte c = str[offset + beginPos];
        if (!isDelim(c)) {
          break;
        }
      }
      if (msgLength == beginPos) {
        bounds[0] = beginPos;
        bounds[1] = endPos;
        return false;
      }

      boolean containsDecimalDigit = false;
      boolean containsAlphabet = false;

      // Find next delimiter
      endPos = beginPos;
      for (; endPos < msgLength; ++endPos) {
        byte c = str[offset + endPos];
        if (isDecimalDigit(c)) {
          containsDecimalDigit = true;
        } else if (isAlphabet(c)) {
          containsAlphabet = true;
        } else if (isDelim(c)) {
          break;
        }
      }

      // Treat token as variable if:
      // - it contains a decimal digit, or
      // - it's directly preceded by '=' and contains an alphabet char, or
      // - it could be a multi-digit hex value
      if (containsDecimalDigit
          || (0 < beginPos && '=' == str[offset + beginPos - 1] && containsAlphabet)
          || couldBeMultiDigitHexValue(str, offset + beginPos, offset + endPos)) {
        break;
      }
    }

    bounds[0] = beginPos;
    bounds[1] = endPos;
    return msgLength != beginPos;
  }

  public static boolean getBoundsOfNextVar(String str, int[] bounds) {
    int msgLength = str.length();
    int endPos = bounds[1];
    if (msgLength <= endPos) {
      return false;
    }

    int beginPos;
    while (true) {
      beginPos = endPos;

      // Find next non-delimiter
      for (; beginPos < msgLength; ++beginPos) {
        char c = str.charAt(beginPos);
        if (!isDelim(c)) {
          break;
        }
      }
      if (msgLength == beginPos) {
        bounds[0] = beginPos;
        bounds[1] = endPos;
        return false;
      }

      boolean containsDecimalDigit = false;
      boolean containsAlphabet = false;

      // Find next delimiter
      endPos = beginPos;
      for (; endPos < msgLength; ++endPos) {
        char c = str.charAt(endPos);
        if (isDecimalDigit(c)) {
          containsDecimalDigit = true;
        } else if (isAlphabet(c)) {
          containsAlphabet = true;
        } else if (isDelim(c)) {
          break;
        }
      }

      if (containsDecimalDigit
          || (0 < beginPos && '=' == str.charAt(beginPos - 1) && containsAlphabet)
          || couldBeMultiDigitHexValue(str, beginPos, endPos)) {
        break;
      }
    }

    bounds[0] = beginPos;
    bounds[1] = endPos;
    return msgLength != beginPos;
  }

  public static void escapeAndAppendConstantToLogtype(byte[] constant, int offset, int length,
                                                      ByteArrayOutputStream logtype) {
    int beginPos = 0;
    for (int i = 0; i < length; ++i) {
      byte c = constant[offset + i];
      if (VariablePlaceholder.EscapeCharacter == c || isVariablePlaceholder(c)) {
        logtype.write(constant, offset + beginPos, i - beginPos);
        logtype.write(VariablePlaceholder.EscapeCharacter);
        beginPos = i;
      }
    }
    logtype.write(constant, offset + beginPos, length - beginPos);
  }
}
