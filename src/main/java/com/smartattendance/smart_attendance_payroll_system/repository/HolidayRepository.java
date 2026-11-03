package com.smartattendance.smart_attendance_payroll_system.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartattendance.smart_attendance_payroll_system.entity.Holiday;

public interface HolidayRepository extends JpaRepository<Holiday, Integer> {

    List<Holiday> findByHolidayDateBetween(
            LocalDate startDate,
            LocalDate endDate);

    List<Holiday> findByHolidayDateOrderByHolidayNameAsc(
            LocalDate holidayDate);

    boolean existsByHolidayDateAndHolidayName(
            LocalDate holidayDate,
            String holidayName);
}