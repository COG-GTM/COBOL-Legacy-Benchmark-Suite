package com.portfolio.batch.report;

import com.portfolio.domain.ReturnCodeRecord;
import com.portfolio.repository.ReturnCodeRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ReturnCodeAnalysisService {
  private final ReturnCodeRepository returnCodeRepository;

  public ReturnCodeAnalysisService(ReturnCodeRepository returnCodeRepository) {
    this.returnCodeRepository = returnCodeRepository;
  }

  public ReportResult generate() {
    Map<String, Summary> summaries = new LinkedHashMap<>();
    for (ReturnCodeRecord record : returnCodeRepository.findAll()) {
      Summary summary = summaries.computeIfAbsent(record.getProgramId(), ignored -> new Summary());
      summary.count++;
      summary.maximum = Math.max(summary.maximum, record.getReturnCode());
      if (record.getReturnCode() > 0) {
        summary.failures++;
      }
    }
    List<String> lines = new ArrayList<>();
    lines.add("RETURN CODE ANALYSIS REPORT");
    summaries.forEach(
        (program, summary) ->
            lines.add(
                program
                    + "|count="
                    + summary.count
                    + "|max="
                    + summary.maximum
                    + "|failures="
                    + summary.failures));
    return new ReportResult("RTNANA00", lines, 0);
  }

  private static class Summary {
    private int count;
    private int maximum;
    private int failures;
  }
}
