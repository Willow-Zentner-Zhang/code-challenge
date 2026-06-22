package interview.willow.codechallenge.service;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/** Calculates a comparable popularity score from engagement and recent activity. */
@Component
public class PopularityScoreCalculator {

    private static final double FORK_WEIGHT = 2.0;
    private static final double RECENCY_WINDOW_DAYS = 365.0;
    private static final double SCORE_SCALE = 100.0;

    private final Clock clock;

    public PopularityScoreCalculator(final Clock clock) {
        this.clock = clock;
    }

    public double calculate(final int stars, final int forks, final Instant updatedAt) {
        if (stars < 0 || forks < 0) {
            throw new IllegalArgumentException("Stars and forks cannot be negative");
        }

        final var ageInDays = Math.max(0, Duration.between(updatedAt, clock.instant()).toDays());
        final var engagement = stars + FORK_WEIGHT * forks;
        final var recencyFactor = 1.0 / (1.0 + ageInDays / RECENCY_WINDOW_DAYS);
        final var score = SCORE_SCALE * Math.log1p(engagement) * recencyFactor;

        return Math.round(score * 100.0) / 100.0;
    }
}
