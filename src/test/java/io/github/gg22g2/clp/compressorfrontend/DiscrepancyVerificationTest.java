package io.github.gg22g2.clp.compressorfrontend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

public class DiscrepancyVerificationTest {

  /**
   * 差异 1 验证：
   * 原版 MessageDecoder.batchEncodedVarsWildcardMatch 中，对 varWildcardQueryEndIndexes 进行了单调性和越界校验：
   * 若 i < lastIdx 或 i > serializedVarWildcardQueries.length，明确抛出 IllegalArgumentException。
   *
   * 纯 Java 版省略了该校验，导致在反序列化循环中直接执行：
   * new String(serializedVarWildcardQueries, queryBeginIdx, endIdx - queryBeginIdx, ...)
   * 从而抛出底层的 StringIndexOutOfBoundsException，违反原版对外 API 契约。
   */
  @Test
  @DisplayName("验证差异1: varWildcardQueryEndIndexes 乱序或越界时，抛出 StringIndexOutOfBoundsException 而非原版的 IllegalArgumentException")
  void verifyDiscrepancy1_varWildcardQueryEndIndexesValidationMissing() {
    MessageDecoder decoder = new MessageDecoder(
        BuiltInVariableHandlingRuleVersions.VariablesSchemaV2,
        BuiltInVariableHandlingRuleVersions.VariableEncodingMethodsV1
    );

    byte[][] logtypes = new byte[][]{"test".getBytes(StandardCharsets.ISO_8859_1)};
    long[][] encodedVars = new long[][]{new long[0]};
    byte[] placeholders = new byte[0];
    byte[] serializedQueries = "abcde".getBytes(StandardCharsets.ISO_8859_1);
    int[] matchResults = new int[1];

    // 情况 A：非单调递增（例如 {3, 1}）
    int[] nonMonotonicEndIndexes = new int[]{3, 1};
    // 原版行为期望：IllegalArgumentException
    // 纯 Java 版实际行为：抛出 StringIndexOutOfBoundsException (length is negative: -2)
    assertThrows(StringIndexOutOfBoundsException.class, () -> {
      decoder.batchEncodedVarsWildcardMatch(
          logtypes,
          encodedVars,
          placeholders,
          serializedQueries,
          nonMonotonicEndIndexes,
          matchResults
      );
    });

    // 情况 B：越界（例如 {100} > serializedQueries.length）
    int[] outOfBoundsEndIndexes = new int[]{100};
    // 原版行为期望：IllegalArgumentException
    // 纯 Java 版实际行为：抛出 StringIndexOutOfBoundsException (offset 0, count 100, length 5)
    assertThrows(StringIndexOutOfBoundsException.class, () -> {
      decoder.batchEncodedVarsWildcardMatch(
          logtypes,
          encodedVars,
          placeholders,
          serializedQueries,
          outOfBoundsEndIndexes,
          matchResults
      );
    });
  }

  /**
   * 差异 2 验证：
   * 原版 C++ ffi::wildcard_match_encoded_vars 在参数不匹配（如 wildcard_var_queries.size() != wildcard_var_placeholders.length()）时
   * 抛出 ffi::EncodingException，被 JNI 层包装为 java.io.IOException 抛给 Java 层。原版 batchEncodedVarsWildcardMatch 声明了 throws IOException。
   *
   * 纯 Java 版 EncodingMethods.wildcardMatchEncodedVars 直接抛出了非受检的 IllegalArgumentException。
   * 当调用方使用标准的 IOException 捕获时，该异常无法被捕获，导致行为不一致。
   */
  @Test
  @DisplayName("验证差异2: wildcardMatchEncodedVars 参数不匹配时抛出 IllegalArgumentException 而非原版 JNI 的 IOException")
  void verifyDiscrepancy2_parameterMismatchThrowsIllegalArgumentExceptionInsteadOfIOException() {
    MessageDecoder decoder = new MessageDecoder(
        BuiltInVariableHandlingRuleVersions.VariablesSchemaV2,
        BuiltInVariableHandlingRuleVersions.VariableEncodingMethodsV1
    );

    byte[][] logtypes = new byte[][]{"test \u0011".getBytes(StandardCharsets.ISO_8859_1)};
    long[][] encodedVars = new long[][]{new long[]{123L}};
    // 提供 2 个占位符，但只有 1 个查询，产生数量不匹配
    byte[] placeholders = new byte[]{VariablePlaceholder.Integer, VariablePlaceholder.Integer};
    byte[] serializedQueries = "123".getBytes(StandardCharsets.ISO_8859_1);
    int[] queryEndIndexes = new int[]{3}; // 仅 1 个查询
    int[] matchResults = new int[1];

    // 原版期望：抛出 IOException
    // 纯 Java 版实际：抛出 IllegalArgumentException
    assertThrows(IllegalArgumentException.class, () -> {
      decoder.batchEncodedVarsWildcardMatch(
          logtypes,
          encodedVars,
          placeholders,
          serializedQueries,
          queryEndIndexes,
          matchResults
      );
    });

    // 直接调用 EncodingMethods 也是抛出 IllegalArgumentException
    assertThrows(IllegalArgumentException.class, () -> {
      EncodingMethods.wildcardMatchEncodedVars(
          logtypes[0],
          encodedVars[0],
          encodedVars[0].length,
          placeholders,
          new String[]{"123"}
      );
    });
  }

  /**
   * 差异 3 验证：
   * 原版 C++ string_utils.cpp 中使用 std::tolower((unsigned char)c) 进行单字节 ASCII 大小写转换。
   * 纯 Java 版 StringUtils.wildcardMatchUnsafe 中调用了无参 toLowerCase()，依赖系统默认 Locale.getDefault()。
   * 在土耳其语 Locale (tr-TR) 下，'I' 的小写变成 '\u0131'（无点 i），导致大小写不敏感匹配失败。
   */
  @Test
  @DisplayName("验证差异3: StringUtils.wildcardMatchUnsafe 未指定 Locale.ROOT 导致在特异语言环境（如土耳其语）下 ASCII 匹配失效")
  void verifyDiscrepancy3_localeSensitiveWildcardMatchBug() {
    Locale originalLocale = Locale.getDefault();
    try {
      // 在通用环境下正常匹配
      Locale.setDefault(Locale.US);
      assertTrue(StringUtils.wildcardMatchUnsafe("INFO", "info", false));
      assertTrue(StringUtils.wildcardMatchUnsafe("INFO", "i*", false));

      // 切换至土耳其语环境
      Locale.setDefault(Locale.forLanguageTag("tr-TR"));

      // 原版 C++ 由于是纯 ASCII 逐字节处理，不受系统语言影响，预期为 true
      // 纯 Java 版修复为 toLowerCase(Locale.ROOT) 后，与原版完全一致，返回 true
      assertTrue(StringUtils.wildcardMatchUnsafe("INFO", "info", false));
      assertTrue(StringUtils.wildcardMatchUnsafe("INFO", "i*", false));
    } finally {
      Locale.setDefault(originalLocale);
    }
  }

  /**
   * 差异 4 验证：
   * 原版 Java_MessageDecoder.cpp 中：
   * auto logtype_len = nullptr == Java_logtype ? 0 : jni_env->GetArrayLength(Java_logtype);
   * 当 logtypes[i] == null 时，C++ 获取其长度为 0，视为长度为 0 的字节序列（空字符串），并正常进行 wildcard_match_encoded_vars。
   * 当查询无变量通配符时，原版 C++ 判定匹配成功，写入 1。
   *
   * 纯 Java 版修复后：
   * byte[] logtype = EmptyArrayUtils.getNonNullByteArray(logtypes[i]);
   * 与原版 C++ JNI 完全一致，logtype == null 也作为空数组处理，返回 1。
   */
  @Test
  @DisplayName("验证差异4: batchEncodedVarsWildcardMatch 对 logtype == null 与空数组处理保持自洽且与原版 JNI 完全一致")
  void verifyDiscrepancy4_logtypeNullHandlingInconsistency() throws IOException {
    MessageDecoder decoder = new MessageDecoder(
        BuiltInVariableHandlingRuleVersions.VariablesSchemaV2,
        BuiltInVariableHandlingRuleVersions.VariableEncodingMethodsV1
    );

    // 两个消息：一个 logtype 为空 byte[]，一个为 null
    byte[][] logtypes = new byte[][]{
        new byte[0],
        null
    };
    long[][] encodedVars = new long[][]{
        new long[0],
        new long[0]
    };
    byte[] placeholders = new byte[0];
    byte[] serializedQueries = new byte[0];
    int[] queryEndIndexes = new int[0];
    int[] matchResults = new int[2];

    decoder.batchEncodedVarsWildcardMatch(
        logtypes,
        encodedVars,
        placeholders,
        serializedQueries,
        queryEndIndexes,
        matchResults
    );

    // 修复后与原版完全一致：两个都匹配为 1
    assertEquals(1, matchResults[0]);
    assertEquals(1, matchResults[1]);
  }
}
