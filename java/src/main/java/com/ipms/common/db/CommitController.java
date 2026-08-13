package com.ipms.common.db;

import com.ipms.domain.ReturnCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.HashMap;
import java.util.Map;

/**
 * Commit/rollback/savepoint controller, ported from {@code src/programs/common/DB2CMT.cbl}.
 *
 * <p>Preserves the original functions: INIT (reset statistics), CMIT (commit when the
 * processed-record count reaches the commit frequency, or when forced), RBAK (rollback),
 * SAVE (named savepoint), REST (rollback to savepoint), STAT (report statistics).
 */
public class CommitController {

    private static final Logger log = LoggerFactory.getLogger(CommitController.class);

    private final Connection connection;
    private final Map<String, Savepoint> savepoints = new HashMap<>();

    private long commitCount;
    private long rollbackCount;
    private long savepointCount;

    public CommitController(Connection connection) {
        this.connection = connection;
    }

    /** FUNC-INIT: resets commit statistics. */
    public void initialize() {
        commitCount = 0;
        rollbackCount = 0;
        savepointCount = 0;
        savepoints.clear();
    }

    /**
     * FUNC-CMIT (2000-COMMIT): commits when {@code recordsProcessed >= commitFrequency}
     * or {@code force} is set.
     *
     * @return true if a commit was issued
     */
    public boolean commitIfDue(long recordsProcessed, int commitFrequency, boolean force) {
        if (recordsProcessed >= commitFrequency || force) {
            commit();
            return true;
        }
        return false;
    }

    /** 2100-ISSUE-COMMIT. */
    public void commit() {
        try {
            connection.commit();
            commitCount++;
        } catch (SQLException e) {
            throw new ConnectionException("Commit failed", ReturnCodes.RC_ERROR, e);
        }
    }

    /** FUNC-RBAK (3000-ROLLBACK). */
    public void rollback() {
        try {
            connection.rollback();
            rollbackCount++;
        } catch (SQLException e) {
            throw new ConnectionException("Rollback failed", ReturnCodes.RC_ERROR, e);
        }
    }

    /** FUNC-SAVE (4000-SAVEPOINT): creates a named savepoint. */
    public void savepoint(String name) {
        try {
            savepoints.put(name, connection.setSavepoint(name));
            savepointCount++;
        } catch (SQLException e) {
            throw new ConnectionException("Savepoint creation failed", ReturnCodes.RC_ERROR, e);
        }
    }

    /** FUNC-REST (5000-RESTORE): rolls back to a previously created savepoint. */
    public void restore(String name) {
        Savepoint savepoint = savepoints.get(name);
        if (savepoint == null) {
            throw new ConnectionException("Unknown savepoint: " + name, ReturnCodes.RC_ERROR, null);
        }
        try {
            connection.rollback(savepoint);
            rollbackCount++;
        } catch (SQLException e) {
            throw new ConnectionException("Savepoint restore failed", ReturnCodes.RC_ERROR, e);
        }
    }

    /** FUNC-STAT (6000-STATISTICS). */
    public void reportStatistics() {
        log.info("DB2 Commit Controller Statistics: Commits: {} Rollbacks: {} Savepoints: {}",
                commitCount, rollbackCount, savepointCount);
    }

    public long getCommitCount() {
        return commitCount;
    }

    public long getRollbackCount() {
        return rollbackCount;
    }

    public long getSavepointCount() {
        return savepointCount;
    }
}
