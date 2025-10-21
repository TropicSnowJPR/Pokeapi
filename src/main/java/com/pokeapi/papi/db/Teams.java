package com.pokeapi.papi.db;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "Teams", schema = "main")
@Setter
@Getter
public class Teams {
    @Id
    @SequenceGenerator(name = "team_seq", sequenceName = "team_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "team_seq")
    @Column(name = "id")
    private Long id;

    @Column(name = "uid")
    private Long uid;

    @Column(name = "mem1")
    private String mem1;

    @Column(name = "mem2")
    private String mem2;

    @Column(name = "mem3")
    private String mem3;

    @Column(name = "mem4")
    private String mem4;

    @Column(name = "mem5")
    private String mem5;

    @Column(name = "mem6")
    private String mem6;
}