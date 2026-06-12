package com.clbs.portfolio;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Minimal boot configuration so {@code @DataJpaTest} can locate a
 * {@code @SpringBootConfiguration} and entity/repository packages for the
 * portfolio module (which has no production application class of its own).
 */
@SpringBootApplication
public class TestApplication {
}
