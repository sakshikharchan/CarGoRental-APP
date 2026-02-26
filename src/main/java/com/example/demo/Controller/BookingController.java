//package com.example.demo.Controller;
//
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.security.core.Authentication;
//import org.springframework.web.bind.annotation.*;
//
//import com.example.demo.DTOs.ApiResponse;
//import com.example.demo.DTOs.BookingRequest;
//import com.example.demo.Model.Booking;
//import com.example.demo.Service.BookingService;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/bookings")
//public class BookingController {
//
//    private final BookingService bookingService;
//
//    public BookingController(BookingService bookingService) {
//        this.bookingService = bookingService;
//    }
//
//    // CUSTOMER + ADMIN can create bookings
//    @PostMapping
//    @PreAuthorize("hasAnyAuthority('ROLE_CUSTOMER', 'ROLE_ADMIN')")
//    public ResponseEntity<ApiResponse<Booking>> createBooking(
//            @RequestBody BookingRequest request,
//            Authentication auth) {
//        try {
//            Long customerId = (Long) auth.getCredentials();
//            Booking booking = bookingService.createBooking(customerId, request);
//            return ResponseEntity.ok(ApiResponse.success("Booking created successfully", booking));
//        } catch (RuntimeException e) {
//            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
//        }
//    }
//
//    // Any logged-in user sees their own bookings
//    @GetMapping("/my")
//    @PreAuthorize("isAuthenticated()")
//    public ResponseEntity<ApiResponse<List<Booking>>> getMyBookings(
//            Authentication auth,
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "10") int size) {
//        Long customerId = (Long) auth.getCredentials();
//        return ResponseEntity.ok(ApiResponse.success(
//                bookingService.getBookingsByCustomer(customerId, page, size)));
//    }
//
//    // Any logged-in user can cancel their own booking
//    @PatchMapping("/{id}/cancel")
//    @PreAuthorize("isAuthenticated()")
//    public ResponseEntity<ApiResponse<Booking>> cancelBooking(
//            @PathVariable Long id,
//            Authentication auth) {
//        try {
//            Long customerId = (Long) auth.getCredentials();
//            Booking booking = bookingService.getBookingById(id);
//            if (!booking.getCustomerId().equals(customerId)) {
//                return ResponseEntity.status(403)
//                        .body(ApiResponse.error("Not authorized to cancel this booking"));
//            }
//            Booking cancelled = bookingService.updateStatus(id, "CANCELLED");
//            return ResponseEntity.ok(ApiResponse.success("Booking cancelled", cancelled));
//        } catch (RuntimeException e) {
//            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
//        }
//    }
//
//    // ADMIN + VENDOR can see all bookings
//    @GetMapping
//    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_VENDOR')")
//    public ResponseEntity<ApiResponse<List<Booking>>> getAllBookings(
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "10") int size) {
//        return ResponseEntity.ok(ApiResponse.success(bookingService.getAllBookings(page, size)));
//    }
//
//    // ADMIN + VENDOR can filter by status
//    @GetMapping("/status/{status}")
//    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_VENDOR')")
//    public ResponseEntity<ApiResponse<List<Booking>>> getByStatus(
//            @PathVariable String status,
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "10") int size) {
//        return ResponseEntity.ok(ApiResponse.success(
//                bookingService.getBookingsByStatus(status, page, size)));
//    }
//
//    // Authenticated user can view their own booking; ADMIN/VENDOR can view any
//    @GetMapping("/{id}")
//    @PreAuthorize("isAuthenticated()")
//    public ResponseEntity<ApiResponse<Booking>> getBookingById(
//            @PathVariable Long id,
//            Authentication auth) {
//        try {
//            Booking booking = bookingService.getBookingById(id);
//            Long userId = (Long) auth.getCredentials();
//            boolean isAdminOrVendor = auth.getAuthorities().stream()
//                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_VENDOR"));
//            if (!isAdminOrVendor && !booking.getCustomerId().equals(userId)) {
//                return ResponseEntity.status(403)
//                        .body(ApiResponse.error("Not authorized to view this booking"));
//            }
//            return ResponseEntity.ok(ApiResponse.success(booking));
//        } catch (RuntimeException e) {
//            return ResponseEntity.notFound().build();
//        }
//    }
//
//    // Public — track by booking number
//    @GetMapping("/track/{bookingNumber}")
//    public ResponseEntity<ApiResponse<Booking>> trackBooking(@PathVariable String bookingNumber) {
//        try {
//            return ResponseEntity.ok(ApiResponse.success(
//                    bookingService.getByBookingNumber(bookingNumber)));
//        } catch (RuntimeException e) {
//            return ResponseEntity.notFound().build();
//        }
//    }
//
//    // ADMIN + VENDOR can update booking status
//    @PatchMapping("/{id}/status")
//    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_VENDOR')")
//    public ResponseEntity<ApiResponse<Booking>> updateStatus(
//            @PathVariable Long id,
//            @RequestParam String status) {
//        try {
//            Booking updated = bookingService.updateStatus(id, status.toUpperCase());
//            return ResponseEntity.ok(ApiResponse.success("Status updated to " + status, updated));
//        } catch (RuntimeException e) {
//            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
//        }
//    }
//}

package com.example.demo.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.example.demo.DTOs.ApiResponse;
import com.example.demo.DTOs.BookingRequest;
import com.example.demo.Model.Booking;
import com.example.demo.Service.BookingService;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final JdbcTemplate jdbcTemplate;

    public BookingController(BookingService bookingService, JdbcTemplate jdbcTemplate) {
        this.bookingService = bookingService;
        this.jdbcTemplate = jdbcTemplate;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    // ✅ FIX BUG-007: Resolve vendors.id from users.id for vendor scoping
    private Long getVendorIdOrNull(Long userId) {
        List<Long> result = jdbcTemplate.queryForList(
                "SELECT id FROM vendors WHERE user_id = ?", Long.class, userId);
        return result.isEmpty() ? null : result.get(0);
    }

    // ── Endpoints ─────────────────────────────────────────────────────────────

    // CUSTOMER + ADMIN can create bookings
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_CUSTOMER', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Booking>> createBooking(
            @RequestBody BookingRequest request,
            Authentication auth) {
        try {
            Long customerId = (Long) auth.getCredentials();
            Booking booking = bookingService.createBooking(customerId, request);
            return ResponseEntity.ok(ApiResponse.success("Booking created successfully", booking));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // Any logged-in user sees their own bookings
    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<Booking>>> getMyBookings(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long customerId = (Long) auth.getCredentials();
        return ResponseEntity.ok(ApiResponse.success(
                bookingService.getBookingsByCustomer(customerId, page, size)));
    }

    // Any logged-in user can cancel their own booking
    @PatchMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Booking>> cancelBooking(
            @PathVariable Long id,
            Authentication auth) {
        try {
            Long customerId = (Long) auth.getCredentials();
            Booking booking = bookingService.getBookingById(id);
            if (!booking.getCustomerId().equals(customerId)) {
                return ResponseEntity.status(403)
                        .body(ApiResponse.error("Not authorized to cancel this booking"));
            }
            Booking cancelled = bookingService.updateStatus(id, "CANCELLED");
            return ResponseEntity.ok(ApiResponse.success("Booking cancelled", cancelled));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ✅ FIX BUG-007: ROLE_VENDOR only sees their own bookings; ROLE_ADMIN sees all
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_VENDOR')")
    public ResponseEntity<ApiResponse<List<Booking>>> getAllBookings(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        if (!isAdmin(auth)) {
            Long userId = (Long) auth.getCredentials();
            Long vendorId = getVendorIdOrNull(userId);
            if (vendorId == null) {
                return ResponseEntity.status(403)
                        .body(ApiResponse.error("Vendor profile not found"));
            }
            return ResponseEntity.ok(ApiResponse.success(
                    bookingService.getBookingsByVendor(vendorId, page, size)));
        }

        return ResponseEntity.ok(ApiResponse.success(bookingService.getAllBookings(page, size)));
    }

    // ✅ FIX BUG-007: ROLE_VENDOR only sees their own bookings filtered by status
    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_VENDOR')")
    public ResponseEntity<ApiResponse<List<Booking>>> getByStatus(
            @PathVariable String status,
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        if (!isAdmin(auth)) {
            Long userId = (Long) auth.getCredentials();
            Long vendorId = getVendorIdOrNull(userId);
            if (vendorId == null) {
                return ResponseEntity.status(403)
                        .body(ApiResponse.error("Vendor profile not found"));
            }
            List<Booking> all = bookingService.getBookingsByVendor(vendorId, page, size);
            List<Booking> filtered = all.stream()
                    .filter(b -> b.getStatus().equalsIgnoreCase(status))
                    .toList();
            return ResponseEntity.ok(ApiResponse.success(filtered));
        }

        return ResponseEntity.ok(ApiResponse.success(
                bookingService.getBookingsByStatus(status, page, size)));
    }

    // Authenticated user can view their own booking; ADMIN/VENDOR can view any
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Booking>> getBookingById(
            @PathVariable Long id,
            Authentication auth) {
        try {
            Booking booking = bookingService.getBookingById(id);
            Long userId = (Long) auth.getCredentials();
            boolean isAdminOrVendor = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_VENDOR"));

            if (!isAdminOrVendor && !booking.getCustomerId().equals(userId)) {
                return ResponseEntity.status(403)
                        .body(ApiResponse.error("Not authorized to view this booking"));
            }

            // ✅ FIX BUG-007: Vendor can only view bookings belonging to their account
            if (!isAdmin(auth) && isAdminOrVendor) {
                Long vendorId = getVendorIdOrNull(userId);
                if (vendorId != null && !vendorId.equals(booking.getVendorId())) {
                    return ResponseEntity.status(403)
                            .body(ApiResponse.error("Not authorized to view this booking"));
                }
            }

            return ResponseEntity.ok(ApiResponse.success(booking));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Public — track by booking number
    @GetMapping("/track/{bookingNumber}")
    public ResponseEntity<ApiResponse<Booking>> trackBooking(@PathVariable String bookingNumber) {
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    bookingService.getByBookingNumber(bookingNumber)));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ✅ FIX BUG-008: Vendor can only update status of their OWN bookings
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_VENDOR')")
    public ResponseEntity<ApiResponse<Booking>> updateStatus(
            @PathVariable Long id,
            @RequestParam String status,
            Authentication auth) {
        try {
            if (!isAdmin(auth)) {
                Long userId = (Long) auth.getCredentials();
                Long vendorId = getVendorIdOrNull(userId);
                Booking booking = bookingService.getBookingById(id);
                if (vendorId == null || !vendorId.equals(booking.getVendorId())) {
                    return ResponseEntity.status(403)
                            .body(ApiResponse.error("Not authorized to update this booking"));
                }
            }
            Booking updated = bookingService.updateStatus(id, status.toUpperCase());
            return ResponseEntity.ok(ApiResponse.success("Status updated to " + status, updated));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}