package com.example.demo.Repository;

import com.example.demo.Model.Vendor;
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
public class VendorRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Vendor> vendorRowMapper = (rs, rowNum) -> Vendor.builder()
            .id(rs.getLong("id"))
            .userId(rs.getLong("user_id"))
            .companyName(rs.getString("company_name"))
            .licenseNumber(rs.getString("license_number"))
            .gstNumber(rs.getString("gst_number"))
            .rating(rs.getBigDecimal("rating"))
            .isApproved(rs.getBoolean("is_approved"))
            .createdAt(rs.getTimestamp("created_at") != null
                    ? rs.getTimestamp("created_at").toLocalDateTime() : null)
            // ✅ ADD THESE:
            .userFullName(rs.getString("full_name"))
            .userEmail(rs.getString("email"))
            .userPhone(rs.getString("phone"))
            .build();

    public List<Vendor> findAll(int page, int size) {
        return jdbcTemplate.query(
            "SELECT v.*, u.full_name, u.email, u.phone " +
            "FROM vendors v " +
            "LEFT JOIN users u ON v.user_id = u.id " +
            "ORDER BY v.created_at DESC LIMIT ? OFFSET ?",
            vendorRowMapper, size, page * size);
    }

    public Optional<Vendor> findById(Long id) {
        List<Vendor> list = jdbcTemplate.query(
            "SELECT v.*, u.full_name, u.email, u.phone " +
            "FROM vendors v LEFT JOIN users u ON v.user_id = u.id " +
            "WHERE v.id = ?",
            vendorRowMapper, id);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public Optional<Vendor> findByUserId(Long userId) {
        List<Vendor> list = jdbcTemplate.query(
            "SELECT v.*, u.full_name, u.email, u.phone " +
            "FROM vendors v LEFT JOIN users u ON v.user_id = u.id " +
            "WHERE v.user_id = ?",
            vendorRowMapper, userId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public List<Vendor> findPending(int page, int size) {
        return jdbcTemplate.query(
            "SELECT v.*, u.full_name, u.email, u.phone " +
            "FROM vendors v LEFT JOIN users u ON v.user_id = u.id " +
            "WHERE v.is_approved = false LIMIT ? OFFSET ?",
            vendorRowMapper, size, page * size);
    }

    public long save(Vendor vendor) {
        String sql = "INSERT INTO vendors (user_id, company_name, license_number, gst_number, rating, is_approved) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, vendor.getUserId());
            ps.setString(2, vendor.getCompanyName());
            ps.setString(3, vendor.getLicenseNumber());
            ps.setString(4, vendor.getGstNumber());
            ps.setBigDecimal(5, vendor.getRating() != null ? vendor.getRating() : java.math.BigDecimal.ZERO);
            ps.setBoolean(6, vendor.isApproved());
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public void update(Vendor vendor) {
        String sql = "UPDATE vendors SET company_name=?, license_number=?, gst_number=? WHERE id=?";
        jdbcTemplate.update(sql,
                vendor.getCompanyName(), vendor.getLicenseNumber(),
                vendor.getGstNumber(), vendor.getId());
    }

    public void updateApproval(Long id, boolean approved) {
        jdbcTemplate.update("UPDATE vendors SET is_approved = ? WHERE id = ?", approved, id);
    }

    public long countAll() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM vendors", Long.class);
        return count != null ? count : 0;
    }

    public long countPending() {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM vendors WHERE is_approved = false", Long.class);
        return count != null ? count : 0;
    }
}