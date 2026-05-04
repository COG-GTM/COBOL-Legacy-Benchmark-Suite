package com.portfolio.web;

import com.portfolio.TestDataGenerator;
import com.portfolio.domain.Portfolio;
import com.portfolio.repository.PortfolioRepository;
import com.portfolio.repository.TransactionRepository;
import com.portfolio.repository.PositionRepository;
import com.portfolio.repository.PosHistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Inquiry Controller Test - validates web endpoints match COBOL INQONLN.cbl behavior.
 * Tests the replacement of CICS transaction PINQ with Spring MVC endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InquiryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private PositionRepository positionRepository;

    @Autowired
    private PosHistRepository posHistRepository;

    @BeforeEach
    void setUp() {
        posHistRepository.deleteAll();
        positionRepository.deleteAll();
        transactionRepository.deleteAll();
        portfolioRepository.deleteAll();
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testMenuPage() throws Exception {
        Portfolio p = TestDataGenerator.createTestPortfolio("MENU0001");
        portfolioRepository.save(p);

        mockMvc.perform(get("/menu"))
                .andExpect(status().isOk())
                .andExpect(view().name("menu"))
                .andExpect(model().attributeExists("portfolios"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testPositionsPage() throws Exception {
        Portfolio p = TestDataGenerator.createTestPortfolio("POS00001");
        portfolioRepository.save(p);

        mockMvc.perform(get("/portfolio/POS00001/positions"))
                .andExpect(status().isOk())
                .andExpect(view().name("positions"))
                .andExpect(model().attributeExists("positions"))
                .andExpect(model().attributeExists("accountNo"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testHistoryPage() throws Exception {
        Portfolio p = TestDataGenerator.createTestPortfolio("HIS00001");
        portfolioRepository.save(p);

        mockMvc.perform(get("/portfolio/HIS00001/history?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(view().name("history"))
                .andExpect(model().attributeExists("transactions"))
                .andExpect(model().attribute("currentPage", 0));
    }

    @Test
    void testUnauthenticatedAccessRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/menu"))
                .andExpect(status().is3xxRedirection());
    }
}
