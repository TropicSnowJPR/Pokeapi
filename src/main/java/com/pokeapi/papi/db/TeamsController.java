package com.pokeapi.papi.db;

import com.pokeapi.papi.PokeApiDBService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/teams")
public class TeamsController {

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private TeamsRepository teamsRepository;

    record createRequest(Long uid) {}

    @PostMapping("/create")
    public void createTeam(@RequestBody createRequest req) {
        PokeApiDBService.createTeam(teamsRepository, usersRepository, req.uid);
    }
}