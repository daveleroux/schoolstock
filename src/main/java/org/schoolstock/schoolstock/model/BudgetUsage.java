package org.schoolstock.schoolstock.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * The data behind the "View Budget" usage chart for a single budget period:
 * the period's bounds/amount, and the series of {@link BudgetUsagePoint}s
 * tracking budget remaining over time as stock purchases are logged.
 */
public class BudgetUsage {

    private final LocalDate startDate;
    private final LocalDate endDate;
    private final BigDecimal amount;
    private final List<BudgetUsagePoint> points;

    public BudgetUsage(LocalDate startDate, LocalDate endDate, BigDecimal amount, List<BudgetUsagePoint> points) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.amount = amount;
        this.points = points;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public List<BudgetUsagePoint> getPoints() {
        return points;
    }
}
