//package com.example.demo.Model;
//
//import lombok.AllArgsConstructor;
//import lombok.Builder;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//
//import java.math.BigDecimal;
//import java.time.LocalDateTime;
//
//@Data
//@Builder
//@NoArgsConstructor
//@AllArgsConstructor
//public class Driver {
//    private Long id;
//    private Long userId;
//    private Long vendorId;
//    private String licenseNumber;
//    private Integer experienceYears;
//    private boolean isAvailable;
//    private BigDecimal rating;
//    private LocalDateTime createdAt;
//
//    // Joined fields
//    private String userFullName;
//    private String userEmail;
//    private String userPhone;
//    private String vendorCompanyName;
//}

package com.example.demo.Model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true) // ✅ FIX: ignore unknown fields instead of throwing
public class Driver {
    private Long id;
    private Long userId;
    private Long vendorId;
    private String licenseNumber;
    private Integer experienceYears;

    // ✅ FIX: Lombok generates isAvailable() getter for boolean fields,
    // causing Jackson to serialize/deserialize as "available" (strips "is" prefix).
    // @JsonProperty forces the JSON key to always be "isAvailable" in both directions.
    // @JsonAlias also accepts "available" or "is_available" from legacy callers.
    @JsonProperty("isAvailable")
    @JsonAlias({"available", "is_available"})
    private boolean isAvailable;

    private BigDecimal rating;
    private LocalDateTime createdAt;

    // Joined fields
    private String userFullName;
    private String userEmail;
    private String userPhone;
    private String vendorCompanyName;
}