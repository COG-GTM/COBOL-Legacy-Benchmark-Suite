package com.cog.portfolio.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Decision 1: role-based authorization only (admin / operator / user). AUTHFILE
 * per-resource checks are not migrated in this demo (TODO produccion).
 */
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureObservability
class SecurityRolesTest {

    @Autowired
    MockMvc mvc;

    @Test
    void anonymousIsRedirectedToLoginForInquiry() throws Exception {
        mvc.perform(get("/menu").accept("text/html")).andExpect(status().is3xxRedirection());
    }

    @Test
    void anonymousGetsUnauthorizedOnApi() throws Exception {
        mvc.perform(get("/api/portfolios").accept("application/json")).andExpect(status().isUnauthorized());
    }

    @Test
    void userCanUseInquiryScreens() throws Exception {
        mvc.perform(get("/menu").with(user("u").roles("user"))).andExpect(status().isOk());
        mvc.perform(get("/inquiry/position").with(user("u").roles("user"))).andExpect(status().isOk());
        mvc.perform(get("/inquiry/history").with(user("u").roles("user"))).andExpect(status().isOk());
        mvc.perform(get("/api/portfolios").with(user("u").roles("user"))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("user cannot launch batch (JCL replacement), maintenance, validation or mutate portfolios")
    void userIsDeniedOperatorFunctions() throws Exception {
        mvc.perform(post("/batch/report/positions").with(user("u").roles("user"))).andExpect(status().isForbidden());
        mvc.perform(post("/batch/returncodes/analyze").with(user("u").roles("user"))).andExpect(status().isForbidden());
        mvc.perform(post("/maintenance/CLEANUP").with(user("u").roles("user"))).andExpect(status().isForbidden());
        mvc.perform(post("/validation/run").with(user("u").roles("user"))).andExpect(status().isForbidden());
        mvc.perform(post("/api/portfolios").contentType("application/json").content("{}")
                .with(user("u").roles("user"))).andExpect(status().isForbidden());
        mvc.perform(get("/actuator/metrics").with(user("u").roles("user"))).andExpect(status().isForbidden());
    }

    @Test
    void operatorCanLaunchBatchAndMaintenance() throws Exception {
        mvc.perform(post("/batch/report/statistics").with(user("o").roles("operator"))).andExpect(status().isAccepted());
        mvc.perform(post("/batch/report/audit").with(user("o").roles("operator"))).andExpect(status().isAccepted());
        mvc.perform(post("/maintenance/ANALYZE").with(user("o").roles("operator"))).andExpect(status().isOk());
        mvc.perform(post("/validation/run").with(user("o").roles("operator"))).andExpect(status().isOk());
        mvc.perform(get("/actuator/metrics").with(user("o").roles("operator"))).andExpect(status().isOk());
    }

    @Test
    void operatorCannotOpenH2Console() throws Exception {
        mvc.perform(get("/h2-console/").with(user("o").roles("operator"))).andExpect(status().isForbidden());
    }

    @Test
    void adminHasEverything() throws Exception {
        mvc.perform(get("/menu").with(user("a").roles("admin"))).andExpect(status().isOk());
        mvc.perform(post("/batch/returncodes/analyze").with(user("a").roles("admin"))).andExpect(status().isAccepted());
        mvc.perform(post("/maintenance/ANALYZE").with(user("a").roles("admin"))).andExpect(status().isOk());
        mvc.perform(get("/actuator/prometheus").with(user("a").roles("admin"))).andExpect(status().isOk());
    }

    @Test
    void healthIsPublic() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }
}
