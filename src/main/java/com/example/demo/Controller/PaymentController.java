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

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final BookingService bookingService;

    public PaymentController(PaymentService paymentService, BookingService bookingService) {
        this.paymentService = paymentService;
        this.bookingService = bookingService;
    }

    @PostMapping("/process/{bookingId}")
    @PreAuthorize("hasAnyAuthority('ROLE_CUSTOMER', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Payment>> processPayment(
            @PathVariable Long bookingId,
            @RequestParam(defaultValue = "CASH") String paymentMethod,
            Authentication auth) {
        try {
            Long userId = (Long) auth.getCredentials();
            Booking booking = bookingService.getBookingById(bookingId);
            boolean isAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            if (!isAdmin && !booking.getCustomerId().equals(userId)) {
                return ResponseEntity.status(403)
                        .body(ApiResponse.error("Not authorized to pay for this booking"));
            }
            Payment payment = paymentService.processPayment(bookingId, paymentMethod);
            return ResponseEntity.ok(ApiResponse.success("Payment processed successfully", payment));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<List<Payment>>> getAllPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getAllPayments(page, size)));
    }

    @GetMapping("/booking/{bookingId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Payment>> getPaymentByBooking(
            @PathVariable Long bookingId,
            Authentication auth) {
        try {
            Long userId = (Long) auth.getCredentials();
            Booking booking = bookingService.getBookingById(bookingId);
            boolean isAdminOrVendor = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_VENDOR"));
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

//package com.example.demo.Controller;
//
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.security.core.Authentication;
//import org.springframework.web.bind.annotation.*;
//
//import com.example.demo.DTOs.ApiResponse;
//import com.example.demo.Model.Booking;
//import com.example.demo.Model.Payment;
//import com.example.demo.Service.BookingService;
//import com.example.demo.Service.PaymentService;
//
//import java.util.List;
//import java.util.Set;
//
//@RestController
//@RequestMapping("/api/payments")
//public class PaymentController {
//
//    private final PaymentService paymentService;
//    private final BookingService bookingService;
//
//    // ✅ FIX: Valid payment methods — reject anything else
//    private static final Set<String> VALID_PAYMENT_METHODS = Set.of(
//            "CASH", "CARD", "UPI", "NET_BANKING", "WALLET"
//    );
//
//    public PaymentController(PaymentService paymentService, BookingService bookingService) {
//        this.paymentService = paymentService;
//        this.bookingService = bookingService;
//    }
//
//    @PostMapping("/process/{bookingId}")
//    @PreAuthorize("hasAnyAuthority('ROLE_CUSTOMER', 'ROLE_ADMIN')")
//    public ResponseEntity<ApiResponse<Payment>> processPayment(
//            @PathVariable Long bookingId,
//            @RequestParam(defaultValue = "CASH") String paymentMethod,
//            Authentication auth) {
//        try {
//            // ✅ FIX: Validate payment method before processing
//            String method = paymentMethod.toUpperCase().trim();
//            if (!VALID_PAYMENT_METHODS.contains(method)) {
//                return ResponseEntity.badRequest().body(ApiResponse.error(
//                        "Invalid payment method '" + paymentMethod + "'. " +
//                        "Allowed values: " + VALID_PAYMENT_METHODS));
//            }
//
//            Long userId = (Long) auth.getCredentials();
//            Booking booking = bookingService.getBookingById(bookingId);
//
//            boolean isAdmin = auth.getAuthorities().stream()
//                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
//            if (!isAdmin && !booking.getCustomerId().equals(userId)) {
//                return ResponseEntity.status(403)
//                        .body(ApiResponse.error("Not authorized to pay for this booking"));
//            }
//
//            // ✅ FIX: Prevent duplicate payment — check if already paid
//            // (PaymentService should also handle this, but defence-in-depth)
//            Payment payment = paymentService.processPayment(bookingId, method);
//            return ResponseEntity.ok(ApiResponse.success("Payment processed successfully", payment));
//
//        } catch (RuntimeException e) {
//            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
//        }
//    }
//
//    @GetMapping
//    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
//    public ResponseEntity<ApiResponse<List<Payment>>> getAllPayments(
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "10") int size) {
//
//        // ✅ FIX: Cap size to max 100
//        size = Math.min(size, 100);
//        return ResponseEntity.ok(ApiResponse.success(paymentService.getAllPayments(page, size)));
//    }
//
//    @GetMapping("/booking/{bookingId}")
//    @PreAuthorize("isAuthenticated()")
//    public ResponseEntity<ApiResponse<Payment>> getPaymentByBooking(
//            @PathVariable Long bookingId,
//            Authentication auth) {
//        try {
//            Long userId = (Long) auth.getCredentials();
//            Booking booking = bookingService.getBookingById(bookingId);
//
//            boolean isAdminOrVendor = auth.getAuthorities().stream()
//                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
//                               || a.getAuthority().equals("ROLE_VENDOR"));
//            if (!isAdminOrVendor && !booking.getCustomerId().equals(userId)) {
//                return ResponseEntity.status(403)
//                        .body(ApiResponse.error("Not authorized to view this payment"));
//            }
//
//            return ResponseEntity.ok(ApiResponse.success(
//                    paymentService.getPaymentByBookingId(bookingId)));
//        } catch (RuntimeException e) {
//            return ResponseEntity.notFound().build();
//        }
//    }
//
//    @PostMapping("/{paymentId}/refund")
//    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
//    public ResponseEntity<ApiResponse<Payment>> refundPayment(@PathVariable Long paymentId) {
//        try {
//            return ResponseEntity.ok(ApiResponse.success("Payment refunded",
//                    paymentService.refundPayment(paymentId)));
//        } catch (RuntimeException e) {
//            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
//        }
//    }
//}
