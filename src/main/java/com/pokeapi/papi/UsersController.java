package com.pokeapi.papi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UsersController {

    @Autowired
    private UsersRepository usersRepository;

    record SignupRequest(String username, String email, String password, String salt) {}

    @PostMapping("/create")
    public void createUser(@RequestBody SignupRequest req) {
        PokeApiDBService.createUser(usersRepository, req.username, req.email, req.password, req.salt);
    }
}