package com.smartattendance.smart_attendance_payroll_system.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartattendance.smart_attendance_payroll_system.entity.AttendanceOtp;

public interface AttendanceOtpRepository
        extends JpaRepository<AttendanceOtp, Integer> {

    Optional<AttendanceOtp> findTopByEmployeeIdAndUsedFalseOrderByCreatedAtDesc(
            Integer employeeId);
}