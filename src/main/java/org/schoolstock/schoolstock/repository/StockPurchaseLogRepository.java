package org.schoolstock.schoolstock.repository;

import org.schoolstock.schoolstock.model.StockPurchaseLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface StockPurchaseLogRepository extends JpaRepository<StockPurchaseLog, Long> {

    List<StockPurchaseLog> findByPurchaseDateBetweenOrderByPurchaseDateAscIdAsc(LocalDate from, LocalDate to);
}
