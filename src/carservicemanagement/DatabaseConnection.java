package carservicemanagement;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/car_service_db";
    private static final String USER = "root"; // ganti kalau user database-mu beda
    private static final String PASSWORD = ""; // ganti kalau ada password di database-mu

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
