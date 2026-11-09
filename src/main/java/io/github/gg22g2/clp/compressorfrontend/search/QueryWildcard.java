package io.github.gg22g2.clp.compressorfrontend.search;

import java.util.ArrayList;
import java.util.List;

public class QueryWildcard {
  private final char type;
  private final int posInQuery;
  private final List<WildcardInterpretation> possibleInterpretations = new ArrayList<>();
  private int currentInterpretationIdx = 0;

  public QueryWildcard(char wildcard, int posInQuery, boolean isBoundaryWildcard) {
    if (WildcardType.AnyChar != wildcard && WildcardType.ZeroOrMoreChars != wildcard) {
      throw new IllegalArgumentException("QueryWildcard invalid wildcard character: " + wildcard);
    }
    this.type = wildcard;
    this.posInQuery = posInQuery;

    if (isBoundaryWildcard && WildcardType.ZeroOrMoreChars == type) {
      possibleInterpretations.add(WildcardInterpretation.ContainsDelimiters);
    } else {
      possibleInterpretations.add(WildcardInterpretation.ContainsDelimiters);
      possibleInterpretations.add(WildcardInterpretation.NoDelimiters);
    }
    this.currentInterpretationIdx = 0;
  }

  public boolean nextInterpretation() {
    ++currentInterpretationIdx;
    if (currentInterpretationIdx < possibleInterpretations.size()) {
      return true;
    } else {
      currentInterpretationIdx = 0;
      return false;
    }
  }

  public WildcardInterpretation getCurrentInterpretation() {
    return possibleInterpretations.get(currentInterpretationIdx);
  }

  public int getPosInQuery() {
    return posInQuery;
  }

  public char getType() {
    return type;
  }
}
