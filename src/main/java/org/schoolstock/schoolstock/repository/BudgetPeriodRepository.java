package org.schoolstock.schoolstock.repository;

import org.schoolstock.schoolstock.model.BudgetPeriod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BudgetPeriodRepository extends JpaRepository<BudgetPeriod, Long> {

    List<BudgetPeriod> findAllByOrderByStartDateDesc();
}
