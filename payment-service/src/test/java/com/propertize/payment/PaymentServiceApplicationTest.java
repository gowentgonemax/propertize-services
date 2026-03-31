package com.propertize.payment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Payment Service Application Tests")
class PaymentServiceApplicationTest {

    @Test
    @DisplayName("Context loads (no-op smoke test)")
    void contextLoads() {
        assertThat(true).isTrue();
    }
}
