package com.example.demo.Repository;

import com.example.demo.DTOs.ReviewResponse;
import com.example.demo.Model.Review;
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
public class ReviewRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Review> rowMapper = (rs, rowNum) -> Review.builder()
            .id(rs.getLong("id"))
            .bookingId(rs.getLong("booking_id"))
            .customerId(rs.getLong("customer_id"))
            .carId(rs.getLong("car_id"))
            .rating(rs.getInt("rating"))
            .comment(rs.getString("comment"))
            .createdAt(rs.getTimestamp("created_at") != null
                    ? rs.getTimestamp("created_at").toLocalDateTime() : null)
            .build();

    public List<ReviewResponse> findAll(int page, int size) {

        String sql = """
            SELECT 
                r.id,
                r.rating,
                r.comment,
                r.created_at,
                u.full_name AS customerName,
                c.model AS carName
            FROM reviews r
            JOIN users u ON r.customer_id = u.id
            JOIN cars c ON r.car_id = c.id
            ORDER BY r.created_at DESC
            LIMIT ? OFFSET ?
        """;

        return jdbcTemplate.query(sql, (rs, rowNum) ->
                ReviewResponse.builder()
                        .id(rs.getLong("id"))
                        .rating(rs.getInt("rating"))
                        .comment(rs.getString("comment"))
                        .createdAt(rs.getTimestamp("created_at") != null
                                ? rs.getTimestamp("created_at").toLocalDateTime() : null)
                        .customerName(rs.getString("customerName"))
                        .carName(rs.getString("carName"))
                        .build(),
                size, page * size
        );
    }
    public List<Review> findByCarId(Long carId, int page, int size) {
        return jdbcTemplate.query(
                "SELECT * FROM reviews WHERE car_id = ? ORDER BY created_at DESC LIMIT ? OFFSET ?",
                rowMapper, carId, size, page * size);
    }

    public Optional<Review> findByBookingIdAndCustomerId(Long bookingId, Long customerId) {
        List<Review> list = jdbcTemplate.query(
                "SELECT * FROM reviews WHERE booking_id = ? AND customer_id = ?",
                rowMapper, bookingId, customerId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public long save(Review review) {
        String sql = "INSERT INTO reviews (booking_id, customer_id, car_id, rating, comment) " +
                "VALUES (?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, review.getBookingId());
            ps.setLong(2, review.getCustomerId());
            ps.setLong(3, review.getCarId());
            ps.setInt(4, review.getRating());
            ps.setString(5, review.getComment());
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public Double getAverageRatingByCarId(Long carId) {
        Double avg = jdbcTemplate.queryForObject(
                "SELECT AVG(rating) FROM reviews WHERE car_id = ?", Double.class, carId);
        return avg != null ? avg : 0.0;
    }
}