package com.smartattendance.smart_attendance_payroll_system.controller;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.smartattendance.smart_attendance_payroll_system.entity.UserAccount;
import com.smartattendance.smart_attendance_payroll_system.repository.UserAccountRepository;

@Controller
public class LoginController {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public LoginController(
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder) {

        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/login")
    public String showLoginPage() {
        return "login";
    }

    @GetMapping("/forgot-password")
    public String showForgotPasswordPage() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(
            @RequestParam("email") String email,
            Model model) {

        System.out.println(
                "FORGOT PASSWORD REQUEST: " + email);

        if (userAccountRepository
                .findByUsername(email)
                .isPresent()) {

            System.out.println(
                    "EMAIL FOUND: " + email);

            model.addAttribute(
                    "email",
                    email);

            return "reset-password";
        }

        System.out.println(
                "EMAIL NOT FOUND: " + email);

        model.addAttribute(
                "error",
                "No account was found with this email address.");

        return "forgot-password";
    }

    @PostMapping("/reset-password")
    public String resetPassword(
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam("confirmPassword") String confirmPassword,
            Model model) {

        System.out.println(
                "RESET PASSWORD REQUEST: " + email);

        // Check whether both passwords match
        if (!password.equals(confirmPassword)) {

            model.addAttribute(
                    "email",
                    email);

            model.addAttribute(
                    "error",
                    "Passwords do not match.");

            return "reset-password";
        }

        // Find the user account
        UserAccount userAccount =
                userAccountRepository
                        .findByUsername(email)
                        .orElse(null);

        if (userAccount == null) {

            model.addAttribute(
                    "error",
                    "No account was found with this email address.");

            return "forgot-password";
        }

        // Encrypt the new password
        String encodedPassword =
                passwordEncoder.encode(password);

        userAccount.setPassword(encodedPassword);

        // Save the updated password
        userAccountRepository.save(userAccount);

        System.out.println(
                "PASSWORD RESET SUCCESSFUL: " + email);

        return "redirect:/login?resetSuccess=true";
    }
}