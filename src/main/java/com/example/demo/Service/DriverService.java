//package com.example.demo.Service;
//
//import com.example.demo.Model.Driver;
//import com.example.demo.Repository.DriverRepository;
//import com.example.demo.Repository.UserRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//
//import java.math.BigDecimal;
//import java.util.List;
//
//@Service
//@RequiredArgsConstructor
//public class DriverService {
//
//    private final DriverRepository driverRepository;
//    private final UserRepository userRepository;
//
//    public List<Driver> getAllDrivers(int page, int size) {
//        return driverRepository.findAll(page, size);
//    }
//
//    public List<Driver> getAvailableDrivers(int page, int size) {
//        return driverRepository.findAvailable(page, size);
//    }
//
//    public Driver getDriverById(Long id) {
//        return driverRepository.findById(id)
//                .orElseThrow(() -> new RuntimeException("Driver not found with id: " + id));
//    }
//
//    public List<Driver> getDriversByVendor(Long vendorId, int page, int size) {
//        return driverRepository.findByVendorId(vendorId, page, size);
//    }
//
//    public Driver addDriver(Driver driver) {
//        if (driver.getLicenseNumber() == null || driver.getLicenseNumber().isBlank()) {
//            throw new RuntimeException("License number is required");
//        }
//        if (driverRepository.existsByLicenseNumber(driver.getLicenseNumber())) {
//            throw new RuntimeException("Driver with this license number already exists");
//        }
//        userRepository.findById(driver.getUserId())
//                .orElseThrow(() -> new RuntimeException("User not found with id: " + driver.getUserId()));
//        if (driver.getRating() == null) {
//            driver.setRating(BigDecimal.ZERO);
//        }
//        long id = driverRepository.save(driver);
//        return driverRepository.findById(id)
//                .orElseThrow(() -> new RuntimeException("Driver creation failed"));
//    }
//
//    public Driver toggleAvailability(Long id, boolean available) {
//        driverRepository.findById(id)
//                .orElseThrow(() -> new RuntimeException("Driver not found with id: " + id));
//        driverRepository.updateAvailability(id, available);
//        return driverRepository.findById(id).orElseThrow();
//    }
//}

package com.example.demo.Service;

import com.example.demo.Model.Driver;
import com.example.demo.Repository.DriverRepository;
import com.example.demo.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DriverService {

    private final DriverRepository driverRepository;
    private final UserRepository userRepository;

    public List<Driver> getAllDrivers(int page, int size) {
        return driverRepository.findAll(page, size);
    }

    public List<Driver> getAvailableDrivers(int page, int size) {
        return driverRepository.findAvailable(page, size);
    }

    public Driver getDriverById(Long id) {
        return driverRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Driver not found with id: " + id));
    }

    public List<Driver> getDriversByVendor(Long vendorId, int page, int size) {
        return driverRepository.findByVendorId(vendorId, page, size);
    }

    public Driver addDriver(Driver driver) {
        if (driver.getLicenseNumber() == null || driver.getLicenseNumber().isBlank()) {
            throw new RuntimeException("License number is required");
        }
        if (driverRepository.existsByLicenseNumber(driver.getLicenseNumber())) {
            throw new RuntimeException("Driver with this license number already exists");
        }
        userRepository.findById(driver.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found with id: " + driver.getUserId()));
        if (driver.getRating() == null) {
            driver.setRating(BigDecimal.ZERO);
        }
        long id = driverRepository.save(driver);
        return driverRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Driver creation failed"));
    }

    public Driver toggleAvailability(Long id, boolean available) {
        driverRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Driver not found with id: " + id));
        driverRepository.updateAvailability(id, available);
        return driverRepository.findById(id).orElseThrow();
    }
}