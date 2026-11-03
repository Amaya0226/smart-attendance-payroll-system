package com.smartattendance.smart_attendance_payroll_system.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.smartattendance.smart_attendance_payroll_system.entity.Overtime;
import com.smartattendance.smart_attendance_payroll_system.repository.EmployeeRepository;
import com.smartattendance.smart_attendance_payroll_system.service.OvertimeService;

@Controller
@RequestMapping("/overtime")
public class OvertimeController {

    private final OvertimeService overtimeService;
    private final EmployeeRepository employeeRepository;

    public OvertimeController(
            OvertimeService overtimeService,
            EmployeeRepository employeeRepository) {

        this.overtimeService = overtimeService;
        this.employeeRepository = employeeRepository;
    }

    @GetMapping
    public String showOvertime(Model model) {

        model.addAttribute(
                "overtimeList",
                overtimeService.getAllOvertime());

        model.addAttribute(
                "employees",
                employeeRepository.findAll());

        return "overtime";
    }

    @GetMapping("/add")
    public String showAddOvertime(Model model) {

        model.addAttribute(
                "overtime",
                new Overtime());

        model.addAttribute(
                "employees",
                employeeRepository.findAll());

        return "add-overtime";
    }

    @PostMapping("/save")
    public String saveOvertime(
            Overtime overtime,
            Model model) {

        try {

            overtimeService.saveOvertime(overtime);

            return "redirect:/overtime";

        } catch (IllegalArgumentException e) {

            model.addAttribute(
                    "errorMessage",
                    e.getMessage());

            model.addAttribute(
                    "employees",
                    employeeRepository.findAll());

            return "add-overtime";
        }
    }

    @GetMapping("/edit/{id}")
    public String editOvertime(
            @PathVariable("id") Integer id,
            Model model) {

        Overtime overtime =
                overtimeService.getOvertimeById(id);

        if (overtime == null) {

            return "redirect:/overtime";
        }

        model.addAttribute(
                "overtime",
                overtime);

        model.addAttribute(
                "employees",
                employeeRepository.findAll());

        return "edit-overtime";
    }

    @PostMapping("/update")
    public String updateOvertime(
            Overtime overtime,
            Model model) {

        try {

            overtimeService.updateOvertime(overtime);

            return "redirect:/overtime";

        } catch (IllegalArgumentException e) {

            model.addAttribute(
                    "errorMessage",
                    e.getMessage());

            model.addAttribute(
                    "employees",
                    employeeRepository.findAll());

            return "edit-overtime";
        }
    }

    @GetMapping("/delete/{id}")
    public String deleteOvertime(
            @PathVariable("id") Integer id) {

        try {

            overtimeService.deleteOvertime(id);

        } catch (IllegalArgumentException e) {

            // Ignore invalid delete request
        }

        return "redirect:/overtime";
    }
}