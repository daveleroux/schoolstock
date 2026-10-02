package org.schoolstock.schoolstock.controller;

import org.schoolstock.schoolstock.model.BudgetUsage;
import org.schoolstock.schoolstock.service.BudgetService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

/**
 * Read-only budget viewing for the "View Budget" tab, shared by Admins and
 * Approvers (unlike {@link BudgetController}'s "Set Budget" tab, which is
 * Admin-only for creating/editing periods).
 */
@Controller
@RequestMapping("/budget")
public class BudgetViewController {

    private final BudgetService budgetService;

    public BudgetViewController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @GetMapping("/periods")
    public String periods(Model model) {
        model.addAttribute("periods", budgetService.getAllBudgetPeriods());
        return "fragments/budget-period-options :: budget-period-options";
    }

    @GetMapping("/periods/{id}/usage")
    @ResponseBody
    public BudgetUsage usage(@PathVariable Long id) {
        try {
            return budgetService.getBudgetUsage(id);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }
}
