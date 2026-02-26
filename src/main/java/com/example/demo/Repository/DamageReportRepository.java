package com.example.demo.Repository;

import com.example.demo.Model.DamageReport;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DamageReportRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<DamageReport> rowMapper = (rs, rowNum) -> DamageReport.builder()
            .id(rs.getLong("id"))
            .carId(rs.getObject("car_id") != null ? rs.getLong("car_id") : null)
            .bookingId(rs.getLong("booking_id"))
            .reportedBy(rs.getLong("reported_by"))
            .severity(rs.getString("severity"))
            .description(rs.getString("description"))
            .repairCost(rs.getBigDecimal("repair_cost"))
            .isCustomerFault(rs.getBoolean("is_customer_fault"))
            .photoUrl(rs.getString("photo_url"))
            .status(rs.getString("status"))
            .createdAt(rs.getTimestamp("created_at") != null
                    ? rs.getTimestamp("created_at").toLocalDateTime() : null)
            .updatedAt(rs.getTimestamp("updated_at") != null
                    ? rs.getTimestamp("updated_at").toLocalDateTime() : null)
            .build();

    public long save(DamageReport report) {
        String sql = """
            INSERT INTO damage_reports
                (car_id, booking_id, reported_by, description, severity, repair_cost, is_customer_fault, photo_url, status)
            VALUES (?,?,?,?,?,?,?,?,?)
            """;
        KeyHolder kh = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            if (report.getCarId() != null) ps.setLong(1, report.getCarId());
            else ps.setNull(1, Types.BIGINT);
            ps.setLong(2, report.getBookingId());
            ps.setLong(3, report.getReportedBy());
            ps.setString(4, report.getDescription());
            ps.setString(5, report.getSeverity() != null ? report.getSeverity() : "MINOR");
            if (report.getRepairCost() != null) ps.setBigDecimal(6, report.getRepairCost());
            else ps.setNull(6, Types.DECIMAL);
            ps.setBoolean(7, report.getIsCustomerFault() == null || report.getIsCustomerFault());
            ps.setString(8, report.getPhotoUrl());
            ps.setString(9, "OPEN");
            return ps;
        }, kh);
        return kh.getKey().longValue();
    }

    public Optional<DamageReport> findById(Long id) {
        List<DamageReport> list = jdbcTemplate.query(
                "SELECT * FROM damage_reports WHERE id = ?", rowMapper, id);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public List<DamageReport> findByBookingId(Long bookingId) {
        return jdbcTemplate.query(
                "SELECT * FROM damage_reports WHERE booking_id = ? ORDER BY created_at DESC",
                rowMapper, bookingId);
    }

    public List<DamageReport> findAll(int page, int size) {
        return jdbcTemplate.query(
                "SELECT * FROM damage_reports ORDER BY created_at DESC LIMIT ? OFFSET ?",
                rowMapper, size, page * size);
    }

    public void updateStatus(Long id, String status) {
        jdbcTemplate.update(
                "UPDATE damage_reports SET status = ?, updated_at = NOW() WHERE id = ?", status, id);
    }
}