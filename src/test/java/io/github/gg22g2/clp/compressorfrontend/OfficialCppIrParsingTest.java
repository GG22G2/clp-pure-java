package io.github.gg22g2.clp.compressorfrontend;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 1:1 verbatim port of official CLP C++ core Catch2 test: test-ir_parsing.cpp
 */
public class OfficialCppIrParsingTest {

  @Test
  void testGetBoundsOfNextVar() {
    String str;
    int[] bounds = new int[]{0, 0};

    // Corner cases
    // Empty string
    str = "";
    bounds[0] = 0;
    bounds[1] = 0;
    assertFalse(Parsing.getBoundsOfNextVar(str, bounds));

    // Non-variables
    str = "/";
    bounds[0] = 0;
    bounds[1] = 0;
    assertFalse(Parsing.getBoundsOfNextVar(str, bounds));
    assertEquals(str.length(), bounds[0]);

    str = "xyz";
    bounds[0] = 0;
    bounds[1] = 0;
    assertFalse(Parsing.getBoundsOfNextVar(str, bounds));
    assertEquals(str.length(), bounds[0]);

    str = "=";
    bounds[0] = 0;
    bounds[1] = 0;
    assertFalse(Parsing.getBoundsOfNextVar(str, bounds));
    assertEquals(str.length(), bounds[0]);

    // Variables
    str = "~=x!abc123;1.2%x:+394/-";
    bounds[0] = 0;
    bounds[1] = 0;

    assertTrue(Parsing.getBoundsOfNextVar(str, bounds));
    assertEquals("x", str.substring(bounds[0], bounds[1]));

    assertTrue(Parsing.getBoundsOfNextVar(str, bounds));
    assertEquals("abc123", str.substring(bounds[0], bounds[1]));

    assertTrue(Parsing.getBoundsOfNextVar(str, bounds));
    assertEquals("1.2", str.substring(bounds[0], bounds[1]));

    assertTrue(Parsing.getBoundsOfNextVar(str, bounds));
    assertEquals("+394", str.substring(bounds[0], bounds[1]));

    assertFalse(Parsing.getBoundsOfNextVar(str, bounds));

    // Hex string
    str = " ad ff 95 24 0d ff ";
    bounds[0] = 0;
    bounds[1] = 0;

    assertTrue(Parsing.getBoundsOfNextVar(str, bounds));
    assertEquals("ad", str.substring(bounds[0], bounds[1]));

    assertTrue(Parsing.getBoundsOfNextVar(str, bounds));
    assertEquals("ff", str.substring(bounds[0], bounds[1]));

    assertTrue(Parsing.getBoundsOfNextVar(str, bounds));
    assertEquals("95", str.substring(bounds[0], bounds[1]));

    assertTrue(Parsing.getBoundsOfNextVar(str, bounds));
    assertEquals("24", str.substring(bounds[0], bounds[1]));

    assertTrue(Parsing.getBoundsOfNextVar(str, bounds));
    assertEquals("0d", str.substring(bounds[0], bounds[1]));

    assertTrue(Parsing.getBoundsOfNextVar(str, bounds));
    assertEquals("ff", str.substring(bounds[0], bounds[1]));

    assertFalse(Parsing.getBoundsOfNextVar(str, bounds));
    assertEquals(str.length(), bounds[0]);

    // String containing variable placeholder
    str = " text " + (char) VariablePlaceholder.Integer + " var123 ";
    bounds[0] = 0;
    bounds[1] = 0;

    assertTrue(Parsing.getBoundsOfNextVar(str, bounds));
    assertEquals("var123", str.substring(bounds[0], bounds[1]));
  }
}
