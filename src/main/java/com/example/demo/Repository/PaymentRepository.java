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

    private final RowMapper<Payment> rowMapper = (rs, rowNum) -> Payment.builder()
            .id(rs.getLong("id"))
            .bookingId(rs.getLong("booking_id"))
            .transactionId(rs.getString("transaction_id"))
            .amount(rs.getBigDecimal("amount"))
            .paymentMethod(rs.getString("payment_method"))
            .paymentStatus(rs.getString("payment_status"))
            .paymentDate(rs.getTimestamp("payment_date") != null
                    ? rs.getTimestamp("payment_date").toLocalDateTime() : null)
            .createdAt(rs.getTimestamp("created_at") != null
                    ? rs.getTimestamp("created_at").toLocalDateTime() : null)
            .build();

    public List<Payment> findAll(int page, int size) {
        return jdbcTemplate.query(
                "SELECT * FROM payments ORDER BY created_at DESC LIMIT ? OFFSET ?",
                rowMapper, size, page * size);
    }

    public Optional<Payment> findById(Long id) {
        List<Payment> list = jdbcTemplate.query(
                "SELECT * FROM payments WHERE id = ?", rowMapper, id);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public Optional<Payment> findByBookingId(Long bookingId) {
        List<Payment> list = jdbcTemplate.query(
                "SELECT * FROM payments WHERE booking_id = ?", rowMapper, bookingId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public long save(Payment payment) {
        String sql = "INSERT INTO payments (booking_id, transaction_id, amount, payment_method, " +
                "payment_status, payment_date) VALUES (?, ?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, payment.getBookingId());
            ps.setString(2, payment.getTransactionId());
            ps.setBigDecimal(3, payment.getAmount());
            ps.setString(4, payment.getPaymentMethod() != null ? payment.getPaymentMethod() : "CASH");
            ps.setString(5, payment.getPaymentStatus() != null ? payment.getPaymentStatus() : "PENDING");
            if (payment.getPaymentDate() != null)
                ps.setTimestamp(6, java.sql.Timestamp.valueOf(payment.getPaymentDate()));
            else
                ps.setNull(6, java.sql.Types.TIMESTAMP);
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public void updateStatus(Long id, String status) {
        jdbcTemplate.update("UPDATE payments SET payment_status = ? WHERE id = ?", status, id);
    }

    public BigDecimal sumSuccessfulPayments() {
        BigDecimal total = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(amount), 0) FROM payments WHERE payment_status = 'SUCCESS'",
                BigDecimal.class);
        return total != null ? total : BigDecimal.ZERO;
    }
}