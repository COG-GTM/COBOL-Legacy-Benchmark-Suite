package com.benchmark.portfolio.common.compare;

import java.util.List;
import java.util.Objects;

/**
 * Layout descriptor for a fixed-width record: ordered field descriptors, the
 * total record length in bytes, and the names of the fields forming the
 * record key used to align two streams during comparison.
 *
 * <p>Includes the two built-in layouts documented in
 * {@code java/test-fixtures/README.md}: the portfolio master record
 * (PORTFLIO.cpy, 148 bytes) and the transaction record (TRNREC.cpy,
 * 152 bytes).</p>
 */
public record RecordLayout(String name, int recordLength, List<FieldLayout> fields, List<String> keyFields) {

    public RecordLayout {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(fields, "fields");
        Objects.requireNonNull(keyFields, "keyFields");
        if (recordLength <= 0) {
            throw new IllegalArgumentException("recordLength must be > 0");
        }
        fields = List.copyOf(fields);
        keyFields = List.copyOf(keyFields);
        for (FieldLayout field : fields) {
            if (field.offset() + field.length() > recordLength) {
                throw new IllegalArgumentException(
                        "field " + field.name() + " extends past record length " + recordLength);
            }
        }
        List<String> fieldNames = fields.stream().map(FieldLayout::name).toList();
        for (String key : keyFields) {
            if (!fieldNames.contains(key)) {
                throw new IllegalArgumentException("key field not declared in layout: " + key);
            }
        }
    }

    public FieldLayout field(String fieldName) {
        return fields.stream()
                .filter(f -> f.name().equals(fieldName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("no such field: " + fieldName));
    }

    /** Portfolio master record layout per PORTFLIO.cpy (148 bytes). */
    public static RecordLayout portfolioMaster() {
        return new RecordLayout("PORTFLIO", 148,
                List.of(
                        FieldLayout.character("PORT-ID", 0, 8),
                        FieldLayout.character("PORT-ACCOUNT-NO", 8, 10),
                        FieldLayout.character("PORT-CLIENT-NAME", 18, 30),
                        FieldLayout.character("PORT-CLIENT-TYPE", 48, 1),
                        FieldLayout.zoned("PORT-CREATE-DATE", 49, 8, 0),
                        FieldLayout.zoned("PORT-LAST-MAINT", 57, 8, 0),
                        FieldLayout.character("PORT-STATUS", 65, 1),
                        FieldLayout.packed("PORT-TOTAL-VALUE", 66, 8, 2),
                        FieldLayout.packed("PORT-CASH-BALANCE", 74, 8, 2),
                        FieldLayout.character("PORT-LAST-USER", 82, 8),
                        FieldLayout.zoned("PORT-LAST-TRANS", 90, 8, 0),
                        FieldLayout.character("PORT-FILLER", 98, 50)),
                List.of("PORT-ID"));
    }

    /** Transaction record layout per TRNREC.cpy (152 bytes). */
    public static RecordLayout transactionRecord() {
        return new RecordLayout("TRNREC", 152,
                List.of(
                        FieldLayout.character("TRN-DATE", 0, 8),
                        FieldLayout.character("TRN-TIME", 8, 6),
                        FieldLayout.character("TRN-PORTFOLIO-ID", 14, 8),
                        FieldLayout.character("TRN-SEQUENCE-NO", 22, 6),
                        FieldLayout.character("TRN-INVESTMENT-ID", 28, 10),
                        FieldLayout.character("TRN-TYPE", 38, 2),
                        FieldLayout.packed("TRN-QUANTITY", 40, 8, 4),
                        FieldLayout.packed("TRN-PRICE", 48, 8, 4),
                        FieldLayout.packed("TRN-AMOUNT", 56, 8, 2),
                        FieldLayout.character("TRN-CURRENCY", 64, 3),
                        FieldLayout.character("TRN-STATUS", 67, 1),
                        FieldLayout.character("TRN-PROCESS-DATE", 68, 26),
                        FieldLayout.character("TRN-PROCESS-USER", 94, 8),
                        FieldLayout.character("TRN-FILLER", 102, 50)),
                List.of("TRN-PORTFOLIO-ID", "TRN-SEQUENCE-NO"));
    }
}
