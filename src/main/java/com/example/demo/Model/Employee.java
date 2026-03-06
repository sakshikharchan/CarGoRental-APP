package com.example.demo.Model;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor

public class Employee {

    private int employeeId;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNum;
}