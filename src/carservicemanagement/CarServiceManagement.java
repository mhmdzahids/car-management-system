package carservicemanagement;

import javax.swing.SwingUtilities;
import java.sql.Connection;
import java.sql.SQLException;

public class CarServiceManagement {
    public static void main(String[] args) {
        // Test database connection
        try {
            Connection conn = DatabaseConnection.getConnection();
            if (conn != null) {
                System.out.println("Koneksi ke database BERHASIL!");
                conn.close(); // Close the connection after testing
            }
        } catch (SQLException ex) {
            System.out.println("Koneksi ke database GAGAL!");
            ex.printStackTrace();
            return; // Exit if database connection fails
        }

        // Launch the application
        SwingUtilities.invokeLater(() -> {
            new LoginForm().setVisible(true);
        });
    }
}
