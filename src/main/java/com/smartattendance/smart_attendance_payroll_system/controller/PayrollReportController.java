
package com.smartattendance.smart_attendance_payroll_system.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.smartattendance.smart_attendance_payroll_system.entity.Payroll;
import com.smartattendance.smart_attendance_payroll_system.repository.PayrollRepository;
import com.smartattendance.smart_attendance_payroll_system.service.PayrollService;

@Controller
public class PayrollReportController {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final PayrollRepository payrollRepository;
    private final PayrollService payrollService;

    public PayrollReportController(
            PayrollRepository payrollRepository,
            PayrollService payrollService) {

        this.payrollRepository = payrollRepository;
        this.payrollService = payrollService;
    }

    @GetMapping("/payroll/report")
    public String monthlyPayrollReport(
            @RequestParam(required = false) String month,
            Model model) {

        YearMonth currentMonth = YearMonth.now();
        YearMonth selectedMonth = currentMonth;

        List<String> validationWarnings = new ArrayList<>();
        List<String> attendanceInformation = new ArrayList<>();
        List<Payroll> payrollRecords = new ArrayList<>();

        List<Map<String, Object>> currentAttendanceSummaries =
                new ArrayList<>();

        boolean validMonth = true;
        boolean futureMonth = false;

        // 1. Validate selected month.
        if (month != null && !month.isBlank()) {
            try {
                selectedMonth = YearMonth.parse(month.trim());
            } catch (DateTimeParseException ex) {
                validMonth = false;

                model.addAttribute(
                        "errorMessage",
                        "Invalid month format. Please select a valid month.");
            }
        }

        // 2. Prevent future-month reports.
        if (validMonth && selectedMonth.isAfter(currentMonth)) {
            futureMonth = true;

            model.addAttribute(
                    "errorMessage",
                    "A payroll report cannot be generated for a future month.");

            validationWarnings.add(
                    "The selected month is in the future. "
                    + "No payroll records were loaded.");
        }

        // 3. Retrieve stored payroll records.
        if (validMonth && !futureMonth) {

            LocalDate startDate = selectedMonth.atDay(1);
            LocalDate endDate = selectedMonth.atEndOfMonth();

            List<Payroll> records =
                    payrollRepository.findByPayrollMonthBetween(
                            startDate,
                            endDate);

            if (records != null) {
                payrollRecords = records;
            }

            if (payrollRecords.isEmpty()) {
                model.addAttribute(
                        "infoMessage",
                        "No payroll records are available for "
                        + selectedMonth + ".");
            }
        }

        // 4. Financial totals: read only.
        BigDecimal totalBasic = ZERO;
        BigDecimal totalAllowances = ZERO;
        BigDecimal totalOvertime = ZERO;
        BigDecimal totalDeductions = ZERO;
        BigDecimal totalTax = ZERO;
        BigDecimal totalNetSalary = ZERO;

        int negativeAmountCount = 0;
        int invalidSalaryCount = 0;

        // Stored payroll attendance snapshot validation.
        int incompleteSnapshotCount = 0;
        int inconsistentSnapshotCount = 0;

        // Current attendance summary validation.
        int currentIncompleteAttendanceCount = 0;
        int currentInconsistentAttendanceCount = 0;
        int currentSummaryErrorCount = 0;

        int snapshotDifferenceCount = 0;

        /*
         * Blocking validation errors are kept separate from
         * attendance-completeness warnings.
         *
         * Missing attendance and snapshot differences alone do not
         * block report review. They remain visible as warnings/info.
         */
        int blockingValidationErrorCount = 0;

        // 5. Validate each stored payroll record.
        for (Payroll payroll : payrollRecords) {

            if (payroll == null) {
                continue;
            }

            BigDecimal basic = safeAmount(payroll.getBasicSalary());
            BigDecimal allowances = safeAmount(payroll.getAllowances());
            BigDecimal overtime = safeAmount(payroll.getOvertime());
            BigDecimal deductions = safeAmount(payroll.getDeductions());
            BigDecimal tax = safeAmount(payroll.getTax());
            BigDecimal netSalary = safeAmount(payroll.getNetSalary());

            // 6. Add stored financial values to report totals.
            totalBasic = totalBasic.add(basic);
            totalAllowances = totalAllowances.add(allowances);
            totalOvertime = totalOvertime.add(overtime);
            totalDeductions = totalDeductions.add(deductions);
            totalTax = totalTax.add(tax);
            totalNetSalary = totalNetSalary.add(netSalary);

            // 7. Negative financial values.
            if (basic.signum() < 0
                    || allowances.signum() < 0
                    || overtime.signum() < 0
                    || deductions.signum() < 0
                    || tax.signum() < 0
                    || netSalary.signum() < 0) {

                negativeAmountCount++;
                blockingValidationErrorCount++;
            }

            // 8. Validate stored net salary.
            // Basic + Allowances + Overtime - Deductions - Tax.
            BigDecimal expectedNetSalary = basic
                    .add(allowances)
                    .add(overtime)
                    .subtract(deductions)
                    .subtract(tax)
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal actualNetSalary =
                    netSalary.setScale(2, RoundingMode.HALF_UP);

            if (expectedNetSalary.compareTo(actualNetSalary) != 0) {
                invalidSalaryCount++;
                blockingValidationErrorCount++;
            }

            // 9. Validate saved attendance snapshot.
            int savedWorkingDays = safeDays(payroll.getWorkingDays());
            int savedPresentDays = safeDays(payroll.getPresentDays());
            int savedAbsentDays = safeDays(payroll.getAbsentDays());
            int savedLeaveDays = safeDays(payroll.getLeaveDays());

            long savedClassifiedDays =
                    (long) savedPresentDays
                    + savedAbsentDays
                    + savedLeaveDays;

            boolean invalidSavedAttendance =
                    savedWorkingDays < 0
                    || savedPresentDays < 0
                    || savedAbsentDays < 0
                    || savedLeaveDays < 0
                    || savedClassifiedDays > savedWorkingDays;

            if (invalidSavedAttendance) {
                inconsistentSnapshotCount++;
                blockingValidationErrorCount++;

            } else if (savedClassifiedDays < savedWorkingDays) {
                incompleteSnapshotCount++;
            }

            // 10. Read current attendance summary.
            // This does not recalculate or save the payroll record.
            try {
                if (payroll.getEmployeeId() == null
                        || payroll.getPayrollMonth() == null) {

                    currentSummaryErrorCount++;
                    blockingValidationErrorCount++;
                    continue;
                }

                Map<String, Integer> summary =
                        payrollService.getAttendanceSummary(
                                payroll.getEmployeeId(),
                                payroll.getPayrollMonth());

                if (summary == null) {
                    currentSummaryErrorCount++;
                    blockingValidationErrorCount++;
                    continue;
                }

                int currentWorkingDays =
                        summary.getOrDefault("workingDays", 0);

                int currentPresentDays =
                        summary.getOrDefault("presentDays", 0);

                int currentAbsentDays =
                        summary.getOrDefault("absentDays", 0);

                int currentLeaveDays =
                        summary.getOrDefault("leaveDays", 0);

                int currentMissingDays =
                        summary.getOrDefault("missingAttendanceDays", 0);

                long currentClassifiedDays =
                        (long) currentPresentDays
                        + currentAbsentDays
                        + currentLeaveDays;

                boolean invalidCurrentAttendance =
                        currentWorkingDays < 0
                        || currentPresentDays < 0
                        || currentAbsentDays < 0
                        || currentLeaveDays < 0
                        || currentMissingDays < 0
                        || currentClassifiedDays > currentWorkingDays
                        || currentClassifiedDays + currentMissingDays
                                != currentWorkingDays;

                if (invalidCurrentAttendance) {
                    currentInconsistentAttendanceCount++;
                    blockingValidationErrorCount++;

                } else if (currentMissingDays > 0) {
                    currentIncompleteAttendanceCount++;
                }

                // Store saved and current counts for the HTML table.
                Map<String, Object> attendanceRow = new HashMap<>();

                attendanceRow.put("payrollId", payroll.getPayrollId());
                attendanceRow.put("employeeId", payroll.getEmployeeId());
                attendanceRow.put("payrollMonth", payroll.getPayrollMonth());

                attendanceRow.put("savedWorkingDays", savedWorkingDays);
                attendanceRow.put("savedPresentDays", savedPresentDays);
                attendanceRow.put("savedAbsentDays", savedAbsentDays);
                attendanceRow.put("savedLeaveDays", savedLeaveDays);

                attendanceRow.put("currentWorkingDays", currentWorkingDays);
                attendanceRow.put("currentPresentDays", currentPresentDays);
                attendanceRow.put("currentAbsentDays", currentAbsentDays);
                attendanceRow.put("currentLeaveDays", currentLeaveDays);
                attendanceRow.put("currentMissingDays", currentMissingDays);

                boolean snapshotDiffers =
                        savedWorkingDays != currentWorkingDays
                        || savedPresentDays != currentPresentDays
                        || savedAbsentDays != currentAbsentDays
                        || savedLeaveDays != currentLeaveDays;

                attendanceRow.put("snapshotDiffers", snapshotDiffers);

                currentAttendanceSummaries.add(attendanceRow);

                if (snapshotDiffers) {
                    snapshotDifferenceCount++;
                }

            } catch (RuntimeException ex) {
                // One summary failure must not crash the entire report.
                currentSummaryErrorCount++;
                blockingValidationErrorCount++;
            }
        }

        // 11. Financial validation warnings.
        if (negativeAmountCount > 0) {
            validationWarnings.add(
                    negativeAmountCount
                    + " payroll record(s) contain negative financial amounts. "
                    + "Please verify the stored salary values.");
        }

        if (invalidSalaryCount > 0) {
            validationWarnings.add(
                    invalidSalaryCount
                    + " payroll record(s) have net salary totals that do not "
                    + "match Basic + Allowances + Overtime - Deductions - Tax.");
        }

        // 12. Saved attendance snapshot validation.
        if (inconsistentSnapshotCount > 0) {
            validationWarnings.add(
                    inconsistentSnapshotCount
                    + " saved payroll attendance snapshot(s) have invalid "
                    + "day counts. Check for negative values or classified "
                    + "days exceeding working days.");
        }

        if (incompleteSnapshotCount > 0) {
            validationWarnings.add(
                    incompleteSnapshotCount
                    + " saved payroll attendance snapshot(s) have "
                    + "unclassified working days. This describes the counts "
                    + "stored in payroll; it does not automatically mean "
                    + "that the current attendance records are incomplete.");
        }

        // 13. Current attendance summary validation.
        if (currentInconsistentAttendanceCount > 0) {
            validationWarnings.add(
                    currentInconsistentAttendanceCount
                    + " current attendance summary/summaries have "
                    + "inconsistent day counts. Please check attendance "
                    + "calculation and holiday configuration.");
        }

        if (currentIncompleteAttendanceCount > 0) {
            validationWarnings.add(
                    currentIncompleteAttendanceCount
                    + " employee attendance summary/summaries contain "
                    + "working days without a classified attendance status. "
                    + "Missing days are not automatically treated as absences.");
        }

        if (currentSummaryErrorCount > 0) {
            validationWarnings.add(
                    "Could not generate the current attendance summary for "
                    + currentSummaryErrorCount
                    + " payroll record(s). Check the application logs and "
                    + "employee/month data.");
        }

        // Saved/current attendance differences are informational.
        if (snapshotDifferenceCount > 0) {
            attendanceInformation.add(
                    snapshotDifferenceCount
                    + " payroll record(s) have attendance counts that differ "
                    + "from the current attendance summary. The report has "
                    + "not updated or recalculated those payroll records.");
        }

        // 14. Pass report data to Thymeleaf.
        model.addAttribute("payrollRecords", payrollRecords);

        model.addAttribute(
                "selectedMonth",
                validMonth
                        ? selectedMonth.toString()
                        : (month == null ? "" : month.trim()));

        model.addAttribute(
                "monthLabel",
                validMonth
                        ? selectedMonth.toString()
                        : "Invalid month");

        model.addAttribute("totalEmployees", payrollRecords.size());

        model.addAttribute("totalBasic", totalBasic);
        model.addAttribute("totalAllowances", totalAllowances);
        model.addAttribute("totalOvertime", totalOvertime);
        model.addAttribute("totalDeductions", totalDeductions);
        model.addAttribute("totalTax", totalTax);
        model.addAttribute("totalNetSalary", totalNetSalary);

        model.addAttribute("validationWarnings", validationWarnings);
        model.addAttribute(
                "hasValidationWarnings",
                !validationWarnings.isEmpty());

        model.addAttribute(
                "attendanceInformation",
                attendanceInformation);

        model.addAttribute(
                "currentAttendanceSummaries",
                currentAttendanceSummaries);

        model.addAttribute(
                "hasBlockingValidationErrors",
                blockingValidationErrorCount > 0);

        /*
         * Report review readiness is separate from attendance completeness.
         * Incomplete attendance remains visible as a warning.
         */
        model.addAttribute(
                "reportCanBeReviewed",
                validMonth
                        && !futureMonth
                        && !payrollRecords.isEmpty()
                        && blockingValidationErrorCount == 0);

        return "payroll-report";
    }

    private BigDecimal safeAmount(BigDecimal amount) {
        return amount == null ? ZERO : amount;
    }

    private int safeDays(Integer days) {
        return days == null ? 0 : days;
    }
}
