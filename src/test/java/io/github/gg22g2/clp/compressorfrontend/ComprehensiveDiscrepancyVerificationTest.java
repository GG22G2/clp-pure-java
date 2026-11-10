package io.github.gg22g2.clp.compressorfrontend;

import io.github.gg22g2.clp.compressorfrontend.search.QueryToken;
import io.github.gg22g2.clp.compressorfrontend.search.Subquery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 针对文本列出的 12 项潜在非等效项的全面对比回归测试用例。
 * 本测试记录每项的实际入参、复刻版纯 Java 行为、以及与原版 C++ / FFI 的出参差异。
 */
public class ComprehensiveDiscrepancyVerificationTest {

  @Test
  @DisplayName("差异1: QueryMethods 静态文本段缺失 escape_handler，未转义占位符与反斜杠")
  void testDiscrepancy1_queryMethodsMissingEscapeHandler() {
    EightByteClpWildcardQueryEncoder encoder = new EightByteClpWildcardQueryEncoder(
        BuiltInVariableHandlingRuleVersions.VariablesSchemaV2,
        BuiltInVariableHandlingRuleVersions.VariableEncodingMethodsV1
    );

    // 输入 A: msg=\u0011 (包含变量占位符字节 0x11)
    EightByteClpEncodedSubquery[] subqueriesA = encoder.encode("msg=\u0011");
    // 纯 Java 版实际出参：未加反斜杠转义
    byte[] expectedPureJavaBytesA = "msg=\u0011".getBytes(StandardCharsets.ISO_8859_1);
    assertArrayEquals(expectedPureJavaBytesA, subqueriesA[0].getLogtypeQueryAsString().getBytes(StandardCharsets.ISO_8859_1));
    // 原版 y-scope/clp 主分支行为：escape_handler 在占位符前插入 '\' (0x5C)，生成 "msg=\\\u0011"

    // 输入 B: path\\file 123 (经过 cleanUp 后为 path\file 123)
    EightByteClpEncodedSubquery[] subqueriesB = encoder.encode("path\\\\file 123");
    // 纯 Java 版实际出参：保留单个反斜杠（在 Java 字符串字面量中需转义为 \\\\）
    byte[] expectedPureJavaBytesB = "path\\\\file \u0011".getBytes(StandardCharsets.ISO_8859_1);
    assertArrayEquals(expectedPureJavaBytesB, subqueriesB[0].getLogtypeQueryAsString().getBytes(StandardCharsets.ISO_8859_1));
    // 原版 y-scope/clp 主分支行为：在 \f 前再次转义为 "path\\\\file \u0011"
  }

  @Test
  @DisplayName("差异2: Subquery 当包含通配符时缺失对已转义占位符的二次转义 (double_escaped_logtype_query)")
  void testDiscrepancy2_subqueryMissingDoubleEscaping() {
    String logtypeQuery = "*\\\u0012";
    List<QueryToken> tokens = new ArrayList<>();
    Subquery subquery = new Subquery(logtypeQuery, tokens);

    // 纯 Java 版行为：未修改 logtypeQuery
    assertEquals("*\\\u0012", subquery.getLogtypeQuery());
    // 原版 y-scope/clp 主分支行为：Subquery.cpp 检测到通配符 '*' 且有已转义占位符 '\x12'，
    // 会重写为 "*\\\\\u0012" (double-escaped)，防止下游匹配消费掉转义符
  }

  @Test
  @DisplayName("差异3: EncodingMethods.encodeMessage 未转义常量且失败时对象状态变更时机不同")
  void testDiscrepancy3_encodeMessageConstantEscapingAndFailureState() throws IOException {
    byte[] msgWithPlaceholder = "hello \u0011 world".getBytes(StandardCharsets.UTF_8);

    // 1. 直接调用 EncodingMethods.encodeMessage
    EncodedMessage encodedMsg1 = new EncodedMessage();
    boolean encodeResult = EncodingMethods.encodeMessage(msgWithPlaceholder, encodedMsg1);
    // 纯 Java 版（及旧版子模块）遇到占位符直接返回 false
    assertFalse(encodeResult);
    // 且直接调用不会设置 message，此时若调用 getDictionaryVarsAsStrings 抛 NPE
    assertNull(encodedMsg1.getMessage());

    // 2. 通过 MessageEncoder.encodeMessage 调用
    MessageEncoder encoder = new MessageEncoder(
        BuiltInVariableHandlingRuleVersions.VariablesSchemaV2,
        BuiltInVariableHandlingRuleVersions.VariableEncodingMethodsV1
    );
    EncodedMessage encodedMsg2 = new EncodedMessage();
    assertThrows(IOException.class, () -> encoder.encodeMessage("hello \u0011 world", encodedMsg2));
    // 失败后 message 仍已被提前写入
    assertNotNull(encodedMsg2.getMessage());

    // 原版 y-scope/clp 主分支行为：调用 escape_and_append_const_to_logtype，将占位符转义并成功返回 true
  }

  @Test
  @DisplayName("差异4: convertStringToInt 对单独负号 '-' 返回 true 且值为 0L")
  void testDiscrepancy4_convertStringToIntMinusSign() {
    byte[] minusStr = "-".getBytes(StandardCharsets.US_ASCII);
    long[] result = new long[1];

    // 纯 Java 版行为：i++ 后跳过了 while 循环，返回 true，result[0] 为 0L
    boolean success = EncodingMethods.convertStringToInt(minusStr, 0, 1, result);
    assertTrue(success);
    assertEquals(0L, result[0]);

    // 原版 C++ 行为：std::from_chars 因无有效数字字符返回 false (std::errc::invalid_argument)
  }

  @Test
  @DisplayName("差异5: MessageDecoder.batchEncodedVarsWildcardMatch 缺失单调性与越界校验")
  void testDiscrepancy5_batchEncodedVarsMissingMonotonicityCheck() {
    MessageDecoder decoder = new MessageDecoder(
        BuiltInVariableHandlingRuleVersions.VariablesSchemaV2,
        BuiltInVariableHandlingRuleVersions.VariableEncodingMethodsV1
    );

    byte[][] logtypes = new byte[][]{"test".getBytes(StandardCharsets.ISO_8859_1)};
    long[][] encodedVars = new long[][]{new long[0]};
    byte[] placeholders = new byte[0];
    byte[] serializedQueries = "abcde".getBytes(StandardCharsets.ISO_8859_1);
    int[] matchResults = new int[1];

    // 5a: 非单调递增索引
    int[] nonMonotonic = new int[]{3, 1};
    // 原版 FFI 期望抛出：IllegalArgumentException
    // 纯 Java 版实际抛出：StringIndexOutOfBoundsException
    assertThrows(StringIndexOutOfBoundsException.class, () -> {
      decoder.batchEncodedVarsWildcardMatch(logtypes, encodedVars, placeholders, serializedQueries, nonMonotonic, matchResults);
    });

    // 5b: 越界索引
    int[] outOfBounds = new int[]{100};
    // 原版 FFI 期望抛出：IllegalArgumentException
    // 纯 Java 版实际抛出：StringIndexOutOfBoundsException
    assertThrows(StringIndexOutOfBoundsException.class, () -> {
      decoder.batchEncodedVarsWildcardMatch(logtypes, encodedVars, placeholders, serializedQueries, outOfBounds, matchResults);
    });
  }

  @Test
  @DisplayName("差异6: wildcardMatchEncodedVars 参数不匹配抛出 IllegalArgumentException 而非受检异常 IOException")
  void testDiscrepancy6_exceptionTypeInconsistency() {
    MessageDecoder decoder = new MessageDecoder(
        BuiltInVariableHandlingRuleVersions.VariablesSchemaV2,
        BuiltInVariableHandlingRuleVersions.VariableEncodingMethodsV1
    );

    byte[][] logtypes = new byte[][]{"test \u0011".getBytes(StandardCharsets.ISO_8859_1)};
    long[][] encodedVars = new long[][]{new long[]{123L}};
    byte[] placeholders = new byte[]{0x11, 0x11}; // 2 个占位符
    byte[] serializedQueries = "123".getBytes(StandardCharsets.ISO_8859_1);
    int[] queryEndIndexes = new int[]{3}; // 仅 1 个查询
    int[] matchResults = new int[1];

    // 原版 FFI 期望抛出：java.io.IOException (由 C++ EncodingException 映射)
    // 纯 Java 版实际抛出：java.lang.IllegalArgumentException (RuntimeException)
    assertThrows(IllegalArgumentException.class, () -> {
      decoder.batchEncodedVarsWildcardMatch(logtypes, encodedVars, placeholders, serializedQueries, queryEndIndexes, matchResults);
    });
  }

  @Test
  @DisplayName("差异7: wildcardVarQueries 为空数组且 logtype 含占位符时的边界保护")
  void testDiscrepancy7_emptyQueryArrayBoundsHandling() throws IOException {
    byte[] logtypeWithInt = "val=\u0011".getBytes(StandardCharsets.ISO_8859_1);
    long[] encodedVars = new long[]{123L};
    byte[] emptyPlaceholders = new byte[0];
    String[] emptyQueries = new String[0];

    // 纯 Java 版：加了 wildcardVarIx < wildcardVarQueriesLen 短路保护，安全返回 true
    boolean match = EncodingMethods.wildcardMatchEncodedVars(
        logtypeWithInt, encodedVars, 1, emptyPlaceholders, emptyQueries
    );
    assertTrue(match);

    // 原版 C++：wildcard_var_placeholders[0] 在空 string_view 上越界访问（未定义行为）
  }

  @Test
  @DisplayName("差异8: StringUtils.wildcardMatchUnsafe 的 4 处行为差异")
  void testDiscrepancy8_stringUtilsWildcardMatchDiscrepancies() {
    // 8a: 非 ASCII 字符大小写折叠
    // 纯 Java: true (Unicode 完整大小写折叠)；原版 C++: false (std::tolower 仅 ASCII)
    assertTrue(StringUtils.wildcardMatchUnsafe("CAFÉ", "café", false));

    // 8b: 多字节 UTF-8 字符匹配单字符通配符 '?'
    // 纯 Java: true (UTF-16 char 匹配)；原版 C++: false (UTF-8 占 2 字节，'?' 仅匹配 1 字节)
    assertTrue(StringUtils.wildcardMatchUnsafe("é", "?", true));

    // 8c: 未清理的末尾悬挂反斜杠
    // 纯 Java: 抛 StringIndexOutOfBoundsException；原版 C++: 返回 false
    assertThrows(StringIndexOutOfBoundsException.class, () -> {
      StringUtils.wildcardMatchUnsafe("ab", "a\\", true);
    });

    // 8d: 转义状态循环内重置
    // 纯 Java: 返回 true；原版旧子模块 2b884bd: 返回 false；原版 clp 主分支: 返回 true
    assertTrue(StringUtils.wildcardMatchUnsafe("12", "\\1?", true));
  }

  @Test
  @DisplayName("差异9: 非 ISO-8859-1 (> U+00FF) 字符转换与分词顺序差异")
  void testDiscrepancy9_nonIso88591CharacterHandlingOrder() throws IOException {
    // 9a: 查询编码子查询生成数差异
    EightByteClpWildcardQueryEncoder encoder = new EightByteClpWildcardQueryEncoder(
        BuiltInVariableHandlingRuleVersions.VariablesSchemaV2,
        BuiltInVariableHandlingRuleVersions.VariableEncodingMethodsV1
    );
    // 纯 Java 版先分词后转码：将 \u4e2d123 作为普通包含数字的词元，仅生成 1 个子查询
    EightByteClpEncodedSubquery[] pureSubqueries = encoder.encode("user=\u4e2d123");
    assertEquals(1, pureSubqueries.length);
    // 原版 FFI 先转 ISO_8859_1 字节：\u4e2d 变成 0x3F ('?')，被当成通配符分词，生成 4 个子查询！

    // 9b: 整数通配符匹配差异
    MessageDecoder decoder = new MessageDecoder(
        BuiltInVariableHandlingRuleVersions.VariablesSchemaV2,
        BuiltInVariableHandlingRuleVersions.VariableEncodingMethodsV1
    );
    // 纯 Java 版按字符比较：\u0100 与 '1' 不匹配，返回 false
    boolean pureMatched = decoder.wildcardQueryMatchesAnyIntVar("\u010023", "\u0011", new long[]{123L});
    assertFalse(pureMatched);
    // 原版 FFI 先转 ISO_8859_1：\u0100 变成 '?'，"?23" 成功匹配 "123"，返回 true！
  }

  @Test
  @DisplayName("差异10: decodeMessage 字典变量越界偏移量处理抛 IndexOutOfBoundsException 而非自动截断")
  void testDiscrepancy10_decodeMessageOutOfBoundsOffset() {
    MessageDecoder decoder = new MessageDecoder(
        BuiltInVariableHandlingRuleVersions.VariablesSchemaV2,
        BuiltInVariableHandlingRuleVersions.VariableEncodingMethodsV1
    );

    byte[] logtype = new byte[]{0x12}; // 字典变量占位符
    long[] encodedVars = new long[0];
    byte[] allDictVars = "abc".getBytes(StandardCharsets.ISO_8859_1); // 长度 3
    int[] dictVarEndOffsets = new int[]{10}; // 偏移量 10 > 3

    // 纯 Java 版：ByteArrayOutputStream.write 抛出 IndexOutOfBoundsException
    assertThrows(IndexOutOfBoundsException.class, () -> {
      decoder.decodeMessage(logtype, new FlattenedByteArray(allDictVars, dictVarEndOffsets), encodedVars);
    });

    // 原版 C++：std::string::append 自动截断至字符串末尾，返回 "abc"
  }

  @Test
  @DisplayName("差异11: Parsing.getBoundsOfNextVar String 重载返回 char 偏移且负数 bounds 抛异常")
  void testDiscrepancy11_parsingGetBoundsOfNextVarCharOffsetAndNegativeBounds() {
    String str = "é=abc";
    int[] bounds = new int[]{0, 0};

    // 11a: String 重载返回 UTF-16 char 偏移量 [2, 5]
    assertTrue(Parsing.getBoundsOfNextVar(str, bounds));
    assertArrayEquals(new int[]{2, 5}, bounds);

    // byte[] 重载返回 UTF-8 字节偏移量 [3, 6]（与原版 C++ 一致）
    byte[] utf8Bytes = str.getBytes(StandardCharsets.UTF_8);
    int[] byteBounds = new int[]{0, 0};
    assertTrue(Parsing.getBoundsOfNextVar(utf8Bytes, 0, utf8Bytes.length, byteBounds));
    assertArrayEquals(new int[]{3, 6}, byteBounds);

    // 11b: 负数 bounds[1] 抛 StringIndexOutOfBoundsException 而非提前返回 false
    int[] negBounds = new int[]{0, -1};
    assertThrows(StringIndexOutOfBoundsException.class, () -> {
      Parsing.getBoundsOfNextVar("test", negBounds);
    });
  }

  @Test
  @DisplayName("差异12: API 契约、缓存失效与版本常量")
  void testDiscrepancy12_apiContractCacheInvalidationAndVersions() {
    // 12a 核实: 原版与复刻版 EncodedMessage 均使用 getLogTypeAsString()（大写 T），此项为误报
    assertNotNull(assertDoesNotThrow(() -> EncodedMessage.class.getMethod("getLogTypeAsString")));

    // 12b: setDictionaryVarBounds 未失效 flattenedDictVars 缓存
    EncodedMessage msg = new EncodedMessage();
    msg.setMessage("user=alice".getBytes(StandardCharsets.UTF_8));
    msg.setDictionaryVarBounds(new int[]{5, 10});
    FlattenedByteArray cached1 = msg.getDictionaryVarsAsFlattenedByteArray();
    assertEquals("alice", msg.getDictionaryVarsAsStrings()[0]);

    // 修改 bounds 但不调用 setMessage
    msg.setDictionaryVarBounds(new int[]{5, 8});
    FlattenedByteArray cached2 = msg.getDictionaryVarsAsFlattenedByteArray();
    // 纯 Java 版缓存未清空，返回同一个过期对象
    assertSame(cached1, cached2);

    // 12c: 版本常量前缀差异
    assertEquals("io.github.gg22g2.clp.VariablesSchemaV2", BuiltInVariableHandlingRuleVersions.VariablesSchemaV2);
    // 原版为 "com.yscope.clp.VariablesSchemaV2"

    // 12d: validateVariableHandlingRuleVersions 传入 null 抛出 UnsupportedOperationException 而非 NPE
    assertThrows(UnsupportedOperationException.class, () -> {
      MessageEncoder.validateVariableHandlingRuleVersions(null, null);
    });
  }
}
