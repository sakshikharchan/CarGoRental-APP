package com.example.demo.Repository;

import com.example.demo.Model.Driver;
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
public class DriverRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Driver> rowMapper = (rs, rowNum) -> Driver.builder()
            .id(rs.getLong("id"))
            .userId(rs.getLong("user_id"))
            .vendorId(rs.getObject("vendor_id") != null ? rs.getLong("vendor_id") : null)
            .licenseNumber(rs.getString("license_number"))
            .experienceYears(rs.getInt("experience_years"))
            .isAvailable(rs.getBoolean("is_available"))
            .rating(rs.getBigDecimal("rating"))
            .createdAt(rs.getTimestamp("created_at") != null
                    ? rs.getTimestamp("created_at").toLocalDateTime() : null)
            // ✅ ADD THESE 4 lines:
            .userFullName(rs.getString("full_name"))
            .userEmail(rs.getString("email"))
            .userPhone(rs.getString("phone"))
            .vendorCompanyName(rs.getString("company_name"))
            .build();

    // ✅ FIX all queries to use JOIN
    public List<Driver> findAll(int page, int size) {
        return jdbcTemplate.query(
            "SELECT d.*, u.full_name, u.email, u.phone, v.company_name " +
            "FROM drivers d " +
            "LEFT JOIN users u ON d.user_id = u.id " +
            "LEFT JOIN vendors v ON d.vendor_id = v.id " +
            "ORDER BY d.created_at DESC LIMIT ? OFFSET ?",
            rowMapper, size, page * size);
    }

    public List<Driver> findAvailable(int page, int size) {
        return jdbcTemplate.query(
            "SELECT d.*, u.full_name, u.email, u.phone, v.company_name " +
            "FROM drivers d " +
            "LEFT JOIN users u ON d.user_id = u.id " +
            "LEFT JOIN vendors v ON d.vendor_id = v.id " +
            "WHERE d.is_available = true " +
            "ORDER BY d.created_at DESC LIMIT ? OFFSET ?",
            rowMapper, size, page * size);
    }


    public Optional<Driver> findById(Long id) {
        List<Driver> list = jdbcTemplate.query(
            "SELECT d.*, u.full_name, u.email, u.phone, v.company_name " +
            "FROM drivers d " +
            "LEFT JOIN users u ON d.user_id = u.id " +
            "LEFT JOIN vendors v ON d.vendor_id = v.id " +
            "WHERE d.id = ?",
            rowMapper, id);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public List<Driver> findByVendorId(Long vendorId, int page, int size) {
        return jdbcTemplate.query(
            "SELECT d.*, u.full_name, u.email, u.phone, v.company_name " +
            "FROM drivers d " +
            "LEFT JOIN users u ON d.user_id = u.id " +
            "LEFT JOIN vendors v ON d.vendor_id = v.id " +
            "WHERE d.vendor_id = ? LIMIT ? OFFSET ?",
            rowMapper, vendorId, size, page * size);
    }

    public long save(Driver driver) {
        String sql = "INSERT INTO drivers (user_id, vendor_id, license_number, experience_years, " +
                "is_available, rating) VALUES (?, ?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, driver.getUserId());
            if (driver.getVendorId() != null) ps.setLong(2, driver.getVendorId());
            else ps.setNull(2, java.sql.Types.BIGINT);
            ps.setString(3, driver.getLicenseNumber());
            ps.setInt(4, driver.getExperienceYears() != null ? driver.getExperienceYears() : 0);
            ps.setBoolean(5, driver.isAvailable());
            ps.setBigDecimal(6, driver.getRating() != null
                    ? driver.getRating() : java.math.BigDecimal.ZERO);
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public void updateAvailability(Long id, boolean available) {
        jdbcTemplate.update("UPDATE drivers SET is_available = ? WHERE id = ?", available, id);
    }

    public boolean existsByLicenseNumber(String licenseNumber) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM drivers WHERE license_number = ?", Integer.class, licenseNumber);
        return count != null && count > 0;
    }

    public long countAll() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM drivers", Long.class);
        return count != null ? count : 0;
    }
}