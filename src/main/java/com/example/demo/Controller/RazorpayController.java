package com.example.demo.Controller;

import com.example.demo.DTOs.ApiResponse;
import com.example.demo.DTOs.RazorpayOrderRequest;
import com.example.demo.DTOs.RazorpayOrderResponse;
import com.example.demo.DTOs.RazorpayVerifyRequest;
import com.example.demo.Model.Booking;
import com.example.demo.Model.Payment;
import com.example.demo.Service.BookingService;
import com.example.demo.Service.RazorpayService;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/razorpay")
@RequiredArgsConstructor
public class RazorpayController {

    private final RazorpayService razorpayService;
    private final BookingService bookingService;

    /**
     * POST /api/razorpay/create-order
     * Called when user clicks "Pay Now" — creates a Razorpay order and returns orderId + key
     */
    @PostMapping("/create-order")
    @PreAuthorize("hasAnyAuthority('ROLE_CUSTOMER', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<RazorpayOrderResponse>> createOrder(
            @RequestBody RazorpayOrderRequest request,
            Authentication auth) {
        try {
            Long userId = (Long) auth.getCredentials();
            Booking booking = bookingService.getBookingById(request.getBookingId());

            boolean isAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

            if (!isAdmin && !booking.getCustomerId().equals(userId)) {
                return ResponseEntity.status(403)
                        .body(ApiResponse.error("Not authorized to pay for this booking"));
            }

            RazorpayOrderResponse orderResponse = razorpayService.createOrder(request.getBookingId());
            return ResponseEntity.ok(ApiResponse.success("Order created", orderResponse));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (RazorpayException e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Razorpay error: " + e.getMessage()));
        }
    }

    /**
     * POST /api/razorpay/verify
     * Called after user completes payment — verifies signature and marks payment SUCCESS
     */
    @PostMapping("/verify")
    @PreAuthorize("hasAnyAuthority('ROLE_CUSTOMER', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Payment>> verifyPayment(
            @RequestBody RazorpayVerifyRequest request,
            Authentication auth) {
        try {
            Long userId = (Long) auth.getCredentials();
            Booking booking = bookingService.getBookingById(request.getBookingId());

            boolean isAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

            if (!isAdmin && !booking.getCustomerId().equals(userId)) {
                return ResponseEntity.status(403)
                        .body(ApiResponse.error("Not authorized"));
            }

            Payment payment = razorpayService.verifyAndCapture(request);
            return ResponseEntity.ok(ApiResponse.success("Payment verified successfully!", payment));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}