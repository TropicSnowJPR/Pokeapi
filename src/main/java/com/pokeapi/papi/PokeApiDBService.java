package com.pokeapi.papi;

import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Optional;


@Service
@Transactional
@Component
public class PokeApiDBService {

    private static final Logger logger = LoggerFactory.getLogger(PokeApiApplication.class);
    private static final SecureRandom secureRandom = new SecureRandom();

    // UserService

    public static void createUser(UsersRepository usersRepository, String name, String email, String password, String salt) {
        if(!usersRepository.findByName(name).isEmpty() || !usersRepository.findByEmail(email).isEmpty() || password.isEmpty() || salt.isEmpty()) {throw new RuntimeException("Ikd some error");}
        Users user = new Users();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(password);
        user.setSalt(salt);
        usersRepository.save(user);
    }



    public static void deleteUser(UsersRepository usersRepository, String name) {
        if(!usersRepository.findByName(name).isEmpty()) {return;}
        usersRepository.deleteByName(name);
    }

    public static void changeUsername(UsersRepository usersRepository, String name, String newName) {
        if(!usersRepository.findByName(newName).isEmpty()) {return;}
        List<Users> users = usersRepository.findByName(name);
        assert users.size() == 1;
        Users user = users.getFirst();
        user.setName(newName);
        usersRepository.save(user);
    }

    public static void changeEmail(UsersRepository usersRepository, String name, String email, String newEmail) {
        if(usersRepository.findByName(name).isEmpty() || usersRepository.findByEmail(email).isEmpty() || !usersRepository.findByEmail(newEmail).isEmpty()) {return;}
        if(email.equals(newEmail)) {return;}
        List<Users> users = usersRepository.findByName(name);
        assert users.size() == 1;
        Users user = users.getFirst();
        if(!Objects.equals(user.getEmail(), email)) {return;}
        user.setEmail(newEmail);
        usersRepository.save(user);
    }

    public static void changePassword(UsersRepository usersRepository, String name, String password, String salt, String newPassword, String newSalt) {
        if(usersRepository.findByName(name).isEmpty() || usersRepository.findBySalt(salt).isEmpty() || usersRepository.findByPassword(password).isEmpty()) {return;}
        if(password != newPassword && salt != newSalt) {return;}
        List<Users> users = usersRepository.findByName(name);
        assert users.size() == 1;
        Users user = users.getFirst();
        if(!Objects.equals(user.getPassword(), password) || !Objects.equals(user.getSalt(), salt)) {return;}
        if(!Objects.equals(password,newPassword) || !Objects.equals(salt, newSalt)) {return;}
        user.setPassword(newPassword);
        user.setSalt(newSalt);
        usersRepository.save(user);
    }

    public static Optional<Boolean> validateLogin(UsersRepository usersRepository, CookiesRepository cookiesRepository, String usernameOrEmail, String password) {
        if(usersRepository.findByName(usernameOrEmail).isEmpty() && usersRepository.findByEmail(usernameOrEmail).isEmpty()) {return Optional.empty();}
        if(usersRepository.findByPassword(password).isEmpty()) {return Optional.empty();}
        return Optional.empty();
    }


//    public static void validatePassword(UsersRepository usersRepository, String name, String password) {
//    }

    // maybe other way \/

    public static Long getIdByUsernameOrEmail(UsersRepository usersRepository, String usernameOrEmail) {
//        if(usersRepository.findByEmail(usernameOrEmail).isEmpty() && usersRepository.findByName(usernameOrEmail).isEmpty()) {return null;}
        Long id = null;
        logger.info(usernameOrEmail);
        try {
            List<Users> users = usersRepository.findByName(usernameOrEmail);
            logger.info(String.valueOf(users));
            assert users.size() == 1;
            id = users.getFirst().getId();
        } catch(Exception e) {
            logger.error("Error getting user by name: " + e.getMessage());
        }

        try {
            List<Users> users = usersRepository.findByEmail(usernameOrEmail);
            logger.info(String.valueOf(users));
            assert users.size() == 1;
            id = users.getFirst().getId();
        } catch(Exception e) {
            logger.error("Error getting user by email: " + e.getMessage());
        }

        logger.info("User ID: " + id);
        return id;
    }

    public static Optional<String> getUsernameByCookie(UsersRepository usersRepository, CookiesRepository cookiesRepository, String value) {
        if(cookiesRepository.findByValue(value).isEmpty()) { return Optional.empty(); }
        List<Cookies> cookies = cookiesRepository.findByValue(value);
        assert cookies.size() == 1;
        Long id = cookies.get(0).getUid();
        Optional<Users> userOpt = usersRepository.findById(id);
        return userOpt.map(Users::getName);
    }





    // CookieService

    public static String createCookie(UsersRepository usersRepository, CookiesRepository cookiesRepository, Long uid) {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String value = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        logger.info("Generated cookie value: " + value);
        Instant expires = Instant.now().plus(7, ChronoUnit.DAYS);
        if(!cookiesRepository.findByValue(value).isEmpty()) {return null;}
        Cookies cookies = new Cookies();
        cookies.setUid(uid);
        cookies.setValue(value);
        cookies.setExpires(String.valueOf(expires));
        cookiesRepository.save(cookies);
        logger.info("Returning cookie value: " + value);
        return value;
    }

    public static boolean validateCookie(CookiesRepository cookiesRepository, String value) {
        if(cookiesRepository.findByValue(value).isEmpty()) { return false; }
        List<Cookies> cookies = cookiesRepository.findByValue(value);
        assert cookies.size() == 1;
        Cookies cookie = cookies.getFirst();
        Instant expires = Instant.parse(cookie.getExpires());
        if(Instant.now().isAfter(expires)) {
            cookiesRepository.deleteByValue(value);
            return false;
        }
        return true;
    }
}