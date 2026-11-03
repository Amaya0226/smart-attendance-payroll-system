
package com.smartattendance.smart_attendance_payroll_system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.smartattendance.smart_attendance_payroll_system.entity.Employee;
import com.smartattendance.smart_attendance_payroll_system.entity.Payroll;
import com.smartattendance.smart_attendance_payroll_system.entity.PayrollStatus;
import com.smartattendance.smart_attendance_payroll_system.repository.AttendanceRepository;
import com.smartattendance.smart_attendance_payroll_system.repository.EmployeeRepository;
import com.smartattendance.smart_attendance_payroll_system.repository.OvertimeRepository;
import com.smartattendance.smart_attendance_payroll_system.repository.PayrollRepository;
import com.smartattendance.smart_attendance_payroll_system.repository.PayrollRuleRepository;
import com.smartattendance.smart_attendance_payroll_system.repository.SalaryRevisionRepository;
import com.smartattendance.smart_attendance_payroll_system.service.HolidayService;
import com.smartattendance.smart_attendance_payroll_system.service.PayrollService;

@ExtendWith(MockitoExtension.class)
class PayrollServiceTest {

    @Mock
    private PayrollRepository payrollRepository;

    @Mock
    private PayrollRuleRepository payrollRuleRepository;

    @Mock
    private OvertimeRepository overtimeRepository;

    @Mock
    private SalaryRevisionRepository salaryRevisionRepository;

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private HolidayService holidayService;

    @Mock
    private EmployeeRepository employeeRepository;

    private PayrollService payrollService;

    @BeforeEach
    void setUp() {
        payrollService = new PayrollService(
                payrollRepository,
                payrollRuleRepository,
                overtimeRepository,
                salaryRevisionRepository,
                attendanceRepository,
                holidayService,
                employeeRepository
        );
    }

    @Test
    void incompleteAttendanceShouldBlockApproval() {

        Payroll payroll = new Payroll();

        payroll.setPayrollId(1);
        payroll.setEmployeeId(7);
        payroll.setPayrollMonth(
                YearMonth.now().minusMonths(1).atDay(1));
        payroll.setStatus(PayrollStatus.CALCULATED);

        // Employee must exist for attendance validation.
        Employee employee = new Employee();
        employee.setJoiningDate(
                YearMonth.now().minusMonths(1).atDay(1));

        when(payrollRepository.findById(1))
                .thenReturn(Optional.of(payroll));

        when(employeeRepository.findById(7))
                .thenReturn(Optional.of(employee));

        when(holidayService.getHolidaysBetween(
                any(LocalDate.class),
                any(LocalDate.class)))
                .thenReturn(List.of());

        when(attendanceRepository.findByEmployeeId(7))
                .thenReturn(List.of());

        assertThrows(
                IllegalArgumentException.class,
                () -> payrollService.saveStatusOnly(
                        1, PayrollStatus.APPROVED)
        );

        assertEquals(
                PayrollStatus.CALCULATED,
                payroll.getStatus()
        );

        verify(payrollRepository, never())
                .save(any(Payroll.class));
    }

    @Test
    void futurePayrollShouldNotBeApproved() {

        Payroll payroll = new Payroll();

        payroll.setPayrollId(2);
        payroll.setEmployeeId(7);
        payroll.setPayrollMonth(
                YearMonth.now().plusMonths(1).atDay(1));
        payroll.setStatus(PayrollStatus.CALCULATED);

        when(payrollRepository.findById(2))
                .thenReturn(Optional.of(payroll));

        assertThrows(
                IllegalArgumentException.class,
                () -> payrollService.saveStatusOnly(
                        2, PayrollStatus.APPROVED)
        );

        assertEquals(
                PayrollStatus.CALCULATED,
                payroll.getStatus()
        );

        verify(payrollRepository, never())
                .save(any(Payroll.class));
    }

    @Test
    void paymentShouldPreserveSavedSalaryAmounts() {

        Payroll payroll = new Payroll();

        payroll.setPayrollId(3);
        payroll.setEmployeeId(7);
        payroll.setPayrollMonth(
                YearMonth.now().minusMonths(1).atDay(1));
        payroll.setStatus(PayrollStatus.APPROVED);

        payroll.setBasicSalary(new BigDecimal("100000.00"));
        payroll.setAllowances(new BigDecimal("5000.00"));
        payroll.setOvertime(new BigDecimal("0.00"));
        payroll.setDeductions(new BigDecimal("2000.00"));
        payroll.setTax(new BigDecimal("10000.00"));
        payroll.setNetSalary(new BigDecimal("93000.00"));

        when(payrollRepository.findById(3))
                .thenReturn(Optional.of(payroll));

        when(payrollRepository.save(any(Payroll.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        payrollService.saveStatusOnly(3, PayrollStatus.PAID);

        assertEquals(PayrollStatus.PAID, payroll.getStatus());

        assertEquals(
                0,
                new BigDecimal("100000.00")
                        .compareTo(payroll.getBasicSalary())
        );

        assertEquals(
                0,
                new BigDecimal("93000.00")
                        .compareTo(payroll.getNetSalary())
        );

        assertEquals(
                0,
                new BigDecimal("5000.00")
                        .compareTo(payroll.getAllowances())
        );

        assertEquals(
                0,
                new BigDecimal("2000.00")
                        .compareTo(payroll.getDeductions())
        );

        assertEquals(
                0,
                new BigDecimal("10000.00")
                        .compareTo(payroll.getTax())
        );

        verify(payrollRepository).save(payroll);
    }

    @Test
    void calculatedPayrollCannotBeMarkedPaidDirectly() {

        Payroll payroll = new Payroll();

        payroll.setPayrollId(4);
        payroll.setEmployeeId(7);
        payroll.setPayrollMonth(
                YearMonth.now().minusMonths(1).atDay(1));
        payroll.setStatus(PayrollStatus.CALCULATED);

        when(payrollRepository.findById(4))
                .thenReturn(Optional.of(payroll));

        assertThrows(
                IllegalArgumentException.class,
                () -> payrollService.saveStatusOnly(
                        4, PayrollStatus.PAID)
        );

        assertEquals(
                PayrollStatus.CALCULATED,
                payroll.getStatus()
        );

        verify(payrollRepository, never())
                .save(any(Payroll.class));
    }
}
