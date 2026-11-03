package com.smartattendance.smart_attendance_payroll_system.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartattendance.smart_attendance_payroll_system.entity.Overtime;

public interface OvertimeRepository
        extends JpaRepository<Overtime, Integer> {

    List<Overtime> findByEmployeeId(Integer employeeId);

    List<Overtime> findByOtDateBetween(
            LocalDate startDate,
            LocalDate endDate);

    List<Overtime> findByEmployeeIdAndOtDateBetween(
            Integer employeeId,
            LocalDate startDate,
            LocalDate endDate);
}
