//package com.example.demo.Controller;
//
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.security.core.Authentication;
//import org.springframework.web.bind.annotation.*;
//
//import com.example.demo.DTOs.ApiResponse;
//import com.example.demo.Model.Vendor;
//import com.example.demo.Service.VendorService;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/vendors")
//public class VendorController {
//
//    private final VendorService vendorService;
//
//    public VendorController(VendorService vendorService) {
//        this.vendorService = vendorService;
//    }
//
//    @GetMapping
//    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
//    public ResponseEntity<ApiResponse<List<Vendor>>> getAllVendors(
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "10") int size) {
//        return ResponseEntity.ok(ApiResponse.success(vendorService.getAllVendors(page, size)));
//    }
//
//    @GetMapping("/{id}")
//    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_VENDOR')")
//    public ResponseEntity<ApiResponse<Vendor>> getVendorById(@PathVariable Long id) {
//        try {
//            return ResponseEntity.ok(ApiResponse.success(vendorService.getVendorById(id)));
//        } catch (RuntimeException e) {
//            return ResponseEntity.notFound().build();
//        }
//    }
//
//    @GetMapping("/pending")
//    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
//    public ResponseEntity<ApiResponse<List<Vendor>>> getPendingVendors(
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "10") int size) {
//        return ResponseEntity.ok(ApiResponse.success(vendorService.getPendingVendors(page, size)));
//    }
//
//    @PostMapping
//    @PreAuthorize("isAuthenticated()")
//    public ResponseEntity<ApiResponse<Vendor>> registerVendor(
//            @RequestBody Vendor vendor,
//            Authentication auth) {
//        try {
//            // Enforce userId from authenticated user — prevent linking to arbitrary user
//            Long userId = (Long) auth.getCredentials();
//            vendor.setUserId(userId);
//            return ResponseEntity.ok(ApiResponse.success("Vendor registered",
//                    vendorService.registerVendor(vendor)));
//        } catch (RuntimeException e) {
//            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
//        }
//    }
//
//    @PutMapping("/{id}")
//    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_VENDOR')")
//    public ResponseEntity<ApiResponse<Vendor>> updateVendor(
//            @PathVariable Long id,
//            @RequestBody Vendor vendor) {
//        try {
//            return ResponseEntity.ok(ApiResponse.success("Updated",
//                    vendorService.updateVendor(id, vendor)));
//        } catch (RuntimeException e) {
//            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
//        }
//    }
//
//    @PatchMapping("/{id}/approve")
//    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
//    public ResponseEntity<ApiResponse<Vendor>> approveVendor(
//            @PathVariable Long id,
//            @RequestParam boolean approved) {
//        try {
//            return ResponseEntity.ok(ApiResponse.success(
//                    approved ? "Vendor approved" : "Vendor rejected",
//                    vendorService.approveVendor(id, approved)));
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
import com.example.demo.Model.Vendor;
import com.example.demo.Service.VendorService;

import java.util.List;

@RestController
@RequestMapping("/api/vendors")
public class VendorController {

    private final VendorService vendorService;
    private final JdbcTemplate jdbcTemplate;

    public VendorController(VendorService vendorService, JdbcTemplate jdbcTemplate) {
        this.vendorService = vendorService;
        this.jdbcTemplate = jdbcTemplate;
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    // ✅ FIX BUG-013: look up the vendor record owned by this user
    private Long getVendorIdForUser(Long userId) {
        List<Long> result = jdbcTemplate.queryForList(
                "SELECT id FROM vendors WHERE user_id = ?", Long.class, userId);
        return result.isEmpty() ? null : result.get(0);
    }

    // ── Endpoints ─────────────────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<List<Vendor>>> getAllVendors(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(vendorService.getAllVendors(page, size)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_VENDOR')")
    public ResponseEntity<ApiResponse<Vendor>> getVendorById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ApiResponse.success(vendorService.getVendorById(id)));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<List<Vendor>>> getPendingVendors(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(vendorService.getPendingVendors(page, size)));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Vendor>> registerVendor(
            @RequestBody Vendor vendor,
            Authentication auth) {
        try {
            Long userId = (Long) auth.getCredentials();
            vendor.setUserId(userId); // Enforce: always link to authenticated user
            return ResponseEntity.ok(ApiResponse.success("Vendor registered",
                    vendorService.registerVendor(vendor)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ✅ FIX BUG-013: Vendor can only update THEIR OWN profile; admin can update any
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_VENDOR')")
    public ResponseEntity<ApiResponse<Vendor>> updateVendor(
            @PathVariable Long id,
            @RequestBody Vendor vendor,
            Authentication auth) {
        try {
            if (!isAdmin(auth)) {
                Long userId = (Long) auth.getCredentials();
                Long ownedVendorId = getVendorIdForUser(userId);
                if (ownedVendorId == null || !ownedVendorId.equals(id)) {
                    return ResponseEntity.status(403)
                            .body(ApiResponse.error("Not authorized to update this vendor profile"));
                }
            }
            return ResponseEntity.ok(ApiResponse.success("Updated",
                    vendorService.updateVendor(id, vendor)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Vendor>> approveVendor(
            @PathVariable Long id,
            @RequestParam boolean approved) {
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    approved ? "Vendor approved" : "Vendor rejected",
                    vendorService.approveVendor(id, approved)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}