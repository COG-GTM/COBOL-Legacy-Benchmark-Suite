package com.clbs.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.clbs.online.InquiryCommArea;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Boots the migrated application with the sample dataset and exercises the REST adapters that
 * front the COBOL programs.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RestApiIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        return (Map<String, Object>) value;
    }

    @Test
    void portfolioReportListsTheSamplePortfolios() {
        ResponseEntity<Map> response = rest.getForEntity("/api/portfolios", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(asMap(response.getBody().get("counters")).get("recordsRead")).isEqualTo(3);
        assertThat(response.getBody().get("programName")).isEqualTo("PORTREAD");
    }

    @Test
    void portfolioCanBeCreatedReadAndDeleted() {
        Map<String, Object> record = Map.of("portId", "PORT09999", "accountNo", "ACCT099999",
                "clientName", "REST PORTFOLIO", "status", "A", "totalValue", "250.75");

        ResponseEntity<Map> created = rest.postForEntity("/api/portfolios", record, Map.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<Map> read = rest.getForEntity(
                "/api/portfolios/PORT09999?accountNo=ACCT099999", Map.class);
        assertThat(read.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(asMap(read.getBody().get("record")).get("clientName"))
                .isEqualTo("REST PORTFOLIO");
        assertThat(asMap(read.getBody().get("record")).get("totalValue")).isEqualTo(250.75);

        rest.delete("/api/portfolios/PORT09999?accountNo=ACCT099999");
        assertThat(rest.getForEntity("/api/portfolios/PORT09999?accountNo=ACCT099999", Map.class)
                .getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void unknownPortfolioReadReturnsNotFound() {
        assertThat(rest.getForEntity("/api/portfolios/PORT00404", Map.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    /** PORTTRAN 2000-PROCESS-TRANSACTIONS only validates; invalid types are counted as errors. */
    @Test
    void transactionBatchCountsValidAndInvalidRecords() {
        Map<String, Object> valid = transaction("BU");
        Map<String, Object> invalid = transaction("ZZ");

        ResponseEntity<Map> response =
                rest.postForEntity("/api/transactions", List.of(valid, invalid), Map.class);

        Map<String, Object> counters = asMap(response.getBody().get("counters"));
        assertThat(counters.get("read")).isEqualTo(2);
        assertThat(counters.get("processed")).isEqualTo(1);
        assertThat(counters.get("errors")).isEqualTo(1);
    }

    private static Map<String, Object> transaction(String type) {
        return Map.of("portfolioId", "PORT00001", "accountNo", "ACCT000001", "sequenceNo",
                "00000001", "type", type, "investmentId", "IBM0000001", "quantity", "1.0000",
                "price", "10.00", "amount", "10.00");
    }

    @Test
    void inquiryRequiresAnAuthorizedUser() {
        InquiryCommArea commArea = new InquiryCommArea();
        commArea.setFunction(InquiryCommArea.FUNC_PORTFOLIO);
        commArea.setAccountNo("PORT00001");

        assertThat(post("/api/inquiry", commArea, "USER0001").getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(post("/api/inquiry", commArea, "NOBODY").getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void positionReportIsServedFromTheBatchEndpoint() {
        ResponseEntity<Map> response =
                rest.getForEntity("/api/batch/reports/positions", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(asMap(response.getBody().get("counters")).get("positions")).isEqualTo(3);
    }

    private ResponseEntity<Map> post(String path, Object body, String userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", userId);
        return rest.exchange(path, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);
    }
}
