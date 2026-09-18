package org.schoolstock.schoolstock.model;

import java.time.LocalDate;

/**
 * A read-only display row for the admin "Set Budget" list: a {@link BudgetPeriod}
 * together with information about the gap (if any) between it and the next most
 * recent period above it in the list, so the UI can render that gap visually.
 */
public class BudgetPeriodRow {

    private final BudgetPeriod period;
    private final boolean gapBefore;
    private final LocalDate gapStart;
    private final LocalDate gapEnd;

    public BudgetPeriodRow(BudgetPeriod period, boolean gapBefore, LocalDate gapStart, LocalDate gapEnd) {
        this.period = period;
        this.gapBefore = gapBefore;
        this.gapStart = gapStart;
        this.gapEnd = gapEnd;
    }

    public BudgetPeriod getPeriod() {
        return period;
    }

    public boolean isGapBefore() {
        return gapBefore;
    }

    public LocalDate getGapStart() {
        return gapStart;
    }

    public LocalDate getGapEnd() {
        return gapEnd;
    }
}
