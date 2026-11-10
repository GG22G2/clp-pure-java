# CLP Pure Java

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java](https://img.shields.io/badge/Java-8%2B-orange.svg)](https://www.oracle.com/java/)

> [!WARNING]
> 本项目完全由 AI 实现，本人只使用压缩归档，检索相关的存在问题。

---

**CLP Pure Java** is a 100% Pure Java implementation of the core encoding and decoding algorithms of **CLP (Compressed Log Processor)**.

It provides 1:1 algorithmic parity with the official [YScope CLP C++ implementation](https://github.com/y-scope/clp) while eliminating all native JNI / FFI dependencies, making it seamlessly portable across all operating systems and Java environments.

---

> [!NOTE]
> **Disclaimer**: This is an independent, unofficial 100% Pure Java port of YScope CLP. 
> YScope and CLP are trademarks and projects of their respective owners.

---

## Features

- **100% Pure Java**: Zero native libraries (`.so`, `.dll`, `.dylib`), zero JNI/FFI overhead or platform compatibility headaches (no glibc version mismatch issues).
- **1:1 Algorithmic Parity**: Fully bit-exact lossless log message encoding and decoding matching the official YScope CLP V2 schema.
- **High Performance**: Highly optimized zero-allocation and segment parsing routines for high-throughput log ingestion.
- **Broad Compatibility**: Compiles with and supports Java 8 and above.

---

## Installation

### Maven

```xml
<dependency>
    <groupId>io.github.gg22g2</groupId>
    <artifactId>clp-pure-java</artifactId>
    <version>0.4.0</version>
</dependency>
```

### Gradle

```groovy
implementation 'io.github.gg22g2:clp-pure-java:0.4.0'
```

---

## Quick Start

### Encoding and Decoding Log Messages

```java
import io.github.gg22g2.clp.compressorfrontend.BuiltInVariableHandlingRuleVersions;
import io.github.gg22g2.clp.compressorfrontend.EncodedMessage;
import io.github.gg22g2.clp.compressorfrontend.MessageDecoder;
import io.github.gg22g2.clp.compressorfrontend.MessageEncoder;

public class Example {
    public static void main(String[] args) throws Exception {
        // Initialize encoder and decoder using standard CLP V2 schema rules
        MessageEncoder encoder = new MessageEncoder(
            BuiltInVariableHandlingRuleVersions.VariablesSchemaV2,
            BuiltInVariableHandlingRuleVersions.VariableEncodingMethodsV1
        );
        MessageDecoder decoder = new MessageDecoder(
            BuiltInVariableHandlingRuleVersions.VariablesSchemaV2,
            BuiltInVariableHandlingRuleVersions.VariableEncodingMethodsV1
        );

        String originalLog = "2026-11-09 09:30:15.123 [main] INFO  com.example.Service - Request processed in 45ms for user_id=10086";

        // 1. Encode
        EncodedMessage encodedMessage = new EncodedMessage();
        encoder.encodeMessage(originalLog, encodedMessage);

        System.out.println("Logtype template: " + encodedMessage.getLogTypeAsString());
        System.out.println("Encoded numbers count: " + (encodedMessage.getEncodedVars() == null ? 0 : encodedMessage.getEncodedVars().length));
        System.out.println("Dictionary variables count: " + (encodedMessage.getDictionaryVarsAsStrings() == null ? 0 : encodedMessage.getDictionaryVarsAsStrings().length));

        // 2. Decode (100% lossless reproduction)
        String decodedLog = decoder.decodeMessage(encodedMessage.getLogTypeAsString(), encodedMessage.getDictionaryVarsAsStrings(), encodedMessage.getEncodedVars());
        assert originalLog.equals(decodedLog);
        System.out.println("Decoded matches original: " + originalLog.equals(decodedLog));
    }
}
```

---

## Building from Source

```bash
git clone https://github.com/gg22g2/clp-pure-java.git
cd clp-pure-java
mvn clean test
```

---

## License & Attribution

This project is licensed under the [Apache License 2.0](LICENSE).

This product includes software developed at [YScope Inc.](https://github.com/y-scope/clp), originally licensed under the Apache License 2.0.
See the [NOTICE](NOTICE) file for additional details.
