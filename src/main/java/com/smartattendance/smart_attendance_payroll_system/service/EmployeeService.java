package com.smartattendance.smart_attendance_payroll_system.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.smartattendance.smart_attendance_payroll_system.entity.Employee;
import com.smartattendance.smart_attendance_payroll_system.repository.EmployeeRepository;

import email.EmailService;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final UserAccountService userAccountService;
    private final EmailService emailService;

    public EmployeeService(
            EmployeeRepository employeeRepository,
            UserAccountService userAccountService,
            EmailService emailService) {

        this.employeeRepository = employeeRepository;
        this.userAccountService = userAccountService;
        this.emailService = emailService;
    }

    // =========================================================
    // GET ALL EMPLOYEES
    // =========================================================

    public List<Employee> getAllEmployees() {

        return employeeRepository.findAll();
    }

    // =========================================================
    // GET EMPLOYEE BY ID
    // =========================================================

    public Employee getEmployeeById(Integer id) {

        return employeeRepository
                .findById(id)
                .orElse(null);
    }

    // =========================================================
    // SAVE EMPLOYEE
    // =========================================================

    public Employee saveEmployee(Employee employee) {

        // Check duplicate employee code
        if (employeeRepository.existsByEmployeeCode(
                employee.getEmployeeCode())) {

            throw new IllegalArgumentException(
                    "Employee Code already exists."
            );
        }

        // Check duplicate NIC
        if (employeeRepository.existsByNic(
                employee.getNic())) {

            throw new IllegalArgumentException(
                    "NIC already exists."
            );
        }

        // Check duplicate email
        if (employeeRepository.existsByEmail(
                employee.getEmail())) {

            throw new IllegalArgumentException(
                    "Email already exists."
            );
        }

        // Save employee
        Employee savedEmployee =
                employeeRepository.save(employee);

        // =====================================================
        // CREATE EMPLOYEE LOGIN ACCOUNT
        // =====================================================

        String temporaryPassword =
                savedEmployee.getEmployeeCode();

        userAccountService.createEmployeeAccount(
                savedEmployee.getEmail(),
                temporaryPassword
        );

        // =====================================================
        // SEND LOGIN CREDENTIALS BY EMAIL
        // =====================================================

        emailService.sendEmployeeLoginCredentials(
                savedEmployee.getEmail(),
                savedEmployee.getFullName(),
                savedEmployee.getEmail(),
                temporaryPassword
        );

        return savedEmployee;
    }

    // =========================================================
    // UPDATE EMPLOYEE
    // =========================================================

    public Employee updateEmployee(Employee employee) {

        Employee existingEmployee =
                employeeRepository
                        .findById(employee.getEmployeeId())
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Employee not found."
                                )
                        );

        // Check duplicate employee code
        if (!existingEmployee.getEmployeeCode()
                .equalsIgnoreCase(
                        employee.getEmployeeCode())
                && employeeRepository.existsByEmployeeCode(
                        employee.getEmployeeCode())) {

            throw new IllegalArgumentException(
                    "Employee Code already exists."
            );
        }

        // Check duplicate NIC
        if (!existingEmployee.getNic()
                .equalsIgnoreCase(employee.getNic())
                && employeeRepository.existsByNic(
                        employee.getNic())) {

            throw new IllegalArgumentException(
                    "NIC already exists."
            );
        }

        // Check duplicate email
        if (!existingEmployee.getEmail()
                .equalsIgnoreCase(employee.getEmail())
                && employeeRepository.existsByEmail(
                        employee.getEmail())) {

            throw new IllegalArgumentException(
                    "Email already exists."
            );
        }

        return employeeRepository.save(employee);
    }

    // =========================================================
    // DELETE EMPLOYEE
    // =========================================================

    public void deleteEmployee(Integer id) {

        employeeRepository.deleteById(id);
    }

    // =========================================================
    // SEARCH BY NAME
    // =========================================================

    public List<Employee> searchByName(String name) {

        return employeeRepository
                .findByFullNameContainingIgnoreCase(name);
    }

    // =========================================================
    // SEARCH BY EMPLOYEE CODE
    // =========================================================

    public List<Employee> searchByEmployeeCode(
            String employeeCode) {

        return employeeRepository
                .findByEmployeeCodeContainingIgnoreCase(
                        employeeCode
                );
    }
}