package com.example.demo.Repository;

import com.example.demo.Model.Booking;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.List;
import java.util.Optional;

@Repository
public class BookingRepository {

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    public BookingRepository(JdbcTemplate jdbcTemplate, DataSource dataSource) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
    }

    // ✅ FIX: Safe column reading with null-fallback for columns that may not exist yet
    private static <T> T safeGet(ResultSet rs, String col, Class<T> type) {
        try {
            Object val = rs.getObject(col);
            if (val == null) return null;
            if (type == Long.class) return type.cast(rs.getLong(col));
            if (type == String.class) return type.cast(rs.getString(col));
            if (type == java.math.BigDecimal.class) return type.cast(rs.getBigDecimal(col));
            if (type == Boolean.class) return type.cast(rs.getBoolean(col));
            if (type == Integer.class) return type.cast(rs.getInt(col));
            return type.cast(val);
        } catch (SQLException e) {
            return null; // column doesn't exist yet → return null safely
        }
    }

    // Base row mapper for booking columns only (used by save/findById when no JOIN)
    private final RowMapper<Booking> baseRowMapper = (rs, rowNum) -> Booking.builder()
            .id(rs.getLong("id"))
            .bookingNumber(rs.getString("booking_number"))
            .customerId(rs.getLong("customer_id"))
            .carId(rs.getLong("car_id"))
            .vendorId(safeGet(rs, "vendor_id", Long.class))
            .driverId(rs.getObject("driver_id") != null ? rs.getLong("driver_id") : null)
            .pickupLocation(rs.getString("pickup_location"))
            .dropoffLocation(rs.getString("dropoff_location"))
            .pickupDate(rs.getDate("pickup_date") != null ? rs.getDate("pickup_date").toLocalDate() : null)
            .returnDate(rs.getDate("return_date") != null ? rs.getDate("return_date").toLocalDate() : null)
            .actualReturnDate(safeGet(rs, "actual_return_date", String.class) != null
                    ? rs.getDate("actual_return_date") != null ? rs.getDate("actual_return_date").toLocalDate() : null
                    : null)
            .totalDays(rs.getInt("total_days"))
            .dailyRate(rs.getBigDecimal("daily_rate"))
            .totalAmount(rs.getBigDecimal("total_amount"))
            .withDriver(rs.getBoolean("with_driver"))
            .driverCharge(rs.getBigDecimal("driver_charge"))
            .discountAmount(safeGet(rs, "discount_amount", java.math.BigDecimal.class))
            .couponCode(safeGet(rs, "coupon_code", String.class))
            .status(rs.getString("status"))
            .notes(rs.getString("notes"))
            .adminNotes(safeGet(rs, "admin_notes", String.class))
            .cancellationReason(safeGet(rs, "cancellation_reason", String.class))
            .cancelledBy(safeGet(rs, "cancelled_by", String.class))
            .createdAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null)
            .updatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null)
            .build();

    // Full row mapper with JOINed fields (customer, car, driver, vendor names)
    private final RowMapper<Booking> rowMapper = (rs, rowNum) -> {
        Booking b = baseRowMapper.mapRow(rs, rowNum);
        b.setCustomerName(safeGet(rs, "customer_name", String.class));
        b.setCustomerEmail(safeGet(rs, "customer_email", String.class));
        b.setCustomerPhone(safeGet(rs, "customer_phone", String.class));
        b.setCarBrand(safeGet(rs, "car_brand", String.class));
        b.setCarModel(safeGet(rs, "car_model", String.class));
        b.setCarRegistrationNo(safeGet(rs, "car_registration_no", String.class));
        b.setDriverName(safeGet(rs, "driver_name", String.class));
        b.setVendorCompanyName(safeGet(rs, "vendor_company_name", String.class));
        return b;
    };

    private static final String BOOKING_JOIN_SQL =
            "SELECT b.*, " +
            "u.full_name AS customer_name, u.email AS customer_email, u.phone AS customer_phone, " +
            "c.brand AS car_brand, c.model AS car_model, c.registration_no AS car_registration_no, " +
            "d_u.full_name AS driver_name, " +
            "v.company_name AS vendor_company_name " +
            "FROM bookings b " +
            "LEFT JOIN users u ON b.customer_id = u.id " +
            "LEFT JOIN cars c ON b.car_id = c.id " +
            "LEFT JOIN drivers d ON b.driver_id = d.id " +
            "LEFT JOIN users d_u ON d.user_id = d_u.id " +
            "LEFT JOIN vendors v ON b.vendor_id = v.id";

    /**
     * ✅ FIX: Save booking — uses raw JDBC, maps ALL new columns explicitly
     */
    public long save(Booking booking) {
        String sql = "INSERT INTO bookings " +
            "(booking_number, customer_id, car_id, vendor_id, driver_id, " +
            "pickup_location, dropoff_location, pickup_date, return_date, " +
            "total_days, daily_rate, total_amount, with_driver, " +
            "driver_charge, discount_amount, coupon_code, status, notes) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, booking.getBookingNumber());
            ps.setLong(2, booking.getCustomerId());
            ps.setLong(3, booking.getCarId());

            if (booking.getVendorId() != null) ps.setLong(4, booking.getVendorId());
            else ps.setNull(4, Types.BIGINT);

            if (booking.getDriverId() != null) ps.setLong(5, booking.getDriverId());
            else ps.setNull(5, Types.BIGINT);

            ps.setString(6, booking.getPickupLocation() != null ? booking.getPickupLocation() : "");
            ps.setString(7, booking.getDropoffLocation() != null ? booking.getDropoffLocation() : "");

            // ✅ KEY FIX: Explicit LocalDate → java.sql.Date conversion
            ps.setDate(8, Date.valueOf(booking.getPickupDate()));
            ps.setDate(9, Date.valueOf(booking.getReturnDate()));

            ps.setInt(10, booking.getTotalDays());
            ps.setBigDecimal(11, booking.getDailyRate());
            ps.setBigDecimal(12, booking.getTotalAmount());
            ps.setBoolean(13, Boolean.TRUE.equals(booking.getWithDriver()));
            ps.setBigDecimal(14, booking.getDriverCharge() != null ? booking.getDriverCharge() : java.math.BigDecimal.ZERO);
            ps.setBigDecimal(15, booking.getDiscountAmount() != null ? booking.getDiscountAmount() : java.math.BigDecimal.ZERO);

            if (booking.getCouponCode() != null) ps.setString(16, booking.getCouponCode());
            else ps.setNull(16, Types.VARCHAR);

            ps.setString(17, booking.getStatus() != null ? booking.getStatus() : "PENDING");

            if (booking.getNotes() != null) ps.setString(18, booking.getNotes());
            else ps.setNull(18, Types.VARCHAR);

            int rows = ps.executeUpdate();
            if (rows == 0) throw new RuntimeException("Booking insert failed — 0 rows affected");

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
                throw new RuntimeException("Booking insert failed — no generated key");
            }

        } catch (SQLException e) {
            throw new RuntimeException("Booking save failed: " + e.getMessage(), e);
        }
    }

    public List<Booking> findAll(int page, int size) {
        return jdbcTemplate.query(
                BOOKING_JOIN_SQL + " ORDER BY b.created_at DESC LIMIT ? OFFSET ?",
                rowMapper, size, page * size);
    }

    public List<Booking> findByStatus(String status, int page, int size) {
        return jdbcTemplate.query(
                BOOKING_JOIN_SQL + " WHERE b.status = ? ORDER BY b.created_at DESC LIMIT ? OFFSET ?",
                rowMapper, status, size, page * size);
    }

    public List<Booking> findByCustomerId(Long customerId, int page, int size) {
        return jdbcTemplate.query(
                BOOKING_JOIN_SQL + " WHERE b.customer_id = ? ORDER BY b.created_at DESC LIMIT ? OFFSET ?",
                rowMapper, customerId, size, page * size);
    }

    public List<Booking> findByVendorId(Long vendorId, int page, int size) {
        return jdbcTemplate.query(
                BOOKING_JOIN_SQL + " WHERE b.vendor_id = ? ORDER BY b.created_at DESC LIMIT ? OFFSET ?",
                rowMapper, vendorId, size, page * size);
    }

    public Optional<Booking> findById(Long id) {
        List<Booking> list = jdbcTemplate.query(
                BOOKING_JOIN_SQL + " WHERE b.id = ?", rowMapper, id);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public Optional<Booking> findByBookingNumber(String bookingNumber) {
        List<Booking> list = jdbcTemplate.query(
                BOOKING_JOIN_SQL + " WHERE b.booking_number = ?", rowMapper, bookingNumber);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public void updateStatus(Long id, String status) {
        jdbcTemplate.update(
                "UPDATE bookings SET status = ?, updated_at = NOW() WHERE id = ?",
                status, id);
    }

    public boolean hasActiveBookingForCar(Long carId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM bookings WHERE car_id = ? AND status IN ('PENDING','CONFIRMED','ACTIVE')",
                Integer.class, carId);
        return count != null && count > 0;
    }

    public long countAll() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM bookings", Long.class);
        return count != null ? count : 0;
    }

    public long countByStatus(String status) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM bookings WHERE status = ?", Long.class, status);
        return count != null ? count : 0;
    }

    public long countToday() {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM bookings WHERE DATE(created_at) = CURDATE()", Long.class);
        return count != null ? count : 0;
    }
}
