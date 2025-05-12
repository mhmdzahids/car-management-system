package carservicemanagement;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class VehicleForm extends JFrame {
    private JTextField licensePlateField;
    private JTextField modelField;
    private JComboBox<CustomerItem> customerComboBox;
    private JButton addButton, updateButton, deleteButton;
    private JTable vehicleTable;
    private DefaultListModel<String> listModel;
    private int selectedId = -1;

    public VehicleForm() {
        setTitle("Vehicle Management");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        initComponents();
        loadVehicles();
        loadCustomers();

        setVisible(true);
    }

    private void initComponents() {
        licensePlateField = new JTextField(15);
        modelField = new JTextField(15);
        customerComboBox = new JComboBox<>();

        addButton = new JButton("Add");
        updateButton = new JButton("Update");
        deleteButton = new JButton("Delete");

        vehicleTable = new JTable();
        JScrollPane scrollPane = new JScrollPane(vehicleTable);

        JPanel inputPanel = new JPanel(new GridLayout(4, 2));
        inputPanel.add(new JLabel("License Plate:"));
        inputPanel.add(licensePlateField);
        inputPanel.add(new JLabel("Model:"));
        inputPanel.add(modelField);
        inputPanel.add(new JLabel("Customer:"));
        inputPanel.add(customerComboBox);
        inputPanel.add(addButton);
        inputPanel.add(updateButton);
        inputPanel.add(deleteButton);

        setLayout(new BorderLayout());
        add(inputPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        vehicleTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int row = vehicleTable.getSelectedRow();
                if (row != -1) {
                    selectedId = (int) vehicleTable.getValueAt(row, 0);
                    licensePlateField.setText((String) vehicleTable.getValueAt(row, 1));
                    modelField.setText((String) vehicleTable.getValueAt(row, 2));
                    selectCustomerById((int) vehicleTable.getValueAt(row, 3));
                }
            }
        });

        addButton.addActionListener(e -> addVehicle());
        updateButton.addActionListener(e -> updateVehicle());
        deleteButton.addActionListener(e -> deleteVehicle());
    }

    private void loadVehicles() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT vehicle.id, vehicle.license_plate, vehicle.model, vehicle.customer_id FROM vehicle")) {

            String[] columns = {"ID", "License Plate", "Model", "Customer ID"};
            Object[][] data = new Object[50][4];
            int i = 0;

            while (rs.next()) {
                data[i][0] = rs.getInt("id");
                data[i][1] = rs.getString("license_plate");
                data[i][2] = rs.getString("model");
                data[i][3] = rs.getInt("customer_id");
                i++;
            }

            vehicleTable.setModel(new javax.swing.table.DefaultTableModel(data, columns));
        } catch (SQLException ex) {
            ex.printStackTrace();
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
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private void selectCustomerById(int id) {
        for (int i = 0; i < customerComboBox.getItemCount(); i++) {
            CustomerItem item = customerComboBox.getItemAt(i);
            if (item.getId() == id) {
                customerComboBox.setSelectedIndex(i);
                return;
            }
        }
    }

    private void addVehicle() {
        String licensePlate = licensePlateField.getText();
        String model = modelField.getText();
        CustomerItem selectedCustomer = (CustomerItem) customerComboBox.getSelectedItem();
        if (licensePlate.isEmpty() || model.isEmpty() || selectedCustomer == null) {
            JOptionPane.showMessageDialog(this, "Please fill all fields");
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO vehicle (license_plate, model, customer_id) VALUES (?, ?, ?)")) {

            stmt.setString(1, licensePlate);
            stmt.setString(2, model);
            stmt.setInt(3, selectedCustomer.getId());
            stmt.executeUpdate();
            JOptionPane.showMessageDialog(this, "Vehicle added!");
            loadVehicles();
            clearForm();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private void updateVehicle() {
        if (selectedId == -1) {
            JOptionPane.showMessageDialog(this, "Please select a vehicle first");
            return;
        }

        String licensePlate = licensePlateField.getText();
        String model = modelField.getText();
        CustomerItem selectedCustomer = (CustomerItem) customerComboBox.getSelectedItem();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE vehicle SET license_plate=?, model=?, customer_id=? WHERE id=?")) {

            stmt.setString(1, licensePlate);
            stmt.setString(2, model);
            stmt.setInt(3, selectedCustomer.getId());
            stmt.setInt(4, selectedId);
            stmt.executeUpdate();
            JOptionPane.showMessageDialog(this, "Vehicle updated!");
            loadVehicles();
            clearForm();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private void deleteVehicle() {
        if (selectedId == -1) {
            JOptionPane.showMessageDialog(this, "Please select a vehicle first");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure to delete?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM vehicle WHERE id=?")) {

            stmt.setInt(1, selectedId);
            stmt.executeUpdate();
            JOptionPane.showMessageDialog(this, "Vehicle deleted!");
            loadVehicles();
            clearForm();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private void clearForm() {
        licensePlateField.setText("");
        modelField.setText("");
        selectedId = -1;
        customerComboBox.setSelectedIndex(-1);
    }

    // Helper class untuk ComboBox Customer
    private static class CustomerItem {
        private int id;
        private String name;

        public CustomerItem(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int getId() {
            return id;
        }

        public String toString() {
            return name;
        }
    }
}
