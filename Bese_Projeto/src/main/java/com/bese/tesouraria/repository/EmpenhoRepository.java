package com.bese.tesouraria.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.bese.tesouraria.entity.Empenho;

import java.util.Optional;

public interface EmpenhoRepository extends JpaRepository<Empenho, Long>{

    Optional<Empenho> findByNumeroAndAno(Integer numero, Integer ano);

    Boolean existsByNumeroAndAno(Integer numero, Integer ano);

    Page<Empenho> findAllByOrderByIdDesc(Pageable pageable);
}