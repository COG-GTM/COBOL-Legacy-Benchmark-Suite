package com.clbs.portfolio.service;

import com.clbs.portfolio.domain.Portfolio;
import com.clbs.portfolio.domain.Position;
import com.clbs.portfolio.domain.PositionHistory;
import com.clbs.portfolio.exception.NotFoundException;
import com.clbs.portfolio.repository.PortfolioRepository;
import com.clbs.portfolio.repository.PositionHistoryRepository;
import com.clbs.portfolio.repository.PositionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Online inquiries: INQPORT (positions for an account) and INQHIST (last 10 history rows for an account). */
@Service
@Transactional(readOnly = true)
public class InquiryService {

    static final String POSITION_NOT_FOUND = "Position not found for account";

    private final PortfolioRepository portfolioRepository;
    private final PositionRepository positionRepository;
    private final PositionHistoryRepository historyRepository;

    public InquiryService(PortfolioRepository portfolioRepository, PositionRepository positionRepository,
                          PositionHistoryRepository historyRepository) {
        this.portfolioRepository = portfolioRepository;
        this.positionRepository = positionRepository;
        this.historyRepository = historyRepository;
    }

    /** INQPORT P200-GET-POSITION; NOTFND maps to P900-NOT-FOUND. */
    public List<Position> positionsForAccount(String accountNo) {
        List<String> portfolioIds = portfolioRepository.findByKeyAccountNo(accountNo).stream()
                .map(Portfolio::getPortfolioId)
                .toList();
        List<Position> positions = portfolioIds.isEmpty() ? List.of()
                : positionRepository.findByPortfolioIdInOrderByPortfolioIdAscInvestmentIdAscPositionDateDesc(portfolioIds);
        if (positions.isEmpty()) {
            throw new NotFoundException(POSITION_NOT_FOUND);
        }
        return positions;
    }

    /** INQHIST P200-GET-HISTORY: newest first, at most 10 rows (WS-HISTORY-ENTRY OCCURS 10). */
    public List<PositionHistory> historyForAccount(String accountNo) {
        return historyRepository.findTop10ByAccountNoOrderByTransDateDescTransTimeDescIdDesc(accountNo);
    }
}
