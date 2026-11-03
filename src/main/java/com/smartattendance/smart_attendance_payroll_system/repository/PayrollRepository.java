package com.smartattendance.smart_attendance_payroll_system.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartattendance.smart_attendance_payroll_system.entity.Payroll;

public interface PayrollRepository extends JpaRepository<Payroll, Integer> {

    List<Payroll> findByEmployeeId(Integer employeeId);

    List<Payroll> findByPayrollMonth(LocalDate payrollMonth);

    List<Payroll> findByEmployeeIdAndPayrollMonth(
            Integer employeeId,
            LocalDate payrollMonth);

    boolean existsByEmployeeIdAndPayrollMonth(
            Integer employeeId,
            LocalDate payrollMonth);

    // Get all payroll records within a selected month
    List<Payroll> findByPayrollMonthBetween(
            LocalDate startDate,
            LocalDate endDate);
}