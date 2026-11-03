package com.smartattendance.smart_attendance_payroll_system.service;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.smartattendance.smart_attendance_payroll_system.entity.UserAccount;
import com.smartattendance.smart_attendance_payroll_system.entity.UserRole;
import com.smartattendance.smart_attendance_payroll_system.repository.UserAccountRepository;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder) {

        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {

        if (userAccountRepository
                .findByUsername("admin@smartattendance.com")
                .isEmpty()) {

            UserAccount admin = new UserAccount();

            admin.setUsername("admin@smartattendance.com");

            admin.setPassword(
                    passwordEncoder.encode("Admin@123")
            );

            admin.setRole(UserRole.ADMIN);

            admin.setEnabled(true);

            userAccountRepository.save(admin);

            System.out.println(
                    "====================================");
            System.out.println(
                    "Default Admin Account Created");
            System.out.println(
                    "Username: admin@smartattendance.com");
            System.out.println(
                    "Password: Admin@123");
            System.out.println(
                    "====================================");
        }
    }
}