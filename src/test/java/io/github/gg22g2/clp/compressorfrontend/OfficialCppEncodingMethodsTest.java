package io.github.gg22g2.clp.compressorfrontend;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 1:1 verbatim port of official CLP C++ core Catch2 test: test-encoding_methods.cpp
 */
public class OfficialCppEncodingMethodsTest {

  @Test
  void testEncodingIntegersOfficialCases() {
    long[] encodedVar = new long[1];
    String value;
    String decodedValue;

    // Test basic conversions
    value = "0";
    assertTrue(EncodingMethods.encodeIntegerString(value, encodedVar));
    decodedValue = EncodingMethods.decodeIntegerVar(encodedVar[0]);
    assertEquals(value, decodedValue);

    value = "-1";
    assertTrue(EncodingMethods.encodeIntegerString(value, encodedVar));
    decodedValue = EncodingMethods.decodeIntegerVar(encodedVar[0]);
    assertEquals(value, decodedValue);

    value = "1";
    assertTrue(EncodingMethods.encodeIntegerString(value, encodedVar));
    decodedValue = EncodingMethods.decodeIntegerVar(encodedVar[0]);
    assertEquals(value, decodedValue);

    // Test edges of representable range
    value = Long.toString(Long.MIN_VALUE);
    assertTrue(EncodingMethods.encodeIntegerString(value, encodedVar));
    decodedValue = EncodingMethods.decodeIntegerVar(encodedVar[0]);
    assertEquals(value, decodedValue);

    value = Long.toString(Long.MAX_VALUE);
    assertTrue(EncodingMethods.encodeIntegerString(value, encodedVar));
    decodedValue = EncodingMethods.decodeIntegerVar(encodedVar[0]);
    assertEquals(value, decodedValue);

    // Overflow cases
    value = "9223372036854775808";  // Long.MAX_VALUE + 1 == 2^63
    assertFalse(EncodingMethods.encodeIntegerString(value, encodedVar));

    value = "-9223372036854775809";  // Long.MIN_VALUE - 1 == -2^63 - 1
    assertFalse(EncodingMethods.encodeIntegerString(value, encodedVar));

    // Test non-integers
    String[] nonIntegers = new String[]{
        "", "a", "-", "+", "-a", "+a", "--", "++",
        " 1", "- 1", "1 ", "01", "+1", "1u", "1U", "1l", "1L", "1ll", "1LL",
        "0.0", "-0"
    };
    for (String nonInt : nonIntegers) {
      assertFalse(EncodingMethods.encodeIntegerString(nonInt, encodedVar), "Should reject: " + nonInt);
    }
  }

  @Test
  void testEncodingFloatsOfficialCases() throws IOException {
    long[] encodedVar = new long[1];
    String value;
    String decodedValue;

    // Test basic conversions
    value = "0.0";
    assertTrue(EncodingMethods.encodeFloatString(value, encodedVar));
    decodedValue = EncodingMethods.decodeFloatVar(encodedVar[0]);
    assertEquals(value, decodedValue);

    value = "-1.0";
    assertTrue(EncodingMethods.encodeFloatString(value, encodedVar));
    decodedValue = EncodingMethods.decodeFloatVar(encodedVar[0]);
    assertEquals(value, decodedValue);

    value = "1.0";
    assertTrue(EncodingMethods.encodeFloatString(value, encodedVar));
    decodedValue = EncodingMethods.decodeFloatVar(encodedVar[0]);
    assertEquals(value, decodedValue);

    value = ".1";
    assertTrue(EncodingMethods.encodeFloatString(value, encodedVar));
    decodedValue = EncodingMethods.decodeFloatVar(encodedVar[0]);
    assertEquals(value, decodedValue);

    value = "-00.00";
    assertTrue(EncodingMethods.encodeFloatString(value, encodedVar));
    decodedValue = EncodingMethods.decodeFloatVar(encodedVar[0]);
    assertEquals(value, decodedValue);

    // Test edges of representable range (8-byte encoding)
    value = "-999999999999999.9";
    assertTrue(EncodingMethods.encodeFloatString(value, encodedVar));
    decodedValue = EncodingMethods.decodeFloatVar(encodedVar[0]);
    assertEquals(value, decodedValue);

    value = "999999999999999.9";
    assertTrue(EncodingMethods.encodeFloatString(value, encodedVar));
    decodedValue = EncodingMethods.decodeFloatVar(encodedVar[0]);
    assertEquals(value, decodedValue);

    value = "-.9999999999999999";
    assertTrue(EncodingMethods.encodeFloatString(value, encodedVar));
    decodedValue = EncodingMethods.decodeFloatVar(encodedVar[0]);
    assertEquals(value, decodedValue);

    value = ".9999999999999999";
    assertTrue(EncodingMethods.encodeFloatString(value, encodedVar));
    decodedValue = EncodingMethods.decodeFloatVar(encodedVar[0]);
    assertEquals(value, decodedValue);

    // Non-floats
    String[] nonFloats = new String[]{
        "", "a", "-", "+", "-a", "+a", "--", "++", ".", "1.",
        " 1.0", "1.0 ", "- 1.0", "+1.0", "1.0f", "1.0F", "1.0l", "1.0L", "1.0.0"
    };
    for (String nonFloat : nonFloats) {
      assertFalse(EncodingMethods.encodeFloatString(nonFloat, encodedVar), "Should reject non-float: " + nonFloat);
    }
  }

  @Test
  void testEncodingMessageOfficialCatch2Case() throws IOException {
    MessageEncoder messageEncoder = new MessageEncoder(
        BuiltInVariableHandlingRuleVersions.VariablesSchemaV2,
        BuiltInVariableHandlingRuleVersions.VariableEncodingMethodsV1
    );
    MessageDecoder messageDecoder = new MessageDecoder(
        BuiltInVariableHandlingRuleVersions.VariablesSchemaV2,
        BuiltInVariableHandlingRuleVersions.VariableEncodingMethodsV1
    );
    EncodedMessage encodedMessage = new EncodedMessage();

    String message = "here is a string with a small int 4938"
        + " and a medium int " + Integer.MAX_VALUE
        + " and a very large int " + Long.MAX_VALUE
        + " and a small float 0.1"
        + " and a medium float -25.519686"
        + " and a weird float -00.00"
        + " and a string with numbers bin/python2.7.3"
        + " and another string with numbers abc123";

    messageEncoder.encodeMessage(message, encodedMessage);

    // Verify 100% losslessness decode
    String decodedMessage = messageDecoder.decodeMessage(
        encodedMessage.getLogtype(),
        encodedMessage.getDictionaryVarsAsFlattenedByteArray(),
        encodedMessage.getEncodedVars()
    );
    assertEquals(message, decodedMessage);

    // Message with placeholder after variables
    String badMessage = " test var123 " + (char) VariablePlaceholder.Integer;
    assertThrows(IOException.class, () -> messageEncoder.encodeMessage(badMessage, encodedMessage));

    // Message with placeholder before variable
    String badMessage2 = badMessage + " var234";
    assertThrows(IOException.class, () -> messageEncoder.encodeMessage(badMessage2, encodedMessage));
  }
}
