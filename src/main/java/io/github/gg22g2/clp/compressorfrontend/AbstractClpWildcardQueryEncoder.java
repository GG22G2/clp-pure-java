package io.github.gg22g2.clp.compressorfrontend;

import java.util.Objects;
import org.jetbrains.annotations.NotNull;

/**
 * 100% Pure Java implementation of AbstractClpWildcardQueryEncoder.
 */
public abstract class AbstractClpWildcardQueryEncoder {
  public AbstractClpWildcardQueryEncoder (
      @NotNull String variablesSchemaVersion,
      @NotNull String variableEncodingMethodsVersion
  ) throws UnsupportedOperationException {
    Objects.requireNonNull(variablesSchemaVersion);
    Objects.requireNonNull(variableEncodingMethodsVersion);
    MessageEncoder.validateVariableHandlingRuleVersions(variablesSchemaVersion, variableEncodingMethodsVersion);
  }
}
