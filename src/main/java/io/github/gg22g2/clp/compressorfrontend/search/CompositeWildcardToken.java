package io.github.gg22g2.clp.compressorfrontend.search;

import io.github.gg22g2.clp.compressorfrontend.Parsing;
import io.github.gg22g2.clp.compressorfrontend.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class CompositeWildcardToken extends QueryToken {
  private final List<QueryWildcard> wildcards = new ArrayList<>();
  private final List<QueryToken> variables = new ArrayList<>();

  public CompositeWildcardToken(String query, int beginPos, int endPos) {
    super(query, beginPos, endPos);

    boolean isEscaped = false;
    for (int i = beginPos; i < endPos; ++i) {
      char c = query.charAt(i);
      if (isEscaped) {
        isEscaped = false;
      } else if ('\\' == c) {
        isEscaped = true;
      } else if (StringUtils.isWildcard(c)) {
        wildcards.add(new QueryWildcard(c, i, beginPos == i || endPos - 1 == i));
      }
    }
    if (wildcards.isEmpty()) {
      throw new IllegalArgumentException("CompositeWildcardToken contains no wildcards.");
    }

    tokenizeIntoWildcardVariableTokens();
  }

  public void addToQuery(StringBuilder logtypeQuery, List<QueryToken> variableTokens) {
    int constantBeginPos = beginPos;
    for (QueryToken var : variables) {
      int varBeginPos = var.getBeginPos();
      if (varBeginPos > constantBeginPos) {
        logtypeQuery.append(query, constantBeginPos, varBeginPos);
      }
      if (var instanceof ExactVariableToken) {
        ((ExactVariableToken) var).addToLogtypeQuery(logtypeQuery);
        variableTokens.add(var);
      } else if (var instanceof WildcardToken) {
        WildcardToken wildcardVar = (WildcardToken) var;
        if (wildcardVar.addToLogtypeQuery(logtypeQuery)) {
          variableTokens.add(new WildcardToken(wildcardVar));
        }
      }
      constantBeginPos = var.getEndPos();
    }

    if (!variables.isEmpty()) {
      QueryToken lastVar = variables.get(variables.size() - 1);
      if (lastVar instanceof WildcardToken) {
        if (((WildcardToken) lastVar).hasSuffixStarWildcard()) {
          logtypeQuery.append(WildcardType.ZeroOrMoreChars);
        }
      }
    }
    logtypeQuery.append(query, constantBeginPos, endPos);
  }

  public boolean generateNextInterpretation() {
    for (QueryToken v : variables) {
      if (v instanceof WildcardToken) {
        WildcardToken wildcardVar = (WildcardToken) v;
        if (wildcardVar.nextInterpretation()) {
          return true;
        }
      }
    }

    for (QueryWildcard w : wildcards) {
      if (w.nextInterpretation()) {
        tokenizeIntoWildcardVariableTokens();
        return true;
      }
    }

    return false;
  }

  private void tokenizeIntoWildcardVariableTokens() {
    variables.clear();

    QueryWildcard lastWildcard = null;
    boolean wildcardInVar = false;
    int varBeginPos;
    int varEndPos;

    for (QueryWildcard w : wildcards) {
      switch (w.getCurrentInterpretation()) {
        case NoDelimiters:
          wildcardInVar = true;
          break;
        case ContainsDelimiters: {
          int wildcardPos = w.getPosInQuery();
          if (wildcardPos == beginPos) {
            lastWildcard = w;
            continue;
          }

          if (null == lastWildcard) {
            varBeginPos = beginPos;
          } else {
            if (WildcardType.ZeroOrMoreChars == lastWildcard.getType()) {
              varBeginPos = lastWildcard.getPosInQuery();
              wildcardInVar = true;
            } else {
              varBeginPos = lastWildcard.getPosInQuery() + 1;
            }
          }

          if (WildcardType.ZeroOrMoreChars == w.getType()) {
            varEndPos = wildcardPos + 1;
            wildcardInVar = true;
          } else {
            varEndPos = wildcardPos;
          }

          tryAddWildcardVariable(varBeginPos, varEndPos, wildcardInVar);

          lastWildcard = w;
          wildcardInVar = false;
          break;
        }
        default:
          throw new UnsupportedOperationException("Unknown wildcard interpretation");
      }
    }

    if (null == lastWildcard) {
      variables.add(new WildcardToken(query, beginPos, endPos));
    } else if (lastWildcard.getPosInQuery() < endPos - 1) {
      if (WildcardType.ZeroOrMoreChars == lastWildcard.getType()) {
        varBeginPos = lastWildcard.getPosInQuery();
        wildcardInVar = true;
      } else {
        varBeginPos = lastWildcard.getPosInQuery() + 1;
      }
      varEndPos = endPos;
      tryAddWildcardVariable(varBeginPos, varEndPos, wildcardInVar);
    }
  }

  private void tryAddWildcardVariable(int beginPos, int endPos, boolean wildcardInToken) {
    if (wildcardInToken) {
      variables.add(new WildcardToken(query, beginPos, endPos));
    } else {
      String var = query.substring(beginPos, endPos);
      if (Parsing.isVar(var)) {
        variables.add(new ExactVariableToken(query, beginPos, endPos));
      }
    }
  }
}
