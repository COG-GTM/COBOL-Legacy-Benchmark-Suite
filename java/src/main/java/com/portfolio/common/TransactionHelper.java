package com.portfolio.common;

import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Migration of {@code src/programs/common/DB2CMT.cbl} (DB2 Commit Controller).
 *
 * <p>Mapping of DB2CMT functions:
 * <ul>
 *   <li>INIT (1000-INITIALIZE) → {@link #resetStatistics()}</li>
 *   <li>CMIT with LS-COMMIT-FREQ (2000/2100) → {@link #commitIfDue(long, int, boolean)}
 *       / {@link #executeInTransaction(TransactionCallback)}; in Spring Batch,
 *       periodic commits are the chunk boundary instead</li>
 *   <li>RBAK (3000-ROLLBACK) → {@link TransactionStatus#setRollbackOnly()} inside a
 *       callback, or a thrown runtime exception that rolls the transaction back</li>
 *   <li>SAVE/REST (4000/5000 savepoints) → nested transactions
 *       (PROPAGATION_NESTED) when needed; not required by the HISTLD00 slice</li>
 *   <li>STAT (6000-STATISTICS) → {@link #getCommitCount()} / {@link #getRollbackCount()}</li>
 *   <li>Non-zero SQLCODE + RC 8 branches → {@link SqlProcessingException} /
 *       Spring {@code TransactionException}s</li>
 * </ul>
 */
@Component
public class TransactionHelper {

    private final TransactionTemplate transactionTemplate;
    private final AtomicLong commitCount = new AtomicLong();
    private final AtomicLong rollbackCount = new AtomicLong();

    public TransactionHelper(PlatformTransactionManager transactionManager) {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /** Runs the callback in a transaction; commit on success, rollback on exception. */
    public <T> T executeInTransaction(TransactionCallback<T> callback) {
        try {
            T result = transactionTemplate.execute(callback);
            commitCount.incrementAndGet();
            return result;
        } catch (RuntimeException e) {
            rollbackCount.incrementAndGet();
            throw e;
        }
    }

    /**
     * Equivalent of DB2CMT 2000-COMMIT: commit only when the number of records
     * processed reaches the commit frequency, or when forced.
     *
     * @return true if a commit boundary is due (caller runs its unit of work
     *         via {@link #executeInTransaction(TransactionCallback)})
     */
    public boolean commitIfDue(long recordsProcessed, int commitFrequency, boolean force) {
        return force || recordsProcessed >= commitFrequency;
    }

    /** DB2CMT INIT function. */
    public void resetStatistics() {
        commitCount.set(0);
        rollbackCount.set(0);
    }

    public long getCommitCount() {
        return commitCount.get();
    }

    public long getRollbackCount() {
        return rollbackCount.get();
    }
}
