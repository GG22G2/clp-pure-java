package io.github.gg22g2.clp.compressorfrontend;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class IntegerEncodingTest {

  @Test
  void testValidIntegers() {
    String[] validInts = new String[]{
        "0",
        "1",
        "-1",
        "42",
        "-42",
        "123456789",
        "-987654321",
        String.valueOf(Long.MAX_VALUE),
        String.valueOf(Long.MIN_VALUE)
    };

    long[] encodedVar = new long[1];
    for (String original : validInts) {
      boolean success = EncodingMethods.encodeIntegerString(original, encodedVar);
      assertTrue(success, "Failed to encode integer: " + original);

      String decoded = EncodingMethods.decodeIntegerVar(encodedVar[0]);
      assertEquals(original, decoded, "Mismatch for integer: " + original);
    }
  }

  @Test
  void testInvalidIntegers() {
    String[] invalidInts = new String[]{
        "",
        "-0",              // -0 is invalid in CLP integer encoding
        "0123",            // Leading zero is treated as dictionary var, not encoded int
        "-0123",           // Negative with leading zero
        "+123",            // Plus sign is not allowed in pure integer encoding
        "123a",            // Non-digit suffix
        "a123",            // Non-digit prefix
        "123.45",          // Float, not integer
        "99999999999999999999999999999999" // Overflow
    };

    long[] encodedVar = new long[1];
    for (String invalid : invalidInts) {
      boolean success = EncodingMethods.encodeIntegerString(invalid, encodedVar);
      assertFalse(success, "Should fail for invalid integer: " + invalid);
    }
  }
}
