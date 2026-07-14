package com.cog.gtm.clbs.migration.golden;

import com.cog.gtm.clbs.migration.service.validation.ValidationResult;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.core.io.ClassPathResource;

public final class GoldenMasterFixture {

    private GoldenMasterFixture() {
    }

    public static List<GoldenMasterCase> load(String inputsPath, String expectedPath) throws IOException {
        List<GoldenMasterInput> inputs = loadInputs(inputsPath);
        List<GoldenMasterExpected> expecteds = loadExpected(expectedPath);

        if (inputs.size() != expecteds.size()) {
            throw new IllegalStateException(
                    "Input fixture size (" + inputs.size() + ") does not match expected fixture size (" + expecteds.size() + ")");
        }

        List<GoldenMasterCase> cases = new ArrayList<>(inputs.size());
        for (int i = 0; i < inputs.size(); i++) {
            GoldenMasterInput in = inputs.get(i);
            GoldenMasterExpected exp = expecteds.get(i);
            if (!in.validationType().equals(exp.validationType()) || !in.inputValue().equals(exp.inputValue())) {
                throw new IllegalStateException(
                        "Input and expected fixtures are not aligned at row " + (i + 1)
                                + ": input=" + in + ", expected=" + exp);
            }
            cases.add(new GoldenMasterCase(in.validationType(), in.inputValue(),
                    new ValidationResult(exp.returnCode(), exp.errorMessage())));
        }
        return cases;
    }

    private static List<GoldenMasterInput> loadInputs(String path) throws IOException {
        List<GoldenMasterInput> rows = new ArrayList<>();
        try (BufferedReader reader = readerFor(path)) {
            String line = reader.readLine(); // skip header
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                String[] parts = line.split(",", -1);
                rows.add(new GoldenMasterInput(parts[0], parts[1]));
            }
        }
        return rows;
    }

    private static List<GoldenMasterExpected> loadExpected(String path) throws IOException {
        List<GoldenMasterExpected> rows = new ArrayList<>();
        try (BufferedReader reader = readerFor(path)) {
            String line = reader.readLine(); // skip header
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                String[] parts = line.split(",", -1);
                int rc = Integer.parseInt(parts[2].trim());
                String msg = parts.length > 3 ? parts[3] : "";
                rows.add(new GoldenMasterExpected(parts[0], parts[1], rc, msg));
            }
        }
        return rows;
    }

    private static BufferedReader readerFor(String path) throws IOException {
        ClassPathResource resource = new ClassPathResource(path);
        InputStream is = resource.getInputStream();
        return new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
    }

    private record GoldenMasterInput(String validationType, String inputValue) {
    }

    private record GoldenMasterExpected(String validationType, String inputValue, int returnCode, String errorMessage) {
    }
}
