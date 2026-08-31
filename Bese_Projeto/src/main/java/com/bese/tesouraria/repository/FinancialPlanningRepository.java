package com.bese.tesouraria.repository;

import com.bese.tesouraria.entity.FinancialPlanning;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FinancialPlanningRepository extends JpaRepository<FinancialPlanning, Long> {

    @Query("SELECT f FROM FinancialPlanning f WHERE f.numero = :numero AND YEAR(f.data) = :ano")
    Optional<FinancialPlanning> findByNumeroAndAno(@Param("numero") Integer numero, @Param("ano") Integer ano);

    @Query("SELECT COUNT(f) > 0 FROM FinancialPlanning f WHERE f.numero = :numero AND YEAR(f.data) = :ano")
    Boolean existsByNumeroAndAno(@Param("numero") Integer numero, @Param("ano") Integer ano);

    Page<FinancialPlanning> findAllByOrderByIdDesc(Pageable pageable);
}