package com.smartattendance.smart_attendance_payroll_system.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartattendance.smart_attendance_payroll_system.entity.PayrollRule;
import com.smartattendance.smart_attendance_payroll_system.entity.PayrollRuleStatus;

public interface PayrollRuleRepository extends JpaRepository<PayrollRule, Integer> {

    List<PayrollRule> findByStatus(PayrollRuleStatus status);

    List<PayrollRule> findByRuleType(String ruleType);

    List<PayrollRule> findByStatusAndRuleType(
            PayrollRuleStatus status,
            String ruleType);
}
