package com.pokeapi.papi;

import com.pokeapi.papi.config.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.IOException;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.sql.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class PokeApiDB {

    private static final Logger logger = LoggerFactory.getLogger(PokeApiApplication.class);
    private static final SecureRandom secureRandom = new SecureRandom();

    private static PokeApiApplication.MyConfig cfg;

    static {
        ConfigManager<PokeApiApplication.MyConfig> cm = new ConfigManager<>(
                Paths.get("config.yml"),
                PokeApiApplication.MyConfig.class
        );
        try {
            cm.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        cfg = cm.get();
    }




    // --------------- //
    //      USERS      //
    // --------------- //



    public static void checkLogin(String usernameOrEmail, String inputPassword) {
        Users users = new Users();
    }




    // User creation
//    public static String createUser(String Username, String Email, String HashedPassword, String Salt) throws IOException {
//        String insertSQL = "INSERT INTO main.users (username, email, password_hash, salt, role) VALUES (?, ?, ?, ?, ?)";
//
//        try (Connection conn = DriverManager.getConnection(cfg.url, cfg.username, cfg.password);
//             PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
//
//            pstmt.setString(1, Username);
//            pstmt.setString(2, Email);
//            pstmt.setString(3, HashedPassword);
//            pstmt.setString(4, Salt);
//            pstmt.setString(5, "user");
//
//            pstmt.executeUpdate();
//
//        } catch (SQLException e) {
//            if (String.valueOf(e).contains("Detail: Schlüssel »(username)=") && String.valueOf(e).contains("« existiert bereits.")) {
//                return "Username already in use";
//            } else if (String.valueOf(e).contains("Detail: Schlüssel »(email)") && String.valueOf(e).contains("« existiert bereits.")) {
//                return "Email already in use";
//            } else {
//                return "Unknown error";
//            }
//        }
//        return "null";
//    }

    public static String deleteUser(String Username, String Password) {
        return null;
    }



    public static void resetAllCookies() {

        String query = "TRUNCATE main.cookies";

        try (Connection conn = DriverManager.getConnection(cfg.url, cfg.username, cfg.password);
             Statement stmt = conn.createStatement()) {
            stmt.executeQuery(query);
        } catch (SQLException e) {
            //e.printStackTrace();
        }
    }

//    public static boolean checkLogin(String usernameOrEmail, String inputPassword) {
//        String query = "SELECT password_hash, salt FROM main.users WHERE username=? OR email=?";
//
//        try (Connection conn = DriverManager.getConnection(cfg.url, cfg.username, cfg.password);
//             PreparedStatement pstmt = conn.prepareStatement(query)) {
//
//
//            pstmt.setString(1, usernameOrEmail);
//            pstmt.setString(2, usernameOrEmail);
//
//            ResultSet rs = pstmt.executeQuery();
//            if (rs.next()) {
//                String storedHash = rs.getString("password_hash");
//                String storedSalt = rs.getString("salt");
//                String hashedInput = hashPassword(inputPassword, storedSalt);
//                return storedHash.equals(hashedInput);
//            }
//
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//        return false;
//    }

    private static final int SALT_LENGTH = 16;
    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256;

    public static String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    public static String hashPassword(String password, String salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), Base64.getDecoder().decode(salt), ITERATIONS, KEY_LENGTH);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hash = skf.generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("Fehler beim Hashen des Passworts", e);
        }
    }

    public static String generateCookie(String usernameOrEmail) {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String cookieToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        Instant expiresAt = Instant.now().plus(7, ChronoUnit.DAYS);

        String queryUserId = "SELECT id FROM main.users WHERE username=? OR email=?";
        Integer userId = null;

        try (Connection conn = DriverManager.getConnection(cfg.url, cfg.username, cfg.password);
             PreparedStatement stmt = conn.prepareStatement(queryUserId)) {

            stmt.setString(1, usernameOrEmail);
            stmt.setString(2, usernameOrEmail);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                userId = rs.getInt("id");
            } else {
                throw new RuntimeException("User not found");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }

        String insertCookie = "INSERT INTO main.cookies (uid, value, expires_at) VALUES (?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(cfg.url, cfg.username, cfg.password);
             PreparedStatement stmt = conn.prepareStatement(insertCookie)) {

            stmt.setInt(1, userId);
            stmt.setString(2, cookieToken);
            stmt.setTimestamp(3, Timestamp.from(expiresAt));
            stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }

        return cookieToken;
    }

    public static boolean checkIfCookieValid(String cookieValue) {
        String checkIfCookieValid = "SELECT EXISTS ( SELECT 1 FROM main.cookies WHERE value=? )";

        try (Connection conn = DriverManager.getConnection(cfg.url, cfg.username, cfg.password);
             PreparedStatement stmt = conn.prepareStatement(checkIfCookieValid)) {

            stmt.setString(1, cookieValue);
            ResultSet rs = stmt.executeQuery();


            if (rs.next()) {
                return rs.getBoolean(1); // PostgreSQL returns boolean in the first column
            }


        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public static String getUsernameFromCookie(String cookieValue) {
        String queryUserIdFromCookie = "SELECT uid FROM main.cookies WHERE value=?";
        Integer userId = null;

        try (Connection conn = DriverManager.getConnection(cfg.url, cfg.username, cfg.password);
             PreparedStatement stmt = conn.prepareStatement(queryUserIdFromCookie)) {

            stmt.setString(1, cookieValue);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                userId = rs.getInt("uid");
            } else {
                return "invalid cookie";
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        String queryUsernameFromId = "SELECT username FROM main.users WHERE id=?";
        String username = "";

        try (Connection conn = DriverManager.getConnection(cfg.url, cfg.username, cfg.password);
             PreparedStatement stmt = conn.prepareStatement(queryUsernameFromId)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                username = rs.getString("username");
            } else {
                return "invalid cookie";
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return username;
    }

    public static Map<String, Object> getPokemonsFromUser(String cookieValue) {
        String queryPokemons = "SELECT team_member_1, team_member_2, team_member_3, team_member_4, team_member_5, team_member_6 FROM main.teams WHERE created_by=?";
        Map<String, Object> result = new HashMap<>();

        String queryUserIdFromCookie = "SELECT uid FROM main.cookies WHERE value=?";
        Integer uid = null;

        try (Connection conn = DriverManager.getConnection(cfg.url, cfg.username, cfg.password);
             PreparedStatement stmt = conn.prepareStatement(queryUserIdFromCookie)) {

            stmt.setString(1, cookieValue);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                uid = rs.getInt("uid");
            } else {
                result.put("success", false);
                return result;
            }

        } catch (SQLException e) {
            logger.error("Error fetching user ID: " + e.getMessage());
            result.put("success", false);
            return result;
        }

        if (uid == null) {
            result.put("success", false);
            return result;
        }

        List<Integer> teamMembers = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(cfg.url, cfg.username, cfg.password);
             PreparedStatement stmt = conn.prepareStatement(queryPokemons)) {

            stmt.setInt(1, uid);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                teamMembers.add(rs.getInt("team_member_1"));
                teamMembers.add(rs.getInt("team_member_2"));
                teamMembers.add(rs.getInt("team_member_3"));
                teamMembers.add(rs.getInt("team_member_4"));
                teamMembers.add(rs.getInt("team_member_5"));
                teamMembers.add(rs.getInt("team_member_6"));
                result.put("success", true);
            } else {
                // No team found for user - treat as valid empty team
                logger.info("No team found for user ID: " + uid);
                teamMembers.add(0);
                teamMembers.add(0);
                teamMembers.add(0);
                teamMembers.add(0);
                teamMembers.add(0);
                teamMembers.add(0);
                result.put("success", true);
            }

        } catch (SQLException e) {
            logger.error("Error fetching team: " + e.getMessage());
            result.put("success", false);
        }

        result.put("team_members", teamMembers);
        return result;
    }

    public static Map<String, Object> addPokemonToTeam(String nameid) {
        if (!(nameid + 0).equals(nameid)) {

        }
        return Map.of();
    }
}















