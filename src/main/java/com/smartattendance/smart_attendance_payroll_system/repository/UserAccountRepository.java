package com.smartattendance.smart_attendance_payroll_system.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartattendance.smart_attendance_payroll_system.entity.UserAccount;

public interface UserAccountRepository
        extends JpaRepository<UserAccount, Integer> {

    Optional<UserAccount> findByUsername(String username);

}