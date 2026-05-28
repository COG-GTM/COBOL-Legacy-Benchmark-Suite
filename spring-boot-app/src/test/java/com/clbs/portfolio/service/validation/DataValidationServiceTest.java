package com.clbs.portfolio.service.validation;

import com.clbs.portfolio.entity.Position;
import com.clbs.portfolio.entity.TransactionRecord;
import com.clbs.portfolio.enums.EntityStatus;
import com.clbs.portfolio.enums.TransactionType;
import com.clbs.portfolio.repository.PositionRepository;
import com.clbs.portfolio.repository.TransactionRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataValidationServiceTest {

    @Mock
    private PositionRepository positionRepository;

    @Mock
    private TransactionRecordRepository transactionRecordRepository;

    private DataValidationService dataValidationService;

    @BeforeEach
    void setUp() {
        IntegrityValidator integrityValidator = new IntegrityValidator(
                positionRepository, transactionRecordRepository);
        CrossReferenceValidator crossRefValidator = new CrossReferenceValidator(
                positionRepository, transactionRecordRepository);
        FormatValidator formatValidator = new FormatValidator(
                positionRepository, transactionRecordRepository);
        BalanceReconciliationValidator balanceValidator = new BalanceReconciliationValidator(
                positionRepository, transactionRecordRepository);

        dataValidationService = new DataValidationService(
                List.of(integrityValidator, crossRefValidator, formatValidator, balanceValidator));
    }

    @Test
    void validate_integrityCheck_noOrphans() {
        when(positionRepository.findOrphanedPositions()).thenReturn(List.of());
        when(transactionRecordRepository.findOrphanedTransactions()).thenReturn(List.of());
        when(positionRepository.count()).thenReturn(5L);
        when(transactionRecordRepository.count()).thenReturn(10L);

        Map<String, ValidationResult> results = dataValidationService.validate(List.of("INTEGRITY"));

        assertThat(results).containsKey("INTEGRITY");
        assertThat(results.get("INTEGRITY").hasErrors()).isFalse();
    }

    @Test
    void validate_integrityCheck_withOrphans() {
        Position orphan = Position.builder()
                .portfolioId("ORPHAN01")
                .investmentId("INV001")
                .build();
        when(positionRepository.findOrphanedPositions()).thenReturn(List.of(orphan));
        when(transactionRecordRepository.findOrphanedTransactions()).thenReturn(List.of());
        when(positionRepository.count()).thenReturn(5L);
        when(transactionRecordRepository.count()).thenReturn(10L);

        Map<String, ValidationResult> results = dataValidationService.validate(List.of("INTEGRITY"));

        assertThat(results.get("INTEGRITY").hasErrors()).isTrue();
        assertThat(results.get("INTEGRITY").getErrors()).hasSize(1);
        assertThat(results.get("INTEGRITY").getErrors().get(0).description())
                .contains("non-existent portfolio");
    }

    @Test
    void validate_formatCheck_validData() {
        Position pos = Position.builder()
                .id(1L)
                .portfolioId("PORT0001")
                .investmentId("INV001")
                .positionDate(LocalDate.now())
                .quantity(new BigDecimal("100.0000"))
                .currency("USD")
                .status(EntityStatus.ACTIVE)
                .build();
        when(positionRepository.findAll()).thenReturn(List.of(pos));

        TransactionRecord trn = TransactionRecord.builder()
                .portfolioId("PORT0001")
                .transactionDate(LocalDate.now())
                .transactionType(TransactionType.BUY)
                .amount(new BigDecimal("1000.00"))
                .status("D")
                .currency("USD")
                .build();
        when(transactionRecordRepository.findAll()).thenReturn(List.of(trn));

        Map<String, ValidationResult> results = dataValidationService.validate(List.of("FORMAT"));

        assertThat(results.get("FORMAT").hasErrors()).isFalse();
    }

    @Test
    void validate_formatCheck_invalidCurrency() {
        Position pos = Position.builder()
                .id(1L)
                .portfolioId("PORT0001")
                .investmentId("INV001")
                .positionDate(LocalDate.now())
                .quantity(new BigDecimal("100.0000"))
                .currency("XYZ")
                .status(EntityStatus.ACTIVE)
                .build();
        when(positionRepository.findAll()).thenReturn(List.of(pos));
        when(transactionRecordRepository.findAll()).thenReturn(List.of());

        Map<String, ValidationResult> results = dataValidationService.validate(List.of("FORMAT"));

        assertThat(results.get("FORMAT").hasErrors()).isTrue();
        assertThat(results.get("FORMAT").getErrors().get(0).description())
                .contains("invalid currency");
    }

    @Test
    void validate_balanceCheck_matchingTotals() {
        Position pos = Position.builder()
                .portfolioId("PORT0001")
                .investmentId("INV001")
                .costBasis(new BigDecimal("10000.00"))
                .status(EntityStatus.ACTIVE)
                .build();
        when(positionRepository.findByStatus(EntityStatus.ACTIVE)).thenReturn(List.of(pos));

        TransactionRecord trn = TransactionRecord.builder()
                .portfolioId("PORT0001")
                .investmentId("INV001")
                .transactionType(TransactionType.BUY)
                .amount(new BigDecimal("10000.00"))
                .build();
        when(transactionRecordRepository.findAll()).thenReturn(List.of(trn));

        Map<String, ValidationResult> results = dataValidationService.validate(List.of("BALANCE"));

        assertThat(results.get("BALANCE").hasErrors()).isFalse();
    }

    @Test
    void validate_unknownType_returnsError() {
        Map<String, ValidationResult> results = dataValidationService.validate(List.of("UNKNOWN"));

        assertThat(results).containsKey("UNKNOWN");
        assertThat(results.get("UNKNOWN").hasErrors()).isTrue();
    }

    @Test
    void validate_multipleTypes() {
        when(positionRepository.findOrphanedPositions()).thenReturn(List.of());
        when(transactionRecordRepository.findOrphanedTransactions()).thenReturn(List.of());
        when(positionRepository.count()).thenReturn(0L);
        when(transactionRecordRepository.count()).thenReturn(0L);
        when(positionRepository.findAll()).thenReturn(List.of());
        when(transactionRecordRepository.findAll()).thenReturn(List.of());

        Map<String, ValidationResult> results = dataValidationService.validate(
                List.of("INTEGRITY", "FORMAT"));

        assertThat(results).containsKeys("INTEGRITY", "FORMAT");
    }
}
