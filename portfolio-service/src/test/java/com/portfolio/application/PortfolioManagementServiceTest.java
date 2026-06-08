package com.portfolio.application;

import com.portfolio.domain.exception.ValidationException;
import com.portfolio.domain.model.ClientType;
import com.portfolio.domain.model.Portfolio;
import com.portfolio.domain.model.PortfolioStatus;
import com.portfolio.domain.repository.PortfolioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PortfolioManagementServiceTest {

    @Autowired
    private PortfolioManagementService service;

    @Autowired
    private PortfolioRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    // --- Create ---

    @Nested
    class CreatePortfolioTests {

        @Test
        void shouldCreatePortfolioSuccessfully() {
            Portfolio p = service.createPortfolio("PORT0001", "ACCT000001", "John Doe", ClientType.INDIVIDUAL);

            assertNotNull(p);
            assertEquals("PORT0001", p.getPortfolioId());
            assertEquals("ACCT000001", p.getAccountNumber());
            assertEquals("John Doe", p.getClientName());
            assertEquals(ClientType.INDIVIDUAL, p.getClientType());
            assertEquals(PortfolioStatus.ACTIVE, p.getStatus());
            assertNotNull(p.getCreateDate());
            assertNotNull(p.getLastMaintenance());

            assertTrue(repository.findById("PORT0001").isPresent());
        }

        @Test
        void shouldRejectBlankPortfolioId() {
            ValidationException ex = assertThrows(ValidationException.class,
                    () -> service.createPortfolio("", "ACCT000001", "John Doe", ClientType.INDIVIDUAL));
            assertTrue(ex.getMessage().contains("blank"));
        }

        @Test
        void shouldRejectPortfolioIdNotEightChars() {
            ValidationException ex = assertThrows(ValidationException.class,
                    () -> service.createPortfolio("SHORT", "ACCT000001", "John Doe", ClientType.INDIVIDUAL));
            assertTrue(ex.getMessage().contains("8 characters"));
        }

        @Test
        void shouldRejectBlankAccountNumber() {
            ValidationException ex = assertThrows(ValidationException.class,
                    () -> service.createPortfolio("PORT0001", "", "John Doe", ClientType.INDIVIDUAL));
            assertTrue(ex.getMessage().contains("blank"));
        }

        @Test
        void shouldRejectAccountNumberNotTenChars() {
            ValidationException ex = assertThrows(ValidationException.class,
                    () -> service.createPortfolio("PORT0001", "SHORT", "John Doe", ClientType.INDIVIDUAL));
            assertTrue(ex.getMessage().contains("10 characters"));
        }

        @Test
        void shouldRejectBlankClientName() {
            ValidationException ex = assertThrows(ValidationException.class,
                    () -> service.createPortfolio("PORT0001", "ACCT000001", "", ClientType.INDIVIDUAL));
            assertTrue(ex.getMessage().contains("blank"));
        }

        @Test
        void shouldRejectDuplicatePortfolioId() {
            service.createPortfolio("PORT0001", "ACCT000001", "John Doe", ClientType.INDIVIDUAL);

            ValidationException ex = assertThrows(ValidationException.class,
                    () -> service.createPortfolio("PORT0001", "ACCT000002", "Jane Doe", ClientType.CORPORATE));
            assertEquals(22, ex.getValidationCode());
            assertTrue(ex.getMessage().contains("already exists"));
        }
    }

    // --- Read ---

    @Nested
    class ReadPortfolioTests {

        @Test
        void shouldReadExistingPortfolio() {
            service.createPortfolio("PORT0001", "ACCT000001", "John Doe", ClientType.INDIVIDUAL);

            Portfolio p = service.readPortfolio("PORT0001");

            assertEquals("PORT0001", p.getPortfolioId());
            assertEquals("John Doe", p.getClientName());
        }

        @Test
        void shouldThrowWhenPortfolioNotFound() {
            ValidationException ex = assertThrows(ValidationException.class,
                    () -> service.readPortfolio("NONEXIST"));
            assertEquals(23, ex.getValidationCode());
        }

        @Test
        void shouldReadByAccountNumber() {
            service.createPortfolio("PORT0001", "ACCT000001", "John Doe", ClientType.INDIVIDUAL);

            Optional<Portfolio> opt = service.readPortfolioByAccount("ACCT000001");

            assertTrue(opt.isPresent());
            assertEquals("PORT0001", opt.get().getPortfolioId());
        }

        @Test
        void shouldReturnEmptyForUnknownAccount() {
            Optional<Portfolio> opt = service.readPortfolioByAccount("ACCT999999");
            assertTrue(opt.isEmpty());
        }
    }

    // --- Update ---

    @Nested
    class UpdatePortfolioTests {

        @Test
        void shouldUpdatePortfolioFields() {
            service.createPortfolio("PORT0001", "ACCT000001", "John Doe", ClientType.INDIVIDUAL);

            Portfolio updated = service.updatePortfolio("PORT0001", "Jane Smith", ClientType.CORPORATE);

            assertEquals("Jane Smith", updated.getClientName());
            assertEquals(ClientType.CORPORATE, updated.getClientType());
            assertNotNull(updated.getLastMaintenance());
        }

        @Test
        void shouldThrowWhenUpdatingNonexistentPortfolio() {
            ValidationException ex = assertThrows(ValidationException.class,
                    () -> service.updatePortfolio("NONEXIST", "Name", ClientType.INDIVIDUAL));
            assertEquals(23, ex.getValidationCode());
        }

        @Test
        void shouldThrowWhenUpdatingClosedPortfolio() {
            service.createPortfolio("PORT0001", "ACCT000001", "John Doe", ClientType.INDIVIDUAL);
            service.deletePortfolio("PORT0001", "SYSTEM");

            ValidationException ex = assertThrows(ValidationException.class,
                    () -> service.updatePortfolio("PORT0001", "New Name", ClientType.TRUST));
            assertEquals(8, ex.getValidationCode());
            assertTrue(ex.getMessage().contains("CLOSED"));
        }
    }

    // --- Delete (soft) ---

    @Nested
    class DeletePortfolioTests {

        @Test
        void shouldSoftDeletePortfolio() {
            service.createPortfolio("PORT0001", "ACCT000001", "John Doe", ClientType.INDIVIDUAL);

            Portfolio deleted = service.deletePortfolio("PORT0001", "ADMIN01");

            assertEquals(PortfolioStatus.CLOSED, deleted.getStatus());
            assertEquals("ADMIN01", deleted.getLastUser());
            assertNotNull(deleted.getLastMaintenance());
            // record still exists (soft delete)
            assertTrue(repository.findById("PORT0001").isPresent());
        }

        @Test
        void shouldThrowWhenDeletingNonexistentPortfolio() {
            ValidationException ex = assertThrows(ValidationException.class,
                    () -> service.deletePortfolio("NONEXIST", "ADMIN01"));
            assertEquals(23, ex.getValidationCode());
        }

        @Test
        void shouldThrowWhenDeletingAlreadyClosedPortfolio() {
            service.createPortfolio("PORT0001", "ACCT000001", "John Doe", ClientType.INDIVIDUAL);
            service.deletePortfolio("PORT0001", "ADMIN01");

            ValidationException ex = assertThrows(ValidationException.class,
                    () -> service.deletePortfolio("PORT0001", "ADMIN01"));
            assertEquals(8, ex.getValidationCode());
            assertTrue(ex.getMessage().contains("CLOSED"));
        }
    }

    // --- List ---

    @Nested
    class ListPortfoliosTests {

        @Test
        void shouldReturnEmptyListWhenNoPortfolios() {
            List<Portfolio> list = service.listPortfolios();
            assertTrue(list.isEmpty());
        }

        @Test
        void shouldListAllPortfolios() {
            service.createPortfolio("PORT0001", "ACCT000001", "John Doe", ClientType.INDIVIDUAL);
            service.createPortfolio("PORT0002", "ACCT000002", "Jane Smith", ClientType.CORPORATE);
            service.createPortfolio("PORT0003", "ACCT000003", "Trust Fund", ClientType.TRUST);

            List<Portfolio> list = service.listPortfolios();

            assertEquals(3, list.size());
        }
    }
}
