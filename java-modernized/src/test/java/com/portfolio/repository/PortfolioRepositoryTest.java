package com.portfolio.repository;

import com.portfolio.model.Portfolio;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Repository-level tests using @DataJpaTest with H2.
 * Verifies JPA mappings and custom finder methods.
 */
@DataJpaTest
class PortfolioRepositoryTest {

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Test
    void testSeedDataCount() {
        long count = portfolioRepository.count();
        assertEquals(10, count, "Seed migration should insert 10 records");
    }

    @Test
    void testFindById() {
        Optional<Portfolio> result = portfolioRepository.findById("PORT0001");
        assertTrue(result.isPresent());
        Portfolio p = result.get();
        assertEquals("Acme Corporation", p.getClientName());
        assertEquals("C", p.getClientType());
        assertEquals(0, new BigDecimal("1250000.00").compareTo(p.getTotalValue()));
    }

    @Test
    void testFindByStatus() {
        List<Portfolio> active = portfolioRepository.findByStatus("A");
        assertFalse(active.isEmpty());
        active.forEach(p -> assertEquals("A", p.getStatus()));

        List<Portfolio> closed = portfolioRepository.findByStatus("C");
        assertEquals(1, closed.size());
        assertEquals("PORT0007", closed.get(0).getPortId());
    }

    @Test
    void testFindByClientType() {
        List<Portfolio> corporate = portfolioRepository.findByClientType("C");
        assertFalse(corporate.isEmpty());
        corporate.forEach(p -> assertEquals("C", p.getClientType()));

        List<Portfolio> trust = portfolioRepository.findByClientType("T");
        assertFalse(trust.isEmpty());
        trust.forEach(p -> assertEquals("T", p.getClientType()));
    }

    @Test
    void testFindByAccountNo() {
        List<Portfolio> results = portfolioRepository.findByAccountNo("1000000001");
        assertEquals(1, results.size());
        assertEquals("PORT0001", results.get(0).getPortId());
    }

    @Test
    void testExistsByPortId() {
        assertTrue(portfolioRepository.existsByPortId("PORT0001"));
        assertFalse(portfolioRepository.existsByPortId("PORT9999"));
    }

    @Test
    void testCrudOperations() {
        Portfolio newPort = new Portfolio();
        newPort.setPortId("PORT0100");
        newPort.setAccountNo("5555555555");
        newPort.setClientName("CRUD Test");
        newPort.setClientType("I");
        newPort.setCreateDate(LocalDate.now());
        newPort.setStatus("A");
        newPort.setTotalValue(new BigDecimal("100000.00"));
        newPort.setCashBalance(new BigDecimal("10000.00"));

        Portfolio saved = portfolioRepository.save(newPort);
        assertNotNull(saved);
        assertEquals("PORT0100", saved.getPortId());

        saved.setClientName("CRUD Test Updated");
        Portfolio updated = portfolioRepository.save(saved);
        assertEquals("CRUD Test Updated", updated.getClientName());

        portfolioRepository.deleteById("PORT0100");
        assertFalse(portfolioRepository.existsByPortId("PORT0100"));
    }

    @Test
    void testFirstSeedRecordFieldValues() {
        Portfolio p = portfolioRepository.findById("PORT0001").orElseThrow();
        assertEquals("PORT0001", p.getPortId());
        assertEquals("1000000001", p.getAccountNo());
        assertEquals("Acme Corporation", p.getClientName());
        assertEquals("C", p.getClientType());
        assertEquals(LocalDate.of(2024, 3, 20), p.getCreateDate());
        assertEquals("A", p.getStatus());
        assertEquals(0, new BigDecimal("1250000.00").compareTo(p.getTotalValue()));
        assertEquals(0, new BigDecimal("125000.00").compareTo(p.getCashBalance()));
        assertEquals("SYSTEM", p.getLastUser());
    }
}
