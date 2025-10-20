package com.pokeapi.papi;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CookiesRepository extends JpaRepository<Cookies, Long> {
    List<Cookies> findByUid(Long uid);
    List<Cookies> findByValue(String value);
    void deleteById(Long id);
    void deleteByValue(String value);
}