package com.example.demo.Repository;

import com.example.demo.Model.TrustScore;
import com.example.demo.Model.TrustScoreEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class TrustScoreRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<TrustScore> scoreMapper = (rs, rowNum) -> TrustScore.builder()
            .id(rs.getLong("id"))
            .userId(rs.getLong("user_id"))
            .drivingBehaviorScore(rs.getBigDecimal("driving_behavior_score"))
            .damageHistoryScore(rs.getBigDecimal("damage_history_score"))
            .paymentReliabilityScore(rs.getBigDecimal("payment_reliability_score"))
            .cancellationScore(rs.getBigDecimal("cancellation_score"))
            .overallScore(rs.getBigDecimal("overall_score"))
            .trustGrade(rs.getString("trust_grade"))
            .totalBookings(rs.getInt("total_bookings"))
            .completedBookings(rs.getInt("completed_bookings"))
            .cancelledBookings(rs.getInt("cancelled_bookings"))
            .lateReturns(rs.getInt("late_returns"))
            .damageIncidents(rs.getInt("damage_incidents"))
            .paymentFailures(rs.getInt("payment_failures"))
            .paymentSuccess(rs.getInt("payment_success"))
            .avgReviewRating(rs.getBigDecimal("avg_review_rating"))
            .calculatedAt(rs.getTimestamp("calculated_at") != null
                    ? rs.getTimestamp("calculated_at").toLocalDateTime() : null)
            .updatedAt(rs.getTimestamp("updated_at") != null
                    ? rs.getTimestamp("updated_at").toLocalDateTime() : null)
            .build();

    private final RowMapper<TrustScoreEvent> eventMapper = (rs, rowNum) -> TrustScoreEvent.builder()
            .id(rs.getLong("id"))
            .userId(rs.getLong("user_id"))
            .eventType(rs.getString("event_type"))
            .eventDetail(rs.getString("event_detail"))
            .scoreImpact(rs.getBigDecimal("score_impact"))
            .bookingId(rs.getObject("booking_id", Long.class))
            .createdAt(rs.getTimestamp("created_at") != null
                    ? rs.getTimestamp("created_at").toLocalDateTime() : null)
            .build();

    public Optional<TrustScore> findByUserId(Long userId) {
        List<TrustScore> list = jdbcTemplate.query(
                "SELECT * FROM user_trust_scores WHERE user_id = ?", scoreMapper, userId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public List<TrustScore> findAll(int page, int size) {
        return jdbcTemplate.query(
                "SELECT * FROM user_trust_scores ORDER BY overall_score DESC LIMIT ? OFFSET ?",
                scoreMapper, size, page * size);
    }

    public long insert(TrustScore score) {
        String sql = """
            INSERT INTO user_trust_scores
                (user_id, driving_behavior_score, damage_history_score,
                 payment_reliability_score, cancellation_score,
                 overall_score, trust_grade,
                 total_bookings, completed_bookings, cancelled_bookings,
                 late_returns, damage_incidents, payment_failures, payment_success,
                 avg_review_rating, calculated_at)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,NOW())
            """;
        KeyHolder kh = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, score.getUserId());
            ps.setBigDecimal(2, score.getDrivingBehaviorScore());
            ps.setBigDecimal(3, score.getDamageHistoryScore());
            ps.setBigDecimal(4, score.getPaymentReliabilityScore());
            ps.setBigDecimal(5, score.getCancellationScore());
            ps.setBigDecimal(6, score.getOverallScore());
            ps.setString(7, score.getTrustGrade());
            ps.setInt(8, score.getTotalBookings());
            ps.setInt(9, score.getCompletedBookings());
            ps.setInt(10, score.getCancelledBookings());
            ps.setInt(11, score.getLateReturns());
            ps.setInt(12, score.getDamageIncidents());
            ps.setInt(13, score.getPaymentFailures());
            ps.setInt(14, score.getPaymentSuccess());
            ps.setBigDecimal(15, score.getAvgReviewRating());
            return ps;
        }, kh);
        return kh.getKey().longValue();
    }

    public void update(TrustScore score) {
        jdbcTemplate.update("""
            UPDATE user_trust_scores SET
                driving_behavior_score    = ?,
                damage_history_score      = ?,
                payment_reliability_score = ?,
                cancellation_score        = ?,
                overall_score             = ?,
                trust_grade               = ?,
                total_bookings            = ?,
                completed_bookings        = ?,
                cancelled_bookings        = ?,
                late_returns              = ?,
                damage_incidents          = ?,
                payment_failures          = ?,
                payment_success           = ?,
                avg_review_rating         = ?,
                calculated_at             = NOW()
            WHERE user_id = ?
            """,
                score.getDrivingBehaviorScore(),
                score.getDamageHistoryScore(),
                score.getPaymentReliabilityScore(),
                score.getCancellationScore(),
                score.getOverallScore(),
                score.getTrustGrade(),
                score.getTotalBookings(),
                score.getCompletedBookings(),
                score.getCancelledBookings(),
                score.getLateReturns(),
                score.getDamageIncidents(),
                score.getPaymentFailures(),
                score.getPaymentSuccess(),
                score.getAvgReviewRating(),
                score.getUserId());
    }

    public void saveEvent(TrustScoreEvent event) {
        jdbcTemplate.update("""
            INSERT INTO trust_score_events
                (user_id, event_type, event_detail, score_impact, booking_id)
            VALUES (?,?,?,?,?)
            """,
                event.getUserId(),
                event.getEventType(),
                event.getEventDetail(),
                event.getScoreImpact(),
                event.getBookingId());
    }

    public List<TrustScoreEvent> findEventsByUserId(Long userId, int page, int size) {
        return jdbcTemplate.query(
                "SELECT * FROM trust_score_events WHERE user_id = ? ORDER BY created_at DESC LIMIT ? OFFSET ?",
                eventMapper, userId, size, page * size);
    }

    // ── Aggregates used for recalculation ────────────────────────────────────

    public int countBookings(Long userId) {
        return count("SELECT COUNT(*) FROM bookings WHERE customer_id = ?", userId);
    }

    public int countCompletedBookings(Long userId) {
        return count("SELECT COUNT(*) FROM bookings WHERE customer_id = ? AND status='COMPLETED'", userId);
    }

    public int countCancelledBookings(Long userId) {
        return count("SELECT COUNT(*) FROM bookings WHERE customer_id = ? AND status='CANCELLED'", userId);
    }

    /**
     * ✅ FIX: Original query used `actual_return_date > return_date` but that column
     * did not exist in the original schema. Now uses the migration-added column with
     * a safe fallback — counts zero if column not present.
     */
    public int countLateReturns(Long userId) {
        try {
            return count("""
                SELECT COUNT(*) FROM bookings
                WHERE customer_id = ?
                  AND actual_return_date IS NOT NULL
                  AND actual_return_date > return_date
                """, userId);
        } catch (Exception e) {
            // Column does not exist yet (migration not run) — return 0 safely
            return 0;
        }
    }

    /**
     * ✅ FIX: Original query used `dr.is_customer_fault = TRUE` but that column
     * did not exist. Now uses migration-added column with safe fallback.
     */
    public int countDamageIncidents(Long userId) {
        try {
            return count("""
                SELECT COUNT(*) FROM damage_reports dr
                JOIN bookings b ON dr.booking_id = b.id
                WHERE b.customer_id = ? AND dr.is_customer_fault = TRUE
                """, userId);
        } catch (Exception e) {
            // Column does not exist yet — count all damage reports for user as fallback
            try {
                return count("""
                    SELECT COUNT(*) FROM damage_reports dr
                    JOIN bookings b ON dr.booking_id = b.id
                    WHERE b.customer_id = ? AND dr.charged_to_customer = TRUE
                    """, userId);
            } catch (Exception ex) {
                return 0;
            }
        }
    }

    public int countPaymentFailures(Long userId) {
        return count("""
            SELECT COUNT(*) FROM payments p
            JOIN bookings b ON p.booking_id = b.id
            WHERE b.customer_id = ? AND p.payment_status = 'FAILED'
            """, userId);
    }

    public int countPaymentSuccess(Long userId) {
        return count("""
            SELECT COUNT(*) FROM payments p
            JOIN bookings b ON p.booking_id = b.id
            WHERE b.customer_id = ? AND p.payment_status = 'SUCCESS'
            """, userId);
    }

    public Double getAvgReviewRating(Long userId) {
        return jdbcTemplate.queryForObject(
                "SELECT AVG(rating) FROM reviews WHERE customer_id = ?",
                Double.class, userId);
    }

    private int count(String sql, Long userId) {
        Integer c = jdbcTemplate.queryForObject(sql, Integer.class, userId);
        return c != null ? c : 0;
    }
}
