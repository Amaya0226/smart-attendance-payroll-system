package com.smartattendance.smart_attendance_payroll_system.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.smartattendance.smart_attendance_payroll_system.entity.SalaryRevision;
import com.smartattendance.smart_attendance_payroll_system.repository.EmployeeRepository;
import com.smartattendance.smart_attendance_payroll_system.service.SalaryRevisionService;

@Controller
@RequestMapping("/salary-revisions")
public class SalaryRevisionController {

    private final SalaryRevisionService salaryRevisionService;
    private final EmployeeRepository employeeRepository;

    public SalaryRevisionController(
            SalaryRevisionService salaryRevisionService,
            EmployeeRepository employeeRepository) {

        this.salaryRevisionService = salaryRevisionService;
        this.employeeRepository = employeeRepository;
    }

    @GetMapping
    public String showSalaryRevisions(Model model) {

        model.addAttribute(
                "salaryRevisions",
                salaryRevisionService.getAllSalaryRevisions());

        return "salary-revisions";
    }

    @GetMapping("/add")
    public String showAddSalaryRevisionForm(Model model) {

        model.addAttribute(
                "salaryRevision",
                new SalaryRevision());

        model.addAttribute(
                "employees",
                employeeRepository.findAll());

        return "add-salary-revision";
    }

    @PostMapping("/save")
    public String saveSalaryRevision(
            SalaryRevision salaryRevision,
            Model model) {

        try {

            salaryRevisionService
                    .saveSalaryRevision(salaryRevision);

            return "redirect:/salary-revisions";

        } catch (IllegalArgumentException e) {

            model.addAttribute(
                    "errorMessage",
                    e.getMessage());

            model.addAttribute(
                    "employees",
                    employeeRepository.findAll());

            return "add-salary-revision";
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditSalaryRevisionForm(
            @PathVariable Integer id,
            Model model) {

        SalaryRevision salaryRevision =
                salaryRevisionService.getSalaryRevisionById(id);

        if (salaryRevision == null) {
            return "redirect:/salary-revisions";
        }

        model.addAttribute(
                "salaryRevision",
                salaryRevision);

        model.addAttribute(
                "employees",
                employeeRepository.findAll());

        return "edit-salary-revision";
    }

    @PostMapping("/update")
    public String updateSalaryRevision(
            SalaryRevision salaryRevision,
            Model model) {

        try {

            salaryRevisionService
                    .updateSalaryRevision(salaryRevision);

            return "redirect:/salary-revisions";

        } catch (IllegalArgumentException e) {

            model.addAttribute(
                    "errorMessage",
                    e.getMessage());

            model.addAttribute(
                    "employees",
                    employeeRepository.findAll());

            return "edit-salary-revision";
        }
    }

    @GetMapping("/delete/{id}")
    public String deleteSalaryRevision(
            @PathVariable Integer id) {

        salaryRevisionService.deleteSalaryRevision(id);

        return "redirect:/salary-revisions";
    }

    @GetMapping("/history/{employeeId}")
    public String showEmployeeSalaryHistory(
            @PathVariable Integer employeeId,
            Model model) {

        model.addAttribute(
                "salaryRevisions",
                salaryRevisionService
                        .getEmployeeSalaryHistory(employeeId));

        model.addAttribute(
                "employee",
                employeeRepository
                        .findById(employeeId)
                        .orElse(null));

        return "salary-history";
    }
}