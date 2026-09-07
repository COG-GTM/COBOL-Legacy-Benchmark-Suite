package com.clbs.store;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** QSAM sequential dataset replacement (audit trail, report and extract files). */
public class SequentialFile<T> {

    private final List<T> records = new ArrayList<>();
    private final String name;
    private String status = FileStatus.SUCCESS;
    private int readPosition;

    public SequentialFile(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    public String status() {
        return status;
    }

    /** OPEN INPUT: rewind the read cursor. */
    public void openInput() {
        readPosition = 0;
        status = FileStatus.SUCCESS;
    }

    /** OPEN OUTPUT: replace the dataset contents. */
    public void openOutput() {
        records.clear();
        readPosition = 0;
        status = FileStatus.SUCCESS;
    }

    /** WRITE. */
    public String write(T record) {
        records.add(record);
        status = FileStatus.SUCCESS;
        return status;
    }

    /** READ ... AT END. */
    public T read() {
        if (readPosition >= records.size()) {
            status = FileStatus.END_OF_FILE;
            return null;
        }
        status = FileStatus.SUCCESS;
        return records.get(readPosition++);
    }

    public boolean atEnd() {
        return FileStatus.END_OF_FILE.equals(status);
    }

    public List<T> all() {
        return Collections.unmodifiableList(new ArrayList<>(records));
    }

    public int size() {
        return records.size();
    }

    public void clear() {
        records.clear();
        readPosition = 0;
        status = FileStatus.SUCCESS;
    }
}
