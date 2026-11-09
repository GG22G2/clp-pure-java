package io.github.gg22g2.clp.compressorfrontend;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

/**
 * 100% Pure Java implementation of MessageEncoder.
 * Replaces native library FFI with direct pure Java encoding methods.
 */
public class MessageEncoder {
  public MessageEncoder(
      @NotNull String variablesSchemaVersion,
      @NotNull String variableEncodingMethodsVersion
  ) throws UnsupportedOperationException {
    Objects.requireNonNull(variablesSchemaVersion);
    Objects.requireNonNull(variableEncodingMethodsVersion);
    validateVariableHandlingRuleVersions(variablesSchemaVersion, variableEncodingMethodsVersion);
  }

  public static void validateVariableHandlingRuleVersions(
      String variablesSchemaVersion,
      String variableEncodingMethodsVersion
  ) throws UnsupportedOperationException {
    if (!BuiltInVariableHandlingRuleVersions.VariablesSchemaV2.equals(variablesSchemaVersion)) {
      throw new UnsupportedOperationException("Unsupported variables schema version: " + variablesSchemaVersion);
    }
    if (!BuiltInVariableHandlingRuleVersions.VariableEncodingMethodsV1.equals(variableEncodingMethodsVersion)) {
      throw new UnsupportedOperationException("Unsupported variable encoding methods version: " + variableEncodingMethodsVersion);
    }
  }

  /**
   * Encodes the given log message.
   *
   * @param message
   * @param encodedMessage
   * @throws IOException if encoding fails (e.g. contains unescaped variable placeholders)
   */
  public void encodeMessage(String message, EncodedMessage encodedMessage) throws IOException {
    byte[] messageAsBytes = message.getBytes(StandardCharsets.UTF_8);
    encodedMessage.setMessage(messageAsBytes);
    if (!EncodingMethods.encodeMessage(messageAsBytes, encodedMessage)) {
      throw new IOException("message contains variable placeholders");
    }
  }
}
