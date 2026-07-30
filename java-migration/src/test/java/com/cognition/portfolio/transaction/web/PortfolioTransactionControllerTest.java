package com.cognition.portfolio.transaction.web;

import static org.hamcrest.Matchers.closeTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cognition.portfolio.transaction.repository.PortfolioTransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Contract tests for the endpoint set that replaces the COBOL file operations. */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:web-test;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class PortfolioTransactionControllerTest {

  private static final String KEY = "20240320093015PORT0001000001";

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private PortfolioTransactionRepository repository;

  @BeforeEach
  void clean() {
    repository.deleteAll();
  }

  private String createBuyPayload() throws Exception {
    return objectMapper.writeValueAsString(
        java.util.Map.of(
            "transactionDate", "20240320",
            "transactionTime", "093015",
            "portfolioId", "PORT0001",
            "investmentId", "AAPL000001",
            "type", "BU",
            "quantity", new BigDecimal("150.0000"),
            "price", new BigDecimal("187.4500"),
            "currency", "USD"));
  }

  @Test
  @DisplayName("POST assigns TRN-SEQUENCE-NO (BR-20) and derives TRN-AMOUNT (BR-22)")
  void insertDerivesSequenceAndAmount() throws Exception {
    mockMvc
        .perform(post("/api/v1/transactions").contentType(MediaType.APPLICATION_JSON).content(createBuyPayload()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.sequenceNo").value("000001"))
        .andExpect(jsonPath("$.transactionKey").value(KEY))
        .andExpect(jsonPath("$.amount").value(closeTo(28117.50, 0.001)))
        .andExpect(jsonPath("$.status").value("P"));
  }

  @Test
  @DisplayName("GET by 28 character TRN-KEY performs the keyed read")
  void keyedRead() throws Exception {
    mockMvc.perform(post("/api/v1/transactions").contentType(MediaType.APPLICATION_JSON).content(createBuyPayload()));

    mockMvc
        .perform(get("/api/v1/transactions/{key}", KEY))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.portfolioId").value("PORT0001"))
        .andExpect(jsonPath("$.type").value("BU"));

    mockMvc
        .perform(get("/api/v1/transactions/{key}", "20991231235959PORT0001000001"))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("GET browses in key sequence and filters on TRN-PORTFOLIO-ID")
  void browse() throws Exception {
    mockMvc.perform(post("/api/v1/transactions").contentType(MediaType.APPLICATION_JSON).content(createBuyPayload()));

    mockMvc
        .perform(get("/api/v1/transactions").param("portfolioId", "PORT0001").param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].transactionKey").value(KEY));

    mockMvc
        .perform(get("/api/v1/transactions").param("portfolioId", "PORT0002"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(0));
  }

  @Test
  @DisplayName("PUT rewrites the record; POST /status applies a BR-23 transition")
  void rewriteAndStatusTransition() throws Exception {
    mockMvc.perform(post("/api/v1/transactions").contentType(MediaType.APPLICATION_JSON).content(createBuyPayload()));

    String update =
        objectMapper.writeValueAsString(
            java.util.Map.of(
                "investmentId", "AAPL000001",
                "type", "BU",
                "quantity", new BigDecimal("175.0000"),
                "price", new BigDecimal("190.0000"),
                "currency", "USD",
                "processUser", "ONLINE01"));

    mockMvc
        .perform(put("/api/v1/transactions/{key}", KEY).contentType(MediaType.APPLICATION_JSON).content(update))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.amount").value(closeTo(33250.00, 0.001)))
        .andExpect(jsonPath("$.processUser").value("ONLINE01"));

    mockMvc
        .perform(
            post("/api/v1/transactions/{key}/status", KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"D\",\"processUser\":\"BATCH001\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("D"));

    mockMvc
        .perform(
            post("/api/v1/transactions/{key}/status", KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"P\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.ruleId").value("BR-23"));
  }

  @Test
  @DisplayName("POST /process returns the portfolio deltas of PORTTRAN 2200-UPDATE-POSITIONS")
  void process() throws Exception {
    mockMvc.perform(post("/api/v1/transactions").contentType(MediaType.APPLICATION_JSON).content(createBuyPayload()));

    mockMvc
        .perform(
            post("/api/v1/transactions/{key}/process", KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"availableUnits\":1000.0000}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.transaction.status").value("D"))
        .andExpect(jsonPath("$.unitsDelta").value(closeTo(150.0, 0.001)))
        .andExpect(jsonPath("$.auditAction").value("CREATE"));
  }

  @Test
  @DisplayName("BR-23: POST /process is refused once the record is no longer pending")
  void processRefusedOnceTerminal() throws Exception {
    mockMvc.perform(post("/api/v1/transactions").contentType(MediaType.APPLICATION_JSON).content(createBuyPayload()));
    mockMvc.perform(
        post("/api/v1/transactions/{key}/process", KEY)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"availableUnits\":1000.0000}"));

    mockMvc
        .perform(
            post("/api/v1/transactions/{key}/process", KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"availableUnits\":1000.0000}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.ruleId").value("BR-23"))
        .andExpect(jsonPath("$.message").value("Transaction is not pending: TRN-STATUS D"));
  }

  @Test
  @DisplayName("BR-13: a failed posting still reports AUD-ACTION with AUD-STATUS 'FAIL'")
  void failedPostingReportsAuditEntry() throws Exception {
    String sell =
        objectMapper.writeValueAsString(
            java.util.Map.of(
                "transactionDate", "20240320",
                "transactionTime", "101122",
                "portfolioId", "PORT0001",
                "investmentId", "AAPL000001",
                "type", "SL",
                "quantity", new BigDecimal("50.0000"),
                "price", new BigDecimal("191.2000"),
                "currency", "USD"));
    mockMvc.perform(post("/api/v1/transactions").contentType(MediaType.APPLICATION_JSON).content(sell));
    String sellKey = "20240320101122PORT0001000001";

    mockMvc
        .perform(
            post("/api/v1/transactions/{key}/process", sellKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"availableUnits\":10.0000}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.errorText").value("Insufficient units for sale"))
        .andExpect(jsonPath("$.auditAction").value("DELETE"))
        .andExpect(jsonPath("$.auditStatus").value("FAIL"));

    // The position is read from PORTFILE in COBOL, so omitting it is a caller error, not BR-10.
    mockMvc
        .perform(
            post("/api/v1/transactions/{key}/process", sellKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("BR-03: a rejected TRN-TYPE is echoed in the COBOL ERR-TEXT")
  void invalidTypeCarriesTheOffendingValue() throws Exception {
    String unknownType =
        objectMapper.writeValueAsString(
            java.util.Map.of(
                "transactionDate", "20240320",
                "transactionTime", "093015",
                "portfolioId", "PORT0001",
                "investmentId", "AAPL000001",
                "type", "ZZ",
                "quantity", new BigDecimal("150.0000"),
                "price", new BigDecimal("187.4500"),
                "currency", "USD"));

    mockMvc
        .perform(post("/api/v1/transactions").contentType(MediaType.APPLICATION_JSON).content(unknownType))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Invalid Transaction Type: ZZ"));
  }

  @Test
  @DisplayName("A validation failure returns the COBOL ERR-TEXT with its rule and paragraph")
  void validationFailureCarriesCobolErrorText() throws Exception {
    String zeroQuantity =
        objectMapper.writeValueAsString(
            java.util.Map.of(
                "transactionDate", "20240320",
                "transactionTime", "093015",
                "portfolioId", "PORT0001",
                "investmentId", "AAPL000001",
                "type", "BU",
                "quantity", BigDecimal.ZERO,
                "price", new BigDecimal("187.4500"),
                "currency", "USD"));

    mockMvc
        .perform(post("/api/v1/transactions").contentType(MediaType.APPLICATION_JSON).content(zeroQuantity))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Quantity must be greater than zero"))
        .andExpect(jsonPath("$.ruleId").value("BR-04"))
        .andExpect(jsonPath("$.cobolParagraph").value("PORTTRAN 2130-CHECK-AMOUNTS"));
  }

  @Test
  @DisplayName("The service publishes an OpenAPI 3 contract covering every migrated operation")
  void openApiContractIsPublished() throws Exception {
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.openapi").exists())
        .andExpect(jsonPath("$.paths['/api/v1/transactions'].post").exists())
        .andExpect(jsonPath("$.paths['/api/v1/transactions'].get").exists())
        .andExpect(jsonPath("$.paths['/api/v1/transactions/{transactionKey}'].get").exists())
        .andExpect(jsonPath("$.paths['/api/v1/transactions/{transactionKey}'].put").exists())
        .andExpect(jsonPath("$.paths['/api/v1/transactions/{transactionKey}/status'].post").exists())
        .andExpect(jsonPath("$.paths['/api/v1/transactions/{transactionKey}/process'].post").exists());
  }
}
