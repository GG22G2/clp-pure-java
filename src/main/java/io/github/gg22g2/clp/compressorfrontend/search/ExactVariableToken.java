package io.github.gg22g2.clp.compressorfrontend.search;

import io.github.gg22g2.clp.compressorfrontend.EncodingMethods;
import io.github.gg22g2.clp.compressorfrontend.VariablePlaceholder;

import java.util.Objects;

public class ExactVariableToken extends QueryToken {
  private final long encodedValue;
  private final byte placeholder;

  public ExactVariableToken(String query, int beginPos, int endPos) {
    super(query, beginPos, endPos);
    String token = query.substring(beginPos, endPos);
    long[] encodedValBuf = new long[1];
    if (EncodingMethods.encodeFloatString(token, encodedValBuf)) {
      this.type = TokenType.FloatVariable;
      this.placeholder = VariablePlaceholder.Float;
      this.encodedValue = encodedValBuf[0];
    } else if (EncodingMethods.encodeIntegerString(token, encodedValBuf)) {
      this.type = TokenType.IntegerVariable;
      this.placeholder = VariablePlaceholder.Integer;
      this.encodedValue = encodedValBuf[0];
    } else {
      this.type = TokenType.DictionaryVariable;
      this.placeholder = VariablePlaceholder.Dictionary;
      this.encodedValue = 0L;
    }
  }

  public long getEncodedValue() {
    return encodedValue;
  }

  public byte getPlaceholder() {
    return placeholder;
  }

  public void addToLogtypeQuery(StringBuilder logtypeQuery) {
    logtypeQuery.append((char) placeholder);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;
    ExactVariableToken that = (ExactVariableToken) o;
    return encodedValue == that.encodedValue && placeholder == that.placeholder;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), encodedValue, placeholder);
  }
}
