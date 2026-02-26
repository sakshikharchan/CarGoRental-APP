package com.example.demo.Service;

import com.example.demo.Model.Vendor;
import com.example.demo.Repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VendorService {

    private static final Logger logger = LoggerFactory.getLogger(VendorService.class);
    private final VendorRepository vendorRepository;

    public List<Vendor> getAllVendors(int page, int size) {
        return vendorRepository.findAll(page, size);   // ✅ fixed: was (page*size, size)
    }

    public Vendor getVendorById(Long id) {
        return vendorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vendor not found: " + id));
    }

    public Vendor getVendorByUserId(Long userId) {
        return vendorRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Vendor profile not found for user: " + userId));
    }

    public List<Vendor> getPendingVendors(int page, int size) {
        return vendorRepository.findPending(page, size); // ✅ fixed: was (page*size, size)
    }

    public Vendor registerVendor(Vendor vendor) {
        long id = vendorRepository.save(vendor);
        logger.info("Vendor registered: {} (id={})", vendor.getCompanyName(), id);
        return vendorRepository.findById(id).orElseThrow();
    }

    public Vendor updateVendor(Long id, Vendor updatedVendor) {
        vendorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vendor not found: " + id));
        updatedVendor.setId(id);
        vendorRepository.update(updatedVendor);
        return vendorRepository.findById(id).orElseThrow();
    }

    public Vendor approveVendor(Long vendorId, boolean approved) {
        vendorRepository.findById(vendorId)
                .orElseThrow(() -> new RuntimeException("Vendor not found: " + vendorId));
        vendorRepository.updateApproval(vendorId, approved); // ✅ fixed: was .approve()
        logger.info("Vendor {} {}", vendorId, approved ? "approved" : "rejected");
        return vendorRepository.findById(vendorId).orElseThrow();
    }
}