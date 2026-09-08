package com.clbs.store;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * VSAM KSDS replacement: a keyed record store with COBOL file-status semantics.
 *
 * <p>Records are held in key order so sequential browse (START / READ NEXT) behaves like the
 * indexed reads in the COBOL programs. Every operation sets {@link #status()} to the value the
 * COBOL FILE STATUS field would carry, which the migrated programs branch on exactly as the
 * originals did on INVALID KEY / AT END.
 */
public class IndexedFile<T> {

    private final TreeMap<String, T> records = new TreeMap<>(Comparator.naturalOrder());
    private final Function<T, String> keyExtractor;
    private final String name;
    private String status = FileStatus.SUCCESS;
    private String browsePosition;
    private boolean open;

    public IndexedFile(String name, Function<T, String> keyExtractor) {
        this.name = name;
        this.keyExtractor = keyExtractor;
    }

    public String name() {
        return name;
    }

    public String status() {
        return status;
    }

    public boolean isOpen() {
        return open;
    }

    public void open() {
        open = true;
        status = FileStatus.SUCCESS;
        browsePosition = null;
    }

    public void close() {
        open = false;
        status = FileStatus.SUCCESS;
    }

    /** READ ... INVALID KEY. Returns null and status 23 when the key is absent. */
    public T read(String key) {
        T record = records.get(key);
        status = record == null ? FileStatus.NOT_FOUND : FileStatus.SUCCESS;
        return record;
    }

    /** WRITE ... INVALID KEY. Status 22 on duplicate key. */
    public String write(T record) {
        String key = keyExtractor.apply(record);
        if (records.containsKey(key)) {
            status = FileStatus.DUPLICATE_KEY;
            return status;
        }
        records.put(key, record);
        status = FileStatus.SUCCESS;
        return status;
    }

    /** REWRITE ... INVALID KEY. Status 23 when the record does not exist. */
    public String rewrite(T record) {
        String key = keyExtractor.apply(record);
        if (!records.containsKey(key)) {
            status = FileStatus.NOT_FOUND;
            return status;
        }
        records.put(key, record);
        status = FileStatus.SUCCESS;
        return status;
    }

    /** DELETE ... INVALID KEY. */
    public String delete(String key) {
        status = records.remove(key) == null ? FileStatus.NOT_FOUND : FileStatus.SUCCESS;
        return status;
    }

    /** START ... KEY >= / KEY >. */
    public String start(String key, boolean inclusive) {
        Map.Entry<String, T> entry = inclusive ? records.ceilingEntry(key) : records.higherEntry(key);
        if (entry == null) {
            status = FileStatus.NOT_FOUND;
            browsePosition = null;
            return status;
        }
        browsePosition = entry.getKey();
        status = FileStatus.SUCCESS;
        return status;
    }

    /** Position the browse before the first record (READ NEXT from start of file). */
    public void startAtBeginning() {
        browsePosition = records.isEmpty() ? null : records.firstKey();
        status = records.isEmpty() ? FileStatus.END_OF_FILE : FileStatus.SUCCESS;
    }

    /** READ NEXT ... AT END. Returns null and status 10 at end of file. */
    public T readNext() {
        if (browsePosition == null) {
            status = FileStatus.END_OF_FILE;
            return null;
        }
        T record = records.get(browsePosition);
        Map.Entry<String, T> next = records.higherEntry(browsePosition);
        browsePosition = next == null ? null : next.getKey();
        status = FileStatus.SUCCESS;
        return record;
    }

    public boolean atEnd() {
        return FileStatus.END_OF_FILE.equals(status);
    }

    public List<T> all() {
        return new ArrayList<>(records.values());
    }

    public int size() {
        return records.size();
    }

    public void clear() {
        records.clear();
        browsePosition = null;
        status = FileStatus.SUCCESS;
    }
}
