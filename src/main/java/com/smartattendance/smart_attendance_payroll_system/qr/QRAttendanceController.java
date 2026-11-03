package com.smartattendance.smart_attendance_payroll_system.qr;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.smartattendance.smart_attendance_payroll_system.entity.Attendance;
import com.smartattendance.smart_attendance_payroll_system.entity.AttendanceOtp;
import com.smartattendance.smart_attendance_payroll_system.entity.AttendanceStatus;
import com.smartattendance.smart_attendance_payroll_system.entity.Employee;
import com.smartattendance.smart_attendance_payroll_system.entity.Overtime;
import com.smartattendance.smart_attendance_payroll_system.repository.AttendanceOtpRepository;
import com.smartattendance.smart_attendance_payroll_system.repository.AttendanceRepository;
import com.smartattendance.smart_attendance_payroll_system.repository.EmployeeRepository;
import com.smartattendance.smart_attendance_payroll_system.repository.HolidayRepository;
import com.smartattendance.smart_attendance_payroll_system.repository.OvertimeRepository;
import com.smartattendance.smart_attendance_payroll_system.service.AttendanceOtpService;

@Controller
public class QRAttendanceController {

    private static final int NORMAL_WORKING_HOURS = 8;

    private static final int WORKING_DAYS_PER_MONTH = 22;

    private static final BigDecimal OT_MULTIPLIER =
            new BigDecimal("1.5");

    private final EmployeeRepository employeeRepository;

    private final AttendanceRepository attendanceRepository;

    private final OvertimeRepository overtimeRepository;

    private final HolidayRepository holidayRepository;

    private final AttendanceOtpRepository attendanceOtpRepository;

    private final AttendanceOtpService attendanceOtpService;

    public QRAttendanceController(
            EmployeeRepository employeeRepository,
            AttendanceRepository attendanceRepository,
            OvertimeRepository overtimeRepository,
            HolidayRepository holidayRepository,
            AttendanceOtpRepository attendanceOtpRepository,
            AttendanceOtpService attendanceOtpService) {

        this.employeeRepository = employeeRepository;

        this.attendanceRepository = attendanceRepository;

        this.overtimeRepository = overtimeRepository;

        this.holidayRepository = holidayRepository;

        this.attendanceOtpRepository = attendanceOtpRepository;

        this.attendanceOtpService = attendanceOtpService;
    }

    // =========================================================
    // QR ATTENDANCE PAGE
    // =========================================================

    @GetMapping("/qr-attendance")
    public String showQRAttendancePage() {

        return "qr-attendance";
    }

    // =========================================================
    // STEP 1
    // QR SCAN
    // =========================================================

    @PostMapping("/qr-attendance/scan")
    @ResponseBody
    public ResponseEntity<String> scanQRCode(
            @RequestParam String employeeCode) {

        List<Employee> employees =
                employeeRepository
                        .findByEmployeeCodeContainingIgnoreCase(
                                employeeCode);

        if (employees.isEmpty()) {

            return ResponseEntity.badRequest()
                    .body("Employee not found: " + employeeCode);
        }

        Employee employee = employees.get(0);

        // Generate and send OTP
        boolean otpSent =
                attendanceOtpService.generateAndSendOtp(
                        employee.getEmployeeId());

        if (!otpSent) {

            return ResponseEntity.internalServerError()
                    .body(
                            "Unable to generate attendance OTP. "
                            + "Please contact the administrator."
                    );
        }

        return ResponseEntity.ok(
                "QR verified for "
                + employee.getFullName()
                + ". A 6-digit OTP has been sent to the registered email address."
        );
    }

    // =========================================================
    // STEP 2
    // VERIFY OTP
    // =========================================================

    @PostMapping("/qr-attendance/verify-otp")
    @ResponseBody
    public ResponseEntity<String> verifyOtp(
            @RequestParam String employeeCode,
            @RequestParam String otp) {

        // Validate OTP format
        if (otp == null || !otp.matches("\\d{6}")) {

            return ResponseEntity.badRequest()
                    .body("Invalid OTP. Please enter the 6-digit OTP.");
        }

        // Find employee
        List<Employee> employees =
                employeeRepository
                        .findByEmployeeCodeContainingIgnoreCase(
                                employeeCode);

        if (employees.isEmpty()) {

            return ResponseEntity.badRequest()
                    .body("Employee not found.");
        }

        Employee employee = employees.get(0);

        // Get latest unused OTP
        Optional<AttendanceOtp> otpOptional =
                attendanceOtpRepository
                        .findTopByEmployeeIdAndUsedFalseOrderByCreatedAtDesc(
                                employee.getEmployeeId());

        if (otpOptional.isEmpty()) {

            return ResponseEntity.badRequest()
                    .body(
                            "No active OTP found. "
                            + "Please scan the QR code again."
                    );
        }

        AttendanceOtp attendanceOtp =
                otpOptional.get();

        // =====================================================
        // CHECK OTP EXPIRY
        // =====================================================

        if (attendanceOtp.getExpiresAt()
                .isBefore(LocalDateTime.now())) {

            // Expired OTP cannot be reused
            attendanceOtp.setUsed(true);

            attendanceOtpRepository.save(attendanceOtp);

            return ResponseEntity.badRequest()
                    .body(
                            "OTP has expired. "
                            + "Please scan the QR code again to receive a new OTP."
                    );
        }

        // =====================================================
        // CHECK OTP CODE
        // =====================================================

        if (!attendanceOtp.getOtpCode().equals(otp)) {

            return ResponseEntity.badRequest()
                    .body(
                            "Incorrect OTP. "
                            + "Please check your email and try again."
                    );
        }

        // =====================================================
        // OTP IS VALID
        // MARK AS USED
        // =====================================================

        attendanceOtp.setUsed(true);

        attendanceOtpRepository.save(attendanceOtp);

        // =====================================================
        // PROCESS ATTENDANCE
        // =====================================================

        return processAttendance(employee);
    }

    // =========================================================
    // ATTENDANCE PROCESSING
    // =========================================================

    private ResponseEntity<String> processAttendance(
            Employee employee) {

        LocalDate today =
                LocalDate.now();

        LocalTime currentTime =
                LocalTime.now();

        List<Attendance> todayAttendance =
                attendanceRepository
                        .findByEmployeeIdAndAttendanceDate(
                                employee.getEmployeeId(),
                                today);

        // =====================================================
        // CHECK-IN
        // =====================================================

        if (todayAttendance.isEmpty()) {

            Attendance attendance =
                    new Attendance();

            attendance.setEmployeeId(
                    employee.getEmployeeId());

            attendance.setAttendanceDate(
                    today);

            attendance.setCheckIn(
                    currentTime);

            attendance.setStatus(
                    AttendanceStatus.PRESENT);

            attendanceRepository.save(attendance);

            return ResponseEntity.ok(
                    "Check-in successful for "
                    + employee.getFullName()
                    + " at "
                    + currentTime
            );
        }

        Attendance attendance =
                todayAttendance.get(0);

        // =====================================================
        // CHECK-OUT
        // =====================================================

        if (attendance.getCheckOut() == null) {

            attendance.setCheckOut(
                    currentTime);

            attendanceRepository.save(attendance);

            String message =
                    "Check-out successful for "
                    + employee.getFullName()
                    + " at "
                    + currentTime;

            // =================================================
            // AUTOMATIC OVERTIME CALCULATION
            // =================================================

            if (attendance.getCheckIn() != null) {

                Duration workedDuration =
                        Duration.between(
                                attendance.getCheckIn(),
                                currentTime);

                long workedMinutes =
                        workedDuration.toMinutes();

                long normalWorkingMinutes =
                        NORMAL_WORKING_HOURS * 60L;

                if (workedMinutes >
                        normalWorkingMinutes) {

                    long overtimeMinutes =
                            workedMinutes
                                    - normalWorkingMinutes;

                    BigDecimal overtimeHours =
                            BigDecimal.valueOf(
                                    overtimeMinutes)
                                    .divide(
                                            BigDecimal.valueOf(60),
                                            2,
                                            RoundingMode.HALF_UP);

                    // =========================================
                    // CHECK EXISTING OT FOR SAME DAY
                    // =========================================

                    List<Overtime> existingOvertime =
                            overtimeRepository
                                    .findByEmployeeIdAndOtDateBetween(
                                            employee.getEmployeeId(),
                                            today,
                                            today);

                    if (existingOvertime.isEmpty()) {

                        BigDecimal basicSalary =
                                employee.getBasicSalary();

                        if (basicSalary != null
                                && basicSalary.compareTo(
                                        BigDecimal.ZERO) > 0) {

                            // =================================
                            // HOURLY BASIC SALARY
                            // =================================

                            BigDecimal hourlyBasicSalary =
                                    basicSalary.divide(
                                            BigDecimal.valueOf(
                                                    WORKING_DAYS_PER_MONTH),
                                            2,
                                            RoundingMode.HALF_UP)
                                    .divide(
                                            BigDecimal.valueOf(
                                                    NORMAL_WORKING_HOURS),
                                            2,
                                            RoundingMode.HALF_UP);

                            // =================================
                            // OT RATE
                            // =================================

                            BigDecimal otRate =
                                    hourlyBasicSalary
                                            .multiply(
                                                    OT_MULTIPLIER)
                                            .setScale(
                                                    2,
                                                    RoundingMode.HALF_UP);

                            // =================================
                            // DETERMINE OT TYPE
                            // =================================

                            String otType;

                            boolean isHoliday =
                                    !holidayRepository
                                            .findByHolidayDateOrderByHolidayNameAsc(
                                                    today)
                                            .isEmpty();

                            DayOfWeek dayOfWeek =
                                    today.getDayOfWeek();

                            if (isHoliday) {

                                otType = "HOLIDAY";

                            } else if (
                                    dayOfWeek == DayOfWeek.SATURDAY
                                            || dayOfWeek == DayOfWeek.SUNDAY) {

                                otType = "WEEKEND";

                            } else {

                                otType = "WEEKDAY";
                            }

                            // =================================
                            // CREATE OVERTIME
                            // =================================

                            Overtime overtime =
                                    new Overtime();

                            overtime.setEmployeeId(
                                    employee.getEmployeeId());

                            overtime.setOtDate(
                                    today);

                            overtime.setOtHours(
                                    overtimeHours);

                            overtime.setOtRate(
                                    otRate);

                            overtime.setOtType(
                                    otType);

                            BigDecimal otAmount =
                                    overtimeHours
                                            .multiply(otRate)
                                            .setScale(
                                                    2,
                                                    RoundingMode.HALF_UP);

                            overtime.setOtAmount(
                                    otAmount);

                            overtime.setDescription(
                                    "Automatically generated from QR attendance "
                                    + "after OTP verification. Worked "
                                    + workedMinutes
                                    + " minutes.");

                            overtimeRepository.save(
                                    overtime);

                            message +=
                                    " | Overtime: "
                                    + overtimeHours
                                    + " hours"
                                    + " | OT Amount: Rs. "
                                    + otAmount;
                        }
                    }
                }
            }

            return ResponseEntity.ok(message);
        }

        // =====================================================
        // ATTENDANCE ALREADY COMPLETED
        // =====================================================

        return ResponseEntity.ok(
                "Attendance already completed for "
                + employee.getFullName()
        );
    }
}