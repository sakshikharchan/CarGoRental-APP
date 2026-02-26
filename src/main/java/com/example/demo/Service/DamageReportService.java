package com.example.demo.Service;

import com.example.demo.Model.Booking;
import com.example.demo.Model.DamageReport;
import com.example.demo.Repository.BookingRepository;
import com.example.demo.Repository.DamageReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DamageReportService {

    private final DamageReportRepository damageReportRepository;
    private final BookingRepository bookingRepository;
    private final TrustScoreService trustScoreService;
    private final NotificationService notificationService;

    public DamageReport reportDamage(Long reportedByUserId, DamageReport report) {
        Booking booking = bookingRepository.findById(report.getBookingId())
                .orElseThrow(() -> new RuntimeException("Booking not found: " + report.getBookingId()));

        report.setReportedBy(reportedByUserId);
        long id = damageReportRepository.save(report);

        // If customer is at fault, penalise their trust score
        if (Boolean.TRUE.equals(report.getIsCustomerFault())) {
            String severityLabel = report.getSeverity() != null ? report.getSeverity() : "MINOR";
            String detail = "Damage reported on booking #" + booking.getBookingNumber()
                    + " – " + severityLabel + " – " + report.getDescription();

            trustScoreService.recordEvent(
                    booking.getCustomerId(),
                    "DAMAGE_REPORTED",
                    detail,
                    booking.getId());

            // Notify customer
            notificationService.send(
                    booking.getCustomerId(),
                    "⚠️ Damage Report Filed",
                    "A " + severityLabel.toLowerCase() + " damage report was filed against booking "
                    + booking.getBookingNumber() + ". This may affect your trust score.",
                    "DAMAGE", id, "DAMAGE_REPORT");
        }

        return damageReportRepository.findById(id).orElseThrow();
    }

    public List<DamageReport> getByBooking(Long bookingId) {
        return damageReportRepository.findByBookingId(bookingId);
    }

    public List<DamageReport> getAll(int page, int size) {
        return damageReportRepository.findAll(page, size);
    }

    public DamageReport resolveReport(Long reportId, String status) {
        damageReportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Damage report not found: " + reportId));
        damageReportRepository.updateStatus(reportId, status.toUpperCase());
        return damageReportRepository.findById(reportId).orElseThrow();
    }
}