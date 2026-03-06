package com.example.demo.Service;

import com.example.demo.DTOs.RazorpayOrderResponse;
import com.example.demo.DTOs.RazorpayVerifyRequest;
import com.example.demo.Model.Booking;
import com.example.demo.Model.Payment;
import com.example.demo.Repository.BookingRepository;
import com.example.demo.Repository.PaymentRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class RazorpayService {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    /**
     * STEP 1: Create a Razorpay order for a booking.
     * Called when user clicks "Pay with Razorpay".
     */
    public RazorpayOrderResponse createOrder(Long bookingId) throws RazorpayException {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found: " + bookingId));

        if ("CANCELLED".equals(booking.getStatus())) {
            throw new RuntimeException("Cannot pay for a cancelled booking");
        }

        // Check if already paid
        paymentRepository.findByBookingId(bookingId).ifPresent(p -> {
            if ("SUCCESS".equals(p.getPaymentStatus())) {
                throw new RuntimeException("Booking is already paid");
            }
        });

        // Amount must be in paise (1 INR = 100 paise)
        BigDecimal amountInRupees = booking.getTotalAmount();
        long amountInPaise = amountInRupees.multiply(BigDecimal.valueOf(100)).longValue();

        // Create order via Razorpay API
        RazorpayClient client = new RazorpayClient(keyId, keySecret);

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amountInPaise);
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", "booking_" + bookingId);
        orderRequest.put("payment_capture", 1); // auto-capture

        Order order = client.orders.create(orderRequest);
        String razorpayOrderId = order.get("id");

        // Save a PENDING payment record so we can track
        Payment pending = Payment.builder()
                .bookingId(bookingId)
                .transactionId(razorpayOrderId)   // store order id as transaction id temporarily
                .amount(amountInRupees)
                .paymentMethod("RAZORPAY")
                .paymentStatus("PENDING")
                .paymentDate(LocalDateTime.now())
                .build();

        // Only save if no pending record yet
        paymentRepository.findByBookingId(bookingId).ifPresentOrElse(
            existing -> paymentRepository.updateTransactionId(existing.getId(), razorpayOrderId),
            () -> paymentRepository.save(pending)
        );

        return new RazorpayOrderResponse(
                razorpayOrderId,
                amountInRupees,
                amountInPaise,
                "INR",
                bookingId,
                booking.getBookingNumber(),
                keyId   // safe to send public key to frontend
        );
    }

    /**
     * STEP 2: Verify signature after user completes payment.
     * Razorpay sends razorpayOrderId + razorpayPaymentId + razorpaySignature.
     * We verify HMAC-SHA256 signature to confirm payment is genuine.
     */
    public Payment verifyAndCapture(RazorpayVerifyRequest req) {
        // Verify signature: HMAC-SHA256(orderId + "|" + paymentId, secret)
        String payload = req.getRazorpayOrderId() + "|" + req.getRazorpayPaymentId();
        String expectedSignature = hmacSHA256(payload, keySecret);

        if (!expectedSignature.equals(req.getRazorpaySignature())) {
            throw new RuntimeException("Payment verification failed: Invalid signature");
        }

        // Signature valid — mark payment as SUCCESS
        Long bookingId = req.getBookingId();

        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new RuntimeException("Payment record not found for booking: " + bookingId));

        // Update payment record with actual Razorpay payment ID
        paymentRepository.updateTransactionIdAndStatus(
                payment.getId(),
                req.getRazorpayPaymentId(),   // actual payment ID (pay_xxx)
                "SUCCESS"
        );

        // Confirm the booking
        bookingRepository.updateStatus(bookingId, "CONFIRMED");

        return paymentRepository.findById(payment.getId())
                .orElseThrow(() -> new RuntimeException("Payment not found after update"));
    }

    // HMAC-SHA256 helper
    private String hmacSHA256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("HMAC-SHA256 computation failed", e);
        }
    }
}