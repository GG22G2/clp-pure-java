package io.github.gg22g2.clp.compressorfrontend;

import org.junit.jupiter.api.Test;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class FloatEncodingTest {

  @Test
  void testFloatEncodingAndDecoding() throws IOException {
    String[] validFloats = new String[]{
        "0.0",
        "1.1",
        "-1.1",
        "3.1415926535",
        "-0.001",
        "1.1000",          // Trailing zeros must be strictly preserved!
        "-1.100000",       // Negative with trailing zeros
        ".123456",         // No leading digit before decimal
        "-.123456",        // Negative without leading digit
        "123456789012345.6", // Max 16 digits
        "-123456789012345.6",
        "0.123456789012345",
        "-0.123456789012345",
        "123.4567890123456"
    };

    long[] encodedVar = new long[1];
    for (String original : validFloats) {
      boolean success = EncodingMethods.encodeFloatString(original, encodedVar);
      assertTrue(success, "Failed to encode valid float: " + original);

      String decoded = EncodingMethods.decodeFloatVar(encodedVar[0]);
      assertEquals(original, decoded, "Decoded float does not match original for: " + original);
    }
  }

  @Test
  void testInvalidFloats() {
    String[] invalidFloats = new String[]{
        "",
        "123",             // Integer, not float (no decimal point)
        "1.",              // No digits after decimal point
        ".",               // No digits
        "-.",              // Sign only with decimal
        "1..0",            // Double decimal
        "--1.0",           // Double negative
        "1.0a",            // Non-numeric character
        "abc.def",         // Alphabetic
        "1234567890123456.7", // 17 digits (> 16 digits)
        "1.2.3.4"
    };

    long[] encodedVar = new long[1];
    for (String invalid : invalidFloats) {
      boolean success = EncodingMethods.encodeFloatString(invalid, encodedVar);
      assertFalse(success, "Should fail for invalid float: " + invalid);
    }
  }

  @Test
  void testEncodeFloatPropertiesBitFormat() throws IOException {
    // Test encodeFloatProperties manually
    boolean isNegative = true;
    long digits = 12345;
    int numDigits = 5;
    int decimalPointPos = 2; // e.g., -123.45

    long encoded = EncodingMethods.encodeFloatProperties(isNegative, digits, numDigits, decimalPointPos);
    String decoded = EncodingMethods.decodeFloatVar(encoded);
    assertEquals("-123.45", decoded);
  }
}
