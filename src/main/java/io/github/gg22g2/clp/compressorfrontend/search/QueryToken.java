package io.github.gg22g2.clp.compressorfrontend.search;

import java.util.Objects;

public class QueryToken {
  protected final String query;
  protected final int beginPos;
  protected final int endPos;
  protected TokenType type;

  public QueryToken(String query, int beginPos, int endPos) {
    this.query = query;
    this.beginPos = beginPos;
    this.endPos = endPos;
    this.type = TokenType.StaticText;
  }

  public int getBeginPos() {
    return beginPos;
  }

  public int getEndPos() {
    return endPos;
  }

  public TokenType getType() {
    return type;
  }

  public String getValue() {
    return query.substring(beginPos, endPos);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    QueryToken that = (QueryToken) o;
    return beginPos == that.beginPos && endPos == that.endPos && type == that.type && Objects.equals(query, that.query);
  }

  @Override
  public int hashCode() {
    return Objects.hash(query, beginPos, endPos, type);
  }
}
