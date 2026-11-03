package com.smartattendance.smart_attendance_payroll_system.entity;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "holiday")
public class Holiday {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer holidayId;

    private LocalDate holidayDate;

    private String holidayName;

    private String holidayType;

    private String description;

    // =========================
    // Constructors
    // =========================

    public Holiday() {
    }

    public Holiday(
            LocalDate holidayDate,
            String holidayName,
            String holidayType,
            String description) {

        this.holidayDate = holidayDate;
        this.holidayName = holidayName;
        this.holidayType = holidayType;
        this.description = description;
    }

    // =========================
    // Getters and Setters
    // =========================

    public Integer getHolidayId() {
        return holidayId;
    }

    public void setHolidayId(Integer holidayId) {
        this.holidayId = holidayId;
    }

    public LocalDate getHolidayDate() {
        return holidayDate;
    }

    public void setHolidayDate(LocalDate holidayDate) {
        this.holidayDate = holidayDate;
    }

    public String getHolidayName() {
        return holidayName;
    }

    public void setHolidayName(String holidayName) {
        this.holidayName = holidayName;
    }

    public String getHolidayType() {
        return holidayType;
    }

    public void setHolidayType(String holidayType) {
        this.holidayType = holidayType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}