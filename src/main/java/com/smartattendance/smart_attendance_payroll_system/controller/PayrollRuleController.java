package com.smartattendance.smart_attendance_payroll_system.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.smartattendance.smart_attendance_payroll_system.entity.PayrollRule;
import com.smartattendance.smart_attendance_payroll_system.entity.PayrollRuleStatus;
import com.smartattendance.smart_attendance_payroll_system.service.PayrollRuleService;

@Controller
@RequestMapping("/payroll-rules")
public class PayrollRuleController {

    private final PayrollRuleService payrollRuleService;

    public PayrollRuleController(
            PayrollRuleService payrollRuleService) {

        this.payrollRuleService = payrollRuleService;
    }


    // Display all payroll rules
    @GetMapping
    public String showRules(Model model) {

        model.addAttribute(
                "rules",
                payrollRuleService.getAllRules());

        return "payroll-rules";
    }


    // Show add rule page
    @GetMapping("/add")
    public String showAddRule(Model model) {

        PayrollRule rule = new PayrollRule();

        rule.setStatus(PayrollRuleStatus.ACTIVE);

        model.addAttribute("rule", rule);

        return "add-payroll-rule";
    }


    // Save rule
    @PostMapping("/save")
    public String saveRule(
            @ModelAttribute("rule") PayrollRule rule,
            Model model) {

        try {

            payrollRuleService.saveRule(rule);

        } catch (IllegalArgumentException e) {

            model.addAttribute(
                    "errorMessage",
                    e.getMessage());

            return "add-payroll-rule";
        }

        return "redirect:/payroll-rules";
    }


    // Show edit rule page
    @GetMapping("/edit/{id}")
    public String editRule(
            @PathVariable("id") Integer id,
            Model model) {

        PayrollRule rule =
                payrollRuleService.getRuleById(id);

        if (rule == null) {

            return "redirect:/payroll-rules";
        }

        model.addAttribute("rule", rule);

        return "edit-payroll-rule";
    }


    // Update rule
    @PostMapping("/update")
    public String updateRule(
            @ModelAttribute("rule") PayrollRule rule,
            Model model) {

        try {

            payrollRuleService.updateRule(rule);

        } catch (IllegalArgumentException e) {

            model.addAttribute(
                    "errorMessage",
                    e.getMessage());

            return "edit-payroll-rule";
        }

        return "redirect:/payroll-rules";
    }


    // Delete rule
    @GetMapping("/delete/{id}")
    public String deleteRule(
            @PathVariable("id") Integer id) {

        try {

            payrollRuleService.deleteRule(id);

        } catch (IllegalArgumentException e) {

            // Ignore invalid delete request
        }

        return "redirect:/payroll-rules";
    }
}