
package com.smartattendance.smart_attendance_payroll_system.controller;

import java.util.Comparator;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.smartattendance.smart_attendance_payroll_system.entity.Employee;
import com.smartattendance.smart_attendance_payroll_system.entity.Payroll;
import com.smartattendance.smart_attendance_payroll_system.repository.EmployeeRepository;
import com.smartattendance.smart_attendance_payroll_system.repository.PayrollRepository;

@Controller
@RequestMapping("/employee")
public class EmployeePayrollController {

    private final EmployeeRepository employeeRepository;
    private final PayrollRepository payrollRepository;

    public EmployeePayrollController(
            EmployeeRepository employeeRepository,
            PayrollRepository payrollRepository) {

        this.employeeRepository = employeeRepository;
        this.payrollRepository = payrollRepository;
    }

    private Employee getLoggedInEmployee(Authentication authentication) {

        if (authentication == null) {
            return null;
        }

        return employeeRepository
                .findByEmail(authentication.getName())
                .orElse(null);
    }

    private List<Payroll> getEmployeePayrolls(Integer employeeId) {

        List<Payroll> payrollList =
                payrollRepository.findByEmployeeId(employeeId);

        payrollList.sort(
                Comparator.comparing(
                        Payroll::getPayrollMonth,
                        Comparator.nullsLast(
                                Comparator.reverseOrder())));

        return payrollList;
    }

    @GetMapping("/payroll")
    public String myPayroll(
            Authentication authentication,
            Model model) {

        Employee employee = getLoggedInEmployee(authentication);

        if (employee == null) {
            return "redirect:/login?error=true";
        }

        model.addAttribute("employee", employee);
        model.addAttribute(
                "payrollList",
                getEmployeePayrolls(employee.getEmployeeId()));

        return "employee-payroll";
    }

    @GetMapping("/payslips")
    public String myPayslips(
            Authentication authentication,
            Model model) {

        Employee employee = getLoggedInEmployee(authentication);

        if (employee == null) {
            return "redirect:/login?error=true";
        }

        model.addAttribute("employee", employee);
        model.addAttribute(
                "payrollList",
                getEmployeePayrolls(employee.getEmployeeId()));

        return "employee-payslips";
    }

    @GetMapping("/payslips/{id}")
    public String viewPayslip(
            @PathVariable("id") Integer id,
            Authentication authentication,
            Model model) {

        Employee employee = getLoggedInEmployee(authentication);

        if (employee == null) {
            return "redirect:/login?error=true";
        }

        Payroll payroll = payrollRepository.findById(id)
                .orElse(null);

        // Prevent access to another employee's payslip.
        if (payroll == null
                || !employee.getEmployeeId().equals(
                        payroll.getEmployeeId())) {

            return "redirect:/employee/payslips?notFound=true";
        }

        model.addAttribute("employee", employee);
        model.addAttribute("payroll", payroll);

        return "employee-payslip-details";
    }
}
