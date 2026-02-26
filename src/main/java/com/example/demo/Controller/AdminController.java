//package com.example.demo.Controller;
//
//import com.example.demo.DTOs.ApiResponse;
//import com.example.demo.Repository.UserRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.ResponseEntity;
//import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.web.bind.annotation.*;
//
//import java.math.BigDecimal;
//import java.util.List;
//import java.util.Map;
//
//@RestController
//@RequestMapping("/api/admin")
//@RequiredArgsConstructor
//@PreAuthorize("hasAuthority('ROLE_ADMIN')")
//public class AdminController {
//
//    private final UserRepository userRepository;
//    private final PasswordEncoder passwordEncoder;
//    private final JdbcTemplate jdbcTemplate;
//
//    // ── Dashboard Overview ────────────────────────────────────────────────────
//
//    @GetMapping("/overview")
//    public ResponseEntity<ApiResponse<Map<String, Object>>> getOverview() {
//    	Map<String, Object> stats = jdbcTemplate.queryForMap("""
//    		    SELECT
//    		      (SELECT COUNT(*) FROM users) AS totalUsers,
//    		      (SELECT COUNT(*) FROM vendors) AS totalVendors,
//    		      (SELECT COUNT(*) FROM vendors WHERE is_approved = false) AS pendingVendors,
//    		      (SELECT COUNT(*) FROM drivers) AS totalDrivers,
//    		      (SELECT COUNT(*) FROM cars) AS totalCars,
//    		      (SELECT COUNT(*) FROM cars WHERE is_available = true) AS availableCars,
//    		      (SELECT COUNT(*) FROM bookings) AS totalBookings,
//    		      (SELECT COUNT(*) FROM bookings WHERE status = 'PENDING') AS pendingBookings,
//    		      (SELECT COUNT(*) FROM bookings WHERE status = 'ACTIVE') AS activeBookings,
//    		      (SELECT COUNT(*) FROM bookings WHERE status = 'COMPLETED') AS completedBookings,
//    		      (SELECT COUNT(*) FROM bookings WHERE DATE(created_at) = CURDATE()) AS todayBookings,
//    		      (SELECT COALESCE(SUM(amount),0) FROM payments WHERE payment_status = 'SUCCESS') AS totalRevenue,
//    		      (SELECT COALESCE(SUM(amount),0) FROM payments WHERE payment_status = 'SUCCESS' AND DATE(payment_date) = CURDATE()) AS todayRevenue,
//    		      (SELECT COALESCE(SUM(amount),0) FROM payments WHERE payment_status = 'SUCCESS' AND YEAR(payment_date) = YEAR(CURDATE()) AND MONTH(payment_date) = MONTH(CURDATE())) AS monthRevenue,
//    		      (SELECT COUNT(*) FROM disputes WHERE status = 'OPEN') AS openDisputes,
//    		      (SELECT COUNT(*) FROM license_verifications WHERE status = 'PENDING') AS pendingLicenses,
//    		      (SELECT COUNT(*) FROM damage_reports WHERE status = 'OPEN') AS newDamageReports
//    		""");
//
//        // Monthly revenue chart
//        List<Map<String, Object>> monthlyRevenue = jdbcTemplate.queryForList("""
//            SELECT MONTH(payment_date) AS month, COALESCE(SUM(amount),0) AS revenue, COUNT(*) AS transactions
//            FROM payments
//            WHERE YEAR(payment_date) = YEAR(CURDATE()) AND payment_status = 'SUCCESS'
//            GROUP BY MONTH(payment_date) ORDER BY month
//        """);
//
//        // Recent activity
//        List<Map<String, Object>> recentBookings = jdbcTemplate.queryForList("""
//            SELECT b.booking_number, b.status, b.total_amount, b.created_at,
//                   u.full_name AS customerName, c.brand, c.model
//            FROM bookings b JOIN users u ON b.customer_id = u.id JOIN cars c ON b.car_id = c.id
//            ORDER BY b.created_at DESC LIMIT 5
//        """);
//
//        return ResponseEntity.ok(ApiResponse.success(Map.of(
//            "stats", stats,
//            "monthlyRevenue", monthlyRevenue,
//            "recentBookings", recentBookings
//        )));
//    }
//
//    // ── Users ─────────────────────────────────────────────────────────────────
//
//    @GetMapping("/users")
//    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAllUsers(
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "10") int size,
//            @RequestParam(defaultValue = "") String search,
//            @RequestParam(defaultValue = "") String role) {
//
//        StringBuilder sql = new StringBuilder("""
//            SELECT u.id, u.full_name AS fullName, u.email, u.phone,
//                   u.is_active AS isActive, u.created_at AS createdAt, r.name AS roleName,
//                   (SELECT COUNT(*) FROM bookings WHERE customer_id = u.id) AS totalBookings,
//                   lv.status AS licenseStatus
//            FROM users u
//            JOIN roles r ON u.role_id = r.id
//            LEFT JOIN license_verifications lv ON lv.user_id = u.id
//            WHERE (u.full_name LIKE ? OR u.email LIKE ?)
//        """);
//        String like = "%" + search + "%";
//        List<Object> params = new java.util.ArrayList<>(List.of(like, like));
//
//        if (!role.isEmpty()) { sql.append(" AND r.name = ?"); params.add(role); }
//        sql.append(" ORDER BY u.created_at DESC LIMIT ? OFFSET ?");
//        params.add(size); params.add(page * size);
//
//        List<Map<String, Object>> users = jdbcTemplate.queryForList(sql.toString(), params.toArray());
//        return ResponseEntity.ok(ApiResponse.success(users));
//    }
//
//    @PatchMapping("/users/{id}/toggle-active")
//    public ResponseEntity<ApiResponse<Void>> toggleActive(@PathVariable Long id, Authentication auth) {
//        Long adminId = (Long) auth.getCredentials();
//        Integer current = jdbcTemplate.queryForObject(
//            "SELECT is_active FROM users WHERE id = ?", Integer.class, id);
//        if (current == null) return ResponseEntity.notFound().build();
//        jdbcTemplate.update("UPDATE users SET is_active = ?, updated_at = NOW() WHERE id = ?",
//            current == 0, id);
//        jdbcTemplate.update("INSERT INTO audit_logs (user_id, action, entity_type, entity_id, details) VALUES (?,?,?,?,?)",
//            adminId, "TOGGLE_USER", "USER", id, "Status changed to " + (current == 0 ? "ACTIVE" : "INACTIVE"));
//        return ResponseEntity.ok(ApiResponse.success("User status updated", null));
//    }
//
//    @PatchMapping("/users/{id}/reset-password")
//    public ResponseEntity<ApiResponse<Void>> resetPassword(
//            @PathVariable Long id,
//            @RequestBody Map<String, String> body,
//            Authentication auth) {
//        Long adminId = (Long) auth.getCredentials();
//        String newPassword = body.get("newPassword");
//        if (newPassword == null || newPassword.length() < 6)
//            return ResponseEntity.badRequest().body(ApiResponse.error("Password must be at least 6 characters"));
//        jdbcTemplate.update("UPDATE users SET password = ?, updated_at = NOW() WHERE id = ?",
//            passwordEncoder.encode(newPassword), id);
//        jdbcTemplate.update("INSERT INTO audit_logs (user_id, action, entity_type, entity_id, details) VALUES (?,?,?,?,?)",
//            adminId, "RESET_PASSWORD", "USER", id, "Admin password reset");
//        return ResponseEntity.ok(ApiResponse.success("Password reset successfully", null));
//    }
//
//    @PatchMapping("/users/{id}/role")
//    public ResponseEntity<ApiResponse<Void>> changeRole(
//            @PathVariable Long id,
//            @RequestBody Map<String, String> body,
//            Authentication auth) {
//        Long adminId = (Long) auth.getCredentials();
//        String roleName = body.get("roleName");
//        Long roleId = jdbcTemplate.queryForObject("SELECT id FROM roles WHERE name = ?", Long.class, roleName);
//        if (roleId == null) return ResponseEntity.badRequest().body(ApiResponse.error("Role not found"));
//        jdbcTemplate.update("UPDATE users SET role_id = ?, updated_at = NOW() WHERE id = ?", roleId, id);
//        jdbcTemplate.update("INSERT INTO audit_logs (user_id, action, entity_type, entity_id, details) VALUES (?,?,?,?,?)",
//            adminId, "CHANGE_ROLE", "USER", id, "Role changed to " + roleName);
//        return ResponseEntity.ok(ApiResponse.success("Role updated", null));
//    }
//
//    // ── Vendor Management ─────────────────────────────────────────────────────
//
//    @GetMapping("/vendors")
//    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAllVendors(
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "10") int size,
//            @RequestParam(defaultValue = "") String approved) {
//
//        StringBuilder sql = new StringBuilder("""
//            SELECT v.*, u.full_name, u.email, u.phone,
//                   (SELECT COUNT(*) FROM cars WHERE vendor_id = v.id) AS totalCars,
//                   (SELECT COUNT(*) FROM bookings WHERE vendor_id = v.id) AS totalBookings,
//                   (SELECT COALESCE(SUM(p.amount),0) FROM payments p JOIN bookings b ON p.booking_id = b.id
//                    WHERE b.vendor_id = v.id AND p.payment_status = 'SUCCESS') AS grossRevenue
//            FROM vendors v JOIN users u ON v.user_id = u.id WHERE 1=1
//        """);
//        List<Object> params = new java.util.ArrayList<>();
//        if (!approved.isEmpty()) { sql.append(" AND v.is_approved = ?"); params.add(approved.equals("true")); }
//        sql.append(" ORDER BY v.created_at DESC LIMIT ? OFFSET ?");
//        params.add(size); params.add(page * size);
//
//        List<Map<String, Object>> vendors = jdbcTemplate.queryForList(sql.toString(), params.toArray());
//        return ResponseEntity.ok(ApiResponse.success(vendors));
//    }
//
//    @PatchMapping("/vendors/{id}/approve")
//    public ResponseEntity<ApiResponse<Void>> approveVendor(
//            @PathVariable Long id,
//            @RequestParam boolean approved,
//            Authentication auth) {
//        Long adminId = (Long) auth.getCredentials();
//        jdbcTemplate.update("UPDATE vendors SET is_approved = ? WHERE id = ?", approved, id);
//        jdbcTemplate.update("INSERT INTO audit_logs (user_id, action, entity_type, entity_id, details) VALUES (?,?,?,?,?)",
//            adminId, approved ? "APPROVE_VENDOR" : "REJECT_VENDOR", "VENDOR", id,
//            approved ? "Vendor approved" : "Vendor rejected");
//
//        // Notify vendor
//        Long vendorUserId = jdbcTemplate.queryForObject("SELECT user_id FROM vendors WHERE id = ?", Long.class, id);
//        if (vendorUserId != null) {
//            jdbcTemplate.update("""
//                INSERT INTO notifications (user_id, title, message, type, reference_id, reference_type)
//                VALUES (?, ?, ?, 'VENDOR', ?, 'VENDOR')
//            """, vendorUserId,
//                approved ? "Vendor Account Approved!" : "Vendor Application Rejected",
//                approved ? "Congratulations! Your vendor account has been approved." : "Your vendor application was rejected.",
//                id);
//        }
//        return ResponseEntity.ok(ApiResponse.success(approved ? "Vendor approved" : "Vendor rejected", null));
//    }
//
//    // ── Commission & Payouts ──────────────────────────────────────────────────
//
//    @GetMapping("/payouts")
//    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAllPayouts(
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "10") int size,
//            @RequestParam(defaultValue = "") String status) {
//
//        StringBuilder sql = new StringBuilder("""
//            SELECT vp.*, v.company_name, u.full_name AS vendorOwner, u.email AS vendorEmail
//            FROM vendor_payouts vp
//            JOIN vendors v ON vp.vendor_id = v.id
//            JOIN users u ON v.user_id = u.id
//            WHERE 1=1
//        """);
//        List<Object> params = new java.util.ArrayList<>();
//        if (!status.isEmpty()) { sql.append(" AND vp.status = ?"); params.add(status); }
//        sql.append(" ORDER BY vp.created_at DESC LIMIT ? OFFSET ?");
//        params.add(size); params.add(page * size);
//
//        List<Map<String, Object>> payouts = jdbcTemplate.queryForList(sql.toString(), params.toArray());
//        return ResponseEntity.ok(ApiResponse.success(payouts));
//    }
//
//    @PostMapping("/payouts/generate/{vendorId}")
//    public ResponseEntity<ApiResponse<Void>> generatePayout(
//            @PathVariable Long vendorId,
//            @RequestBody Map<String, String> body) {
//        // Calculate unpaid revenue for period
//        String from = body.get("periodFrom");
//        String to = body.get("periodTo");
//
//        Map<String, Object> revenue = jdbcTemplate.queryForMap("""
//            SELECT COALESCE(SUM(p.amount), 0) AS gross
//            FROM payments p JOIN bookings b ON p.booking_id = b.id
//            WHERE b.vendor_id = ? AND p.payment_status = 'SUCCESS'
//              AND DATE(p.payment_date) BETWEEN ? AND ?
//        """, vendorId, from, to);
//
//        BigDecimal gross = new BigDecimal(revenue.get("gross").toString());
//        BigDecimal commissionRate = new BigDecimal("15.00");
//        BigDecimal commission = gross.multiply(commissionRate).divide(new BigDecimal("100"));
//        BigDecimal net = gross.subtract(commission);
//
//        jdbcTemplate.update("""
//            INSERT INTO vendor_payouts (vendor_id, period_from, period_to, gross_amount,
//            commission_rate, commission_amount, net_amount)
//            VALUES (?, ?, ?, ?, ?, ?, ?)
//        """, vendorId, from, to, gross, commissionRate, commission, net);
//
//        return ResponseEntity.ok(ApiResponse.success("Payout generated", null));
//    }
//
//    @PatchMapping("/payouts/{id}/process")
//    public ResponseEntity<ApiResponse<Void>> processPayout(
//            @PathVariable Long id,
//            @RequestBody Map<String, String> body,
//            Authentication auth) {
//        Long adminId = (Long) auth.getCredentials();
//        jdbcTemplate.update("""
//            UPDATE vendor_payouts SET status = 'PAID', paid_at = NOW(),
//            transaction_ref = ?, notes = ? WHERE id = ?
//        """, body.get("transactionRef"), body.get("notes"), id);
//        jdbcTemplate.update("INSERT INTO audit_logs (user_id, action, entity_type, entity_id, details) VALUES (?,?,?,?,?)",
//            adminId, "PROCESS_PAYOUT", "PAYOUT", id, "Payout processed: ref=" + body.get("transactionRef"));
//        return ResponseEntity.ok(ApiResponse.success("Payout processed", null));
//    }
//
//    // ── Disputes Management ───────────────────────────────────────────────────
//
//    @GetMapping("/disputes")
//    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAllDisputes(
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "10") int size,
//            @RequestParam(defaultValue = "") String status) {
//
//        StringBuilder sql = new StringBuilder("""
//            SELECT d.*, b.booking_number, b.total_amount,
//                   u.full_name AS raisedByName, u.email AS raisedByEmail,
//                   c.brand AS carBrand, c.model AS carModel
//            FROM disputes d
//            JOIN bookings b ON d.booking_id = b.id
//            JOIN users u ON d.raised_by = u.id
//            JOIN cars c ON b.car_id = c.id
//            WHERE 1=1
//        """);
//        List<Object> params = new java.util.ArrayList<>();
//        if (!status.isEmpty()) { sql.append(" AND d.status = ?"); params.add(status); }
//        sql.append(" ORDER BY d.created_at DESC LIMIT ? OFFSET ?");
//        params.add(size); params.add(page * size);
//
//        List<Map<String, Object>> disputes = jdbcTemplate.queryForList(sql.toString(), params.toArray());
//        return ResponseEntity.ok(ApiResponse.success(disputes));
//    }
//
//    @PatchMapping("/disputes/{id}/resolve")
//    public ResponseEntity<ApiResponse<Void>> resolveDispute(
//            @PathVariable Long id,
//            @RequestBody Map<String, String> body,
//            Authentication auth) {
//        Long adminId = (Long) auth.getCredentials();
//        jdbcTemplate.update("""
//            UPDATE disputes SET status = 'RESOLVED', resolution = ?,
//            resolved_by = ?, resolved_at = NOW(), updated_at = NOW()
//            WHERE id = ?
//        """, body.get("resolution"), adminId, id);
//
//        // Notify the customer
//        jdbcTemplate.update("""
//            INSERT INTO notifications (user_id, title, message, type, reference_id, reference_type)
//            SELECT d.raised_by, 'Dispute Resolved', ?, 'DISPUTE', d.id, 'DISPUTE'
//            FROM disputes d WHERE d.id = ?
//        """, "Your dispute has been resolved: " + body.get("resolution"), id);
//
//        return ResponseEntity.ok(ApiResponse.success("Dispute resolved", null));
//    }
//
//    // ── License Verifications ─────────────────────────────────────────────────
//
//    @GetMapping("/licenses")
//    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getLicenses(
//            @RequestParam(defaultValue = "") String status,
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "20") int size) {
//
//        StringBuilder sql = new StringBuilder("""
//            SELECT lv.*, u.full_name, u.email, u.phone
//            FROM license_verifications lv JOIN users u ON lv.user_id = u.id
//            WHERE 1=1
//        """);
//        List<Object> params = new java.util.ArrayList<>();
//        if (!status.isEmpty()) { sql.append(" AND lv.status = ?"); params.add(status); }
//        sql.append(" ORDER BY lv.created_at DESC LIMIT ? OFFSET ?");
//        params.add(size); params.add(page * size);
//
//        List<Map<String, Object>> licenses = jdbcTemplate.queryForList(sql.toString(), params.toArray());
//        return ResponseEntity.ok(ApiResponse.success(licenses));
//    }
//
//    @PatchMapping("/licenses/{id}/verify")
//    public ResponseEntity<ApiResponse<Void>> verifyLicense(
//            @PathVariable Long id,
//            @RequestBody Map<String, String> body,
//            Authentication auth) {
//        Long adminId = (Long) auth.getCredentials();
//        String status = body.get("status"); // VERIFIED or REJECTED
//        String reason = body.get("reason");
//
//        jdbcTemplate.update("""
//            UPDATE license_verifications SET status = ?, rejection_reason = ?,
//            verified_by = ?, verified_at = NOW() WHERE id = ?
//        """, status, reason, adminId, id);
//
//        // Notify user
//        jdbcTemplate.update("""
//            INSERT INTO notifications (user_id, title, message, type)
//            SELECT user_id,
//                   CASE WHEN ? = 'VERIFIED' THEN 'License Verified ✓' ELSE 'License Rejected' END,
//                   CASE WHEN ? = 'VERIFIED' THEN 'Your driving license has been verified successfully!'
//                        ELSE CONCAT('License rejected: ', COALESCE(?, 'Please re-submit')) END,
//                   'LICENSE'
//            FROM license_verifications WHERE id = ?
//        """, status, status, reason, id);
//
//        return ResponseEntity.ok(ApiResponse.success("License " + status.toLowerCase(), null));
//    }
//
//    // ── Damage Reports ────────────────────────────────────────────────────────
//
//    @GetMapping("/damage-reports")
//    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getDamageReports(
//            @RequestParam(defaultValue = "") String status,
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "20") int size) {
//
//        StringBuilder sql = new StringBuilder("""
//            SELECT dr.*, c.brand, c.model, c.registration_no,
//                   b.booking_number, u.full_name AS reportedByName,
//                   v.company_name AS vendorName
//            FROM damage_reports dr
//            JOIN cars c ON dr.car_id = c.id
//            LEFT JOIN bookings b ON dr.booking_id = b.id
//            JOIN users u ON dr.reported_by = u.id
//            LEFT JOIN vendors v ON c.vendor_id = v.id
//            WHERE 1=1
//        """);
//        List<Object> params = new java.util.ArrayList<>();
//        if (!status.isEmpty()) { sql.append(" AND dr.status = ?"); params.add(status); }
//        sql.append(" ORDER BY dr.created_at DESC LIMIT ? OFFSET ?");
//        params.add(size); params.add(page * size);
//
//        List<Map<String, Object>> reports = jdbcTemplate.queryForList(sql.toString(), params.toArray());
//        return ResponseEntity.ok(ApiResponse.success(reports));
//    }
//
//    @PatchMapping("/damage-reports/{id}")
//    public ResponseEntity<ApiResponse<Void>> updateDamageReport(
//            @PathVariable Long id,
//            @RequestBody Map<String, Object> body) {
//        jdbcTemplate.update("""
//            UPDATE damage_reports SET status = ?, actual_cost = ?,
//            charged_to_customer = ?, updated_at = NOW() WHERE id = ?
//        """, body.get("status"), body.get("actualCost"), body.get("chargedToCustomer"), id);
//        return ResponseEntity.ok(ApiResponse.success("Damage report updated", null));
//    }
//
//    // ── Audit Logs ────────────────────────────────────────────────────────────
//
//    @GetMapping("/audit-logs")
//    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAuditLogs(
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "20") int size,
//            @RequestParam(defaultValue = "") String action,
//            @RequestParam(defaultValue = "") String userId,
//            @RequestParam(defaultValue = "") String dateFrom,
//            @RequestParam(defaultValue = "") String dateTo) {
//
//        StringBuilder sql = new StringBuilder("""
//            SELECT al.*, u.full_name AS userFullName, u.email AS userEmail
//            FROM audit_logs al LEFT JOIN users u ON al.user_id = u.id WHERE 1=1
//        """);
//        List<Object> params = new java.util.ArrayList<>();
//        if (!action.isEmpty()) { sql.append(" AND al.action = ?"); params.add(action); }
//        if (!userId.isEmpty()) { sql.append(" AND al.user_id = ?"); params.add(Long.parseLong(userId)); }
//        if (!dateFrom.isEmpty()) { sql.append(" AND DATE(al.created_at) >= ?"); params.add(dateFrom); }
//        if (!dateTo.isEmpty()) { sql.append(" AND DATE(al.created_at) <= ?"); params.add(dateTo); }
//        sql.append(" ORDER BY al.created_at DESC LIMIT ? OFFSET ?");
//        params.add(size); params.add(page * size);
//
//        List<Map<String, Object>> logs = jdbcTemplate.queryForList(sql.toString(), params.toArray());
//        return ResponseEntity.ok(ApiResponse.success(logs));
//    }
//
//    // ── Reports ───────────────────────────────────────────────────────────────
//
//    @GetMapping("/reports/summary")
//    public ResponseEntity<ApiResponse<Map<String, Object>>> getReportSummary() {
//        double[] revenue = new double[12];
//        long[] bookings = new long[12];
//
//        jdbcTemplate.queryForList("""
//            SELECT MONTH(payment_date) AS month, COALESCE(SUM(amount), 0) AS revenue
//            FROM payments WHERE YEAR(payment_date) = YEAR(CURDATE()) AND payment_status = 'SUCCESS'
//            GROUP BY MONTH(payment_date)
//        """).forEach(row -> {
//            int m = ((Number) row.get("month")).intValue() - 1;
//            revenue[m] = ((Number) row.get("revenue")).doubleValue();
//        });
//
//        jdbcTemplate.queryForList("""
//            SELECT MONTH(created_at) AS month, COUNT(*) AS count
//            FROM bookings WHERE YEAR(created_at) = YEAR(CURDATE())
//            GROUP BY MONTH(created_at)
//        """).forEach(row -> {
//            int m = ((Number) row.get("month")).intValue() - 1;
//            bookings[m] = ((Number) row.get("count")).longValue();
//        });
//
//        List<Map<String, Object>> topCars = jdbcTemplate.queryForList("""
//            SELECT c.brand, c.model, COUNT(b.id) AS bookingCount,
//                   COALESCE(SUM(b.total_amount), 0) AS revenue
//            FROM bookings b JOIN cars c ON b.car_id = c.id
//            WHERE b.status = 'COMPLETED'
//            GROUP BY c.id ORDER BY bookingCount DESC LIMIT 5
//        """);
//
//        List<Map<String, Object>> topVendors = jdbcTemplate.queryForList("""
//            SELECT v.company_name, COUNT(b.id) AS bookings,
//                   COALESCE(SUM(p.amount), 0) AS revenue
//            FROM vendors v
//            LEFT JOIN bookings b ON b.vendor_id = v.id
//            LEFT JOIN payments p ON p.booking_id = b.id AND p.payment_status = 'SUCCESS'
//            GROUP BY v.id ORDER BY revenue DESC LIMIT 5
//        """);
//
//        return ResponseEntity.ok(ApiResponse.success(Map.of(
//            "monthlyRevenue", revenue,
//            "monthlyBookings", bookings,
//            "topCars", topCars,
//            "topVendors", topVendors
//        )));
//    }
//
//    // ── Fraud Detection ───────────────────────────────────────────────────────
//
//    @GetMapping("/fraud/suspicious-users")
//    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getSuspiciousUsers() {
//        // Users with high cancellation rates
//        List<Map<String, Object>> highCancellations = jdbcTemplate.queryForList("""
//            SELECT u.id, u.full_name, u.email,
//                   COUNT(*) AS totalBookings,
//                   SUM(CASE WHEN b.status = 'CANCELLED' THEN 1 ELSE 0 END) AS cancellations,
//                   ROUND(SUM(CASE WHEN b.status = 'CANCELLED' THEN 1 ELSE 0 END) * 100.0 / COUNT(*), 2) AS cancelRate
//            FROM bookings b JOIN users u ON b.customer_id = u.id
//            GROUP BY u.id
//            HAVING totalBookings >= 3 AND cancelRate > 50
//            ORDER BY cancelRate DESC LIMIT 20
//        """);
//        return ResponseEntity.ok(ApiResponse.success(highCancellations));
//    }
//
//    @GetMapping("/fraud/payment-failures")
//    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getPaymentFailures() {
//        List<Map<String, Object>> failures = jdbcTemplate.queryForList("""
//            SELECT u.id, u.full_name, u.email,
//                   COUNT(p.id) AS failedPayments,
//                   MAX(p.created_at) AS lastFailure
//            FROM payments p
//            JOIN bookings b ON p.booking_id = b.id
//            JOIN users u ON b.customer_id = u.id
//            WHERE p.payment_status = 'FAILED'
//              AND p.created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)
//            GROUP BY u.id
//            HAVING failedPayments >= 2
//            ORDER BY failedPayments DESC LIMIT 20
//        """);
//        return ResponseEntity.ok(ApiResponse.success(failures));
//    }
//}

package com.example.demo.Controller;

import com.example.demo.DTOs.ApiResponse;
import com.example.demo.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    // ✅ FIX: Cap size to max 100 to prevent abuse / huge DB queries
    private int capSize(int size) {
        return Math.min(size, 100);
    }

    // ── Dashboard Overview ────────────────────────────────────────────────────

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOverview() {
        Map<String, Object> stats = jdbcTemplate.queryForMap("""
                SELECT
                  (SELECT COUNT(*) FROM users) AS totalUsers,
                  (SELECT COUNT(*) FROM vendors) AS totalVendors,
                  (SELECT COUNT(*) FROM vendors WHERE is_approved = false) AS pendingVendors,
                  (SELECT COUNT(*) FROM drivers) AS totalDrivers,
                  (SELECT COUNT(*) FROM cars) AS totalCars,
                  (SELECT COUNT(*) FROM cars WHERE is_available = true) AS availableCars,
                  (SELECT COUNT(*) FROM bookings) AS totalBookings,
                  (SELECT COUNT(*) FROM bookings WHERE status = 'PENDING') AS pendingBookings,
                  (SELECT COUNT(*) FROM bookings WHERE status = 'ACTIVE') AS activeBookings,
                  (SELECT COUNT(*) FROM bookings WHERE status = 'COMPLETED') AS completedBookings,
                  (SELECT COUNT(*) FROM bookings WHERE DATE(created_at) = CURDATE()) AS todayBookings,
                  (SELECT COALESCE(SUM(amount),0) FROM payments WHERE payment_status = 'SUCCESS') AS totalRevenue,
                  (SELECT COALESCE(SUM(amount),0) FROM payments WHERE payment_status = 'SUCCESS' AND DATE(payment_date) = CURDATE()) AS todayRevenue,
                  (SELECT COALESCE(SUM(amount),0) FROM payments WHERE payment_status = 'SUCCESS' AND YEAR(payment_date) = YEAR(CURDATE()) AND MONTH(payment_date) = MONTH(CURDATE())) AS monthRevenue,
                  (SELECT COUNT(*) FROM disputes WHERE status = 'OPEN') AS openDisputes,
                  (SELECT COUNT(*) FROM license_verifications WHERE status = 'PENDING') AS pendingLicenses,
                  (SELECT COUNT(*) FROM damage_reports WHERE status = 'OPEN') AS newDamageReports
                """);

        List<Map<String, Object>> monthlyRevenue = jdbcTemplate.queryForList("""
                SELECT MONTH(payment_date) AS month, COALESCE(SUM(amount),0) AS revenue, COUNT(*) AS transactions
                FROM payments
                WHERE YEAR(payment_date) = YEAR(CURDATE()) AND payment_status = 'SUCCESS'
                GROUP BY MONTH(payment_date) ORDER BY month
                """);

        List<Map<String, Object>> recentBookings = jdbcTemplate.queryForList("""
                SELECT b.booking_number, b.status, b.total_amount, b.created_at,
                       u.full_name AS customerName, c.brand, c.model
                FROM bookings b JOIN users u ON b.customer_id = u.id JOIN cars c ON b.car_id = c.id
                ORDER BY b.created_at DESC LIMIT 5
                """);

        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "stats", stats,
                "monthlyRevenue", monthlyRevenue,
                "recentBookings", recentBookings
        )));
    }

    // ── Users ─────────────────────────────────────────────────────────────────

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "") String role) {

        size = capSize(size); // ✅ FIX: cap size

        StringBuilder sql = new StringBuilder("""
                SELECT u.id, u.full_name AS fullName, u.email, u.phone,
                       u.is_active AS isActive, u.created_at AS createdAt, r.name AS roleName,
                       (SELECT COUNT(*) FROM bookings WHERE customer_id = u.id) AS totalBookings,
                       lv.status AS licenseStatus
                FROM users u
                JOIN roles r ON u.role_id = r.id
                LEFT JOIN license_verifications lv ON lv.user_id = u.id
                WHERE (u.full_name LIKE ? OR u.email LIKE ?)
                """);
        String like = "%" + search + "%";
        List<Object> params = new java.util.ArrayList<>(List.of(like, like));

        if (!role.isEmpty()) { sql.append(" AND r.name = ?"); params.add(role); }
        sql.append(" ORDER BY u.created_at DESC LIMIT ? OFFSET ?");
        params.add(size); params.add(page * size);

        List<Map<String, Object>> users = jdbcTemplate.queryForList(sql.toString(), params.toArray());
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @PatchMapping("/users/{id}/toggle-active")
    public ResponseEntity<ApiResponse<Void>> toggleActive(@PathVariable Long id, Authentication auth) {
        Long adminId = (Long) auth.getCredentials();
        Integer current = jdbcTemplate.queryForObject(
                "SELECT is_active FROM users WHERE id = ?", Integer.class, id);
        if (current == null) return ResponseEntity.notFound().build();
        jdbcTemplate.update("UPDATE users SET is_active = ?, updated_at = NOW() WHERE id = ?",
                current == 0, id);
        jdbcTemplate.update("INSERT INTO audit_logs (user_id, action, entity_type, entity_id, details) VALUES (?,?,?,?,?)",
                adminId, "TOGGLE_USER", "USER", id, "Status changed to " + (current == 0 ? "ACTIVE" : "INACTIVE"));
        return ResponseEntity.ok(ApiResponse.success("User status updated", null));
    }

    @PatchMapping("/users/{id}/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            Authentication auth) {
        Long adminId = (Long) auth.getCredentials();
        String newPassword = body.get("newPassword");
        if (newPassword == null || newPassword.length() < 6)
            return ResponseEntity.badRequest().body(ApiResponse.error("Password must be at least 6 characters"));
        jdbcTemplate.update("UPDATE users SET password = ?, updated_at = NOW() WHERE id = ?",
                passwordEncoder.encode(newPassword), id);
        jdbcTemplate.update("INSERT INTO audit_logs (user_id, action, entity_type, entity_id, details) VALUES (?,?,?,?,?)",
                adminId, "RESET_PASSWORD", "USER", id, "Admin password reset");
        return ResponseEntity.ok(ApiResponse.success("Password reset successfully", null));
    }

    @PatchMapping("/users/{id}/role")
    public ResponseEntity<ApiResponse<Void>> changeRole(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            Authentication auth) {
        Long adminId = (Long) auth.getCredentials();
        String roleName = body.get("roleName");
        Long roleId = jdbcTemplate.queryForObject("SELECT id FROM roles WHERE name = ?", Long.class, roleName);
        if (roleId == null) return ResponseEntity.badRequest().body(ApiResponse.error("Role not found"));
        jdbcTemplate.update("UPDATE users SET role_id = ?, updated_at = NOW() WHERE id = ?", roleId, id);
        jdbcTemplate.update("INSERT INTO audit_logs (user_id, action, entity_type, entity_id, details) VALUES (?,?,?,?,?)",
                adminId, "CHANGE_ROLE", "USER", id, "Role changed to " + roleName);
        return ResponseEntity.ok(ApiResponse.success("Role updated", null));
    }

    // ── Vendor Management ─────────────────────────────────────────────────────

    @GetMapping("/vendors")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAllVendors(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String approved) {

        size = capSize(size); // ✅ FIX: cap size

        StringBuilder sql = new StringBuilder("""
                SELECT v.*, u.full_name, u.email, u.phone,
                       (SELECT COUNT(*) FROM cars WHERE vendor_id = v.id) AS totalCars,
                       (SELECT COUNT(*) FROM bookings WHERE vendor_id = v.id) AS totalBookings,
                       (SELECT COALESCE(SUM(p.amount),0) FROM payments p JOIN bookings b ON p.booking_id = b.id
                        WHERE b.vendor_id = v.id AND p.payment_status = 'SUCCESS') AS grossRevenue
                FROM vendors v JOIN users u ON v.user_id = u.id WHERE 1=1
                """);
        List<Object> params = new java.util.ArrayList<>();
        if (!approved.isEmpty()) { sql.append(" AND v.is_approved = ?"); params.add(approved.equals("true")); }
        sql.append(" ORDER BY v.created_at DESC LIMIT ? OFFSET ?");
        params.add(size); params.add(page * size);

        List<Map<String, Object>> vendors = jdbcTemplate.queryForList(sql.toString(), params.toArray());
        return ResponseEntity.ok(ApiResponse.success(vendors));
    }

    @PatchMapping("/vendors/{id}/approve")
    public ResponseEntity<ApiResponse<Void>> approveVendor(
            @PathVariable Long id,
            @RequestParam boolean approved,
            Authentication auth) {
        Long adminId = (Long) auth.getCredentials();
        jdbcTemplate.update("UPDATE vendors SET is_approved = ? WHERE id = ?", approved, id);
        jdbcTemplate.update("INSERT INTO audit_logs (user_id, action, entity_type, entity_id, details) VALUES (?,?,?,?,?)",
                adminId, approved ? "APPROVE_VENDOR" : "REJECT_VENDOR", "VENDOR", id,
                approved ? "Vendor approved" : "Vendor rejected");

        Long vendorUserId = jdbcTemplate.queryForObject("SELECT user_id FROM vendors WHERE id = ?", Long.class, id);
        if (vendorUserId != null) {
            jdbcTemplate.update("""
                    INSERT INTO notifications (user_id, title, message, type, reference_id, reference_type)
                    VALUES (?, ?, ?, 'VENDOR', ?, 'VENDOR')
                    """, vendorUserId,
                    approved ? "Vendor Account Approved!" : "Vendor Application Rejected",
                    approved ? "Congratulations! Your vendor account has been approved." : "Your vendor application was rejected.",
                    id);
        }
        return ResponseEntity.ok(ApiResponse.success(approved ? "Vendor approved" : "Vendor rejected", null));
    }

    // ── Commission & Payouts ──────────────────────────────────────────────────

    @GetMapping("/payouts")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAllPayouts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String status) {

        size = capSize(size); // ✅ FIX: cap size

        StringBuilder sql = new StringBuilder("""
                SELECT vp.*, v.company_name, u.full_name AS vendorOwner, u.email AS vendorEmail
                FROM vendor_payouts vp
                JOIN vendors v ON vp.vendor_id = v.id
                JOIN users u ON v.user_id = u.id
                WHERE 1=1
                """);
        List<Object> params = new java.util.ArrayList<>();
        if (!status.isEmpty()) { sql.append(" AND vp.status = ?"); params.add(status); }
        sql.append(" ORDER BY vp.created_at DESC LIMIT ? OFFSET ?");
        params.add(size); params.add(page * size);

        List<Map<String, Object>> payouts = jdbcTemplate.queryForList(sql.toString(), params.toArray());
        return ResponseEntity.ok(ApiResponse.success(payouts));
    }

    @PostMapping("/payouts/generate/{vendorId}")
    public ResponseEntity<ApiResponse<Void>> generatePayout(
            @PathVariable Long vendorId,
            @RequestBody Map<String, String> body) {
        String from = body.get("periodFrom");
        String to = body.get("periodTo");

        Map<String, Object> revenue = jdbcTemplate.queryForMap("""
                SELECT COALESCE(SUM(p.amount), 0) AS gross
                FROM payments p JOIN bookings b ON p.booking_id = b.id
                WHERE b.vendor_id = ? AND p.payment_status = 'SUCCESS'
                  AND DATE(p.payment_date) BETWEEN ? AND ?
                """, vendorId, from, to);

        BigDecimal gross = new BigDecimal(revenue.get("gross").toString());
        BigDecimal commissionRate = new BigDecimal("15.00");
        BigDecimal commission = gross.multiply(commissionRate).divide(new BigDecimal("100"));
        BigDecimal net = gross.subtract(commission);

        jdbcTemplate.update("""
                INSERT INTO vendor_payouts (vendor_id, period_from, period_to, gross_amount,
                commission_rate, commission_amount, net_amount)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, vendorId, from, to, gross, commissionRate, commission, net);

        return ResponseEntity.ok(ApiResponse.success("Payout generated", null));
    }

    @PatchMapping("/payouts/{id}/process")
    public ResponseEntity<ApiResponse<Void>> processPayout(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            Authentication auth) {
        Long adminId = (Long) auth.getCredentials();
        jdbcTemplate.update("""
                UPDATE vendor_payouts SET status = 'PAID', paid_at = NOW(),
                transaction_ref = ?, notes = ? WHERE id = ?
                """, body.get("transactionRef"), body.get("notes"), id);
        jdbcTemplate.update("INSERT INTO audit_logs (user_id, action, entity_type, entity_id, details) VALUES (?,?,?,?,?)",
                adminId, "PROCESS_PAYOUT", "PAYOUT", id, "Payout processed: ref=" + body.get("transactionRef"));
        return ResponseEntity.ok(ApiResponse.success("Payout processed", null));
    }

    // ── Disputes Management ───────────────────────────────────────────────────

    @GetMapping("/disputes")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAllDisputes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String status) {

        size = capSize(size); // ✅ FIX: cap size

        StringBuilder sql = new StringBuilder("""
                SELECT d.*, b.booking_number, b.total_amount,
                       u.full_name AS raisedByName, u.email AS raisedByEmail,
                       c.brand AS carBrand, c.model AS carModel
                FROM disputes d
                JOIN bookings b ON d.booking_id = b.id
                JOIN users u ON d.raised_by = u.id
                JOIN cars c ON b.car_id = c.id
                WHERE 1=1
                """);
        List<Object> params = new java.util.ArrayList<>();
        if (!status.isEmpty()) { sql.append(" AND d.status = ?"); params.add(status); }
        sql.append(" ORDER BY d.created_at DESC LIMIT ? OFFSET ?");
        params.add(size); params.add(page * size);

        List<Map<String, Object>> disputes = jdbcTemplate.queryForList(sql.toString(), params.toArray());
        return ResponseEntity.ok(ApiResponse.success(disputes));
    }

    @PatchMapping("/disputes/{id}/resolve")
    public ResponseEntity<ApiResponse<Void>> resolveDispute(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            Authentication auth) {
        Long adminId = (Long) auth.getCredentials();
        jdbcTemplate.update("""
                UPDATE disputes SET status = 'RESOLVED', resolution = ?,
                resolved_by = ?, resolved_at = NOW(), updated_at = NOW()
                WHERE id = ?
                """, body.get("resolution"), adminId, id);

        jdbcTemplate.update("""
                INSERT INTO notifications (user_id, title, message, type, reference_id, reference_type)
                SELECT d.raised_by, 'Dispute Resolved', ?, 'DISPUTE', d.id, 'DISPUTE'
                FROM disputes d WHERE d.id = ?
                """, "Your dispute has been resolved: " + body.get("resolution"), id);

        return ResponseEntity.ok(ApiResponse.success("Dispute resolved", null));
    }

    // ── License Verifications ─────────────────────────────────────────────────

    @GetMapping("/licenses")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getLicenses(
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        size = capSize(size); // ✅ FIX: cap size

        StringBuilder sql = new StringBuilder("""
                SELECT lv.*, u.full_name, u.email, u.phone
                FROM license_verifications lv JOIN users u ON lv.user_id = u.id
                WHERE 1=1
                """);
        List<Object> params = new java.util.ArrayList<>();
        if (!status.isEmpty()) { sql.append(" AND lv.status = ?"); params.add(status); }
        sql.append(" ORDER BY lv.created_at DESC LIMIT ? OFFSET ?");
        params.add(size); params.add(page * size);

        List<Map<String, Object>> licenses = jdbcTemplate.queryForList(sql.toString(), params.toArray());
        return ResponseEntity.ok(ApiResponse.success(licenses));
    }

    @PatchMapping("/licenses/{id}/verify")
    public ResponseEntity<ApiResponse<Void>> verifyLicense(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            Authentication auth) {
        Long adminId = (Long) auth.getCredentials();
        String status = body.get("status");
        String reason = body.get("reason");

        jdbcTemplate.update("""
                UPDATE license_verifications SET status = ?, rejection_reason = ?,
                verified_by = ?, verified_at = NOW() WHERE id = ?
                """, status, reason, adminId, id);

        jdbcTemplate.update("""
                INSERT INTO notifications (user_id, title, message, type)
                SELECT user_id,
                       CASE WHEN ? = 'VERIFIED' THEN 'License Verified ✓' ELSE 'License Rejected' END,
                       CASE WHEN ? = 'VERIFIED' THEN 'Your driving license has been verified successfully!'
                            ELSE CONCAT('License rejected: ', COALESCE(?, 'Please re-submit')) END,
                       'LICENSE'
                FROM license_verifications WHERE id = ?
                """, status, status, reason, id);

        return ResponseEntity.ok(ApiResponse.success("License " + status.toLowerCase(), null));
    }

    // ── Damage Reports ────────────────────────────────────────────────────────

    @GetMapping("/damage-reports")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getDamageReports(
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        size = capSize(size); // ✅ FIX: cap size

        StringBuilder sql = new StringBuilder("""
                SELECT dr.*, c.brand, c.model, c.registration_no,
                       b.booking_number, u.full_name AS reportedByName,
                       v.company_name AS vendorName
                FROM damage_reports dr
                JOIN cars c ON dr.car_id = c.id
                LEFT JOIN bookings b ON dr.booking_id = b.id
                JOIN users u ON dr.reported_by = u.id
                LEFT JOIN vendors v ON c.vendor_id = v.id
                WHERE 1=1
                """);
        List<Object> params = new java.util.ArrayList<>();
        if (!status.isEmpty()) { sql.append(" AND dr.status = ?"); params.add(status); }
        sql.append(" ORDER BY dr.created_at DESC LIMIT ? OFFSET ?");
        params.add(size); params.add(page * size);

        List<Map<String, Object>> reports = jdbcTemplate.queryForList(sql.toString(), params.toArray());
        return ResponseEntity.ok(ApiResponse.success(reports));
    }

    @PatchMapping("/damage-reports/{id}")
    public ResponseEntity<ApiResponse<Void>> updateDamageReport(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        jdbcTemplate.update("""
                UPDATE damage_reports SET status = ?, actual_cost = ?,
                charged_to_customer = ?, updated_at = NOW() WHERE id = ?
                """, body.get("status"), body.get("actualCost"), body.get("chargedToCustomer"), id);
        return ResponseEntity.ok(ApiResponse.success("Damage report updated", null));
    }

    // ── Audit Logs ────────────────────────────────────────────────────────────

    @GetMapping("/audit-logs")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "") String action,
            @RequestParam(defaultValue = "") String userId,
            @RequestParam(defaultValue = "") String dateFrom,
            @RequestParam(defaultValue = "") String dateTo) {

        size = capSize(size); // ✅ FIX: cap size

        StringBuilder sql = new StringBuilder("""
                SELECT al.*, u.full_name AS userFullName, u.email AS userEmail
                FROM audit_logs al LEFT JOIN users u ON al.user_id = u.id WHERE 1=1
                """);
        List<Object> params = new java.util.ArrayList<>();
        if (!action.isEmpty()) { sql.append(" AND al.action = ?"); params.add(action); }
        if (!userId.isEmpty()) { sql.append(" AND al.user_id = ?"); params.add(Long.parseLong(userId)); }
        if (!dateFrom.isEmpty()) { sql.append(" AND DATE(al.created_at) >= ?"); params.add(dateFrom); }
        if (!dateTo.isEmpty()) { sql.append(" AND DATE(al.created_at) <= ?"); params.add(dateTo); }
        sql.append(" ORDER BY al.created_at DESC LIMIT ? OFFSET ?");
        params.add(size); params.add(page * size);

        List<Map<String, Object>> logs = jdbcTemplate.queryForList(sql.toString(), params.toArray());
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    // ── Reports ───────────────────────────────────────────────────────────────

    @GetMapping("/reports/summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getReportSummary() {
        double[] revenue = new double[12];
        long[] bookings = new long[12];

        jdbcTemplate.queryForList("""
                SELECT MONTH(payment_date) AS month, COALESCE(SUM(amount), 0) AS revenue
                FROM payments WHERE YEAR(payment_date) = YEAR(CURDATE()) AND payment_status = 'SUCCESS'
                GROUP BY MONTH(payment_date)
                """).forEach(row -> {
            int m = ((Number) row.get("month")).intValue() - 1;
            revenue[m] = ((Number) row.get("revenue")).doubleValue();
        });

        jdbcTemplate.queryForList("""
                SELECT MONTH(created_at) AS month, COUNT(*) AS count
                FROM bookings WHERE YEAR(created_at) = YEAR(CURDATE())
                GROUP BY MONTH(created_at)
                """).forEach(row -> {
            int m = ((Number) row.get("month")).intValue() - 1;
            bookings[m] = ((Number) row.get("count")).longValue();
        });

        List<Map<String, Object>> topCars = jdbcTemplate.queryForList("""
                SELECT c.brand, c.model, COUNT(b.id) AS bookingCount,
                       COALESCE(SUM(b.total_amount), 0) AS revenue
                FROM bookings b JOIN cars c ON b.car_id = c.id
                WHERE b.status = 'COMPLETED'
                GROUP BY c.id ORDER BY bookingCount DESC LIMIT 5
                """);

        List<Map<String, Object>> topVendors = jdbcTemplate.queryForList("""
                SELECT v.company_name, COUNT(b.id) AS bookings,
                       COALESCE(SUM(p.amount), 0) AS revenue
                FROM vendors v
                LEFT JOIN bookings b ON b.vendor_id = v.id
                LEFT JOIN payments p ON p.booking_id = b.id AND p.payment_status = 'SUCCESS'
                GROUP BY v.id ORDER BY revenue DESC LIMIT 5
                """);

        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "monthlyRevenue", revenue,
                "monthlyBookings", bookings,
                "topCars", topCars,
                "topVendors", topVendors
        )));
    }

    // ── Fraud Detection ───────────────────────────────────────────────────────

    @GetMapping("/fraud/suspicious-users")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getSuspiciousUsers() {
        List<Map<String, Object>> highCancellations = jdbcTemplate.queryForList("""
                SELECT u.id, u.full_name, u.email,
                       COUNT(*) AS totalBookings,
                       SUM(CASE WHEN b.status = 'CANCELLED' THEN 1 ELSE 0 END) AS cancellations,
                       ROUND(SUM(CASE WHEN b.status = 'CANCELLED' THEN 1 ELSE 0 END) * 100.0 / COUNT(*), 2) AS cancelRate
                FROM bookings b JOIN users u ON b.customer_id = u.id
                GROUP BY u.id
                HAVING totalBookings >= 3 AND cancelRate > 50
                ORDER BY cancelRate DESC LIMIT 20
                """);
        return ResponseEntity.ok(ApiResponse.success(highCancellations));
    }

    @GetMapping("/fraud/payment-failures")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getPaymentFailures() {
        List<Map<String, Object>> failures = jdbcTemplate.queryForList("""
                SELECT u.id, u.full_name, u.email,
                       COUNT(p.id) AS failedPayments,
                       MAX(p.created_at) AS lastFailure
                FROM payments p
                JOIN bookings b ON p.booking_id = b.id
                JOIN users u ON b.customer_id = u.id
                WHERE p.payment_status = 'FAILED'
                  AND p.created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)
                GROUP BY u.id
                HAVING failedPayments >= 2
                ORDER BY failedPayments DESC LIMIT 20
                """);
        return ResponseEntity.ok(ApiResponse.success(failures));
    }
}