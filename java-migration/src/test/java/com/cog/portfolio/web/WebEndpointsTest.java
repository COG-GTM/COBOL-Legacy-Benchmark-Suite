package com.cog.portfolio.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.cog.portfolio.TestData;
import com.cog.portfolio.domain.PortfolioStatus;
import com.cog.portfolio.domain.PositionHistory;
import com.cog.portfolio.domain.PositionHistoryId;
import com.cog.portfolio.domain.TransactionType;
import com.cog.portfolio.repository.PortfolioRepository;
import com.cog.portfolio.repository.PositionHistoryRepository;
import com.cog.portfolio.repository.PositionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** INQONLN/INQSET screens (MVC) and PORTADD/PORTREAD/PORTUPDT/PORTDEL (REST). */
@SpringBootTest
@AutoConfigureMockMvc
class WebEndpointsTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    PortfolioRepository portfolioRepository;
    @Autowired
    PositionRepository positionRepository;
    @Autowired
    PositionHistoryRepository positionHistoryRepository;

    @BeforeEach
    void setUp() {
        cleanup();
        portfolioRepository.save(TestData.portfolio());
        positionRepository.save(TestData.position("123.4567", "1500.00"));
        for (int i = 0; i < 12; i++) {
            PositionHistory ph = new PositionHistory(new PositionHistoryId(TestData.ACCOUNT_NO, TestData.PORTFOLIO_ID,
                    LocalDate.of(2024, 1, 1).plusDays(i), LocalTime.of(10, 0)), TransactionType.BUY, TestData.INVESTMENT_ID);
            ph.setQuantity(new BigDecimal("1.000"));
            ph.setPrice(new BigDecimal("2.000"));
            ph.setAmount(new BigDecimal("2.00"));
            ph.setFees(BigDecimal.ZERO);
            ph.setTotalAmount(new BigDecimal("2.00"));
            ph.setCostBasis(new BigDecimal("2.00"));
            ph.setGainLoss(BigDecimal.ZERO);
            ph.setProcessDate(LocalDate.now());
            ph.setProcessTime(LocalTime.NOON);
            ph.setProgramId("TEST");
            ph.setUserId("TEST");
            positionHistoryRepository.save(ph);
        }
    }

    @AfterEach
    void cleanup() {
        positionHistoryRepository.deleteAll();
        positionRepository.deleteAll();
        portfolioRepository.deleteAll();
    }

    @Test
    void menuOptionsRouteLikeInqonln() throws Exception {
        mvc.perform(get("/menu").with(user("u").roles("user")))
                .andExpect(status().isOk()).andExpect(view().name("menu"))
                .andExpect(content().string(containsString("Portfolio Management System")));
        mvc.perform(get("/menu").param("option", "1").with(user("u").roles("user")))
                .andExpect(redirectedUrl("/inquiry/position"));
        mvc.perform(get("/menu").param("option", "2").with(user("u").roles("user")))
                .andExpect(redirectedUrl("/inquiry/history"));
        mvc.perform(get("/menu").param("option", "9").with(user("u").roles("user")))
                .andExpect(status().isOk()).andExpect(model().attribute("errmsg", "Invalid option selected"));
    }

    @Test
    void positionInquiryShowsPosmapFields() throws Exception {
        mvc.perform(get("/inquiry/position").param("accountNo", TestData.ACCOUNT_NO).with(user("u").roles("user")))
                .andExpect(status().isOk())
                .andExpect(view().name("position"))
                .andExpect(content().string(containsString("FUND001")))
                .andExpect(content().string(containsString("Test Fund")))
                .andExpect(content().string(containsString("123.4567")))
                .andExpect(content().string(containsString("1,500.00")));
    }

    @Test
    void positionInquiryUnknownAccountShowsError() throws Exception {
        mvc.perform(get("/inquiry/position").param("accountNo", "9999999999").with(user("u").roles("user")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("errmsg", "Account not found"));
        mvc.perform(get("/inquiry/position").param("accountNo", "12AB").with(user("u").roles("user")))
                .andExpect(model().attribute("errmsg", "Account number must be 10 digits"));
    }

    @Test
    void historyInquiryIsDescendingAndPaged10() throws Exception {
        String page0 = mvc.perform(get("/inquiry/history").param("accountNo", TestData.ACCOUNT_NO)
                        .with(user("u").roles("user")))
                .andExpect(status().isOk()).andExpect(view().name("history"))
                .andExpect(model().attribute("hasNext", true))
                .andExpect(model().attribute("hasPrevious", false))
                .andReturn().getResponse().getContentAsString();
        assertThat(page0).contains("2024-01-12").doesNotContain("2024-01-02");
        assertThat(page0.indexOf("2024-01-12")).isLessThan(page0.indexOf("2024-01-03"));

        mvc.perform(get("/inquiry/history").param("accountNo", TestData.ACCOUNT_NO).param("page", "1")
                        .with(user("u").roles("user")))
                .andExpect(model().attribute("hasNext", false))
                .andExpect(content().string(containsString("2024-01-01")));
    }

    @Test
    void portfolioRestCrud() throws Exception {
        String body = """
                {"portfolioId":"PORT0002","accountNo":"2222222222","clientName":"Second","clientType":"CORPORATE",
                 "status":"ACTIVE","totalValue":100.00,"cashBalance":10.00}
                """;
        mvc.perform(post("/api/portfolios").contentType(MediaType.APPLICATION_JSON).content(body)
                        .with(user("o").roles("operator")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id.portfolioId").value("PORT0002"));

        mvc.perform(post("/api/portfolios").contentType(MediaType.APPLICATION_JSON).content(body)
                        .with(user("o").roles("operator")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("E003"));

        mvc.perform(get("/api/portfolios/PORT0002/2222222222").with(user("u").roles("user")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.clientName").value("Second"));

        mvc.perform(patch("/api/portfolios").contentType(MediaType.APPLICATION_JSON).content("""
                        [{"portfolioId":"PORT0002","accountNo":"2222222222","updateType":"STATUS","newValue":"C"},
                         {"portfolioId":"PORT0002","accountNo":"2222222222","updateType":"VALUE","newValue":"250.50"},
                         {"portfolioId":"PORT9999","accountNo":"2222222222","updateType":"NAME","newValue":"x"}]
                        """).with(user("o").roles("operator")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updated").value(2))
                .andExpect(jsonPath("$.errors").value(1));
        assertThat(portfolioRepository.findFirstByIdPortfolioId("PORT0002").orElseThrow())
                .satisfies(p -> {
                    assertThat(p.getStatus()).isEqualTo(PortfolioStatus.CLOSED);
                    assertThat(p.getTotalValue()).isEqualByComparingTo("250.50");
                });

        mvc.perform(delete("/api/portfolios/PORT0002/2222222222").with(user("o").roles("operator")))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/portfolios/PORT0002/2222222222").with(user("u").roles("user")))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.errorCode").value("E002"));
    }

    @Test
    void invalidPortfolioIdIsRejectedByPortvaldRules() throws Exception {
        mvc.perform(post("/api/portfolios").contentType(MediaType.APPLICATION_JSON).content("""
                        {"portfolioId":"XXXX0002","accountNo":"2222222222","clientName":"Bad","clientType":"CORPORATE",
                         "status":"ACTIVE"}
                        """).with(user("o").roles("operator")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid Portfolio ID format"));
    }
}
