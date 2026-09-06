package com.cog.portfolio.service;

import com.cog.portfolio.common.PortfolioException;
import com.cog.portfolio.domain.Position;
import com.cog.portfolio.repository.PositionRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * INQPORT.cbl: READ POSFILE by account number and populate POSMAP
 * (account, fund id, fund name, units, cost basis, market value).
 */
@Service
public class PositionInquiryService {

    public static final String MSG_NOT_FOUND = "Account not found";

    private final PositionRepository positionRepository;

    public PositionInquiryService(PositionRepository positionRepository) {
        this.positionRepository = positionRepository;
    }

    @Transactional(readOnly = true)
    public List<Position> findByAccount(String accountNo) {
        List<Position> positions = positionRepository.findByAccountNo(accountNo);
        if (positions.isEmpty()) {
            throw PortfolioException.notFound(MSG_NOT_FOUND);
        }
        return positions;
    }
}
