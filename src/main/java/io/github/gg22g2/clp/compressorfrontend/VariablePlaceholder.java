package io.github.gg22g2.clp.compressorfrontend;

public final class VariablePlaceholder {
  public static final byte Integer = 0x11;
  public static final byte Dictionary = 0x12;
  public static final byte Float = 0x13;
  public static final byte EscapeCharacter = '\\';

  private VariablePlaceholder() {}

  public static boolean isVariablePlaceholder(byte c) {
    return c == Integer || c == Dictionary || c == Float;
  }
}
