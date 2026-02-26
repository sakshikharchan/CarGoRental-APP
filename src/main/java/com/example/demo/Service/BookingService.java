//package com.example.demo.Service;
//
//import com.example.demo.DTOs.BookingRequest;
//import com.example.demo.DTOs.CouponValidateResponse;
//import com.example.demo.Model.Booking;
//import com.example.demo.Model.Car;
//import com.example.demo.Repository.BookingRepository;
//import com.example.demo.Repository.CarRepository;
//import com.example.demo.Repository.CouponRepository;
//import com.example.demo.Repository.DriverRepository;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//
//import java.math.BigDecimal;
//import java.time.temporal.ChronoUnit;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//import java.util.UUID;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class BookingService {
//
//    private final BookingRepository bookingRepository;
//    private final CarRepository carRepository;
//    private final DriverRepository driverRepository;
//    private final CouponRepository couponRepository;
//    private final NotificationService notificationService;
//    private final CouponService couponService;
//    private final TrustScoreService trustScoreService;
//
//    public Booking createBooking(Long customerId, BookingRequest request) {
//
//        // ✅ FIX: Log exactly what arrives so we can see the date values
//        log.info("=== createBooking called ===");
//        log.info("customerId={}", customerId);
//        log.info("carId={}", request.getCarId());
//        log.info("pickupDate={}", request.getPickupDate());
//        log.info("returnDate={}", request.getReturnDate());
//        log.info("pickupLocation={}", request.getPickupLocation());
//        log.info("withDriver={}", request.getWithDriver());
//
//        // ✅ FIX: Validate dates BEFORE using them - give a clear error message
//        //    instead of a cryptic NPE from ChronoUnit.DAYS.between()
//        if (request.getPickupDate() == null) {
//            throw new RuntimeException("Pickup date is required and could not be parsed. " +
//                "Please send date in format: yyyy-MM-dd (e.g. 2026-02-21)");
//        }
//        if (request.getReturnDate() == null) {
//            throw new RuntimeException("Return date is required and could not be parsed. " +
//                "Please send date in format: yyyy-MM-dd (e.g. 2026-03-21)");
//        }
//
//        Car car = carRepository.findById(request.getCarId())
//                .orElseThrow(() -> new RuntimeException("Car not found"));
//
//        if (!Boolean.TRUE.equals(car.getAvailable())) {
//            throw new RuntimeException("Car is not available for booking");
//        }
//
//        if (bookingRepository.hasActiveBookingForCar(request.getCarId())) {
//            throw new RuntimeException("Car already has an active booking for the selected dates");
//        }
//
//        // ✅ Now safe to call - dates are guaranteed non-null
//        long totalDays = ChronoUnit.DAYS.between(request.getPickupDate(), request.getReturnDate());
//        if (totalDays <= 0) {
//            throw new RuntimeException("Return date must be after pickup date");
//        }
//
//        BigDecimal driverCharge = BigDecimal.ZERO;
//        Long driverId = null;
//
//        if (Boolean.TRUE.equals(request.getWithDriver()) && request.getDriverId() != null) {
//            driverRepository.findById(request.getDriverId())
//                    .orElseThrow(() -> new RuntimeException("Driver not found"));
//            driverId = request.getDriverId();
//            driverCharge = BigDecimal.valueOf(500).multiply(BigDecimal.valueOf(totalDays));
//        }
//
//        BigDecimal dailyRate = car.getDailyRate();
//        BigDecimal baseAmount = dailyRate.multiply(BigDecimal.valueOf(totalDays)).add(driverCharge);
//        BigDecimal discountAmount = BigDecimal.ZERO;
//        String couponCode = null;
//
//        if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
//            CouponValidateResponse couponResult = couponService.validateCoupon(
//                    request.getCouponCode(), baseAmount);
//            if (couponResult.isValid()) {
//                discountAmount = couponResult.getDiscountAmount();
//                couponCode = request.getCouponCode().toUpperCase();
//                couponRepository.incrementUsage(couponCode);
//            }
//        }
//
//        BigDecimal totalAmount = baseAmount.subtract(discountAmount);
//        if (totalAmount.compareTo(BigDecimal.ZERO) < 0) totalAmount = BigDecimal.ZERO;
//
//        String bookingNumber = "BK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
//
//        Booking booking = Booking.builder()
//                .bookingNumber(bookingNumber)
//                .customerId(customerId)
//                .carId(request.getCarId())
//                .vendorId(car.getVendorId())
//                .driverId(driverId)
//                .pickupLocation(request.getPickupLocation())
//                .dropoffLocation(request.getDropoffLocation())
//                .pickupDate(request.getPickupDate())
//                .returnDate(request.getReturnDate())
//                .totalDays((int) totalDays)
//                .dailyRate(dailyRate)
//                .totalAmount(totalAmount)
//                .withDriver(Boolean.TRUE.equals(request.getWithDriver()))
//                .driverCharge(driverCharge)
//                .discountAmount(discountAmount)
//                .couponCode(couponCode)
//                .status("PENDING")
//                .notes(request.getNotes())
//                .build();
//
//        long id = bookingRepository.save(booking);
//        carRepository.updateAvailability(request.getCarId(), false);
//
//        notificationService.send(customerId,
//                "Booking Confirmed! 🎉",
//                "Your booking " + bookingNumber + " has been created. Total: ₹" + totalAmount,
//                "BOOKING", id, "BOOKING");
//
//        return bookingRepository.findById(id)
//                .orElseThrow(() -> new RuntimeException("Booking creation failed"));
//    }
//
//    public List<Booking> getAllBookings(int page, int size) {
//        return bookingRepository.findAll(page, size);
//    }
//
//    public List<Booking> getBookingsByStatus(String status, int page, int size) {
//        return bookingRepository.findByStatus(status.toUpperCase(), page, size);
//    }
//
//    public List<Booking> getBookingsByCustomer(Long customerId, int page, int size) {
//        return bookingRepository.findByCustomerId(customerId, page, size);
//    }
//
//    public List<Booking> getBookingsByVendor(Long vendorId, int page, int size) {
//        return bookingRepository.findByVendorId(vendorId, page, size);
//    }
//
//    public Booking getBookingById(Long id) {
//        return bookingRepository.findById(id)
//                .orElseThrow(() -> new RuntimeException("Booking not found with id: " + id));
//    }
//
//    public Booking getByBookingNumber(String bookingNumber) {
//        return bookingRepository.findByBookingNumber(bookingNumber)
//                .orElseThrow(() -> new RuntimeException("Booking not found: " + bookingNumber));
//    }
//
//    public Booking updateStatus(Long id, String status) {
//        Booking booking = bookingRepository.findById(id)
//                .orElseThrow(() -> new RuntimeException("Booking not found with id: " + id));
//
//        validateStatusTransition(booking.getStatus(), status);
//        bookingRepository.updateStatus(id, status);
//
//        if ("COMPLETED".equals(status)) {
//            carRepository.updateAvailability(booking.getCarId(), true);
//            if (booking.getDriverId() != null) {
//                driverRepository.updateAvailability(booking.getDriverId(), true);
//            }
//            trustScoreService.recordEvent(
//                    booking.getCustomerId(),
//                    "BOOKING_COMPLETED",
//                    "Booking " + booking.getBookingNumber() + " completed successfully.",
//                    id);
//        }
//
//        if ("CANCELLED".equals(status)) {
//            carRepository.updateAvailability(booking.getCarId(), true);
//            if (booking.getDriverId() != null) {
//                driverRepository.updateAvailability(booking.getDriverId(), true);
//            }
//            trustScoreService.recordEvent(
//                    booking.getCustomerId(),
//                    "BOOKING_CANCELLED",
//                    "Booking " + booking.getBookingNumber() + " was cancelled.",
//                    id);
//        }
//
//        notificationService.send(booking.getCustomerId(),
//                "Booking Status Updated",
//                "Your booking " + booking.getBookingNumber() + " status changed to " + status,
//                "BOOKING", id, "BOOKING");
//
//        return bookingRepository.findById(id).orElseThrow();
//    }
//
//    // Valid state transitions: currentStatus → set of allowed next statuses
//    private static final Map<String, Set<String>> VALID_TRANSITIONS = Map.of(
//            "PENDING",    Set.of("CONFIRMED", "CANCELLED"),
//            "CONFIRMED",  Set.of("ACTIVE", "CANCELLED"),
//            "ACTIVE",     Set.of("COMPLETED", "CANCELLED"),
//            "COMPLETED",  Set.of(),
//            "CANCELLED",  Set.of()
//    );
//
//    private void validateStatusTransition(String currentStatus, String newStatus) {
//        List<String> validStatuses = List.of("PENDING", "CONFIRMED", "ACTIVE", "COMPLETED", "CANCELLED");
//        if (!validStatuses.contains(newStatus.toUpperCase())) {
//            throw new RuntimeException("Invalid status: " + newStatus);
//        }
//        Set<String> allowed = VALID_TRANSITIONS.getOrDefault(currentStatus, Set.of());
//        if (!allowed.contains(newStatus.toUpperCase())) {
//            throw new RuntimeException(
//                    "Cannot transition from " + currentStatus + " to " + newStatus);
//        }
//    }
//}
package com.example.demo.Service;

import com.example.demo.DTOs.BookingRequest;
import com.example.demo.DTOs.CouponValidateResponse;
import com.example.demo.Model.Booking;
import com.example.demo.Model.Car;
import com.example.demo.Repository.BookingRepository;
import com.example.demo.Repository.CarRepository;
import com.example.demo.Repository.CouponRepository;
import com.example.demo.Repository.DriverRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final CarRepository carRepository;
    private final DriverRepository driverRepository;
    private final CouponRepository couponRepository;
    private final NotificationService notificationService;
    private final CouponService couponService;
    private final TrustScoreService trustScoreService;

    public Booking createBooking(Long customerId, BookingRequest request) {

        log.info("=== createBooking called ===");
        log.info("customerId={}", customerId);
        log.info("carId={}", request.getCarId());
        log.info("pickupDate={}", request.getPickupDate());
        log.info("returnDate={}", request.getReturnDate());
        log.info("pickupLocation={}", request.getPickupLocation());
        log.info("withDriver={}", request.getWithDriver());

        if (request.getPickupDate() == null) {
            throw new RuntimeException("Pickup date is required and could not be parsed. " +
                "Please send date in format: yyyy-MM-dd (e.g. 2026-02-21)");
        }
        if (request.getReturnDate() == null) {
            throw new RuntimeException("Return date is required and could not be parsed. " +
                "Please send date in format: yyyy-MM-dd (e.g. 2026-03-21)");
        }

        Car car = carRepository.findById(request.getCarId())
                .orElseThrow(() -> new RuntimeException("Car not found"));

        // ✅ FIXED: was car.getAvailable() → now car.getIsAvailable()
        if (!Boolean.TRUE.equals(car.getIsAvailable())) {
            throw new RuntimeException("Car is not available for booking");
        }

        if (bookingRepository.hasActiveBookingForCar(request.getCarId())) {
            throw new RuntimeException("Car already has an active booking for the selected dates");
        }

        long totalDays = ChronoUnit.DAYS.between(request.getPickupDate(), request.getReturnDate());
        if (totalDays <= 0) {
            throw new RuntimeException("Return date must be after pickup date");
        }

        BigDecimal driverCharge = BigDecimal.ZERO;
        Long driverId = null;

        if (Boolean.TRUE.equals(request.getWithDriver()) && request.getDriverId() != null) {
            driverRepository.findById(request.getDriverId())
                    .orElseThrow(() -> new RuntimeException("Driver not found"));
            driverId = request.getDriverId();
            driverCharge = BigDecimal.valueOf(500).multiply(BigDecimal.valueOf(totalDays));
        }

        BigDecimal dailyRate = car.getDailyRate();
        BigDecimal baseAmount = dailyRate.multiply(BigDecimal.valueOf(totalDays)).add(driverCharge);
        BigDecimal discountAmount = BigDecimal.ZERO;
        String couponCode = null;

        if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
            CouponValidateResponse couponResult = couponService.validateCoupon(
                    request.getCouponCode(), baseAmount);
            if (couponResult.isValid()) {
                discountAmount = couponResult.getDiscountAmount();
                couponCode = request.getCouponCode().toUpperCase();
                couponRepository.incrementUsage(couponCode);
            }
        }

        BigDecimal totalAmount = baseAmount.subtract(discountAmount);
        if (totalAmount.compareTo(BigDecimal.ZERO) < 0) totalAmount = BigDecimal.ZERO;

        String bookingNumber = "BK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Booking booking = Booking.builder()
                .bookingNumber(bookingNumber)
                .customerId(customerId)
                .carId(request.getCarId())
                .vendorId(car.getVendorId())
                .driverId(driverId)
                .pickupLocation(request.getPickupLocation())
                .dropoffLocation(request.getDropoffLocation())
                .pickupDate(request.getPickupDate())
                .returnDate(request.getReturnDate())
                .totalDays((int) totalDays)
                .dailyRate(dailyRate)
                .totalAmount(totalAmount)
                .withDriver(Boolean.TRUE.equals(request.getWithDriver()))
                .driverCharge(driverCharge)
                .discountAmount(discountAmount)
                .couponCode(couponCode)
                .status("PENDING")
                .notes(request.getNotes())
                .build();

        long id = bookingRepository.save(booking);
        carRepository.updateAvailability(request.getCarId(), false);

        notificationService.send(customerId,
                "Booking Confirmed! 🎉",
                "Your booking " + bookingNumber + " has been created. Total: ₹" + totalAmount,
                "BOOKING", id, "BOOKING");

        return bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking creation failed"));
    }

    public List<Booking> getAllBookings(int page, int size) {
        return bookingRepository.findAll(page, size);
    }

    public List<Booking> getBookingsByStatus(String status, int page, int size) {
        return bookingRepository.findByStatus(status.toUpperCase(), page, size);
    }

    public List<Booking> getBookingsByCustomer(Long customerId, int page, int size) {
        return bookingRepository.findByCustomerId(customerId, page, size);
    }

    public List<Booking> getBookingsByVendor(Long vendorId, int page, int size) {
        return bookingRepository.findByVendorId(vendorId, page, size);
    }

    public Booking getBookingById(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found with id: " + id));
    }

    public Booking getByBookingNumber(String bookingNumber) {
        return bookingRepository.findByBookingNumber(bookingNumber)
                .orElseThrow(() -> new RuntimeException("Booking not found: " + bookingNumber));
    }

    public Booking updateStatus(Long id, String status) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found with id: " + id));

        validateStatusTransition(booking.getStatus(), status);
        bookingRepository.updateStatus(id, status);

        if ("COMPLETED".equals(status)) {
            carRepository.updateAvailability(booking.getCarId(), true);
            if (booking.getDriverId() != null) {
                driverRepository.updateAvailability(booking.getDriverId(), true);
            }
            trustScoreService.recordEvent(
                    booking.getCustomerId(),
                    "BOOKING_COMPLETED",
                    "Booking " + booking.getBookingNumber() + " completed successfully.",
                    id);
        }

        if ("CANCELLED".equals(status)) {
            carRepository.updateAvailability(booking.getCarId(), true);
            if (booking.getDriverId() != null) {
                driverRepository.updateAvailability(booking.getDriverId(), true);
            }
            trustScoreService.recordEvent(
                    booking.getCustomerId(),
                    "BOOKING_CANCELLED",
                    "Booking " + booking.getBookingNumber() + " was cancelled.",
                    id);
        }

        notificationService.send(booking.getCustomerId(),
                "Booking Status Updated",
                "Your booking " + booking.getBookingNumber() + " status changed to " + status,
                "BOOKING", id, "BOOKING");

        return bookingRepository.findById(id).orElseThrow();
    }

    private static final Map<String, Set<String>> VALID_TRANSITIONS = Map.of(
            "PENDING",    Set.of("CONFIRMED", "CANCELLED"),
            "CONFIRMED",  Set.of("ACTIVE", "CANCELLED"),
            "ACTIVE",     Set.of("COMPLETED", "CANCELLED"),
            "COMPLETED",  Set.of(),
            "CANCELLED",  Set.of()
    );

    private void validateStatusTransition(String currentStatus, String newStatus) {
        List<String> validStatuses = List.of("PENDING", "CONFIRMED", "ACTIVE", "COMPLETED", "CANCELLED");
        if (!validStatuses.contains(newStatus.toUpperCase())) {
            throw new RuntimeException("Invalid status: " + newStatus);
        }
        Set<String> allowed = VALID_TRANSITIONS.getOrDefault(currentStatus, Set.of());
        if (!allowed.contains(newStatus.toUpperCase())) {
            throw new RuntimeException(
                    "Cannot transition from " + currentStatus + " to " + newStatus);
        }
    }
}