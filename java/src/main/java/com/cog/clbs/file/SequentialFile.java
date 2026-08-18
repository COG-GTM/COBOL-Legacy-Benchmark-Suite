package com.cog.clbs.file;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Sequential file abstraction.
 *
 * <p>Java equivalent of the ORGANIZATION IS SEQUENTIAL patterns in
 * {@code src/templates/program/file-handling.cbl}: OPEN INPUT / OPEN OUTPUT,
 * READ with AT END detection, WRITE, and CLOSE, with every operation
 * updating a COBOL-style {@link FileStatus} field so callers can apply the
 * 9000-CHECK-FILE-STATUS pattern.
 *
 * <p>Records are lines of text, mirroring fixed-format 80-byte records.
 */
public class SequentialFile implements AutoCloseable {

    /** Mirrors OPEN INPUT vs OPEN OUTPUT mode selection. */
    public enum Mode { INPUT, OUTPUT }

    private final Path path;
    private BufferedReader reader;
    private BufferedWriter writer;
    private FileStatus fileStatus = FileStatus.SUCCESS;

    public SequentialFile(Path path) {
        this.path = path;
    }

    /** OPEN INPUT / OPEN OUTPUT. */
    public FileStatus open(Mode mode) {
        try {
            if (mode == Mode.INPUT) {
                reader = Files.newBufferedReader(path);
            } else {
                writer = Files.newBufferedWriter(path);
            }
            return status(FileStatus.SUCCESS);
        } catch (IOException e) {
            return status(FileStatus.IO_ERROR);
        }
    }

    /** READ ... AT END: returns empty and sets status '10' at end of file. */
    public Optional<String> read() {
        if (reader == null) {
            status(FileStatus.FILE_NOT_OPEN);
            return Optional.empty();
        }
        try {
            String line = reader.readLine();
            if (line == null) {
                status(FileStatus.END_OF_FILE);
                return Optional.empty();
            }
            status(FileStatus.SUCCESS);
            return Optional.of(line);
        } catch (IOException e) {
            status(FileStatus.IO_ERROR);
            throw new UncheckedIOException(e);
        }
    }

    /** WRITE: appends one record. */
    public FileStatus write(String record) {
        if (writer == null) {
            return status(FileStatus.FILE_NOT_OPEN);
        }
        try {
            writer.write(record);
            writer.newLine();
            return status(FileStatus.SUCCESS);
        } catch (IOException e) {
            return status(FileStatus.IO_ERROR);
        }
    }

    /** CLOSE. */
    @Override
    public void close() {
        try {
            if (reader != null) {
                reader.close();
                reader = null;
            }
            if (writer != null) {
                writer.close();
                writer = null;
            }
            status(FileStatus.SUCCESS);
        } catch (IOException e) {
            status(FileStatus.IO_ERROR);
        }
    }

    public FileStatus getFileStatus() {
        return fileStatus;
    }

    private FileStatus status(FileStatus s) {
        this.fileStatus = s;
        return s;
    }
}
