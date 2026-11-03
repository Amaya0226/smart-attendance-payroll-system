package com.smartattendance.smart_attendance_payroll_system.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.smartattendance.smart_attendance_payroll_system.entity.Employee;
import com.smartattendance.smart_attendance_payroll_system.repository.AttendanceRepository;
import com.smartattendance.smart_attendance_payroll_system.repository.EmployeeRepository;

@Controller
public class EmployeeDashboardController {

    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;

    public EmployeeDashboardController(
            EmployeeRepository employeeRepository,
            AttendanceRepository attendanceRepository) {

        this.employeeRepository = employeeRepository;
        this.attendanceRepository = attendanceRepository;
    }

    @GetMapping("/employee/dashboard")
    public String employeeDashboard(
            Authentication authentication,
            Model model) {

        String username = authentication.getName();

        Employee employee = employeeRepository
                .findByEmail(username)
                .orElse(null);

        model.addAttribute("employee", employee);

        return "employee-dashboard";
    }

    @GetMapping("/employee/attendance")
    public String employeeAttendance(
            Authentication authentication,
            Model model) {

        String username = authentication.getName();

        Employee employee = employeeRepository
                .findByEmail(username)
                .orElse(null);

        if (employee == null) {
            return "redirect:/login?error=true";
        }

        model.addAttribute("employee", employee);

        model.addAttribute(
                "attendanceList",
                attendanceRepository.findByEmployeeId(
                        employee.getEmployeeId()
                )
        );

        return "employee-attendance";
    }
}