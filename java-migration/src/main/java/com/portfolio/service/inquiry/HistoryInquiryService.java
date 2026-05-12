package com.portfolio.service.inquiry;

import com.portfolio.model.entity.PositionHistory;
import com.portfolio.repository.PositionHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HistoryInquiryService {

    private static final Logger log = LoggerFactory.getLogger(HistoryInquiryService.class);
    private static final int PAGE_SIZE = 10;

    private final PositionHistoryRepository positionHistoryRepository;

    public HistoryInquiryService(PositionHistoryRepository positionHistoryRepository) {
        this.positionHistoryRepository = positionHistoryRepository;
    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000))
    @Transactional(readOnly = true)
    public Page<PositionHistory> lookup(String accountNo, int page) {
        log.debug("Looking up history for account: {} page: {}", accountNo, page);
        return positionHistoryRepository.findByAccountNoOrderByTransDateDescTransTimeDesc(
                accountNo, PageRequest.of(page, PAGE_SIZE));
    }
}
