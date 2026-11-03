package com.smartattendance.smart_attendance_payroll_system.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.smartattendance.smart_attendance_payroll_system.entity.Overtime;
import com.smartattendance.smart_attendance_payroll_system.repository.OvertimeRepository;

@Service
public class OvertimeService {

    private final OvertimeRepository overtimeRepository;

    public OvertimeService(
            OvertimeRepository overtimeRepository) {

        this.overtimeRepository = overtimeRepository;
    }

    public List<Overtime> getAllOvertime() {

        return overtimeRepository.findAll();
    }

    public Overtime getOvertimeById(Integer id) {

        return overtimeRepository
                .findById(id)
                .orElse(null);
    }

    public List<Overtime> getOvertimeByEmployee(
            Integer employeeId) {

        return overtimeRepository
                .findByEmployeeId(employeeId);
    }

    public List<Overtime> getOvertimeBetween(
            LocalDate startDate,
            LocalDate endDate) {

        return overtimeRepository
                .findByOtDateBetween(
                        startDate,
                        endDate);
    }

    public List<Overtime> getEmployeeOvertimeBetween(
            Integer employeeId,
            LocalDate startDate,
            LocalDate endDate) {

        return overtimeRepository
                .findByEmployeeIdAndOtDateBetween(
                        employeeId,
                        startDate,
                        endDate);
    }

    public Overtime saveOvertime(Overtime overtime) {

        validateOvertime(overtime);

        calculateAmount(overtime);

        return overtimeRepository.save(overtime);
    }

    public Overtime updateOvertime(Overtime overtime) {

        if (overtime.getOtId() == null
                || !overtimeRepository.existsById(
                        overtime.getOtId())) {

            throw new IllegalArgumentException(
                    "Overtime record not found.");
        }

        validateOvertime(overtime);

        calculateAmount(overtime);

        return overtimeRepository.save(overtime);
    }

    public void deleteOvertime(Integer id) {

        if (!overtimeRepository.existsById(id)) {

            throw new IllegalArgumentException(
                    "Overtime record not found.");
        }

        overtimeRepository.deleteById(id);
    }

    private void validateOvertime(Overtime overtime) {

        if (overtime.getEmployeeId() == null) {

            throw new IllegalArgumentException(
                    "Employee is required.");
        }

        if (overtime.getOtDate() == null) {

            throw new IllegalArgumentException(
                    "OT date is required.");
        }

        if (overtime.getOtHours() == null
                || overtime.getOtHours()
                        .compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException(
                    "OT hours must be greater than zero.");
        }

        if (overtime.getOtRate() == null
                || overtime.getOtRate()
                        .compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException(
                    "OT rate must be greater than zero.");
        }

        if (overtime.getOtType() == null
                || overtime.getOtType()
                        .trim()
                        .isEmpty()) {

            throw new IllegalArgumentException(
                    "OT type is required.");
        }

        overtime.setOtType(
                overtime.getOtType()
                        .trim());

        if (overtime.getDescription() != null) {

            overtime.setDescription(
                    overtime.getDescription()
                            .trim());
        }
    }

    private void calculateAmount(Overtime overtime) {

        BigDecimal hours =
                overtime.getOtHours();

        BigDecimal rate =
                overtime.getOtRate();

        BigDecimal amount =
                hours.multiply(rate)
                        .setScale(
                                2,
                                RoundingMode.HALF_UP);

        overtime.setOtAmount(amount);
    }
}