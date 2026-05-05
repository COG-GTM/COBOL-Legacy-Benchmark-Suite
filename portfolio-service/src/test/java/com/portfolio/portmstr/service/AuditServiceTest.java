package com.portfolio.portmstr.service;

import com.portfolio.portmstr.model.AuditLog;
import com.portfolio.portmstr.model.PortfolioMaster;
import com.portfolio.portmstr.model.enums.AuditAction;
import com.portfolio.portmstr.model.enums.ClientType;
import com.portfolio.portmstr.model.enums.PortfolioStatus;
import com.portfolio.portmstr.repository.AuditLogRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;

/**
 * Tests for AuditService.
 * Verifies audit logging matches COBOL CALL 'AUDPROC' behavior.
 */
@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditService auditService;

    private PortfolioMaster portfolio;

    @BeforeEach
    void setUp() {
        portfolio = new PortfolioMaster();
        portfolio.setPortfolioId("PORT0001");
        portfolio.setAccountNo("1000000001");
        portfolio.setClientName("John Doe");
        portfolio.setClientType(ClientType.INDIVIDUAL);
        portfolio.setStatus(PortfolioStatus.ACTIVE);
        portfolio.setTotalValue(new BigDecimal("100000.00"));
    }

    @Test
    @DisplayName("logPortfolioCreate sets after-image and CREATE action")
    void logPortfolioCreate() {
        auditService.logPortfolioCreate(portfolio, "TESTUSER");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertEquals(AuditAction.CREATE, saved.getAuditAction());
        assertEquals("PORT0001", saved.getPortfolioId());
        assertEquals("TESTUSER", saved.getUserId());
        assertNotNull(saved.getAfterImage());
        assertNull(saved.getBeforeImage());
        assertNotNull(saved.getAuditTimestamp());
    }

    @Test
    @DisplayName("logPortfolioUpdate sets both before and after images")
    void logPortfolioUpdate() {
        PortfolioMaster updated = new PortfolioMaster();
        updated.setPortfolioId("PORT0001");
        updated.setAccountNo("1000000001");
        updated.setClientName("Jane Doe");
        updated.setClientType(ClientType.INDIVIDUAL);
        updated.setStatus(PortfolioStatus.ACTIVE);
        updated.setTotalValue(new BigDecimal("200000.00"));

        auditService.logPortfolioUpdate(portfolio, updated, "TESTUSER");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertEquals(AuditAction.UPDATE, saved.getAuditAction());
        assertNotNull(saved.getBeforeImage());
        assertNotNull(saved.getAfterImage());
    }

    @Test
    @DisplayName("logPortfolioDelete sets before-image with reason")
    void logPortfolioDelete() {
        auditService.logPortfolioDelete(portfolio, "TESTUSER", "03");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertEquals(AuditAction.DELETE, saved.getAuditAction());
        assertNotNull(saved.getBeforeImage());
        assertNull(saved.getAfterImage());
        assertNotNull(saved.getMessage());
        assertTrue(saved.getMessage().contains("03"));
    }

    @Test
    @DisplayName("logTransaction records transaction details")
    void logTransaction() {
        auditService.logTransaction("PORT0001", "1000000001", "BU", "5000.00", "TESTUSER");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertEquals("PORT0001", saved.getPortfolioId());
        assertEquals("PORTTRAN", saved.getProgramName());
        assertNotNull(saved.getMessage());
        assertTrue(saved.getMessage().contains("BU"));
        assertTrue(saved.getMessage().contains("5000.00"));
    }

    private void assertTrue(boolean condition) {
        org.junit.jupiter.api.Assertions.assertTrue(condition);
    }
}
