
package com.example.demo.Service;

import com.example.demo.Model.Car;
import com.example.demo.Repository.CarCategoryRepository;
import com.example.demo.Repository.CarRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CarService {

    private final CarRepository carRepository;
    private final CarCategoryRepository carCategoryRepository;

    public List<Car> getAllCars(int page, int size) {
        return carRepository.findAll(page, size);
    }

    public List<Car> getAvailableCars(int page, int size) {
        return carRepository.findAvailable(page, size);
    }

    public Car getCarById(Long id) {
        return carRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Car not found with id: " + id));
    }

    public List<Car> getCarsByVendor(Long vendorId, int page, int size) {
        return carRepository.findByVendorId(vendorId, page, size);
    }

    public List<Car> searchCars(String brand, String fuelType, String transmission,
                                Long categoryId, Boolean isAvailable, int page, int size) {
        return carRepository.search(brand, fuelType, transmission, categoryId, isAvailable, page, size);
    }

    public Car addCar(Car car) {
        System.out.println("=== addCar called ===");
        System.out.println("brand=" + car.getBrand());
        System.out.println("model=" + car.getModel());
        System.out.println("registrationNo=" + car.getRegistrationNo());
        System.out.println("dailyRate=" + car.getDailyRate());
        System.out.println("categoryId=" + car.getCategoryId());
        System.out.println("vendorId=" + car.getVendorId());

        // ✅ FIXED: was car.getAvailable() / car.setAvailable() → now getIsAvailable() / setIsAvailable()
        if (car.getIsAvailable() == null) car.setIsAvailable(true);
        if (car.getCategoryId() == null) car.setCategoryId(1L);

        if (car.getBrand() == null || car.getBrand().isBlank())
            throw new RuntimeException("Car brand is required");
        if (car.getModel() == null || car.getModel().isBlank())
            throw new RuntimeException("Car model is required");
        if (car.getDailyRate() == null)
            throw new RuntimeException("Daily rate is required");

        if (car.getRegistrationNo() == null || car.getRegistrationNo().isBlank()) {
            car.setRegistrationNo("REG-" + System.currentTimeMillis());
            System.out.println("⚠️ Registration number was missing — auto-generated: " + car.getRegistrationNo());
        }

        if (carRepository.existsByRegistrationNo(car.getRegistrationNo())) {
            throw new RuntimeException("Car with registration number '" + car.getRegistrationNo() + "' already exists");
        }

        if (car.getCategoryId() != null && car.getCategoryId() > 0) {
            carCategoryRepository.findById(car.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found with id: " + car.getCategoryId()));
        }

        long id = carRepository.save(car);
        return carRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Car creation failed"));
    }

    public Car updateCar(Long id, Car request) {
        Car existing = carRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Car not found with id: " + id));

        existing.setCategoryId(request.getCategoryId() != null ? request.getCategoryId() : existing.getCategoryId());
        existing.setBrand(request.getBrand() != null ? request.getBrand() : existing.getBrand());
        existing.setModel(request.getModel() != null ? request.getModel() : existing.getModel());
        existing.setYear(request.getYear() != null && request.getYear() != 0 ? request.getYear() : existing.getYear());
        existing.setRegistrationNo(request.getRegistrationNo() != null ? request.getRegistrationNo() : existing.getRegistrationNo());
        existing.setColor(request.getColor() != null ? request.getColor() : existing.getColor());
        existing.setSeats(request.getSeats() != null ? request.getSeats() : existing.getSeats());
        existing.setFuelType(request.getFuelType() != null ? request.getFuelType() : existing.getFuelType());
        existing.setTransmission(request.getTransmission() != null ? request.getTransmission() : existing.getTransmission());
        existing.setDailyRate(request.getDailyRate() != null ? request.getDailyRate() : existing.getDailyRate());
        existing.setImageUrl(request.getImageUrl() != null ? request.getImageUrl() : existing.getImageUrl());

        // ✅ FIXED: was request.getAvailable() / existing.setAvailable() → getIsAvailable() / setIsAvailable()
        if (request.getIsAvailable() != null) existing.setIsAvailable(request.getIsAvailable());

        carRepository.update(existing);
        return carRepository.findById(id).orElseThrow();
    }

    public void deleteCar(Long id) {
        carRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Car not found with id: " + id));
        carRepository.delete(id);
    }

    public Car toggleAvailability(Long id, boolean available) {
        carRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Car not found with id: " + id));
        carRepository.updateAvailability(id, available);
        return carRepository.findById(id).orElseThrow();
    }
}