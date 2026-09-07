package com.portfolio.batch;

import com.portfolio.batch.report.ReportResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

public class ReportTasklet implements Tasklet {
  private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.BASIC_ISO_DATE;

  private final Supplier<ReportResult> reportSupplier;
  private final Path reportDirectory;

  public ReportTasklet(Supplier<ReportResult> reportSupplier, Path reportDirectory) {
    this.reportSupplier = reportSupplier;
    this.reportDirectory = reportDirectory;
  }

  @Override
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext)
      throws IOException {
    ReportResult result = reportSupplier.get();
    Files.createDirectories(reportDirectory);
    Path reportFile =
        reportDirectory.resolve(
            result.programId() + "-" + LocalDate.now().format(FILE_DATE) + ".txt");
    Files.write(reportFile, result.lines());
    chunkContext
        .getStepContext()
        .getStepExecution()
        .getExecutionContext()
        .putInt("returnCode", result.returnCode());
    contribution.setExitStatus(new ExitStatus("COMPLETED", "RC=" + result.returnCode()));
    return RepeatStatus.FINISHED;
  }
}
