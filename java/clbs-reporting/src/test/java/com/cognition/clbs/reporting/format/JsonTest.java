package com.cognition.clbs.reporting.format;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JsonTest {

  @Test
  void serialisesSupportedTypes() {
    Map<String, Object> root = Json.object();
    root.put("s", "quote\" back\\ nl\n cr\r tab\t ctl\u0001");
    root.put("n", 42);
    root.put("d", new BigDecimal("1234.50"));
    root.put("b", true);
    root.put("z", null);
    root.put("list", List.of(1, "two"));
    root.put("nested", Map.of("k", "v"));
    assertThat(Json.write(root))
        .isEqualTo(
            "{\"s\":\"quote\\\" back\\\\ nl\\n"
                + " cr\\r"
                + " tab\\t ctl\\u0001\",\"n\":42,"
                + "\"d\":1234.50,\"b\":true,\"z\":null,\"list\":[1,\"two\"],\"nested\":{\"k\":\"v\"}}");
  }

  @Test
  void rejectsUnsupportedTypes() {
    assertThatThrownBy(() -> Json.write(new Object())).isInstanceOf(IllegalArgumentException.class);
  }
}
