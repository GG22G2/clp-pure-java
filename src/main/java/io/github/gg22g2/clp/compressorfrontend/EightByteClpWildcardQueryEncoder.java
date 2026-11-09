package io.github.gg22g2.clp.compressorfrontend;

import io.github.gg22g2.clp.compressorfrontend.search.ExactVariableToken;
import io.github.gg22g2.clp.compressorfrontend.search.QueryMethods;
import io.github.gg22g2.clp.compressorfrontend.search.QueryToken;
import io.github.gg22g2.clp.compressorfrontend.search.Subquery;
import io.github.gg22g2.clp.compressorfrontend.search.WildcardToken;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 100% Pure Java implementation of EightByteClpWildcardQueryEncoder.
 */
public class EightByteClpWildcardQueryEncoder extends AbstractClpWildcardQueryEncoder {
  public EightByteClpWildcardQueryEncoder (
      @NotNull String variablesSchemaVersion,
      @NotNull String variableEncodingMethodsVersion
  ) throws UnsupportedOperationException {
    super(variablesSchemaVersion, variableEncodingMethodsVersion);
  }

  /**
   * Encodes the given wildcard query into an array of subqueries that can be
   * run on encoded messages. The array of subqueries is a logical disjunction
   * (i.e., all subqueries OR-ed together).
   *
   * @param wildcardQuery The wildcard query using two wildcards: '?' and '*'
   * @return The subqueries
   */
  public EightByteClpEncodedSubquery[] encode (@NotNull String wildcardQuery) throws IllegalArgumentException {
    Objects.requireNonNull(wildcardQuery);
    String cleanWildcardQuery = StringUtils.cleanUpWildcardSearchString(wildcardQuery);
    byte[] cleanWildcardQueryBytes = cleanWildcardQuery.getBytes(StandardCharsets.ISO_8859_1);

    List<Subquery> subqueries = new ArrayList<>();
    QueryMethods.generateSubqueries(cleanWildcardQuery, subqueries);

    EightByteClpEncodedSubquery[] result = new EightByteClpEncodedSubquery[subqueries.size()];
    List<Integer> dictVarBounds = new ArrayList<>();
    List<Long> encodedVars = new ArrayList<>();
    List<Byte> wildcardVarPlaceholders = new ArrayList<>();
    List<Integer> wildcardVarBounds = new ArrayList<>();

    for (int i = 0; i < subqueries.size(); ++i) {
      Subquery subquery = subqueries.get(i);
      byte[] logtypeQuery = subquery.getLogtypeQuery().getBytes(StandardCharsets.ISO_8859_1);

      dictVarBounds.clear();
      encodedVars.clear();
      wildcardVarPlaceholders.clear();
      wildcardVarBounds.clear();

      for (QueryToken queryVar : subquery.getQueryVars()) {
        if (queryVar instanceof ExactVariableToken) {
          ExactVariableToken exactVar = (ExactVariableToken) queryVar;
          if (VariablePlaceholder.Dictionary == exactVar.getPlaceholder()) {
            dictVarBounds.add(exactVar.getBeginPos());
            dictVarBounds.add(exactVar.getEndPos());
          } else {
            encodedVars.add(exactVar.getEncodedValue());
          }
        } else if (queryVar instanceof WildcardToken) {
          WildcardToken wildcardVar = (WildcardToken) queryVar;
          switch (wildcardVar.getCurrentInterpretation()) {
            case IntegerVariable:
              wildcardVarPlaceholders.add(VariablePlaceholder.Integer);
              break;
            case FloatVariable:
              wildcardVarPlaceholders.add(VariablePlaceholder.Float);
              break;
            case DictionaryVariable:
              wildcardVarPlaceholders.add(VariablePlaceholder.Dictionary);
              break;
            default:
              throw new IllegalStateException("Unexpected interpretation: " + wildcardVar.getCurrentInterpretation());
          }
          wildcardVarBounds.add(wildcardVar.getBeginPos());
          wildcardVarBounds.add(wildcardVar.getEndPos());
        }
      }

      int[] dictVarBoundsArr = new int[dictVarBounds.size()];
      for (int j = 0; j < dictVarBoundsArr.length; ++j) {
        dictVarBoundsArr[j] = dictVarBounds.get(j);
      }

      long[] encodedVarsArr = new long[encodedVars.size()];
      for (int j = 0; j < encodedVarsArr.length; ++j) {
        encodedVarsArr[j] = encodedVars.get(j);
      }

      byte[] wildcardVarPlaceholdersArr = new byte[wildcardVarPlaceholders.size()];
      for (int j = 0; j < wildcardVarPlaceholdersArr.length; ++j) {
        wildcardVarPlaceholdersArr[j] = wildcardVarPlaceholders.get(j);
      }

      int[] wildcardVarBoundsArr = new int[wildcardVarBounds.size()];
      for (int j = 0; j < wildcardVarBoundsArr.length; ++j) {
        wildcardVarBoundsArr[j] = wildcardVarBounds.get(j);
      }

      result[i] = new EightByteClpEncodedSubquery(
          cleanWildcardQueryBytes,
          logtypeQuery,
          subquery.logtypeQueryContainsWildcards(),
          dictVarBoundsArr,
          encodedVarsArr,
          wildcardVarPlaceholdersArr,
          wildcardVarBoundsArr
      );
    }

    return result;
  }
}
