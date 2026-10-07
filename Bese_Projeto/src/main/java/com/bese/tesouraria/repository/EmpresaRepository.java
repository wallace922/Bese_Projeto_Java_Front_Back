package com.bese.tesouraria.repository;

import com.bese.tesouraria.entity.Empresa;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface EmpresaRepository extends JpaRepository<Empresa, Long> {

    // Buscas legadas: queries nativas NÃO passam pelo CnpjConverter, então os
    // parâmetros são comparados em claro — só acertam linha ainda não migrada.
    @Query(value = "SELECT * FROM empresa WHERE cnpj = :cnpj", nativeQuery = true)
    Optional<Empresa> findByCnpjLegacy(String cnpj);

    Optional<Empresa> findByCnpjHash(String cnpjHash);

    Boolean existsByCnpjHash(String cnpjHash);

    Page<Empresa> findAllByOrderByIdDesc(Pageable pageable);

}