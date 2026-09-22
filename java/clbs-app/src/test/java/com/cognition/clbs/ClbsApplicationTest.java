package com.cognition.clbs;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

@SpringBootTest
class ClbsApplicationTest {

  @Autowired private ApplicationContext context;

  @Test
  void contextLoadsWithAllDomainModulesOnClasspath() {
    assertThat(context.getBean(ClbsApplication.class)).isNotNull();
  }
}
