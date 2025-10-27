package com.pokeapi.papi.db;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "Extras", schema = "main")
@Setter
@Getter
public class Extras {
    @Id
    @SequenceGenerator(name = "cookies_seq", sequenceName = "cookies_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cookies_seq")
    @Column(name = "id")
    private Long id;

    @Column(name = "uid")
    private Long uid;

    @Column(name = "terapokemon")
    private String teraPokemon;

    @Column(name = "teratype")
    private String teraType;

}
