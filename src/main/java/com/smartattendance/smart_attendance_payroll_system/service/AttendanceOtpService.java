package com.smartattendance.smart_attendance_payroll_system.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.smartattendance.smart_attendance_payroll_system.entity.AttendanceOtp;
import com.smartattendance.smart_attendance_payroll_system.entity.Employee;
import com.smartattendance.smart_attendance_payroll_system.repository.AttendanceOtpRepository;
import com.smartattendance.smart_attendance_payroll_system.repository.EmployeeRepository;

import email.EmailService;

@Service
public class AttendanceOtpService {

    private final AttendanceOtpRepository attendanceOtpRepository;
    private final EmployeeRepository employeeRepository;
    private final EmailService emailService;

    private final SecureRandom secureRandom = new SecureRandom();

    public AttendanceOtpService(
            AttendanceOtpRepository attendanceOtpRepository,
            EmployeeRepository employeeRepository,
            EmailService emailService) {

        this.attendanceOtpRepository = attendanceOtpRepository;
        this.employeeRepository = employeeRepository;
        this.emailService = emailService;
    }

    public boolean generateAndSendOtp(Integer employeeId) {

        // Find employee
        Optional<Employee> employeeOptional =
                employeeRepository.findById(employeeId);

        if (employeeOptional.isEmpty()) {
            return false;
        }

        Employee employee = employeeOptional.get();

        // Invalidate previous unused OTP
        Optional<AttendanceOtp> oldOtp =
                attendanceOtpRepository
                        .findTopByEmployeeIdAndUsedFalseOrderByCreatedAtDesc(
                                employeeId);

        if (oldOtp.isPresent()) {

            AttendanceOtp previousOtp = oldOtp.get();

            previousOtp.setUsed(true);

            attendanceOtpRepository.save(previousOtp);
        }

        // Generate secure 6-digit OTP
        String otpCode =
                String.format(
                        "%06d",
                        secureRandom.nextInt(1_000_000)
                );

        LocalDateTime now =
                LocalDateTime.now();

        // Create new OTP
        AttendanceOtp attendanceOtp =
                new AttendanceOtp();

        attendanceOtp.setEmployeeId(employeeId);

        attendanceOtp.setOtpCode(otpCode);

        // OTP expires after 2 minutes
        attendanceOtp.setExpiresAt(
                now.plusMinutes(2)
        );

        attendanceOtp.setUsed(false);

        attendanceOtp.setCreatedAt(now);

        // Save OTP to database
        attendanceOtpRepository.save(attendanceOtp);

        // Send OTP to registered employee email
        emailService.sendAttendanceOtp(
                employee.getEmail(),
                employee.getFullName(),
                otpCode
        );

        return true;
    }
}