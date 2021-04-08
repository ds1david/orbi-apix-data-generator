package br.com.realizecfi.orbiapixdatagenerator;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class OrbiApixNumberOfTransactionGeneratorApplicationTests {

    @Test
    @SuppressWarnings("java:S3415")
    void contextLoads() {
        OrbiApixDataGeneratorApplication.main(new String[0]);

        assertThat(true).isTrue();
    }
}
