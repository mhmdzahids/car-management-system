package carservicemanagement;

import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class RegisterForm extends JFrame {
    private JTextField nameField;
    private JTextField phoneField;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JPasswordField confirmPasswordField;

    public RegisterForm() {
        setTitle("Register");
        setSize(350, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridLayout(6, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        panel.add(new JLabel("Name:"));
        nameField = new JTextField();
        panel.add(nameField);

        panel.add(new JLabel("Phone:"));
        phoneField = new JTextField();
        panel.add(phoneField);

        panel.add(new JLabel("Username:"));
        usernameField = new JTextField();
        panel.add(usernameField);

        panel.add(new JLabel("Password:"));
        passwordField = new JPasswordField();
        panel.add(passwordField);

        panel.add(new JLabel("Confirm Password:"));
        confirmPasswordField = new JPasswordField();
        panel.add(confirmPasswordField);

        JButton registerButton = new JButton("Register");
        registerButton.addActionListener(e -> register());
        panel.add(registerButton);

        JButton backToLoginButton = new JButton("Back to Login");
        backToLoginButton.addActionListener(e -> backToLogin());
        panel.add(backToLoginButton);

        add(panel);
    }

    private void register() {
        String name = nameField.getText().trim();
        String phone = phoneField.getText().trim();
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());

        // Validation
        if (name.isEmpty() || phone.isEmpty() || username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill all fields", 
                "Registration Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!password.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(this, "Passwords do not match", 
                "Registration Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            // First, check if username already exists
            try (PreparedStatement checkStmt = conn.prepareStatement(
                    "SELECT COUNT(*) FROM users WHERE username = ?")) {
                checkStmt.setString(1, username);
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    JOptionPane.showMessageDialog(this, "Username already exists", 
                        "Registration Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            // Insert user into users table and get generated user_id
            int userId;
            try (PreparedStatement userStmt = conn.prepareStatement(
                    "INSERT INTO users (username, password, role) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                userStmt.setString(1, username);
                userStmt.setString(2, password);
                userStmt.setString(3, "user");
                userStmt.executeUpdate();
                ResultSet generatedKeys = userStmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    userId = generatedKeys.getInt(1);
                } else {
                    throw new SQLException("Failed to retrieve user ID.");
                }
            }

            // Insert customer info with user_id
            try (PreparedStatement custStmt = conn.prepareStatement(
                    "INSERT INTO customer (name, phone, user_id) VALUES (?, ?, ?)")) {
                custStmt.setString(1, name);
                custStmt.setString(2, phone);
                custStmt.setInt(3, userId);
                custStmt.executeUpdate();
            }

            JOptionPane.showMessageDialog(this, "Registration Successful!", 
                "Success", JOptionPane.INFORMATION_MESSAGE);
            backToLogin();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, 
                "Registration error: " + ex.getMessage(), 
                "Registration Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void backToLogin() {
        new LoginForm().setVisible(true);
        dispose();
    }
}