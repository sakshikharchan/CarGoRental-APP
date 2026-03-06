package com.example.demo.Repository;

import com.example.demo.Model.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PaymentRepository {

    private final JdbcTemplate jdbcTemplate;

    // ── Helper: normalize null/blank strings safely ───────────────────────────
    private String normalize(String value, String fallback) {
        return (value != null && !value.isBlank()) ? value : fallback;
    }

    // ── Enriched row mapper — payments + booking number + customer name ────────
    // ✅ Single mapper used for ALL queries — normalizes null/empty fields at read time
    private final RowMapper<Payment> enrichedRowMapper = (rs, rowNum) -> Payment.builder()
            .id(rs.getLong("id"))
            .bookingId(rs.getLong("booking_id"))
            .transactionId(rs.getString("transaction_id"))
            .amount(rs.getBigDecimal("amount"))
            .paymentMethod(normalize(rs.getString("payment_method"), "UNKNOWN"))
            .paymentStatus(normalize(rs.getString("payment_status"), "PENDING"))
            .paymentDate(rs.getTimestamp("payment_date") != null
                    ? rs.getTimestamp("payment_date").toLocalDateTime() : null)
            .createdAt(rs.getTimestamp("created_at") != null
                    ? rs.getTimestamp("created_at").toLocalDateTime() : null)
            .bookingNumber(rs.getString("booking_number"))
            .customerName(rs.getString("customer_name"))
            .build();

    // ── findAll with customer name + booking number JOIN ──────────────────────
    public List<Payment> findAll(int page, int size) {
        String sql = """
                SELECT p.*,
                       b.booking_number,
                       u.full_name AS customer_name
                FROM payments p
                LEFT JOIN bookings b ON b.id = p.booking_id
                LEFT JOIN users u    ON u.id = b.customer_id
                ORDER BY p.created_at DESC
                LIMIT ? OFFSET ?
                """;
        return jdbcTemplate.query(sql, enrichedRowMapper, size, page * size);
    }

    // ── findById with JOIN ────────────────────────────────────────────────────
    public Optional<Payment> findById(Long id) {
        String sql = """
                SELECT p.*,
                       b.booking_number,
                       u.full_name AS customer_name
                FROM payments p
                LEFT JOIN bookings b ON b.id = p.booking_id
                LEFT JOIN users u    ON u.id = b.customer_id
                WHERE p.id = ?
                """;
        List<Payment> list = jdbcTemplate.query(sql, enrichedRowMapper, id);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    // ── findByBookingId with JOIN ─────────────────────────────────────────────
    public Optional<Payment> findByBookingId(Long bookingId) {
        String sql = """
                SELECT p.*,
                       b.booking_number,
                       u.full_name AS customer_name
                FROM payments p
                LEFT JOIN bookings b ON b.id = p.booking_id
                LEFT JOIN users u    ON u.id = b.customer_id
                WHERE p.booking_id = ?
                """;
        List<Payment> list = jdbcTemplate.query(sql, enrichedRowMapper, bookingId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    // ── save ──────────────────────────────────────────────────────────────────
    public long save(Payment payment) {
        String sql = "INSERT INTO payments (booking_id, transaction_id, amount, payment_method, " +
                "payment_status, payment_date) VALUES (?, ?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, payment.getBookingId());
            ps.setString(2, payment.getTransactionId());
            ps.setBigDecimal(3, payment.getAmount());
            // ✅ Never insert null/blank — always fallback
            ps.setString(4, normalize(payment.getPaymentMethod(), "CASH"));
            ps.setString(5, normalize(payment.getPaymentStatus(), "PENDING"));
            if (payment.getPaymentDate() != null)
                ps.setTimestamp(6, java.sql.Timestamp.valueOf(payment.getPaymentDate()));
            else
                ps.setNull(6, java.sql.Types.TIMESTAMP);
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    // ── updateStatus ──────────────────────────────────────────────────────────
    public void updateStatus(Long id, String status) {
        jdbcTemplate.update(
                "UPDATE payments SET payment_status = ? WHERE id = ?",
                status, id);
    }

    // ── For Razorpay — update transaction ID only (order_id → payment_id) ────
    public void updateTransactionId(Long id, String transactionId) {
        jdbcTemplate.update(
                "UPDATE payments SET transaction_id = ? WHERE id = ?",
                transactionId, id);
    }

    // ── For Razorpay — update transaction ID + status + method in one shot ────
    // ✅ Also sets payment_method = 'UPI' since Razorpay payments are always online
    public void updateTransactionIdAndStatus(Long id, String transactionId, String status) {
        jdbcTemplate.update(
                "UPDATE payments SET transaction_id = ?, payment_status = ?, " +
                "payment_method = 'UPI', payment_date = NOW() WHERE id = ?",
                transactionId, status, id);
    }

    // ── sumSuccessfulPayments ─────────────────────────────────────────────────
    public BigDecimal sumSuccessfulPayments() {
        BigDecimal total = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(amount), 0) FROM payments WHERE payment_status = 'SUCCESS'",
                BigDecimal.class);
        return total != null ? total : BigDecimal.ZERO;
    }
}