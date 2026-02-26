//package com.example.demo.Controller;

//
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.web.bind.annotation.*;
//
//import com.example.demo.DTOs.ApiResponse;
//import com.example.demo.Model.Car;
//import com.example.demo.Service.CarService;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/cars")
//public class CarController {
//
//    private final CarService carService;
//
//    public CarController(CarService carService) {
//        this.carService = carService;
//    }
//
//    @GetMapping
//    public ResponseEntity<ApiResponse<List<Car>>> getAllCars(
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "10") int size) {
//        return ResponseEntity.ok(ApiResponse.success(carService.getAllCars(page, size)));
//    }
//
//    @GetMapping("/available")
//    public ResponseEntity<ApiResponse<List<Car>>> getAvailableCars(
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "10") int size) {
//        return ResponseEntity.ok(ApiResponse.success(carService.getAvailableCars(page, size)));
//    }
//
//    @GetMapping("/{id}")
//    public ResponseEntity<ApiResponse<Car>> getCarById(@PathVariable Long id) {
//        try {
//            return ResponseEntity.ok(ApiResponse.success(carService.getCarById(id)));
//        } catch (RuntimeException e) {
//            return ResponseEntity.notFound().build();
//        }
//    }
//
//    @GetMapping("/search")
//    public ResponseEntity<ApiResponse<List<Car>>> searchCars(
//            @RequestParam(required = false) String brand,
//            @RequestParam(required = false) String fuelType,
//            @RequestParam(required = false) String transmission,
//            @RequestParam(required = false) Long categoryId,
//            @RequestParam(required = false) Boolean isAvailable,
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "10") int size) {
//        return ResponseEntity.ok(ApiResponse.success(
//                carService.searchCars(brand, fuelType, transmission, categoryId, isAvailable, page, size)));
//    }
//
//    @GetMapping("/vendor/{vendorId}")
//    @PreAuthorize("hasAnyAuthority('ROLE_VENDOR', 'ROLE_ADMIN')")
//    public ResponseEntity<ApiResponse<List<Car>>> getCarsByVendor(
//            @PathVariable Long vendorId,
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "10") int size) {
//        return ResponseEntity.ok(ApiResponse.success(carService.getCarsByVendor(vendorId, page, size)));
//    }
//
//    @PostMapping
//    @PreAuthorize("hasAnyAuthority('ROLE_VENDOR', 'ROLE_ADMIN')")
//    public ResponseEntity<ApiResponse<Car>> addCar(@RequestBody Car car) {
//        try {
//            return ResponseEntity.ok(ApiResponse.success("Car added successfully", carService.addCar(car)));
//        } catch (RuntimeException e) {
//            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
//        }
//    }
//
//    @PutMapping("/{id}")
//    @PreAuthorize("hasAnyAuthority('ROLE_VENDOR', 'ROLE_ADMIN')")
//    public ResponseEntity<ApiResponse<Car>> updateCar(@PathVariable Long id, @RequestBody Car car) {
//        try {
//            return ResponseEntity.ok(ApiResponse.success("Car updated", carService.updateCar(id, car)));
//        } catch (RuntimeException e) {
//            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
//        }
//    }
//
//    @DeleteMapping("/{id}")
//    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
//    public ResponseEntity<ApiResponse<Void>> deleteCar(@PathVariable Long id) {
//        try {
//            carService.deleteCar(id);
//            return ResponseEntity.ok(ApiResponse.success("Car deleted", null));
//        } catch (RuntimeException e) {
//            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
//        }
//    }
//
//    @PatchMapping("/{id}/availability")
//    @PreAuthorize("hasAnyAuthority('ROLE_VENDOR', 'ROLE_ADMIN')")
//    public ResponseEntity<ApiResponse<Car>> toggleAvailability(
//            @PathVariable Long id,
//            @RequestParam boolean available) {
//        try {
//            return ResponseEntity.ok(ApiResponse.success("Availability updated",
//                    carService.toggleAvailability(id, available)));
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
import com.example.demo.Model.Car;
import com.example.demo.Service.CarService;

import java.util.List;

@RestController
@RequestMapping("/api/cars")
public class CarController {

    private final CarService carService;
    private final JdbcTemplate jdbcTemplate;

    public CarController(CarService carService, JdbcTemplate jdbcTemplate) {
        this.carService = carService;
        this.jdbcTemplate = jdbcTemplate;
    }

    // ── Helper: resolve vendors.id from users.id ──────────────────────────────
    // ✅ FIX: Used for ownership checks — vendors.id != users.id
    private Long getVendorIdOrNull(Long userId) {
        List<Long> result = jdbcTemplate.queryForList(
                "SELECT id FROM vendors WHERE user_id = ?", Long.class, userId);
        return result.isEmpty() ? null : result.get(0);
    }

    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    // ── Endpoints ─────────────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<ApiResponse<List<Car>>> getAllCars(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(carService.getAllCars(page, size)));
    }

    @GetMapping("/available")
    public ResponseEntity<ApiResponse<List<Car>>> getAvailableCars(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return ResponseEntity.ok(ApiResponse.success(carService.getAvailableCars(page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Car>> getCarById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ApiResponse.success(carService.getCarById(id)));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<Car>>> searchCars(
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String fuelType,
            @RequestParam(required = false) String transmission,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Boolean isAvailable,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                carService.searchCars(brand, fuelType, transmission, categoryId, isAvailable, page, size)));
    }

    // ✅ FIX: Add ownership check — a vendor can only fetch their own cars.
    // Admin can fetch any vendor's cars freely.
    @GetMapping("/vendor/{vendorId}")
    @PreAuthorize("hasAnyAuthority('ROLE_VENDOR', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<List<Car>>> getCarsByVendor(
            @PathVariable Long vendorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication auth) {

        if (!isAdmin(auth)) {
            Long userId = (Long) auth.getCredentials();
            Long myVendorId = getVendorIdOrNull(userId);
            if (myVendorId == null || !myVendorId.equals(vendorId)) {
                return ResponseEntity.status(403)
                        .body(ApiResponse.error("Not authorized to view this vendor's cars"));
            }
        }

        return ResponseEntity.ok(ApiResponse.success(carService.getCarsByVendor(vendorId, page, size)));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_VENDOR', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Car>> addCar(
            @RequestBody Car car,
            Authentication auth) {

        // ✅ FIX: Vendor can only add cars to their own vendor account
        if (!isAdmin(auth)) {
            Long userId = (Long) auth.getCredentials();
            Long myVendorId = getVendorIdOrNull(userId);
            if (myVendorId == null) {
                return ResponseEntity.status(403)
                        .body(ApiResponse.error("Vendor profile not found"));
            }
            // Override vendorId with authenticated vendor's actual ID
            car.setVendorId(myVendorId);
        }

        try {
            return ResponseEntity.ok(ApiResponse.success("Car added successfully", carService.addCar(car)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ✅ FIX: Vendor can only update their own cars
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_VENDOR', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Car>> updateCar(
            @PathVariable Long id,
            @RequestBody Car car,
            Authentication auth) {

        if (!isAdmin(auth)) {
            Long userId = (Long) auth.getCredentials();
            Long myVendorId = getVendorIdOrNull(userId);
            if (myVendorId == null) {
                return ResponseEntity.status(403).body(ApiResponse.error("Vendor profile not found"));
            }
            try {
                Car existing = carService.getCarById(id);
                if (!myVendorId.equals(existing.getVendorId())) {
                    return ResponseEntity.status(403)
                            .body(ApiResponse.error("Not authorized to update this car"));
                }
            } catch (RuntimeException e) {
                return ResponseEntity.notFound().build();
            }
        }

        try {
            return ResponseEntity.ok(ApiResponse.success("Car updated", carService.updateCar(id, car)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteCar(@PathVariable Long id) {
        try {
            carService.deleteCar(id);
            return ResponseEntity.ok(ApiResponse.success("Car deleted", null));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ✅ FIX: Vendor can only toggle availability of their own cars
    @PatchMapping("/{id}/availability")
    @PreAuthorize("hasAnyAuthority('ROLE_VENDOR', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Car>> toggleAvailability(
            @PathVariable Long id,
            @RequestParam boolean available,
            Authentication auth) {

        if (!isAdmin(auth)) {
            Long userId = (Long) auth.getCredentials();
            Long myVendorId = getVendorIdOrNull(userId);
            if (myVendorId == null) {
                return ResponseEntity.status(403).body(ApiResponse.error("Vendor profile not found"));
            }
            try {
                Car existing = carService.getCarById(id);
                if (!myVendorId.equals(existing.getVendorId())) {
                    return ResponseEntity.status(403)
                            .body(ApiResponse.error("Not authorized to modify this car"));
                }
            } catch (RuntimeException e) {
                return ResponseEntity.notFound().build();
            }
        }

        try {
            return ResponseEntity.ok(ApiResponse.success("Availability updated",
                    carService.toggleAvailability(id, available)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}