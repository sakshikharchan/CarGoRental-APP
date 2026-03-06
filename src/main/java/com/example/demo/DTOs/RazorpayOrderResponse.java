package com.example.demo.DTOs;

import java.math.BigDecimal;

public class RazorpayOrderResponse {
    private String razorpayOrderId;
    private BigDecimal amount;       // in rupees (for display)
    private long amountInPaise;      // in paise (for Razorpay SDK)
    private String currency;
    private Long bookingId;
    private String bookingNumber;
    private String keyId;            // Razorpay public key (safe to send to frontend)

    // Constructors
    public RazorpayOrderResponse() {}

    public RazorpayOrderResponse(String razorpayOrderId, BigDecimal amount, long amountInPaise,
                                  String currency, Long bookingId, String bookingNumber, String keyId) {
        this.razorpayOrderId = razorpayOrderId;
        this.amount = amount;
        this.amountInPaise = amountInPaise;
        this.currency = currency;
        this.bookingId = bookingId;
        this.bookingNumber = bookingNumber;
        this.keyId = keyId;
    }

    // Getters & Setters
    public String getRazorpayOrderId() { return razorpayOrderId; }
    public void setRazorpayOrderId(String razorpayOrderId) { this.razorpayOrderId = razorpayOrderId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public long getAmountInPaise() { return amountInPaise; }
    public void setAmountInPaise(long amountInPaise) { this.amountInPaise = amountInPaise; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }
    public String getBookingNumber() { return bookingNumber; }
    public void setBookingNumber(String bookingNumber) { this.bookingNumber = bookingNumber; }
    public String getKeyId() { return keyId; }
    public void setKeyId(String keyId) { this.keyId = keyId; }
}