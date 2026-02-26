//package com.example.demo.Controller;
//
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.web.bind.annotation.*;
//
//import com.example.demo.DTOs.ApiResponse;
//import com.example.demo.Model.Driver;
//import com.example.demo.Service.DriverService;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/drivers")
//public class DriverController {
//
//    private final DriverService driverService;
//
//    public DriverController(DriverService driverService) {
//        this.driverService = driverService;
//    }
//
//    @GetMapping
//    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_VENDOR')")
//    public ResponseEntity<ApiResponse<List<Driver>>> getAllDrivers(
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "10") int size) {
//        return ResponseEntity.ok(ApiResponse.success(driverService.getAllDrivers(page, size)));
//    }
//
//    @GetMapping("/available")
//    public ResponseEntity<ApiResponse<List<Driver>>> getAvailableDrivers(
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "10") int size) {
//        return ResponseEntity.ok(ApiResponse.success(driverService.getAvailableDrivers(page, size)));
//    }
//
//    @GetMapping("/{id}")
//    public ResponseEntity<ApiResponse<Driver>> getDriverById(@PathVariable Long id) {
//        try {
//            return ResponseEntity.ok(ApiResponse.success(driverService.getDriverById(id)));
//        } catch (RuntimeException e) {
//            return ResponseEntity.notFound().build();
//        }
//    }
//
//    @GetMapping("/vendor/{vendorId}")
//    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_VENDOR')")
//    public ResponseEntity<ApiResponse<List<Driver>>> getDriversByVendor(
//            @PathVariable Long vendorId,
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "10") int size) {
//        return ResponseEntity.ok(ApiResponse.success(
//                driverService.getDriversByVendor(vendorId, page, size)));
//    }
//
//    @PostMapping
//    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_VENDOR')")
//    public ResponseEntity<ApiResponse<Driver>> addDriver(@RequestBody Driver driver) {
//        try {
//            return ResponseEntity.ok(ApiResponse.success("Driver registered",
//                    driverService.addDriver(driver)));
//        } catch (RuntimeException e) {
//            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
//        }
//    }
//
//    @PatchMapping("/{id}/availability")
//    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_VENDOR', 'ROLE_DRIVER')")
//    public ResponseEntity<ApiResponse<Driver>> toggleAvailability(
//            @PathVariable Long id,
//            @RequestParam boolean available) {
//        try {
//            return ResponseEntity.ok(ApiResponse.success("Availability updated",
//                    driverService.toggleAvailability(id, available)));
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
import com.example.demo.Model.Driver;
import com.example.demo.Service.DriverService;

import java.util.List;

@RestController
@RequestMapping("/api/drivers")
public class DriverController {

    private final DriverService driverService;
    private final JdbcTemplate jdbcTemplate;

    public DriverController(DriverService driverService, JdbcTemplate jdbcTemplate) {
        this.driverService = driverService;
        this.jdbcTemplate = jdbcTemplate;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    // ✅ FIX BUG-012: resolve the vendor ID owned by this user
    private Long getVendorIdOrNull(Long userId) {
        List<Long> result = jdbcTemplate.queryForList(
                "SELECT id FROM vendors WHERE user_id = ?", Long.class, userId);
        return result.isEmpty() ? null : result.get(0);
    }

    // ── Endpoints ─────────────────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_VENDOR')")
    public ResponseEntity<ApiResponse<List<Driver>>> getAllDrivers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(driverService.getAllDrivers(page, size)));
    }

    @GetMapping("/available")
    public ResponseEntity<ApiResponse<List<Driver>>> getAvailableDrivers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(driverService.getAvailableDrivers(page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Driver>> getDriverById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ApiResponse.success(driverService.getDriverById(id)));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/vendor/{vendorId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_VENDOR')")
    public ResponseEntity<ApiResponse<List<Driver>>> getDriversByVendor(
            @PathVariable Long vendorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                driverService.getDriversByVendor(vendorId, page, size)));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_VENDOR')")
    public ResponseEntity<ApiResponse<Driver>> addDriver(@RequestBody Driver driver) {
        try {
            return ResponseEntity.ok(ApiResponse.success("Driver registered",
                    driverService.addDriver(driver)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ✅ FIX BUG-012: Vendor can only toggle availability of their OWN drivers
    @PatchMapping("/{id}/availability")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_VENDOR', 'ROLE_DRIVER')")
    public ResponseEntity<ApiResponse<Driver>> toggleAvailability(
            @PathVariable Long id,
            @RequestParam boolean available,
            Authentication auth) {
        try {
            boolean admin = isAdmin(auth);
            boolean isDriverRole = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_DRIVER"));

            if (!admin && !isDriverRole) {
                // Vendor: ensure the driver belongs to their vendor account
                Long userId = (Long) auth.getCredentials();
                Long vendorId = getVendorIdOrNull(userId);
                if (vendorId == null) {
                    return ResponseEntity.status(403)
                            .body(ApiResponse.error("Vendor profile not found"));
                }
                Driver driver = driverService.getDriverById(id);
                if (!vendorId.equals(driver.getVendorId())) {
                    return ResponseEntity.status(403)
                            .body(ApiResponse.error("Not authorized to update this driver"));
                }
            }

            return ResponseEntity.ok(ApiResponse.success("Availability updated",
                    driverService.toggleAvailability(id, available)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}