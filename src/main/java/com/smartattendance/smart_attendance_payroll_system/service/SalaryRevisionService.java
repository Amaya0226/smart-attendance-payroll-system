package com.smartattendance.smart_attendance_payroll_system.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.smartattendance.smart_attendance_payroll_system.entity.Employee;
import com.smartattendance.smart_attendance_payroll_system.entity.SalaryRevision;
import com.smartattendance.smart_attendance_payroll_system.repository.EmployeeRepository;
import com.smartattendance.smart_attendance_payroll_system.repository.SalaryRevisionRepository;

@Service
public class SalaryRevisionService {

    private final SalaryRevisionRepository salaryRevisionRepository;
    private final EmployeeRepository employeeRepository;

    public SalaryRevisionService(
            SalaryRevisionRepository salaryRevisionRepository,
            EmployeeRepository employeeRepository) {

        this.salaryRevisionRepository = salaryRevisionRepository;
        this.employeeRepository = employeeRepository;
    }

    public List<SalaryRevision> getAllSalaryRevisions() {
        return salaryRevisionRepository.findAll();
    }

    public SalaryRevision getSalaryRevisionById(Integer id) {
        return salaryRevisionRepository.findById(id).orElse(null);
    }

    public List<SalaryRevision> getEmployeeSalaryHistory(
            Integer employeeId) {

        return salaryRevisionRepository
                .findByEmployeeIdOrderByEffectiveDateDesc(employeeId);
    }

    public void saveSalaryRevision(SalaryRevision revision) {

        if (revision.getEmployeeId() == null) {
            throw new IllegalArgumentException(
                    "Employee is required.");
        }

        if (revision.getEffectiveDate() == null) {
            throw new IllegalArgumentException(
                    "Effective date is required.");
        }

        if (revision.getNewSalary() == null) {
            throw new IllegalArgumentException(
                    "New salary is required.");
        }

        if (revision.getNewSalary().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "New salary must be greater than zero.");
        }

        Employee employee = employeeRepository
                .findById(revision.getEmployeeId())
                .orElse(null);

        if (employee == null) {
            throw new IllegalArgumentException(
                    "Employee not found.");
        }

        BigDecimal currentSalary = employee.getBasicSalary();

        if (currentSalary == null) {
            currentSalary = BigDecimal.ZERO;
        }

        currentSalary = currentSalary.setScale(
                2,
                RoundingMode.HALF_UP);

        BigDecimal newSalary = revision.getNewSalary()
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal changeAmount = newSalary.subtract(currentSalary)
                .setScale(2, RoundingMode.HALF_UP);

        revision.setPreviousSalary(currentSalary);
        revision.setNewSalary(newSalary);
        revision.setChangeAmount(changeAmount);

        if (revision.getRevisionType() == null
                || revision.getRevisionType().trim().isEmpty()) {

            revision.setRevisionType("REVISION");
        }

        revision.setCreatedAt(LocalDateTime.now());

        salaryRevisionRepository.save(revision);

        /*
         * Update the employee's current salary immediately only when
         * the effective date has already arrived.
         *
         * Future salary revisions will be applied later according
         * to their effective date.
         */
        if (!revision.getEffectiveDate()
                .isAfter(LocalDate.now())) {

            employee.setBasicSalary(newSalary);

            employeeRepository.save(employee);
        }
    }

    public void updateSalaryRevision(SalaryRevision revision) {

        if (revision.getRevisionId() == null) {
            throw new IllegalArgumentException(
                    "Revision ID is required.");
        }

        SalaryRevision existing = salaryRevisionRepository
                .findById(revision.getRevisionId())
                .orElse(null);

        if (existing == null) {
            throw new IllegalArgumentException(
                    "Salary revision not found.");
        }

        saveSalaryRevision(revision);
    }

    public void deleteSalaryRevision(Integer id) {

        if (!salaryRevisionRepository.existsById(id)) {
            throw new IllegalArgumentException(
                    "Salary revision not found.");
        }

        salaryRevisionRepository.deleteById(id);
    }
}