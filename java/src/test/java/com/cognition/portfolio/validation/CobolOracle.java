package com.cognition.portfolio.validation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Runs every parity case through the real, unmodified PORTVALD module compiled with GnuCOBOL,
 * via the thin PVDRIVER program (src/test/cobol/PVDRIVER.cbl).
 *
 * <p>Record layouts (bytes are ISO-8859-1, no line terminators):
 * <pre>
 * PVIN  (51): LS-VALIDATE-TYPE X(1) | LS-INPUT-VALUE X(50)
 * PVOUT (76): LS-RETURN-CODE S9(4) SIGN LEADING SEPARATE (5) | LS-ERROR-MSG X(50)
 *             | RETURN-CODE S9(4) SIGN LEADING SEPARATE (5)
 *             | move probe S9(13)V99 SIGN LEADING SEPARATE (16)
 * </pre>
 */
final class CobolOracle {

    private static final int IN_RECORD = 1 + PortvalConstants.FIELD_LENGTH;
    private static final int OUT_RECORD = 5 + PortvalConstants.FIELD_LENGTH + 5 + 16;

    private CobolOracle() {
    }

    static List<CobolResult> run(Path driver, Path libraryPath, Path workDir, List<ParityCase> cases)
            throws IOException, InterruptedException {
        Files.createDirectories(workDir);
        Path in = workDir.resolve("pvin.dat");
        Path out = workDir.resolve("pvout.dat");
        Path log = workDir.resolve("pvdriver.log");
        Files.deleteIfExists(out);

        StringBuilder records = new StringBuilder();
        for (ParityCase c : cases) {
            records.append(c.validateType()).append(AlphanumericField.pad(c.inputValue(), PortvalConstants.FIELD_LENGTH));
        }
        Files.write(in, records.toString().getBytes(StandardCharsets.ISO_8859_1));

        ProcessBuilder pb = new ProcessBuilder(driver.toAbsolutePath().toString())
                .redirectErrorStream(true)
                .redirectOutput(log.toFile());
        pb.environment().put("PVIN", in.toAbsolutePath().toString());
        pb.environment().put("PVOUT", out.toAbsolutePath().toString());
        pb.environment().put("COB_LIBRARY_PATH", libraryPath.toAbsolutePath().toString());
        Process process = pb.start();
        if (!process.waitFor(60, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IllegalStateException("PVDRIVER timed out; see " + log);
        }
        if (process.exitValue() != 0) {
            throw new IllegalStateException("PVDRIVER exited " + process.exitValue() + ": " + Files.readString(log));
        }

        byte[] data = Files.readAllBytes(out);
        if (data.length != cases.size() * OUT_RECORD) {
            throw new IllegalStateException("PVOUT has " + data.length + " bytes; expected "
                    + cases.size() * OUT_RECORD + " for " + cases.size() + " cases");
        }
        String text = new String(data, StandardCharsets.ISO_8859_1);
        List<CobolResult> results = new ArrayList<>();
        for (int i = 0; i < cases.size(); i++) {
            String rec = text.substring(i * OUT_RECORD, (i + 1) * OUT_RECORD);
            results.add(new CobolResult(cases.get(i).id(),
                    Integer.parseInt(rec.substring(0, 5)),
                    rec.substring(5, 55),
                    Integer.parseInt(rec.substring(55, 60)),
                    probeToDecimal(rec.substring(60, 76))));
        }
        return results;
    }

    /** "+000000000012345" -> "123.45"; "-000000000000000" -> "-0.00". */
    static String probeToDecimal(String signLeadingSeparate) {
        char sign = signLeadingSeparate.charAt(0);
        String digits = signLeadingSeparate.substring(1);
        if ((sign != '+' && sign != '-') || digits.length() != 15 || !digits.chars().allMatch(Character::isDigit)) {
            throw new IllegalStateException("malformed move probe: " + signLeadingSeparate);
        }
        String integerPart = digits.substring(0, 13).replaceFirst("^0+(?=.)", "");
        return (sign == '-' ? "-" : "") + integerPart + "." + digits.substring(13);
    }
}
