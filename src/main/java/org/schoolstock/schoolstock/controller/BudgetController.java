package org.schoolstock.schoolstock.controller;

import org.schoolstock.schoolstock.service.BudgetService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;

@Controller
@RequestMapping("/admin/budget")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    private void populateModel(Model model) {
        model.addAttribute("budgetPeriodRows", budgetService.getBudgetPeriodRows());
    }

    @GetMapping
    public String list(Model model) {
        populateModel(model);
        return "fragments/budget-periods :: budget-periods";
    }

    @PostMapping("/create")
    public String create(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                          @RequestParam BigDecimal amount,
                          Model model) {
        try {
            budgetService.createBudgetPeriod(startDate, endDate, amount);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        populateModel(model);
        return "fragments/budget-periods :: budget-periods";
    }

    @PostMapping("/{id}/edit")
    public String edit(@PathVariable Long id,
                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                       @RequestParam BigDecimal amount,
                       Model model) {
        try {
            budgetService.updateBudgetPeriod(id, startDate, endDate, amount);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        populateModel(model);
        return "fragments/budget-periods :: budget-periods";
    }
}
