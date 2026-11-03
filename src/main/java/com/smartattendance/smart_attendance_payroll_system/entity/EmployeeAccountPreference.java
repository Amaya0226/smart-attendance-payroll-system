
package com.smartattendance.smart_attendance_payroll_system.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "employee_account_preferences")
public class EmployeeAccountPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "employee_id", nullable = false, unique = true)
    private Integer employeeId;

    @Column(name = "attendance_notifications", nullable = false)
    private boolean attendanceNotifications = true;

    @Column(name = "payroll_notifications", nullable = false)
    private boolean payrollNotifications = true;

    @Column(name = "payslip_notifications", nullable = false)
    private boolean payslipNotifications = true;

    @Column(name = "theme", nullable = false, length = 10)
    private String theme = "LIGHT";

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public EmployeeAccountPreference() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Integer employeeId) {
        this.employeeId = employeeId;
    }

    public boolean isAttendanceNotifications() {
        return attendanceNotifications;
    }

    public void setAttendanceNotifications(boolean attendanceNotifications) {
        this.attendanceNotifications = attendanceNotifications;
    }

    public boolean isPayrollNotifications() {
        return payrollNotifications;
    }

    public void setPayrollNotifications(boolean payrollNotifications) {
        this.payrollNotifications = payrollNotifications;
    }

    public boolean isPayslipNotifications() {
        return payslipNotifications;
    }

    public void setPayslipNotifications(boolean payslipNotifications) {
        this.payslipNotifications = payslipNotifications;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
