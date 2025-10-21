package com.pokeapi.papi.db;

import com.pokeapi.papi.PokeApiDBService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pokemons")
public class PokemonsController {

    @Autowired
    private TeamsRepository teamsRepository;

    record createRequest(Long id, String name) {}

    @PostMapping("/create")
    public void createPokemon(@RequestBody createRequest req) {
        PokeApiDBService.createPokemon(teamsRepository, req.id, req.name);
    }
}