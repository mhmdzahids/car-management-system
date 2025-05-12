package carservicemanagement;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.Vector;

public class AppointmentForm extends JFrame {
    private JComboBox<CustomerItem> customerComboBox;
    private JTextField appointmentDateField;
    private JComboBox<String> statusComboBox;
    private JTable appointmentTable;
    private JButton addButton, updateButton, deleteButton, refreshButton;
    private DefaultTableModel tableModel;
    private int selectedAppointmentId = -1;

    public AppointmentForm() {
        setTitle("Appointment Management");
        setSize(700, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        initComponents();
        loadData();
        setVisible(true);
    }

    private void initComponents() {
        customerComboBox = new JComboBox<>();
        appointmentDateField = new JTextField(10);
        statusComboBox = new JComboBox<>(new String[]{"Scheduled", "Confirmed", "In Progress", "Completed", "Cancelled"});
        addButton = new JButton("Add");
        updateButton = new JButton("Update");
        deleteButton = new JButton("Delete");
        refreshButton = new JButton("Refresh");
        appointmentTable = new JTable();
        
        updateButton.setEnabled(false);
        deleteButton.setEnabled(false);

        // Form panel
        JPanel formPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        formPanel.setBorder(BorderFactory.createTitledBorder("Appointment Info"));
        formPanel.add(new JLabel("Customer:"));
        formPanel.add(customerComboBox);
        formPanel.add(new JLabel("Date (YYYY-MM-DD):"));
        formPanel.add(appointmentDateField);
        formPanel.add(new JLabel("Status:"));
        formPanel.add(statusComboBox);

        // Button panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(addButton);
        buttonPanel.add(updateButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(refreshButton);

        // Layout
        setLayout(new BorderLayout());
        add(formPanel, BorderLayout.NORTH);
        add(new JScrollPane(appointmentTable), BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        // Event handlers
        addButton.addActionListener(e -> addAppointment());
        updateButton.addActionListener(e -> updateAppointment());
        deleteButton.addActionListener(e -> deleteAppointment());
        refreshButton.addActionListener(e -> loadData());

        // Table selection listener
        appointmentTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = appointmentTable.getSelectedRow();
                if (row >= 0) {
                    selectedAppointmentId = (int) appointmentTable.getValueAt(row, 0);
                    loadSelectedRecord();
                    updateButton.setEnabled(true);
                    deleteButton.setEnabled(true);
                    addButton.setEnabled(false);
                }
            }
        });
    }

    private void loadData() {
        // Load customers
        loadCustomers();
        
        // Load appointments
        String[] columns = {"ID", "Customer", "Date", "Status"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
            @Override
            public Class<?> getColumnClass(int column) {
                return column == 0 ? Integer.class : String.class;
            }
        };
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT a.id, c.name, a.appointment_date, a.status " +
                 "FROM appointment a JOIN customer c ON a.customer_id = c.id " +
                 "ORDER BY a.appointment_date")) {

            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getInt("id"));
                row.add(rs.getString("name"));
                row.add(rs.getString("appointment_date"));
                row.add(rs.getString("status"));
                tableModel.addRow(row);
            }
            appointmentTable.setModel(tableModel);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading data: " + e.getMessage());
        }
    }

    private void loadCustomers() {
        customerComboBox.removeAllItems();
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, name FROM customer")) {
            while (rs.next()) {
                customerComboBox.addItem(new CustomerItem(rs.getInt("id"), rs.getString("name")));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading customers: " + e.getMessage());
        }
    }

    private void loadSelectedRecord() {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT customer_id, appointment_date, status FROM appointment WHERE id = ?")) {
            stmt.setInt(1, selectedAppointmentId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                // Set customer
                int customerId = rs.getInt("customer_id");
                for (int i = 0; i < customerComboBox.getItemCount(); i++) {
                    if (customerComboBox.getItemAt(i).getId() == customerId) {
                        customerComboBox.setSelectedIndex(i);
                        break;
                    }
                }
                
                // Set other fields
                appointmentDateField.setText(rs.getString("appointment_date"));
                String status = rs.getString("status");
                for (int i = 0; i < statusComboBox.getItemCount(); i++) {
                    if (statusComboBox.getItemAt(i).equals(status)) {
                        statusComboBox.setSelectedIndex(i);
                        break;
                    }
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading record: " + e.getMessage());
        }
    }

    private void clearForm() {
        if (customerComboBox.getItemCount() > 0) customerComboBox.setSelectedIndex(0);
        appointmentDateField.setText("");
        if (statusComboBox.getItemCount() > 0) statusComboBox.setSelectedIndex(0);
        selectedAppointmentId = -1;
        updateButton.setEnabled(false);
        deleteButton.setEnabled(false);
        addButton.setEnabled(true);
        appointmentTable.clearSelection();
    }

    private void addAppointment() {
        CustomerItem customer = (CustomerItem) customerComboBox.getSelectedItem();
        String date = appointmentDateField.getText().trim();
        String status = (String) statusComboBox.getSelectedItem();

        if (customer == null || date.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill all required fields");
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "INSERT INTO appointment (customer_id, appointment_date, status) VALUES (?, ?, ?)")) {
            stmt.setInt(1, customer.getId());
            stmt.setString(2, date);
            stmt.setString(3, status);
            
            int result = stmt.executeUpdate();
            if (result > 0) {
                JOptionPane.showMessageDialog(this, "Appointment added successfully");
                clearForm();
                loadData();
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error adding appointment: " + e.getMessage());
        }
    }

    private void updateAppointment() {
        if (selectedAppointmentId == -1) return;
        
        CustomerItem customer = (CustomerItem) customerComboBox.getSelectedItem();
        String date = appointmentDateField.getText().trim();
        String status = (String) statusComboBox.getSelectedItem();

        if (customer == null || date.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill all required fields");
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "UPDATE appointment SET customer_id = ?, appointment_date = ?, status = ? WHERE id = ?")) {
            stmt.setInt(1, customer.getId());
            stmt.setString(2, date);
            stmt.setString(3, status);
            stmt.setInt(4, selectedAppointmentId);
            
            int result = stmt.executeUpdate();
            if (result > 0) {
                JOptionPane.showMessageDialog(this, "Appointment updated successfully");
                clearForm();
                loadData();
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error updating appointment: " + e.getMessage());
        }
    }

    private void deleteAppointment() {
        if (selectedAppointmentId == -1) return;
        
        int confirm = JOptionPane.showConfirmDialog(this, 
            "Are you sure you want to delete this appointment?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM appointment WHERE id = ?")) {
            stmt.setInt(1, selectedAppointmentId);
            
            int result = stmt.executeUpdate();
            if (result > 0) {
                JOptionPane.showMessageDialog(this, "Appointment deleted successfully");
                clearForm();
                loadData();
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error deleting appointment: " + e.getMessage());
        }
    }

    // Helper class for customer combo box
    private static class CustomerItem {
        private int id;
        private String name;

        public CustomerItem(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int getId() { return id; }
        public String toString() { return name; }
    }
}