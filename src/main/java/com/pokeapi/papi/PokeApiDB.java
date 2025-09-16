package com.pokeapi.papi;

import com.pokeapi.papi.config.ConfigManager;

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
import java.util.Base64;

public class PokeApiDB {

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

    // User creation
    public static String createUser(String Username, String Email, String HashedPassword, String Salt) throws IOException {
        String insertSQL = "INSERT INTO main.users (username, email, password_hash, salt, role) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(cfg.url, cfg.username, cfg.password);
             PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {

            pstmt.setString(1, Username);
            pstmt.setString(2, Email);
            pstmt.setString(3, HashedPassword);
            pstmt.setString(4, Salt);
            pstmt.setString(5, "user");

            pstmt.executeUpdate();

        } catch (SQLException e) {
            if (String.valueOf(e).contains("Detail: Schlüssel »(username)=") && String.valueOf(e).contains("« existiert bereits.")) {
                return "Username already in use";
            } else if (String.valueOf(e).contains("Detail: Schlüssel »(email)") && String.valueOf(e).contains("« existiert bereits.")) {
                return "Email already in use";
            } else {
                return "Unknown error";
            }
        }
        return "null";
    }

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

    public static boolean checkLogin(String usernameOrEmail, String inputPassword) {
        String query = "SELECT password_hash, salt FROM main.users WHERE username=? OR email=?";

        try (Connection conn = DriverManager.getConnection(cfg.url, cfg.username, cfg.password);
             PreparedStatement pstmt = conn.prepareStatement(query)) {


            pstmt.setString(1, usernameOrEmail);
            pstmt.setString(2, usernameOrEmail);

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String storedHash = rs.getString("password_hash");
                String storedSalt = rs.getString("salt");
                String hashedInput = hashPassword(inputPassword, storedSalt);
                return storedHash.equals(hashedInput);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

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

        String insertCookie = "INSERT INTO main.cookies (user_id, cookie_value, expires_at) VALUES (?, ?, ?)";
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
        String checkIfCookieValid = "SELECT EXISTS ( SELECT 1 FROM main.cookies WHERE cookie_value=? )";

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
        String queryUserIdFromCookie = "SELECT user_id FROM main.cookies WHERE cookie_value=?";
        Integer userId = null;

        try (Connection conn = DriverManager.getConnection(cfg.url, cfg.username, cfg.password);
             PreparedStatement stmt = conn.prepareStatement(queryUserIdFromCookie)) {

            stmt.setString(1, cookieValue);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                userId = rs.getInt("user_id");
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
}















