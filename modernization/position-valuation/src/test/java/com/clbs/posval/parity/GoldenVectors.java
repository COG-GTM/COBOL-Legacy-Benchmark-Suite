package com.clbs.posval.parity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads the pipe-separated golden vector files under {@code src/test/resources/parity}.
 *
 * <p>Those files are not hand written. They are the stdout of COBOL harnesses compiled by
 * {@code parity/generate-golden-vectors.sh} against the unmodified programs and the production
 * {@code PIC} clauses, so every expectation asserted against them is an observation of COBOL
 * behaviour rather than a reading of the source.
 */
public final class GoldenVectors {

    private GoldenVectors() {}

    /** One row of a golden vector file, with the header line removed. */
    public record Row(List<String> fields) {

        public String get(int index) {
            return fields.get(index);
        }

        /** The field with the trailing spaces of its COBOL edited picture removed. */
        public String trimmed(int index) {
            return fields.get(index).strip();
        }
    }

    public static List<Row> load(String resource) {
        List<Row> rows = new ArrayList<>();
        try (InputStream in = GoldenVectors.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("missing golden vector file: " + resource);
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            reader.readLine(); // header
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) {
                    rows.add(new Row(List.of(line.split("\\|", -1))));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        if (rows.isEmpty()) {
            throw new IllegalStateException("empty golden vector file: " + resource);
        }
        return rows;
    }
}
