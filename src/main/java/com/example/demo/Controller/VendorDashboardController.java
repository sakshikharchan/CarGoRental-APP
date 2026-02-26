//package com.example.demo.Controller;
//
//import com.example.demo.DTOs.ApiResponse;
//import com.example.demo.Model.Booking;
//import com.example.demo.Model.Vendor;
//import com.example.demo.Service.VendorService;
//import com.example.demo.Service.BookingService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.ResponseEntity;
//import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.security.core.Authentication;
//import org.springframework.web.bind.annotation.*;
//
//import java.math.BigDecimal;
//import java.util.List;
//import java.util.Map;
//
//@RestController
//@RequestMapping("/api/vendor")
//@RequiredArgsConstructor
//@PreAuthorize("hasAnyAuthority('ROLE_VENDOR','ROLE_ADMIN')")
//public class VendorDashboardController {
//
//    private final VendorService vendorService;
//    private final BookingService bookingService;
//    private final JdbcTemplate jdbcTemplate;
//
//    // ── Profile ──────────────────────────────────────────────────────────────
//
//    @GetMapping("/profile")
//    public ResponseEntity<ApiResponse<Map<String, Object>>> getMyProfile(Authentication auth) {
//        Long userId = (Long) auth.getCredentials();
//        List<Map<String, Object>> result = jdbcTemplate.queryForList("""
//            SELECT v.*, u.full_name, u.email, u.phone,
//                   (SELECT COUNT(*) FROM cars WHERE vendor_id = v.id) AS totalCars,
//                   (SELECT COUNT(*) FROM cars WHERE vendor_id = v.id AND is_available = true) AS availableCars,
//                   (SELECT COUNT(*) FROM bookings WHERE vendor_id = v.id) AS totalBookings,
//                   (SELECT COALESCE(SUM(p.amount),0) FROM payments p
//                    JOIN bookings b ON p.booking_id = b.id
//                    WHERE b.vendor_id = v.id AND p.payment_status = 'SUCCESS') AS totalRevenue
//            FROM vendors v JOIN users u ON v.user_id = u.id
//            WHERE v.user_id = ?
//        """, userId);
//        if (result.isEmpty()) return ResponseEntity.notFound().build();
//        return ResponseEntity.ok(ApiResponse.success(result.get(0)));
//    }
//
//    // ── Revenue Dashboard ─────────────────────────────────────────────────────
//
//    @GetMapping("/revenue")
//    public ResponseEntity<ApiResponse<Map<String, Object>>> getRevenueDashboard(Authentication auth) {
//        Long userId = (Long) auth.getCredentials();
//        Long vendorId = getVendorId(userId);
//
//        // Monthly revenue current year
//        List<Map<String, Object>> monthly = jdbcTemplate.queryForList("""
//            SELECT MONTH(p.payment_date) AS month,
//                   COALESCE(SUM(p.amount), 0) AS revenue,
//                   COUNT(b.id) AS bookings
//            FROM payments p
//            JOIN bookings b ON p.booking_id = b.id
//            WHERE b.vendor_id = ? AND YEAR(p.payment_date) = YEAR(CURDATE())
//              AND p.payment_status = 'SUCCESS'
//            GROUP BY MONTH(p.payment_date)
//            ORDER BY month
//        """, vendorId);
//
//        // Commission breakdown
//        Map<String, Object> commission = jdbcTemplate.queryForMap("""
//            SELECT COALESCE(SUM(p.amount), 0) AS grossRevenue,
//                   COALESCE(SUM(p.amount) * 0.15, 0) AS commission,
//                   COALESCE(SUM(p.amount) * 0.85, 0) AS netRevenue
//            FROM payments p JOIN bookings b ON p.booking_id = b.id
//            WHERE b.vendor_id = ? AND p.payment_status = 'SUCCESS'
//        """, vendorId);
//
//        // Pending payouts
//        List<Map<String, Object>> pendingPayouts = jdbcTemplate.queryForList("""
//            SELECT * FROM vendor_payouts WHERE vendor_id = ? ORDER BY created_at DESC LIMIT 10
//        """, vendorId);
//
//        // Top cars by revenue
//        List<Map<String, Object>> topCars = jdbcTemplate.queryForList("""
//            SELECT c.brand, c.model, c.registration_no,
//                   COUNT(b.id) AS bookings,
//                   COALESCE(SUM(b.total_amount), 0) AS revenue
//            FROM bookings b JOIN cars c ON b.car_id = c.id
//            WHERE b.vendor_id = ? AND b.status = 'COMPLETED'
//            GROUP BY c.id ORDER BY revenue DESC LIMIT 5
//        """, vendorId);
//
//        return ResponseEntity.ok(ApiResponse.success(Map.of(
//            "monthlyRevenue", monthly,
//            "commission", commission,
//            "pendingPayouts", pendingPayouts,
//            "topCars", topCars
//        )));
//    }
//
//    // ── Fleet Management ──────────────────────────────────────────────────────
//
//    @GetMapping("/fleet")
//    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMyFleet(
//            Authentication auth,
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "10") int size) {
//        Long userId = (Long) auth.getCredentials();
//        Long vendorId = getVendorId(userId);
//        List<Map<String, Object>> fleet = jdbcTemplate.queryForList("""
//            SELECT c.*, cc.name AS categoryName,
//                   (SELECT COUNT(*) FROM bookings b WHERE b.car_id = c.id AND b.status = 'COMPLETED') AS totalTrips,
//                   (SELECT AVG(r.rating) FROM reviews r WHERE r.car_id = c.id) AS avgRating,
//                   (SELECT COUNT(*) FROM damage_reports dr WHERE dr.car_id = c.id AND dr.status != 'CLOSED') AS openDamages
//            FROM cars c
//            LEFT JOIN car_categories cc ON c.category_id = cc.id
//            WHERE c.vendor_id = ?
//            ORDER BY c.created_at DESC LIMIT ? OFFSET ?
//        """, vendorId, size, page * size);
//        return ResponseEntity.ok(ApiResponse.success(fleet));
//    }
//
//    // ── Bookings ──────────────────────────────────────────────────────────────
//
//    @GetMapping("/bookings")
//    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMyBookings(
//            Authentication auth,
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "10") int size,
//            @RequestParam(defaultValue = "") String status) {
//        Long userId = (Long) auth.getCredentials();
//        Long vendorId = getVendorId(userId);
//
//        StringBuilder sql = new StringBuilder("""
//            SELECT b.*, u.full_name AS customerName, u.phone AS customerPhone,
//                   c.brand AS carBrand, c.model AS carModel, c.registration_no,
//                   d.license_number AS driverLicense,
//                   du.full_name AS driverName
//            FROM bookings b
//            JOIN users u ON b.customer_id = u.id
//            JOIN cars c ON b.car_id = c.id
//            LEFT JOIN drivers d ON b.driver_id = d.id
//            LEFT JOIN users du ON d.user_id = du.id
//            WHERE b.vendor_id = ?
//        """);
//        List<Object> params = new java.util.ArrayList<>();
//        params.add(vendorId);
//        if (!status.isEmpty()) { sql.append(" AND b.status = ?"); params.add(status.toUpperCase()); }
//        sql.append(" ORDER BY b.created_at DESC LIMIT ? OFFSET ?");
//        params.add(size); params.add(page * size);
//
//        List<Map<String, Object>> bookings = jdbcTemplate.queryForList(sql.toString(), params.toArray());
//        return ResponseEntity.ok(ApiResponse.success(bookings));
//    }
//
//    @PatchMapping("/bookings/{id}/approve")
//    public ResponseEntity<ApiResponse<Void>> approveBooking(
//            @PathVariable Long id,
//            @RequestParam boolean approved,
//            Authentication auth) {
//        Long userId = (Long) auth.getCredentials();
//        Long vendorId = getVendorId(userId);
//
//        // Verify booking belongs to vendor
//        Integer count = jdbcTemplate.queryForObject(
//            "SELECT COUNT(*) FROM bookings WHERE id = ? AND vendor_id = ?",
//            Integer.class, id, vendorId);
//        if (count == null || count == 0)
//            return ResponseEntity.status(403).body(ApiResponse.error("Not authorized"));
//
//        String newStatus = approved ? "CONFIRMED" : "CANCELLED";
//        jdbcTemplate.update("UPDATE bookings SET status = ?, updated_at = NOW() WHERE id = ?", newStatus, id);
//        return ResponseEntity.ok(ApiResponse.success("Booking " + (approved ? "approved" : "rejected"), null));
//    }
//
//    @PatchMapping("/bookings/{id}/assign-driver")
//    public ResponseEntity<ApiResponse<Void>> assignDriver(
//            @PathVariable Long id,
//            @RequestBody Map<String, Long> body,
//            Authentication auth) {
//        Long userId = (Long) auth.getCredentials();
//        Long vendorId = getVendorId(userId);
//        Long driverId = body.get("driverId");
//
//        Integer count = jdbcTemplate.queryForObject(
//            "SELECT COUNT(*) FROM bookings WHERE id = ? AND vendor_id = ?",
//            Integer.class, id, vendorId);
//        if (count == null || count == 0)
//            return ResponseEntity.status(403).body(ApiResponse.error("Not authorized"));
//
//        jdbcTemplate.update("UPDATE bookings SET driver_id = ?, updated_at = NOW() WHERE id = ?", driverId, id);
//        // Create driver trip record
//        jdbcTemplate.update("INSERT IGNORE INTO driver_trips (driver_id, booking_id) VALUES (?, ?)", driverId, id);
//        return ResponseEntity.ok(ApiResponse.success("Driver assigned", null));
//    }
//
//    // ── Damage Reports ────────────────────────────────────────────────────────
//
//    @GetMapping("/damage-reports")
//    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getDamageReports(Authentication auth) {
//        Long userId = (Long) auth.getCredentials();
//        Long vendorId = getVendorId(userId);
//        List<Map<String, Object>> reports = jdbcTemplate.queryForList("""
//            SELECT dr.*, c.brand, c.model, c.registration_no,
//                   u.full_name AS reportedByName
//            FROM damage_reports dr
//            JOIN cars c ON dr.car_id = c.id
//            JOIN users u ON dr.reported_by = u.id
//            WHERE c.vendor_id = ?
//            ORDER BY dr.created_at DESC
//        """, vendorId);
//        return ResponseEntity.ok(ApiResponse.success(reports));
//    }
//
//    @PostMapping("/damage-reports")
//    public ResponseEntity<ApiResponse<Void>> reportDamage(
//            @RequestBody Map<String, Object> body,
//            Authentication auth) {
//        Long userId = (Long) auth.getCredentials();
//        jdbcTemplate.update("""
//            INSERT INTO damage_reports (car_id, booking_id, reported_by, description, severity, estimated_cost)
//            VALUES (?, ?, ?, ?, ?, ?)
//        """,
//            body.get("carId"), body.get("bookingId"), userId,
//            body.get("description"), body.get("severity"), body.get("estimatedCost"));
//        return ResponseEntity.ok(ApiResponse.success("Damage report submitted", null));
//    }
//
//    // ── Drivers ───────────────────────────────────────────────────────────────
//
//    @GetMapping("/drivers")
//    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMyDrivers(Authentication auth) {
//        Long userId = (Long) auth.getCredentials();
//        Long vendorId = getVendorId(userId);
//        List<Map<String, Object>> drivers = jdbcTemplate.queryForList("""
//            SELECT d.*, u.full_name, u.email, u.phone,
//                   (SELECT COUNT(*) FROM driver_trips dt WHERE dt.driver_id = d.id AND dt.status = 'COMPLETED') AS totalTrips
//            FROM drivers d JOIN users u ON d.user_id = u.id
//            WHERE d.vendor_id = ?
//            ORDER BY d.created_at DESC
//        """, vendorId);
//        return ResponseEntity.ok(ApiResponse.success(drivers));
//    }
//
//    // ── Payouts ───────────────────────────────────────────────────────────────
//
//    @GetMapping("/payouts")
//    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMyPayouts(Authentication auth) {
//        Long userId = (Long) auth.getCredentials();
//        Long vendorId = getVendorId(userId);
//        List<Map<String, Object>> payouts = jdbcTemplate.queryForList("""
//            SELECT * FROM vendor_payouts WHERE vendor_id = ? ORDER BY created_at DESC
//        """, vendorId);
//        return ResponseEntity.ok(ApiResponse.success(payouts));
//    }
//
//    // ── Disputes ──────────────────────────────────────────────────────────────
//
//    @GetMapping("/disputes")
//    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMyDisputes(Authentication auth) {
//        Long userId = (Long) auth.getCredentials();
//        Long vendorId = getVendorId(userId);
//        List<Map<String, Object>> disputes = jdbcTemplate.queryForList("""
//            SELECT d.*, b.booking_number, u.full_name AS raisedByName
//            FROM disputes d
//            JOIN bookings b ON d.booking_id = b.id
//            JOIN users u ON d.raised_by = u.id
//            WHERE b.vendor_id = ?
//            ORDER BY d.created_at DESC
//        """, vendorId);
//        return ResponseEntity.ok(ApiResponse.success(disputes));
//    }
//
//    // ── Helper ────────────────────────────────────────────────────────────────
//
//    private Long getVendorId(Long userId) {
//        Long vendorId = jdbcTemplate.queryForObject(
//            "SELECT id FROM vendors WHERE user_id = ?", Long.class, userId);
//        if (vendorId == null) throw new RuntimeException("Vendor profile not found");
//        return vendorId;
//    }
//}

package com.example.demo.Controller;

import com.example.demo.DTOs.ApiResponse;
import com.example.demo.Model.Booking;
import com.example.demo.Model.Vendor;
import com.example.demo.Service.VendorService;
import com.example.demo.Service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vendor")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_VENDOR','ROLE_ADMIN')")
public class VendorDashboardController {

    private final VendorService vendorService;
    private final BookingService bookingService;
    private final JdbcTemplate jdbcTemplate;

    // ── Profile ──────────────────────────────────────────────────────────────

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMyProfile(Authentication auth) {
        Long userId = (Long) auth.getCredentials();
        List<Map<String, Object>> result = jdbcTemplate.queryForList("""
                SELECT v.*, u.full_name, u.email, u.phone,
                       (SELECT COUNT(*) FROM cars WHERE vendor_id = v.id) AS totalCars,
                       (SELECT COUNT(*) FROM cars WHERE vendor_id = v.id AND is_available = true) AS availableCars,
                       (SELECT COUNT(*) FROM bookings WHERE vendor_id = v.id) AS totalBookings,
                       (SELECT COALESCE(SUM(p.amount),0) FROM payments p
                        JOIN bookings b ON p.booking_id = b.id
                        WHERE b.vendor_id = v.id AND p.payment_status = 'SUCCESS') AS totalRevenue
                FROM vendors v JOIN users u ON v.user_id = u.id
                WHERE v.user_id = ?
                """, userId);
        if (result.isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(ApiResponse.success(result.get(0)));
    }

    // ── Revenue Dashboard ─────────────────────────────────────────────────────

    @GetMapping("/revenue")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRevenueDashboard(Authentication auth) {
        Long userId = (Long) auth.getCredentials();

        // ✅ FIX: Return 404 instead of 500 if vendor profile missing
        Long vendorId = getVendorIdOrNull(userId);
        if (vendorId == null) return ResponseEntity.notFound().build();

        List<Map<String, Object>> monthly = jdbcTemplate.queryForList("""
                SELECT MONTH(p.payment_date) AS month,
                       COALESCE(SUM(p.amount), 0) AS revenue,
                       COUNT(b.id) AS bookings
                FROM payments p
                JOIN bookings b ON p.booking_id = b.id
                WHERE b.vendor_id = ? AND YEAR(p.payment_date) = YEAR(CURDATE())
                  AND p.payment_status = 'SUCCESS'
                GROUP BY MONTH(p.payment_date)
                ORDER BY month
                """, vendorId);

        Map<String, Object> commission = jdbcTemplate.queryForMap("""
                SELECT COALESCE(SUM(p.amount), 0) AS grossRevenue,
                       COALESCE(SUM(p.amount) * 0.15, 0) AS commission,
                       COALESCE(SUM(p.amount) * 0.85, 0) AS netRevenue
                FROM payments p JOIN bookings b ON p.booking_id = b.id
                WHERE b.vendor_id = ? AND p.payment_status = 'SUCCESS'
                """, vendorId);

        List<Map<String, Object>> pendingPayouts = jdbcTemplate.queryForList("""
                SELECT * FROM vendor_payouts WHERE vendor_id = ? ORDER BY created_at DESC LIMIT 10
                """, vendorId);

        List<Map<String, Object>> topCars = jdbcTemplate.queryForList("""
                SELECT c.brand, c.model, c.registration_no,
                       COUNT(b.id) AS bookings,
                       COALESCE(SUM(b.total_amount), 0) AS revenue
                FROM bookings b JOIN cars c ON b.car_id = c.id
                WHERE b.vendor_id = ? AND b.status = 'COMPLETED'
                GROUP BY c.id ORDER BY revenue DESC LIMIT 5
                """, vendorId);

        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "monthlyRevenue", monthly,
                "commission", commission,
                "pendingPayouts", pendingPayouts,
                "topCars", topCars
        )));
    }

    // ── Fleet Management ──────────────────────────────────────────────────────

    @GetMapping("/fleet")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMyFleet(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = (Long) auth.getCredentials();

        // ✅ FIX: Return 404 instead of 500 if vendor profile missing
        Long vendorId = getVendorIdOrNull(userId);
        if (vendorId == null) return ResponseEntity.notFound().build();

        List<Map<String, Object>> fleet = jdbcTemplate.queryForList("""
                SELECT c.*, cc.name AS categoryName,
                       (SELECT COUNT(*) FROM bookings b WHERE b.car_id = c.id AND b.status = 'COMPLETED') AS totalTrips,
                       (SELECT AVG(r.rating) FROM reviews r WHERE r.car_id = c.id) AS avgRating,
                       (SELECT COUNT(*) FROM damage_reports dr WHERE dr.car_id = c.id AND dr.status != 'CLOSED') AS openDamages
                FROM cars c
                LEFT JOIN car_categories cc ON c.category_id = cc.id
                WHERE c.vendor_id = ?
                ORDER BY c.created_at DESC LIMIT ? OFFSET ?
                """, vendorId, size, page * size);
        return ResponseEntity.ok(ApiResponse.success(fleet));
    }

    // ── Bookings ──────────────────────────────────────────────────────────────

    @GetMapping("/bookings")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMyBookings(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String status) {
        Long userId = (Long) auth.getCredentials();

        // ✅ FIX: Return 404 instead of 500 if vendor profile missing
        Long vendorId = getVendorIdOrNull(userId);
        if (vendorId == null) return ResponseEntity.notFound().build();

        StringBuilder sql = new StringBuilder("""
                SELECT b.*, u.full_name AS customerName, u.phone AS customerPhone,
                       c.brand AS carBrand, c.model AS carModel, c.registration_no,
                       d.license_number AS driverLicense,
                       du.full_name AS driverName
                FROM bookings b
                JOIN users u ON b.customer_id = u.id
                JOIN cars c ON b.car_id = c.id
                LEFT JOIN drivers d ON b.driver_id = d.id
                LEFT JOIN users du ON d.user_id = du.id
                WHERE b.vendor_id = ?
                """);
        List<Object> params = new java.util.ArrayList<>();
        params.add(vendorId);
        if (!status.isEmpty()) { sql.append(" AND b.status = ?"); params.add(status.toUpperCase()); }
        sql.append(" ORDER BY b.created_at DESC LIMIT ? OFFSET ?");
        params.add(size); params.add(page * size);

        List<Map<String, Object>> bookings = jdbcTemplate.queryForList(sql.toString(), params.toArray());
        return ResponseEntity.ok(ApiResponse.success(bookings));
    }

    @PatchMapping("/bookings/{id}/approve")
    public ResponseEntity<ApiResponse<Void>> approveBooking(
            @PathVariable Long id,
            @RequestParam boolean approved,
            Authentication auth) {
        Long userId = (Long) auth.getCredentials();

        // ✅ FIX: Return 404 instead of 500 if vendor profile missing
        Long vendorId = getVendorIdOrNull(userId);
        if (vendorId == null) return ResponseEntity.notFound().build();

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM bookings WHERE id = ? AND vendor_id = ?",
                Integer.class, id, vendorId);
        if (count == null || count == 0)
            return ResponseEntity.status(403).body(ApiResponse.error("Not authorized"));

        String newStatus = approved ? "CONFIRMED" : "CANCELLED";
        jdbcTemplate.update("UPDATE bookings SET status = ?, updated_at = NOW() WHERE id = ?", newStatus, id);
        return ResponseEntity.ok(ApiResponse.success("Booking " + (approved ? "approved" : "rejected"), null));
    }

    @PatchMapping("/bookings/{id}/assign-driver")
    public ResponseEntity<ApiResponse<Void>> assignDriver(
            @PathVariable Long id,
            @RequestBody Map<String, Long> body,
            Authentication auth) {
        Long userId = (Long) auth.getCredentials();

        // ✅ FIX: Return 404 instead of 500 if vendor profile missing
        Long vendorId = getVendorIdOrNull(userId);
        if (vendorId == null) return ResponseEntity.notFound().build();

        Long driverId = body.get("driverId");

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM bookings WHERE id = ? AND vendor_id = ?",
                Integer.class, id, vendorId);
        if (count == null || count == 0)
            return ResponseEntity.status(403).body(ApiResponse.error("Not authorized"));

        jdbcTemplate.update("UPDATE bookings SET driver_id = ?, updated_at = NOW() WHERE id = ?", driverId, id);
        jdbcTemplate.update("INSERT IGNORE INTO driver_trips (driver_id, booking_id) VALUES (?, ?)", driverId, id);
        return ResponseEntity.ok(ApiResponse.success("Driver assigned", null));
    }

    // ── Damage Reports ────────────────────────────────────────────────────────

    @GetMapping("/damage-reports")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getDamageReports(Authentication auth) {
        Long userId = (Long) auth.getCredentials();

        // ✅ FIX: Return 404 instead of 500 if vendor profile missing
        Long vendorId = getVendorIdOrNull(userId);
        if (vendorId == null) return ResponseEntity.notFound().build();

        List<Map<String, Object>> reports = jdbcTemplate.queryForList("""
                SELECT dr.*, c.brand, c.model, c.registration_no,
                       u.full_name AS reportedByName
                FROM damage_reports dr
                JOIN cars c ON dr.car_id = c.id
                JOIN users u ON dr.reported_by = u.id
                WHERE c.vendor_id = ?
                ORDER BY dr.created_at DESC
                """, vendorId);
        return ResponseEntity.ok(ApiResponse.success(reports));
    }

    @PostMapping("/damage-reports")
    public ResponseEntity<ApiResponse<Void>> reportDamage(
            @RequestBody Map<String, Object> body,
            Authentication auth) {
        Long userId = (Long) auth.getCredentials();
        jdbcTemplate.update("""
                INSERT INTO damage_reports (car_id, booking_id, reported_by, description, severity, estimated_cost)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                body.get("carId"), body.get("bookingId"), userId,
                body.get("description"), body.get("severity"), body.get("estimatedCost"));
        return ResponseEntity.ok(ApiResponse.success("Damage report submitted", null));
    }

    // ── Drivers ───────────────────────────────────────────────────────────────

    @GetMapping("/drivers")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMyDrivers(Authentication auth) {
        Long userId = (Long) auth.getCredentials();

        // ✅ FIX: Return 404 instead of 500 if vendor profile missing
        Long vendorId = getVendorIdOrNull(userId);
        if (vendorId == null) return ResponseEntity.notFound().build();

        List<Map<String, Object>> drivers = jdbcTemplate.queryForList("""
                SELECT d.*, u.full_name, u.email, u.phone,
                       (SELECT COUNT(*) FROM driver_trips dt WHERE dt.driver_id = d.id AND dt.status = 'COMPLETED') AS totalTrips
                FROM drivers d JOIN users u ON d.user_id = u.id
                WHERE d.vendor_id = ?
                ORDER BY d.created_at DESC
                """, vendorId);
        return ResponseEntity.ok(ApiResponse.success(drivers));
    }

    // ── Payouts ───────────────────────────────────────────────────────────────

    @GetMapping("/payouts")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMyPayouts(Authentication auth) {
        Long userId = (Long) auth.getCredentials();

        // ✅ FIX: Return 404 instead of 500 if vendor profile missing
        Long vendorId = getVendorIdOrNull(userId);
        if (vendorId == null) return ResponseEntity.notFound().build();

        List<Map<String, Object>> payouts = jdbcTemplate.queryForList("""
                SELECT * FROM vendor_payouts WHERE vendor_id = ? ORDER BY created_at DESC
                """, vendorId);
        return ResponseEntity.ok(ApiResponse.success(payouts));
    }

    // ── Disputes ──────────────────────────────────────────────────────────────

    @GetMapping("/disputes")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMyDisputes(Authentication auth) {
        Long userId = (Long) auth.getCredentials();

        // ✅ FIX: Return 404 instead of 500 if vendor profile missing
        Long vendorId = getVendorIdOrNull(userId);
        if (vendorId == null) return ResponseEntity.notFound().build();

        List<Map<String, Object>> disputes = jdbcTemplate.queryForList("""
                SELECT d.*, b.booking_number, u.full_name AS raisedByName
                FROM disputes d
                JOIN bookings b ON d.booking_id = b.id
                JOIN users u ON d.raised_by = u.id
                WHERE b.vendor_id = ?
                ORDER BY d.created_at DESC
                """, vendorId);
        return ResponseEntity.ok(ApiResponse.success(disputes));
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    // ✅ FIX: Returns null instead of throwing RuntimeException (which causes 500).
    // All callers check for null and return 404 themselves.
    private Long getVendorIdOrNull(Long userId) {
        List<Long> result = jdbcTemplate.queryForList(
                "SELECT id FROM vendors WHERE user_id = ?", Long.class, userId);
        return result.isEmpty() ? null : result.get(0);
    }
}