package io.github.gg22g2.clp.compressorfrontend.search;

import io.github.gg22g2.clp.compressorfrontend.Parsing;
import io.github.gg22g2.clp.compressorfrontend.StringUtils;

import java.util.ArrayList;
import java.util.List;

public final class QueryMethods {
  private QueryMethods() {}

  public static void generateSubqueries(String wildcardQuery, List<Subquery> subqueries) {
    if (wildcardQuery == null || wildcardQuery.isEmpty()) {
      throw new IllegalArgumentException("wildcard_query cannot be empty");
    }

    List<QueryToken> tokens = new ArrayList<>();
    List<Integer> compositeWildcardTokenIndexes = new ArrayList<>();
    tokenizeQuery(wildcardQuery, tokens, compositeWildcardTokenIndexes);

    boolean allInterpretationsComplete = false;
    StringBuilder logtypeQuery = new StringBuilder();
    List<QueryToken> queryVars = new ArrayList<>();

    while (!allInterpretationsComplete) {
      logtypeQuery.setLength(0);
      queryVars.clear();
      int constantBeginPos = 0;

      for (QueryToken token : tokens) {
        int beginPos = token.getBeginPos();
        logtypeQuery.append(wildcardQuery, constantBeginPos, beginPos);

        if (token instanceof ExactVariableToken) {
          ((ExactVariableToken) token).addToLogtypeQuery(logtypeQuery);
          queryVars.add(token);
        } else if (token instanceof CompositeWildcardToken) {
          ((CompositeWildcardToken) token).addToQuery(logtypeQuery, queryVars);
        }

        constantBeginPos = token.getEndPos();
      }
      logtypeQuery.append(wildcardQuery, constantBeginPos, wildcardQuery.length());

      String currentLogtypeQuery = logtypeQuery.toString();
      List<QueryToken> currentQueryVars = new ArrayList<>(queryVars);

      boolean subQueryExists = false;
      for (Subquery subquery : subqueries) {
        if (subquery.matches(currentLogtypeQuery, currentQueryVars)) {
          subQueryExists = true;
          break;
        }
      }
      if (!subQueryExists) {
        subqueries.add(new Subquery(currentLogtypeQuery, currentQueryVars));
      }

      allInterpretationsComplete = true;
      for (int i : compositeWildcardTokenIndexes) {
        CompositeWildcardToken w = (CompositeWildcardToken) tokens.get(i);
        if (w.generateNextInterpretation()) {
          allInterpretationsComplete = false;
          break;
        }
      }
    }
  }

  private static void tokenizeQuery(
      String wildcardQuery,
      List<QueryToken> tokens,
      List<Integer> compositeWildcardTokenIndexes
  ) {
    int endPos = 0;
    int len = wildcardQuery.length();
    int[] posHolder = new int[]{0};
    boolean[] wildcardHolder = new boolean[]{false};

    while (true) {
      int beginPos = endPos;
      posHolder[0] = beginPos;
      wildcardHolder[0] = false;

      if (!findWildcardOrNonDelimiter(wildcardQuery, posHolder, wildcardHolder)) {
        break;
      }
      beginPos = posHolder[0];

      boolean[] flags = new boolean[]{false, false, wildcardHolder[0]}; // [alphabet, decimalDigit, wildcard]
      endPos = beginPos;
      posHolder[0] = endPos;
      findDelimiter(wildcardQuery, posHolder, flags);
      endPos = posHolder[0];

      boolean containsAlphabet = flags[0];
      boolean containsDecimalDigit = flags[1];
      boolean containsWildcard = flags[2];

      if (containsWildcard) {
        if (endPos - beginPos > 1) {
          tokens.add(new CompositeWildcardToken(wildcardQuery, beginPos, endPos));
          compositeWildcardTokenIndexes.add(tokens.size() - 1);
        }
      } else {
        String variable = wildcardQuery.substring(beginPos, endPos);
        if (containsDecimalDigit
            || (beginPos > 0 && '=' == wildcardQuery.charAt(beginPos - 1) && containsAlphabet)
            || Parsing.couldBeMultiDigitHexValue(variable)) {
          tokens.add(new ExactVariableToken(wildcardQuery, beginPos, endPos));
        }
      }
    }
  }

  private static void findDelimiter(
      String value,
      int[] posHolder,
      boolean[] flags
  ) {
    int pos = posHolder[0];
    int len = value.length();
    boolean isEscaped = false;

    for (; pos < len; ++pos) {
      char c = value.charAt(pos);

      if (isEscaped) {
        isEscaped = false;
        if (Parsing.isDelim(c)) {
          --pos;
          posHolder[0] = pos;
          return;
        }
      } else if ('\\' == c) {
        isEscaped = true;
      } else {
        if (StringUtils.isWildcard(c)) {
          flags[2] = true; // contains_wildcard
        } else if (Parsing.isDelim(c)) {
          posHolder[0] = pos;
          return;
        }
      }

      if (Parsing.isDecimalDigit(c)) {
        flags[1] = true; // contains_decimal_digit
      } else if (Parsing.isAlphabet(c)) {
        flags[0] = true; // contains_alphabet
      }
    }

    posHolder[0] = pos;
  }

  private static boolean findWildcardOrNonDelimiter(
      String value,
      int[] posHolder,
      boolean[] wildcardHolder
  ) {
    int pos = posHolder[0];
    int len = value.length();
    boolean isEscaped = false;
    wildcardHolder[0] = false;

    for (; pos < len; ++pos) {
      char c = value.charAt(pos);

      if (isEscaped) {
        isEscaped = false;
        if (!Parsing.isDelim(c)) {
          --pos;
          posHolder[0] = pos;
          return true;
        }
      } else if ('\\' == c) {
        isEscaped = true;
      } else {
        if (StringUtils.isWildcard(c)) {
          wildcardHolder[0] = true;
          posHolder[0] = pos;
          return true;
        } else if (!Parsing.isDelim(c)) {
          posHolder[0] = pos;
          return true;
        }
      }
    }

    posHolder[0] = pos;
    return false;
  }
}
