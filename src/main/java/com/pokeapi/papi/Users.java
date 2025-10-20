package com.pokeapi.papi;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "Users", schema = "main")
@Setter
@Getter
public class Users {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String email;
    private String password;
    private String salt;
//    private String role;  // WILL BE REMOVED
//    private Date created;  // WILL BE REMOVED
}