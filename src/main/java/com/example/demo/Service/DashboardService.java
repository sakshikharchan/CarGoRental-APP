package com.example.demo.Service;
import com.example.demo.DTOs.DashboardStats;
import com.example.demo.Repository.BookingRepository;
import com.example.demo.Repository.CarRepository;
import com.example.demo.Repository.DriverRepository;
import com.example.demo.Repository.PaymentRepository;
import com.example.demo.Repository.UserRepository;
import com.example.demo.Repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {
    private final BookingRepository bookingRepository;
    private final CarRepository carRepository;
    private final UserRepository userRepository;
    private final VendorRepository vendorRepository;
    private final DriverRepository driverRepository;
    private final PaymentRepository paymentRepository;
    private final JdbcTemplate jdbcTemplate;

    public DashboardStats getStats() {
        try {
            long totalBookings = bookingRepository.countAll();
            long pending = bookingRepository.countByStatus("PENDING");
            long confirmed = bookingRepository.countByStatus("CONFIRMED");
            long active = bookingRepository.countByStatus("ACTIVE");
            long completed = bookingRepository.countByStatus("COMPLETED");
            long cancelled = bookingRepository.countByStatus("CANCELLED");
            BigDecimal revenue = paymentRepository.sumSuccessfulPayments();
            long totalCars = carRepository.countAll();
            long availableCars = carRepository.countAvailable();
            long totalVendors = vendorRepository.countAll();
            long totalDrivers = driverRepository.countAll();
            long pendingApprovals = vendorRepository.countPending();

            // Additional stats
            long totalCustomers = userRepository.countByRoleName("ROLE_CUSTOMER");
            long approvedVendors = safeCountQuery(
                    "SELECT COUNT(*) FROM vendors WHERE is_approved = true");
            long availableDrivers = safeCountQuery(
                    "SELECT COUNT(*) FROM drivers WHERE is_available = true");
            long totalReviews = safeCountQuery(
                    "SELECT COUNT(*) FROM reviews");
            Double avgRating = safeDoubleQuery(
                    "SELECT AVG(rating) FROM reviews");
            long totalPayments = safeCountQuery(
                    "SELECT COUNT(*) FROM payments");
            long pendingPayments = safeCountQuery(
                    "SELECT COUNT(*) FROM payments WHERE payment_status = 'PENDING'");
            long todayBookings = bookingRepository.countToday();
            BigDecimal monthlyRevenue = safeBigDecimalQuery(
                    "SELECT COALESCE(SUM(amount), 0) FROM payments WHERE payment_status = 'SUCCESS' AND MONTH(payment_date) = MONTH(CURDATE()) AND YEAR(payment_date) = YEAR(CURDATE())");
            BigDecimal todayRevenueVal = safeBigDecimalQuery(
                    "SELECT COALESCE(SUM(amount), 0) FROM payments WHERE payment_status = 'SUCCESS' AND DATE(payment_date) = CURDATE()");

            return DashboardStats.builder()
                    .totalBookings(totalBookings)
                    .pendingBookings(pending)
                    .confirmedBookings(confirmed)
                    .activeBookings(active)
                    .completedBookings(completed)
                    .cancelledBookings(cancelled)
                    .totalRevenue(revenue)
                    .monthlyRevenue(monthlyRevenue)
                    .totalCars(totalCars)
                    .availableCars(availableCars)
                    .totalCustomers(totalCustomers)
                    .totalVendors(totalVendors)
                    .approvedVendors(approvedVendors)
                    .totalDrivers(totalDrivers)
                    .availableDrivers(availableDrivers)
                    .pendingVendorApprovals(pendingApprovals)
                    .totalReviews(totalReviews)
                    .avgRating(avgRating)
                    .totalPayments(totalPayments)
                    .pendingPayments(pendingPayments)
                    .todayBookings(todayBookings)
                    .todayRevenue(todayRevenueVal != null ? todayRevenueVal.longValue() : 0L)
                    .build();

        } catch (Exception e) {
            log.error("Dashboard stats error: {}", e.getMessage(), e);
            throw e;
        }
    }

    private long safeCountQuery(String sql) {
        try {
            Long count = jdbcTemplate.queryForObject(sql, Long.class);
            return count != null ? count : 0;
        } catch (Exception e) {
            log.warn("Dashboard query failed: {} — {}", sql, e.getMessage());
            return 0;
        }
    }

    private Double safeDoubleQuery(String sql) {
        try {
            return jdbcTemplate.queryForObject(sql, Double.class);
        } catch (Exception e) {
            log.warn("Dashboard query failed: {} — {}", sql, e.getMessage());
            return null;
        }
    }

    private BigDecimal safeBigDecimalQuery(String sql) {
        try {
            BigDecimal val = jdbcTemplate.queryForObject(sql, BigDecimal.class);
            return val != null ? val : BigDecimal.ZERO;
        } catch (Exception e) {
            log.warn("Dashboard query failed: {} — {}", sql, e.getMessage());
            return BigDecimal.ZERO;
        }
    }
}
