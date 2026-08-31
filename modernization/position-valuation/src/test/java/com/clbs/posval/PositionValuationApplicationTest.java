package com.clbs.posval;

import static org.assertj.core.api.Assertions.assertThat;

import com.clbs.posval.batch.PositionUpdateBatch;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** The Spring context wires the slice and defaults to the faithful validate-only behaviour. */
@SpringBootTest
class PositionValuationApplicationTest {

    @Autowired
    private PositionUpdateBatch batch;

    @Test
    void contextLoads() {
        assertThat(batch).isNotNull();
    }
}
