package com.benchmark.portfolio.common.compare;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads CSV datasets (header row required) into {@link ComparableRecord}s,
 * using a {@link RecordLayout} to determine field types: numeric fields are
 * parsed as BigDecimal at the layout's scale, character fields as trimmed
 * Strings. Header columns must match the layout's field names; layout fields
 * absent from the CSV (e.g. FILLER columns) decode as empty strings.
 */
public final class CsvRecordReader {

    private final RecordLayout layout;

    public CsvRecordReader(RecordLayout layout) {
        this.layout = layout;
    }

    public List<ComparableRecord> readAll(Path file) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return readAll(reader);
        }
    }

    public List<ComparableRecord> readAll(Reader reader) throws IOException {
        BufferedReader buffered = reader instanceof BufferedReader br ? br : new BufferedReader(reader);
        String headerLine = buffered.readLine();
        if (headerLine == null) {
            throw new IllegalArgumentException("CSV is empty: missing header row");
        }
        String[] header = headerLine.split(",", -1);
        List<ComparableRecord> records = new ArrayList<>();
        String line;
        while ((line = buffered.readLine()) != null) {
            if (line.isBlank()) {
                continue;
            }
            String[] cells = line.split(",", -1);
            if (cells.length != header.length) {
                throw new IllegalArgumentException(
                        "CSV row has " + cells.length + " cells, expected " + header.length + ": " + line);
            }
            Map<String, String> row = new LinkedHashMap<>();
            for (int i = 0; i < header.length; i++) {
                row.put(header[i].trim(), cells[i]);
            }
            records.add(decodeRow(row));
        }
        return records;
    }

    private ComparableRecord decodeRow(Map<String, String> row) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (FieldLayout field : layout.fields()) {
            String raw = row.get(field.name());
            if (raw == null) {
                values.put(field.name(), field.type() == FieldType.CHAR
                        ? ""
                        : BigDecimal.ZERO.setScale(field.scale()));
                continue;
            }
            values.put(field.name(), switch (field.type()) {
                case CHAR -> raw.trim();
                case ZONED, PACKED -> new BigDecimal(raw.trim()).setScale(field.scale());
            });
        }
        return new ComparableRecord(values, layout.keyFields());
    }
}
