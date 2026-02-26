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
@RequestMapping("/api/customer")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_CUSTOMER')")
public class CustomerController {

    private final JdbcTemplate jdbcTemplate;

    // ── Profile ───────────────────────────────────────────────────────────────

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProfile(Authentication auth) {
        Long userId = (Long) auth.getCredentials();
        List<Map<String, Object>> result = jdbcTemplate.queryForList("""
            SELECT u.id, u.full_name, u.email, u.phone, u.address, u.profile_image,
                   u.is_active, u.created_at,
                   r.name AS role,
                   (SELECT COUNT(*) FROM bookings WHERE customer_id = u.id) AS totalBookings,
                   (SELECT COUNT(*) FROM bookings WHERE customer_id = u.id AND status = 'COMPLETED') AS completedBookings,
                   (SELECT COUNT(*) FROM bookings WHERE customer_id = u.id AND status = 'CANCELLED') AS cancelledBookings,
                   (SELECT COUNT(*) FROM reviews WHERE customer_id = u.id) AS totalReviews,
                   lv.status AS licenseStatus, lv.license_number, lv.expiry_date AS licenseExpiry
            FROM users u
            JOIN roles r ON u.role_id = r.id
            LEFT JOIN license_verifications lv ON lv.user_id = u.id
            WHERE u.id = ?
        """, userId);
        if (result.isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(ApiResponse.success(result.get(0)));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<Void>> updateProfile(
            @RequestBody Map<String, String> body,
            Authentication auth) {
        Long userId = (Long) auth.getCredentials();
        jdbcTemplate.update("""
            UPDATE users SET full_name = ?, phone = ?, address = ?, updated_at = NOW()
            WHERE id = ?
        """, body.get("fullName"), body.get("phone"), body.get("address"), userId);
        return ResponseEntity.ok(ApiResponse.success("Profile updated", null));
    }

    // ── License Verification ──────────────────────────────────────────────────

    @PostMapping("/license/submit")
    public ResponseEntity<ApiResponse<Void>> submitLicense(
            @RequestBody Map<String, Object> body,
            Authentication auth) {
        Long userId = (Long) auth.getCredentials();

        // Check if already submitted
        Integer existing = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM license_verifications WHERE user_id = ?",
            Integer.class, userId);

        if (existing != null && existing > 0) {
            // Update existing
            jdbcTemplate.update("""
                UPDATE license_verifications
                SET license_number = ?, license_type = ?, issuing_state = ?,
                    issue_date = ?, expiry_date = ?, document_url = ?, status = 'PENDING'
                WHERE user_id = ?
            """,
                body.get("licenseNumber"), body.get("licenseType"), body.get("issuingState"),
                body.get("issueDate"), body.get("expiryDate"), body.get("documentUrl"), userId);
        } else {
            jdbcTemplate.update("""
                INSERT INTO license_verifications
                    (user_id, license_number, license_type, issuing_state, issue_date, expiry_date, document_url)
                VALUES (?, ?, ?, ?, ?, ?, ?)
            """,
                userId, body.get("licenseNumber"), body.get("licenseType"), body.get("issuingState"),
                body.get("issueDate"), body.get("expiryDate"), body.get("documentUrl"));
        }
        return ResponseEntity.ok(ApiResponse.success("License submitted for verification", null));
    }

    @GetMapping("/license/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getLicenseStatus(Authentication auth) {
        Long userId = (Long) auth.getCredentials();
        List<Map<String, Object>> result = jdbcTemplate.queryForList(
            "SELECT * FROM license_verifications WHERE user_id = ?", userId);
        if (result.isEmpty())
            return ResponseEntity.ok(ApiResponse.success(Map.of("status", "NOT_SUBMITTED")));
        return ResponseEntity.ok(ApiResponse.success(result.get(0)));
    }

    // ── Booking History ───────────────────────────────────────────────────────

    @GetMapping("/bookings")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMyBookings(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String status) {
        Long userId = (Long) auth.getCredentials();

        StringBuilder sql = new StringBuilder("""
            SELECT b.*, c.brand AS carBrand, c.model AS carModel, c.image_url,
                   c.registration_no, cc.name AS categoryName,
                   v.company_name AS vendorName,
                   d.license_number, du.full_name AS driverName,
                   p.payment_status, p.payment_method, p.transaction_id
            FROM bookings b
            JOIN cars c ON b.car_id = c.id
            LEFT JOIN car_categories cc ON c.category_id = cc.id
            LEFT JOIN vendors v ON b.vendor_id = v.id
            LEFT JOIN drivers d ON b.driver_id = d.id
            LEFT JOIN users du ON d.user_id = du.id
            LEFT JOIN payments p ON p.booking_id = b.id
            WHERE b.customer_id = ?
        """);

        List<Map<String, Object>> bookings;
        if (!status.isEmpty()) {
            bookings = jdbcTemplate.queryForList(
                sql + " AND b.status = ? ORDER BY b.created_at DESC LIMIT ? OFFSET ?",
                userId, status.toUpperCase(), size, page * size);
        } else {
            bookings = jdbcTemplate.queryForList(
                sql + " ORDER BY b.created_at DESC LIMIT ? OFFSET ?",
                userId, size, page * size);
        }
        return ResponseEntity.ok(ApiResponse.success(bookings));
    }

    @GetMapping("/bookings/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBookingDetail(
            @PathVariable Long id,
            Authentication auth) {
        Long userId = (Long) auth.getCredentials();
        List<Map<String, Object>> result = jdbcTemplate.queryForList("""
            SELECT b.*, c.brand AS carBrand, c.model AS carModel, c.color, c.fuel_type,
                   c.transmission, c.seats, c.image_url, c.registration_no,
                   cc.name AS categoryName,
                   v.company_name AS vendorName,
                   du.full_name AS driverName, d.experience_years, d.rating AS driverRating,
                   p.payment_status, p.payment_method, p.transaction_id, p.amount AS paidAmount,
                   r.rating AS myRating, r.comment AS myReview
            FROM bookings b
            JOIN cars c ON b.car_id = c.id
            LEFT JOIN car_categories cc ON c.category_id = cc.id
            LEFT JOIN vendors v ON b.vendor_id = v.id
            LEFT JOIN drivers d ON b.driver_id = d.id
            LEFT JOIN users du ON d.user_id = du.id
            LEFT JOIN payments p ON p.booking_id = b.id
            LEFT JOIN reviews r ON r.booking_id = b.id AND r.customer_id = b.customer_id
            WHERE b.id = ? AND b.customer_id = ?
        """, id, userId);
        if (result.isEmpty()) return ResponseEntity.status(403).body(ApiResponse.error("Not found"));
        return ResponseEntity.ok(ApiResponse.success(result.get(0)));
    }

    // ── Disputes ──────────────────────────────────────────────────────────────

    @PostMapping("/disputes")
    public ResponseEntity<ApiResponse<Void>> raiseDispute(
            @RequestBody Map<String, Object> body,
            Authentication auth) {
        Long userId = (Long) auth.getCredentials();
        Long bookingId = Long.valueOf(body.get("bookingId").toString());

        // Verify booking ownership
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM bookings WHERE id = ? AND customer_id = ?",
            Integer.class, bookingId, userId);
        if (count == null || count == 0)
            return ResponseEntity.status(403).body(ApiResponse.error("Not authorized"));

        jdbcTemplate.update("""
            INSERT INTO disputes (booking_id, raised_by, title, description, type)
            VALUES (?, ?, ?, ?, ?)
        """,
            bookingId, userId, body.get("title"), body.get("description"), body.get("type"));
        return ResponseEntity.ok(ApiResponse.success("Dispute raised successfully", null));
    }

    @GetMapping("/disputes")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMyDisputes(Authentication auth) {
        Long userId = (Long) auth.getCredentials();
        List<Map<String, Object>> disputes = jdbcTemplate.queryForList("""
            SELECT d.*, b.booking_number, c.brand AS carBrand, c.model AS carModel
            FROM disputes d
            JOIN bookings b ON d.booking_id = b.id
            JOIN cars c ON b.car_id = c.id
            WHERE d.raised_by = ?
            ORDER BY d.created_at DESC
        """, userId);
        return ResponseEntity.ok(ApiResponse.success(disputes));
    }

    // ── Notifications ─────────────────────────────────────────────────────────

    @GetMapping("/notifications")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getNotifications(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = (Long) auth.getCredentials();
        List<Map<String, Object>> notifs = jdbcTemplate.queryForList("""
            SELECT * FROM notifications WHERE user_id = ?
            ORDER BY created_at DESC LIMIT ? OFFSET ?
        """, userId, size, page * size);
        return ResponseEntity.ok(ApiResponse.success(notifs));
    }

    // ── Dashboard Summary ─────────────────────────────────────────────────────

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboard(Authentication auth) {
        Long userId = (Long) auth.getCredentials();

        Map<String, Object> stats = jdbcTemplate.queryForMap("""
            SELECT
                (SELECT COUNT(*) FROM bookings WHERE customer_id = ?) AS totalBookings,
                (SELECT COUNT(*) FROM bookings WHERE customer_id = ? AND status = 'PENDING') AS pendingBookings,
                (SELECT COUNT(*) FROM bookings WHERE customer_id = ? AND status = 'ACTIVE') AS activeBookings,
                (SELECT COUNT(*) FROM bookings WHERE customer_id = ? AND status = 'COMPLETED') AS completedBookings,
                (SELECT COUNT(*) FROM bookings WHERE customer_id = ? AND status = 'CANCELLED') AS cancelledBookings,
                (SELECT COUNT(*) FROM disputes d JOIN bookings b ON d.booking_id = b.id
                 WHERE b.customer_id = ? AND d.status = 'OPEN') AS openDisputes,
                (SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = 0) AS unreadNotifications
        """, userId, userId, userId, userId, userId, userId, userId);

        // Active booking details
        List<Map<String, Object>> activeBooking = jdbcTemplate.queryForList("""
            SELECT b.*, c.brand, c.model, c.image_url
            FROM bookings b JOIN cars c ON b.car_id = c.id
            WHERE b.customer_id = ? AND b.status IN ('CONFIRMED','ACTIVE')
            ORDER BY b.pickup_date ASC LIMIT 1
        """, userId);

        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "stats", stats,
            "activeBooking", activeBooking
        )));
    }
}
//
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
//import java.util.Set;
//
//@RestController
//@RequestMapping("/api/customer")
//@RequiredArgsConstructor
//@PreAuthorize("hasAuthority('ROLE_CUSTOMER')")
//public class CustomerController {
//
//    private final JdbcTemplate jdbcTemplate;
//
//    // ✅ FIX: Valid dispute types — reject anything else
//    private static final Set<String> VALID_DISPUTE_TYPES = Set.of(
//            "BILLING", "VEHICLE_CONDITION", "DRIVER_BEHAVIOUR",
//            "CANCELLATION", "REFUND", "OTHER"
//    );
//
//    // ✅ FIX: Cap size to max 100 to prevent huge DB queries
//    private int capSize(int size) {
//        return Math.min(size, 100);
//    }
//
//    // ── Profile ───────────────────────────────────────────────────────────────
//
//    @GetMapping("/profile")
//    public ResponseEntity<ApiResponse<Map<String, Object>>> getProfile(Authentication auth) {
//        Long userId = (Long) auth.getCredentials();
//        List<Map<String, Object>> result = jdbcTemplate.queryForList("""
//                SELECT u.id, u.full_name, u.email, u.phone, u.address, u.profile_image,
//                       u.is_active, u.created_at,
//                       r.name AS role,
//                       (SELECT COUNT(*) FROM bookings WHERE customer_id = u.id) AS totalBookings,
//                       (SELECT COUNT(*) FROM bookings WHERE customer_id = u.id AND status = 'COMPLETED') AS completedBookings,
//                       (SELECT COUNT(*) FROM bookings WHERE customer_id = u.id AND status = 'CANCELLED') AS cancelledBookings,
//                       (SELECT COUNT(*) FROM reviews WHERE customer_id = u.id) AS totalReviews,
//                       lv.status AS licenseStatus, lv.license_number, lv.expiry_date AS licenseExpiry
//                FROM users u
//                JOIN roles r ON u.role_id = r.id
//                LEFT JOIN license_verifications lv ON lv.user_id = u.id
//                WHERE u.id = ?
//                """, userId);
//        if (result.isEmpty()) return ResponseEntity.notFound().build();
//        return ResponseEntity.ok(ApiResponse.success(result.get(0)));
//    }
//
//    @PutMapping("/profile")
//    public ResponseEntity<ApiResponse<Void>> updateProfile(
//            @RequestBody Map<String, String> body,
//            Authentication auth) {
//        Long userId = (Long) auth.getCredentials();
//        jdbcTemplate.update("""
//                UPDATE users SET full_name = ?, phone = ?, address = ?, updated_at = NOW()
//                WHERE id = ?
//                """, body.get("fullName"), body.get("phone"), body.get("address"), userId);
//        return ResponseEntity.ok(ApiResponse.success("Profile updated", null));
//    }
//
//    // ── License Verification ──────────────────────────────────────────────────
//
//    @PostMapping("/license/submit")
//    public ResponseEntity<ApiResponse<Void>> submitLicense(
//            @RequestBody Map<String, Object> body,
//            Authentication auth) {
//        Long userId = (Long) auth.getCredentials();
//
//        // ✅ FIX: Validate required license fields before touching the DB
//        String licenseNumber = body.get("licenseNumber") != null ? body.get("licenseNumber").toString().trim() : null;
//        String licenseType   = body.get("licenseType")   != null ? body.get("licenseType").toString().trim()   : null;
//        String expiryDate    = body.get("expiryDate")    != null ? body.get("expiryDate").toString().trim()    : null;
//
//        if (licenseNumber == null || licenseNumber.isBlank())
//            return ResponseEntity.badRequest().body(ApiResponse.error("License number is required"));
//        if (licenseType == null || licenseType.isBlank())
//            return ResponseEntity.badRequest().body(ApiResponse.error("License type is required"));
//        if (expiryDate == null || expiryDate.isBlank())
//            return ResponseEntity.badRequest().body(ApiResponse.error("Expiry date is required"));
//
//        Integer existing = jdbcTemplate.queryForObject(
//                "SELECT COUNT(*) FROM license_verifications WHERE user_id = ?",
//                Integer.class, userId);
//
//        if (existing != null && existing > 0) {
//            jdbcTemplate.update("""
//                    UPDATE license_verifications
//                    SET license_number = ?, license_type = ?, issuing_state = ?,
//                        issue_date = ?, expiry_date = ?, document_url = ?, status = 'PENDING'
//                    WHERE user_id = ?
//                    """,
//                    licenseNumber, licenseType, body.get("issuingState"),
//                    body.get("issueDate"), expiryDate, body.get("documentUrl"), userId);
//        } else {
//            jdbcTemplate.update("""
//                    INSERT INTO license_verifications
//                        (user_id, license_number, license_type, issuing_state, issue_date, expiry_date, document_url)
//                    VALUES (?, ?, ?, ?, ?, ?, ?)
//                    """,
//                    userId, licenseNumber, licenseType, body.get("issuingState"),
//                    body.get("issueDate"), expiryDate, body.get("documentUrl"));
//        }
//        return ResponseEntity.ok(ApiResponse.success("License submitted for verification", null));
//    }
//
//    @GetMapping("/license/status")
//    public ResponseEntity<ApiResponse<Map<String, Object>>> getLicenseStatus(Authentication auth) {
//        Long userId = (Long) auth.getCredentials();
//        List<Map<String, Object>> result = jdbcTemplate.queryForList(
//                "SELECT * FROM license_verifications WHERE user_id = ?", userId);
//        if (result.isEmpty())
//            return ResponseEntity.ok(ApiResponse.success(Map.of("status", "NOT_SUBMITTED")));
//        return ResponseEntity.ok(ApiResponse.success(result.get(0)));
//    }
//
//    // ── Booking History ───────────────────────────────────────────────────────
//
//    @GetMapping("/bookings")
//    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMyBookings(
//            Authentication auth,
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "10") int size,
//            @RequestParam(defaultValue = "") String status) {
//        Long userId = (Long) auth.getCredentials();
//        size = capSize(size); // ✅ FIX: cap size
//
//        StringBuilder sql = new StringBuilder("""
//                SELECT b.*, c.brand AS carBrand, c.model AS carModel, c.image_url,
//                       c.registration_no, cc.name AS categoryName,
//                       v.company_name AS vendorName,
//                       d.license_number, du.full_name AS driverName,
//                       p.payment_status, p.payment_method, p.transaction_id
//                FROM bookings b
//                JOIN cars c ON b.car_id = c.id
//                LEFT JOIN car_categories cc ON c.category_id = cc.id
//                LEFT JOIN vendors v ON b.vendor_id = v.id
//                LEFT JOIN drivers d ON b.driver_id = d.id
//                LEFT JOIN users du ON d.user_id = du.id
//                LEFT JOIN payments p ON p.booking_id = b.id
//                WHERE b.customer_id = ?
//                """);
//
//        List<Map<String, Object>> bookings;
//        if (!status.isEmpty()) {
//            bookings = jdbcTemplate.queryForList(
//                    sql + " AND b.status = ? ORDER BY b.created_at DESC LIMIT ? OFFSET ?",
//                    userId, status.toUpperCase(), size, page * size);
//        } else {
//            bookings = jdbcTemplate.queryForList(
//                    sql + " ORDER BY b.created_at DESC LIMIT ? OFFSET ?",
//                    userId, size, page * size);
//        }
//        return ResponseEntity.ok(ApiResponse.success(bookings));
//    }
//
//    @GetMapping("/bookings/{id}")
//    public ResponseEntity<ApiResponse<Map<String, Object>>> getBookingDetail(
//            @PathVariable Long id,
//            Authentication auth) {
//        Long userId = (Long) auth.getCredentials();
//        List<Map<String, Object>> result = jdbcTemplate.queryForList("""
//                SELECT b.*, c.brand AS carBrand, c.model AS carModel, c.color, c.fuel_type,
//                       c.transmission, c.seats, c.image_url, c.registration_no,
//                       cc.name AS categoryName,
//                       v.company_name AS vendorName,
//                       du.full_name AS driverName, d.experience_years, d.rating AS driverRating,
//                       p.payment_status, p.payment_method, p.transaction_id, p.amount AS paidAmount,
//                       r.rating AS myRating, r.comment AS myReview
//                FROM bookings b
//                JOIN cars c ON b.car_id = c.id
//                LEFT JOIN car_categories cc ON c.category_id = cc.id
//                LEFT JOIN vendors v ON b.vendor_id = v.id
//                LEFT JOIN drivers d ON b.driver_id = d.id
//                LEFT JOIN users du ON d.user_id = du.id
//                LEFT JOIN payments p ON p.booking_id = b.id
//                LEFT JOIN reviews r ON r.booking_id = b.id AND r.customer_id = b.customer_id
//                WHERE b.id = ? AND b.customer_id = ?
//                """, id, userId);
//        if (result.isEmpty()) return ResponseEntity.status(403).body(ApiResponse.error("Not found"));
//        return ResponseEntity.ok(ApiResponse.success(result.get(0)));
//    }
//
//    // ── Disputes ──────────────────────────────────────────────────────────────
//
//    @PostMapping("/disputes")
//    public ResponseEntity<ApiResponse<Void>> raiseDispute(
//            @RequestBody Map<String, Object> body,
//            Authentication auth) {
//        Long userId = (Long) auth.getCredentials();
//
//        // ✅ FIX: Validate required fields
//        if (body.get("bookingId") == null)
//            return ResponseEntity.badRequest().body(ApiResponse.error("bookingId is required"));
//        if (body.get("title") == null || body.get("title").toString().isBlank())
//            return ResponseEntity.badRequest().body(ApiResponse.error("Dispute title is required"));
//        if (body.get("description") == null || body.get("description").toString().isBlank())
//            return ResponseEntity.badRequest().body(ApiResponse.error("Dispute description is required"));
//
//        // ✅ FIX: Validate dispute type against allowed values
//        String type = body.get("type") != null ? body.get("type").toString().toUpperCase().trim() : "";
//        if (type.isBlank()) {
//            return ResponseEntity.badRequest().body(ApiResponse.error("Dispute type is required"));
//        }
//        if (!VALID_DISPUTE_TYPES.contains(type)) {
//            return ResponseEntity.badRequest().body(ApiResponse.error(
//                    "Invalid dispute type. Allowed values: " + VALID_DISPUTE_TYPES));
//        }
//
//        Long bookingId = Long.valueOf(body.get("bookingId").toString());
//
//        // Verify booking ownership
//        Integer count = jdbcTemplate.queryForObject(
//                "SELECT COUNT(*) FROM bookings WHERE id = ? AND customer_id = ?",
//                Integer.class, bookingId, userId);
//        if (count == null || count == 0)
//            return ResponseEntity.status(403).body(ApiResponse.error("Not authorized"));
//
//        // ✅ FIX: Prevent duplicate open disputes for the same booking
//        Integer openDispute = jdbcTemplate.queryForObject(
//                "SELECT COUNT(*) FROM disputes WHERE booking_id = ? AND raised_by = ? AND status = 'OPEN'",
//                Integer.class, bookingId, userId);
//        if (openDispute != null && openDispute > 0)
//            return ResponseEntity.badRequest().body(
//                    ApiResponse.error("You already have an open dispute for this booking"));
//
//        jdbcTemplate.update("""
//                INSERT INTO disputes (booking_id, raised_by, title, description, type)
//                VALUES (?, ?, ?, ?, ?)
//                """,
//                bookingId, userId, body.get("title").toString().trim(),
//                body.get("description").toString().trim(), type);
//
//        return ResponseEntity.ok(ApiResponse.success("Dispute raised successfully", null));
//    }
//
//    @GetMapping("/disputes")
//    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMyDisputes(Authentication auth) {
//        Long userId = (Long) auth.getCredentials();
//        List<Map<String, Object>> disputes = jdbcTemplate.queryForList("""
//                SELECT d.*, b.booking_number, c.brand AS carBrand, c.model AS carModel
//                FROM disputes d
//                JOIN bookings b ON d.booking_id = b.id
//                JOIN cars c ON b.car_id = c.id
//                WHERE d.raised_by = ?
//                ORDER BY d.created_at DESC
//                """, userId);
//        return ResponseEntity.ok(ApiResponse.success(disputes));
//    }
//
//    // ── Notifications ─────────────────────────────────────────────────────────
//
//    @GetMapping("/notifications")
//    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getNotifications(
//            Authentication auth,
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "20") int size) {
//        Long userId = (Long) auth.getCredentials();
//        size = capSize(size); // ✅ FIX: cap size
//
//        List<Map<String, Object>> notifs = jdbcTemplate.queryForList("""
//                SELECT * FROM notifications WHERE user_id = ?
//                ORDER BY created_at DESC LIMIT ? OFFSET ?
//                """, userId, size, page * size);
//        return ResponseEntity.ok(ApiResponse.success(notifs));
//    }
//
//    // ── Dashboard Summary ─────────────────────────────────────────────────────
//
//    @GetMapping("/dashboard")
//    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboard(Authentication auth) {
//        Long userId = (Long) auth.getCredentials();
//
//        Map<String, Object> stats = jdbcTemplate.queryForMap("""
//                SELECT
//                    (SELECT COUNT(*) FROM bookings WHERE customer_id = ?) AS totalBookings,
//                    (SELECT COUNT(*) FROM bookings WHERE customer_id = ? AND status = 'PENDING') AS pendingBookings,
//                    (SELECT COUNT(*) FROM bookings WHERE customer_id = ? AND status = 'ACTIVE') AS activeBookings,
//                    (SELECT COUNT(*) FROM bookings WHERE customer_id = ? AND status = 'COMPLETED') AS completedBookings,
//                    (SELECT COUNT(*) FROM bookings WHERE customer_id = ? AND status = 'CANCELLED') AS cancelledBookings,
//                    (SELECT COUNT(*) FROM disputes d JOIN bookings b ON d.booking_id = b.id
//                     WHERE b.customer_id = ? AND d.status = 'OPEN') AS openDisputes,
//                    (SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = 0) AS unreadNotifications
//                """, userId, userId, userId, userId, userId, userId, userId);
//
//        List<Map<String, Object>> activeBooking = jdbcTemplate.queryForList("""
//                SELECT b.*, c.brand, c.model, c.image_url
//                FROM bookings b JOIN cars c ON b.car_id = c.id
//                WHERE b.customer_id = ? AND b.status IN ('CONFIRMED','ACTIVE')
//                ORDER BY b.pickup_date ASC LIMIT 1
//                """, userId);
//
//        return ResponseEntity.ok(ApiResponse.success(Map.of(
//                "stats", stats,
//                "activeBooking", activeBooking
//        )));
//    }
//}