
package com.smartattendance.smart_attendance_payroll_system.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartattendance.smart_attendance_payroll_system.entity.EmployeeAccountPreference;

public interface EmployeeAccountPreferenceRepository
        extends JpaRepository<EmployeeAccountPreference, Integer> {

    Optional<EmployeeAccountPreference> findByEmployeeId(Integer employeeId);
}
