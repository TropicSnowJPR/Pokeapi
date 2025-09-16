import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class UpdateData {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://localhost:5432/mydb";
        String user = "username";
        String password = "password";

        try (Connection conn = DriverManager.getConnection(url, user, password);
             PreparedStatement pstmt = conn.prepareStatement(
                     "UPDATE students SET grade = ? WHERE name = ?")) {

            pstmt.setString(1, "B");
            pstmt.setString(2, "Alice");
            int rowsUpdated = pstmt.executeUpdate();

            System.out.println(rowsUpdated + " Zeilen aktualisiert!");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}