package com.smartattendance.smart_attendance_payroll_system.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartattendance.smart_attendance_payroll_system.entity.Employee;

public interface EmployeeRepository extends JpaRepository<Employee, Integer> {

    // Search by employee name
    List<Employee> findByFullNameContainingIgnoreCase(String fullName);

    // Search by employee code
    List<Employee> findByEmployeeCodeContainingIgnoreCase(String employeeCode);

    // Find employee by email
    Optional<Employee> findByEmail(String email);

    // Check duplicate employee code
    boolean existsByEmployeeCode(String employeeCode);

    // Check duplicate NIC
    boolean existsByNic(String nic);

    // Check duplicate email
    boolean existsByEmail(String email);
}