package com.smartattendance.smart_attendance_payroll_system.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.smartattendance.smart_attendance_payroll_system.entity.Holiday;
import com.smartattendance.smart_attendance_payroll_system.service.HolidayService;

@Controller
@RequestMapping("/holidays")
public class HolidayController {

    private final HolidayService holidayService;

    public HolidayController(HolidayService holidayService) {
        this.holidayService = holidayService;
    }

    // =========================
    // View All Holidays
    // =========================

    @GetMapping
    public String showHolidays(Model model) {

        model.addAttribute(
                "holidayList",
                holidayService.getAllHolidays());

        return "holidays";
    }

    // =========================
    // Add Holiday Page
    // =========================

    @GetMapping("/add")
    public String showAddHoliday(Model model) {

        model.addAttribute(
                "holiday",
                new Holiday());

        return "add-holiday";
    }

    // =========================
    // Save Holiday
    // =========================

    @PostMapping("/save")
    public String saveHoliday(
            Holiday holiday,
            Model model) {

        try {

            holidayService.saveHoliday(holiday);

        } catch (IllegalArgumentException e) {

            model.addAttribute(
                    "errorMessage",
                    e.getMessage());

            return "add-holiday";
        }

        return "redirect:/holidays";
    }

    // =========================
    // Edit Holiday
    // =========================

    @GetMapping("/edit/{id}")
    public String editHoliday(
            @PathVariable("id") Integer id,
            Model model) {

        Holiday holiday =
                holidayService.getHolidayById(id);

        if (holiday == null) {
            return "redirect:/holidays";
        }

        model.addAttribute(
                "holiday",
                holiday);

        return "edit-holiday";
    }

    // =========================
    // Update Holiday
    // =========================

    @PostMapping("/update")
    public String updateHoliday(
            Holiday holiday,
            Model model) {

        try {

            holidayService.updateHoliday(holiday);

        } catch (IllegalArgumentException e) {

            model.addAttribute(
                    "errorMessage",
                    e.getMessage());

            return "edit-holiday";
        }

        return "redirect:/holidays";
    }

    // =========================
    // Delete Holiday
    // =========================

    @GetMapping("/delete/{id}")
    public String deleteHoliday(
            @PathVariable("id") Integer id) {

        try {

            holidayService.deleteHoliday(id);

        } catch (IllegalArgumentException e) {

            // Ignore invalid delete request
        }

        return "redirect:/holidays";
    }
}