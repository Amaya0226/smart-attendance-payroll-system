
package com.smartattendance.smart_attendance_payroll_system.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.smartattendance.smart_attendance_payroll_system.entity.Employee;
import com.smartattendance.smart_attendance_payroll_system.entity.EmployeeProfileChangeRequest;
import com.smartattendance.smart_attendance_payroll_system.repository.EmployeeRepository;
import com.smartattendance.smart_attendance_payroll_system.repository.EmployeeProfileChangeRequestRepository;

@Controller
@RequestMapping("/admin/profile-change-requests")
public class AdminProfileChangeRequestController {

    private final EmployeeProfileChangeRequestRepository requestRepository;
    private final EmployeeRepository employeeRepository;

    public AdminProfileChangeRequestController(
            EmployeeProfileChangeRequestRepository requestRepository,
            EmployeeRepository employeeRepository) {

        this.requestRepository = requestRepository;
        this.employeeRepository = employeeRepository;
    }

    // =====================================================
    // VIEW ALL PROFILE CHANGE REQUESTS
    // =====================================================

    @GetMapping
    public String viewRequests(Model model) {

        List<EmployeeProfileChangeRequest> requests =
                requestRepository.findAllByOrderByRequestedAtDesc();

        Map<Integer, String> employeeNames = new HashMap<>();
        Map<Integer, String> employeeCodes = new HashMap<>();
        Map<Integer, String> currentPhones = new HashMap<>();
        Map<Integer, String> currentAddresses = new HashMap<>();

        for (Employee employee : employeeRepository.findAll()) {

            Integer employeeId = employee.getEmployeeId();

            employeeNames.put(
                    employeeId,
                    employee.getFullName() != null
                            ? employee.getFullName()
                            : "Unknown Employee"
            );

            employeeCodes.put(
                    employeeId,
                    employee.getEmployeeCode() != null
                            ? employee.getEmployeeCode()
                            : "N/A"
            );

            currentPhones.put(
                    employeeId,
                    employee.getPhone() != null
                            ? employee.getPhone()
                            : "-"
            );

            currentAddresses.put(
                    employeeId,
                    employee.getAddress() != null
                            ? employee.getAddress()
                            : "-"
            );
        }

        long pendingCount = requests.stream()
                .filter(r -> "PENDING".equalsIgnoreCase(r.getStatus()))
                .count();

        long approvedCount = requests.stream()
                .filter(r -> "APPROVED".equalsIgnoreCase(r.getStatus()))
                .count();

        long rejectedCount = requests.stream()
                .filter(r -> "REJECTED".equalsIgnoreCase(r.getStatus()))
                .count();

        model.addAttribute("requests", requests);

        model.addAttribute("employeeNames", employeeNames);
        model.addAttribute("employeeCodes", employeeCodes);
        model.addAttribute("currentPhones", currentPhones);
        model.addAttribute("currentAddresses", currentAddresses);

        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("approvedCount", approvedCount);
        model.addAttribute("rejectedCount", rejectedCount);

        return "admin-profile-change-requests";
    }

    // =====================================================
    // APPROVE PROFILE CHANGE REQUEST
    // =====================================================

    @PostMapping("/{requestId}/approve")
    @Transactional
    public String approveRequest(
            @PathVariable Integer requestId,

            @RequestParam(name = "remarks", required = false)
            String adminRemarks,

            RedirectAttributes redirectAttributes) {

        EmployeeProfileChangeRequest request =
                requestRepository.findById(requestId).orElse(null);

        if (request == null) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Profile change request not found."
            );

            return "redirect:/admin/profile-change-requests";
        }

        if (!"PENDING".equalsIgnoreCase(request.getStatus())) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "This request has already been reviewed."
            );

            return "redirect:/admin/profile-change-requests";
        }

        Employee employee = employeeRepository
                .findById(request.getEmployeeId())
                .orElse(null);

        if (employee == null) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Employee record not found."
            );

            return "redirect:/admin/profile-change-requests";
        }

        // Update phone only if a new phone number was requested.
        if (request.getRequestedPhone() != null
                && !request.getRequestedPhone().isBlank()) {

            employee.setPhone(request.getRequestedPhone().trim());
        }

        // Update address only if a new address was requested.
        if (request.getRequestedAddress() != null
                && !request.getRequestedAddress().isBlank()) {

            employee.setAddress(request.getRequestedAddress().trim());
        }

        request.setStatus("APPROVED");
        request.setReviewedAt(LocalDateTime.now());
        request.setAdminRemarks(normalizeRemarks(adminRemarks));

        employeeRepository.save(employee);
        requestRepository.save(request);

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Profile change request approved successfully."
        );

        return "redirect:/admin/profile-change-requests";
    }

    // =====================================================
    // REJECT PROFILE CHANGE REQUEST
    // =====================================================

    @PostMapping("/{requestId}/reject")
    @Transactional
    public String rejectRequest(
            @PathVariable Integer requestId,

            @RequestParam(name = "remarks", required = false)
            String adminRemarks,

            RedirectAttributes redirectAttributes) {

        EmployeeProfileChangeRequest request =
                requestRepository.findById(requestId).orElse(null);

        if (request == null) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Profile change request not found."
            );

            return "redirect:/admin/profile-change-requests";
        }

        if (!"PENDING".equalsIgnoreCase(request.getStatus())) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "This request has already been reviewed."
            );

            return "redirect:/admin/profile-change-requests";
        }

        // Rejecting a request does not modify employee details.
        request.setStatus("REJECTED");
        request.setReviewedAt(LocalDateTime.now());
        request.setAdminRemarks(normalizeRemarks(adminRemarks));

        requestRepository.save(request);

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Profile change request rejected successfully."
        );

        return "redirect:/admin/profile-change-requests";
    }

    // =====================================================
    // NORMALIZE ADMIN REMARKS
    // =====================================================

    private String normalizeRemarks(String remarks) {

        if (remarks == null || remarks.isBlank()) {
            return null;
        }

        String cleaned = remarks.trim();

        return cleaned.length() > 500
                ? cleaned.substring(0, 500)
                : cleaned;
    }
}
