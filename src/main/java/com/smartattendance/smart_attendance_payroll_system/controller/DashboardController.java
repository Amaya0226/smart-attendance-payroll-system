
package com.smartattendance.smart_attendance_payroll_system.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.smartattendance.smart_attendance_payroll_system.entity.Attendance;
import com.smartattendance.smart_attendance_payroll_system.entity.Employee;
import com.smartattendance.smart_attendance_payroll_system.entity.Payroll;
import com.smartattendance.smart_attendance_payroll_system.repository.AttendanceRepository;
import com.smartattendance.smart_attendance_payroll_system.repository.EmployeeRepository;
import com.smartattendance.smart_attendance_payroll_system.repository.PayrollRepository;

@Controller
public class DashboardController {

    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;
    private final PayrollRepository payrollRepository;

    public DashboardController(
            EmployeeRepository employeeRepository,
            AttendanceRepository attendanceRepository,
            PayrollRepository payrollRepository) {

        this.employeeRepository = employeeRepository;
        this.attendanceRepository = attendanceRepository;
        this.payrollRepository = payrollRepository;
    }

    // Admin Dashboard: "/dashboard"
    @GetMapping("/dashboard")
    public String showDashboard(Model model) {

        LocalDate today = LocalDate.now();
        YearMonth currentMonth = YearMonth.now();
        LocalDate monthStart = currentMonth.atDay(1);
        LocalDate monthEnd = currentMonth.atEndOfMonth();

        // 1. Employee overview
        List<Employee> employees = employeeRepository.findAll();
        long totalEmployees = employees.size();

        // 2. Today's attendance
        List<Attendance> todayAttendanceList =
                attendanceRepository.findByAttendanceDate(today);

        long todayAttendance = todayAttendanceList.size();

        long presentToday = todayAttendanceList.stream()
                .filter(this::isPresentStatus)
                .count();

        long absentToday = todayAttendanceList.stream()
                .filter(a -> hasStatus(a, "ABSENT"))
                .count();

        long leaveToday = todayAttendanceList.stream()
                .filter(a -> hasStatus(a, "LEAVE"))
                .count();

        long notMarkedToday =
                Math.max(0L, totalEmployees - todayAttendance);

        // 3. Current month attendance, up to today
        LocalDate attendanceEnd =
                today.isBefore(monthEnd) ? today : monthEnd;

        List<Attendance> monthlyAttendanceList =
                attendanceRepository.findByAttendanceDateBetween(
                        monthStart, attendanceEnd);

        long monthlyPresent = monthlyAttendanceList.stream()
                .filter(this::isPresentStatus)
                .count();

        long monthlyAbsent = monthlyAttendanceList.stream()
                .filter(a -> hasStatus(a, "ABSENT"))
                .count();

        long monthlyLeave = monthlyAttendanceList.stream()
                .filter(a -> hasStatus(a, "LEAVE"))
                .count();

        long monthlyAttendanceTotal = monthlyAttendanceList.size();

        List<String> attendanceLabels = new ArrayList<>();
        List<Long> attendancePresentData = new ArrayList<>();
        List<Long> attendanceAbsentData = new ArrayList<>();
        List<Long> attendanceLeaveData = new ArrayList<>();

        Map<LocalDate, long[]> dailyCounts = new HashMap<>();

        for (Attendance attendance : monthlyAttendanceList) {

            if (attendance == null
                    || attendance.getAttendanceDate() == null) {
                continue;
            }

            LocalDate date = attendance.getAttendanceDate();

            long[] counts = dailyCounts.computeIfAbsent(
                    date, key -> new long[3]);

            if (isPresentStatus(attendance)) {
                counts[0]++;
            } else if (hasStatus(attendance, "ABSENT")) {
                counts[1]++;
            } else if (hasStatus(attendance, "LEAVE")) {
                counts[2]++;
            }
        }

        // Only show days up to today in the current month chart
        for (int day = 1; day <= today.getDayOfMonth(); day++) {

            LocalDate date = currentMonth.atDay(day);

            long[] counts =
                    dailyCounts.getOrDefault(date, new long[3]);

            attendanceLabels.add(String.valueOf(day));
            attendancePresentData.add(counts[0]);
            attendanceAbsentData.add(counts[1]);
            attendanceLeaveData.add(counts[2]);
        }

        // 4. Monthly payroll summary
        List<Payroll> monthlyPayrollList =
                payrollRepository.findByPayrollMonth(monthStart);

        BigDecimal monthlyNetSalary = BigDecimal.ZERO;
        BigDecimal monthlyAllowances = BigDecimal.ZERO;
        BigDecimal monthlyDeductions = BigDecimal.ZERO;
        BigDecimal monthlyTax = BigDecimal.ZERO;

        Set<Integer> employeesWithPayroll = new HashSet<>();

        for (Payroll payroll : monthlyPayrollList) {

            if (payroll == null) {
                continue;
            }

            if (payroll.getNetSalary() != null) {
                monthlyNetSalary =
                        monthlyNetSalary.add(payroll.getNetSalary());
            }

            if (payroll.getAllowances() != null) {
                monthlyAllowances =
                        monthlyAllowances.add(payroll.getAllowances());
            }

            if (payroll.getDeductions() != null) {
                monthlyDeductions =
                        monthlyDeductions.add(payroll.getDeductions());
            }

            if (payroll.getTax() != null) {
                monthlyTax = monthlyTax.add(payroll.getTax());
            }

            if (payroll.getEmployeeId() != null) {
                employeesWithPayroll.add(payroll.getEmployeeId());
            }
        }

        long monthlyPayrollCount = monthlyPayrollList.size();

        long employeesWithoutPayroll =
                Math.max(
                        0L,
                        totalEmployees - employeesWithPayroll.size());

        long monthlyPayrollPending = employeesWithoutPayroll;

        // 5. Recent attendance
        List<Attendance> recentAttendanceList =
                attendanceRepository
                        .findTop10ByOrderByAttendanceDateDescAttendanceIdDesc();

        Map<Integer, String> employeeNames = new HashMap<>();

        for (Attendance attendance : recentAttendanceList) {

            if (attendance == null
                    || attendance.getEmployeeId() == null) {
                continue;
            }

            Integer employeeId = attendance.getEmployeeId();

            if (employeeNames.containsKey(employeeId)) {
                continue;
            }

            Employee employee =
                    employeeRepository.findById(employeeId).orElse(null);

            if (employee != null && employee.getFullName() != null) {

                employeeNames.put(
                        employeeId, employee.getFullName());

            } else {

                employeeNames.put(
                        employeeId, "Unknown Employee");
            }
        }

        // 6. Thymeleaf model attributes
        model.addAttribute("totalEmployees", totalEmployees);
        model.addAttribute("todayAttendance", todayAttendance);
        model.addAttribute("presentToday", presentToday);
        model.addAttribute("absentToday", absentToday);
        model.addAttribute("leaveToday", leaveToday);
        model.addAttribute("notMarkedToday", notMarkedToday);

        model.addAttribute("monthlyPresent", monthlyPresent);
        model.addAttribute("monthlyAbsent", monthlyAbsent);
        model.addAttribute("monthlyLeave", monthlyLeave);

        model.addAttribute(
                "monthlyAttendanceTotal", monthlyAttendanceTotal);

        model.addAttribute("attendanceLabels", attendanceLabels);

        model.addAttribute(
                "attendancePresentData", attendancePresentData);

        model.addAttribute(
                "attendanceAbsentData", attendanceAbsentData);

        model.addAttribute(
                "attendanceLeaveData", attendanceLeaveData);

        model.addAttribute(
                "recentAttendanceList", recentAttendanceList);

        model.addAttribute("employeeNames", employeeNames);

        model.addAttribute("currentMonth", currentMonth.toString());

        model.addAttribute(
                "monthlyPayrollCount", monthlyPayrollCount);

        model.addAttribute(
                "monthlyPayrollPending", monthlyPayrollPending);

        model.addAttribute(
                "employeesWithoutPayroll", employeesWithoutPayroll);

        model.addAttribute("monthlyNetSalary", monthlyNetSalary);
        model.addAttribute("monthlyAllowances", monthlyAllowances);
        model.addAttribute("monthlyDeductions", monthlyDeductions);
        model.addAttribute("monthlyTax", monthlyTax);

        return "dashboard";
    }

    private boolean isPresentStatus(Attendance attendance) {

        return hasStatus(attendance, "PRESENT")
                || hasStatus(attendance, "LATE");
    }

    private boolean hasStatus(
            Attendance attendance, String expectedStatus) {

        return attendance != null
                && attendance.getStatus() != null
                && expectedStatus.equals(
                        attendance.getStatus().name());
    }
}
