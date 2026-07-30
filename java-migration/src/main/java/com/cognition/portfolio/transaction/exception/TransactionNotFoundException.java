package com.cognition.portfolio.transaction.exception;

/** Equivalent of the {@code INVALID KEY} branch of a keyed {@code READ} on {@code TRANHIST}. */
public class TransactionNotFoundException extends RuntimeException {

  public TransactionNotFoundException(String key) {
    super("Transaction not found for TRN-KEY: " + key);
  }
}
