package Student;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class StudentFees extends JPanel {

    private JLabel pendingFeeAmountLabel;
    private JTable paymentHistoryTable;
    private DefaultTableModel paymentHistoryModel;
    private JButton downloadButton;
    private String studentUsername;
    private final Color primaryColor = new Color(41, 128, 185);
    private final Color secondaryColor = new Color(52, 73, 94);
    private final Color textColor = Color.WHITE;
    private final Font mainFont = new Font("Segoe UI", Font.PLAIN, 16);
    private final Font boldFont = new Font("Segoe UI", Font.BOLD, 18);
    private final Font titleFont = new Font("Segoe UI", Font.BOLD, 24);
    private final DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MMMM");
    private final DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH"); // Adjust if 'XX' is not hours

    public StudentFees(String username) {
        this.studentUsername = username;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(800, 600));
        setBorder(BorderFactory.createEmptyBorder(30, 50, 30, 50));

        // Title
        JLabel titleLabel = new JLabel("Payment History", SwingConstants.CENTER);
        titleLabel.setFont(titleFont);
        titleLabel.setForeground(primaryColor);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(titleLabel);
        add(Box.createVerticalStrut(40));

        // Pending Fee Section
        JPanel pendingFeePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        pendingFeePanel.add(new JLabel("Pending Fee: "));
        JLabel pendingLabelText = new JLabel("Rs.");
        pendingLabelText.setFont(mainFont);
        pendingFeePanel.add(pendingLabelText);
        pendingFeeAmountLabel = new JLabel("Loading...");
        pendingFeeAmountLabel.setFont(boldFont);
        pendingFeePanel.add(pendingFeeAmountLabel);
        pendingFeePanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(pendingFeePanel);
        add(Box.createVerticalStrut(30));

        // Payment History Table
        JLabel historyLabel = new JLabel("Payment Details");
        historyLabel.setFont(boldFont);
        historyLabel.setForeground(secondaryColor);
        historyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(historyLabel);
        add(Box.createVerticalStrut(15));

        paymentHistoryModel = new DefaultTableModel(new String[]{"Month", "Credit (Rs.)", "Remaining (Rs.)", "Status"}, 0);
        paymentHistoryTable = new JTable(paymentHistoryModel);
        paymentHistoryTable.setFont(mainFont);
        paymentHistoryTable.setRowHeight(25);
        paymentHistoryTable.getTableHeader().setFont(mainFont);

        // Center align table cells
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < paymentHistoryTable.getColumnCount(); i++) {
            paymentHistoryTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        JScrollPane historyScrollPane = new JScrollPane(paymentHistoryTable);
        historyScrollPane.setPreferredSize(new Dimension(600, 200));
        historyScrollPane.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(historyScrollPane);
        add(Box.createVerticalStrut(30));

        // Download Button
        downloadButton = new JButton("Download Payment History (CSV)");
        downloadButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        downloadButton.setMaximumSize(new Dimension(300, 50));
        downloadButton.setBackground(primaryColor);
        downloadButton.setForeground(textColor);
        downloadButton.setFont(mainFont);
        downloadButton.setFocusPainted(false);
        downloadButton.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        downloadButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                downloadPaymentHistory();
            }
        });
        add(downloadButton);

        loadPendingFee();
        loadPaymentHistoryFromFeeTable();
    }

    private void loadPendingFee() {
        try (Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/hostelms", "root", "");
             PreparedStatement pst = con.prepareStatement("SELECT SUM(f_remaining) AS total_remaining FROM fee WHERE std_id = (SELECT std_id FROM student WHERE std_username = ?) AND f_status != 'Paid'")) {
            pst.setString(1, studentUsername);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                double totalRemaining = rs.getDouble("total_remaining");
                pendingFeeAmountLabel.setText(String.format("%,.2f", totalRemaining));
            } else {
                pendingFeeAmountLabel.setText("0.00");
            }
        } catch (SQLException ex) {
            pendingFeeAmountLabel.setText("Error");
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading pending fee: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadPaymentHistoryFromFeeTable() {
        paymentHistoryModel.setRowCount(0);
        try (Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/hostelms", "root", "");
             PreparedStatement pst = con.prepareStatement("SELECT f_date_month, f_credit, f_remaining, f_status FROM fee WHERE std_id = (SELECT std_id FROM student WHERE std_username = ?) ORDER BY f_date_month DESC")) {
            pst.setString(1, studentUsername);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                String rawDate = rs.getString("f_date_month");
                try {
                    LocalDate dateMonth = LocalDate.parse(rawDate, inputFormatter);
                    paymentHistoryModel.addRow(new Object[]{
                            outputFormatter.format(dateMonth),
                            String.format("%,.2f", rs.getDouble("f_credit")),
                            String.format("%,.2f", rs.getDouble("f_remaining")),
                            rs.getString("f_status")
                    });
                } catch (DateTimeParseException e) {
                    System.err.println("Error parsing date: " + rawDate + " - " + e.getMessage());
                    paymentHistoryModel.addRow(new Object[]{
                            rawDate, // Show the raw value if parsing fails
                            String.format("%,.2f", rs.getDouble("f_credit")),
                            String.format("%,.2f", rs.getDouble("f_remaining")),
                            rs.getString("f_status")
                    });
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading payment history from fee table: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void downloadPaymentHistory() {
        try (Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/hostelms", "root", "");
             PreparedStatement pst = con.prepareStatement("SELECT f_date_month, f_credit, f_remaining, f_status FROM fee WHERE std_id = (SELECT std_id FROM student WHERE std_username = ?) ORDER BY f_date_month DESC");
             FileWriter writer = new FileWriter("payment_history_" + studentUsername + ".csv")) {

            writer.append("Month,Credit (Rs.),Remaining (Rs.),Status\n");
            pst.setString(1, studentUsername);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                String rawDate = rs.getString("f_date_month");
                String formattedMonth;
                try {
                    LocalDate dateMonth = LocalDate.parse(rawDate, inputFormatter);
                    formattedMonth = outputFormatter.format(dateMonth);
                } catch (DateTimeParseException e) {
                    formattedMonth = rawDate; // Use raw value if parsing fails
                    System.err.println("Error parsing date for download: " + rawDate + " - " + e.getMessage());
                }
                writer.append(formattedMonth).append(",");
                writer.append(String.format("%.2f", rs.getDouble("f_credit"))).append(",");
                writer.append(String.format("%.2f", rs.getDouble("f_remaining"))).append(",");
                writer.append(rs.getString("f_status")).append("\n");
            }
            JOptionPane.showMessageDialog(this, "Payment history downloaded to payment_history_" + studentUsername + ".csv");

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error fetching payment history from fee table for download: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error saving payment history to file: " + ex.getMessage(), "File Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Student Fees");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.add(new StudentFees("testuser"));
        frame.setVisible(true);
    }
}