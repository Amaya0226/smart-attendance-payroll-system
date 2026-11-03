
package com.smartattendance.smart_attendance_payroll_system.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartattendance.smart_attendance_payroll_system.entity.EmployeeProfileChangeRequest;

public interface EmployeeProfileChangeRequestRepository
        extends JpaRepository<EmployeeProfileChangeRequest, Integer> {

    List<EmployeeProfileChangeRequest> findByEmployeeIdOrderByRequestedAtDesc(
            Integer employeeId);

    boolean existsByEmployeeIdAndStatus(Integer employeeId, String status);

    List<EmployeeProfileChangeRequest> findAllByOrderByRequestedAtDesc();

    List<EmployeeProfileChangeRequest> findByStatusOrderByRequestedAtDesc(
            String status);

    Optional<EmployeeProfileChangeRequest> findByRequestId(Integer requestId);
}
