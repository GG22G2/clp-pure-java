package io.github.gg22g2.clp.compressorfrontend;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ComprehensiveEndToEndTest {

  @Test
  void testComprehensiveProductionLogsParity() throws IOException {
    MessageEncoder encoder = new MessageEncoder(
        BuiltInVariableHandlingRuleVersions.VariablesSchemaV2,
        BuiltInVariableHandlingRuleVersions.VariableEncodingMethodsV1
    );
    MessageDecoder decoder = new MessageDecoder(
        BuiltInVariableHandlingRuleVersions.VariablesSchemaV2,
        BuiltInVariableHandlingRuleVersions.VariableEncodingMethodsV1
    );
    EncodedMessage encodedMessage = new EncodedMessage();

    List<String> productionLogPatterns = Arrays.asList(
        // 1. Pure static text
        "INFO Application initialized successfully.",
        "DEBUG Connection pool ready.",

        // 2. Pure numbers & floats
        "42",
        "-42",
        "3.1415926535",
        "-0.00100",
        "1.1000000000",

        // 3. Timestamps & IP addresses
        "2026-11-08 23:30:15.123 [main] INFO com.example.Server - Listening on 192.168.1.100:8080",
        "2026/10/22 08:00:00.000 [worker-1] WARN Client 10.0.0.1 disconnected after 3000 ms",

        // 4. Hex hashes, UUIDs, IDs
        "Session created: c3ab8ff13720e8ad9047dd39466b3c89 for user_id=10086",
        "Transaction tx_999888777 committed in 12.34 ms with hash=0xdeadbeefcafebabe",

        // 5. Key-value pairs & JSON structures
        "Request received: method=POST uri=/api/v1/orders params={orderId=1001, amount=99.90, status=PENDING}",
        "metrics cpu_usage=0.75 memory_free=1048576 disk_io=123.456",

        // 6. Chinese and UTF-8 multibyte characters
        "2026-11-08 23:35:00 [http-nio-8080-exec-1] INFO 用户登录成功: uid=10001, 用户名=张三, IP=127.0.0.1, 耗时=15.2ms",
        "订单创建失败: 错误码=50001, 原因=余额不足, 当前余额=0.00, 扣款金额=199.99",
        "系统告警: 节点 [bj-node-01] CPU 负载过高 (当前: 98.5%, 阈值: 80.0%)",

        // 7. Special edge cases & delimiters
        "Path: C:\\Users\\Administrator\\AppData\\Local\\Temp\\log_2026.txt",
        "Equation: a + b = c (1 + 2 = 3), -5 * 10 = -50, float=-.5",
        "Empty braces: [], {}, (), <>, ==, !=, +=, -=",
        "Tabs\tand\nnewlines\rare\tpreserved\t123"
    );

    for (String originalLog : productionLogPatterns) {
      encoder.encodeMessage(originalLog, encodedMessage);

      // Verify that decoding from String matches original byte-for-byte
      String decodedFromString = decoder.decodeMessage(
          encodedMessage.getLogTypeAsString(),
          encodedMessage.getDictionaryVarsAsStrings(),
          encodedMessage.getEncodedVars()
      );
      assertEquals(originalLog, decodedFromString, "String decoding mismatch for: " + originalLog);

      // Verify decoding from byte arrays matches original
      String decodedFromByteArrays = decoder.decodeMessage(
          encodedMessage.getLogtype(),
          FlattenedByteArrayFactory.fromByteArrays(encodedMessage.getDictionaryVarsAsByteArrays()),
          encodedMessage.getEncodedVars()
      );
      assertEquals(originalLog, decodedFromByteArrays, "Byte array decoding mismatch for: " + originalLog);

      // Verify decoding from FlattenedByteArray matches original
      String decodedFromFlattened = decoder.decodeMessage(
          encodedMessage.getLogtype(),
          encodedMessage.getDictionaryVarsAsFlattenedByteArray(),
          encodedMessage.getEncodedVars()
      );
      assertEquals(originalLog, decodedFromFlattened, "FlattenedByteArray decoding mismatch for: " + originalLog);

      // Verify decodeMessageAsBytes
      byte[] decodedBytes = decoder.decodeMessageAsBytes(
          encodedMessage.getLogtype(),
          encodedMessage.getDictionaryVarsAsFlattenedByteArray(),
          encodedMessage.getEncodedVars()
      );
      assertArrayEquals(originalLog.getBytes(StandardCharsets.UTF_8), decodedBytes,
          "decodeMessageAsBytes mismatch for: " + originalLog);
    }
  }

  @Test
  void testWildcardSearchMethodsOnEncodedMessage() throws IOException {
    MessageEncoder encoder = new MessageEncoder(
        BuiltInVariableHandlingRuleVersions.VariablesSchemaV2,
        BuiltInVariableHandlingRuleVersions.VariableEncodingMethodsV1
    );
    MessageDecoder decoder = new MessageDecoder(
        BuiltInVariableHandlingRuleVersions.VariablesSchemaV2,
        BuiltInVariableHandlingRuleVersions.VariableEncodingMethodsV1
    );
    EncodedMessage encodedMessage = new EncodedMessage();

    encoder.encodeMessage("Status code 200, latency 12.34 ms, count 999", encodedMessage);
    String logtype = encodedMessage.getLogTypeAsString();
    long[] encodedVars = encodedMessage.getEncodedVars();

    // Integer searches: 200 and 999
    assertTrue(decoder.wildcardQueryMatchesAnyIntVar("2*0", logtype, encodedVars));
    assertTrue(decoder.wildcardQueryMatchesAnyIntVar("200", logtype, encodedVars));
    assertTrue(decoder.wildcardQueryMatchesAnyIntVar("*99*", logtype, encodedVars));
    assertFalse(decoder.wildcardQueryMatchesAnyIntVar("404", logtype, encodedVars));
    assertFalse(decoder.wildcardQueryMatchesAnyIntVar("12.34", logtype, encodedVars)); // Float, not int

    // Float searches: 12.34
    assertTrue(decoder.wildcardQueryMatchesAnyFloatVar("12.34", logtype, encodedVars));
    assertTrue(decoder.wildcardQueryMatchesAnyFloatVar("12.*", logtype, encodedVars));
    assertTrue(decoder.wildcardQueryMatchesAnyFloatVar("*.34", logtype, encodedVars));
    assertTrue(decoder.wildcardQueryMatchesAnyFloatVar("*2*3*", logtype, encodedVars));
    assertFalse(decoder.wildcardQueryMatchesAnyFloatVar("200", logtype, encodedVars)); // Int, not float
  }
}
