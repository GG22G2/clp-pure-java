package io.github.gg22g2.clp.compressorfrontend;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

/**
 * 100% Pure Java implementation of MessageDecoder.
 * Replaces native library FFI with direct pure Java decoding methods.
 */
public class MessageDecoder {
  public MessageDecoder(
      @NotNull String variablesSchemaVersion,
      @NotNull String variableEncodingMethodsVersion
  ) throws UnsupportedOperationException {
    Objects.requireNonNull(variablesSchemaVersion);
    Objects.requireNonNull(variableEncodingMethodsVersion);
    MessageEncoder.validateVariableHandlingRuleVersions(variablesSchemaVersion, variableEncodingMethodsVersion);
  }

  /**
   * Decodes the message with the given logtype and variables
   * @param logtype
   * @param dictionaryVars
   * @param encodedVars
   * @return The decoded message
   * @throws IOException if decoding fails
   */
  public String decodeMessage(
      @NotNull String logtype,
      String[] dictionaryVars,
      long[] encodedVars
  ) throws IOException {
    byte[] message = decodeMessageAsBytes(logtype, dictionaryVars, encodedVars);
    return new String(message, StandardCharsets.UTF_8);
  }

  /**
   * Decodes the message with the given logtype and variables
   * @param logtype
   * @param dictionaryVars
   * @param encodedVars
   * @return The decoded message
   * @throws IOException if decoding fails
   */
  public String decodeMessage(
      byte @NotNull [] logtype,
      FlattenedByteArray dictionaryVars,
      long[] encodedVars
  ) throws IOException {
    return new String(
        decodeMessageAsBytes(logtype, dictionaryVars, encodedVars),
        StandardCharsets.UTF_8
    );
  }

  /**
   * Decodes the message with the given logtype and variables
   * @param logtype
   * @param dictionaryVars
   * @param encodedVars
   * @return The decoded message
   * @throws IOException if decoding fails
   */
  public byte[] decodeMessageAsBytes(
      byte @NotNull [] logtype,
      FlattenedByteArray dictionaryVars,
      long[] encodedVars
  ) throws IOException {
    Objects.requireNonNull(logtype);
    if (null == dictionaryVars) {
      return decodeMessagePureJava(
          logtype,
          logtype.length,
          null,
          0,
          null,
          0,
          encodedVars,
          null == encodedVars ? 0 : encodedVars.length
      );
    } else {
      return decodeMessagePureJava(
          logtype,
          logtype.length,
          dictionaryVars.getFlattenedElems(),
          dictionaryVars.getFlattenedElems().length,
          dictionaryVars.getElemEndOffsets(),
          dictionaryVars.getElemEndOffsets().length,
          encodedVars,
          null == encodedVars ? 0 : encodedVars.length
      );
    }
  }

  private byte[] decodeMessageAsBytes(
      @NotNull String logtype,
      String[] dictionaryVars,
      long[] encodedVars
  ) throws IOException {
    Objects.requireNonNull(logtype);
    return decodeMessageAsBytes(
        logtype.getBytes(StandardCharsets.ISO_8859_1),
        null == dictionaryVars ? null : FlattenedByteArrayFactory.fromStrings(dictionaryVars),
        encodedVars
    );
  }

  private byte[] decodeMessagePureJava(
      byte[] logtype,
      int logtypeLen,
      byte[] allDictionaryVars,
      int allDictionaryVarsLen,
      int[] dictionaryVarEndOffsets,
      int dictionaryVarEndOffsetsLen,
      long[] encodedVars,
      int encodedVarsLen
  ) throws IOException {
    return EncodingMethods.decodeMessage(
        logtype,
        logtypeLen,
        allDictionaryVars,
        allDictionaryVarsLen,
        dictionaryVarEndOffsets,
        dictionaryVarEndOffsetsLen,
        encodedVars,
        encodedVarsLen
    );
  }

  /**
   * Checks whether any encoded integer variable matches the given wildcard query
   * @param wildcardQuery
   * @param logtype
   * @param encodedVars
   * @return true if a match was found, false otherwise
   */
  public boolean wildcardQueryMatchesAnyIntVar(
      @NotNull String wildcardQuery,
      @NotNull String logtype,
      long[] encodedVars
  ) throws IOException {
    Objects.requireNonNull(wildcardQuery);
    Objects.requireNonNull(logtype);
    return EncodingMethods.wildcardQueryMatchesAnyIntVar(
        wildcardQuery,
        logtype,
        encodedVars,
        null == encodedVars ? 0 : encodedVars.length
    );
  }

  /**
   * Checks whether any encoded float variable matches the given wildcard query
   * @param wildcardQuery
   * @param logtype
   * @param encodedVars
   * @return true if a match was found, false otherwise
   */
  public boolean wildcardQueryMatchesAnyFloatVar(
      @NotNull String wildcardQuery,
      @NotNull String logtype,
      long[] encodedVars
  ) throws IOException {
    Objects.requireNonNull(wildcardQuery);
    Objects.requireNonNull(logtype);
    return EncodingMethods.wildcardQueryMatchesAnyFloatVar(
        wildcardQuery,
        logtype,
        encodedVars,
        null == encodedVars ? 0 : encodedVars.length
    );
  }

  /**
   * For each log message in a batch of messages, checks whether the given
   * wildcard queries for variables match the message's encoded variables.
   *
   * @param logtypes
   * @param encodedVarArrays
   * @param wildcardVarPlaceholders
   * @param serializedVarWildcardQueries
   * @param varWildcardQueryEndIndexes
   * @param matchResults
   */
  public void batchEncodedVarsWildcardMatch(
      @NotNull byte[][] logtypes,
      @NotNull long[][] encodedVarArrays,
      @NotNull byte[] wildcardVarPlaceholders,
      @NotNull byte[] serializedVarWildcardQueries,
      @NotNull int[] varWildcardQueryEndIndexes,
      @NotNull int[] matchResults
  ) throws IOException {
    Objects.requireNonNull(logtypes);
    Objects.requireNonNull(encodedVarArrays);
    Objects.requireNonNull(wildcardVarPlaceholders);
    Objects.requireNonNull(serializedVarWildcardQueries);
    Objects.requireNonNull(varWildcardQueryEndIndexes);
    Objects.requireNonNull(matchResults);

    // Validate the array lengths
    if (logtypes.length != matchResults.length) {
      throw new IllegalArgumentException("Number of logtypes given doesn't match size of output array.");
    }
    if (encodedVarArrays.length != matchResults.length) {
      throw new IllegalArgumentException("Number of encoded variable arrays given doesn't match size of output array.");
    }

    // Deserialize variable queries
    int numQueries = varWildcardQueryEndIndexes.length;
    String[] varWildcardQueries = new String[numQueries];
    int queryBeginIdx = 0;
    for (int i = 0; i < numQueries; ++i) {
      int endIdx = varWildcardQueryEndIndexes[i];
      varWildcardQueries[i] = new String(
          serializedVarWildcardQueries, queryBeginIdx, endIdx - queryBeginIdx, StandardCharsets.ISO_8859_1
      );
      queryBeginIdx = endIdx;
    }

    for (int i = 0; i < logtypes.length; ++i) {
      long[] encodedVars = encodedVarArrays[i];
      if (null == encodedVars) {
        matchResults[i] = 0;
        continue;
      }
      byte[] logtype = EmptyArrayUtils.getNonNullByteArray(logtypes[i]);
      boolean match = EncodingMethods.wildcardMatchEncodedVars(
          logtype,
          encodedVars,
          encodedVars.length,
          wildcardVarPlaceholders,
          varWildcardQueries
      );
      matchResults[i] = match ? 1 : 0;
    }
  }
}
