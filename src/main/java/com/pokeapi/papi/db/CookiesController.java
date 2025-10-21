package com.pokeapi.papi.db;

import com.pokeapi.papi.PokeApiDBService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cookies")
public class CookiesController {

    @Autowired
    private CookiesRepository cookiesRepository;

    @Autowired
    private UsersRepository usersRepository;

    record LoginRequest(String id, String uid, String value, String expires) {}

    @PostMapping("/create")
    public void createCookie(@RequestBody CookiesController.LoginRequest req) {
        PokeApiDBService.createCookie(usersRepository, cookiesRepository, Long.valueOf(req.uid));
    }
}
