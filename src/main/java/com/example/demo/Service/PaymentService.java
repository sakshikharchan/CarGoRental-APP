package com.example.demo.Service;

import com.example.demo.Model.Booking;
import com.example.demo.Model.Payment;
import com.example.demo.Repository.BookingRepository;
import com.example.demo.Repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;

    public Payment processPayment(Long bookingId, String paymentMethod) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found with id: " + bookingId));

        if ("CANCELLED".equals(booking.getStatus())) {
            throw new RuntimeException("Cannot process payment for a cancelled booking");
        }

        paymentRepository.findByBookingId(bookingId).ifPresent(p -> {
            if ("SUCCESS".equals(p.getPaymentStatus())) {
                throw new RuntimeException("Booking already paid");
            }
        });

        String transactionId = "TXN-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();

        Payment payment = Payment.builder()
                .bookingId(bookingId)
                .transactionId(transactionId)
                .amount(booking.getTotalAmount())
                .paymentMethod(paymentMethod.toUpperCase())
                .paymentStatus("SUCCESS")
                .paymentDate(LocalDateTime.now())
                .build();

        long id = paymentRepository.save(payment);
        bookingRepository.updateStatus(bookingId, "CONFIRMED");

        return paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment creation failed"));
    }

    public List<Payment> getAllPayments(int page, int size) {
        return paymentRepository.findAll(page, size);
    }

    public Payment getPaymentByBookingId(Long bookingId) {
        return paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new RuntimeException("Payment not found for booking id: " + bookingId));
    }

    public Payment refundPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found with id: " + paymentId));

        if (!"SUCCESS".equals(payment.getPaymentStatus())) {
            throw new RuntimeException("Only successful payments can be refunded");
        }

        paymentRepository.updateStatus(paymentId, "REFUNDED");
        bookingRepository.updateStatus(payment.getBookingId(), "CANCELLED");

        return paymentRepository.findById(paymentId).orElseThrow();
    }
}