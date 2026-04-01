package com.propertize.payroll.calculation;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for the payroll calculation package.
 *
 * <p>
 * Tests are structured by class (Nested) to keep them discoverable
 * and independently runnable.
 * </p>
 */
class DeductionStrategyTest {

    // =========================================================================
    // PayrollContext – compact constructor validation
    // =========================================================================

    @Nested
    class PayrollContextValidation {

        PayrollContext sampleContext(BigDecimal grossPay) {
            return new PayrollContext(
                    1L, "FULL_TIME", grossPay,
                    BigDecimal.valueOf(5000), 80, 0,
                    "CA", true, true, new BigDecimal("0.05"));
        }

        @Test
        void rejectsNegativeGrossPay() {
            assertThatThrownBy(() -> sampleContext(new BigDecimal("-1")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("grossPay");
        }

        @Test
        void rejectsNegativeYtdEarnings() {
            assertThatThrownBy(() -> new PayrollContext(
                    1L, "FULL_TIME", BigDecimal.valueOf(1000),
                    new BigDecimal("-1"), 80, 0,
                    "CA", false, false, BigDecimal.ZERO)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ytdEarnings");
        }
    }

    // =========================================================================
    // FederalTaxStrategy
    // =========================================================================

    @Nested
    class FederalTaxStrategyTests {

        private final FederalTaxStrategy strategy = new FederalTaxStrategy();

        private PayrollContext ctx(String employmentType, double grossPay) {
            return new PayrollContext(
                    1L, employmentType, BigDecimal.valueOf(grossPay),
                    BigDecimal.valueOf(10000), 80, 0,
                    "TX", false, false, BigDecimal.ZERO);
        }

        @Test
        void contractorPaysNoFederalTax() {
            PayrollContext ctx = ctx("CONTRACTOR", 5000.00);
            assertThat(strategy.calculate(ctx)).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        void nameIsCorrect() {
            assertThat(strategy.name()).isEqualTo("FederalIncomeTax");
        }

        @ParameterizedTest(name = "grossPay={0} → tax >= 0")
        @CsvSource({ "500.00", "1000.00", "3000.00", "7000.00", "20000.00" })
        void taxIsNonNegativeForCommonPayAmounts(BigDecimal grossPay) {
            PayrollContext ctx = new PayrollContext(
                    1L, "FULL_TIME", grossPay,
                    BigDecimal.valueOf(10000), 80, 0,
                    "CA", false, false, BigDecimal.ZERO);
            assertThat(strategy.calculate(ctx)).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        }

        @Test
        void higherGrossPayYieldsHigherTax() {
            PayrollContext low = ctx("FULL_TIME", 1000.00);
            PayrollContext high = ctx("FULL_TIME", 5000.00);
            assertThat(strategy.calculate(high)).isGreaterThan(strategy.calculate(low));
        }
    }

    // =========================================================================
    // Contribution401kStrategy
    // =========================================================================

    @Nested
    class Contribution401kStrategyTests {

        private final Contribution401kStrategy strategy = new Contribution401kStrategy();

        private PayrollContext ctx(boolean has401k, double grossPay, double ytdEarnings, double rate) {
            return new PayrollContext(
                    1L, "FULL_TIME", BigDecimal.valueOf(grossPay),
                    BigDecimal.valueOf(ytdEarnings), 80, 0,
                    "TX", false, has401k, BigDecimal.valueOf(rate));
        }

        @Test
        void zeroWhenNo401k() {
            assertThat(strategy.calculate(ctx(false, 5000, 0, 0.06)))
                    .isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        void contributionEqualsGrossTimesRate() {
            // 5% of 2000 = 100.00 (well below annual cap)
            BigDecimal result = strategy.calculate(ctx(true, 2000, 0, 0.05));
            assertThat(result).isEqualByComparingTo(new BigDecimal("100.00"));
        }

        @Test
        void nameIsCorrect() {
            assertThat(strategy.name()).isEqualTo("401kContribution");
        }
    }

    // =========================================================================
    // PayrollCalculationEngine (integration of strategies)
    // =========================================================================

    @Nested
    class PayrollCalculationEngineTests {

        private PayrollContext ctx() {
            return new PayrollContext(
                    1L, "FULL_TIME", new BigDecimal("5000.00"),
                    new BigDecimal("20000.00"), 80, 0,
                    "CA", false, true, new BigDecimal("0.06"));
        }

        @Test
        void netPayPlusDeductionsEqualsGross() {
            List<DeductionStrategy> strategies = List.of(
                    new FederalTaxStrategy(), new Contribution401kStrategy());
            PayrollCalculationEngine engine = new PayrollCalculationEngine(strategies);

            PayrollResult result = engine.calculate(ctx());

            assertThat(result.netPay().add(result.totalDeductions()))
                    .isEqualByComparingTo(result.grossPay());
        }

        @Test
        void breakdownContainsAllStrategyNames() {
            List<DeductionStrategy> strategies = List.of(
                    new FederalTaxStrategy(), new Contribution401kStrategy());
            PayrollCalculationEngine engine = new PayrollCalculationEngine(strategies);

            PayrollResult result = engine.calculate(ctx());

            assertThat(result.deductionBreakdown())
                    .containsKeys("FederalIncomeTax", "401kContribution");
        }

        @Test
        void noStrategiesYieldsZeroDeductions() {
            PayrollCalculationEngine engine = new PayrollCalculationEngine(List.of());
            PayrollResult result = engine.calculate(ctx());

            assertThat(result.totalDeductions()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.netPay()).isEqualByComparingTo(result.grossPay());
        }
    }
}
