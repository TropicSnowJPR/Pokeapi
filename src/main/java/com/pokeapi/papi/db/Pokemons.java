package com.pokeapi.papi.db;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "Pokemons", schema = "main")
@Setter
@Getter
public class Pokemons {
    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "name")
    private String name;
}