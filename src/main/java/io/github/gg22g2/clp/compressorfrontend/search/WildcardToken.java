package io.github.gg22g2.clp.compressorfrontend.search;

import io.github.gg22g2.clp.compressorfrontend.Parsing;
import io.github.gg22g2.clp.compressorfrontend.VariablePlaceholder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class WildcardToken extends QueryToken {
  private final boolean hasPrefixStarWildcard;
  private final boolean hasSuffixStarWildcard;
  private final List<TokenType> possibleVariableTypes = new ArrayList<>();
  private int currentInterpretationIdx = 0;

  public WildcardToken(String query, int beginPos, int endPos) {
    super(query, beginPos, endPos);
    this.hasPrefixStarWildcard = ('*' == query.charAt(beginPos));
    this.hasSuffixStarWildcard = ('*' == query.charAt(endPos - 1));

    String token = query.substring(beginPos, endPos);
    if (couldBeIntVar(token)) {
      possibleVariableTypes.add(TokenType.IntegerVariable);
    }
    if (couldBeFloatVar(token)) {
      possibleVariableTypes.add(TokenType.FloatVariable);
    }
    if (couldBeStaticText(query, beginPos, endPos)) {
      possibleVariableTypes.add(TokenType.StaticText);
    }
    possibleVariableTypes.add(TokenType.DictionaryVariable);
    this.currentInterpretationIdx = 0;
  }

  public WildcardToken(WildcardToken other) {
    super(other.query, other.beginPos, other.endPos);
    this.type = other.type;
    this.hasPrefixStarWildcard = other.hasPrefixStarWildcard;
    this.hasSuffixStarWildcard = other.hasSuffixStarWildcard;
    this.possibleVariableTypes.addAll(other.possibleVariableTypes);
    this.currentInterpretationIdx = other.currentInterpretationIdx;
  }

  public static boolean couldBeFloatVar(String token) {
    int numDecimals = 0;
    int numNegativeSigns = 0;
    int numDigits = 0;
    int len = token.length();
    for (int i = 0; i < len; ++i) {
      char c = token.charAt(i);
      if ('.' == c) {
        ++numDecimals;
        if (numDecimals > 1) {
          return false;
        }
      } else if ('-' == c) {
        ++numNegativeSigns;
        if (numNegativeSigns > 1) {
          return false;
        }
      } else if ('0' <= c && c <= '9') {
        ++numDigits;
        if (numDigits > 16) {
          return false;
        }
      } else if ('*' != c && '?' != c) {
        return false;
      }
    }
    return true;
  }

  public static boolean couldBeIntVar(String token) {
    int numNegativeSigns = 0;
    int numDigits = 0;
    int len = token.length();
    for (int i = 0; i < len; ++i) {
      char c = token.charAt(i);
      if ('-' == c) {
        ++numNegativeSigns;
        if (numNegativeSigns > 1) {
          return false;
        }
      } else if ('0' <= c && c <= '9') {
        ++numDigits;
        if (numDigits > 19) {
          return false;
        }
      } else if ('*' != c && '?' != c) {
        return false;
      }
    }
    return true;
  }

  public static boolean couldBeStaticText(String query, int beginPos, int endPos) {
    boolean isEscaped = false;
    boolean containsAlphabet = false;
    for (int i = beginPos; i < endPos; ++i) {
      char c = query.charAt(i);
      if (isEscaped) {
        isEscaped = false;
      } else if ('\\' == c) {
        isEscaped = true;
      } else if (Parsing.isDecimalDigit(c)) {
        return false;
      } else if (Parsing.isAlphabet(c)) {
        containsAlphabet = true;
      }
    }

    if (beginPos > 0 && '=' == query.charAt(beginPos - 1)) {
      if ('?' == query.charAt(beginPos) && containsAlphabet) {
        return false;
      }
    }

    return true;
  }

  public boolean addToLogtypeQuery(StringBuilder logtypeQuery) {
    TokenType currentInterpretation = possibleVariableTypes.get(currentInterpretationIdx);
    if (TokenType.StaticText == currentInterpretation) {
      if (hasSuffixStarWildcard) {
        logtypeQuery.append(query, beginPos, endPos - 1);
      } else {
        logtypeQuery.append(query, beginPos, endPos);
      }
      return false;
    } else {
      if (hasPrefixStarWildcard) {
        logtypeQuery.append(WildcardType.ZeroOrMoreChars);
      }
      switch (currentInterpretation) {
        case DictionaryVariable:
          logtypeQuery.append((char) VariablePlaceholder.Dictionary);
          break;
        case FloatVariable:
          logtypeQuery.append((char) VariablePlaceholder.Float);
          break;
        case IntegerVariable:
          logtypeQuery.append((char) VariablePlaceholder.Integer);
          break;
        default:
          throw new UnsupportedOperationException("Unexpected token type: " + currentInterpretation);
      }
      return true;
    }
  }

  public boolean nextInterpretation() {
    ++currentInterpretationIdx;
    if (currentInterpretationIdx < possibleVariableTypes.size()) {
      return true;
    } else {
      currentInterpretationIdx = 0;
      return false;
    }
  }

  public boolean hasSuffixStarWildcard() {
    return hasSuffixStarWildcard;
  }

  public boolean hasPrefixStarWildcard() {
    return hasPrefixStarWildcard;
  }

  public TokenType getCurrentInterpretation() {
    return possibleVariableTypes.get(currentInterpretationIdx);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;
    WildcardToken that = (WildcardToken) o;
    return hasPrefixStarWildcard == that.hasPrefixStarWildcard
        && hasSuffixStarWildcard == that.hasSuffixStarWildcard
        && currentInterpretationIdx == that.currentInterpretationIdx
        && Objects.equals(possibleVariableTypes, that.possibleVariableTypes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), hasPrefixStarWildcard, hasSuffixStarWildcard,
        possibleVariableTypes, currentInterpretationIdx);
  }
}
