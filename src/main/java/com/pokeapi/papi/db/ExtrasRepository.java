package com.pokeapi.papi.db;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExtrasRepository extends JpaRepository<Extras, Long> {
    List<Extras> findByUid(Long uid);
    void deleteByUid(Long id);
}
