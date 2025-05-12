package carservicemanagement;

import java.sql.Connection;
import java.sql.SQLException;

public class CarServiceManagement {
    public static void main(String[] args) {
        try {
            Connection conn = DatabaseConnection.getConnection();
            if (conn != null) {
                System.out.println("Koneksi ke database BERHASIL!");
            }
        } catch (SQLException ex) {
            System.out.println("Koneksi ke database GAGAL!");
            ex.printStackTrace();
        }
    }
}
