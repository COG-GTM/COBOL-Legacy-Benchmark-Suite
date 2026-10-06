package com.cognition.clbs.reporting.format;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Minimal JSON serializer for the optional report outputs. Supports the value types the report
 * models use: strings, numbers, booleans, {@code null}, ordered maps and collections.
 */
public final class Json {

  private Json() {}

  /** An insertion-ordered object builder. */
  public static Map<String, Object> object() {
    return new LinkedHashMap<>();
  }

  /** Serialises {@code value} as compact JSON. */
  public static String write(Object value) {
    StringBuilder sb = new StringBuilder();
    write(sb, value);
    return sb.toString();
  }

  private static void write(StringBuilder sb, Object value) {
    if (value == null) {
      sb.append("null");
    } else if (value instanceof String s) {
      writeString(sb, s);
    } else if (value instanceof BigDecimal d) {
      sb.append(d.toPlainString());
    } else if (value instanceof Number || value instanceof Boolean) {
      sb.append(value);
    } else if (value instanceof Map<?, ?> map) {
      sb.append('{');
      boolean first = true;
      for (Map.Entry<?, ?> e : map.entrySet()) {
        if (!first) {
          sb.append(',');
        }
        first = false;
        writeString(sb, String.valueOf(e.getKey()));
        sb.append(':');
        write(sb, e.getValue());
      }
      sb.append('}');
    } else if (value instanceof Collection<?> items) {
      sb.append('[');
      boolean first = true;
      for (Object item : items) {
        if (!first) {
          sb.append(',');
        }
        first = false;
        write(sb, item);
      }
      sb.append(']');
    } else {
      throw new IllegalArgumentException("Unsupported JSON value type " + value.getClass());
    }
  }

  private static void writeString(StringBuilder sb, String s) {
    sb.append('"');
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      switch (c) {
        case '"' -> sb.append("\\\"");
        case '\\' -> sb.append("\\\\");
        case '\n' -> sb.append("\\n");
        case '\r' -> sb.append("\\r");
        case '\t' -> sb.append("\\t");
        default -> {
          if (c < 0x20) {
            sb.append(String.format("\\u%04x", (int) c));
          } else {
            sb.append(c);
          }
        }
      }
    }
    sb.append('"');
  }
}
