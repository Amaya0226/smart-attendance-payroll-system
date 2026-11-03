package com.smartattendance.smart_attendance_payroll_system.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "overtime")
public class Overtime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer otId;

    private Integer employeeId;

    private LocalDate otDate;

    private BigDecimal otHours;

    private BigDecimal otRate;

    private String otType;

    private BigDecimal otAmount;

    private String description;

    public Overtime() {
    }

    public Integer getOtId() {
        return otId;
    }

    public void setOtId(Integer otId) {
        this.otId = otId;
    }

    public Integer getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Integer employeeId) {
        this.employeeId = employeeId;
    }

    public LocalDate getOtDate() {
        return otDate;
    }

    public void setOtDate(LocalDate otDate) {
        this.otDate = otDate;
    }

    public BigDecimal getOtHours() {
        return otHours;
    }

    public void setOtHours(BigDecimal otHours) {
        this.otHours = otHours;
    }

    public BigDecimal getOtRate() {
        return otRate;
    }

    public void setOtRate(BigDecimal otRate) {
        this.otRate = otRate;
    }

    public String getOtType() {
        return otType;
    }

    public void setOtType(String otType) {
        this.otType = otType;
    }

    public BigDecimal getOtAmount() {
        return otAmount;
    }

    public void setOtAmount(BigDecimal otAmount) {
        this.otAmount = otAmount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}