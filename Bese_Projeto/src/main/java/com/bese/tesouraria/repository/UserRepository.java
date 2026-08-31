package com.bese.tesouraria.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bese.tesouraria.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Page<User> findAllByOrderByIdDesc(Pageable pageable);

    Optional<User> findByCpf(String cpf);
}
