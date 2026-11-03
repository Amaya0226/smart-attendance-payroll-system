
package com.smartattendance.smart_attendance_payroll_system.controller;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.smartattendance.smart_attendance_payroll_system.entity.Employee;
import com.smartattendance.smart_attendance_payroll_system.entity.EmployeeAccountPreference;
import com.smartattendance.smart_attendance_payroll_system.entity.EmployeeProfileChangeRequest;
import com.smartattendance.smart_attendance_payroll_system.entity.UserAccount;
import com.smartattendance.smart_attendance_payroll_system.repository.EmployeeAccountPreferenceRepository;
import com.smartattendance.smart_attendance_payroll_system.repository.EmployeeProfileChangeRequestRepository;
import com.smartattendance.smart_attendance_payroll_system.repository.EmployeeRepository;
import com.smartattendance.smart_attendance_payroll_system.repository.UserAccountRepository;

@Controller
@RequestMapping("/employee")
public class EmployeeAccountController {

    private final UserAccountRepository userAccountRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeAccountPreferenceRepository preferenceRepository;
    private final EmployeeProfileChangeRequestRepository requestRepository;
    private final PasswordEncoder passwordEncoder;

    public EmployeeAccountController(
            UserAccountRepository userAccountRepository,
            EmployeeRepository employeeRepository,
            EmployeeAccountPreferenceRepository preferenceRepository,
            EmployeeProfileChangeRequestRepository requestRepository,
            PasswordEncoder passwordEncoder) {

        this.userAccountRepository = userAccountRepository;
        this.employeeRepository = employeeRepository;
        this.preferenceRepository = preferenceRepository;
        this.requestRepository = requestRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // -------------------------------------------------
    // ACCOUNT SETTINGS PAGE
    // -------------------------------------------------

    @GetMapping("/account-settings")
    public String showAccountSettings(
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {

        Employee employee = getAuthenticatedEmployee(authentication);

        if (employee == null) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Your employee account could not be verified.");

            return "redirect:/login";
        }

        EmployeeAccountPreference preferences =
                getOrCreatePreferences(employee.getEmployeeId());

        model.addAttribute("employee", employee);
        model.addAttribute("preferences", preferences);

        model.addAttribute(
                "profileRequests",
                requestRepository
                        .findByEmployeeIdOrderByRequestedAtDesc(
                                employee.getEmployeeId()));

        return "employee-account-settings";
    }

    // -------------------------------------------------
    // PASSWORD CHANGE
    // -------------------------------------------------

    @PostMapping("/change-password")
    public String changePassword(
            Authentication authentication,
            @RequestParam("currentPassword") String currentPassword,
            @RequestParam("newPassword") String newPassword,
            @RequestParam("confirmPassword") String confirmPassword,
            RedirectAttributes redirectAttributes) {

        UserAccount userAccount = getAuthenticatedAccount(authentication);

        if (userAccount == null || !userAccount.isEnabled()) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Your account could not be verified.");

            return settingsRedirect();
        }

        if (currentPassword == null || currentPassword.isBlank()
                || !passwordEncoder.matches(
                        currentPassword,
                        userAccount.getPassword())) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Your current password is incorrect.");

            return settingsRedirect();
        }

        if (newPassword == null || newPassword.length() < 8) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "New password must contain at least 8 characters.");

            return settingsRedirect();
        }

        if (newPassword.length() > 72) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "New password must not exceed 72 characters.");

            return settingsRedirect();
        }

        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "New password and confirmation do not match.");

            return settingsRedirect();
        }

        if (passwordEncoder.matches(
                newPassword,
                userAccount.getPassword())) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "New password must be different from your current password.");

            return settingsRedirect();
        }

        userAccount.setPassword(passwordEncoder.encode(newPassword));
        userAccountRepository.save(userAccount);

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Your password has been changed successfully.");

        return settingsRedirect();
    }

    // -------------------------------------------------
    // PERSONAL INFORMATION CHANGE REQUEST
    // -------------------------------------------------

    @PostMapping("/profile-change-request")
    public String submitProfileChangeRequest(
            Authentication authentication,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) String reason,
            RedirectAttributes redirectAttributes) {

        Employee employee = getAuthenticatedEmployee(authentication);

        if (employee == null) {
            return "redirect:/login";
        }

        String cleanPhone = phone == null ? "" : phone.trim();
        String cleanAddress = address == null ? "" : address.trim();
        String cleanReason = reason == null ? "" : reason.trim();

        if (cleanPhone.length() > 20 || cleanAddress.length() > 255
                || cleanReason.length() > 500) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Please check the maximum allowed field lengths.");

            return settingsRedirect();
        }

        boolean phoneChanged = !cleanPhone.equals(
                employee.getPhone() == null ? "" : employee.getPhone());

        boolean addressChanged = !cleanAddress.equals(
                employee.getAddress() == null ? "" : employee.getAddress());

        if (!phoneChanged && !addressChanged) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "No profile changes were detected.");

            return settingsRedirect();
        }

        if (cleanReason.isBlank()) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Please provide a reason for your profile change request.");

            return settingsRedirect();
        }

        if (requestRepository.existsByEmployeeIdAndStatus(
                employee.getEmployeeId(), "PENDING")) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "You already have a pending profile change request.");

            return settingsRedirect();
        }

        EmployeeProfileChangeRequest request =
                new EmployeeProfileChangeRequest();

        request.setEmployeeId(employee.getEmployeeId());
        request.setRequestedPhone(
                phoneChanged ? cleanPhone : null);
        request.setRequestedAddress(
                addressChanged ? cleanAddress : null);
        request.setReason(cleanReason);
        request.setStatus("PENDING");

        requestRepository.save(request);

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Your profile change request has been submitted for admin approval.");

        return settingsRedirect();
    }

    // -------------------------------------------------
    // SAVE NOTIFICATION PREFERENCES
    // -------------------------------------------------

    @PostMapping("/save-notifications")
    public String saveNotifications(
            Authentication authentication,
            @RequestParam(defaultValue = "false")
            boolean attendanceNotifications,
            @RequestParam(defaultValue = "false")
            boolean payrollNotifications,
            @RequestParam(defaultValue = "false")
            boolean payslipNotifications,
            RedirectAttributes redirectAttributes) {

        Employee employee = getAuthenticatedEmployee(authentication);

        if (employee == null) {
            return "redirect:/login";
        }

        EmployeeAccountPreference preferences =
                getOrCreatePreferences(employee.getEmployeeId());

        preferences.setAttendanceNotifications(
                attendanceNotifications);

        preferences.setPayrollNotifications(
                payrollNotifications);

        preferences.setPayslipNotifications(
                payslipNotifications);

        preferenceRepository.save(preferences);

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Your notification preferences have been saved.");

        return settingsRedirect();
    }

    // -------------------------------------------------
    // SAVE APPEARANCE PREFERENCE
    // -------------------------------------------------

    @PostMapping("/save-appearance")
    public String saveAppearance(
            Authentication authentication,
            @RequestParam("theme") String theme,
            RedirectAttributes redirectAttributes) {

        Employee employee = getAuthenticatedEmployee(authentication);

        if (employee == null) {
            return "redirect:/login";
        }

        if (theme == null
                || (!theme.equals("LIGHT") && !theme.equals("DARK"))) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Invalid appearance preference.");

            return settingsRedirect();
        }

        EmployeeAccountPreference preferences =
                getOrCreatePreferences(employee.getEmployeeId());

        preferences.setTheme(theme);
        preferenceRepository.save(preferences);

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Your appearance preference has been saved.");

        return settingsRedirect();
    }

    // -------------------------------------------------
    // HELPER METHODS
    // -------------------------------------------------

    private UserAccount getAuthenticatedAccount(
            Authentication authentication) {

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {

            return null;
        }

        return userAccountRepository
                .findByUsername(authentication.getName())
                .orElse(null);
    }

    private Employee getAuthenticatedEmployee(
            Authentication authentication) {

        UserAccount account = getAuthenticatedAccount(authentication);

        if (account == null || !account.isEnabled()) {
            return null;
        }

        Optional<Employee> employee =
                employeeRepository.findByEmail(account.getUsername());

        return employee.orElse(null);
    }

    private EmployeeAccountPreference getOrCreatePreferences(
            Integer employeeId) {

        return preferenceRepository.findByEmployeeId(employeeId)
                .orElseGet(() -> {
                    EmployeeAccountPreference preferences =
                            new EmployeeAccountPreference();

                    preferences.setEmployeeId(employeeId);

                    return preferenceRepository.save(preferences);
                });
    }

    private String settingsRedirect() {
        return "redirect:/employee/account-settings";
    }
}
