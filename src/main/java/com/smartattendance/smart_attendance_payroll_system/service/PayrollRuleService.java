package com.smartattendance.smart_attendance_payroll_system.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.smartattendance.smart_attendance_payroll_system.entity.PayrollRule;
import com.smartattendance.smart_attendance_payroll_system.entity.PayrollRuleStatus;
import com.smartattendance.smart_attendance_payroll_system.repository.PayrollRuleRepository;

@Service
public class PayrollRuleService {

    private final PayrollRuleRepository payrollRuleRepository;

    public PayrollRuleService(
            PayrollRuleRepository payrollRuleRepository) {

        this.payrollRuleRepository = payrollRuleRepository;
    }


    // Get all payroll rules
    public List<PayrollRule> getAllRules() {

        return payrollRuleRepository.findAll();
    }


    // Get rule by ID
    public PayrollRule getRuleById(Integer id) {

        return payrollRuleRepository
                .findById(id)
                .orElse(null);
    }


    // Get active rules
    public List<PayrollRule> getActiveRules() {

        return payrollRuleRepository
                .findByStatus(PayrollRuleStatus.ACTIVE);
    }


    // Get rules by type
    public List<PayrollRule> getRulesByType(String ruleType) {

        return payrollRuleRepository
                .findByRuleType(ruleType);
    }


    // Get active rules by type
    public List<PayrollRule> getActiveRulesByType(
            String ruleType) {

        return payrollRuleRepository
                .findByStatusAndRuleType(
                        PayrollRuleStatus.ACTIVE,
                        ruleType);
    }


    // Save new rule
    public PayrollRule saveRule(PayrollRule payrollRule) {

        if (payrollRule.getStatus() == null) {

            payrollRule.setStatus(
                    PayrollRuleStatus.ACTIVE);
        }

        return payrollRuleRepository.save(payrollRule);
    }


    // Update existing rule
    public PayrollRule updateRule(PayrollRule payrollRule) {

        PayrollRule existingRule =
                payrollRuleRepository
                        .findById(
                                payrollRule.getRuleId())
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Payroll rule not found."));

        existingRule.setRuleName(
                payrollRule.getRuleName());

        existingRule.setRuleType(
                payrollRule.getRuleType());

        existingRule.setRuleValue(
                payrollRule.getRuleValue());

        existingRule.setDescription(
                payrollRule.getDescription());

        existingRule.setStatus(
                payrollRule.getStatus());

        return payrollRuleRepository.save(existingRule);
    }


    // Delete rule
    public void deleteRule(Integer id) {

        if (!payrollRuleRepository.existsById(id)) {

            throw new IllegalArgumentException(
                    "Payroll rule not found.");
        }

        payrollRuleRepository.deleteById(id);
    }
}
