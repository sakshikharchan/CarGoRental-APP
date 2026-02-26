package com.example.demo.Controller;

import com.example.demo.DTOs.ApiResponse;
import com.example.demo.Model.CarCategory;
import com.example.demo.Repository.CarCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CarCategoryRepository categoryRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CarCategory>>> getAll() {
        return ResponseEntity.ok(
                ApiResponse.success(categoryRepository.findAll())
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CarCategory>> getById(@PathVariable Long id) {
        return categoryRepository.findById(id)
                .map(category -> ResponseEntity.ok(ApiResponse.success(category)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<CarCategory>> create(@RequestBody CarCategory category) {

        long id = categoryRepository.save(category);

        CarCategory saved = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category creation failed"));

        return ResponseEntity.ok(
                ApiResponse.success("Category created successfully", saved)
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<CarCategory>> update(
            @PathVariable Long id,
            @RequestBody CarCategory category) {

        categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        category.setId(id);
        categoryRepository.update(category);

        return ResponseEntity.ok(
                ApiResponse.success("Category updated successfully",
                        categoryRepository.findById(id).orElseThrow())
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {

        categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        categoryRepository.delete(id);

        return ResponseEntity.ok(
                ApiResponse.success("Category deleted successfully", null)
        );
    }
}
