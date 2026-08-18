package com.cog.clbs.file;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileHandlingTest {

    @Test
    void vsamCrudOperationsSetFileStatus() {
        VsamFile<String> file = new VsamFile<>();

        assertEquals(FileStatus.FILE_NOT_OPEN, file.write("K1", "data"));

        assertEquals(FileStatus.SUCCESS, file.open());
        assertEquals(FileStatus.SUCCESS, file.write("K1", "data-1"));
        assertEquals(FileStatus.DUPLICATE_KEY, file.write("K1", "dup"));

        assertEquals(Optional.of("data-1"), file.read("K1"));
        assertEquals(FileStatus.SUCCESS, file.getFileStatus());

        assertTrue(file.read("MISSING").isEmpty());
        assertEquals(FileStatus.RECORD_NOT_FOUND, file.getFileStatus());

        assertEquals(FileStatus.SUCCESS, file.rewrite("K1", "data-2"));
        assertEquals(Optional.of("data-2"), file.read("K1"));

        assertEquals(FileStatus.SUCCESS, file.delete("K1"));
        assertEquals(FileStatus.RECORD_NOT_FOUND, file.delete("K1"));

        assertEquals(FileStatus.SUCCESS, file.close());
        assertFalse(file.isOpen());
    }

    @Test
    void vsamReadNextBrowsesInKeyOrder() {
        VsamFile<String> file = new VsamFile<>();
        file.open();
        file.write("B", "b");
        file.write("A", "a");

        var first = file.readNext("");
        assertTrue(first.isPresent());
        assertEquals("A", first.get().getKey());

        assertTrue(file.readNext("Z").isEmpty());
        assertEquals(FileStatus.END_OF_FILE, file.getFileStatus());
    }

    @Test
    void sequentialWriteThenReadWithAtEnd(@TempDir Path dir) {
        Path path = dir.resolve("seqfile.dat");

        try (SequentialFile out = new SequentialFile(path)) {
            assertEquals(FileStatus.SUCCESS, out.open(SequentialFile.Mode.OUTPUT));
            assertEquals(FileStatus.SUCCESS, out.write("RECORD-1"));
            assertEquals(FileStatus.SUCCESS, out.write("RECORD-2"));
        }

        try (SequentialFile in = new SequentialFile(path)) {
            assertEquals(FileStatus.SUCCESS, in.open(SequentialFile.Mode.INPUT));
            assertEquals(Optional.of("RECORD-1"), in.read());
            assertEquals(Optional.of("RECORD-2"), in.read());
            assertTrue(in.read().isEmpty());
            assertEquals(FileStatus.END_OF_FILE, in.getFileStatus());
        }
    }
}
