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


@Service
@Transactional
@Component
public class PokeApiDBService {

    private static final Logger logger = LoggerFactory.getLogger(PokeApiApplication.class);
    private static final SecureRandom secureRandom = new SecureRandom();



    // ===================================================== //
    // * UserService                                         //
    // ===================================================== //

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
        Users user = users.getFirst();
        user.setName(newName);
        usersRepository.save(user);
    }

    public static void changeEmail(UsersRepository usersRepository, String name, String email, String newEmail) {
        if(usersRepository.findByName(name).isEmpty() || usersRepository.findByEmail(email).isEmpty() || !usersRepository.findByEmail(newEmail).isEmpty()) {return;}
        if(email.equals(newEmail)) {return;}
        List<Users> users = usersRepository.findByName(name);
        Users user = users.getFirst();
        if(!Objects.equals(user.getEmail(), email)) {return;}
        user.setEmail(newEmail);
        usersRepository.save(user);
    }

    public static void changePassword(UsersRepository usersRepository, String name, String password, String salt, String newPassword, String newSalt) {
        if(usersRepository.findByName(name).isEmpty() || usersRepository.findBySalt(salt).isEmpty() || usersRepository.findByPassword(password).isEmpty()) {return;}
        if(password != newPassword && salt != newSalt) {return;}
        List<Users> users = usersRepository.findByName(name);
        Users user = users.getFirst();
        if(!Objects.equals(user.getPassword(), password) || !Objects.equals(user.getSalt(), salt)) {return;}
        if(!Objects.equals(password,newPassword) || !Objects.equals(salt, newSalt)) {return;}
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
    // * ConvertService                                      //
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
            return null;
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
            return null;
        }
        logger.info("Retrieved salt for username/email: {}", usernameoremail);

        return salt;
    }



    // ===================================================== //
    // * CookieService                                       //
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
        if(cookiesRepository.findByValue(value).isEmpty()) { logger.error("Cookie does not exist in db!"); return null; }
        List<Cookies> cookies = cookiesRepository.findByValue(value);
        Long id = cookies.getFirst().getUid();
        Users user = usersRepository.findById(id).get();
        return user.getName();
    }



}