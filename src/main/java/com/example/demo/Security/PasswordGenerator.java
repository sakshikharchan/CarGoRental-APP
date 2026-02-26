package com.example.demo.Security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        System.out.println("admin123 -> " + encoder.encode("admin123"));
        System.out.println("vendor123 -> " + encoder.encode("vendor123"));
        System.out.println("customer123 -> " + encoder.encode("customer123"));
        System.out.println("driver123 -> " + encoder.encode("driver123"));
    }
}
