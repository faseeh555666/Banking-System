import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.sql.*;
import java.util.zip.*;

public class BankingSystem extends JFrame {
    // DB connection details - replace with your settings
    private static final String DB_URL = "jdbc:mysql://localhost:3306/bankingdb";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "";

    private JTextField nameField;
    private JTextField balanceField;
    private JTable customerTable;
    private DefaultTableModel tableModel;

    public BankingSystem() {
        setTitle("Banking System");
        setSize(600, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initComponents();
        loadCustomers();
    }

    private void initComponents() {
        JPanel panel = new JPanel(new BorderLayout());

        // Input form panel
        JPanel formPanel = new JPanel(new FlowLayout());

        formPanel.add(new JLabel("Name:"));
        nameField = new JTextField(15);
        formPanel.add(nameField);

        formPanel.add(new JLabel("Balance:"));
        balanceField = new JTextField(10);
        formPanel.add(balanceField);

        JButton addButton = new JButton("Add Customer");
        formPanel.add(addButton);

        JButton exportButton = new JButton("Export & ZIP");
        formPanel.add(exportButton);

        panel.add(formPanel, BorderLayout.NORTH);

        // Table for customers
        tableModel = new DefaultTableModel(new Object[]{"ID", "Name", "Balance"}, 0);
        customerTable = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(customerTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        add(panel);

        // Button actions
        addButton.addActionListener(e -> addCustomer());
        exportButton.addActionListener(e -> exportAndZip());
    }

    private void loadCustomers() {
        tableModel.setRowCount(0); // Clear table
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM customers")) {

            while (rs.next()) {
                Object[] row = {
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getDouble("balance")
                };
                tableModel.addRow(row);
            }
        } catch (SQLException e) {
            showError("Error loading customers: " + e.getMessage());
        }
    }

    private void addCustomer() {
        String name = nameField.getText().trim();
        String balanceStr = balanceField.getText().trim();

        if (name.isEmpty() || balanceStr.isEmpty()) {
            showError("Please enter both name and balance.");
            return;
        }

        double balance;
        try {
            balance = Double.parseDouble(balanceStr);
        } catch (NumberFormatException e) {
            showError("Balance must be a valid number.");
            return;
        }

        String sql = "INSERT INTO customers (name, balance) VALUES (?, ?)";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, name);
            pstmt.setDouble(2, balance);
            pstmt.executeUpdate();

            showMessage("Customer added successfully.");
            nameField.setText("");
            balanceField.setText("");
            loadCustomers();

        } catch (SQLException e) {
            showError("Error adding customer: " + e.getMessage());
        }
    }

    private void exportAndZip() {
        String txtFile = "customers.txt";
        String zipFile = "customers_data.zip";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM customers");
             BufferedWriter writer = new BufferedWriter(new FileWriter(txtFile))) {

            // Write data to text file
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                double balance = rs.getDouble("balance");
                writer.write(id + "," + name + "," + balance);
                writer.newLine();
            }
            writer.close();

            // Zip the text file
            try (
                FileOutputStream fos = new FileOutputStream(zipFile);
                ZipOutputStream zos = new ZipOutputStream(fos);
                FileInputStream fis = new FileInputStream(txtFile);
            ) {
                ZipEntry zipEntry = new ZipEntry(txtFile);
                zos.putNextEntry(zipEntry);

                byte[] buffer = new byte[1024];
                int length;
                while ((length = fis.read(buffer)) >= 0) {
                    zos.write(buffer, 0, length);
                }
            }

            showMessage("Exported and zipped successfully: " + zipFile);

        } catch (Exception e) {
            showError("Error exporting data: " + e.getMessage());
        }
    }

    private void showMessage(String message) {
        JOptionPane.showMessageDialog(this, message);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        // Load JDBC driver
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            JOptionPane.showMessageDialog(null, "MySQL JDBC Driver not found.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        SwingUtilities.invokeLater(() -> {
            BankingSystem app = new BankingSystem();
            app.setVisible(true);
        });
    }
}
