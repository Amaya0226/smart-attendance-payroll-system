
package com.smartattendance.smart_attendance_payroll_system.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartattendance.smart_attendance_payroll_system.entity.Attendance;
import com.smartattendance.smart_attendance_payroll_system.entity.AttendanceStatus;
import com.smartattendance.smart_attendance_payroll_system.entity.Employee;
import com.smartattendance.smart_attendance_payroll_system.entity.Holiday;
import com.smartattendance.smart_attendance_payroll_system.entity.Overtime;
import com.smartattendance.smart_attendance_payroll_system.entity.Payroll;
import com.smartattendance.smart_attendance_payroll_system.entity.PayrollRule;
import com.smartattendance.smart_attendance_payroll_system.entity.PayrollRuleStatus;
import com.smartattendance.smart_attendance_payroll_system.entity.PayrollStatus;
import com.smartattendance.smart_attendance_payroll_system.entity.SalaryRevision;

import com.smartattendance.smart_attendance_payroll_system.repository.AttendanceRepository;
import com.smartattendance.smart_attendance_payroll_system.repository.EmployeeRepository;
import com.smartattendance.smart_attendance_payroll_system.repository.OvertimeRepository;
import com.smartattendance.smart_attendance_payroll_system.repository.PayrollRepository;
import com.smartattendance.smart_attendance_payroll_system.repository.PayrollRuleRepository;
import com.smartattendance.smart_attendance_payroll_system.repository.SalaryRevisionRepository;

@Service
public class PayrollService {

    private final PayrollRepository payrollRepository;
    private final PayrollRuleRepository payrollRuleRepository;
    private final OvertimeRepository overtimeRepository;
    private final SalaryRevisionRepository salaryRevisionRepository;
    private final AttendanceRepository attendanceRepository;
    private final HolidayService holidayService;
    private final EmployeeRepository employeeRepository;

    private static final BigDecimal ZERO =
            BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private static final BigDecimal ONE_HUNDRED =
            new BigDecimal("100");

    public PayrollService(
            PayrollRepository payrollRepository,
            PayrollRuleRepository payrollRuleRepository,
            OvertimeRepository overtimeRepository,
            SalaryRevisionRepository salaryRevisionRepository,
            AttendanceRepository attendanceRepository,
            HolidayService holidayService,
            EmployeeRepository employeeRepository) {

        this.payrollRepository = payrollRepository;
        this.payrollRuleRepository = payrollRuleRepository;
        this.overtimeRepository = overtimeRepository;
        this.salaryRevisionRepository = salaryRevisionRepository;
        this.attendanceRepository = attendanceRepository;
        this.holidayService = holidayService;
        this.employeeRepository = employeeRepository;
    }

    // --------------------------------------------------
    // GET ALL PAYROLL
    // --------------------------------------------------

    public List<Payroll> getAllPayroll() {
        return payrollRepository.findAll();
    }

    // --------------------------------------------------
    // GET PAYROLL BY ID
    // --------------------------------------------------

    public Payroll getPayrollById(Integer id) {

        if (id == null) {
            return null;
        }

        return payrollRepository.findById(id).orElse(null);
    }

    // --------------------------------------------------
    // GET EMPLOYEE
    // --------------------------------------------------

    private Employee getEmployee(Integer employeeId) {

        if (employeeId == null) {
            throw new IllegalArgumentException(
                    "Employee is required.");
        }

        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Selected employee was not found."));
    }

    // --------------------------------------------------
    // GET EFFECTIVE PAYROLL START DATE
    // --------------------------------------------------

    private LocalDate getEffectivePayrollStartDate(
            Employee employee,
            LocalDate payrollMonth) {

        LocalDate firstDay = payrollMonth.withDayOfMonth(1);

        LocalDate joiningDate = employee.getJoiningDate();

        if (joiningDate == null) {
            throw new IllegalArgumentException(
                    "Employee joining date is missing.");
        }

        return joiningDate.isAfter(firstDay)
                ? joiningDate
                : firstDay;
    }

    // --------------------------------------------------
    // EFFECTIVE BASIC SALARY
    // --------------------------------------------------

    public BigDecimal getEffectiveBasicSalary(
            Integer employeeId,
            LocalDate payrollMonth) {

        if (employeeId == null || payrollMonth == null) {
            throw new IllegalArgumentException(
                    "Employee and payroll month are required.");
        }

        Employee employee = getEmployee(employeeId);

        BigDecimal basicSalary = employee.getBasicSalary();

        validateNonNegative(
                basicSalary,
                "Employee basic salary");

        LocalDate firstDay = payrollMonth.withDayOfMonth(1);

        SalaryRevision revision = salaryRevisionRepository
                .findTopByEmployeeIdAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(
                        employeeId, firstDay);

        if (revision != null) {

            BigDecimal revisedSalary = revision.getNewSalary();

            validateNonNegative(
                    revisedSalary,
                    "Applicable revised salary");

            basicSalary = revisedSalary;
        }

        return money(basicSalary);
    }

    // --------------------------------------------------
    // ATTENDANCE SUMMARY
    // Payroll calculation uses completed days only.
    // Employee joining date is respected.
    // --------------------------------------------------

    public Map<String, Integer> getAttendanceSummary(
            Integer employeeId,
            LocalDate payrollMonth) {

        Map<String, Integer> result = new HashMap<>();

        result.put("workingDays", 0);
        result.put("presentDays", 0);
        result.put("absentDays", 0);
        result.put("leaveDays", 0);
        result.put("missingAttendanceDays", 0);

        if (employeeId == null || payrollMonth == null) {
            return result;
        }

        Employee employee = getEmployee(employeeId);

        LocalDate firstDay = payrollMonth.withDayOfMonth(1);

        LocalDate monthEnd = firstDay.withDayOfMonth(
                firstDay.lengthOfMonth());

        LocalDate today = LocalDate.now();

        YearMonth selectedMonth = YearMonth.from(firstDay);
        YearMonth currentMonth = YearMonth.from(today);

        LocalDate lastCompletedDay;

        if (selectedMonth.isAfter(currentMonth)) {
            return result;

        } else if (selectedMonth.equals(currentMonth)) {

            // Today's attendance is displayed separately.
            // It is not included in payroll calculations.
            lastCompletedDay = today.minusDays(1);

        } else {
            lastCompletedDay = monthEnd;
        }

        LocalDate effectiveStartDate =
                getEffectivePayrollStartDate(employee, firstDay);

        if (effectiveStartDate.isAfter(lastCompletedDay)) {
            return result;
        }

        Set<LocalDate> holidayDates = getHolidayDates(
                effectiveStartDate,
                lastCompletedDay);

        int workingDays = 0;

        for (LocalDate date = effectiveStartDate;
                !date.isAfter(lastCompletedDay);
                date = date.plusDays(1)) {

            if (isWorkingDay(date, holidayDates)) {
                workingDays++;
            }
        }

        /*
         * One effective attendance status per employee per working date.
         *
         * Priority:
         * PRESENT / LATE > LEAVE > confirmed ABSENT.
         *
         * Missing attendance is never automatically treated as absence.
         */

        Map<LocalDate, Integer> dailyStatus = new HashMap<>();

        List<Attendance> attendanceList =
                attendanceRepository.findByEmployeeId(employeeId);

        if (attendanceList != null) {

            for (Attendance attendance : attendanceList) {

                if (attendance == null) {
                    continue;
                }

                LocalDate date = attendance.getAttendanceDate();

                if (date == null
                        || date.isBefore(effectiveStartDate)
                        || date.isAfter(lastCompletedDay)
                        || !isWorkingDay(date, holidayDates)
                        || attendance.getStatus() == null) {
                    continue;
                }

                int newStatus = 0;

                if (attendance.getStatus() == AttendanceStatus.PRESENT
                        || attendance.getStatus() == AttendanceStatus.LATE) {

                    newStatus = 3;

                } else if (attendance.getStatus() == AttendanceStatus.LEAVE) {

                    newStatus = 2;

                } else if (attendance.getStatus() == AttendanceStatus.ABSENT
                        && attendance.isAbsenceConfirmed()) {

                    newStatus = 1;
                }

                if (newStatus == 0) {
                    continue;
                }

                int existingStatus =
                        dailyStatus.getOrDefault(date, 0);

                if (newStatus > existingStatus) {
                    dailyStatus.put(date, newStatus);
                }
            }
        }

        int presentDays = 0;
        int leaveDays = 0;
        int absentDays = 0;

        for (Integer status : dailyStatus.values()) {

            if (status == 3) {
                presentDays++;

            } else if (status == 2) {
                leaveDays++;

            } else if (status == 1) {
                absentDays++;
            }
        }

        int classifiedDays =
                presentDays + leaveDays + absentDays;

        if (classifiedDays > workingDays) {
            throw new IllegalStateException(
                    "Attendance validation failed: classified attendance "
                    + "days exceed working days for employee "
                    + employeeId + ".");
        }

        int missingAttendanceDays =
                workingDays - classifiedDays;

        result.put("workingDays", workingDays);
        result.put("presentDays", presentDays);
        result.put("absentDays", absentDays);
        result.put("leaveDays", leaveDays);
        result.put("missingAttendanceDays", missingAttendanceDays);

        return result;
    }

    // --------------------------------------------------
    // LIVE ATTENDANCE SUMMARY
    // Today's attendance is separate from payroll totals.
    // PRESENT and LATE are displayed separately.
    // --------------------------------------------------

    public Map<String, Object> getTodayAttendanceSummary(
            Integer employeeId) {

        Map<String, Object> result = new HashMap<>();

        LocalDate today = LocalDate.now();

        result.put("date", today.toString());
        result.put("workingDay", false);
        result.put("attendanceRecorded", false);
        result.put("status", "NOT_MARKED");
        result.put("message",
                "Today's attendance has not been marked.");

        if (employeeId == null) {
            result.put("message", "Employee is required.");
            return result;
        }

        Employee employee = getEmployee(employeeId);

        LocalDate joiningDate = employee.getJoiningDate();

        if (joiningDate != null && today.isBefore(joiningDate)) {

            result.put("status", "NOT_JOINED");
            result.put("message",
                    "Today is before the employee joining date.");

            return result;
        }

        Set<LocalDate> holidayDates =
                getHolidayDates(today, today);

        if (!isWorkingDay(today, holidayDates)) {

            result.put("status", "NON_WORKING_DAY");

            result.put("message",
                    holidayDates.contains(today)
                            ? "Today is a public/company holiday."
                            : "Today is a non-working day.");

            return result;
        }

        result.put("workingDay", true);

        List<Attendance> attendanceList =
                attendanceRepository.findByEmployeeId(employeeId);

        /*
         * Live attendance priority:
         *
         * PRESENT          = 4
         * LATE             = 3
         * LEAVE            = 2
         * Confirmed ABSENT = 1
         * NOT MARKED       = 0
         *
         * This priority is only for today's live display.
         * Monthly payroll PRESENT/LATE counting is unchanged.
         */

        int effectiveStatus = 0;

        if (attendanceList != null) {

            for (Attendance attendance : attendanceList) {

                if (attendance == null
                        || attendance.getAttendanceDate() == null
                        || !today.equals(attendance.getAttendanceDate())
                        || attendance.getStatus() == null) {

                    continue;
                }

                int currentStatus = 0;

                if (attendance.getStatus()
                        == AttendanceStatus.PRESENT) {

                    currentStatus = 4;

                } else if (attendance.getStatus()
                        == AttendanceStatus.LATE) {

                    currentStatus = 3;

                } else if (attendance.getStatus()
                        == AttendanceStatus.LEAVE) {

                    currentStatus = 2;

                } else if (attendance.getStatus()
                        == AttendanceStatus.ABSENT
                        && attendance.isAbsenceConfirmed()) {

                    currentStatus = 1;
                }

                effectiveStatus = Math.max(
                        effectiveStatus,
                        currentStatus);
            }
        }

        if (effectiveStatus == 4) {

            result.put("attendanceRecorded", true);
            result.put("status", "PRESENT");
            result.put("message",
                    "Today's attendance has been recorded.");

        } else if (effectiveStatus == 3) {

            result.put("attendanceRecorded", true);
            result.put("status", "LATE");
            result.put("message",
                    "Today's attendance has been recorded as late.");

        } else if (effectiveStatus == 2) {

            result.put("attendanceRecorded", true);
            result.put("status", "LEAVE");
            result.put("message",
                    "Leave has been recorded for today.");

        } else if (effectiveStatus == 1) {

            result.put("attendanceRecorded", true);
            result.put("status", "ABSENT");
            result.put("message",
                    "Today's absence has been confirmed.");

        } else {

            result.put("status", "NOT_MARKED");
            result.put("message",
                    "Today's attendance has not been marked yet.");
        }

        return result;
    }

    // --------------------------------------------------
    // HOLIDAY DATES
    // --------------------------------------------------

    private Set<LocalDate> getHolidayDates(
            LocalDate startDate,
            LocalDate endDate) {

        Set<LocalDate> holidayDates = new HashSet<>();

        if (startDate == null
                || endDate == null
                || endDate.isBefore(startDate)) {
            return holidayDates;
        }

        List<Holiday> holidays =
                holidayService.getHolidaysBetween(
                        startDate,
                        endDate);

        if (holidays != null) {

            for (Holiday holiday : holidays) {

                if (holiday != null
                        && holiday.getHolidayDate() != null) {

                    holidayDates.add(holiday.getHolidayDate());
                }
            }
        }

        return holidayDates;
    }

    // --------------------------------------------------
    // VALIDATE ATTENDANCE SUMMARY
    // --------------------------------------------------

    private void validateAttendanceSummary(
            Map<String, Integer> summary) {

        if (summary == null) {
            throw new IllegalStateException(
                    "Attendance summary could not be generated.");
        }

        int workingDays = summary.getOrDefault("workingDays", 0);
        int presentDays = summary.getOrDefault("presentDays", 0);
        int absentDays = summary.getOrDefault("absentDays", 0);
        int leaveDays = summary.getOrDefault("leaveDays", 0);

        int missingDays =
                summary.getOrDefault("missingAttendanceDays", 0);

        if (workingDays < 0
                || presentDays < 0
                || absentDays < 0
                || leaveDays < 0
                || missingDays < 0) {

            throw new IllegalStateException(
                    "Attendance validation failed: negative day counts found.");
        }

        if (presentDays + absentDays + leaveDays + missingDays
                != workingDays) {

            throw new IllegalStateException(
                    "Attendance validation failed: working days do not "
                    + "match the attendance summary.");
        }
    }

    // --------------------------------------------------
    // CHECK WORKING DAY
    // --------------------------------------------------

    private boolean isWorkingDay(
            LocalDate date,
            Set<LocalDate> holidayDates) {

        DayOfWeek day = date.getDayOfWeek();

        boolean weekend =
                day == DayOfWeek.SATURDAY
                || day == DayOfWeek.SUNDAY;

        return !weekend && !holidayDates.contains(date);
    }

    // --------------------------------------------------
    // LAST COMPLETED DAY OF PAYROLL MONTH
    // --------------------------------------------------

    private LocalDate getLastCompletedDay(LocalDate payrollMonth) {

        LocalDate firstDay = payrollMonth.withDayOfMonth(1);

        LocalDate monthEnd = firstDay.withDayOfMonth(
                firstDay.lengthOfMonth());

        LocalDate today = LocalDate.now();

        YearMonth selectedMonth = YearMonth.from(firstDay);
        YearMonth currentMonth = YearMonth.from(today);

        if (selectedMonth.isAfter(currentMonth)) {
            return firstDay.minusDays(1);
        }

        if (selectedMonth.equals(currentMonth)) {
            return today.minusDays(1);
        }

        return monthEnd;
    }

    // --------------------------------------------------
    // OVERTIME AMOUNT
    // --------------------------------------------------

    public BigDecimal getOvertimeAmount(
            Integer employeeId,
            LocalDate payrollMonth) {

        if (employeeId == null || payrollMonth == null) {
            throw new IllegalArgumentException(
                    "Employee and payroll month are required.");
        }

        Employee employee = getEmployee(employeeId);

        LocalDate firstDay = payrollMonth.withDayOfMonth(1);

        LocalDate lastCompletedDay =
                getLastCompletedDay(firstDay);

        LocalDate effectiveStartDate =
                getEffectivePayrollStartDate(employee, firstDay);

        if (lastCompletedDay.isBefore(effectiveStartDate)) {
            return ZERO;
        }

        List<Overtime> overtimeList =
                overtimeRepository.findByEmployeeIdAndOtDateBetween(
                        employeeId,
                        effectiveStartDate,
                        lastCompletedDay);

        BigDecimal totalOvertime = BigDecimal.ZERO;

        if (overtimeList != null) {

            for (Overtime overtime : overtimeList) {

                if (overtime == null
                        || overtime.getOtAmount() == null) {
                    continue;
                }

                validateNonNegative(
                        overtime.getOtAmount(),
                        "Overtime amount");

                totalOvertime =
                        totalOvertime.add(overtime.getOtAmount());
            }
        }

        return money(totalOvertime);
    }

    // --------------------------------------------------
    // VALIDATE PAYROLL MONTH
    // --------------------------------------------------

    private void validatePayrollMonthNotFuture(LocalDate payrollMonth) {

        if (payrollMonth == null) {
            throw new IllegalArgumentException(
                    "Payroll month is required.");
        }

        YearMonth selectedMonth = YearMonth.from(payrollMonth);
        YearMonth currentMonth = YearMonth.now();

        if (selectedMonth.isAfter(currentMonth)) {
            throw new IllegalArgumentException(
                    "Future payroll month is not allowed. Selected month: "
                    + selectedMonth + ". Current system month: "
                    + currentMonth + ".");
        }
    }

    // --------------------------------------------------
    // COMMON VALIDATION HELPERS
    // --------------------------------------------------

    private void validateNonNegative(
            BigDecimal value,
            String fieldName) {

        if (value == null) {
            throw new IllegalArgumentException(
                    fieldName + " is required.");
        }

        if (value.signum() < 0) {
            throw new IllegalArgumentException(
                    fieldName + " cannot be negative.");
        }
    }

    private BigDecimal money(BigDecimal value) {

        if (value == null) {
            return ZERO;
        }

        return value.setScale(
                2,
                RoundingMode.HALF_UP);
    }

    private BigDecimal safeAmount(
            BigDecimal value,
            String fieldName) {

        if (value == null) {
            return ZERO;
        }

        validateNonNegative(value, fieldName);

        return money(value);
    }

    // --------------------------------------------------
    // VALIDATE PAYROLL DETAILS
    // --------------------------------------------------

    private void validatePayrollDetails(Payroll payroll) {

        if (payroll == null) {
            throw new IllegalArgumentException(
                    "Payroll details are required.");
        }

        if (payroll.getEmployeeId() == null
                || payroll.getPayrollMonth() == null) {

            throw new IllegalArgumentException(
                    "Employee and payroll month are required.");
        }

        if (payroll.getEmployeeId() <= 0) {
            throw new IllegalArgumentException(
                    "Invalid employee ID.");
        }

        if (!employeeRepository.existsById(payroll.getEmployeeId())) {
            throw new IllegalArgumentException(
                    "Selected employee does not exist.");
        }

        validatePayrollMonthNotFuture(
                payroll.getPayrollMonth());
    }

    // --------------------------------------------------
    // SAVE PAYROLL
    // --------------------------------------------------

    @Transactional
    public void savePayroll(Payroll payroll) {

        validatePayrollDetails(payroll);

        LocalDate payrollMonth =
                payroll.getPayrollMonth().withDayOfMonth(1);

        payroll.setPayrollMonth(payrollMonth);

        if (payrollRepository.existsByEmployeeIdAndPayrollMonth(
                payroll.getEmployeeId(),
                payrollMonth)) {

            throw new IllegalArgumentException(
                    "Payroll already exists for this employee and month.");
        }

        calculatePayroll(payroll);

        payroll.setStatus(PayrollStatus.CALCULATED);

        payrollRepository.save(payroll);
    }

    // --------------------------------------------------
    // UPDATE CALCULATED PAYROLL ONLY
    // --------------------------------------------------

    @Transactional
    public void updatePayroll(Payroll payroll) {

        if (payroll == null || payroll.getPayrollId() == null) {
            throw new IllegalArgumentException(
                    "Payroll ID is required for update.");
        }

        Payroll savedPayroll = payrollRepository
                .findById(payroll.getPayrollId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Payroll record not found."));

        if (savedPayroll.getStatus() != PayrollStatus.CALCULATED) {
            throw new IllegalArgumentException(
                    "Only calculated payroll can be edited.");
        }

        validatePayrollDetails(payroll);

        LocalDate payrollMonth =
                payroll.getPayrollMonth().withDayOfMonth(1);

        payroll.setPayrollMonth(payrollMonth);

        List<Payroll> existingPayrolls =
                payrollRepository.findByEmployeeIdAndPayrollMonth(
                        payroll.getEmployeeId(),
                        payrollMonth);

        for (Payroll existing : existingPayrolls) {

            if (!existing.getPayrollId().equals(
                    payroll.getPayrollId())) {

                throw new IllegalArgumentException(
                        "Another payroll already exists for this employee and month.");
            }
        }

        calculatePayroll(payroll);

        payroll.setStatus(savedPayroll.getStatus());

        payrollRepository.save(payroll);
    }

    // --------------------------------------------------
    // APPROVAL AND PAYMENT STATUS
    // --------------------------------------------------

    @Transactional
    public void saveStatusOnly(
            Integer payrollId,
            PayrollStatus requestedStatus) {

        if (payrollId == null || requestedStatus == null) {
            throw new IllegalArgumentException(
                    "Payroll ID and requested status are required.");
        }

        Payroll savedPayroll = payrollRepository
                .findById(payrollId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Payroll record not found."));

        PayrollStatus currentStatus = savedPayroll.getStatus();

        if (currentStatus == null) {
            throw new IllegalArgumentException(
                    "Current payroll status is missing.");
        }

        if (savedPayroll.getPayrollMonth() == null) {
            throw new IllegalArgumentException(
                    "Payroll month is missing.");
        }

        LocalDate payrollMonth =
                savedPayroll.getPayrollMonth().withDayOfMonth(1);

        YearMonth selectedMonth = YearMonth.from(payrollMonth);
        YearMonth currentMonth = YearMonth.now();

        if (selectedMonth.isAfter(currentMonth)) {
            throw new IllegalArgumentException(
                    "Future payroll cannot be approved or paid.");
        }

        if (!selectedMonth.isBefore(currentMonth)) {
            throw new IllegalArgumentException(
                    "Monthly payroll can only be approved or paid "
                    + "after the month has ended.");
        }

        boolean validTransition =
                (currentStatus == PayrollStatus.CALCULATED
                        && requestedStatus == PayrollStatus.APPROVED)
                || (currentStatus == PayrollStatus.APPROVED
                        && requestedStatus == PayrollStatus.PAID);

        if (!validTransition) {
            throw new IllegalArgumentException(
                    "Invalid payroll status transition: "
                    + currentStatus + " -> " + requestedStatus);
        }

        if (requestedStatus == PayrollStatus.APPROVED) {

            Map<String, Integer> attendanceSummary =
                    getAttendanceSummary(
                            savedPayroll.getEmployeeId(),
                            payrollMonth);

            validateAttendanceSummary(attendanceSummary);

            int missingAttendanceDays =
                    attendanceSummary.getOrDefault(
                            "missingAttendanceDays",
                            0);

            if (missingAttendanceDays > 0) {
                throw new IllegalArgumentException(
                        "Payroll cannot be approved because "
                        + missingAttendanceDays
                        + " working day(s) have no confirmed attendance. "
                        + "Please verify attendance records first.");
            }

            validateAttendanceSnapshotMatchesCurrent(
                    savedPayroll,
                    attendanceSummary);

            validateSavedPayrollAmounts(savedPayroll);
        }

        if (requestedStatus == PayrollStatus.PAID) {
            validateSavedPayrollAmounts(savedPayroll);
        }

        /*
         * Important:
         * No salary recalculation during approval or payment.
         */
        savedPayroll.setStatus(requestedStatus);

        payrollRepository.save(savedPayroll);
    }

    // --------------------------------------------------
    // VALIDATE SAVED ATTENDANCE SNAPSHOT
    // --------------------------------------------------

    private void validateAttendanceSnapshotMatchesCurrent(
            Payroll payroll,
            Map<String, Integer> currentSummary) {

        boolean matches =
                Objects.equals(
                        payroll.getWorkingDays(),
                        currentSummary.getOrDefault("workingDays", 0))
                && Objects.equals(
                        payroll.getPresentDays(),
                        currentSummary.getOrDefault("presentDays", 0))
                && Objects.equals(
                        payroll.getAbsentDays(),
                        currentSummary.getOrDefault("absentDays", 0))
                && Objects.equals(
                        payroll.getLeaveDays(),
                        currentSummary.getOrDefault("leaveDays", 0));

        if (!matches) {
            throw new IllegalArgumentException(
                    "Payroll approval blocked: the saved attendance "
                    + "snapshot no longer matches current attendance "
                    + "records. Please open this CALCULATED payroll, "
                    + "review it, and update/recalculate it before approval.");
        }
    }

    // --------------------------------------------------
    // VALIDATE STORED PAYROLL AMOUNTS
    // --------------------------------------------------

    private void validateSavedPayrollAmounts(Payroll payroll) {

        validateNonNegative(
                payroll.getBasicSalary(), "Basic salary");

        validateNonNegative(
                payroll.getAllowances(), "Allowances");

        validateNonNegative(
                payroll.getOvertime(), "Overtime");

        validateNonNegative(
                payroll.getDeductions(), "Deductions");

        validateNonNegative(
                payroll.getTax(), "Tax");

        validateNonNegative(
                payroll.getNetSalary(), "Net salary");

        BigDecimal expectedNet = money(
                payroll.getBasicSalary()
                        .add(payroll.getAllowances())
                        .add(payroll.getOvertime())
                        .subtract(payroll.getDeductions())
                        .subtract(payroll.getTax()));

        if (expectedNet.signum() < 0) {
            throw new IllegalArgumentException(
                    "Payroll deductions and tax exceed gross salary.");
        }

        if (expectedNet.compareTo(
                money(payroll.getNetSalary())) != 0) {

            throw new IllegalArgumentException(
                    "Payroll total validation failed. "
                    + "Please recalculate the payroll before approval or payment.");
        }
    }

    // --------------------------------------------------
    // DELETE CALCULATED PAYROLL ONLY
    // --------------------------------------------------

    @Transactional
    public void deletePayroll(Integer id) {

        if (id == null) {
            throw new IllegalArgumentException(
                    "Payroll ID is required.");
        }

        Payroll payroll = payrollRepository
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Payroll record not found."));

        if (payroll.getStatus() != PayrollStatus.CALCULATED) {
            throw new IllegalArgumentException(
                    "Only calculated payroll can be deleted.");
        }

        payrollRepository.delete(payroll);
    }

    // --------------------------------------------------
    // PAYROLL CALCULATION
    // --------------------------------------------------

    @Transactional
    public void calculatePayroll(Payroll payroll) {

        validatePayrollDetails(payroll);

        LocalDate payrollMonth =
                payroll.getPayrollMonth().withDayOfMonth(1);

        payroll.setPayrollMonth(payrollMonth);

        validatePayrollMonthNotFuture(payrollMonth);

        // 1. Effective basic salary.
        BigDecimal basicSalary = getEffectiveBasicSalary(
                payroll.getEmployeeId(),
                payrollMonth);

        // 2. Attendance summary.
        Map<String, Integer> attendanceSummary =
                getAttendanceSummary(
                        payroll.getEmployeeId(),
                        payrollMonth);

        validateAttendanceSummary(attendanceSummary);

        int workingDays =
                attendanceSummary.getOrDefault("workingDays", 0);

        int presentDays =
                attendanceSummary.getOrDefault("presentDays", 0);

        int absentDays =
                attendanceSummary.getOrDefault("absentDays", 0);

        int leaveDays =
                attendanceSummary.getOrDefault("leaveDays", 0);

        /*
         * Missing attendance is not treated as absence.
         * Approval remains blocked until attendance is complete.
         */

        payroll.setWorkingDays(workingDays);
        payroll.setPresentDays(presentDays);
        payroll.setAbsentDays(absentDays);
        payroll.setLeaveDays(leaveDays);

        // 3. Overtime uses the same date rules as the controller.
        BigDecimal totalOvertime = getOvertimeAmount(
                payroll.getEmployeeId(),
                payrollMonth);

        // 4. Active payroll rules.
        List<PayrollRule> activeRules =
                payrollRuleRepository.findByStatus(
                        PayrollRuleStatus.ACTIVE);

        BigDecimal totalAllowances = BigDecimal.ZERO;
        BigDecimal otherDeductions = BigDecimal.ZERO;
        BigDecimal taxRate = BigDecimal.ZERO;

        if (activeRules != null) {

            for (PayrollRule rule : activeRules) {

                if (rule == null
                        || rule.getRuleType() == null
                        || rule.getRuleValue() == null) {
                    continue;
                }

                BigDecimal ruleValue = rule.getRuleValue();

                validateNonNegative(
                        ruleValue,
                        "Active payroll rule value");

                String ruleType =
                        rule.getRuleType().trim().toUpperCase();

                switch (ruleType) {

                    case "ALLOWANCE":
                        totalAllowances =
                                totalAllowances.add(ruleValue);
                        break;

                    case "DEDUCTION":
                        otherDeductions =
                                otherDeductions.add(ruleValue);
                        break;

                    case "TAX":
                        taxRate = taxRate.add(ruleValue);
                        break;

                    default:
                        break;
                }
            }
        }

        if (taxRate.compareTo(ONE_HUNDRED) > 0) {
            throw new IllegalArgumentException(
                    "Combined tax percentage cannot exceed 100%.");
        }

        totalAllowances = money(totalAllowances);
        otherDeductions = money(otherDeductions);

        // 5. Deduction for confirmed absences only.
        BigDecimal absenceDeduction = basicSalary
                .multiply(BigDecimal.valueOf(absentDays))
                .divide(
                        BigDecimal.valueOf(30),
                        2,
                        RoundingMode.HALF_UP);

        BigDecimal totalDeductions = money(
                absenceDeduction.add(otherDeductions));

        // 6. Gross salary.
        BigDecimal grossSalary = money(
                basicSalary
                        .add(totalAllowances)
                        .add(totalOvertime));

        // 7. Tax.
        BigDecimal taxAmount = money(
                grossSalary
                        .multiply(taxRate)
                        .divide(
                                ONE_HUNDRED,
                                2,
                                RoundingMode.HALF_UP));

        // 8. Net salary.
        BigDecimal netSalary = money(
                grossSalary
                        .subtract(totalDeductions)
                        .subtract(taxAmount));

        if (netSalary.signum() < 0) {
            throw new IllegalArgumentException(
                    "Payroll validation failed: deductions and tax "
                    + "exceed gross salary. Please review payroll rules "
                    + "and employee attendance.");
        }

        // 9. Store calculated values.
        payroll.setBasicSalary(basicSalary);
        payroll.setAllowances(totalAllowances);
        payroll.setOvertime(totalOvertime);
        payroll.setDeductions(totalDeductions);
        payroll.setTax(taxAmount);
        payroll.setNetSalary(netSalary);
    }
}
