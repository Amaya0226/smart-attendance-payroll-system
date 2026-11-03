package com.smartattendance.smart_attendance_payroll_system.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartattendance.smart_attendance_payroll_system.entity.Attendance;

public interface AttendanceRepository extends JpaRepository<Attendance, Integer> {

    List<Attendance> findByAttendanceDate(LocalDate attendanceDate);

    List<Attendance> findByAttendanceDateBetween(
            LocalDate startDate,
            LocalDate endDate);

    List<Attendance> findByEmployeeId(Integer employeeId);

    List<Attendance> findByEmployeeIdAndAttendanceDate(
            Integer employeeId,
            LocalDate attendanceDate);

    // Get latest 10 attendance records
    List<Attendance> findTop10ByOrderByAttendanceDateDescAttendanceIdDesc();
}