package com.portfolio.service;

import com.portfolio.exception.PortfolioNotFoundException;
import com.portfolio.model.Portfolio;
import com.portfolio.model.PositionHistory;
import com.portfolio.repository.PortfolioRepository;
import com.portfolio.repository.PositionHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Service layer replacing the PROCEDURE DIVISION logic of INQONLN.cbl.
 *
 * In the original COBOL program, INQONLN.cbl acted as a controller that
 * dispatched to sub-programs via EXEC CICS LINK:
 * - P300-PORTFOLIO-INQUIRY linked to INQPORT (VSAM-based portfolio lookup)
 * - P400-HISTORY-INQUIRY linked to INQHIST (DB2 cursor-based history query)
 *
 * This service encapsulates the same business logic using Spring Data JPA
 * repositories instead of VSAM file I/O and embedded SQL.
 */
@Service
@Transactional(readOnly = true)
public class PortfolioInquiryService {

    private final PortfolioRepository portfolioRepository;
    private final PositionHistoryRepository positionHistoryRepository;

    public PortfolioInquiryService(PortfolioRepository portfolioRepository,
                                   PositionHistoryRepository positionHistoryRepository) {
        this.portfolioRepository = portfolioRepository;
        this.positionHistoryRepository = positionHistoryRepository;
    }

    /**
     * Replaces P300-PORTFOLIO-INQUIRY paragraph from INQONLN.cbl (lines 101-108).
     *
     * Original COBOL:
     * <pre>
     *     EXEC CICS LINK PROGRAM('INQPORT')
     *               COMMAREA(WS-COMMAREA)
     *               LENGTH(LENGTH OF WS-COMMAREA)
     *               RESP(WS-RESPONSE-CODE)
     *     END-EXEC.
     * </pre>
     *
     * INQPORT would read the portfolio from the VSAM KSDS file using the
     * portfolio ID as the key. If the record was not found (VSAM status '23'),
     * it would set an error in the COMMAREA.
     *
     * Here we use {@link PortfolioRepository#findByKeyPortfolioId(String)}
     * and throw {@link PortfolioNotFoundException} if no records are found,
     * which maps to HTTP 404 via the GlobalExceptionHandler.
     *
     * @param portfolioId the portfolio identifier (up to 8 characters, maps to PORT-ID)
     * @return list of matching portfolio records
     * @throws PortfolioNotFoundException if no portfolio is found (VSAM status 23)
     */
    public List<Portfolio> getPortfolio(String portfolioId) {
        List<Portfolio> portfolios = portfolioRepository.findByKeyPortfolioId(portfolioId);
        if (portfolios.isEmpty()) {
            throw new PortfolioNotFoundException(portfolioId);
        }
        return portfolios;
    }

    /**
     * Replaces P400-HISTORY-INQUIRY paragraph from INQONLN.cbl (lines 110-117).
     *
     * Original COBOL:
     * <pre>
     *     EXEC CICS LINK PROGRAM('INQHIST')
     *               COMMAREA(WS-COMMAREA)
     *               LENGTH(LENGTH OF WS-COMMAREA)
     *               RESP(WS-RESPONSE-CODE)
     *     END-EXEC.
     * </pre>
     *
     * INQHIST opened a DB2 cursor on the POSHIST table filtered by
     * PORTFOLIO_ID and TRANS_DATE range, then fetched rows into the COMMAREA.
     *
     * Here we first validate the portfolio exists (just like INQHIST would
     * check the VSAM file before querying DB2), then query the
     * PositionHistoryRepository for records in the date range.
     *
     * @param portfolioId the portfolio identifier
     * @param from        start date (inclusive)
     * @param to          end date (inclusive)
     * @return list of position history records
     * @throws PortfolioNotFoundException if the portfolio does not exist
     */
    public List<PositionHistory> getPortfolioHistory(String portfolioId,
                                                     LocalDate from, LocalDate to) {
        // Validate portfolio exists first (mirrors INQHIST validation step)
        List<Portfolio> portfolios = portfolioRepository.findByKeyPortfolioId(portfolioId);
        if (portfolios.isEmpty()) {
            throw new PortfolioNotFoundException(portfolioId);
        }

        return positionHistoryRepository.findByPortfolioIdAndTransDateBetween(
                portfolioId, from, to);
    }
}
