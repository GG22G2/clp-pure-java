package io.github.gg22g2.clp.compressorfrontend;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class ProductionFullScaleBenchmarkTest {

  @Test
  void testProduction20MBFullScaleLosslessEncodingAndDecoding() throws Exception {
    File logFile = new File("D:\\log_benchmark\\raw_20m\\debug.2026-10-09.0.log");
    if (!logFile.exists()) {
      System.out.println("Log file not found, skipping full scale benchmark: " + logFile.getAbsolutePath());
      return;
    }

    MessageEncoder encoder = new MessageEncoder(
        BuiltInVariableHandlingRuleVersions.VariablesSchemaV2,
        BuiltInVariableHandlingRuleVersions.VariableEncodingMethodsV1
    );
    MessageDecoder decoder = new MessageDecoder(
        BuiltInVariableHandlingRuleVersions.VariablesSchemaV2,
        BuiltInVariableHandlingRuleVersions.VariableEncodingMethodsV1
    );
    EncodedMessage encodedMessage = new EncodedMessage();

    long totalBytes = 0;
    long lineCount = 0;
    long totalLogtypeBytes = 0;
    long totalEncodedVarsCount = 0;
    long totalDictVarsCount = 0;

    System.out.println("Starting full scale 20MB production log test: " + logFile.getName());
    long startTime = System.currentTimeMillis();

    try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(logFile), StandardCharsets.UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) {
        lineCount++;
        totalBytes += line.getBytes(StandardCharsets.UTF_8).length;

        // 1. Encode with Pure Java CLP
        encoder.encodeMessage(line, encodedMessage);

        totalLogtypeBytes += encodedMessage.getLogtype().length;
        if (encodedMessage.getEncodedVars() != null) {
          totalEncodedVarsCount += encodedMessage.getEncodedVars().length;
        }
        if (encodedMessage.getDictionaryVarsAsStrings() != null) {
          totalDictVarsCount += encodedMessage.getDictionaryVarsAsStrings().length;
        }

        // 2. Decode with Pure Java CLP
        String decoded = decoder.decodeMessage(
            encodedMessage.getLogtype(),
            encodedMessage.getDictionaryVarsAsFlattenedByteArray(),
            encodedMessage.getEncodedVars()
        );

        // 3. Assert 100% losslessness
        if (!line.equals(decoded)) {
          fail("Mismatch at line " + lineCount + "\nExpected: " + line + "\nActual:   " + decoded);
        }
      }
    }

    long elapsedMs = System.currentTimeMillis() - startTime;
    double throughputMBps = (totalBytes / (1024.0 * 1024.0)) / (elapsedMs / 1000.0);
    double throughputLinesPerSec = lineCount / (elapsedMs / 1000.0);

    System.out.println("==================================================");
    System.out.println("Pure Java CLP Full Scale Production Verification SUCCESS!");
    System.out.println("Total Lines Verified: " + lineCount);
    System.out.println("Total Raw Bytes: " + totalBytes + " (" + String.format("%.2f", totalBytes / (1024.0 * 1024.0)) + " MB)");
    System.out.println("Total Logtype Bytes: " + totalLogtypeBytes);
    System.out.println("Total Encoded Numbers: " + totalEncodedVarsCount);
    System.out.println("Total Dictionary Vars: " + totalDictVarsCount);
    System.out.println("Time Elapsed: " + elapsedMs + " ms");
    System.out.println("Throughput: " + String.format("%.2f", throughputMBps) + " MB/s (" + String.format("%.0f", throughputLinesPerSec) + " lines/sec)");
    System.out.println("Lossless Rate: 100.000% (" + lineCount + "/" + lineCount + " lines bit-exact)");
    System.out.println("==================================================");
  }
}
