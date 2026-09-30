package com.portfolio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class PortfolioApplicationTests {
  @Autowired JdbcTemplate jdbcTemplate;

  @Test
  void contextLoadsAndFlywayCreatesTables() {
    Integer count =
        jdbcTemplate.queryForObject(
            "select count(*) from information_schema.tables where table_name in ('PORTFOLIO_MASTER','POSHIST','AUTHFILE')",
            Integer.class);
    assertEquals(3, count);
  }
}
