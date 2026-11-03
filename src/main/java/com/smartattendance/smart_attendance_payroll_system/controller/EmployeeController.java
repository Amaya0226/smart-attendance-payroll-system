package com.smartattendance.smart_attendance_payroll_system.controller;

import jakarta.mail.MessagingException;
import jakarta.validation.Valid;

import java.io.IOException;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.google.zxing.WriterException;
import com.smartattendance.smart_attendance_payroll_system.entity.Employee;
import com.smartattendance.smart_attendance_payroll_system.service.EmployeeService;

import email.EmailService;

@Controller
@RequestMapping("/employees")
public class EmployeeController {

    private final EmployeeService employeeService;
    private final EmailService emailService;

    public EmployeeController(
            EmployeeService employeeService,
            EmailService emailService) {

        this.employeeService = employeeService;
        this.emailService = emailService;
    }

    @GetMapping
    public String getAllEmployees(Model model) {

        model.addAttribute(
                "employees",
                employeeService.getAllEmployees());

        return "employees";
    }

    @GetMapping("/search")
    public String searchEmployees(
            @RequestParam("keyword") String keyword,
            Model model) {

        if (keyword == null || keyword.trim().isEmpty()) {

            model.addAttribute(
                    "employees",
                    employeeService.getAllEmployees());

        } else {

            var employeesByName =
                    employeeService.searchByName(keyword);

            var employeesByCode =
                    employeeService.searchByEmployeeCode(keyword);

            employeesByName.addAll(employeesByCode);

            model.addAttribute(
                    "employees",
                    employeesByName);
        }

        model.addAttribute("keyword", keyword);

        return "employees";
    }

    @GetMapping("/add")
    public String showAddEmployeeForm(Model model) {

        model.addAttribute(
                "employee",
                new Employee());

        return "add-employee";
    }

    @PostMapping("/save")
    public String saveEmployee(
            @Valid @ModelAttribute("employee") Employee employee,
            BindingResult bindingResult,
            Model model) {

        if (bindingResult.hasErrors()) {
            return "add-employee";
        }

        try {

            employeeService.saveEmployee(employee);

            return "redirect:/employees/add";

        } catch (IllegalArgumentException e) {

            model.addAttribute(
                    "errorMessage",
                    e.getMessage());

            return "add-employee";
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditEmployeeForm(
            @PathVariable Integer id,
            Model model) {

        Employee employee =
                employeeService.getEmployeeById(id);

        if (employee == null) {
            return "redirect:/employees";
        }

        model.addAttribute(
                "employee",
                employee);

        return "edit-employee";
    }

    @PostMapping("/update")
    public String updateEmployee(
            @Valid @ModelAttribute("employee") Employee employee,
            BindingResult bindingResult,
            Model model) {

        if (bindingResult.hasErrors()) {
            return "edit-employee";
        }

        try {

            employeeService.updateEmployee(employee);

            return "redirect:/employees";

        } catch (IllegalArgumentException e) {

            model.addAttribute(
                    "errorMessage",
                    e.getMessage());

            return "edit-employee";
        }
    }

    @GetMapping("/delete/{id}")
    public String deleteEmployee(
            @PathVariable Integer id) {

        employeeService.deleteEmployee(id);

        return "redirect:/employees";
    }


    // ==============================
    // SEND EMPLOYEE QR CODE BY EMAIL
    // ==============================

    @GetMapping("/send-qr/{id}")
    public String sendQRCodeByEmail(
            @PathVariable Integer id) {

        Employee employee =
                employeeService.getEmployeeById(id);

        if (employee == null) {
            return "redirect:/employees";
        }

        if (employee.getEmail() == null
                || employee.getEmail().trim().isEmpty()) {

            return "redirect:/employees?emailError=No+email+address+registered+for+this+employee";
        }

        try {

            emailService.sendEmployeeQRCode(
                    employee.getEmail(),
                    employee.getFullName(),
                    employee.getEmployeeCode());

            return "redirect:/employees?emailSuccess=QR+code+sent+successfully";

        } catch (MessagingException
                | WriterException
                | IOException e) {

            e.printStackTrace();

            return "redirect:/employees?emailError=Failed+to+send+QR+code";
        }
    }
}