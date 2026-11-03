
package com.smartattendance.smart_attendance_payroll_system.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartattendance.smart_attendance_payroll_system.entity.Attendance;
import com.smartattendance.smart_attendance_payroll_system.entity.AttendanceStatus;
import com.smartattendance.smart_attendance_payroll_system.repository.AttendanceRepository;

@Service
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;

    public AttendanceService(AttendanceRepository attendanceRepository) {
        this.attendanceRepository = attendanceRepository;
    }

    // Get all attendance records
    public List<Attendance> getAllAttendance() {
        return attendanceRepository.findAll();
    }

    // Get attendance record by ID
    public Attendance getAttendanceById(Integer id) {
        if (id == null) {
            return null;
        }

        return attendanceRepository.findById(id).orElse(null);
    }

    // Get attendance records for a specific date
    public List<Attendance> getAttendanceByDate(LocalDate date) {
        return attendanceRepository.findByAttendanceDate(date);
    }

    // Get attendance records for a specific employee
    public List<Attendance> getAttendanceByEmployee(Integer employeeId) {
        return attendanceRepository.findByEmployeeId(employeeId);
    }

    // Get one employee's attendance for a specific date
    public Attendance getEmployeeAttendanceForDate(
            Integer employeeId,
            LocalDate date) {

        List<Attendance> attendanceList =
                attendanceRepository.findByEmployeeIdAndAttendanceDate(
                        employeeId,
                        date);

        if (attendanceList.isEmpty()) {
            return null;
        }

        return attendanceList.get(0);
    }

    // Save a new attendance record
    public Attendance saveAttendance(Attendance attendance) {

        if (attendance == null) {
            throw new IllegalArgumentException(
                    "Attendance record cannot be empty.");
        }

        if (attendance.getEmployeeId() == null
                || attendance.getAttendanceDate() == null
                || attendance.getStatus() == null) {
            throw new IllegalArgumentException(
                    "Employee, attendance date and status are required.");
        }

        Attendance existingAttendance =
                getEmployeeAttendanceForDate(
                        attendance.getEmployeeId(),
                        attendance.getAttendanceDate());

        if (existingAttendance != null) {
            throw new IllegalArgumentException(
                    "Attendance already exists for this employee on this date.");
        }

        // A new attendance record must not be pre-confirmed.
        attendance.setAbsenceConfirmed(false);

        return attendanceRepository.save(attendance);
    }

    // Update an existing attendance record
    @Transactional
    public Attendance updateAttendance(Attendance attendance) {

        if (attendance == null
                || attendance.getAttendanceId() == null) {
            throw new IllegalArgumentException(
                    "Attendance ID is required to update a record.");
        }

        if (attendance.getEmployeeId() == null
                || attendance.getAttendanceDate() == null
                || attendance.getStatus() == null) {
            throw new IllegalArgumentException(
                    "Employee, attendance date and status are required.");
        }

        Attendance existingAttendance =
                attendanceRepository.findById(
                        attendance.getAttendanceId())
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Attendance record not found."));

        Attendance duplicateAttendance =
                getEmployeeAttendanceForDate(
                        attendance.getEmployeeId(),
                        attendance.getAttendanceDate());

        if (duplicateAttendance != null
                && !duplicateAttendance.getAttendanceId()
                        .equals(attendance.getAttendanceId())) {
            throw new IllegalArgumentException(
                    "Attendance already exists for this employee on this date.");
        }

        // Update the existing entity instead of replacing it.
        existingAttendance.setEmployeeId(attendance.getEmployeeId());
        existingAttendance.setAttendanceDate(attendance.getAttendanceDate());
        existingAttendance.setCheckIn(attendance.getCheckIn());
        existingAttendance.setCheckOut(attendance.getCheckOut());
        existingAttendance.setStatus(attendance.getStatus());

        // Preserve confirmation only while the record remains ABSENT.
        if (attendance.getStatus() != AttendanceStatus.ABSENT) {
            existingAttendance.setAbsenceConfirmed(false);
        }

        return attendanceRepository.save(existingAttendance);
    }

    // Confirm an absence for an existing ABSENT record
    @Transactional
    public Attendance confirmAbsence(Integer id) {

        if (id == null) {
            throw new IllegalArgumentException(
                    "Attendance ID is required.");
        }

        Attendance attendance =
                attendanceRepository.findById(id)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Attendance record not found."));

        if (attendance.getStatus() != AttendanceStatus.ABSENT) {
            throw new IllegalArgumentException(
                    "Only ABSENT attendance records can be confirmed.");
        }

        attendance.setAbsenceConfirmed(true);

        return attendanceRepository.save(attendance);
    }

    // Delete an attendance record
    public void deleteAttendance(Integer id) {

        if (id == null || !attendanceRepository.existsById(id)) {
            throw new IllegalArgumentException(
                    "Attendance record not found.");
        }

        attendanceRepository.deleteById(id);
    }
}

