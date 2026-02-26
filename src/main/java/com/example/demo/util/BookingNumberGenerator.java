package com.example.demo.util;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Random;

@Component
public class BookingNumberGenerator {

    private static final String PREFIX = "BKG";
    private static final Random random = new Random();

    public String generate() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int randomNum = 1000 + random.nextInt(9000);
        return PREFIX + date + randomNum;
    }
}
