package com.clbs.testdata;

import static org.assertj.core.api.Assertions.assertThat;

import com.clbs.testdata.TestDataGenerator.GeneratedData;
import com.clbs.testdata.TestDataGenerator.GeneratorConfig;
import java.util.List;
import org.junit.jupiter.api.Test;

class TestDataGeneratorTest {

    private final TestDataGenerator generator = new TestDataGenerator();

    @Test
    void portfolioRunIsDeterministicForASeed() {
        List<GeneratorConfig> configs =
                List.of(new GeneratorConfig(TestDataGenerator.PORTFOLIO, 5, ""));

        GeneratedData first = generator.run(configs, 42L);
        GeneratedData second = generator.run(configs, 42L);

        assertThat(first.portfolios()).hasSize(5);
        assertThat(first.portfolios().get(0).getPortId()).isEqualTo("PORT00001");
        assertThat(first.portfolios().stream().map(p -> p.getTotalValue().toPlainString()).toList())
                .isEqualTo(second.portfolios().stream()
                        .map(p -> p.getTotalValue().toPlainString()).toList());
    }

    @Test
    void transactionAmountsKeepScaleAndMatchQuantityTimesPrice() {
        GeneratedData data = generator.run(
                List.of(new GeneratorConfig(TestDataGenerator.TRANSACTION, 3, "")), 7L);

        assertThat(data.transactions()).hasSize(3);
        assertThat(data.transactions()).allSatisfy(transaction -> {
            assertThat(transaction.getQuantity().scale()).isEqualTo(4);
            assertThat(transaction.getAmount().scale()).isEqualTo(2);
            assertThat(transaction.getAmount()).isEqualByComparingTo(
                    transaction.getQuantity().multiply(transaction.getPrice())
                            .setScale(2, java.math.RoundingMode.HALF_UP));
        });
    }

    @Test
    void volumeRunGeneratesBothRecordTypes() {
        GeneratedData data = generator.run(
                List.of(new GeneratorConfig(TestDataGenerator.VOLUME_TEST, 4, "")), 1L);

        assertThat(data.portfolios()).hasSize(4);
        assertThat(data.transactions()).hasSize(4);
    }

    @Test
    void errorRunProducesRecordsThatFailValidation() {
        GeneratedData data = generator.run(
                List.of(new GeneratorConfig(TestDataGenerator.ERROR_TEST, 2, "")), 1L);

        assertThat(data.portfolios()).allSatisfy(portfolio -> {
            assertThat(portfolio.getPortId()).startsWith("BAD");
            assertThat(portfolio.getStatus()).isEqualTo('X');
        });
        assertThat(data.transactions()).allMatch(transaction -> "ZZ".equals(transaction.getType()));
    }

    @Test
    void unknownTypeIsCountedAsAnError() {
        GeneratedData data =
                generator.run(List.of(new GeneratorConfig("NOPE", 1, "")), 1L);

        assertThat(data.result().counter("errors")).isEqualTo(1);
        assertThat(data.result().getDisplay()).contains(TestDataGenerator.ERR_INVALID_TYPE);
    }
}
