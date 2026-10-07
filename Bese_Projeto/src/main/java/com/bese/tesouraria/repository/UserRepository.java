package com.bese.tesouraria.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.bese.tesouraria.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Page<User> findAllByOrderByIdDesc(Pageable pageable);

    // Busca legada: query nativa NÃO passa pelo CpfConverter, então o
    // parâmetro é comparado em claro — só acerta linha ainda não migrada.
    @Query(value = "SELECT * FROM `user` WHERE cpf = :cpf", nativeQuery = true)
    Optional<User> findByCpfLegacy(String cpf);

    Optional<User> findByCpfHash(String cpfHash);

    Boolean existsByCpfHash(String cpfHash);
}
