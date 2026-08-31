package com.bese.tesouraria.repository;

import com.bese.tesouraria.entity.Empresa;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmpresaRepository extends JpaRepository<Empresa, Long> {

    Optional<Empresa> findByCnpj(String Cnpj);

    Boolean existsByCnpj(String cnpj);

    Page<Empresa> findAllByOrderByIdDesc(Pageable pageable);

}