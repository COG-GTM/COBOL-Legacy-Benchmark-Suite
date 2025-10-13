package com.cobol.benchmark.common.test;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public abstract class TestBase {
    
    @BeforeEach
    public void setUp() {
    }
}
