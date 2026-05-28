package com.clbs.portfolio.service.report;

import com.clbs.portfolio.config.ReportConfig;
import com.clbs.portfolio.entity.Position;
import com.clbs.portfolio.entity.TransactionRecord;
import com.clbs.portfolio.enums.EntityStatus;
import com.clbs.portfolio.enums.TransactionType;
import com.clbs.portfolio.repository.PositionRepository;
import com.clbs.portfolio.repository.TransactionRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PositionReportServiceTest {

    @Mock
    private PositionRepository positionRepository;

    @Mock
    private TransactionRecordRepository transactionRecordRepository;

    @Mock
    private ReportConfig reportConfig;

    @InjectMocks
    private PositionReportService positionReportService;

    @TempDir
    Path tempDir;

    private List<Position> testPositions;
    private List<TransactionRecord> testTransactions;

    @BeforeEach
    void setUp() {
        when(reportConfig.getOutputDirectory()).thenReturn(tempDir.toString());

        testPositions = List.of(
                Position.builder()
                        .portfolioId("PORT0001")
                        .investmentId("INV001")
                        .quantity(new BigDecimal("100.0000"))
                        .costBasis(new BigDecimal("10000.00"))
                        .marketValue(new BigDecimal("11500.00"))
                        .currency("USD")
                        .status(EntityStatus.ACTIVE)
                        .positionDate(LocalDate.now())
                        .build(),
                Position.builder()
                        .portfolioId("PORT0001")
                        .investmentId("INV002")
                        .quantity(new BigDecimal("50.0000"))
                        .costBasis(new BigDecimal("5000.00"))
                        .marketValue(new BigDecimal("4500.00"))
                        .currency("USD")
                        .status(EntityStatus.ACTIVE)
                        .positionDate(LocalDate.now())
                        .build()
        );

        testTransactions = List.of(
                TransactionRecord.builder()
                        .transactionDate(LocalDate.now())
                        .portfolioId("PORT0001")
                        .investmentId("INV001")
                        .transactionType(TransactionType.BUY)
                        .quantity(new BigDecimal("10.0000"))
                        .price(new BigDecimal("115.0000"))
                        .amount(new BigDecimal("1150.00"))
                        .currency("USD")
                        .build()
        );
    }

    @Test
    void generateTextReport_shouldContainHeader() {
        when(positionRepository.findByStatus(EntityStatus.ACTIVE)).thenReturn(testPositions);
        when(transactionRecordRepository.findByTransactionDateBetween(any(), any()))
                .thenReturn(testTransactions);

        String report = positionReportService.generateReport(LocalDate.now(), "text");

        assertThat(report).contains("DAILY POSITION REPORT");
        assertThat(report).contains("REPORT DATE:");
        assertThat(report).contains("PORTFOLIO POSITION SUMMARY");
    }

    @Test
    void generateTextReport_shouldContainPositionDetails() {
        when(positionRepository.findByStatus(EntityStatus.ACTIVE)).thenReturn(testPositions);
        when(transactionRecordRepository.findByTransactionDateBetween(any(), any()))
                .thenReturn(testTransactions);

        String report = positionReportService.generateReport(LocalDate.now(), "text");

        assertThat(report).contains("PORT0001");
        assertThat(report).contains("INV001");
        assertThat(report).contains("INV002");
    }

    @Test
    void generateTextReport_shouldContainTransactionActivity() {
        when(positionRepository.findByStatus(EntityStatus.ACTIVE)).thenReturn(testPositions);
        when(transactionRecordRepository.findByTransactionDateBetween(any(), any()))
                .thenReturn(testTransactions);

        String report = positionReportService.generateReport(LocalDate.now(), "text");

        assertThat(report).contains("TRANSACTION ACTIVITY");
    }

    @Test
    void generateTextReport_shouldContainExceptionReport() {
        when(positionRepository.findByStatus(EntityStatus.ACTIVE)).thenReturn(testPositions);
        when(transactionRecordRepository.findByTransactionDateBetween(any(), any()))
                .thenReturn(testTransactions);

        String report = positionReportService.generateReport(LocalDate.now(), "text");

        assertThat(report).contains("EXCEPTION REPORT");
    }

    @Test
    void generateTextReport_shouldContainSummaryTotals() {
        when(positionRepository.findByStatus(EntityStatus.ACTIVE)).thenReturn(testPositions);
        when(transactionRecordRepository.findByTransactionDateBetween(any(), any()))
                .thenReturn(testTransactions);

        String report = positionReportService.generateReport(LocalDate.now(), "text");

        assertThat(report).contains("SUMMARY TOTALS");
        assertThat(report).contains("Total Positions:");
        assertThat(report).contains("Total Market Value:");
    }

    @Test
    void generateCsvReport_shouldContainHeaders() {
        when(positionRepository.findByStatus(EntityStatus.ACTIVE)).thenReturn(testPositions);
        when(transactionRecordRepository.findByTransactionDateBetween(any(), any()))
                .thenReturn(testTransactions);

        String report = positionReportService.generateReport(LocalDate.now(), "csv");

        assertThat(report).contains("Portfolio ID,Investment ID,Quantity");
        assertThat(report).contains("TRANSACTION ACTIVITY");
        assertThat(report).contains("SUMMARY");
    }

    @Test
    void generateTextReport_shouldDetectLargeValueChange() {
        List<Position> positions = List.of(
                Position.builder()
                        .portfolioId("PORT0002")
                        .investmentId("INV003")
                        .quantity(new BigDecimal("100.0000"))
                        .costBasis(new BigDecimal("10000.00"))
                        .marketValue(new BigDecimal("15000.00"))
                        .currency("USD")
                        .status(EntityStatus.ACTIVE)
                        .positionDate(LocalDate.now())
                        .build()
        );

        when(positionRepository.findByStatus(EntityStatus.ACTIVE)).thenReturn(positions);
        when(transactionRecordRepository.findByTransactionDateBetween(any(), any()))
                .thenReturn(List.of());

        String report = positionReportService.generateReport(LocalDate.now(), "text");

        assertThat(report).contains("LARGE VALUE CHANGE");
    }

    @Test
    void generateTextReport_shouldDetectNoActivity() {
        when(positionRepository.findByStatus(EntityStatus.ACTIVE)).thenReturn(testPositions);
        when(transactionRecordRepository.findByTransactionDateBetween(any(), any()))
                .thenReturn(List.of());

        String report = positionReportService.generateReport(LocalDate.now(), "text");

        assertThat(report).contains("NO ACTIVITY IN PERIOD");
    }
}
