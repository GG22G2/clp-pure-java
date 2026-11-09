package io.github.gg22g2.clp.compressorfrontend.search;

import java.util.List;
import java.util.Objects;

public class Subquery {
  private final String logtypeQuery;
  private boolean logtypeQueryContainsWildcards;
  private final List<QueryToken> queryVars;

  public Subquery(String logtypeQuery, List<QueryToken> queryVars) {
    this.logtypeQuery = logtypeQuery;
    this.queryVars = queryVars;
    this.logtypeQueryContainsWildcards = false;

    boolean isEscaped = false;
    int len = logtypeQuery.length();
    for (int i = 0; i < len; ++i) {
      char c = logtypeQuery.charAt(i);
      if (isEscaped) {
        isEscaped = false;
      } else if ('\\' == c) {
        isEscaped = true;
      } else if (WildcardType.ZeroOrMoreChars == c || WildcardType.AnyChar == c) {
        this.logtypeQueryContainsWildcards = true;
        break;
      }
    }
  }

  public String getLogtypeQuery() {
    return logtypeQuery;
  }

  public boolean logtypeQueryContainsWildcards() {
    return logtypeQueryContainsWildcards;
  }

  public List<QueryToken> getQueryVars() {
    return queryVars;
  }

  public boolean matches(String otherLogtypeQuery, List<QueryToken> otherQueryVars) {
    return Objects.equals(this.logtypeQuery, otherLogtypeQuery) && Objects.equals(this.queryVars, otherQueryVars);
  }
}
