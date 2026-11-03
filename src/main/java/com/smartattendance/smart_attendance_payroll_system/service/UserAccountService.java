package com.smartattendance.smart_attendance_payroll_system.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.smartattendance.smart_attendance_payroll_system.entity.UserAccount;
import com.smartattendance.smart_attendance_payroll_system.entity.UserRole;
import com.smartattendance.smart_attendance_payroll_system.repository.UserAccountRepository;

@Service
public class UserAccountService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public UserAccountService(
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder) {

        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserAccount createEmployeeAccount(
            String email,
            String temporaryPassword) {

        if (userAccountRepository
                .findByUsername(email)
                .isPresent()) {

            throw new IllegalArgumentException(
                    "A login account already exists for this email.");
        }

        UserAccount userAccount = new UserAccount();

        userAccount.setUsername(email);
        userAccount.setPassword(
                passwordEncoder.encode(temporaryPassword));
        userAccount.setRole(UserRole.EMPLOYEE);
        userAccount.setEnabled(true);

        return userAccountRepository.save(userAccount);
    }
}