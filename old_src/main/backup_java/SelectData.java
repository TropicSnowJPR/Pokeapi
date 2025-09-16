import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.*;
import java.util.Base64;

public class SelectData {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://127.0.0.1:5432/pokeapi";
        String user = "pokeapi-user";
        String password = "dbPoKeAP!";

        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM main.users WHERE username = 'Henriette'")) {

            String salt = null;
            String hashedUserPassword = null;
            while (rs.next()) {
                hashedUserPassword = rs.getString("password_hash");
                salt = rs.getString("salt");
            }

            System.out.println("Salt: " + salt);
            System.out.println("Hash: " + hashedUserPassword);
            System.out.println("Prüfung richtig: " + verifyPassword("Hallo", salt, hashedUserPassword));

            rs.close();
            conn.close();

            System.out.println("Connection closed");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256;

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

    public static boolean verifyPassword(String password, String salt, String expectedHash) {
        String hash = hashPassword(password, salt);
        return hash.equals(expectedHash);
    }
}
