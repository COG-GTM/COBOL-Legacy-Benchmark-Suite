package com.portfolio.infrastructure.audit;

import com.portfolio.domain.event.PortfolioCreatedEvent;
import com.portfolio.domain.event.TransactionProcessedEvent;
import com.portfolio.domain.model.AuditAction;
import com.portfolio.domain.model.AuditType;
import com.portfolio.domain.model.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditRepository auditRepository;

    @Captor
    private ArgumentCaptor<AuditRecord> recordCaptor;

    private AuditService auditService;

    @BeforeEach
    void setUp() {
        auditService = new AuditService(auditRepository);
    }

    @Test
    void buyTransactionMapsToCreateAction() {
        TransactionProcessedEvent event = new TransactionProcessedEvent(
                "PORT001", "ACCT00001", TransactionType.BUY,
                new BigDecimal("100.00"), "USER01", LocalDateTime.now());

        auditService.onTransactionProcessed(event);

        verify(auditRepository).save(recordCaptor.capture());
        AuditRecord record = recordCaptor.getValue();
        assertEquals(AuditAction.CREATE, record.getAction());
        assertEquals(AuditType.TRANSACTION, record.getAuditType());
        assertEquals("SUCC", record.getStatus());
        assertEquals("PORT001", record.getPortfolioId());
        assertEquals("ACCT00001", record.getAccountNumber());
        assertEquals("PORTTRAN", record.getProgram());
    }

    @Test
    void sellTransactionMapsToDeleteAction() {
        TransactionProcessedEvent event = new TransactionProcessedEvent(
                "PORT002", "ACCT00002", TransactionType.SELL,
                new BigDecimal("50.00"), "USER02", LocalDateTime.now());

        auditService.onTransactionProcessed(event);

        verify(auditRepository).save(recordCaptor.capture());
        assertEquals(AuditAction.DELETE, recordCaptor.getValue().getAction());
    }

    @Test
    void transferTransactionMapsToUpdateAction() {
        TransactionProcessedEvent event = new TransactionProcessedEvent(
                "PORT003", "ACCT00003", TransactionType.TRANSFER,
                new BigDecimal("200.00"), "USER03", LocalDateTime.now());

        auditService.onTransactionProcessed(event);

        verify(auditRepository).save(recordCaptor.capture());
        assertEquals(AuditAction.UPDATE, recordCaptor.getValue().getAction());
    }

    @Test
    void feeTransactionMapsToUpdateAction() {
        TransactionProcessedEvent event = new TransactionProcessedEvent(
                "PORT004", "ACCT00004", TransactionType.FEE,
                new BigDecimal("10.00"), "USER04", LocalDateTime.now());

        auditService.onTransactionProcessed(event);

        verify(auditRepository).save(recordCaptor.capture());
        assertEquals(AuditAction.UPDATE, recordCaptor.getValue().getAction());
    }

    @Test
    void portfolioCreatedEventCreatesAuditWithCreateAction() {
        PortfolioCreatedEvent event = new PortfolioCreatedEvent(
                "PORT005", "ACCT00005", "USER05", LocalDateTime.now());

        auditService.onPortfolioCreated(event);

        verify(auditRepository).save(recordCaptor.capture());
        AuditRecord record = recordCaptor.getValue();
        assertEquals(AuditAction.CREATE, record.getAction());
        assertEquals(AuditType.TRANSACTION, record.getAuditType());
        assertEquals("SUCC", record.getStatus());
        assertEquals("PORT005", record.getPortfolioId());
        assertEquals("ACCT00005", record.getAccountNumber());
    }

    @Test
    void mapTransactionTypeToAction_coversAllTypes() {
        assertEquals(AuditAction.CREATE, AuditService.mapTransactionTypeToAction(TransactionType.BUY));
        assertEquals(AuditAction.DELETE, AuditService.mapTransactionTypeToAction(TransactionType.SELL));
        assertEquals(AuditAction.UPDATE, AuditService.mapTransactionTypeToAction(TransactionType.TRANSFER));
        assertEquals(AuditAction.UPDATE, AuditService.mapTransactionTypeToAction(TransactionType.FEE));
    }
}
