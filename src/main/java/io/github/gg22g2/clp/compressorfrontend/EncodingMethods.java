package io.github.gg22g2.clp.compressorfrontend;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 1:1 port of CLP core ffi/encoding_methods.hpp and ffi/encoding_methods.inc
 */
public final class EncodingMethods {
  public static final int MAX_DIGITS_IN_REPRESENTABLE_EIGHT_BYTE_FLOAT_VAR = 16;
  public static final long EIGHT_BYTE_ENCODED_FLOAT_DIGITS_BIT_MASK = (1L << 54) - 1L;
  public static final long MAX_REPRESENTABLE_DIGITS_VALUE = 9_999_999_999_999_999L;

  public static final String TOO_FEW_DICTIONARY_VARS_ERROR_MESSAGE =
      "There are fewer dictionary variables than dictionary variable placeholders in the logtype.";
  public static final String TOO_FEW_ENCODED_VARS_ERROR_MESSAGE =
      "There are fewer encoded variables than encoded variable placeholders in the logtype.";

  private EncodingMethods() {}

  public static boolean encodeFloatString(byte[] str, int offset, int length, long[] encodedVar) {
    if (0 == length) {
      return false;
    }

    int pos = 0;
    int maxLength = MAX_DIGITS_IN_REPRESENTABLE_EIGHT_BYTE_FLOAT_VAR + 1;

    boolean isNegative = false;
    if ('-' == str[offset + pos]) {
      isNegative = true;
      ++pos;
      ++maxLength;
    }

    if (length > maxLength) {
      return false;
    }

    int numDigits = 0;
    int decimalPointPos = -1;
    long digits = 0;
    for (; pos < length; ++pos) {
      byte c = str[offset + pos];
      if ('0' <= c && c <= '9') {
        digits *= 10;
        digits += (c - '0');
        ++numDigits;
      } else if (-1 == decimalPointPos && '.' == c) {
        decimalPointPos = length - 1 - pos;
      } else {
        return false;
      }
    }

    if (-1 == decimalPointPos || 0 == decimalPointPos || 0 == numDigits) {
      return false;
    }

    encodedVar[0] = encodeFloatProperties(isNegative, digits, numDigits, decimalPointPos);
    return true;
  }

  public static boolean encodeFloatString(String str, long[] encodedVar) {
    byte[] bytes = str.getBytes(StandardCharsets.ISO_8859_1);
    return encodeFloatString(bytes, 0, bytes.length, encodedVar);
  }

  public static long encodeFloatProperties(
      boolean isNegative,
      long digits,
      int numDigits,
      int decimalPointPos
  ) {
    long encodedFloat = 0;
    if (isNegative) {
      encodedFloat = 1L;
    }
    encodedFloat <<= 55;
    encodedFloat |= (digits & EIGHT_BYTE_ENCODED_FLOAT_DIGITS_BIT_MASK);
    encodedFloat <<= 4;
    encodedFloat |= ((numDigits - 1) & 0x0F);
    encodedFloat <<= 4;
    encodedFloat |= ((decimalPointPos - 1) & 0x0F);
    return encodedFloat;
  }

  public static class FloatProperties {
    public boolean isNegative;
    public long digits;
    public int numDigits;
    public int decimalPointPos;
  }

  public static void decodeFloatProperties(long encodedVar, FloatProperties props) throws IOException {
    props.decimalPointPos = (int) (encodedVar & 0x0F) + 1;
    long temp = encodedVar >>> 4;
    props.numDigits = (int) (temp & 0x0F) + 1;
    temp >>>= 4;
    props.digits = temp & EIGHT_BYTE_ENCODED_FLOAT_DIGITS_BIT_MASK;
    if (props.digits > MAX_REPRESENTABLE_DIGITS_VALUE) {
      throw new IOException("Digits in encoded float are larger than max representable value.");
    }
    temp >>>= 55;
    props.isNegative = temp > 0;
  }

  public static String decodeFloatVar(long encodedVar) throws IOException {
    FloatProperties props = new FloatProperties();
    decodeFloatProperties(encodedVar, props);

    if (props.numDigits < props.decimalPointPos) {
      throw new IOException("Invalid decimal-point position in encoded float.");
    }

    int valueLength = props.numDigits + 1 + (props.isNegative ? 1 : 0);
    byte[] value = new byte[valueLength];
    int numCharsToProcess = valueLength;

    if (props.isNegative) {
      value[0] = '-';
      --numCharsToProcess;
    }

    int pos = valueLength - 1;
    int decimalPointPosFromLeft = valueLength - 1 - props.decimalPointPos;
    long digits = props.digits;

    for (; pos > decimalPointPosFromLeft && digits > 0; --pos) {
      value[pos] = (byte) ('0' + (digits % 10));
      digits /= 10;
      --numCharsToProcess;
    }

    if (digits > 0) {
      String cTooManyDigitsErrorMsg =
          "Encoded number of digits doesn't match encoded digits in encoded float.";
      if (0 == numCharsToProcess) {
        throw new IOException(cTooManyDigitsErrorMsg);
      }
      --pos;
      --numCharsToProcess;

      while (digits > 0) {
        if (0 == numCharsToProcess) {
          throw new IOException(cTooManyDigitsErrorMsg);
        }
        value[pos--] = (byte) ('0' + (digits % 10));
        digits /= 10;
        --numCharsToProcess;
      }
    }

    for (; numCharsToProcess > 0; --numCharsToProcess) {
      value[pos--] = '0';
    }

    value[decimalPointPosFromLeft] = '.';
    return new String(value, StandardCharsets.ISO_8859_1);
  }

  public static boolean convertStringToInt(byte[] str, int offset, int length, long[] result) {
    if (0 == length) {
      return false;
    }
    boolean negative = false;
    int i = offset;
    int end = offset + length;
    if ('-' == str[i]) {
      negative = true;
      i++;
    }
    long limit = negative ? Long.MIN_VALUE : -Long.MAX_VALUE;
    long multmin = limit / 10;
    long val = 0;
    while (i < end) {
      byte c = str[i++];
      if (c < '0' || c > '9') {
        return false;
      }
      int digit = c - '0';
      if (val < multmin) {
        return false;
      }
      val *= 10;
      if (val < limit + digit) {
        return false;
      }
      val -= digit;
    }
    result[0] = negative ? val : -val;
    return true;
  }

  public static boolean encodeIntegerString(byte[] str, int offset, int length, long[] encodedVar) {
    if (0 == length) {
      return false;
    }

    if ('-' == str[offset]) {
      if (length < 2 || str[offset + 1] < '1' || '9' < str[offset + 1]) {
        return false;
      }
    } else {
      if (str[offset] < '0' || '9' < str[offset]) {
        return false;
      }
      if (length > 1 && '0' == str[offset]) {
        return false;
      }
    }

    return convertStringToInt(str, offset, length, encodedVar);
  }

  public static boolean encodeIntegerString(String str, long[] encodedVar) {
    byte[] bytes = str.getBytes(StandardCharsets.ISO_8859_1);
    return encodeIntegerString(bytes, 0, bytes.length, encodedVar);
  }

  public static String decodeIntegerVar(long encodedVar) {
    return Long.toString(encodedVar);
  }

  /**
   * 1:1 port of encode_message
   */
  public static boolean encodeMessage(byte[] message, EncodedMessage encodedMessage) throws IOException {
    int msgLength = message.length;
    ByteArrayOutputStream logtypeStream = new ByteArrayOutputStream(msgLength);
    List<Long> encodedVarsList = new ArrayList<>();
    List<Integer> dictVarBoundsList = new ArrayList<>();

    int[] bounds = new int[]{0, 0};
    int constantBeginPos = 0;
    long[] encodedVarBuf = new long[1];

    while (Parsing.getBoundsOfNextVar(message, 0, msgLength, bounds)) {
      int varBeginPos = bounds[0];
      int varEndPos = bounds[1];

      // Process constant
      int constantLen = varBeginPos - constantBeginPos;
      for (int i = 0; i < constantLen; ++i) {
        if (Parsing.isVariablePlaceholder(message[constantBeginPos + i])) {
          return false;
        }
      }
      logtypeStream.write(message, constantBeginPos, constantLen);
      constantBeginPos = varEndPos;

      // Encode variable
      int varLen = varEndPos - varBeginPos;
      if (encodeFloatString(message, varBeginPos, varLen, encodedVarBuf)) {
        logtypeStream.write(VariablePlaceholder.Float);
        encodedVarsList.add(encodedVarBuf[0]);
      } else if (encodeIntegerString(message, varBeginPos, varLen, encodedVarBuf)) {
        logtypeStream.write(VariablePlaceholder.Integer);
        encodedVarsList.add(encodedVarBuf[0]);
      } else {
        logtypeStream.write(VariablePlaceholder.Dictionary);
        dictVarBoundsList.add(varBeginPos);
        dictVarBoundsList.add(varEndPos);
      }
    }

    if (constantBeginPos < msgLength) {
      int constantLen = msgLength - constantBeginPos;
      for (int i = 0; i < constantLen; ++i) {
        if (Parsing.isVariablePlaceholder(message[constantBeginPos + i])) {
          return false;
        }
      }
      logtypeStream.write(message, constantBeginPos, constantLen);
    }

    byte[] logtype = logtypeStream.toByteArray();
    encodedMessage.setLogtype(logtype);

    if (!encodedVarsList.isEmpty()) {
      long[] encodedVars = new long[encodedVarsList.size()];
      for (int i = 0; i < encodedVars.length; ++i) {
        encodedVars[i] = encodedVarsList.get(i);
      }
      encodedMessage.setEncodedVars(encodedVars);
    } else {
      encodedMessage.setEncodedVars(null);
    }

    if (!dictVarBoundsList.isEmpty()) {
      int[] dictVarBounds = new int[dictVarBoundsList.size()];
      for (int i = 0; i < dictVarBounds.length; ++i) {
        dictVarBounds[i] = dictVarBoundsList.get(i);
      }
      encodedMessage.setDictionaryVarBounds(dictVarBounds);
    } else {
      encodedMessage.setDictionaryVarBounds(null);
    }

    return true;
  }

  /**
   * 1:1 port of decode_message
   */
  public static byte[] decodeMessage(
      byte[] logtype,
      int logtypeLen,
      byte[] allDictionaryVars,
      int allDictionaryVarsLen,
      int[] dictionaryVarEndOffsets,
      int dictionaryVarEndOffsetsLen,
      long[] encodedVars,
      int encodedVarsLen
  ) throws IOException {
    ByteArrayOutputStream message = new ByteArrayOutputStream(logtypeLen * 2);
    int lastVariableEndPos = 0;
    int dictionaryVarBeginPos = 0;
    int dictionaryVarBoundsIx = 0;
    int encodedVarsIx = 0;

    for (int i = 0; i < logtypeLen; ++i) {
      byte c = logtype[i];
      if (VariablePlaceholder.Float == c) {
        message.write(logtype, lastVariableEndPos, i - lastVariableEndPos);
        lastVariableEndPos = i + 1;
        if (encodedVarsIx >= encodedVarsLen) {
          throw new IOException(TOO_FEW_ENCODED_VARS_ERROR_MESSAGE);
        }
        String floatStr = decodeFloatVar(encodedVars[encodedVarsIx++]);
        byte[] floatBytes = floatStr.getBytes(StandardCharsets.ISO_8859_1);
        message.write(floatBytes, 0, floatBytes.length);
      } else if (VariablePlaceholder.Integer == c) {
        message.write(logtype, lastVariableEndPos, i - lastVariableEndPos);
        lastVariableEndPos = i + 1;
        if (encodedVarsIx >= encodedVarsLen) {
          throw new IOException(TOO_FEW_ENCODED_VARS_ERROR_MESSAGE);
        }
        String intStr = decodeIntegerVar(encodedVars[encodedVarsIx++]);
        byte[] intBytes = intStr.getBytes(StandardCharsets.ISO_8859_1);
        message.write(intBytes, 0, intBytes.length);
      } else if (VariablePlaceholder.Dictionary == c) {
        message.write(logtype, lastVariableEndPos, i - lastVariableEndPos);
        lastVariableEndPos = i + 1;
        if (dictionaryVarBoundsIx >= dictionaryVarEndOffsetsLen) {
          throw new IOException(TOO_FEW_DICTIONARY_VARS_ERROR_MESSAGE);
        }
        int endPos = dictionaryVarEndOffsets[dictionaryVarBoundsIx++];
        int varLen = endPos - dictionaryVarBeginPos;
        message.write(allDictionaryVars, dictionaryVarBeginPos, varLen);
        dictionaryVarBeginPos = endPos;
      }
    }

    if (lastVariableEndPos < logtypeLen) {
      message.write(logtype, lastVariableEndPos, logtypeLen - lastVariableEndPos);
    }

    return message.toByteArray();
  }

  public static boolean wildcardQueryMatchesAnyIntVar(
      String wildcardQuery,
      String logtype,
      long[] encodedVars,
      int encodedVarsLen
  ) throws IOException {
    int encodedVarsIx = 0;
    int logtypeLen = logtype.length();
    for (int i = 0; i < logtypeLen; ++i) {
      char c = logtype.charAt(i);
      if (VariablePlaceholder.Float == c) {
        if (encodedVarsIx >= encodedVarsLen) {
          throw new IOException(TOO_FEW_ENCODED_VARS_ERROR_MESSAGE);
        }
        ++encodedVarsIx;
      } else if (VariablePlaceholder.Integer == c) {
        if (encodedVarsIx >= encodedVarsLen) {
          throw new IOException(TOO_FEW_ENCODED_VARS_ERROR_MESSAGE);
        }
        String decodedVar = decodeIntegerVar(encodedVars[encodedVarsIx]);
        if (StringUtils.wildcardMatchUnsafe(decodedVar, wildcardQuery)) {
          return true;
        }
        ++encodedVarsIx;
      }
    }
    return false;
  }

  public static boolean wildcardQueryMatchesAnyFloatVar(
      String wildcardQuery,
      String logtype,
      long[] encodedVars,
      int encodedVarsLen
  ) throws IOException {
    int encodedVarsIx = 0;
    int logtypeLen = logtype.length();
    for (int i = 0; i < logtypeLen; ++i) {
      char c = logtype.charAt(i);
      if (VariablePlaceholder.Float == c) {
        if (encodedVarsIx >= encodedVarsLen) {
          throw new IOException(TOO_FEW_ENCODED_VARS_ERROR_MESSAGE);
        }
        String decodedVar = decodeFloatVar(encodedVars[encodedVarsIx]);
        if (StringUtils.wildcardMatchUnsafe(decodedVar, wildcardQuery)) {
          return true;
        }
        ++encodedVarsIx;
      } else if (VariablePlaceholder.Integer == c) {
        if (encodedVarsIx >= encodedVarsLen) {
          throw new IOException(TOO_FEW_ENCODED_VARS_ERROR_MESSAGE);
        }
        ++encodedVarsIx;
      }
    }
    return false;
  }

  /**
   * 1:1 port of wildcard_match_encoded_vars
   */
  public static boolean wildcardMatchEncodedVars(
      byte[] logtype,
      long[] encodedVars,
      int encodedVarsLen,
      byte[] wildcardVarPlaceholders,
      String[] wildcardVarQueries
  ) throws IOException {
    if (null == encodedVars) {
      throw new IllegalArgumentException(TOO_FEW_ENCODED_VARS_ERROR_MESSAGE);
    }
    if (wildcardVarQueries.length != wildcardVarPlaceholders.length) {
      throw new IllegalArgumentException(TOO_FEW_ENCODED_VARS_ERROR_MESSAGE);
    }

    int wildcardVarQueriesLen = wildcardVarQueries.length;
    int varIx = 0;
    int wildcardVarIx = 0;

    for (byte c : logtype) {
      if (VariablePlaceholder.Float == c) {
        if (varIx >= encodedVarsLen) {
          throw new IOException(TOO_FEW_ENCODED_VARS_ERROR_MESSAGE);
        }

        if (wildcardVarIx < wildcardVarQueriesLen && wildcardVarPlaceholders[wildcardVarIx] == c) {
          String decodedVar = decodeFloatVar(encodedVars[varIx]);
          if (StringUtils.wildcardMatchUnsafe(decodedVar, wildcardVarQueries[wildcardVarIx])) {
            ++wildcardVarIx;
            if (wildcardVarIx == wildcardVarQueriesLen) {
              break;
            }
          }
        }
        ++varIx;
      } else if (VariablePlaceholder.Integer == c) {
        if (varIx >= encodedVarsLen) {
          throw new IOException(TOO_FEW_ENCODED_VARS_ERROR_MESSAGE);
        }

        if (wildcardVarIx < wildcardVarQueriesLen && wildcardVarPlaceholders[wildcardVarIx] == c) {
          String decodedVar = decodeIntegerVar(encodedVars[varIx]);
          if (StringUtils.wildcardMatchUnsafe(decodedVar, wildcardVarQueries[wildcardVarIx])) {
            ++wildcardVarIx;
            if (wildcardVarIx == wildcardVarQueriesLen) {
              break;
            }
          }
        }
        ++varIx;
      }
    }

    return wildcardVarQueriesLen == wildcardVarIx;
  }
}
