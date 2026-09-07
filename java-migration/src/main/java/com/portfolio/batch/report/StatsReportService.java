package com.portfolio.batch.report;

import com.portfolio.domain.Transaction;
import com.portfolio.repository.TransactionRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class StatsReportService {
  private final TransactionRepository transactionRepository;

  public StatsReportService(TransactionRepository transactionRepository) {
    this.transactionRepository = transactionRepository;
  }

  public ReportResult generate() {
    Map<String, Integer> counts = new LinkedHashMap<>();
    BigDecimal total = BigDecimal.ZERO;
    for (Transaction transaction : transactionRepository.findAll()) {
      String key = value(transaction.getTransactionType()) + "/" + value(transaction.getStatus());
      counts.merge(key, 1, Integer::sum);
      total =
          total.add(transaction.getAmount() == null ? BigDecimal.ZERO : transaction.getAmount());
    }
    List<String> lines = new ArrayList<>();
    lines.add("SYSTEM STATISTICS AND PERFORMANCE REPORT");
    counts.forEach((key, count) -> lines.add(key + "|" + count));
    lines.add("TOTAL_TRANSACTIONS|" + transactionRepository.count());
    lines.add("TOTAL_AMOUNT|" + total);
    return new ReportResult("RPTSTA00", lines, 0);
  }

  private String value(Object value) {
    return value == null ? "" : value.toString();
  }
}
