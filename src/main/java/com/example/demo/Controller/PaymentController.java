package com.example.demo.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.example.demo.DTOs.ApiResponse;
import com.example.demo.Model.Booking;
import com.example.demo.Model.Payment;
import com.example.demo.Service.BookingService;
import com.example.demo.Service.PaymentService;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final BookingService bookingService;

    // ✅ Valid payment methods — anything else is rejected upfront
    private static final Set<String> VALID_PAYMENT_METHODS = Set.of(
            "CASH", "CARD", "UPI", "NET_BANKING", "WALLET"
    );

    public PaymentController(PaymentService paymentService, BookingService bookingService) {
        this.paymentService = paymentService;
        this.bookingService = bookingService;
    }

    // ── Process Payment ───────────────────────────────────────────────────────
    @PostMapping("/process/{bookingId}")
    @PreAuthorize("hasAnyAuthority('ROLE_CUSTOMER', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Payment>> processPayment(
            @PathVariable Long bookingId,
            @RequestParam(defaultValue = "CASH") String paymentMethod,
            Authentication auth) {
        try {
            String method = paymentMethod.toUpperCase().trim();

            // ✅ Validate payment method before doing anything
            if (!VALID_PAYMENT_METHODS.contains(method)) {
                return ResponseEntity.badRequest().body(ApiResponse.error(
                        "Invalid payment method '" + paymentMethod + "'. " +
                        "Allowed values: CASH, CARD, UPI, NET_BANKING, WALLET"));
            }

            Long userId = (Long) auth.getCredentials();
            Booking booking = bookingService.getBookingById(bookingId);

            boolean isAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

            // ✅ Only the booking owner or an admin can pay
            if (!isAdmin && !booking.getCustomerId().equals(userId)) {
                return ResponseEntity.status(403)
                        .body(ApiResponse.error("Not authorized to pay for this booking"));
            }

            Payment payment = paymentService.processPayment(bookingId, method);
            return ResponseEntity.ok(ApiResponse.success("Payment processed successfully", payment));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ── Get All Payments (Admin only) ─────────────────────────────────────────
    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<List<Payment>>> getAllPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        // ✅ Cap page size to prevent accidental overload
        size = Math.min(size, 100);
        return ResponseEntity.ok(ApiResponse.success(paymentService.getAllPayments(page, size)));
    }

    // ── Get Payment By Booking ────────────────────────────────────────────────
    @GetMapping("/booking/{bookingId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Payment>> getPaymentByBooking(
            @PathVariable Long bookingId,
            Authentication auth) {
        try {
            Long userId = (Long) auth.getCredentials();
            Booking booking = bookingService.getBookingById(bookingId);

            boolean isAdminOrVendor = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                               || a.getAuthority().equals("ROLE_VENDOR"));

            // ✅ Only booking owner, admin, or vendor can view payment details
            if (!isAdminOrVendor && !booking.getCustomerId().equals(userId)) {
                return ResponseEntity.status(403)
                        .body(ApiResponse.error("Not authorized to view this payment"));
            }

            return ResponseEntity.ok(ApiResponse.success(
                    paymentService.getPaymentByBookingId(bookingId)));

        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ── Refund Payment (Admin only) ───────────────────────────────────────────
    @PostMapping("/{paymentId}/refund")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Payment>> refundPayment(@PathVariable Long paymentId) {
        try {
            return ResponseEntity.ok(ApiResponse.success("Payment refunded",
                    paymentService.refundPayment(paymentId)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}