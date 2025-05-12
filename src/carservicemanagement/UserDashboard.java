package carservicemanagement;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class UserDashboard extends JFrame {
    private int userId;
    private JTable vehicleTable;
    private DefaultTableModel vehicleTableModel;
    private JTextField licensePlateField;
    private JTextField modelField;
    private JButton addVehicleButton, bookAppointmentButton, logoutButton;
    private JTable appointmentTable;
    private DefaultTableModel appointmentTableModel;

    public UserDashboard(int userId) {
        this.userId = userId;
        
        setTitle("User Dashboard");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Main panel with two sections
        JPanel mainPanel = new JPanel(new GridLayout(2, 1, 10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Vehicle Section
        JPanel vehiclePanel = createVehiclePanel();
        mainPanel.add(vehiclePanel);

        // Appointments Section
        JPanel appointmentPanel = createAppointmentPanel();
        mainPanel.add(appointmentPanel);

        // Logout Button
        logoutButton = new JButton("Logout");
        logoutButton.addActionListener(e -> logout());

        // Add components to frame
        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);
        add(logoutButton, BorderLayout.SOUTH);

        // Load initial data
        loadVehicles();
        loadAppointments();
    }

    private JPanel createVehiclePanel() {
        JPanel vehiclePanel = new JPanel(new BorderLayout());
        vehiclePanel.setBorder(BorderFactory.createTitledBorder("My Vehicles"));

        // Vehicle Input Panel
        JPanel vehicleInputPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        vehicleInputPanel.add(new JLabel("License Plate:"));
        licensePlateField = new JTextField();
        vehicleInputPanel.add(licensePlateField);
        
        vehicleInputPanel.add(new JLabel("Model:"));
        modelField = new JTextField();
        vehicleInputPanel.add(modelField);
        
        addVehicleButton = new JButton("Add Vehicle");
        addVehicleButton.addActionListener(e -> addVehicle());
        vehicleInputPanel.add(addVehicleButton);
        
        bookAppointmentButton = new JButton("Book Appointment");
        bookAppointmentButton.addActionListener(e -> bookAppointment());
        vehicleInputPanel.add(bookAppointmentButton);

        vehiclePanel.add(vehicleInputPanel, BorderLayout.NORTH);

        // Vehicle Table
        String[] vehicleColumns = {"Vehicle ID", "License Plate", "Model"};
        vehicleTableModel = new DefaultTableModel(vehicleColumns, 0);
        vehicleTable = new JTable(vehicleTableModel);
        JScrollPane vehicleScrollPane = new JScrollPane(vehicleTable);
        vehiclePanel.add(vehicleScrollPane, BorderLayout.CENTER);

        return vehiclePanel;
    }

    private JPanel createAppointmentPanel() {
        JPanel appointmentPanel = new JPanel(new BorderLayout());
        appointmentPanel.setBorder(BorderFactory.createTitledBorder("My Appointments"));

        // Appointment Table
        String[] appointmentColumns = {"Appointment ID", "Vehicle", "Date", "Status"};
        appointmentTableModel = new DefaultTableModel(appointmentColumns, 0);
        appointmentTable = new JTable(appointmentTableModel);
        JScrollPane appointmentScrollPane = new JScrollPane(appointmentTable);
        appointmentPanel.add(appointmentScrollPane, BorderLayout.CENTER);

        return appointmentPanel;
    }

    private void loadVehicles() {
        vehicleTableModel.setRowCount(0);
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT v.id AS vehicle_id, v.license_plate, v.model FROM vehicle v " +
                     "JOIN customer c ON v.customer_id = c.id " +
                     "WHERE c.user_id = ?")) {
            
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                vehicleTableModel.addRow(new Object[]{
                    rs.getInt("vehicle_id"),
                    rs.getString("license_plate"),
                    rs.getString("model")
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, 
                "Error loading vehicles: " + ex.getMessage(), 
                "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadAppointments() {
        appointmentTableModel.setRowCount(0);
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT ba.id AS appointment_id, v.license_plate, ba.date, s.description AS status " +
                     "FROM bookappointment ba " +
                     "JOIN vehicle v ON ba.vehicle_id = v.id " +
                     "JOIN customer c ON v.customer_id = c.id " +
                     "JOIN status s ON ba.status_id = s.id " +
                     "WHERE c.user_id = ?")) {
            
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                appointmentTableModel.addRow(new Object[]{
                    rs.getInt("appointment_id"),
                    rs.getString("license_plate"),
                    rs.getString("date"),
                    rs.getString("status")
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, 
                "Error loading appointments: " + ex.getMessage(), 
                "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addVehicle() {
        String licensePlate = licensePlateField.getText().trim();
        String model = modelField.getText().trim();

        if (licensePlate.isEmpty() || model.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill all fields");
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO vehicle (customer_id, license_plate, model) " +
                     "VALUES ((SELECT id FROM customer WHERE user_id = ?), ?, ?)")) {

            stmt.setInt(1, userId);
            stmt.setString(2, licensePlate);
            stmt.setString(3, model);
            stmt.executeUpdate();

            JOptionPane.showMessageDialog(this, "Vehicle added successfully!");
            loadVehicles();
            licensePlateField.setText("");
            modelField.setText("");
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, 
                "Error adding vehicle: " + ex.getMessage(), 
                "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void bookAppointment() {
        int selectedRow = vehicleTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a vehicle first");
            return;
        }

        int vehicleId = (int) vehicleTableModel.getValueAt(selectedRow, 0);
        
        // Open a dialog to book appointment
        String date = JOptionPane.showInputDialog(this, "Enter Appointment Date (YYYY-MM-DD):");
        
        if (date != null && !date.trim().isEmpty()) {
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                         "INSERT INTO bookappointment (customer_id, vehicle_id, date, status_id) " +
                         "VALUES ((SELECT id FROM customer WHERE user_id = ?), ?, ?, " +
                         "(SELECT id FROM status WHERE description = 'Pending'))")) {

                stmt.setInt(1, userId);
                stmt.setInt(2, vehicleId);
                stmt.setString(3, date);
                stmt.executeUpdate();

                JOptionPane.showMessageDialog(this, "Appointment booked successfully!");
                loadAppointments();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, 
                    "Error booking appointment: " + ex.getMessage(), 
                    "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void logout() {
        new LoginForm().setVisible(true);
        dispose();
    }
}