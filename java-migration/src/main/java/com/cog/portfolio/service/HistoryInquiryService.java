package com.cog.portfolio.service;

import com.cog.portfolio.domain.PositionHistory;
import com.cog.portfolio.repository.PositionHistoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * INQHIST.cbl: cursor {@code SELECT ... FROM POSHIST WHERE ACCOUNT_NO = ? ORDER BY TRANS_DATE DESC}
 * fetched into the 10 detail lines of HISMAP. The CURSMGR cursor becomes a Page.
 */
@Service
public class HistoryInquiryService {

    /** HISMAP has 10 detail rows (HISTL1..HISTL10). */
    public static final int PAGE_SIZE = 10;

    private final PositionHistoryRepository repository;

    public HistoryInquiryService(PositionHistoryRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Page<PositionHistory> findByAccount(String accountNo, int page) {
        return repository.findByIdAccountNoOrderByIdTransDateDescIdTransTimeDesc(
                accountNo, PageRequest.of(Math.max(page, 0), PAGE_SIZE));
    }
}
