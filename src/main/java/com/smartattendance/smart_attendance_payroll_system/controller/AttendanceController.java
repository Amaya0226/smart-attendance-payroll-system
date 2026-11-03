
package com.smartattendance.smart_attendance_payroll_system.controller;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.smartattendance.smart_attendance_payroll_system.entity.Attendance;
import com.smartattendance.smart_attendance_payroll_system.entity.AttendanceStatus;
import com.smartattendance.smart_attendance_payroll_system.entity.Employee;
import com.smartattendance.smart_attendance_payroll_system.repository.EmployeeRepository;
import com.smartattendance.smart_attendance_payroll_system.service.AttendanceService;

@Controller
@RequestMapping("/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final EmployeeRepository employeeRepository;

    public AttendanceController(
            AttendanceService attendanceService,
            EmployeeRepository employeeRepository) {

        this.attendanceService = attendanceService;
        this.employeeRepository = employeeRepository;
    }

    // Display Attendance Management Page
    @GetMapping
    public String showAttendance(Model model) {
        loadAttendancePageData(model);
        return "attendance";
    }

    // Temporary Test
    @GetMapping("/test")
    @ResponseBody
    public String testAttendance() {
        return "Attendance Controller is working";
    }

    // =====================================================
    // ADMIN ATTENDANCE REPORT
    // =====================================================

    @GetMapping("/report")
    public String showAttendanceReport(

            @RequestParam(required = false)
            LocalDate fromDate,

            @RequestParam(required = false)
            LocalDate toDate,

            @RequestParam(required = false)
            Integer employeeId,

            @RequestParam(required = false)
            String status,

            Model model) {

        LocalDate today = LocalDate.now();

        if (fromDate == null) {
            fromDate = today.withDayOfMonth(1);
        }

        if (toDate == null) {
            toDate = today;
        }

        // Final variables are required for stream lambda expressions.
        final LocalDate reportFromDate = fromDate;
        final LocalDate reportToDate = toDate;

        List<Employee> employees = employeeRepository.findAll();

        model.addAttribute("employees", employees);
        model.addAttribute("fromDate", reportFromDate);
        model.addAttribute("toDate", reportToDate);
        model.addAttribute("selectedEmployeeId", employeeId);
        model.addAttribute(
                "selectedStatus",
                status == null ? "" : status);

        model.addAttribute(
                "statuses",
                AttendanceStatus.values());

        // Validate date range
        if (reportFromDate.isAfter(reportToDate)) {

            model.addAttribute(
                    "errorMessage",
                    "From Date cannot be after To Date.");

            model.addAttribute("reportRows", List.of());

            addEmptyReportSummary(model);

            return "attendance-report";
        }

        // Read existing attendance records only.
        List<Attendance> records =
                attendanceService.getAllAttendance();

        // Apply report filters.
        List<Attendance> filteredRecords = records.stream()

                .filter(a ->
                        !a.getAttendanceDate()
                                .isBefore(reportFromDate))

                .filter(a ->
                        !a.getAttendanceDate()
                                .isAfter(reportToDate))

                .filter(a ->
                        employeeId == null
                                || employeeId.equals(a.getEmployeeId()))

                .filter(a ->
                        status == null
                                || status.isBlank()
                                || a.getStatus().name().equals(status))

                .sorted(
                        Comparator
                                .comparing(
                                        Attendance::getAttendanceDate)
                                .reversed()
                                .thenComparing(
                                        Attendance::getAttendanceId,
                                        Comparator.reverseOrder()))

                .collect(Collectors.toList());

        // Employee lookup map
        Map<Integer, Employee> employeeMap =
                employees.stream()
                        .collect(Collectors.toMap(
                                Employee::getEmployeeId,
                                employee -> employee,
                                (first, second) -> first));

        // Prepare report rows
        List<AttendanceReportRow> reportRows =
                filteredRecords.stream()
                        .map(attendance ->
                                new AttendanceReportRow(
                                        attendance,
                                        employeeMap.get(
                                                attendance.getEmployeeId())))
                        .collect(Collectors.toList());

        // Status summary
        long presentCount =
                countStatus(
                        filteredRecords,
                        AttendanceStatus.PRESENT);

        long absentCount =
                countStatus(
                        filteredRecords,
                        AttendanceStatus.ABSENT);

        long lateCount =
                countStatus(
                        filteredRecords,
                        AttendanceStatus.LATE);

        long leaveCount =
                countStatus(
                        filteredRecords,
                        AttendanceStatus.LEAVE);

        // Absence confirmation summary
        long confirmedAbsences =
                filteredRecords.stream()
                        .filter(a ->
                                a.getStatus() == AttendanceStatus.ABSENT)
                        .filter(Attendance::isAbsenceConfirmed)
                        .count();

        long pendingAbsences =
                absentCount - confirmedAbsences;

        // Add report data to page
        model.addAttribute("reportRows", reportRows);

        model.addAttribute(
                "totalRecords",
                filteredRecords.size());

        model.addAttribute("presentCount", presentCount);
        model.addAttribute("absentCount", absentCount);
        model.addAttribute("lateCount", lateCount);
        model.addAttribute("leaveCount", leaveCount);

        model.addAttribute(
                "confirmedAbsences",
                confirmedAbsences);

        model.addAttribute(
                "pendingAbsences",
                pendingAbsences);

        return "attendance-report";
    }

    // Count attendance status
    private long countStatus(
            List<Attendance> records,
            AttendanceStatus status) {

        return records.stream()
                .filter(a -> a.getStatus() == status)
                .count();
    }

    // Empty report summary
    private void addEmptyReportSummary(Model model) {

        model.addAttribute("totalRecords", 0);
        model.addAttribute("presentCount", 0);
        model.addAttribute("absentCount", 0);
        model.addAttribute("lateCount", 0);
        model.addAttribute("leaveCount", 0);
        model.addAttribute("confirmedAbsences", 0);
        model.addAttribute("pendingAbsences", 0);
    }

    // Report Row DTO
    public static class AttendanceReportRow {

        private final Attendance attendance;
        private final Employee employee;

        public AttendanceReportRow(
                Attendance attendance,
                Employee employee) {

            this.attendance = attendance;
            this.employee = employee;
        }

        public Attendance getAttendance() {
            return attendance;
        }

        public Employee getEmployee() {
            return employee;
        }

        public String getEmployeeCode() {

            return employee != null
                    ? employee.getEmployeeCode()
                    : "N/A";
        }

        public String getEmployeeName() {

            return employee != null
                    ? employee.getFullName()
                    : "Unknown Employee";
        }

        public String getDepartment() {

            return employee != null
                    && employee.getDepartment() != null
                    ? employee.getDepartment()
                    : "N/A";
        }
    }

    // =====================================================
    // CHECK-IN
    // =====================================================

    @PostMapping("/check-in")
    public String checkIn(
            @RequestParam("employeeId") Integer employeeId,
            Model model) {

        LocalDate today = LocalDate.now();

        Attendance existingAttendance =
                attendanceService.getEmployeeAttendanceForDate(
                        employeeId,
                        today);

        if (existingAttendance != null) {

            model.addAttribute(
                    "errorMessage",
                    "Attendance has already been marked for this employee today.");

            loadAttendancePageData(model);

            return "attendance";
        }

        Attendance attendance = new Attendance();

        attendance.setEmployeeId(employeeId);
        attendance.setAttendanceDate(today);
        attendance.setCheckIn(LocalTime.now());
        attendance.setStatus(AttendanceStatus.PRESENT);
        attendance.setAbsenceConfirmed(false);

        try {

            attendanceService.saveAttendance(attendance);

        } catch (IllegalArgumentException e) {

            model.addAttribute(
                    "errorMessage",
                    e.getMessage());

            loadAttendancePageData(model);

            return "attendance";
        }

        return "redirect:/attendance";
    }

    // =====================================================
    // CHECK-OUT
    // =====================================================

    @PostMapping("/check-out")
    public String checkOut(
            @RequestParam("employeeId") Integer employeeId,
            Model model) {

        LocalDate today = LocalDate.now();

        Attendance attendance =
                attendanceService.getEmployeeAttendanceForDate(
                        employeeId,
                        today);

        if (attendance == null) {

            model.addAttribute(
                    "errorMessage",
                    "Please check in before checking out.");

            loadAttendancePageData(model);

            return "attendance";
        }

        if (attendance.getCheckOut() != null) {

            model.addAttribute(
                    "errorMessage",
                    "Check-out has already been recorded for today.");

            loadAttendancePageData(model);

            return "attendance";
        }

        if (attendance.getStatus() == AttendanceStatus.ABSENT
                || attendance.getStatus() == AttendanceStatus.LEAVE) {

            model.addAttribute(
                    "errorMessage",
                    "Check-out is not allowed for ABSENT or LEAVE records.");

            loadAttendancePageData(model);

            return "attendance";
        }

        attendance.setCheckOut(LocalTime.now());

        try {

            attendanceService.updateAttendance(attendance);

        } catch (IllegalArgumentException e) {

            model.addAttribute(
                    "errorMessage",
                    e.getMessage());

            loadAttendancePageData(model);

            return "attendance";
        }

        return "redirect:/attendance";
    }

    // =====================================================
    // MARK ABSENT
    // =====================================================

    @PostMapping("/mark-absent")
    public String markAbsent(
            @RequestParam("employeeId") Integer employeeId,
            @RequestParam("attendanceDate") LocalDate attendanceDate,
            Model model) {

        if (attendanceDate.isAfter(LocalDate.now())) {

            model.addAttribute(
                    "errorMessage",
                    "Future attendance dates cannot be marked absent.");

            loadAttendancePageData(model);

            return "attendance";
        }

        Attendance existingAttendance =
                attendanceService.getEmployeeAttendanceForDate(
                        employeeId,
                        attendanceDate);

        if (existingAttendance != null) {

            model.addAttribute(
                    "errorMessage",
                    "An attendance record already exists for this employee on that date.");

            loadAttendancePageData(model);

            return "attendance";
        }

        Attendance attendance = new Attendance();

        attendance.setEmployeeId(employeeId);
        attendance.setAttendanceDate(attendanceDate);
        attendance.setStatus(AttendanceStatus.ABSENT);
        attendance.setCheckIn(null);
        attendance.setCheckOut(null);
        attendance.setAbsenceConfirmed(false);

        try {

            attendanceService.saveAttendance(attendance);

        } catch (IllegalArgumentException e) {

            model.addAttribute(
                    "errorMessage",
                    e.getMessage());

            loadAttendancePageData(model);

            return "attendance";
        }

        return "redirect:/attendance";
    }

    // =====================================================
    // CONFIRM ABSENCE
    // =====================================================

    @PostMapping("/confirm-absence/{id}")
    public String confirmAbsence(
            @PathVariable("id") Integer id,
            Model model) {

        try {

            Attendance attendance =
                    attendanceService.getAttendanceById(id);

            if (attendance == null) {
                return "redirect:/attendance";
            }

            if (attendance.getStatus()
                    != AttendanceStatus.ABSENT) {

                model.addAttribute(
                        "errorMessage",
                        "Only ABSENT attendance records can be confirmed.");

                loadAttendancePageData(model);

                return "attendance";
            }

            attendance.setAbsenceConfirmed(true);

            attendanceService.updateAttendance(attendance);

        } catch (IllegalArgumentException e) {

            model.addAttribute(
                    "errorMessage",
                    e.getMessage());

            loadAttendancePageData(model);

            return "attendance";
        }

        return "redirect:/attendance";
    }

    // =====================================================
    // DELETE ATTENDANCE
    // Existing route preserved
    // =====================================================

    @GetMapping("/delete/{id}")
    public String deleteAttendance(
            @PathVariable("id") Integer id) {

        try {

            attendanceService.deleteAttendance(id);

        } catch (IllegalArgumentException e) {

            return "redirect:/attendance";
        }

        return "redirect:/attendance";
    }

    // =====================================================
    // LOAD ATTENDANCE PAGE DATA
    // =====================================================

    private void loadAttendancePageData(Model model) {

        model.addAttribute(
                "attendanceList",
                attendanceService.getAllAttendance());

        model.addAttribute(
                "employees",
                employeeRepository.findAll());
    }
}
