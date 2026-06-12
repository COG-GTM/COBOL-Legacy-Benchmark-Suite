package com.benchmark.portfolio.common.compare;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads fixed-width (mainframe RECFM=FB style, no record delimiter) datasets
 * driven by a {@link RecordLayout}. Decodes CHAR fields as trimmed Strings and
 * ZONED / PACKED (COMP-3) fields as BigDecimal with the layout's scale.
 */
public final class FixedWidthRecordReader {

    private final RecordLayout layout;

    public FixedWidthRecordReader(RecordLayout layout) {
        this.layout = layout;
    }

    public List<ComparableRecord> readAll(Path file) throws IOException {
        return readAll(Files.readAllBytes(file));
    }

    public List<ComparableRecord> readAll(InputStream in) throws IOException {
        return readAll(in.readAllBytes());
    }

    public List<ComparableRecord> readAll(byte[] data) {
        int recordLength = layout.recordLength();
        if (data.length % recordLength != 0) {
            throw new IllegalArgumentException(
                    "data length " + data.length + " is not a multiple of record length " + recordLength);
        }
        List<ComparableRecord> records = new ArrayList<>(data.length / recordLength);
        for (int pos = 0; pos < data.length; pos += recordLength) {
            records.add(decodeRecord(data, pos));
        }
        return records;
    }

    private ComparableRecord decodeRecord(byte[] data, int base) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (FieldLayout field : layout.fields()) {
            int start = base + field.offset();
            values.put(field.name(), decodeField(field, data, start));
        }
        return new ComparableRecord(values, layout.keyFields());
    }

    private Object decodeField(FieldLayout field, byte[] data, int start) {
        return switch (field.type()) {
            case CHAR -> new String(data, start, field.length(), StandardCharsets.US_ASCII).trim();
            case ZONED -> decodeZoned(field, data, start);
            case PACKED -> decodePacked(field, data, start);
        };
    }

    /**
     * Decodes a zoned decimal (PIC 9 / S9 DISPLAY) field. Handles plain ASCII
     * digits as well as an overpunched sign in the final byte
     * (EBCDIC-convention zones 0xC/0xF positive, 0xD negative; ASCII
     * overpunch letters A-I/J-R and '{'/'}').
     */
    private BigDecimal decodeZoned(FieldLayout field, byte[] data, int start) {
        StringBuilder digits = new StringBuilder(field.length());
        boolean negative = false;
        for (int i = 0; i < field.length(); i++) {
            int b = data[start + i] & 0xFF;
            char c = (char) b;
            if (c >= '0' && c <= '9') {
                digits.append(c);
            } else if (i == field.length() - 1) {
                if (c == '{') {
                    digits.append('0');
                } else if (c == '}') {
                    digits.append('0');
                    negative = true;
                } else if (c >= 'A' && c <= 'I') {
                    digits.append((char) ('1' + (c - 'A')));
                } else if (c >= 'J' && c <= 'R') {
                    digits.append((char) ('1' + (c - 'J')));
                    negative = true;
                } else {
                    throw new IllegalArgumentException(
                            "invalid zoned decimal byte 0x" + Integer.toHexString(b) + " in field " + field.name());
                }
            } else if (c == ' ') {
                digits.append('0');
            } else {
                throw new IllegalArgumentException(
                        "invalid zoned decimal byte 0x" + Integer.toHexString(b) + " in field " + field.name());
            }
        }
        BigDecimal value = new BigDecimal(new BigInteger(digits.toString()), field.scale());
        return negative ? value.negate() : value;
    }

    /**
     * Decodes a packed decimal (COMP-3) field: two digits per byte, sign in
     * the final nibble (0xD negative; 0xA/0xC/0xE/0xF positive).
     */
    private BigDecimal decodePacked(FieldLayout field, byte[] data, int start) {
        StringBuilder digits = new StringBuilder(field.length() * 2);
        for (int i = 0; i < field.length(); i++) {
            int b = data[start + i] & 0xFF;
            int high = b >> 4;
            int low = b & 0x0F;
            if (high > 9) {
                throw new IllegalArgumentException(
                        "invalid packed decimal digit nibble in field " + field.name());
            }
            digits.append((char) ('0' + high));
            if (i < field.length() - 1) {
                if (low > 9) {
                    throw new IllegalArgumentException(
                            "invalid packed decimal digit nibble in field " + field.name());
                }
                digits.append((char) ('0' + low));
            } else {
                if (low < 0x0A) {
                    throw new IllegalArgumentException(
                            "missing sign nibble in packed decimal field " + field.name());
                }
                BigDecimal value = new BigDecimal(new BigInteger(digits.toString()), field.scale());
                return low == 0x0D || low == 0x0B ? value.negate() : value;
            }
        }
        throw new IllegalStateException("unreachable");
    }
}
