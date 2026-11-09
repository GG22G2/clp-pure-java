package io.github.gg22g2.clp.compressorfrontend;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class ParsingAndDelimiterTest {

  @Test
  void testDelimiterRules() {
    // Non-delimiters: '+', '-', '.', '0'-'9', 'A'-'Z', '\\', '_', 'a'-'z'
    assertFalse(Parsing.isDelim('+'));
    assertFalse(Parsing.isDelim('-'));
    assertFalse(Parsing.isDelim('.'));
    assertFalse(Parsing.isDelim('0'));
    assertFalse(Parsing.isDelim('9'));
    assertFalse(Parsing.isDelim('A'));
    assertFalse(Parsing.isDelim('Z'));
    assertFalse(Parsing.isDelim('\\'));
    assertFalse(Parsing.isDelim('_'));
    assertFalse(Parsing.isDelim('a'));
    assertFalse(Parsing.isDelim('z'));

    // Delimiters
    assertTrue(Parsing.isDelim(' '));
    assertTrue(Parsing.isDelim(':'));
    assertTrue(Parsing.isDelim(','));
    assertTrue(Parsing.isDelim('='));
    assertTrue(Parsing.isDelim('/'));
    assertTrue(Parsing.isDelim('['));
    assertTrue(Parsing.isDelim(']'));
    assertTrue(Parsing.isDelim('{'));
    assertTrue(Parsing.isDelim('}'));
    assertTrue(Parsing.isDelim('('));
    assertTrue(Parsing.isDelim(')'));
    assertTrue(Parsing.isDelim('\t'));
    assertTrue(Parsing.isDelim('\n'));
  }

  @Test
  void testHexDetection() {
    assertTrue(Parsing.couldBeMultiDigitHexValue("00"));
    assertTrue(Parsing.couldBeMultiDigitHexValue("deadbeef"));
    assertTrue(Parsing.couldBeMultiDigitHexValue("1234abcd"));
    assertTrue(Parsing.couldBeMultiDigitHexValue("CAFEBABE"));

    assertFalse(Parsing.couldBeMultiDigitHexValue("a"));      // length < 2
    assertFalse(Parsing.couldBeMultiDigitHexValue("123g"));   // 'g' is not hex
    assertFalse(Parsing.couldBeMultiDigitHexValue("test"));   // 't', 's' not hex
  }

  @Test
  void testIsVar() {
    assertTrue(Parsing.isVar("123"));
    assertTrue(Parsing.isVar("user1"));
    assertTrue(Parsing.isVar("deadbeef"));
    assertTrue(Parsing.isVar("1.2.3.4"));

    assertFalse(Parsing.isVar("user"));    // No digit, not hex (length < 2 or non-hex 'u', 's')
    assertFalse(Parsing.isVar("INFO"));
  }

  @Test
  void testVariablePlaceholderDetectionInMessage() {
    MessageEncoder messageEncoder = new MessageEncoder(
        BuiltInVariableHandlingRuleVersions.VariablesSchemaV2,
        BuiltInVariableHandlingRuleVersions.VariableEncodingMethodsV1
    );
    EncodedMessage encodedMessage = new EncodedMessage();

    // Messages containing raw unescaped variable placeholders must fail with IOException
    String messageWithPlaceholder = "Invalid message \u0011 placeholder";
    assertThrows(IOException.class, () -> messageEncoder.encodeMessage(messageWithPlaceholder, encodedMessage));

    String messageWithDictPlaceholder = "Invalid message \u0012 placeholder";
    assertThrows(IOException.class, () -> messageEncoder.encodeMessage(messageWithDictPlaceholder, encodedMessage));

    String messageWithFloatPlaceholder = "Invalid message \u0013 placeholder";
    assertThrows(IOException.class, () -> messageEncoder.encodeMessage(messageWithFloatPlaceholder, encodedMessage));
  }
}
