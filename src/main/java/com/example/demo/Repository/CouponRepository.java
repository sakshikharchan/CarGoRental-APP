package com.example.demo.Repository;

import com.example.demo.Model.Coupon;
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
public class CouponRepository {
    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Coupon> rowMapper = (rs, rowNum) -> Coupon.builder()
            .id(rs.getLong("id"))
            .code(rs.getString("code"))
            .description(rs.getString("description"))
            .discountType(rs.getString("discount_type"))
            .discountValue(rs.getBigDecimal("discount_value"))
            .minBookingAmount(rs.getBigDecimal("min_booking_amount"))
            .maxDiscount(rs.getBigDecimal("max_discount"))
            .usageLimit(rs.getObject("usage_limit") != null ? rs.getInt("usage_limit") : null)
            .usedCount(rs.getInt("used_count"))
            .isActive(rs.getBoolean("is_active"))
            .validFrom(rs.getDate("valid_from") != null ? rs.getDate("valid_from").toLocalDate() : null)
            .validUntil(rs.getDate("valid_until") != null ? rs.getDate("valid_until").toLocalDate() : null)
            .createdAt(rs.getTimestamp("created_at") != null
                    ? rs.getTimestamp("created_at").toLocalDateTime() : null)
            .build();

    public List<Coupon> findAll() {
        return jdbcTemplate.query("SELECT * FROM coupons ORDER BY created_at DESC", rowMapper);
    }

    public Optional<Coupon> findByCode(String code) {
        List<Coupon> list = jdbcTemplate.query(
                "SELECT * FROM coupons WHERE code = ?", rowMapper, code.toUpperCase());
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public long save(Coupon coupon) {
        String sql = "INSERT INTO coupons (code, description, discount_type, discount_value, min_booking_amount, max_discount, usage_limit, valid_from, valid_until) VALUES (?,?,?,?,?,?,?,?,?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, coupon.getCode().toUpperCase());
            ps.setString(2, coupon.getDescription());
            ps.setString(3, coupon.getDiscountType());
            ps.setBigDecimal(4, coupon.getDiscountValue());
            ps.setBigDecimal(5, coupon.getMinBookingAmount());
            ps.setBigDecimal(6, coupon.getMaxDiscount());
            if (coupon.getUsageLimit() != null) ps.setInt(7, coupon.getUsageLimit());
            else ps.setNull(7, java.sql.Types.INTEGER);
            if (coupon.getValidFrom() != null) ps.setDate(8, java.sql.Date.valueOf(coupon.getValidFrom()));
            else ps.setNull(8, java.sql.Types.DATE);
            if (coupon.getValidUntil() != null) ps.setDate(9, java.sql.Date.valueOf(coupon.getValidUntil()));
            else ps.setNull(9, java.sql.Types.DATE);
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public void incrementUsage(String code) {
        jdbcTemplate.update("UPDATE coupons SET used_count = used_count + 1 WHERE code = ?", code.toUpperCase());
    }

    public void toggleActive(Long id, boolean active) {
        jdbcTemplate.update("UPDATE coupons SET is_active = ? WHERE id = ?", active, id);
    }
}