package com.portfolio.batch.report;

import java.util.List;

public record ReportResult(String programId, List<String> lines, int returnCode) {}
