package com.portfolio;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class InquiryControllerTest {
  @Autowired private MockMvc mockMvc;

  @Test
  @WithMockUser("admin")
  void menuContainsPositionInquiry() throws Exception {
    mockMvc
        .perform(get("/inquiry/menu"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Position Inquiry")));
  }

  @Test
  @WithMockUser("admin")
  void positionContainsSeededSecurity() throws Exception {
    mockMvc
        .perform(get("/inquiry/position").param("accountNo", "1000000001"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("STK000001")));
  }

  @Test
  @WithMockUser("admin")
  void historyContainsBuyTransaction() throws Exception {
    mockMvc
        .perform(get("/inquiry/history").param("accountNo", "1000000001"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("BU")));
  }
}
