package carservicemanagement;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.text.SimpleDateFormat;

public class UserDashboard extends JFrame {
    private int userId;
    private JTable vehicleTable;
    private DefaultTableModel vehicleTableModel;
    private JTextField licensePlateField;
    private JTextField modelField;
    private JButton addVehicleButton, updateVehicleButton, deleteVehicleButton, bookAppointmentButton, logoutButton;
    private JTable appointmentTable;
    private DefaultTableModel appointmentTableModel;
    private int selectedVehicleId = -1;

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
        setVisible(true);
    }

    private JPanel createVehiclePanel() {
        JPanel vehiclePanel = new JPanel(new BorderLayout());
        vehiclePanel.setBorder(BorderFactory.createTitledBorder("My Vehicles"));

        // Vehicle Input Panel
        JPanel vehicleInputPanel = new JPanel(new GridLayout(4, 2, 10, 10));
        vehicleInputPanel.add(new JLabel("License Plate:"));
        licensePlateField = new JTextField();
        vehicleInputPanel.add(licensePlateField);
        
        vehicleInputPanel.add(new JLabel("Model:"));
        modelField = new JTextField();
        vehicleInputPanel.add(modelField);
        
        addVehicleButton = new JButton("Add Vehicle");
        addVehicleButton.addActionListener(e -> addVehicle());
        vehicleInputPanel.add(addVehicleButton);
        
        updateVehicleButton = new JButton("Update Vehicle");
        updateVehicleButton.addActionListener(e -> updateVehicle());
        updateVehicleButton.setEnabled(false);
        vehicleInputPanel.add(updateVehicleButton);
        
        deleteVehicleButton = new JButton("Delete Vehicle");
        deleteVehicleButton.addActionListener(e -> deleteVehicle());
        deleteVehicleButton.setEnabled(false);
        vehicleInputPanel.add(deleteVehicleButton);
        
        bookAppointmentButton = new JButton("Book Appointment");
        bookAppointmentButton.addActionListener(e -> bookAppointment());
        vehicleInputPanel.add(bookAppointmentButton);

        vehiclePanel.add(vehicleInputPanel, BorderLayout.NORTH);

        // Vehicle Table
        String[] vehicleColumns = {"Vehicle ID", "License Plate", "Model"};
        vehicleTableModel = new DefaultTableModel(vehicleColumns, 0);
        vehicleTable = new JTable(vehicleTableModel);
        
        // Add table selection listener for auto-fill
        vehicleTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int selectedRow = vehicleTable.getSelectedRow();
                if (selectedRow >= 0) {
                    selectedVehicleId = (int) vehicleTableModel.getValueAt(selectedRow, 0);
                    licensePlateField.setText((String) vehicleTableModel.getValueAt(selectedRow, 1));
                    modelField.setText((String) vehicleTableModel.getValueAt(selectedRow, 2));
                    
                    // Enable update and delete buttons, disable add button
                    addVehicleButton.setEnabled(false);
                    updateVehicleButton.setEnabled(true);
                    deleteVehicleButton.setEnabled(true);
                }
            }
        });
        
        JScrollPane vehicleScrollPane = new JScrollPane(vehicleTable);
        vehiclePanel.add(vehicleScrollPane, BorderLayout.CENTER);
        
        // Add Clear Selection button
        JPanel vehicleButtonPanel = new JPanel(new FlowLayout());
        JButton clearSelectionButton = new JButton("Clear Selection");
        clearSelectionButton.addActionListener(e -> clearVehicleSelection());
        vehicleButtonPanel.add(clearSelectionButton);
        vehiclePanel.add(vehicleButtonPanel, BorderLayout.SOUTH);

        return vehiclePanel;
    }

    private JPanel createAppointmentPanel() {
        JPanel appointmentPanel = new JPanel(new BorderLayout());
        appointmentPanel.setBorder(BorderFactory.createTitledBorder("My Appointments"));

        // Appointment Table with View Details column
        String[] appointmentColumns = {"Appointment ID", "Vehicle Plate", "Vehicle Type", "Date", "Status", "Action"};
        appointmentTableModel = new DefaultTableModel(appointmentColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 5; // Only the Action column is editable (for button)
            }
        };
        appointmentTable = new JTable(appointmentTableModel);
        
        // Add custom button renderer and editor for the Action column
        appointmentTable.getColumn("Action").setCellRenderer(new ButtonRenderer());
        appointmentTable.getColumn("Action").setCellEditor(new ButtonEditor(new JCheckBox()));
        
        JScrollPane appointmentScrollPane = new JScrollPane(appointmentTable);
        appointmentPanel.add(appointmentScrollPane, BorderLayout.CENTER);

        return appointmentPanel;
    }

    // Custom Button Renderer
    class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            setText("View Details");
            return this;
        }
    }

    // Custom Button Editor
    class ButtonEditor extends DefaultCellEditor {
        protected JButton button;
        private String label;
        private boolean isPushed;
        private int selectedRow;

        public ButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            button = new JButton();
            button.setOpaque(true);
            button.addActionListener(e -> fireEditingStopped());
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            label = "View Details";
            button.setText(label);
            isPushed = true;
            selectedRow = row;
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            if (isPushed) {
                // Get appointment ID from the selected row
                int appointmentId = (int) appointmentTableModel.getValueAt(selectedRow, 0);
                showAppointmentDetails(appointmentId);
            }
            isPushed = false;
            return label;
        }

        @Override
        public boolean stopCellEditing() {
            isPushed = false;
            return super.stopCellEditing();
        }
    }

    private void showAppointmentDetails(int appointmentId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // First, get basic appointment and vehicle info
            PreparedStatement basicInfoStmt = conn.prepareStatement(
                "SELECT a.id, a.appointment_date, a.status, v.id as vehicle_id, v.license_plate, v.model " +
                "FROM appointment a " +
                "JOIN vehicle v ON a.vehicle_id = v.id " +
                "WHERE a.id = ?");
            
            basicInfoStmt.setInt(1, appointmentId);
            ResultSet basicRs = basicInfoStmt.executeQuery();
            
            if (basicRs.next()) {
                int vehicleId = basicRs.getInt("vehicle_id");
                
                // Build basic details
                StringBuilder details = new StringBuilder();
                details.append("Appointment Details\n\n");
                details.append(String.format("Appointment ID: %d\n", basicRs.getInt("id")));
                details.append(String.format("Date: %s\n", basicRs.getString("appointment_date")));
                details.append(String.format("Status: %s\n\n", basicRs.getString("status")));
                details.append("Vehicle Information:\n");
                details.append(String.format("License Plate: %s\n", basicRs.getString("license_plate")));
                details.append(String.format("Model: %s\n\n", basicRs.getString("model")));
                
                // Get service records for this vehicle
                PreparedStatement serviceStmt = conn.prepareStatement(
                    "SELECT service_date, cost, description FROM service_record " +
                    "WHERE vehicle_id = ? ORDER BY service_date DESC");
                
                serviceStmt.setInt(1, vehicleId);
                ResultSet serviceRs = serviceStmt.executeQuery();
                
                details.append("Service History:\n");
                double totalCost = 0;
                boolean hasServices = false;
                
                while (serviceRs.next()) {
                    hasServices = true;
                    double cost = serviceRs.getDouble("cost");
                    totalCost += cost;
                    
                    details.append(String.format("• Date: %s\n", serviceRs.getString("service_date")));
                    details.append(String.format("  Cost: Rp %.2f\n", cost));
                    
                    String description = serviceRs.getString("description");
                    if (description != null && !description.trim().isEmpty()) {
                        details.append(String.format("  Description: %s\n", description));
                    }
                    details.append("\n");
                }
                
                if (!hasServices) {
                    details.append("No service records found for this vehicle.\n\n");
                }
                
                details.append(String.format("Total Service Cost: Rp %.2f", totalCost));
                
                // Create a JTextArea for better display of multi-line text
                JTextArea textArea = new JTextArea(details.toString());
                textArea.setEditable(false);
                textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
                textArea.setCaretPosition(0);
                
                JScrollPane scrollPane = new JScrollPane(textArea);
                scrollPane.setPreferredSize(new Dimension(500, 400));
                
                JOptionPane.showMessageDialog(this, scrollPane, "Appointment Details", 
                    JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, 
                "Error loading appointment details: " + ex.getMessage(), 
                "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadVehicles() {
        vehicleTableModel.setRowCount(0);
        try (Connection conn = DatabaseConnection.getConnection()) {
            PreparedStatement findCustomerStmt = conn.prepareStatement(
                "SELECT id FROM customer WHERE user_id = ?");
            findCustomerStmt.setInt(1, userId);
            ResultSet customerRs = findCustomerStmt.executeQuery();
            
            if (customerRs.next()) {
                int customerId = customerRs.getInt("id");
                
                PreparedStatement stmt = conn.prepareStatement(
                    "SELECT id AS vehicle_id, license_plate, model FROM vehicle WHERE customer_id = ?");
                stmt.setInt(1, customerId);
                ResultSet rs = stmt.executeQuery();
                
                while (rs.next()) {
                    vehicleTableModel.addRow(new Object[]{
                        rs.getInt("vehicle_id"),
                        rs.getString("license_plate"),
                        rs.getString("model")
                    });
                }
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, 
                "Error loading vehicles: " + ex.getMessage(), 
                "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadAppointments() {
        appointmentTableModel.setRowCount(0);
        try (Connection conn = DatabaseConnection.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT a.id AS appointment_id, v.license_plate, v.model, a.appointment_date AS date, a.status " +
                "FROM appointment a " +
                "JOIN vehicle v ON a.vehicle_id = v.id " +
                "JOIN customer c ON a.customer_id = c.id " +
                "WHERE c.user_id = ?");
            
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                appointmentTableModel.addRow(new Object[]{
                    rs.getInt("appointment_id"),
                    rs.getString("license_plate"),
                    rs.getString("model"),
                    rs.getString("date"),
                    rs.getString("status"),
                    "View Details" // This will be rendered as a button
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

        try (Connection conn = DatabaseConnection.getConnection()) {
            PreparedStatement findCustomerStmt = conn.prepareStatement(
                "SELECT id FROM customer WHERE user_id = ?");
            findCustomerStmt.setInt(1, userId);
            ResultSet rs = findCustomerStmt.executeQuery();

            int customerId = -1;
            if (rs.next()) {
                customerId = rs.getInt("id");
            } else {
                PreparedStatement createCustomerStmt = conn.prepareStatement(
                    "INSERT INTO customer (user_id, name) VALUES (?, ?)", 
                    Statement.RETURN_GENERATED_KEYS);
                createCustomerStmt.setInt(1, userId);
                createCustomerStmt.setString(2, "User" + userId);
                createCustomerStmt.executeUpdate();

                ResultSet generatedKeys = createCustomerStmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    customerId = generatedKeys.getInt(1);
                }
            }

            PreparedStatement insertVehicleStmt = conn.prepareStatement(
                "INSERT INTO vehicle (customer_id, license_plate, model) VALUES (?, ?, ?)");
            insertVehicleStmt.setInt(1, customerId);
            insertVehicleStmt.setString(2, licensePlate);
            insertVehicleStmt.setString(3, model);
            insertVehicleStmt.executeUpdate();

            JOptionPane.showMessageDialog(this, "Vehicle added successfully!");
            loadVehicles();
            clearVehicleSelection();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, 
                "Error adding vehicle: " + ex.getMessage(), 
                "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateVehicle() {
        if (selectedVehicleId == -1) {
            JOptionPane.showMessageDialog(this, "Please select a vehicle to update");
            return;
        }

        String licensePlate = licensePlateField.getText().trim();
        String model = modelField.getText().trim();

        if (licensePlate.isEmpty() || model.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill all fields");
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "UPDATE vehicle SET license_plate = ?, model = ? WHERE id = ?")) {
            
            stmt.setString(1, licensePlate);
            stmt.setString(2, model);
            stmt.setInt(3, selectedVehicleId);
            
            int result = stmt.executeUpdate();
            if (result > 0) {
                JOptionPane.showMessageDialog(this, "Vehicle updated successfully!");
                loadVehicles();
                loadAppointments(); // Refresh appointments as vehicle info might be displayed there
                clearVehicleSelection();
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, 
                "Error updating vehicle: " + ex.getMessage(), 
                "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteVehicle() {
        if (selectedVehicleId == -1) {
            JOptionPane.showMessageDialog(this, "Please select a vehicle to delete");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, 
            "Are you sure you want to delete this vehicle?\nThis will also delete all associated appointments and service records.", 
            "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM vehicle WHERE id = ?")) {
            
            stmt.setInt(1, selectedVehicleId);
            int result = stmt.executeUpdate();
            
            if (result > 0) {
                JOptionPane.showMessageDialog(this, "Vehicle deleted successfully!");
                loadVehicles();
                loadAppointments(); // Refresh appointments as some might be deleted
                clearVehicleSelection();
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, 
                "Error deleting vehicle: " + ex.getMessage(), 
                "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearVehicleSelection() {
        licensePlateField.setText("");
        modelField.setText("");
        selectedVehicleId = -1;
        vehicleTable.clearSelection();
        
        // Reset button states
        addVehicleButton.setEnabled(true);
        updateVehicleButton.setEnabled(false);
        deleteVehicleButton.setEnabled(false);
    }

    private void bookAppointment() {
        int selectedRow = vehicleTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a vehicle first");
            return;
        }

        int vehicleId = (int) vehicleTableModel.getValueAt(selectedRow, 0);
        String date = JOptionPane.showInputDialog(this, "Enter Appointment Date (YYYY-MM-DD):");

        if (date != null && !date.trim().isEmpty()) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            sdf.setLenient(false);
            try {
                sdf.parse(date);
                try (Connection conn = DatabaseConnection.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(
                             "INSERT INTO appointment (customer_id, vehicle_id, appointment_date, status) " +
                             "SELECT c.id, ?, ?, 'Pending' " +
                             "FROM customer c WHERE c.user_id = ?")) {

                    stmt.setInt(1, vehicleId);
                    stmt.setString(2, date);
                    stmt.setInt(3, userId);
                    stmt.executeUpdate();

                    JOptionPane.showMessageDialog(this, "Appointment booked successfully!");
                    loadAppointments();
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(this, 
                        "Error booking appointment: " + ex.getMessage(), 
                        "Database Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (java.text.ParseException e) {
                JOptionPane.showMessageDialog(this, 
                    "Invalid date format. Please use YYYY-MM-DD.", 
                    "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void logout() {
        new LoginForm().setVisible(true);
        dispose();
    }
}