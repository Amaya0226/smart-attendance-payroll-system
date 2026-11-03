package com.smartattendance.smart_attendance_payroll_system.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartattendance.smart_attendance_payroll_system.entity.SalaryRevision;

public interface SalaryRevisionRepository
        extends JpaRepository<SalaryRevision, Integer> {

    List<SalaryRevision> findByEmployeeIdOrderByEffectiveDateDesc(
            Integer employeeId);

    List<SalaryRevision> findByEffectiveDateBetween(
            LocalDate startDate,
            LocalDate endDate);

    SalaryRevision findTopByEmployeeIdAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(
            Integer employeeId,
            LocalDate date);
}