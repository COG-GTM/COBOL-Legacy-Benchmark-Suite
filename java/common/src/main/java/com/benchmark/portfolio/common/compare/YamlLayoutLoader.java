package com.benchmark.portfolio.common.compare;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

/**
 * Loads a {@link RecordLayout} from a YAML descriptor, so copybook layouts
 * can be declared without code changes. Expected document shape:
 *
 * <pre>
 * name: PORTFLIO
 * recordLength: 148
 * keyFields: [PORT-ID]
 * fields:
 *   - {name: PORT-ID, offset: 0, length: 8, type: CHAR}
 *   - {name: PORT-TOTAL-VALUE, offset: 66, length: 8, type: PACKED, scale: 2}
 * </pre>
 */
public final class YamlLayoutLoader {

    public RecordLayout load(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            return load(in);
        }
    }

    @SuppressWarnings("unchecked")
    public RecordLayout load(InputStream in) {
        Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
        Map<String, Object> doc = yaml.load(in);
        if (doc == null) {
            throw new IllegalArgumentException("empty YAML layout document");
        }
        String name = requireString(doc, "name");
        int recordLength = requireInt(doc, "recordLength");
        List<String> keyFields = (List<String>) require(doc, "keyFields");
        List<Map<String, Object>> fieldDocs = (List<Map<String, Object>>) require(doc, "fields");

        List<FieldLayout> fields = new ArrayList<>(fieldDocs.size());
        for (Map<String, Object> fieldDoc : fieldDocs) {
            FieldType type = FieldType.valueOf(requireString(fieldDoc, "type"));
            int scale = fieldDoc.containsKey("scale") ? requireInt(fieldDoc, "scale") : 0;
            fields.add(new FieldLayout(
                    requireString(fieldDoc, "name"),
                    requireInt(fieldDoc, "offset"),
                    requireInt(fieldDoc, "length"),
                    type,
                    scale));
        }
        return new RecordLayout(name, recordLength, fields, keyFields);
    }

    private static Object require(Map<String, Object> doc, String key) {
        Object value = doc.get(key);
        if (value == null) {
            throw new IllegalArgumentException("YAML layout missing required key: " + key);
        }
        return value;
    }

    private static String requireString(Map<String, Object> doc, String key) {
        return String.valueOf(require(doc, key));
    }

    private static int requireInt(Map<String, Object> doc, String key) {
        Object value = require(doc, key);
        if (value instanceof Number n) {
            return n.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }
}
