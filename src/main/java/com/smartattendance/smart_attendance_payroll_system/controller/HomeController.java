package com.smartattendance.smart_attendance_payroll_system.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    // Main route redirects to Admin Dashboard
    @GetMapping("/")
    public String showDashboard() {
        return "redirect:/dashboard";
    }

    // Public Home Page
    @GetMapping("/home")
    public String showHomePage() {
        return "home";
    }
}