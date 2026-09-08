package com.clbs.online;

import com.clbs.db2.PositionHistoryEntity;
import com.clbs.db2.PositionHistoryRepository;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * INQHIST — transaction history inquiry. The CURSMGR-driven cursor
 * {@code SELECT TRANS_DATE, TRANS_TYPE, TRANS_UNITS, TRANS_PRICE, TRANS_AMOUNT FROM POSHIST WHERE
 * ACCOUNT_NO = ? ORDER BY TRANS_DATE DESC} becomes a repository query; WS-HISTORY-TABLE OCCURS 10
 * becomes the page size.
 */
@Service
public class HistoryInquiryService {

    /** WS-HISTORY-TABLE OCCURS 10 TIMES. */
    public static final int PAGE_SIZE = 10;

    /** WS-HISTORY-ENTRY. */
    public record HistoryEntry(String transDate, String transType, java.math.BigDecimal units,
            java.math.BigDecimal price, java.math.BigDecimal amount) {
    }

    /** The commarea plus one page of history and the WS-MORE-HISTORY flag. */
    public record HistoryResult(InquiryCommArea commArea, List<HistoryEntry> entries,
            boolean moreRows) {
    }

    private final PositionHistoryRepository repository;

    public HistoryInquiryService(PositionHistoryRepository repository) {
        this.repository = repository;
    }

    /** P200-GET-HISTORY / P250-FETCH-HISTORY. */
    public HistoryResult inquire(InquiryCommArea commArea) {
        String account = commArea.getAccountNo() == null ? "" : commArea.getAccountNo().trim();
        List<PositionHistoryEntity> rows =
                repository.findByAccountNoOrderByTransDateDesc(account);

        List<HistoryEntry> entries = rows.stream()
                .limit(PAGE_SIZE)
                .map(row -> new HistoryEntry(row.getTransDate(), row.getTransType(),
                        row.getTransUnits(), row.getTransPrice(), row.getTransAmount()))
                .toList();

        return new HistoryResult(commArea, entries, rows.size() > PAGE_SIZE);
    }
}
