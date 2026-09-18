package org.schoolstock.schoolstock.service;

import org.schoolstock.schoolstock.model.BudgetPeriod;
import org.schoolstock.schoolstock.model.BudgetPeriodRow;
import org.schoolstock.schoolstock.repository.BudgetPeriodRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class BudgetService {

    private final BudgetPeriodRepository budgetPeriodRepository;

    public BudgetService(BudgetPeriodRepository budgetPeriodRepository) {
        this.budgetPeriodRepository = budgetPeriodRepository;
    }

    /**
     * All budget periods, most recent first, each annotated with whether there's
     * a gap between it and the next most recent period so the UI can render it.
     */
    @Transactional(readOnly = true)
    public List<BudgetPeriodRow> getBudgetPeriodRows() {
        List<BudgetPeriod> periods = budgetPeriodRepository.findAllByOrderByStartDateDesc();

        List<BudgetPeriodRow> rows = new ArrayList<>();
        for (int i = 0; i < periods.size(); i++) {
            BudgetPeriod period = periods.get(i);
            if (i == 0) {
                rows.add(new BudgetPeriodRow(period, false, null, null));
                continue;
            }
            BudgetPeriod moreRecent = periods.get(i - 1);
            LocalDate gapStart = period.getEndDate().plusDays(1);
            LocalDate gapEnd = moreRecent.getStartDate().minusDays(1);
            boolean gap = !gapStart.isAfter(gapEnd);
            rows.add(new BudgetPeriodRow(period, gap, gap ? gapStart : null, gap ? gapEnd : null));
        }
        return rows;
    }

    public BudgetPeriod createBudgetPeriod(LocalDate startDate, LocalDate endDate, BigDecimal amount) {
        validatePeriod(startDate, endDate, amount);
        validateNoOverlap(startDate, endDate, null);
        BudgetPeriod period = new BudgetPeriod(startDate, endDate, amount.setScale(2, java.math.RoundingMode.HALF_UP));
        return budgetPeriodRepository.save(period);
    }

    public void updateBudgetPeriod(Long id, LocalDate startDate, LocalDate endDate, BigDecimal amount) {
        validatePeriod(startDate, endDate, amount);
        BudgetPeriod period = budgetPeriodRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Budget period not found: " + id));
        validateNoOverlap(startDate, endDate, id);
        period.setStartDate(startDate);
        period.setEndDate(endDate);
        period.setAmount(amount.setScale(2, java.math.RoundingMode.HALF_UP));
    }

    private void validatePeriod(LocalDate startDate, LocalDate endDate, BigDecimal amount) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start date and end date are required.");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date cannot be before start date.");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero.");
        }
    }

    /**
     * Ensures [startDate, endDate] doesn't overlap any existing budget period,
     * excluding {@code excludeId} (the period being edited, if any).
     */
    private void validateNoOverlap(LocalDate startDate, LocalDate endDate, Long excludeId) {
        boolean overlaps = budgetPeriodRepository.findAll().stream()
                .filter(existing -> excludeId == null || !existing.getId().equals(excludeId))
                .anyMatch(existing -> !startDate.isAfter(existing.getEndDate())
                        && !endDate.isBefore(existing.getStartDate()));
        if (overlaps) {
            throw new IllegalArgumentException("This period overlaps with an existing budget period.");
        }
    }
}
