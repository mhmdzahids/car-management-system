package carservicemanagement;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class ServiceForm extends JFrame {
    private JTable serviceTable;
    private DefaultTableModel tableModel;
    private JComboBox<VehicleItem> vehicleCombo;
    private JTextField dateField, costField, filterField;
    private JTextArea descArea;
    private int selectedId = -1;

    public ServiceForm() {
        setTitle("Service Management");
        setSize(700, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Form Input
        JPanel inputPanel = new JPanel(new GridLayout(4, 2, 10, 10));
        inputPanel.setBorder(BorderFactory.createTitledBorder("Service Info"));

        vehicleCombo = new JComboBox<>();
        dateField = new JTextField();
        costField = new JTextField();
        descArea = new JTextArea(3, 20);

        inputPanel.add(new JLabel("Vehicle:"));
        inputPanel.add(vehicleCombo);
        inputPanel.add(new JLabel("Date:"));
        inputPanel.add(dateField);
        inputPanel.add(new JLabel("Cost:"));
        inputPanel.add(costField);
        inputPanel.add(new JLabel("Description:"));
        inputPanel.add(new JScrollPane(descArea));

        add(inputPanel, BorderLayout.NORTH);

        // Table
        tableModel = new DefaultTableModel(new String[]{"ID", "Vehicle", "Date", "Cost", "Description"}, 0);
        serviceTable = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(serviceTable);
        add(scrollPane, BorderLayout.CENTER);

        // Filter
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterField = new JTextField(15);
        filterPanel.add(new JLabel("Filter:"));
        filterPanel.add(filterField);
        add(filterPanel, BorderLayout.WEST);

        // Buttons
        JPanel buttonPanel = new JPanel();
        JButton addButton = new JButton("Add");
        JButton updateButton = new JButton("Update");
        JButton clearButton = new JButton("Clear");

        buttonPanel.add(addButton);
        buttonPanel.add(updateButton);
        buttonPanel.add(clearButton);

        add(buttonPanel, BorderLayout.SOUTH);

        // Load data awal
        loadVehicles();
        loadServices();

        // Event
        addButton.addActionListener(e -> addService());
        updateButton.addActionListener(e -> updateService());
        clearButton.addActionListener(e -> clearFields());

        serviceTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int row = serviceTable.getSelectedRow();
                if (row != -1) {
                    selectedId = (int) tableModel.getValueAt(row, 0);
                    vehicleCombo.setSelectedItem(new VehicleItem(0, tableModel.getValueAt(row, 1).toString()));
                    dateField.setText(tableModel.getValueAt(row, 2).toString());
                    costField.setText(tableModel.getValueAt(row, 3).toString());
                    descArea.setText(tableModel.getValueAt(row, 4).toString());
                }
            }
        });

        filterField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { applyFilter(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { applyFilter(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { applyFilter(); }
        });

        setVisible(true);
    }

    private void loadVehicles() {
        vehicleCombo.removeAllItems();
        try (Connection c = DatabaseConnection.getConnection();
             ResultSet rs = c.createStatement().executeQuery("SELECT id, license_plate FROM vehicle")) {
            while (rs.next()) {
                vehicleCombo.addItem(new VehicleItem(rs.getInt(1), rs.getString(2)));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading vehicles: " + e.getMessage());
        }
    }

    private void loadServices() {
        tableModel.setRowCount(0);
        try (Connection c = DatabaseConnection.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT sr.id, v.license_plate, sr.service_date, sr.cost, sr.description FROM service_record sr JOIN vehicle v ON sr.vehicle_id = v.id")) {
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                        rs.getInt(1), rs.getString(2), rs.getString(3), rs.getDouble(4), rs.getString(5)
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading services: " + e.getMessage());
        }
    }

    private void addService() {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("INSERT INTO service_record (vehicle_id, service_date, cost, description) VALUES (?, ?, ?, ?)");) {
            ps.setInt(1, ((VehicleItem) vehicleCombo.getSelectedItem()).id);
            ps.setString(2, dateField.getText());
            ps.setDouble(3, Double.parseDouble(costField.getText()));
            ps.setString(4, descArea.getText());
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Service added.");
            loadServices();
            clearFields();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    private void updateService() {
        if (selectedId < 0) {
            JOptionPane.showMessageDialog(this, "Please select a service to update.");
            return;
        }
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE service_record SET vehicle_id=?, service_date=?, cost=?, description=? WHERE id=?")) {
            ps.setInt(1, ((VehicleItem) vehicleCombo.getSelectedItem()).id);
            ps.setString(2, dateField.getText());
            ps.setDouble(3, Double.parseDouble(costField.getText()));
            ps.setString(4, descArea.getText());
            ps.setInt(5, selectedId);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Service updated.");
            loadServices();
            clearFields();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    private void clearFields() {
        dateField.setText("");
        costField.setText("");
        descArea.setText("");
        selectedId = -1;
        vehicleCombo.setSelectedIndex(0);
    }

    private void applyFilter() {
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
        sorter.setRowFilter(RowFilter.regexFilter("(?i)" + filterField.getText()));
        serviceTable.setRowSorter(sorter);
    }

    private static class VehicleItem {
        int id;
        String plate;

        VehicleItem(int id, String plate) {
            this.id = id;
            this.plate = plate;
        }

        public String toString() {
            return plate;
        }

        public boolean equals(Object o) {
            if (o instanceof VehicleItem other) return plate.equals(other.plate);
            return false;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ServiceForm::new);
    }
}