import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Base64;

public class InsertData {

    public static void main(String[] args) {
        // Take username and password from args or use defaults
        String username = args.length > 0 ? args[0] : "Henriette";
        String userPassword = args.length > 1 ? args[1] : "Hallo";

        String url = "jdbc:postgresql://127.0.0.1:5432/pokeapi";
        String user = "pokeapi-user";
        String password = "dbPoKeAP!";

        String insertSQL = "INSERT INTO main.users (id, username, password_hash, salt, role, created_at) VALUES (?, ?, ?, ?, ?, ?)";

        int id = 1;
        id = getLastID();
        id = id + 1;

        LocalDateTime date = LocalDateTime.now();

        String salt = generateSalt();
        String hashedUserPassword = hashPassword(userPassword, salt);

        try (Connection conn = DriverManager.getConnection(url, user, password);
             PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {

            pstmt.setInt(1, id);
            pstmt.setString(2, username);             // username
            pstmt.setString(3, hashedUserPassword);
            pstmt.setString(4, salt);// password
            pstmt.setString(5, "user");               // role
            pstmt.setTimestamp(6, Timestamp.valueOf(date)); // created_at

            int rows = pstmt.executeUpdate();
            System.out.println(rows + " row(s) inserted.");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static int getLastID() {
        String url = "jdbc:postgresql://127.0.0.1:5432/pokeapi";
        String user = "pokeapi-user";
        String password = "dbPoKeAP!";

        String sql = "SELECT id FROM main.users ORDER BY id DESC LIMIT 1";

        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt("id");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return 1;
    }

    private static final int SALT_LENGTH = 16; // 16 Bytes
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

}
