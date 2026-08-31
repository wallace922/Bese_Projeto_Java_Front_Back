package com.bese.tesouraria.repository;

import com.bese.tesouraria.entity.PaymentNoteEmpenho;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentNoteEmpenhoRepository extends JpaRepository<PaymentNoteEmpenho, Long> {

    Page<PaymentNoteEmpenho> findAllByOrderByIdDesc(Pageable pageable);

    @Query(
        value = """
            SELECT pne FROM PaymentNoteEmpenho pne
            JOIN FETCH pne.paymentNote pn
            JOIN FETCH pn.items i
            JOIN FETCH pne.financialPlanning fp
            WHERE MONTH(pn.datePayment) = :mes
              AND YEAR(pn.datePayment) = :ano
              AND pn.id NOT IN (
                  SELECT DISTINCT pn2.id FROM PaymentNote pn2
                  JOIN pn2.items i2
                  JOIN i2.taxes t2
                  WHERE t2.tipo <> com.bese.tesouraria.enun.OptanteStatus.NAO_OPTANTE
                    AND MONTH(pn2.datePayment) = :mes
                    AND YEAR(pn2.datePayment) = :ano
              )
            """,
        countQuery = """
            SELECT COUNT(pne) FROM PaymentNoteEmpenho pne
            JOIN pne.paymentNote pn
            JOIN pn.items i
            WHERE MONTH(pn.datePayment) = :mes
              AND YEAR(pn.datePayment) = :ano
              AND pn.id NOT IN (
                  SELECT DISTINCT pn2.id FROM PaymentNote pn2
                  JOIN pn2.items i2
                  JOIN i2.taxes t2
                  WHERE t2.tipo <> com.bese.tesouraria.enun.OptanteStatus.NAO_OPTANTE
                    AND MONTH(pn2.datePayment) = :mes
                    AND YEAR(pn2.datePayment) = :ano
              )
            """
    )
    Page<PaymentNoteEmpenho> findByPaymentNoteDatePaymentMonthAndYearAndAllItemsNonOptante(
            @Param("mes") Integer mes,
            @Param("ano") Integer ano,
            Pageable pageable);

    Page<PaymentNoteEmpenho> findByFinancialPlanningIsNull(Pageable pageable);
}
