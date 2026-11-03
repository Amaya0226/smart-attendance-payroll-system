
package com.smartattendance.smart_attendance_payroll_system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan({
        "com.smartattendance.smart_attendance_payroll_system",
        "email"
})
public class SmartAttendancePayrollSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(
                SmartAttendancePayrollSystemApplication.class,
                args);
    }
}

