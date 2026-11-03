package com.smartattendance.smart_attendance_payroll_system.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.smartattendance.smart_attendance_payroll_system.entity.Holiday;
import com.smartattendance.smart_attendance_payroll_system.repository.HolidayRepository;

@Service
public class HolidayService {

    private final HolidayRepository holidayRepository;

    public HolidayService(HolidayRepository holidayRepository) {
        this.holidayRepository = holidayRepository;
    }

    // Get all holidays
    public List<Holiday> getAllHolidays() {
        return holidayRepository.findAll();
    }

    // Get holiday by ID
    public Holiday getHolidayById(Integer id) {
        return holidayRepository.findById(id).orElse(null);
    }

    // Get holidays between two dates
    public List<Holiday> getHolidaysBetween(
            LocalDate startDate,
            LocalDate endDate) {

        return holidayRepository.findByHolidayDateBetween(
                startDate,
                endDate);
    }

    // Save holiday
    public Holiday saveHoliday(Holiday holiday) {

        if (holiday.getHolidayDate() == null) {
            throw new IllegalArgumentException(
                    "Holiday date is required.");
        }

        if (holiday.getHolidayName() == null
                || holiday.getHolidayName().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Holiday name is required.");
        }

        if (holiday.getHolidayType() == null
                || holiday.getHolidayType().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Holiday type is required.");
        }

        boolean exists =
                holidayRepository
                        .existsByHolidayDateAndHolidayName(
                                holiday.getHolidayDate(),
                                holiday.getHolidayName().trim());

        if (exists) {
            throw new IllegalArgumentException(
                    "This holiday already exists.");
        }

        holiday.setHolidayName(
                holiday.getHolidayName().trim());

        holiday.setHolidayType(
                holiday.getHolidayType().trim());

        if (holiday.getDescription() != null) {
            holiday.setDescription(
                    holiday.getDescription().trim());
        }

        return holidayRepository.save(holiday);
    }

    // Update holiday
    public Holiday updateHoliday(Holiday holiday) {

        if (holiday.getHolidayId() == null
                || !holidayRepository.existsById(
                        holiday.getHolidayId())) {

            throw new IllegalArgumentException(
                    "Holiday record not found.");
        }

        if (holiday.getHolidayDate() == null) {
            throw new IllegalArgumentException(
                    "Holiday date is required.");
        }

        if (holiday.getHolidayName() == null
                || holiday.getHolidayName().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Holiday name is required.");
        }

        if (holiday.getHolidayType() == null
                || holiday.getHolidayType().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Holiday type is required.");
        }

        holiday.setHolidayName(
                holiday.getHolidayName().trim());

        holiday.setHolidayType(
                holiday.getHolidayType().trim());

        if (holiday.getDescription() != null) {
            holiday.setDescription(
                    holiday.getDescription().trim());
        }

        return holidayRepository.save(holiday);
    }

    // Delete holiday
    public void deleteHoliday(Integer id) {

        if (!holidayRepository.existsById(id)) {
            throw new IllegalArgumentException(
                    "Holiday record not found.");
        }

        holidayRepository.deleteById(id);
    }
}