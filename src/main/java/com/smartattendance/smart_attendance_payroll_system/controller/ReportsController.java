
package com.smartattendance.smart_attendance_payroll_system.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.smartattendance.smart_attendance_payroll_system.entity.Attendance;
import com.smartattendance.smart_attendance_payroll_system.entity.Employee;
import com.smartattendance.smart_attendance_payroll_system.entity.Payroll;
import com.smartattendance.smart_attendance_payroll_system.repository.AttendanceRepository;
import com.smartattendance.smart_attendance_payroll_system.repository.EmployeeRepository;
import com.smartattendance.smart_attendance_payroll_system.repository.PayrollRepository;

@Controller
@RequestMapping("/reports")
public class ReportsController {

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final PayrollRepository payrollRepository;

    public ReportsController(
            AttendanceRepository attendanceRepository,
            EmployeeRepository employeeRepository,
            PayrollRepository payrollRepository) {
        this.attendanceRepository = attendanceRepository;
        this.employeeRepository = employeeRepository;
        this.payrollRepository = payrollRepository;
    }

    @GetMapping
    public String reportsHome() {
        return "redirect:/reports/attendance";
    }

    @GetMapping("/attendance")
    public String attendanceReport(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate,
            @RequestParam(required = false) Integer employeeId,
            @RequestParam(required = false) String status,
            Model model) {

        List<Attendance> records = List.of();

        boolean searched = fromDate != null || toDate != null
                || employeeId != null || (status != null && !status.isBlank());

        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            model.addAttribute("errorMessage",
                    "From Date cannot be after To Date.");
        } else if (searched) {
            LocalDate start = fromDate != null ? fromDate : LocalDate.of(1900, 1, 1);
            LocalDate end = toDate != null ? toDate : LocalDate.now();

            records = attendanceRepository
                    .findByAttendanceDateBetween(start, end)
                    .stream()
                    .filter(a -> employeeId == null
                            || employeeId.equals(a.getEmployeeId()))
                    .filter(a -> status == null || status.isBlank()
                            || (a.getStatus() != null
                            && status.equals(a.getStatus().name())))
                    .toList();
        }

        Map<Integer, String> employeeNames = employeeRepository.findAll()
                .stream()
                .collect(Collectors.toMap(
                        Employee::getEmployeeId,
                        e -> e.getFullName() == null
                                ? "Unknown Employee" : e.getFullName(),
                        (first, second) -> first));

        model.addAttribute("employees", employeeRepository.findAll());
        model.addAttribute("records", records);
        model.addAttribute("employeeNames", employeeNames);
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);
        model.addAttribute("selectedEmployeeId", employeeId);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("searched", searched);

        return "attendance-report";
    }

    @GetMapping("/attendance/csv")
    public void exportAttendanceCsv(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate,
            @RequestParam(required = false) Integer employeeId,
            @RequestParam(required = false) String status,
            HttpServletResponse response) throws IOException {

        if (fromDate.isAfter(toDate)) {
            response.sendError(400, "Invalid date range.");
            return;
        }

        List<Attendance> records = attendanceRepository
                .findByAttendanceDateBetween(fromDate, toDate)
                .stream()
                .filter(a -> employeeId == null
                        || employeeId.equals(a.getEmployeeId()))
                .filter(a -> status == null || status.isBlank()
                        || (a.getStatus() != null
                        && status.equals(a.getStatus().name())))
                .toList();

        Map<Integer, String> employeeNames = employeeRepository.findAll()
                .stream()
                .collect(Collectors.toMap(
                        Employee::getEmployeeId,
                        e -> e.getFullName() == null
                                ? "Unknown Employee" : e.getFullName(),
                        (first, second) -> first));

        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename=attendance-report.csv");

        var writer = response.getWriter();
        writer.write("\uFEFFEmployee ID,Employee Name,Date,Check In,Check Out,Status,Absence Confirmed\r\n");

        for (Attendance a : records) {
            writer.write(csv(a.getEmployeeId()));
            writer.write(",");
            writer.write(csv(employeeNames.getOrDefault(
                    a.getEmployeeId(), "Unknown Employee")));
            writer.write(",");
            writer.write(csv(a.getAttendanceDate()));
            writer.write(",");
            writer.write(csv(a.getCheckIn()));
            writer.write(",");
            writer.write(csv(a.getCheckOut()));
            writer.write(",");
            writer.write(csv(a.getStatus()));
            writer.write(",");
            writer.write(csv(a.isAbsenceConfirmed()));
            writer.write("\r\n");
        }

        writer.flush();
    }

    @GetMapping("/payroll")
    public String payrollReport(
            @RequestParam(required = false) String month,
            @RequestParam(required = false) Integer employeeId,
            Model model) {

        List<Payroll> records = List.of();
        boolean searched = month != null && !month.isBlank()
                || employeeId != null;

        if (searched) {
            YearMonth selectedMonth = null;

            if (month != null && !month.isBlank()) {
                try {
                    selectedMonth = YearMonth.parse(month);
                } catch (java.time.format.DateTimeParseException e) {
                    model.addAttribute("errorMessage",
                            "Invalid month. Please select a valid month.");
                }
            }

            if (month == null || month.isBlank()
                    || selectedMonth != null) {
                YearMonth filterMonth = selectedMonth;

                records = payrollRepository.findAll().stream()
                        .filter(p -> filterMonth == null
                                || (p.getPayrollMonth() != null
                                && YearMonth.from(p.getPayrollMonth())
                                        .equals(filterMonth)))
                        .filter(p -> employeeId == null
                                || employeeId.equals(p.getEmployeeId()))
                        .toList();
            }
        }

        Map<Integer, String> employeeNames = employeeRepository.findAll()
                .stream()
                .collect(Collectors.toMap(
                        Employee::getEmployeeId,
                        e -> e.getFullName() == null
                                ? "Unknown Employee" : e.getFullName(),
                        (first, second) -> first));

        model.addAttribute("employees", employeeRepository.findAll());
        model.addAttribute("records", records);
        model.addAttribute("employeeNames", employeeNames);
        model.addAttribute("selectedMonth", month);
        model.addAttribute("selectedEmployeeId", employeeId);
        model.addAttribute("searched", searched);

        return "payroll-report";
    }

    @GetMapping("/payroll/csv")
    public void exportPayrollCsv(
            @RequestParam(required = false) String month,
            @RequestParam(required = false) Integer employeeId,
            HttpServletResponse response) throws IOException {

        YearMonth selectedMonth = null;

        if (month != null && !month.isBlank()) {
            try {
                selectedMonth = YearMonth.parse(month);
            } catch (java.time.format.DateTimeParseException e) {
                response.sendError(400, "Invalid payroll month.");
                return;
            }
        }

        YearMonth filterMonth = selectedMonth;

        List<Payroll> records = payrollRepository.findAll().stream()
                .filter(p -> filterMonth == null
                        || (p.getPayrollMonth() != null
                        && YearMonth.from(p.getPayrollMonth())
                                .equals(filterMonth)))
                .filter(p -> employeeId == null
                        || employeeId.equals(p.getEmployeeId()))
                .toList();

        Map<Integer, String> employeeNames = employeeRepository.findAll()
                .stream()
                .collect(Collectors.toMap(
                        Employee::getEmployeeId,
                        e -> e.getFullName() == null
                                ? "Unknown Employee" : e.getFullName(),
                        (first, second) -> first));

        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename=payroll-report.csv");

        var writer = response.getWriter();
        writer.write("\uFEFFEmployee ID,Employee Name,Payroll Month,Basic Salary,Allowances,Deductions,Tax,Overtime,Net Salary,Status\r\n");

        for (Payroll p : records) {
            writer.write(csv(p.getEmployeeId()));
            writer.write(",");
            writer.write(csv(employeeNames.getOrDefault(
                    p.getEmployeeId(), "Unknown Employee")));
            writer.write(",");
            writer.write(csv(p.getPayrollMonth()));
            writer.write(",");
            writer.write(csv(p.getBasicSalary()));
            writer.write(",");
            writer.write(csv(p.getAllowances()));
            writer.write(",");
            writer.write(csv(p.getDeductions()));
            writer.write(",");
            writer.write(csv(p.getTax()));
            writer.write(",");
            writer.write(csv(p.getOvertime()));
            writer.write(",");
            writer.write(csv(p.getNetSalary()));
            writer.write(",");
            writer.write(csv(p.getStatus()));
            writer.write("\r\n");
        }

        writer.flush();
    }

    private String csv(Object value) {
        if (value == null) {
            return "";
        }

        String text = String.valueOf(value);
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }
}
