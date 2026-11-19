package com.clbs.portfolio.web;

import com.clbs.portfolio.DatabaseCleaner;
import com.clbs.portfolio.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class PortfolioApiTest {

    private static final RequestPostProcessor VIEWER = httpBasic("viewer", "viewer-pass");
    private static final RequestPostProcessor OPERATOR = httpBasic("operator", "operator-pass");
    private static final String PORTFOLIO = """
            {"portfolioId":"PORT0001","accountNo":"1234567890","clientName":"GROWTH FUND CLIENT",
             "clientType":"I","status":"A","totalValue":12345.67,"cashBalance":1000.00}
            """;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private DatabaseCleaner cleaner;

    @BeforeEach
    void setUp() {
        cleaner.clean();
    }

    @Test
    void requiresAuthentication() throws Exception {
        mvc.perform(get("/api/v1/portfolios"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("WWW-Authenticate"))
                .andExpect(jsonPath("$.code").value("E006"));
        mvc.perform(get("/api/v1/portfolios").with(httpBasic("viewer", "wrong")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void viewerCannotModify() throws Exception {
        mvc.perform(post("/api/v1/portfolios").with(VIEWER).contentType(MediaType.APPLICATION_JSON).content(PORTFOLIO))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("E006"))
                .andExpect(jsonPath("$.message").value("Access denied"));
        mvc.perform(get("/api/v1/audit").with(VIEWER)).andExpect(status().isForbidden());
    }

    @Test
    void portfolioLifecycle() throws Exception {
        mvc.perform(post("/api/v1/portfolios").with(OPERATOR).contentType(MediaType.APPLICATION_JSON).content(PORTFOLIO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.createDate").value("2024-03-20"))
                .andExpect(jsonPath("$.totalValue").value(12345.67));

        mvc.perform(post("/api/v1/portfolios").with(OPERATOR).contentType(MediaType.APPLICATION_JSON).content(PORTFOLIO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("E003"));

        mvc.perform(get("/api/v1/portfolios").with(VIEWER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mvc.perform(patch("/api/v1/portfolios/PORT0001/accounts/1234567890").with(OPERATOR)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"action\":\"NAME\",\"newValue\":\"RENAMED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientName").value("RENAMED"));

        mvc.perform(get("/api/v1/portfolios/PORT0001/accounts/1234567890").with(VIEWER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientName").value("RENAMED"));

        mvc.perform(delete("/api/v1/portfolios/PORT0001/accounts/1234567890").param("reason", "CLOSED").with(OPERATOR))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/portfolios/PORT0001/accounts/1234567890").with(VIEWER))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("E002"))
                .andExpect(jsonPath("$.message").value("Record not found: PORT00011234567890"))
                .andExpect(jsonPath("$.path").value("/api/v1/portfolios/PORT0001/accounts/1234567890"));

        mvc.perform(get("/api/v1/audit").param("portfolioId", "PORT0001").with(OPERATOR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].action", hasItem("DELETE")));
    }

    @Test
    void validationErrorsUseUniformBody() throws Exception {
        mvc.perform(post("/api/v1/portfolios").with(OPERATOR).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"portfolioId\":\"\",\"accountNo\":\"1\",\"clientName\":\"X\",\"status\":\"A\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E008"))
                .andExpect(jsonPath("$.details[0].field").value("portfolioId"));

        mvc.perform(post("/api/v1/portfolios").with(OPERATOR).contentType(MediaType.APPLICATION_JSON)
                        .content(PORTFOLIO.replace("\"status\":\"A\"", "\"status\":\"C\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("New portfolios must have status 'A'"));

        mvc.perform(post("/api/v1/portfolios").with(OPERATOR).contentType(MediaType.APPLICATION_JSON).content("{bad"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E001"));

        mvc.perform(delete("/api/v1/portfolios/PORT0001/accounts/1234567890").param("reason", "NOPE").with(OPERATOR))
                .andExpect(status().isBadRequest());

        mvc.perform(get("/api/v1/portfolios/PORT00001234/accounts/1").with(VIEWER))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E008"));
    }

    @Test
    void transactionsAndInquiries() throws Exception {
        mvc.perform(post("/api/v1/portfolios").with(OPERATOR).contentType(MediaType.APPLICATION_JSON).content(PORTFOLIO))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/v1/transactions").with(OPERATOR).contentType(MediaType.APPLICATION_JSON).content("""
                        {"portfolioId":"PORT0001","investmentId":"IBM0000001","type":"BU",
                         "quantity":100,"price":125.00,"amount":12500.00,"currency":"USD"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("D"));

        mvc.perform(post("/api/v1/transactions").with(OPERATOR).contentType(MediaType.APPLICATION_JSON).content("""
                        {"portfolioId":"PORT0001","investmentId":"IBM0000001","type":"SL",
                         "quantity":500,"price":125.00,"amount":62500.00}"""))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("E007"))
                .andExpect(jsonPath("$.message").value("Insufficient units for sale"));

        mvc.perform(post("/api/v1/transactions").with(OPERATOR).contentType(MediaType.APPLICATION_JSON).content("""
                        {"portfolioId":"PORT0001","investmentId":"IBM0000001","type":"ZZ","quantity":1,"price":1,"amount":1}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid Transaction Type: ZZ"));

        mvc.perform(get("/api/v1/portfolios/PORT0001/transactions").with(VIEWER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        mvc.perform(get("/api/v1/inquiry/accounts/1234567890/positions").with(VIEWER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].quantity").value(100.0));

        mvc.perform(get("/api/v1/inquiry/accounts/1234567890/history").with(VIEWER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].transType").value("BU"));

        mvc.perform(get("/api/v1/inquiry/accounts/9999999999/positions").with(VIEWER))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Position not found for account"));
    }

    @Test
    void validationEndpointReturnsPortvaldCodes() throws Exception {
        mvc.perform(post("/api/v1/validations").with(VIEWER).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"T\",\"value\":\"ETF\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.returnCode").value(0))
                .andExpect(jsonPath("$.valid").value(true));
        mvc.perform(post("/api/v1/validations").with(VIEWER).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"T\",\"value\":\"XYZ\"}"))
                .andExpect(jsonPath("$.returnCode").value(3))
                .andExpect(jsonPath("$.message").value("Invalid Investment Type"));
    }

    @Test
    void batchJobsAcceptFixedWidthFiles() throws Exception {
        mvc.perform(post("/api/v1/batch/portfolios/add").with(OPERATOR)
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .content(Files.readAllBytes(Path.of("parity/fixtures/portadd.dat"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.counts.added").value(3))
                .andExpect(jsonPath("$.counts.duplicates").value(1))
                .andExpect(jsonPath("$.counts.errors").value(3));

        mvc.perform(get("/api/v1/batch/portfolios/report").with(OPERATOR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.counts.read").value(3));

        mvc.perform(post("/api/v1/batch/portfolios/add").with(OPERATOR)
                        .contentType(MediaType.APPLICATION_OCTET_STREAM).content(new byte[10]))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E001"));

        mvc.perform(get("/api/v1/batch/portfolios/report").with(VIEWER)).andExpect(status().isForbidden());
    }

    @Test
    void docsAndHealthArePublic() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
        mvc.perform(get("/actuator/prometheus").with(VIEWER)).andExpect(status().isForbidden());
    }
}
