package com.pokeapi.papi.db;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PokemonsRepository extends JpaRepository<Pokemons, Long>  {
    List<Pokemons> findByName(String name);
    void deleteById(Long id);
}
