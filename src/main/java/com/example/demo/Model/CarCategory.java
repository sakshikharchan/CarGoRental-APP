package com.example.demo.Model;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class CarCategory {

	 private Long id;
	    private String name;
	    private String description;
	    private LocalDateTime createdAt;

}
