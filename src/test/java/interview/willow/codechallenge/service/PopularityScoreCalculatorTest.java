package interview.willow.codechallenge.service;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PopularityScoreCalculatorTest {

    private static final Instant NOW = Instant.parse("2026-06-22T00:00:00Z");
    private final PopularityScoreCalculator calculator = new PopularityScoreCalculator(
            Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void rewardsEngagementAndWeightsForksMoreThanStars() {
        final var oneStar = calculator.calculate(1, 0, NOW);
        final var oneFork = calculator.calculate(0, 1, NOW);
        final var popular = calculator.calculate(100, 20, NOW);

        assertThat(oneFork).isGreaterThan(oneStar);
        assertThat(popular).isGreaterThan(oneFork);
    }

    @Test
    void decaysGraduallyWithoutBecomingNegative() {
        final var current = calculator.calculate(100, 20, NOW);
        final var oneYearOld = calculator.calculate(100, 20, NOW.minusSeconds(365L * 24 * 60 * 60));
        final var veryOld = calculator.calculate(100, 20, NOW.minusSeconds(20L * 365 * 24 * 60 * 60));

        assertThat(oneYearOld).isCloseTo(current / 2, within(0.01));
        assertThat(veryOld).isPositive().isLessThan(oneYearOld);
    }

    @Test
    void clampsFutureUpdatesToZeroAge() {
        assertThat(calculator.calculate(10, 2, NOW.plusSeconds(60)))
                .isEqualTo(calculator.calculate(10, 2, NOW));
    }

    @Test
    void rejectsImpossibleNegativeCounts() {
        assertThatThrownBy(() -> calculator.calculate(-1, 0, NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Stars and forks cannot be negative");
    }

    private static Offset<Double> within(final double value) {
        return Offset.offset(value);
    }
}
