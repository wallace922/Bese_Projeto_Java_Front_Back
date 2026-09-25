package com.bese.tesouraria.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.bese.tesouraria.entity.PaymentNote;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentNoteRepository extends JpaRepository<PaymentNote, Long>{

    @Query("SELECT p FROM PaymentNote p WHERE p.numeroNp = :numero AND YEAR(p.dataLiquidacao) = :ano")
    Optional<PaymentNote> findByNumeroNpForYear(
            @Param("numero") Integer numero,
            @Param("ano") Integer ano
    );

    @Query("SELECT COUNT(p) > 0 FROM PaymentNote p WHERE p.numeroNp = :numero AND YEAR(p.dataLiquidacao) = :ano")
    boolean existsByNumeroNpAndYear(@Param("numero") Integer numero, @Param("ano") Integer ano);

    Page<PaymentNote> findAllByOrderByIdDesc(Pageable pageable);

    boolean existsByEmpresaId(Long empresaId);

    @Query("SELECT COUNT(p) > 0 FROM PaymentNote p JOIN p.items i WHERE i.empresaBeneficiaria.id = :empresaId")
    boolean existsByEmpresaBeneficiariaId(@Param("empresaId") Long empresaId);
}