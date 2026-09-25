package com.cognition.portfolio.validation;

import java.io.UncheckedIOException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/** Locates the checked in parity data, which lives outside the Maven module. */
final class ParityFiles {

    static final String CASES = "parity/cases/portvald-cases.psv";
    static final String COBOL_OUTPUT = "parity/expected/portvald-cobol-output.psv";

    private ParityFiles() {
    }

    static Path repositoryRoot() {
        Path dir = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        while (dir != null) {
            if (Files.isRegularFile(dir.resolve(CASES))) {
                return dir;
            }
            dir = dir.getParent();
        }
        throw new IllegalStateException(
                "Could not locate " + CASES + " above " + System.getProperty("user.dir"));
    }

    static List<String> readLines(String relativePath) {
        try {
            return Files.readAllLines(repositoryRoot().resolve(relativePath), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
