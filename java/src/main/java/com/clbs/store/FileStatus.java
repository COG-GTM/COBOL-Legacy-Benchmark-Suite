package com.clbs.store;

/** COBOL FILE STATUS values used across the suite (ERRHAND.cpy VSAM-STATUS-CODES). */
public final class FileStatus {

    public static final String SUCCESS = "00";
    public static final String END_OF_FILE = "10";
    public static final String DUPLICATE_KEY = "22";
    public static final String NOT_FOUND = "23";
    public static final String PERMANENT_ERROR = "30";

    private FileStatus() {
    }
}
