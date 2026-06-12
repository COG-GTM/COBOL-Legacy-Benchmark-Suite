package com.clbs.online;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clbs.portfolio.domain.PortfolioKey;
import com.clbs.portfolio.domain.PortfolioMaster;
import com.clbs.portfolio.repository.PortfolioMasterRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/** Full-context boot + inquiry endpoint test for the online tier. */
@SpringBootTest
@AutoConfigureMockMvc
class OnlineApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PortfolioMasterRepository portfolioRepository;

    @Test
    void contextLoads() {
    }

    @Test
    void inquiryEndpointReturnsPortfolio() throws Exception {
        PortfolioMaster pm = new PortfolioMaster();
        pm.setKey(new PortfolioKey("PORT0009", "ACCT000009"));
        pm.setClientName("GROWTH PORTFOLIO");
        pm.setClientType("I");
        pm.setCreateDate(20240320);
        pm.setLastMaint(20240321);
        pm.setStatus("A");
        pm.setTotalValue(new BigDecimal("12345678.99"));
        pm.setCashBalance(new BigDecimal("1000000.00"));
        pm.setLastUser("TSTGEN00");
        pm.setLastTrans(20240321);
        portfolioRepository.save(pm);

        mockMvc.perform(get("/api/portfolios/{portId}", "PORT0009"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].clientName").value("GROWTH PORTFOLIO"))
                .andExpect(jsonPath("$[0].status").value("A"));
    }
}
