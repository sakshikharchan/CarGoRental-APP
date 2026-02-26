package com.example.demo.Service;

import com.example.demo.Model.TrustScore;
import com.example.demo.Model.TrustScoreEvent;
import com.example.demo.Repository.TrustScoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Trust Score Engine
 *
 * Score Components and Weights:
 *  - Driving Behavior    (25%) – late returns, accident history
 *  - Damage History      (30%) – damage incidents, severity
 *  - Payment Reliability (25%) – payment failures vs successes
 *  - Cancellation        (20%) – cancellation rate
 *
 * Each component starts at 100 and deductions/bonuses are applied.
 * Final composite = weighted average, capped [0, 100].
 *
 * Grade:
 *   EXCELLENT  85 – 100
 *   GOOD       70 – 84
 *   FAIR       50 – 69
 *   POOR        0 – 49
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrustScoreService {

    private final TrustScoreRepository trustScoreRepository;

    // ── Weights ───────────────────────────────────────────────────────────────
    private static final BigDecimal W_DRIVING    = BigDecimal.valueOf(0.25);
    private static final BigDecimal W_DAMAGE     = BigDecimal.valueOf(0.30);
    private static final BigDecimal W_PAYMENT    = BigDecimal.valueOf(0.25);
    private static final BigDecimal W_CANCEL     = BigDecimal.valueOf(0.20);

    // ── Deduction constants ───────────────────────────────────────────────────
    private static final BigDecimal DEDUCT_LATE_RETURN        = BigDecimal.valueOf(8);
    private static final BigDecimal DEDUCT_DAMAGE_MINOR       = BigDecimal.valueOf(10);
    private static final BigDecimal DEDUCT_DAMAGE_MODERATE    = BigDecimal.valueOf(20);
    private static final BigDecimal DEDUCT_DAMAGE_SEVERE      = BigDecimal.valueOf(35);
    private static final BigDecimal DEDUCT_PAYMENT_FAILURE    = BigDecimal.valueOf(15);
    private static final BigDecimal BONUS_PAYMENT_SUCCESS     = BigDecimal.valueOf(2);
    private static final BigDecimal BONUS_COMPLETED_BOOKING   = BigDecimal.valueOf(3);

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Get score for a user; creates default if first time.
     */
    public TrustScore getOrCreate(Long userId) {
        return trustScoreRepository.findByUserId(userId)
                .orElseGet(() -> createDefault(userId));
    }

    /**
     * Full recalculation from raw DB counters. Call after any impactful event.
     */
    public TrustScore recalculate(Long userId) {
        // Pull raw counters from DB
        int totalBookings     = trustScoreRepository.countBookings(userId);
        int completed         = trustScoreRepository.countCompletedBookings(userId);
        int cancelled         = trustScoreRepository.countCancelledBookings(userId);
        int lateReturns       = trustScoreRepository.countLateReturns(userId);
        int damageIncidents   = trustScoreRepository.countDamageIncidents(userId);
        int paymentFail       = trustScoreRepository.countPaymentFailures(userId);
        int paymentSuccess    = trustScoreRepository.countPaymentSuccess(userId);
        Double avgRating      = trustScoreRepository.getAvgReviewRating(userId);

        // ── Component: Cancellation Score ────────────────────────────────────
        BigDecimal cancellationScore = BigDecimal.valueOf(100);
        if (totalBookings > 0) {
            double cancelRate = (double) cancelled / totalBookings;
            // Each 10% cancel rate = -15 points
            cancellationScore = BigDecimal.valueOf(100 - (cancelRate * 150))
                    .max(BigDecimal.ZERO);
        }
        // Bonus for high completions
        cancellationScore = cancellationScore
                .add(BigDecimal.valueOf(Math.min(completed, 5)).multiply(BONUS_COMPLETED_BOOKING))
                .min(BigDecimal.valueOf(100));

        // ── Component: Driving Behavior Score ────────────────────────────────
        // Proxy: late returns → poor driving discipline
        BigDecimal drivingScore = BigDecimal.valueOf(100);
        drivingScore = drivingScore
                .subtract(BigDecimal.valueOf(lateReturns).multiply(DEDUCT_LATE_RETURN))
                .max(BigDecimal.ZERO);
        // Boost from positive reviews (reviews proxy real driving behavior)
        if (avgRating != null && avgRating > 0) {
            // rating 5 → +10, rating 1 → -10
            double ratingBonus = (avgRating - 3) * 5.0;
            drivingScore = drivingScore.add(BigDecimal.valueOf(ratingBonus))
                    .max(BigDecimal.ZERO).min(BigDecimal.valueOf(100));
        }

        // ── Component: Damage History Score ──────────────────────────────────
        BigDecimal damageScore = BigDecimal.valueOf(100);
        // We fetch count only (severity-weighted deduction below uses a flat count)
        // For a richer version, you'd query severity from damage_reports
        damageScore = damageScore
                .subtract(BigDecimal.valueOf(damageIncidents).multiply(DEDUCT_DAMAGE_MODERATE))
                .max(BigDecimal.ZERO);

        // ── Component: Payment Reliability Score ─────────────────────────────
        BigDecimal paymentScore = BigDecimal.valueOf(100);
        paymentScore = paymentScore
                .subtract(BigDecimal.valueOf(paymentFail).multiply(DEDUCT_PAYMENT_FAILURE))
                .add(BigDecimal.valueOf(Math.min(paymentSuccess, 10)).multiply(BONUS_PAYMENT_SUCCESS))
                .max(BigDecimal.ZERO)
                .min(BigDecimal.valueOf(100));

        // ── Composite weighted score ──────────────────────────────────────────
        BigDecimal overall = drivingScore.multiply(W_DRIVING)
                .add(damageScore.multiply(W_DAMAGE))
                .add(paymentScore.multiply(W_PAYMENT))
                .add(cancellationScore.multiply(W_CANCEL))
                .setScale(2, RoundingMode.HALF_UP);

        String grade = computeGrade(overall);

        TrustScore score = TrustScore.builder()
                .userId(userId)
                .drivingBehaviorScore(drivingScore.setScale(2, RoundingMode.HALF_UP))
                .damageHistoryScore(damageScore.setScale(2, RoundingMode.HALF_UP))
                .paymentReliabilityScore(paymentScore.setScale(2, RoundingMode.HALF_UP))
                .cancellationScore(cancellationScore.setScale(2, RoundingMode.HALF_UP))
                .overallScore(overall)
                .trustGrade(grade)
                .totalBookings(totalBookings)
                .completedBookings(completed)
                .cancelledBookings(cancelled)
                .lateReturns(lateReturns)
                .damageIncidents(damageIncidents)
                .paymentFailures(paymentFail)
                .paymentSuccess(paymentSuccess)
                .avgReviewRating(avgRating != null
                        ? BigDecimal.valueOf(avgRating).setScale(2, RoundingMode.HALF_UP)
                        : null)
                .build();

        // Upsert
        boolean exists = trustScoreRepository.findByUserId(userId).isPresent();
        if (exists) {
            trustScoreRepository.update(score);
        } else {
            trustScoreRepository.insert(score);
        }

        log.info("Trust score recalculated for userId={} → {}/{}", userId, overall, grade);
        return score;
    }

    /**
     * Record an event and immediately trigger recalculation.
     * eventType: BOOKING_COMPLETED | BOOKING_CANCELLED | LATE_RETURN |
     *            DAMAGE_REPORTED   | PAYMENT_FAILED   | PAYMENT_SUCCESS | REVIEW_RECEIVED
     */
    public TrustScore recordEvent(Long userId, String eventType, String detail, Long bookingId) {
        BigDecimal impact = eventImpact(eventType);
        TrustScoreEvent event = TrustScoreEvent.builder()
                .userId(userId)
                .eventType(eventType)
                .eventDetail(detail)
                .scoreImpact(impact)
                .bookingId(bookingId)
                .build();
        trustScoreRepository.saveEvent(event);
        return recalculate(userId);
    }

    public List<TrustScoreEvent> getHistory(Long userId, int page, int size) {
        return trustScoreRepository.findEventsByUserId(userId, page, size);
    }

    public List<TrustScore> getAllScores(int page, int size) {
        return trustScoreRepository.findAll(page, size);
    }

    // ── Internal Helpers ──────────────────────────────────────────────────────

    private TrustScore createDefault(Long userId) {
        TrustScore score = TrustScore.builder()
                .userId(userId)
                .drivingBehaviorScore(BigDecimal.valueOf(100))
                .damageHistoryScore(BigDecimal.valueOf(100))
                .paymentReliabilityScore(BigDecimal.valueOf(100))
                .cancellationScore(BigDecimal.valueOf(100))
                .overallScore(BigDecimal.valueOf(100))
                .trustGrade("EXCELLENT")
                .totalBookings(0).completedBookings(0).cancelledBookings(0)
                .lateReturns(0).damageIncidents(0)
                .paymentFailures(0).paymentSuccess(0)
                .avgReviewRating(null)
                .build();
        trustScoreRepository.insert(score);
        return score;
    }

    private String computeGrade(BigDecimal score) {
        double s = score.doubleValue();
        if (s >= 85) return "EXCELLENT";
        if (s >= 70) return "GOOD";
        if (s >= 50) return "FAIR";
        return "POOR";
    }

    private BigDecimal eventImpact(String eventType) {
        return switch (eventType) {
            case "BOOKING_COMPLETED"  -> BigDecimal.valueOf(+3);
            case "PAYMENT_SUCCESS"    -> BigDecimal.valueOf(+2);
            case "REVIEW_RECEIVED"    -> BigDecimal.valueOf(+1);
            case "BOOKING_CANCELLED"  -> BigDecimal.valueOf(-5);
            case "LATE_RETURN"        -> BigDecimal.valueOf(-8);
            case "PAYMENT_FAILED"     -> BigDecimal.valueOf(-15);
            case "DAMAGE_REPORTED"    -> BigDecimal.valueOf(-20);
            default                   -> BigDecimal.ZERO;
        };
    }
}