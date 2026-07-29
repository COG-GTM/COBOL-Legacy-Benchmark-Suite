package com.clbs.portfolio.service;

import com.clbs.portfolio.model.CobolText;
import com.clbs.portfolio.model.PortfolioRecord;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Test double for the indexed {@code PORTFOLIO-FILE}: a keyed store that behaves like a VSAM KSDS
 * accessed at random, leaving a file status behind after every operation.
 *
 * <p>Records are copied in and out, because a COBOL {@code READ} fills a record area the program
 * then modifies in place and only a {@code REWRITE} puts those modifications back on the file.
 * The status each operation reports can be forced, so the {@code INVALID KEY} branches and the
 * failed-open path are reachable from a test.
 */
class InMemoryPortfolioRepository implements PortfolioRepository {

    private final Map<String, PortfolioRecord> records = new LinkedHashMap<>();

    private String fileStatus = CobolText.spaces(2);
    private String openStatus = STATUS_SUCCESS;
    private String rewriteStatus = STATUS_SUCCESS;
    private int updateCount;
    private int closeCount;

    /** Loads a record, as a file-load job would. */
    InMemoryPortfolioRepository seed(PortfolioRecord portfolio) {
        records.put(portfolio.getPortId(), new PortfolioRecord(portfolio));
        return this;
    }

    /** Makes {@code OPEN I-O} report a failure. */
    InMemoryPortfolioRepository failingOpen(String status) {
        this.openStatus = status;
        return this;
    }

    /** Makes {@code REWRITE} report the given status, {@code 2x} for the invalid-key branch. */
    InMemoryPortfolioRepository rewriteStatus(String status) {
        this.rewriteStatus = status;
        return this;
    }

    @Override
    public String open() {
        fileStatus = openStatus;
        return fileStatus;
    }

    @Override
    public Optional<PortfolioRecord> findById(String portId) {
        PortfolioRecord found = records.get(CobolText.picX(portId, PortfolioRecord.ID_LENGTH));
        fileStatus = found == null ? STATUS_NOT_FOUND : STATUS_SUCCESS;
        return found == null ? Optional.empty() : Optional.of(new PortfolioRecord(found));
    }

    @Override
    public void update(PortfolioRecord portfolio) {
        updateCount++;
        fileStatus = rewriteStatus;
        if (!PortfolioRepository.isInvalidKey(rewriteStatus)) {
            records.put(portfolio.getPortId(), new PortfolioRecord(portfolio));
        }
    }

    @Override
    public void close() {
        closeCount++;
    }

    @Override
    public String getFileStatus() {
        return fileStatus;
    }

    /** The record as it stands on the file, not the caller's record area. */
    Optional<PortfolioRecord> stored(String portId) {
        PortfolioRecord found = records.get(CobolText.picX(portId, PortfolioRecord.ID_LENGTH));
        return found == null ? Optional.empty() : Optional.of(new PortfolioRecord(found));
    }

    int updateCount() {
        return updateCount;
    }

    int closeCount() {
        return closeCount;
    }
}
