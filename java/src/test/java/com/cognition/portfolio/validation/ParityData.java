package com.cognition.portfolio.validation;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Reads and writes the parity case table and the recorded COBOL results (TSV, see file headers). */
final class ParityData {

    static final String CASES_RESOURCE = "/parity/portvald-cases.tsv";
    static final String GOLDEN_RESOURCE = "/parity/portvald-cobol-golden.tsv";

    static final String GOLDEN_HEADER = "case_id\treturn_code\terror_message\tcall_return_code\tmove_probe";

    private ParityData() {
    }

    static List<ParityCase> loadCases() {
        List<ParityCase> cases = new ArrayList<>();
        for (String[] row : readRows(CASES_RESOURCE, "case_id\tvalidate_type\tinput_value\tcategory\tdescription", 5)) {
            String type = unescape(row[1]);
            if (type.length() != 1) {
                throw new IllegalStateException(row[0] + ": validate_type must be exactly one character");
            }
            String input = unescape(row[2]);
            if (input.length() > PortvalConstants.FIELD_LENGTH) {
                throw new IllegalStateException(row[0] + ": input_value exceeds PIC X(50)");
            }
            cases.add(new ParityCase(row[0], type.charAt(0), input, row[3], row[4]));
        }
        return cases;
    }

    static Map<String, CobolResult> loadGolden() {
        Map<String, CobolResult> results = new LinkedHashMap<>();
        for (String[] row : readRows(GOLDEN_RESOURCE, GOLDEN_HEADER, 5)) {
            CobolResult result = new CobolResult(row[0], Integer.parseInt(row[1]),
                    AlphanumericField.pad(unescape(row[2]), PortvalConstants.FIELD_LENGTH),
                    Integer.parseInt(row[3]), row[4]);
            if (results.put(result.caseId(), result) != null) {
                throw new IllegalStateException("duplicate golden case_id " + result.caseId());
            }
        }
        return results;
    }

    static void writeGolden(Path file, List<CobolResult> results) {
        StringBuilder out = new StringBuilder();
        out.append("# PORTVALD outputs recorded from the compiled COBOL program (GnuCOBOL 3.1.2) by java/run-parity.sh.\n");
        out.append("# Generated file - regenerate with: java/run-parity.sh --update-golden\n");
        out.append("# error_message uses the case-table encoding; trailing spaces of the PIC X(50) field are implicit.\n");
        out.append("# move_probe: PIC S9(13)V99 value after PORTVALD's amount MOVE (signed; '-' kept on negative zero).\n");
        out.append(GOLDEN_HEADER).append('\n');
        for (CobolResult r : results) {
            out.append(r.caseId()).append('\t')
                    .append(r.returnCode()).append('\t')
                    .append(escape(r.errorMessage()).stripTrailing()).append('\t')
                    .append(r.callReturnCode()).append('\t')
                    .append(r.moveProbe()).append('\n');
        }
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, out, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static List<String[]> readRows(String resource, String expectedHeader, int columns) {
        String text;
        try (InputStream in = ParityData.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("missing test resource " + resource);
            }
            text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        List<String[]> rows = new ArrayList<>();
        boolean headerSeen = false;
        for (String line : text.split("\n", -1)) {
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if (!headerSeen) {
                if (!line.equals(expectedHeader)) {
                    throw new IllegalStateException(resource + ": unexpected header " + line);
                }
                headerSeen = true;
                continue;
            }
            String[] row = line.split("\t", -1);
            if (row.length != columns) {
                throw new IllegalStateException(resource + ": expected " + columns + " columns in: " + line);
            }
            rows.add(row);
        }
        return rows;
    }

    /** Decodes {@code \t \n \r \\ \xHH}. Any other backslash sequence is an error. */
    static String unescape(String encoded) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < encoded.length(); i++) {
            char c = encoded.charAt(i);
            if (c != '\\') {
                out.append(c);
                continue;
            }
            if (i + 1 >= encoded.length()) {
                throw new IllegalArgumentException("dangling backslash in: " + encoded);
            }
            char next = encoded.charAt(++i);
            switch (next) {
                case 't' -> out.append('\t');
                case 'n' -> out.append('\n');
                case 'r' -> out.append('\r');
                case '\\' -> out.append('\\');
                case 'x' -> {
                    if (i + 2 >= encoded.length()) {
                        throw new IllegalArgumentException("truncated \\x escape in: " + encoded);
                    }
                    out.append((char) Integer.parseInt(encoded.substring(i + 1, i + 3), 16));
                    i += 2;
                }
                default -> throw new IllegalArgumentException("unknown escape \\" + next + " in: " + encoded);
            }
        }
        return out.toString();
    }

    /** Inverse of {@link #unescape}: printable ASCII except backslash is kept, everything else is escaped. */
    static String escape(String raw) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '\\') {
                out.append("\\\\");
            } else if (c >= 0x20 && c <= 0x7E) {
                out.append(c);
            } else {
                out.append(String.format("\\x%02x", (int) c));
            }
        }
        return out.toString();
    }
}
