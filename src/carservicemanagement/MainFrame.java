package carservicemanagement;

import java.awt.*;
import javax.swing.*;

public class MainFrame extends JFrame {

    public MainFrame() {
        initComponents();
    }

    private void initComponents() {
        setTitle("Car Service Management System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 400);
        setLocationRelativeTo(null); // center window

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10); // margin antar tombol

        JLabel titleLabel = new JLabel("Car Service Management System");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(titleLabel, gbc);

        // Button Customer Management
        JButton customerButton = new JButton("Customer Management");
        gbc.gridy++;
        customerButton.addActionListener(e -> openCustomerForm());
        panel.add(customerButton, gbc);

        // Button Vehicle Management
        JButton vehicleButton = new JButton("Vehicle Management");
        gbc.gridy++;
        vehicleButton.addActionListener(e -> openVehicleForm());
        panel.add(vehicleButton, gbc);

        // Button Service Record Management
        JButton serviceButton = new JButton("Service Record Management");
        gbc.gridy++;
        serviceButton.addActionListener(e -> openServiceForm());
        panel.add(serviceButton, gbc);

        // Button Appointment Management
        JButton appointmentButton = new JButton("Appointment Management");
        gbc.gridy++;
        appointmentButton.addActionListener(e -> openAppointmentForm());
        panel.add(appointmentButton, gbc);

        // Button Exit
        JButton exitButton = new JButton("Exit");
        gbc.gridy++;
        exitButton.addActionListener(e -> System.exit(0));
        panel.add(exitButton, gbc);

        add(panel);
    }

    // Method untuk buka form
    private void openCustomerForm() {
        new CustomerForm().setVisible(true);
    }

    private void openVehicleForm() {
        new VehicleForm().setVisible(true);
    }

    private void openServiceForm() {
        try {
            new ServiceForm().setVisible(true);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal membuka Service Form: " + e.getMessage());
        }
    }

    private void openAppointmentForm() {
        try {
            new AppointmentForm().setVisible(true);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal membuka Appointment Form: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new MainFrame().setVisible(true);
        });
    }
}
