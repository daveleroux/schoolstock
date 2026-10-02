package org.schoolstock.schoolstock.service;

import org.schoolstock.schoolstock.model.BudgetPeriod;
import org.schoolstock.schoolstock.model.BudgetPeriodRow;
import org.schoolstock.schoolstock.model.BudgetUsage;
import org.schoolstock.schoolstock.model.BudgetUsagePoint;
import org.schoolstock.schoolstock.model.Item;
import org.schoolstock.schoolstock.model.StockPurchaseLog;
import org.schoolstock.schoolstock.repository.BudgetPeriodRepository;
import org.schoolstock.schoolstock.repository.ItemRepository;
import org.schoolstock.schoolstock.repository.StockPurchaseLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class BudgetService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final BudgetPeriodRepository budgetPeriodRepository;
    private final StockPurchaseLogRepository stockPurchaseLogRepository;
    private final ItemRepository itemRepository;

    public BudgetService(BudgetPeriodRepository budgetPeriodRepository,
                         StockPurchaseLogRepository stockPurchaseLogRepository,
                         ItemRepository itemRepository) {
        this.budgetPeriodRepository = budgetPeriodRepository;
        this.stockPurchaseLogRepository = stockPurchaseLogRepository;
        this.itemRepository = itemRepository;
    }

    /** All budget periods, most recent first — used to populate period pickers. */
    @Transactional(readOnly = true)
    public List<BudgetPeriod> getAllBudgetPeriods() {
        return budgetPeriodRepository.findAllByOrderByStartDateDesc();
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

    /**
     * Builds the "View Budget" usage series for a period: a point at the
     * period's start holding the full amount, one point per stock purchase
     * logged within the period (each decreasing the running remaining total
     * by price × quantity, in date order), and — if the last purchase didn't
     * land exactly on the period's end date — a trailing point at the end
     * date so the line extends across the whole period.
     */
    @Transactional(readOnly = true)
    public BudgetUsage getBudgetUsage(Long periodId) {
        BudgetPeriod period = budgetPeriodRepository.findById(periodId)
                .orElseThrow(() -> new IllegalArgumentException("Budget period not found: " + periodId));

        List<StockPurchaseLog> logs = stockPurchaseLogRepository
                .findByPurchaseDateBetweenOrderByPurchaseDateAscIdAsc(period.getStartDate(), period.getEndDate());

        Map<Long, String> itemNamesById = itemRepository
                .findAllById(logs.stream().map(StockPurchaseLog::getItemId).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(Item::getId, Item::getName));

        List<BudgetUsagePoint> points = new ArrayList<>();
        BigDecimal remaining = period.getAmount();
        points.add(new BudgetUsagePoint(period.getStartDate().format(DATE_FORMAT), remaining, null, null, null));

        for (StockPurchaseLog log : logs) {
            BigDecimal spent = log.getPrice().multiply(BigDecimal.valueOf(log.getQuantity()));
            remaining = remaining.subtract(spent);
            String itemName = itemNamesById.getOrDefault(log.getItemId(), "Unknown item");
            points.add(new BudgetUsagePoint(
                    log.getPurchaseDate().format(DATE_FORMAT), remaining, itemName, log.getQuantity(), log.getPrice()));
        }

        LocalDate lastPointDate = logs.isEmpty() ? period.getStartDate() : logs.get(logs.size() - 1).getPurchaseDate();
        if (lastPointDate.isBefore(period.getEndDate())) {
            points.add(new BudgetUsagePoint(period.getEndDate().format(DATE_FORMAT), remaining, null, null, null));
        }

        return new BudgetUsage(period.getStartDate(), period.getEndDate(), period.getAmount(), points);
    }
}
