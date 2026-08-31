package com.clbs.posval.repository;

import com.clbs.posval.domain.PortfolioPosition;
import java.util.Optional;

/**
 * The {@code PORTFILE} VSAM KSDS opened {@code I-O} with {@code ACCESS MODE IS RANDOM} and
 * {@code RECORD KEY IS PORT-ID} by {@code PORTTRAN} and {@code PORTUPDT}.
 *
 * <p>{@link #read} models {@code READ … INVALID KEY} (file status 23) and {@link #rewrite} models
 * {@code REWRITE … INVALID KEY}. Records are not created here: neither program in this slice
 * writes a new portfolio record.
 */
public interface PortfolioPositionStore {

    /** {@code READ PORTFOLIO-FILE}; empty models file status 23, record not found. */
    Optional<PortfolioPosition> read(String portfolioId);

    /** {@code REWRITE PORTFOLIO-RECORD}; false models an invalid key on rewrite. */
    boolean rewrite(PortfolioPosition position);
}
