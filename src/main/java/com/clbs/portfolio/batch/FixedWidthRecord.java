package com.clbs.portfolio.batch;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Splits a sequential file of fixed-length records and reads PIC X fields from them. */
final class FixedWidthRecord {

    private final byte[] data;

    private FixedWidthRecord(byte[] data) {
        this.data = data;
    }

    static List<FixedWidthRecord> split(byte[] file, int recordLength) {
        if (file.length % recordLength != 0) {
            throw new IllegalArgumentException("Input length " + file.length
                    + " is not a multiple of the record length " + recordLength);
        }
        List<FixedWidthRecord> records = new ArrayList<>();
        for (int i = 0; i < file.length; i += recordLength) {
            byte[] rec = new byte[recordLength];
            System.arraycopy(file, i, rec, 0, recordLength);
            records.add(new FixedWidthRecord(rec));
        }
        return records;
    }

    /** PIC X(length) at the 0-based offset, exactly as stored (including trailing spaces). */
    String text(int offset, int length) {
        return new String(data, offset, length, StandardCharsets.ISO_8859_1);
    }

    byte[] bytes() {
        return data;
    }
}
