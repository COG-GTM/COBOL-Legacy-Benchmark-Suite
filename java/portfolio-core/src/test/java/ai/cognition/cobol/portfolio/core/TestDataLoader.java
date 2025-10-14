package ai.cognition.cobol.portfolio.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TestDataLoader {
    
    private static final Path TEST_DATA_ROOT = Paths.get("../../shared/test-data");
    
    public static String loadTestData(String filename) throws IOException {
        Path filePath = TEST_DATA_ROOT.resolve(filename);
        return Files.readString(filePath);
    }
}
