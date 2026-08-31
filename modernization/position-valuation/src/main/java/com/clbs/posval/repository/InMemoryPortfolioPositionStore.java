package com.clbs.posval.repository;

import com.clbs.posval.domain.PortfolioPosition;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * A keyed in-memory stand-in for the {@code PORTFILE} KSDS, sufficient for the batch path and for
 * the parity tests. A production deployment substitutes a JPA or JDBC implementation of
 * {@link PortfolioPositionStore}; nothing in the business logic depends on which is bound.
 */
@Repository
public class InMemoryPortfolioPositionStore implements PortfolioPositionStore {

    private final Map<String, PortfolioPosition> records = new LinkedHashMap<>();

    @Override
    public Optional<PortfolioPosition> read(String portfolioId) {
        return Optional.ofNullable(records.get(key(portfolioId)));
    }

    @Override
    public boolean rewrite(PortfolioPosition position) {
        String key = key(position.portfolioId());
        if (!records.containsKey(key)) {
            return false;
        }
        records.put(key, position);
        return true;
    }

    /** Loads a record, as the batch job's file allocation would. */
    public void load(PortfolioPosition position) {
        records.put(key(position.portfolioId()), position);
    }

    public void clear() {
        records.clear();
    }

    /** {@code PORT-ID PIC X(8)}: keys compare space padded and are case sensitive. */
    private static String key(String portfolioId) {
        return com.clbs.posval.cobol.CobolString.move(portfolioId, 8);
    }
}
