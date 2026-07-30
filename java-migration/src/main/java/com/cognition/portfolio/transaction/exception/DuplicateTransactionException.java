package com.cognition.portfolio.transaction.exception;

/**
 * Equivalent of VSAM file status {@code 22} (duplicate key) on a {@code WRITE}, handled in
 * {@code PORTMSTR 2000-CREATE-PORTFOLIO} as 'already exists'.
 */
public class DuplicateTransactionException extends RuntimeException {

  public DuplicateTransactionException(String key) {
    super("Transaction already exists for TRN-KEY: " + key);
  }
}
