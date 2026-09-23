package com.cognition.clbs.common.persistence;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

/** Minimal Boot configuration so {@code @DataJpaTest} can bootstrap this library module. */
@SpringBootConfiguration
@EnableAutoConfiguration
class PersistenceTestApplication {}
