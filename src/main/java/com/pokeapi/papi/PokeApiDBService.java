package com.pokeapi.papi;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pokeapi.papi.db.*;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(PokeApiApplication.class);
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

    public static void deleteUser(UsersRepository usersRepository, String name) {
        if(!usersRepository.findByName(name).isEmpty()) {throw new RuntimeException("User does not exist!");}
        usersRepository.deleteByName(name);
    }

    public static void changeUsername(UsersRepository usersRepository, String name, String newName) {
        if(!usersRepository.findByName(newName).isEmpty()) {throw new RuntimeException("Username is already taken!");}
        List<Users> users = usersRepository.findByName(name);
        Users user = users.getFirst();
        user.setName(newName);
        usersRepository.save(user);
    }

    public static void changeEmail(UsersRepository usersRepository, String name, String email, String newEmail) {
        if(usersRepository.findByName(name).isEmpty() || usersRepository.findByEmail(email).isEmpty() || !usersRepository.findByEmail(newEmail).isEmpty()) {throw new RuntimeException("Invalid user or email already taken!");}
        if(email.equals(newEmail)) {throw new RuntimeException("New email cannot be the same as the old email!");}
        List<Users> users = usersRepository.findByName(name);
        Users user = users.getFirst();
        if(!Objects.equals(user.getEmail(), email)) {throw new RuntimeException("Current email does not match!");}
        user.setEmail(newEmail);
        usersRepository.save(user);
    }

    public static void changePassword(UsersRepository usersRepository, String name, String password, String salt, String newPassword, String newSalt) {
        if(usersRepository.findByName(name).isEmpty() || usersRepository.findBySalt(salt).isEmpty() || usersRepository.findByPassword(password).isEmpty()) {throw new RuntimeException("Invalid user or password or salt!");}
        if(password != newPassword && salt != newSalt) {throw new RuntimeException("New password and salt cannot be the same as the old password and salt!");}
        List<Users> users = usersRepository.findByName(name);
        Users user = users.getFirst();
        if(!Objects.equals(user.getPassword(), password) || !Objects.equals(user.getSalt(), salt)) {throw new RuntimeException("Current password or salt does not match!");}
        if(!Objects.equals(password,newPassword) || !Objects.equals(salt, newSalt)) {throw new RuntimeException("New password or salt cannot be the same as the old password or salt!");}
        user.setPassword(newPassword);
        user.setSalt(newSalt);
        usersRepository.save(user);
    }

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

    public static void createTeam(UsersRepository usersRepository, Long uid) {

    }



    // ===================================================== //
    // * Pokemon Service                                     //
    // ===================================================== //

    public static void createPokemon(TeamsRepository teamsRepository, Long id, String name) {
    }



    // ===================================================== //
    // * Convert Service                                     //
    // ===================================================== //

    public static Long getIdByUsernameOrEmail(UsersRepository usersRepository, String usernameOrEmail) {
        Long id = null;
        try {
            List<Users> users = usersRepository.findByName(usernameOrEmail);
            if (users.isEmpty()) { logger.warn("Failed to retrieve user ID for username: {}", usernameOrEmail);}
            id = users.getFirst().getId();
        } catch(Exception e) {}

        try {
            List<Users> users = usersRepository.findByEmail(usernameOrEmail);
            if (users.isEmpty()) { logger.warn("Failed to retrieve user ID for email: {}", usernameOrEmail);}
            id = users.getFirst().getId();
        } catch(Exception e) {}

        if(id == null) {
            logger.warn("No user found for username/email: {}", usernameOrEmail);
            throw new RuntimeException("No user found for the provided username or email!");
        }
        logger.info("Retrieved user ID: {} for username/email: {}", id, usernameOrEmail);

        return id;
    }

    public static String getSaltByUsernameOrEmail(UsersRepository usersRepository, String usernameoremail) {
        String salt = null;
        try {
            List<Users> users = usersRepository.findByName(usernameoremail);
            if (users.isEmpty()) { logger.warn("Failed to retrieve salt for username: {}", usernameoremail);}
            salt = users.getFirst().getSalt();
        } catch(Exception e) {}

        try {
            List<Users> users = usersRepository.findByEmail(usernameoremail);
            if (users.isEmpty()) { logger.warn("Failed to retrieve salt for email: {}", usernameoremail);}
            salt = users.getFirst().getSalt();
        } catch(Exception e) {}

        if(salt == null) {
            logger.warn("No user found for username/email: {}", usernameoremail);
            throw new RuntimeException("No salt found for the provided username or email!");
        }
        logger.info("Retrieved salt for username/email: {}", usernameoremail);

        return salt;
    }



    // ===================================================== //
    // * Cookie Service                                      //
    // ===================================================== //

    public static String createCookie(UsersRepository usersRepository, CookiesRepository cookiesRepository, Long uid) {
        if(uid == null) {
            logger.error("Invalid id");
            return null;
        }
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String value = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        Instant expires = Instant.now().plus(7, ChronoUnit.DAYS);
        if(!cookiesRepository.findByValue(value).isEmpty()) { logger.error("Cookie already exists!"); return null; }
        Cookies cookies = new Cookies();
        cookies.setUid(uid);
        cookies.setValue(value);
        cookies.setExpires(String.valueOf(expires));
        logger.info("Creating cookie for user ID: {} with value: {} expiring at: {}", uid, value, expires.toString());
        cookiesRepository.save(cookies);
        return value;
    }

    public static boolean validateCookie(CookiesRepository cookiesRepository, String value) {
        if(cookiesRepository.findByValue(value).isEmpty()) { logger.error("Cookie does not exist in db!"); return false; }
        List<Cookies> cookies = cookiesRepository.findByValue(value);
        Cookies cookie = cookies.getFirst();
        Instant expires = Instant.parse(cookie.getExpires());
        if(Instant.now().isAfter(expires)) {
            cookiesRepository.deleteByValue(value);
            logger.warn("Cookie is expired!");
            return false;
        }
        return true;
    }

    public static String getUsernameByCookie(UsersRepository usersRepository, CookiesRepository cookiesRepository, String value) { // THE value VARIABLE DOESN'T GET A VALUE FROM THE PARAMETER
        if(cookiesRepository.findByValue(value).isEmpty()) { throw new RuntimeException("Cookie does not exist in db!"); }
        List<Cookies> cookies = cookiesRepository.findByValue(value);
        Long id = cookies.getFirst().getUid();
        Users user = usersRepository.findById(id).get();
        return user.getName();
    }


    public static Map<String, Object> getTeamFromCookie( PokemonsRepository pokemonsRepository, TeamsRepository teamsRepository, CookiesRepository cookiesRepository, String value) {
        if(cookiesRepository.findByValue(value).isEmpty()) { throw new RuntimeException("Cookie does not exist in db!"); }
        List<Cookies> cookies = cookiesRepository.findByValue(value);
        Long id = cookies.getFirst().getUid();
        if(teamsRepository.findByUid(id).isEmpty()) { throw new RuntimeException("Team does not exist for this user!"); }
        Teams team = teamsRepository.findByUid(id).getFirst();
        List<String> teamMembers = new ArrayList<>();
        for (String memberId : List.of(
                team.getMem1(), team.getMem2(), team.getMem3(),
                team.getMem4(), team.getMem5(), team.getMem6())) {
            if(memberId == null || memberId.isEmpty()) { continue; }
            if(!pokemonsRepository.findById(Long.valueOf(memberId)).isEmpty()) {
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