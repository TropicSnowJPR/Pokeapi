package com.pokeapi.papi.db;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeamsRepository extends JpaRepository<Teams, Long> {
    List<Teams> findByUid(Long uid);
    void deleteById(Long id);

}