package com.pokeapi.papi.db;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "Cookies", schema = "main")
@Setter
@Getter
public class Cookies {
    @Id
    @SequenceGenerator(name = "cookies_seq", sequenceName = "cookies_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cookies_seq")
    @Column(name = "id")
    private Long id;

    @Column(name = "uid")
    private Long uid;

    @Column(name = "value")
    private String value;

    @Column(name = "expires")
    private String expires;
}