package com.pokeapi.papi;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UsersRepository extends JpaRepository<Users, Long> {
    List<Users> findByName(String name);
    List<Users> findByEmail(String email);
    List<Users> findByPassword(String password);
    List<Users> findBySalt(String salt);
    void deleteByName(String name);

}