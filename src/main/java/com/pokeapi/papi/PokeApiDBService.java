package com.pokeapi.papi;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pokeapi.papi.db.*;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;


@Service
@Transactional
@Component
public class PokeApiDBService {

    private static final SecureRandom secureRandom = new SecureRandom();
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();



    // ===================================================== //
    // * User Service                                        //
    // ===================================================== //

    public static void createUser(UsersRepository usersRepository, String name, String email, String password, String salt) {
        if(name == null) {throw new RuntimeException("Name cannot be null!");}
        if(name.length() < 3 || name.length() > 24) {throw new RuntimeException("Invalid name (Name needs to be longer than 3 characters and shorter than 24 characters)!");}
        if(!name.matches("^[a-zA-Z0-9_]+$")) {throw new RuntimeException("Invalid name (Name needs to only contain A-Z, a-z, 0-9 and _)!");}
        if(email == null) {throw new RuntimeException("Email cannot be null!");}
        if(!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {throw new RuntimeException("Invalid email format (Email must be a valid email. Example: 'example@mail.com')!");}
        if(!usersRepository.findByName(name).isEmpty() || !usersRepository.findByEmail(email).isEmpty() || password.isEmpty() || salt.isEmpty()) {throw new RuntimeException("Ikd some error");}
        Users user = new Users();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(password);
        user.setSalt(salt);
        usersRepository.save(user);
    }

//    public static void deleteUser(UsersRepository usersRepository, String name) {
//        if(!usersRepository.findByName(name).isEmpty()) {throw new RuntimeException("User does not exist!");}
//        usersRepository.deleteByName(name);
//    }
//
//    public static void changeUsername(UsersRepository usersRepository, String name, String newName) {
//        if(!usersRepository.findByName(newName).isEmpty()) {throw new RuntimeException("Username is already taken!");}
//        List<Users> users = usersRepository.findByName(name);
//        Users user = users.getFirst();
//        user.setName(newName);
//        usersRepository.save(user);
//    }
//
//    public static void changeEmail(UsersRepository usersRepository, String name, String email, String newEmail) {
//        if(usersRepository.findByName(name).isEmpty() || usersRepository.findByEmail(email).isEmpty() || !usersRepository.findByEmail(newEmail).isEmpty()) {throw new RuntimeException("Invalid user or email already taken!");}
//        if(email.equals(newEmail)) {throw new RuntimeException("New email cannot be the same as the old email!");}
//        List<Users> users = usersRepository.findByName(name);
//        Users user = users.getFirst();
//        if(!Objects.equals(user.getEmail(), email)) {throw new RuntimeException("Current email does not match!");}
//        user.setEmail(newEmail);
//        usersRepository.save(user);
//    }
//
//    public static void changePassword(UsersRepository usersRepository, String name, String password, String salt, String newPassword, String newSalt) {
//        if(usersRepository.findByName(name).isEmpty() || usersRepository.findBySalt(salt).isEmpty() || usersRepository.findByPassword(password).isEmpty()) {throw new RuntimeException("Invalid user or password or salt!");}
//        if(password != newPassword && salt != newSalt) {throw new RuntimeException("New password and salt cannot be the same as the old password and salt!");}
//        List<Users> users = usersRepository.findByName(name);
//        Users user = users.getFirst();
//        if(!Objects.equals(user.getPassword(), password) || !Objects.equals(user.getSalt(), salt)) {throw new RuntimeException("Current password or salt does not match!");}
//        if(!Objects.equals(password,newPassword) || !Objects.equals(salt, newSalt)) {throw new RuntimeException("New password or salt cannot be the same as the old password or salt!");}
//        user.setPassword(newPassword);
//        user.setSalt(newSalt);
//        usersRepository.save(user);
//    }
//
    public static boolean validatePassword(UsersRepository usersRepository, String usernameoremail, String password) {
        if(usersRepository.findByPassword(password).isEmpty() || (usersRepository.findByName(usernameoremail).isEmpty() && usersRepository.findByEmail(usernameoremail).isEmpty())) {return false;}
        List<Users> usersByName = usersRepository.findByName(usernameoremail);
        for(Users user : usersByName) {
            if(Objects.equals(user.getPassword(), password)) {
                return true;
            }
        }
        List<Users> usersByEmail = usersRepository.findByEmail(usernameoremail);
        for(Users user : usersByEmail) {
            if(Objects.equals(user.getPassword(), password)) {
                return true;
            }
        }
        return false;
    }



    // ===================================================== //
    // * Team Service                                        //
    // ===================================================== //

    public static void createTeam(TeamsRepository teamsRepository, UsersRepository usersRepository, Long uid) {
        if(usersRepository.findById(uid).isEmpty()) {throw new RuntimeException("User with the provided ID does not exist!");}
        if(teamsRepository.findByUid(uid).isEmpty()) {
            Teams team = new Teams();
            team.setUid(uid);
            team.setMem1("0");
            team.setMem2("0");
            team.setMem3("0");
            team.setMem4("0");
            team.setMem5("0");
            team.setMem6("0");
            teamsRepository.save(team);
        }
    }

    public static void addPokemonToTeam(CookiesRepository cookiesRepository, TeamsRepository teamsRepository, UsersRepository usersRepository, String cookieValue, String pokemonId) {
        if(usersRepository.findById(PokeApiDBService.getIdByUsernameOrEmail(usersRepository, PokeApiDBService.getUsernameByCookie(usersRepository, cookiesRepository, cookieValue))).isEmpty()) {
            throw new RuntimeException("User with the provided ID does not exist!");
        }
        Long uid = PokeApiDBService.getIdByUsernameOrEmail(usersRepository, PokeApiDBService.getUsernameByCookie(usersRepository, cookiesRepository, cookieValue));
        if(teamsRepository.findByUid(uid).isEmpty()) {throw new RuntimeException("Team for the provided user ID does not exist!");}
        Teams team = teamsRepository.findByUid(uid).getFirst();
        if(team.getMem1().equals("0")) {
            team.setMem1(pokemonId);
        } else if(team.getMem2().equals("0")) {
            team.setMem2(pokemonId);
        } else if(team.getMem3().equals("0")) {
            team.setMem3(pokemonId);
        } else if(team.getMem4().equals("0")) {
            team.setMem4(pokemonId);
        } else if(team.getMem5().equals("0")) {
            team.setMem5(pokemonId);
        } else if(team.getMem6().equals("0")) {
            team.setMem6(pokemonId);
        } else {
            throw new RuntimeException("Team is already full!");
        }
        teamsRepository.save(team);
    }

    public static void removePokemonFromTeam(CookiesRepository cookiesRepository, TeamsRepository teamsRepository, UsersRepository usersRepository, String cookieValue, String id) {
        if(usersRepository.findById(PokeApiDBService.getIdByUsernameOrEmail(usersRepository, PokeApiDBService.getUsernameByCookie(usersRepository, cookiesRepository, cookieValue))).isEmpty()) {
            throw new RuntimeException("User with the provided ID does not exist!");
        }
        Long uid = PokeApiDBService.getIdByUsernameOrEmail(usersRepository, PokeApiDBService.getUsernameByCookie(usersRepository, cookiesRepository, cookieValue));
        if(teamsRepository.findByUid(uid).isEmpty()) {throw new RuntimeException("Team for the provided user ID does not exist!");}
        Teams team = teamsRepository.findByUid(uid).getFirst();
        boolean found = false;
        if(team.getMem1().equals(id)) {
            team.setMem1("0");
            found = true;
        } else if(team.getMem2().equals(id)) {
            team.setMem2("0");
            found = true;
        } else if(team.getMem3().equals(id)) {
            team.setMem3("0");
            found = true;
        } else if(team.getMem4().equals(id)) {
            team.setMem4("0");
            found = true;
        } else if(team.getMem5().equals(id)) {
            team.setMem5("0");
            found = true;
        } else if(team.getMem6().equals(id)) {
            team.setMem6("0");
            found = true;
        }
        if(!found) {
            throw new RuntimeException("Pokemon is not in the team! Pokemon:" + id);
        }
        teamsRepository.save(team);
    }



    // ===================================================== //
    // * Convert Service                                     //
    // ===================================================== //

    public static Long getIdByUsernameOrEmail(UsersRepository usersRepository, String usernameOrEmail) {
        Long id = null;
        try {
            List<Users> users = usersRepository.findByName(usernameOrEmail);
            id = users.getFirst().getId();
        } catch(Exception _) {}

        try {
            List<Users> users = usersRepository.findByEmail(usernameOrEmail);
            id = users.getFirst().getId();
        } catch(Exception _) {}

        if(id == null) {
            throw new RuntimeException("No user found for the provided username or email!");
        }

        return id;
    }

    public static String getSaltByUsernameOrEmail(UsersRepository usersRepository, String usernameoremail) {
        String salt = null;
        try {
            List<Users> users = usersRepository.findByName(usernameoremail);
            salt = users.getFirst().getSalt();
        } catch(Exception _) {}

        try {
            List<Users> users = usersRepository.findByEmail(usernameoremail);
            salt = users.getFirst().getSalt();
        } catch(Exception _) {}

        if(salt == null || salt.isEmpty()) {
            throw new RuntimeException("User not found!");
        }


        return salt;
    }



    // ===================================================== //
    // * Cookie Service                                      //
    // ===================================================== //

    public static String createCookie(UsersRepository usersRepository, CookiesRepository cookiesRepository, Long uid) {
        if(uid == null) {
            return null;
        }
        if(usersRepository.findById(uid).isEmpty()) {
            return null;
        }
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String value = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        Instant expires = Instant.now().plus(7, ChronoUnit.DAYS);
        if(!cookiesRepository.findByValue(value).isEmpty()) { return null; }
        Cookies cookies = new Cookies();
        cookies.setUid(uid);
        cookies.setValue(value);
        cookies.setExpires(String.valueOf(expires));
        cookiesRepository.save(cookies);
        return value;
    }

    public static boolean validateCookie(CookiesRepository cookiesRepository, String value) {
        if(cookiesRepository.findByValue(value).isEmpty()) { return false; }
        List<Cookies> cookies = cookiesRepository.findByValue(value);
        Cookies cookie = cookies.getFirst();
        Instant expires = Instant.parse(cookie.getExpires());
        if(Instant.now().isAfter(expires)) {
            cookiesRepository.deleteByValue(value);
            return false;
        }
        return true;
    }

    public static String getUsernameByCookie(UsersRepository usersRepository, CookiesRepository cookiesRepository, String value) {
        if(cookiesRepository.findByValue(value).isEmpty()) { throw new RuntimeException("Cookie does not exist in db!"); }
        List<Cookies> cookies = cookiesRepository.findByValue(value);
        Long id = cookies.getFirst().getUid();
        if(usersRepository.findById(id).isEmpty()) { throw new RuntimeException("User does not exist in db!"); }
        Users user = usersRepository.findById(id).get();
        return user.getName();
    }


    public static Map<String, Object> getTeamFromCookie( PokemonsRepository pokemonsRepository, TeamsRepository teamsRepository, CookiesRepository cookiesRepository, String value) {
        if(cookiesRepository.findByValue(value).isEmpty()) { throw new RuntimeException("Cookie does not exist in db!"); }
        List<Cookies> cookies = cookiesRepository.findByValue(value);
        Long id = cookies.getFirst().getUid();
        List<String> teamMembers = new ArrayList<>();
        if(teamsRepository.findByUid(id).isEmpty()) { return Map.of("team_members", teamMembers); }
        Teams team = teamsRepository.findByUid(id).getFirst();
        for (String memberId : List.of(
                team.getMem1(), team.getMem2(), team.getMem3(),
                team.getMem4(), team.getMem5(), team.getMem6())) {
            if(memberId == null || memberId.isEmpty() || memberId.equals("0")) { continue; }
            if(pokemonsRepository.findById(Long.valueOf(memberId)).isPresent()) {
                Pokemons pokemon = pokemonsRepository.findById(Long.valueOf(memberId)).get();
                String name = pokemon.getName();
                teamMembers.add(name);
            } else {
                Pokemon pokemon = PokeApiService.getPokemon(memberId)
                        .map(json -> gson.fromJson(json, Pokemon.class))
                        .orElseThrow(() -> new RuntimeException("Failed to fetch pokemon from PokeAPI for ID: " + memberId));
                String name = pokemon.name();
                Pokemons pokemons = new Pokemons();
                pokemons.setName(name);
                pokemons.setId(Long.valueOf(memberId));
                pokemonsRepository.save(pokemons);
                teamMembers.add(name);
            }
        }
        return Map.of("team_members", teamMembers);
    }



}