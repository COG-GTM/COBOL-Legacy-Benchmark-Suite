package com.cog.clbs.file;

import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * VSAM KSDS file abstraction.
 *
 * <p>Java equivalent of the indexed (ORGANIZATION IS INDEXED, ACCESS MODE IS
 * DYNAMIC) file patterns in {@code src/templates/program/file-handling.cbl}:
 * keyed READ, WRITE with INVALID KEY / duplicate-key detection, REWRITE,
 * DELETE, and sequential browse (START / READ NEXT) semantics.
 *
 * <p>Records are keyed strings held in an ordered index (a {@link TreeMap}),
 * representing the KSDS key-sequenced organization. Every operation updates
 * {@link #getFileStatus()}, mirroring the WS-VSAM-STATUS checking pattern
 * (9000-CHECK-FILE-STATUS / 9100-VSAM-ERROR).
 *
 * @param <V> the record type (VSAM-RECORD-DATA)
 */
public class VsamFile<V> {

    private final SortedMap<String, V> records = new TreeMap<>();
    private boolean open = false;
    private FileStatus fileStatus = FileStatus.SUCCESS;

    /** OPEN: makes the file available for keyed operations. */
    public FileStatus open() {
        open = true;
        return status(FileStatus.SUCCESS);
    }

    /** CLOSE: ends processing of the file. */
    public FileStatus close() {
        open = false;
        return status(FileStatus.SUCCESS);
    }

    /** READ ... KEY IS: returns the record for a key, status '23' if absent. */
    public Optional<V> read(String key) {
        if (requireOpen()) {
            return Optional.empty();
        }
        V value = records.get(key);
        status(value != null ? FileStatus.SUCCESS : FileStatus.RECORD_NOT_FOUND);
        return Optional.ofNullable(value);
    }

    /** WRITE: adds a new record, status '22' on duplicate key (INVALID KEY). */
    public FileStatus write(String key, V record) {
        if (requireOpen()) {
            return fileStatus;
        }
        if (records.containsKey(key)) {
            return status(FileStatus.DUPLICATE_KEY);
        }
        records.put(key, record);
        return status(FileStatus.SUCCESS);
    }

    /** REWRITE: replaces an existing record, status '23' if absent. */
    public FileStatus rewrite(String key, V record) {
        if (requireOpen()) {
            return fileStatus;
        }
        if (!records.containsKey(key)) {
            return status(FileStatus.RECORD_NOT_FOUND);
        }
        records.put(key, record);
        return status(FileStatus.SUCCESS);
    }

    /** DELETE: removes a record, status '23' if absent. */
    public FileStatus delete(String key) {
        if (requireOpen()) {
            return fileStatus;
        }
        if (records.remove(key) == null) {
            return status(FileStatus.RECORD_NOT_FOUND);
        }
        return status(FileStatus.SUCCESS);
    }

    /**
     * START key &gt;= ... / READ NEXT: returns the first record at or after
     * the given key, status '10' (end of file) if none.
     */
    public Optional<Map.Entry<String, V>> readNext(String fromKey) {
        if (requireOpen()) {
            return Optional.empty();
        }
        SortedMap<String, V> tail = records.tailMap(fromKey);
        if (tail.isEmpty()) {
            status(FileStatus.END_OF_FILE);
            return Optional.empty();
        }
        status(FileStatus.SUCCESS);
        return Optional.of(Map.entry(tail.firstKey(), tail.get(tail.firstKey())));
    }

    public FileStatus getFileStatus() {
        return fileStatus;
    }

    public boolean isOpen() {
        return open;
    }

    public int recordCount() {
        return records.size();
    }

    private boolean requireOpen() {
        if (!open) {
            status(FileStatus.FILE_NOT_OPEN);
            return true;
        }
        return false;
    }

    private FileStatus status(FileStatus s) {
        this.fileStatus = s;
        return s;
    }
}
