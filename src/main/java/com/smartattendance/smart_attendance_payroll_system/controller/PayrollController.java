
package com.smartattendance.smart_attendance_payroll_system.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.smartattendance.smart_attendance_payroll_system.entity.Payroll;
import com.smartattendance.smart_attendance_payroll_system.entity.PayrollRule;
import com.smartattendance.smart_attendance_payroll_system.entity.PayrollRuleStatus;
import com.smartattendance.smart_attendance_payroll_system.entity.PayrollStatus;
import com.smartattendance.smart_attendance_payroll_system.repository.EmployeeRepository;
import com.smartattendance.smart_attendance_payroll_system.repository.PayrollRuleRepository;
import com.smartattendance.smart_attendance_payroll_system.service.PayrollService;

@Controller
@RequestMapping("/payroll")
public class PayrollController {

    private static final Logger log =
            LoggerFactory.getLogger(PayrollController.class);

    private final PayrollService payrollService;
    private final EmployeeRepository employeeRepository;
    private final PayrollRuleRepository payrollRuleRepository;

    public PayrollController(
            PayrollService payrollService,
            EmployeeRepository employeeRepository,
            PayrollRuleRepository payrollRuleRepository) {

        this.payrollService = payrollService;
        this.employeeRepository = employeeRepository;
        this.payrollRuleRepository = payrollRuleRepository;
    }

    @GetMapping
    public String showPayroll(Model model) {
        model.addAttribute("payrollList", payrollService.getAllPayroll());
        model.addAttribute("employees", employeeRepository.findAll());
        return "payroll";
    }

    @GetMapping("/add")
    public String showAddPayroll(Model model) {
        model.addAttribute("employees", employeeRepository.findAll());
        model.addAttribute("payroll", new Payroll());
        return "add-payroll";
    }

    @GetMapping("/attendance-data/{employeeId}")
    public String getAttendanceData(
            @PathVariable Integer employeeId,
            @RequestParam("month") String month,
            Model model) {

        model.addAllAttributes(getAttendanceSummary(employeeId, month));
        return "attendance-data";
    }

    @GetMapping("/attendance-summary/{employeeId}")
    @ResponseBody
    public Map<String, Integer> getAttendanceSummaryJson(
            @PathVariable Integer employeeId,
            @RequestParam("month") String month) {

        return getAttendanceSummary(employeeId, month);
    }

    private Map<String, Integer> getAttendanceSummary(
            Integer employeeId,
            String month) {

        return payrollService.getAttendanceSummary(
                employeeId, parsePayrollMonth(month));
    }

    @GetMapping("/live-attendance/{employeeId}")
    @ResponseBody
    public Map<String, Object> getLiveAttendance(
            @PathVariable Integer employeeId) {

        return payrollService.getTodayAttendanceSummary(employeeId);
    }

    @GetMapping("/employee-data/{employeeId}")
    @ResponseBody
    public BigDecimal getEmployeeBasicSalary(
            @PathVariable Integer employeeId,
            @RequestParam("month") String month) {

        return payrollService.getEffectiveBasicSalary(
                employeeId, parsePayrollMonth(month));
    }

    @GetMapping("/overtime-data/{employeeId}")
    @ResponseBody
    public BigDecimal getEmployeeOvertime(
            @PathVariable Integer employeeId,
            @RequestParam("month") String month) {

        return payrollService.getOvertimeAmount(
                employeeId, parsePayrollMonth(month));
    }

    @GetMapping("/rule-data")
    @ResponseBody
    public Map<String, BigDecimal> getPayrollRuleData() {

        List<PayrollRule> activeRules =
                payrollRuleRepository.findByStatus(PayrollRuleStatus.ACTIVE);

        BigDecimal totalAllowances = BigDecimal.ZERO;
        BigDecimal totalDeductions = BigDecimal.ZERO;
        BigDecimal taxRate = BigDecimal.ZERO;

        for (PayrollRule rule : activeRules) {

            if (rule == null
                    || rule.getRuleType() == null
                    || rule.getRuleValue() == null) {
                continue;
            }

            BigDecimal value = rule.getRuleValue();

            if (value.signum() < 0) {
                throw new IllegalArgumentException(
                        "Active payroll rule values cannot be negative.");
            }

            switch (rule.getRuleType().trim().toUpperCase()) {
                case "ALLOWANCE":
                    totalAllowances = totalAllowances.add(value);
                    break;
                case "DEDUCTION":
                    totalDeductions = totalDeductions.add(value);
                    break;
                case "TAX":
                    taxRate = taxRate.add(value);
                    break;
                default:
                    break;
            }
        }

        Map<String, BigDecimal> result = new HashMap<>();
        result.put("allowances", totalAllowances);
        result.put("deductions", totalDeductions);
        result.put("taxRate", taxRate);

        return result;
    }

    private LocalDate parsePayrollMonth(String month) {

        if (month == null || month.isBlank()) {
            throw new IllegalArgumentException(
                    "Payroll month is required.");
        }

        try {
            if (month.length() == 7) {
                return LocalDate.parse(month + "-01");
            }

            return LocalDate.parse(month).withDayOfMonth(1);

        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "Invalid payroll month. Use yyyy-MM format.");
        }
    }

    @PostMapping("/save")
    public String savePayroll(
            Payroll payroll,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            payrollService.savePayroll(payroll);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Payroll created successfully.");

            return "redirect:/payroll";

        } catch (IllegalArgumentException e) {

            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("employees", employeeRepository.findAll());
            model.addAttribute("payroll", payroll);

            return "add-payroll";
        }
    }

    @GetMapping("/edit/{id}")
    public String editPayroll(
            @PathVariable Integer id,
            Model model) {

        Payroll payroll = payrollService.getPayrollById(id);

        if (payroll == null
                || payroll.getStatus() != PayrollStatus.CALCULATED) {
            return "redirect:/payroll";
        }

        model.addAttribute("payroll", payroll);
        model.addAttribute("employees", employeeRepository.findAll());

        return "edit-payroll";
    }

    @PostMapping("/update")
    public String updatePayroll(
            Payroll payroll,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            payrollService.updatePayroll(payroll);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Payroll updated successfully.");

            return "redirect:/payroll";

        } catch (IllegalArgumentException e) {

            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("employees", employeeRepository.findAll());
            model.addAttribute("payroll", payroll);

            return "edit-payroll";
        }
    }

    @PostMapping("/approve/{id}")
    public String approvePayroll(
            @PathVariable Integer id,
            RedirectAttributes redirectAttributes) {

        try {
            Payroll payroll = payrollService.getPayrollById(id);

            if (payroll == null) {
                throw new IllegalArgumentException(
                        "Payroll record #" + id + " was not found.");
            }

            if (payroll.getStatus() != PayrollStatus.CALCULATED) {
                throw new IllegalArgumentException(
                        "Only CALCULATED payroll can be approved.");
            }

            payrollService.saveStatusOnly(id, PayrollStatus.APPROVED);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Payroll #" + id + " approved successfully.");

        } catch (IllegalArgumentException e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage", e.getMessage());

        } catch (Exception e) {

            log.error("Failed to approve payroll ID {}", id, e);

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Payroll approval failed due to a system error.");
        }

        return "redirect:/payroll";
    }

    @PostMapping("/paid/{id}")
    public String markPayrollAsPaid(
            @PathVariable Integer id,
            RedirectAttributes redirectAttributes) {

        try {
            Payroll payroll = payrollService.getPayrollById(id);

            if (payroll == null) {
                throw new IllegalArgumentException(
                        "Payroll record #" + id + " was not found.");
            }

            if (payroll.getStatus() != PayrollStatus.APPROVED) {
                throw new IllegalArgumentException(
                        "Only APPROVED payroll can be marked as PAID.");
            }

            payrollService.saveStatusOnly(id, PayrollStatus.PAID);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Payroll #" + id + " marked as PAID successfully.");

        } catch (IllegalArgumentException e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage", e.getMessage());

        } catch (Exception e) {

            log.error("Failed to mark payroll ID {} as PAID", id, e);

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Could not mark payroll as PAID due to a system error.");
        }

        return "redirect:/payroll";
    }

    @PostMapping("/delete/{id}")
    public String deletePayroll(
            @PathVariable Integer id,
            RedirectAttributes redirectAttributes) {

        try {
            payrollService.deletePayroll(id);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Payroll #" + id + " deleted successfully.");

        } catch (IllegalArgumentException e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage", e.getMessage());

        } catch (Exception e) {

            log.error("Failed to delete payroll ID {}", id, e);

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Could not delete this payroll record.");
        }

        return "redirect:/payroll";
    }
}
