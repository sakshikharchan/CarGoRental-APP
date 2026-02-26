//package com.example.demo.Controller;
//
//import com.example.demo.DTOs.ApiResponse;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.ResponseEntity;
//import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.security.core.Authentication;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//import java.util.Map;
//
//@RestController
//@RequestMapping("/api/driver")
//@RequiredArgsConstructor
//@PreAuthorize("hasAuthority('ROLE_DRIVER')")
//public class DriverPortalController {
//
//    private final JdbcTemplate jdbcTemplate;
//
//    // ── Profile ───────────────────────────────────────────────────────────────
//
//    @GetMapping("/profile")
//    public ResponseEntity<ApiResponse<Map<String, Object>>> getProfile(Authentication auth) {
//        Long userId = (Long) auth.getCredentials();
//        List<Map<String, Object>> result = jdbcTemplate.queryForList("""
//            SELECT d.*, u.full_name, u.email, u.phone, u.address,
//                   v.company_name AS vendorName,
//                   (SELECT COUNT(*) FROM driver_trips WHERE driver_id = d.id AND status = 'COMPLETED') AS totalTrips,
//                   (SELECT COUNT(*) FROM driver_trips WHERE driver_id = d.id AND status = 'ASSIGNED') AS pendingTrips,
//                   (SELECT AVG(r.rating) FROM reviews r
//                    JOIN bookings b ON r.booking_id = b.id
//                    WHERE b.driver_id = d.id) AS customerRating
//            FROM drivers d
//            JOIN users u ON d.user_id = u.id
//            LEFT JOIN vendors v ON d.vendor_id = v.id
//            WHERE d.user_id = ?
//        """, userId);
//        if (result.isEmpty()) return ResponseEntity.notFound().build();
//        return ResponseEntity.ok(ApiResponse.success(result.get(0)));
//    }
//
//    @PatchMapping("/availability")
//    public ResponseEntity<ApiResponse<Void>> toggleAvailability(
//            @RequestParam boolean available,
//            Authentication auth) {
//        Long userId = (Long) auth.getCredentials();
//        jdbcTemplate.update(
//            "UPDATE drivers SET is_available = ? WHERE user_id = ?", available, userId);
//        return ResponseEntity.ok(ApiResponse.success(
//            available ? "You are now available" : "You are now offline", null));
//    }
//
//    // ── Trips ─────────────────────────────────────────────────────────────────
//
//    @GetMapping("/trips")
//    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMyTrips(
//            Authentication auth,
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "10") int size,
//            @RequestParam(defaultValue = "") String status) {
//        Long userId = (Long) auth.getCredentials();
//        Long driverId = getDriverId(userId);
//
//        StringBuilder sql = new StringBuilder("""
//            SELECT dt.*, b.booking_number, b.pickup_location, b.dropoff_location,
//                   b.pickup_date, b.return_date, b.total_amount,
//                   c.brand AS carBrand, c.model AS carModel, c.registration_no,
//                   u.full_name AS customerName, u.phone AS customerPhone
//            FROM driver_trips dt
//            JOIN bookings b ON dt.booking_id = b.id
//            JOIN cars c ON b.car_id = c.id
//            JOIN users u ON b.customer_id = u.id
//            WHERE dt.driver_id = ?
//        """);
//        List<Object> params = new java.util.ArrayList<>();
//        params.add(driverId);
//        if (!status.isEmpty()) { sql.append(" AND dt.status = ?"); params.add(status.toUpperCase()); }
//        sql.append(" ORDER BY dt.created_at DESC LIMIT ? OFFSET ?");
//        params.add(size); params.add(page * size);
//
//        List<Map<String, Object>> trips = jdbcTemplate.queryForList(
//            sql.toString(), params.toArray());
//        return ResponseEntity.ok(ApiResponse.success(trips));
//    }
//
//    @GetMapping("/trips/current")
//    public ResponseEntity<ApiResponse<Map<String, Object>>> getCurrentTrip(Authentication auth) {
//        Long userId = (Long) auth.getCredentials();
//        Long driverId = getDriverId(userId);
//
//        List<Map<String, Object>> result = jdbcTemplate.queryForList("""
//            SELECT dt.*, b.booking_number, b.pickup_location, b.dropoff_location,
//                   b.pickup_date, b.return_date, b.total_amount, b.notes,
//                   c.brand AS carBrand, c.model AS carModel, c.registration_no, c.color,
//                   u.full_name AS customerName, u.phone AS customerPhone, u.address AS customerAddress
//            FROM driver_trips dt
//            JOIN bookings b ON dt.booking_id = b.id
//            JOIN cars c ON b.car_id = c.id
//            JOIN users u ON b.customer_id = u.id
//            WHERE dt.driver_id = ? AND dt.status IN ('ASSIGNED','STARTED')
//            ORDER BY dt.created_at DESC LIMIT 1
//        """, driverId);
//
//        if (result.isEmpty()) return ResponseEntity.ok(ApiResponse.success(null));
//        return ResponseEntity.ok(ApiResponse.success(result.get(0)));
//    }
//
//    @PatchMapping("/trips/{id}/start")
//    public ResponseEntity<ApiResponse<Void>> startTrip(
//            @PathVariable Long id,
//            @RequestBody Map<String, Object> body,
//            Authentication auth) {
//        Long userId = (Long) auth.getCredentials();
//        Long driverId = getDriverId(userId);
//
//        Integer count = jdbcTemplate.queryForObject(
//            "SELECT COUNT(*) FROM driver_trips WHERE id = ? AND driver_id = ?",
//            Integer.class, id, driverId);
//        if (count == null || count == 0)
//            return ResponseEntity.status(403).body(ApiResponse.error("Not authorized"));
//
//        jdbcTemplate.update("""
//            UPDATE driver_trips SET status = 'STARTED', start_time = NOW(),
//            start_odometer = ? WHERE id = ?
//        """, body.get("startOdometer"), id);
//
//        // Update booking to ACTIVE
//        jdbcTemplate.update("""
//            UPDATE bookings SET status = 'ACTIVE', updated_at = NOW()
//            WHERE id = (SELECT booking_id FROM driver_trips WHERE id = ?)
//        """, id);
//
//        return ResponseEntity.ok(ApiResponse.success("Trip started", null));
//    }
//
//    @PatchMapping("/trips/{id}/complete")
//    public ResponseEntity<ApiResponse<Void>> completeTrip(
//            @PathVariable Long id,
//            @RequestBody Map<String, Object> body,
//            Authentication auth) {
//        Long userId = (Long) auth.getCredentials();
//        Long driverId = getDriverId(userId);
//
//        Integer count = jdbcTemplate.queryForObject(
//            "SELECT COUNT(*) FROM driver_trips WHERE id = ? AND driver_id = ?",
//            Integer.class, id, driverId);
//        if (count == null || count == 0)
//            return ResponseEntity.status(403).body(ApiResponse.error("Not authorized"));
//
//        jdbcTemplate.update("""
//            UPDATE driver_trips SET status = 'COMPLETED', end_time = NOW(),
//            end_odometer = ?, driver_notes = ? WHERE id = ?
//        """, body.get("endOdometer"), body.get("notes"), id);
//
//        // Update booking to COMPLETED
//        jdbcTemplate.update("""
//            UPDATE bookings SET status = 'COMPLETED', updated_at = NOW()
//            WHERE id = (SELECT booking_id FROM driver_trips WHERE id = ?)
//        """, id);
//
//        // Restore driver availability
//        jdbcTemplate.update(
//            "UPDATE drivers SET is_available = true WHERE id = ?", driverId);
//
//        return ResponseEntity.ok(ApiResponse.success("Trip completed", null));
//    }
//
//    // ── Damage Reporting ──────────────────────────────────────────────────────
//
//    @PostMapping("/damage-report")
//    public ResponseEntity<ApiResponse<Void>> reportDamage(
//            @RequestBody Map<String, Object> body,
//            Authentication auth) {
//        Long userId = (Long) auth.getCredentials();
//
//        jdbcTemplate.update("""
//            INSERT INTO damage_reports (car_id, booking_id, reported_by, description, severity, estimated_cost)
//            VALUES (?, ?, ?, ?, ?, ?)
//        """,
//            body.get("carId"), body.get("bookingId"), userId,
//            body.get("description"), body.get("severity"), body.get("estimatedCost"));
//
//        return ResponseEntity.ok(ApiResponse.success("Damage reported successfully", null));
//    }
//
//    @GetMapping("/damage-reports")
//    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMyDamageReports(Authentication auth) {
//        Long userId = (Long) auth.getCredentials();
//        List<Map<String, Object>> reports = jdbcTemplate.queryForList("""
//            SELECT dr.*, c.brand, c.model, c.registration_no, b.booking_number
//            FROM damage_reports dr
//            JOIN cars c ON dr.car_id = c.id
//            LEFT JOIN bookings b ON dr.booking_id = b.id
//            WHERE dr.reported_by = ?
//            ORDER BY dr.created_at DESC
//        """, userId);
//        return ResponseEntity.ok(ApiResponse.success(reports));
//    }
//
//    // ── Performance Stats ─────────────────────────────────────────────────────
//
//    @GetMapping("/stats")
//    public ResponseEntity<ApiResponse<Map<String, Object>>> getMyStats(Authentication auth) {
//        Long userId = (Long) auth.getCredentials();
//        Long driverId = getDriverId(userId);
//
//        Map<String, Object> stats = jdbcTemplate.queryForMap("""
//            SELECT
//                (SELECT COUNT(*) FROM driver_trips WHERE driver_id = ?) AS totalTrips,
//                (SELECT COUNT(*) FROM driver_trips WHERE driver_id = ? AND status = 'COMPLETED') AS completedTrips,
//                (SELECT COUNT(*) FROM driver_trips WHERE driver_id = ? AND status = 'CANCELLED') AS cancelledTrips,
//                (SELECT AVG(r.rating) FROM reviews r JOIN bookings b ON r.booking_id = b.id
//                 WHERE b.driver_id = ?) AS avgRating,
//                (SELECT COUNT(*) FROM damage_reports WHERE reported_by = ?) AS damageReports
//        """, driverId, driverId, driverId, driverId, userId);
//
//        // Monthly trips chart
//        List<Map<String, Object>> monthlyTrips = jdbcTemplate.queryForList("""
//            SELECT MONTH(end_time) AS month, COUNT(*) AS trips
//            FROM driver_trips
//            WHERE driver_id = ? AND status = 'COMPLETED'
//              AND YEAR(end_time) = YEAR(CURDATE())
//            GROUP BY MONTH(end_time)
//            ORDER BY month
//        """, driverId);
//
//        return ResponseEntity.ok(ApiResponse.success(Map.of(
//            "stats", stats,
//            "monthlyTrips", monthlyTrips
//        )));
//    }
//
//    // ── Helper ────────────────────────────────────────────────────────────────
//
//    private Long getDriverId(Long userId) {
//        Long driverId = jdbcTemplate.queryForObject(
//            "SELECT id FROM drivers WHERE user_id = ?", Long.class, userId);
//        if (driverId == null) throw new RuntimeException("Driver profile not found");
//        return driverId;
//    }
//}

package com.example.demo.Controller;

import com.example.demo.DTOs.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/driver")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_DRIVER')")
public class DriverPortalController {

    private final JdbcTemplate jdbcTemplate;

    // ── Profile ───────────────────────────────────────────────────────────────

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProfile(Authentication auth) {
        Long userId = (Long) auth.getCredentials();
        List<Map<String, Object>> result = jdbcTemplate.queryForList("""
                SELECT d.*, u.full_name, u.email, u.phone, u.address,
                       v.company_name AS vendorName,
                       (SELECT COUNT(*) FROM driver_trips WHERE driver_id = d.id AND status = 'COMPLETED') AS totalTrips,
                       (SELECT COUNT(*) FROM driver_trips WHERE driver_id = d.id AND status = 'ASSIGNED') AS pendingTrips,
                       (SELECT AVG(r.rating) FROM reviews r
                        JOIN bookings b ON r.booking_id = b.id
                        WHERE b.driver_id = d.id) AS customerRating
                FROM drivers d
                JOIN users u ON d.user_id = u.id
                LEFT JOIN vendors v ON d.vendor_id = v.id
                WHERE d.user_id = ?
                """, userId);
        if (result.isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(ApiResponse.success(result.get(0)));
    }

    @PatchMapping("/availability")
    public ResponseEntity<ApiResponse<Void>> toggleAvailability(
            @RequestParam boolean available,
            Authentication auth) {
        Long userId = (Long) auth.getCredentials();

        // ✅ FIX: Return 404 instead of 500 if driver profile missing
        Long driverId = getDriverIdOrNull(userId);
        if (driverId == null)
            return ResponseEntity.notFound().build();

        jdbcTemplate.update(
                "UPDATE drivers SET is_available = ? WHERE user_id = ?", available, userId);
        return ResponseEntity.ok(ApiResponse.success(
                available ? "You are now available" : "You are now offline", null));
    }

    // ── Trips ─────────────────────────────────────────────────────────────────

    @GetMapping("/trips")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMyTrips(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String status) {
        Long userId = (Long) auth.getCredentials();

        // ✅ FIX: Return 404 instead of 500 if driver profile missing
        Long driverId = getDriverIdOrNull(userId);
        if (driverId == null)
            return ResponseEntity.notFound().build();

        StringBuilder sql = new StringBuilder("""
                SELECT dt.*, b.booking_number, b.pickup_location, b.dropoff_location,
                       b.pickup_date, b.return_date, b.total_amount,
                       c.brand AS carBrand, c.model AS carModel, c.registration_no,
                       u.full_name AS customerName, u.phone AS customerPhone
                FROM driver_trips dt
                JOIN bookings b ON dt.booking_id = b.id
                JOIN cars c ON b.car_id = c.id
                JOIN users u ON b.customer_id = u.id
                WHERE dt.driver_id = ?
                """);
        List<Object> params = new java.util.ArrayList<>();
        params.add(driverId);
        if (!status.isEmpty()) { sql.append(" AND dt.status = ?"); params.add(status.toUpperCase()); }
        sql.append(" ORDER BY dt.created_at DESC LIMIT ? OFFSET ?");
        params.add(size); params.add(page * size);

        List<Map<String, Object>> trips = jdbcTemplate.queryForList(sql.toString(), params.toArray());
        return ResponseEntity.ok(ApiResponse.success(trips));
    }

    @GetMapping("/trips/current")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCurrentTrip(Authentication auth) {
        Long userId = (Long) auth.getCredentials();

        // ✅ FIX: Return 404 instead of 500 if driver profile missing
        Long driverId = getDriverIdOrNull(userId);
        if (driverId == null)
            return ResponseEntity.notFound().build();

        List<Map<String, Object>> result = jdbcTemplate.queryForList("""
                SELECT dt.*, b.booking_number, b.pickup_location, b.dropoff_location,
                       b.pickup_date, b.return_date, b.total_amount, b.notes,
                       c.brand AS carBrand, c.model AS carModel, c.registration_no, c.color,
                       u.full_name AS customerName, u.phone AS customerPhone, u.address AS customerAddress
                FROM driver_trips dt
                JOIN bookings b ON dt.booking_id = b.id
                JOIN cars c ON b.car_id = c.id
                JOIN users u ON b.customer_id = u.id
                WHERE dt.driver_id = ? AND dt.status IN ('ASSIGNED','STARTED')
                ORDER BY dt.created_at DESC LIMIT 1
                """, driverId);

        if (result.isEmpty()) return ResponseEntity.ok(ApiResponse.success(null));
        return ResponseEntity.ok(ApiResponse.success(result.get(0)));
    }

    @PatchMapping("/trips/{id}/start")
    public ResponseEntity<ApiResponse<Void>> startTrip(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            Authentication auth) {
        Long userId = (Long) auth.getCredentials();

        // ✅ FIX: Return 404 instead of 500 if driver profile missing
        Long driverId = getDriverIdOrNull(userId);
        if (driverId == null)
            return ResponseEntity.notFound().build();

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM driver_trips WHERE id = ? AND driver_id = ?",
                Integer.class, id, driverId);
        if (count == null || count == 0)
            return ResponseEntity.status(403).body(ApiResponse.error("Not authorized"));

        jdbcTemplate.update("""
                UPDATE driver_trips SET status = 'STARTED', start_time = NOW(),
                start_odometer = ? WHERE id = ?
                """, body.get("startOdometer"), id);

        jdbcTemplate.update("""
                UPDATE bookings SET status = 'ACTIVE', updated_at = NOW()
                WHERE id = (SELECT booking_id FROM driver_trips WHERE id = ?)
                """, id);

        return ResponseEntity.ok(ApiResponse.success("Trip started", null));
    }

    @PatchMapping("/trips/{id}/complete")
    public ResponseEntity<ApiResponse<Void>> completeTrip(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            Authentication auth) {
        Long userId = (Long) auth.getCredentials();

        // ✅ FIX: Return 404 instead of 500 if driver profile missing
        Long driverId = getDriverIdOrNull(userId);
        if (driverId == null)
            return ResponseEntity.notFound().build();

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM driver_trips WHERE id = ? AND driver_id = ?",
                Integer.class, id, driverId);
        if (count == null || count == 0)
            return ResponseEntity.status(403).body(ApiResponse.error("Not authorized"));

        jdbcTemplate.update("""
                UPDATE driver_trips SET status = 'COMPLETED', end_time = NOW(),
                end_odometer = ?, driver_notes = ? WHERE id = ?
                """, body.get("endOdometer"), body.get("notes"), id);

        jdbcTemplate.update("""
                UPDATE bookings SET status = 'COMPLETED', updated_at = NOW()
                WHERE id = (SELECT booking_id FROM driver_trips WHERE id = ?)
                """, id);

        jdbcTemplate.update(
                "UPDATE drivers SET is_available = true WHERE id = ?", driverId);

        return ResponseEntity.ok(ApiResponse.success("Trip completed", null));
    }

    // ── Damage Reporting ──────────────────────────────────────────────────────

    @PostMapping("/damage-report")
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

        return ResponseEntity.ok(ApiResponse.success("Damage reported successfully", null));
    }

    @GetMapping("/damage-reports")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMyDamageReports(Authentication auth) {
        Long userId = (Long) auth.getCredentials();
        List<Map<String, Object>> reports = jdbcTemplate.queryForList("""
                SELECT dr.*, c.brand, c.model, c.registration_no, b.booking_number
                FROM damage_reports dr
                JOIN cars c ON dr.car_id = c.id
                LEFT JOIN bookings b ON dr.booking_id = b.id
                WHERE dr.reported_by = ?
                ORDER BY dr.created_at DESC
                """, userId);
        return ResponseEntity.ok(ApiResponse.success(reports));
    }

    // ── Performance Stats ─────────────────────────────────────────────────────

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMyStats(Authentication auth) {
        Long userId = (Long) auth.getCredentials();

        // ✅ FIX: Return 404 instead of 500 if driver profile missing
        Long driverId = getDriverIdOrNull(userId);
        if (driverId == null)
            return ResponseEntity.notFound().build();

        Map<String, Object> stats = jdbcTemplate.queryForMap("""
                SELECT
                    (SELECT COUNT(*) FROM driver_trips WHERE driver_id = ?) AS totalTrips,
                    (SELECT COUNT(*) FROM driver_trips WHERE driver_id = ? AND status = 'COMPLETED') AS completedTrips,
                    (SELECT COUNT(*) FROM driver_trips WHERE driver_id = ? AND status = 'CANCELLED') AS cancelledTrips,
                    (SELECT AVG(r.rating) FROM reviews r JOIN bookings b ON r.booking_id = b.id
                     WHERE b.driver_id = ?) AS avgRating,
                    (SELECT COUNT(*) FROM damage_reports WHERE reported_by = ?) AS damageReports
                """, driverId, driverId, driverId, driverId, userId);

        List<Map<String, Object>> monthlyTrips = jdbcTemplate.queryForList("""
                SELECT MONTH(end_time) AS month, COUNT(*) AS trips
                FROM driver_trips
                WHERE driver_id = ? AND status = 'COMPLETED'
                  AND YEAR(end_time) = YEAR(CURDATE())
                GROUP BY MONTH(end_time)
                ORDER BY month
                """, driverId);

        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "stats", stats,
                "monthlyTrips", monthlyTrips
        )));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    // ✅ FIX: Returns null instead of throwing RuntimeException (which causes 500)
    // All callers check for null and return 404 themselves.
    private Long getDriverIdOrNull(Long userId) {
        List<Long> result = jdbcTemplate.queryForList(
                "SELECT id FROM drivers WHERE user_id = ?", Long.class, userId);
        return result.isEmpty() ? null : result.get(0);
    }
}