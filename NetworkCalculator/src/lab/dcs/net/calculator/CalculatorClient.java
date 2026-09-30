package lab.dcs.net.calculator;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

public class CalculatorClient extends JFrame {

    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 1234;

    // GUI components
    private JTextField num1Field;
    private JTextField num2Field;
    private JTextField resultField;
    private JButton addButton;
    private JButton subButton;
    private JButton mulButton;
    private JButton divButton;
    private JLabel statusLabel;

    // Network components
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public CalculatorClient() {
        super("Network Calculator - Client");
        initGUI();
        connectToServer();
    }

    private void initGUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        // Main panel with padding
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // === Input panel ===
        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBorder(BorderFactory.createTitledBorder("Input"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Number 1
        gbc.gridx = 0; gbc.gridy = 0;
        inputPanel.add(new JLabel("Number 1:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        num1Field = new JTextField(12);
        num1Field.setFont(new Font("Consolas", Font.PLAIN, 14));
        inputPanel.add(num1Field, gbc);

        // Number 2
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        inputPanel.add(new JLabel("Number 2:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        num2Field = new JTextField(12);
        num2Field.setFont(new Font("Consolas", Font.PLAIN, 14));
        inputPanel.add(num2Field, gbc);

        mainPanel.add(inputPanel, BorderLayout.NORTH);

        // === Buttons panel ===
        JPanel buttonPanel = new JPanel(new GridLayout(1, 4, 10, 10));
        buttonPanel.setBorder(BorderFactory.createTitledBorder("Operation"));

        addButton = new JButton("+");
        subButton = new JButton("-");
        mulButton = new JButton("*");
        divButton = new JButton("/");

        Font btnFont = new Font("SansSerif", Font.BOLD, 18);
        for (JButton btn : new JButton[]{addButton, subButton, mulButton, divButton}) {
            btn.setFont(btnFont);
            btn.setFocusPainted(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            buttonPanel.add(btn);
        }

        mainPanel.add(buttonPanel, BorderLayout.CENTER);

        // === Result panel ===
        JPanel resultPanel = new JPanel(new BorderLayout(5, 5));
        resultPanel.setBorder(BorderFactory.createTitledBorder("Result"));

        resultField = new JTextField();
        resultField.setFont(new Font("Consolas", Font.BOLD, 16));
        resultField.setEditable(false);
        resultField.setBackground(new Color(245, 245, 245));
        resultPanel.add(resultField, BorderLayout.CENTER);

        // Status label
        statusLabel = new JLabel("Connecting...");
        statusLabel.setFont(new Font("SansSerif", Font.ITALIC, 11));
        resultPanel.add(statusLabel, BorderLayout.SOUTH);

        mainPanel.add(resultPanel, BorderLayout.SOUTH);

        // === Action listeners ===
        addButton.addActionListener(e -> sendCalculation("+"));
        subButton.addActionListener(e -> sendCalculation("-"));
        mulButton.addActionListener(e -> sendCalculation("*"));
        divButton.addActionListener(e -> sendCalculation("/"));

        // Close hook: gracefully disconnect
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                disconnect();
            }
        });

        setContentPane(mainPanel);
        pack();
        setLocationRelativeTo(null); // center on screen
        setVisible(true);
    }

    private void connectToServer() {
        try {
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(
                    new BufferedWriter(
                            new OutputStreamWriter(socket.getOutputStream())), true);
            statusLabel.setText("Connected to " + SERVER_HOST + ":" + SERVER_PORT);
            statusLabel.setForeground(new Color(0, 128, 0));
        } catch (IOException e) {
            statusLabel.setText("Connection failed: " + e.getMessage());
            statusLabel.setForeground(Color.RED);
            setButtonsEnabled(false);
            JOptionPane.showMessageDialog(this,
                    "Could not connect to server at " + SERVER_HOST + ":" + SERVER_PORT
                            + "\nMake sure the CalculatorServer is running first!",
                    "Connection Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void sendCalculation(String operation) {
        String n1 = num1Field.getText().trim();
        String n2 = num2Field.getText().trim();

        // Client-side validation
        if (n1.isEmpty() || n2.isEmpty()) {
            resultField.setText("Please enter both numbers!");
            return;
        }

        try {
            Double.parseDouble(n1);
            Double.parseDouble(n2);
        } catch (NumberFormatException ex) {
            resultField.setText("Invalid number format!");
            return;
        }

        // Build request and send to server
        String request = n1 + ";" + operation + ";" + n2;
        try {
            out.println(request);
            out.flush();
            String response = in.readLine();
            if (response != null) {
                resultField.setText(response);
            } else {
                resultField.setText("Server closed the connection.");
                statusLabel.setText("Disconnected");
                statusLabel.setForeground(Color.RED);
                setButtonsEnabled(false);
            }
        } catch (IOException e) {
            resultField.setText("Communication error: " + e.getMessage());
            statusLabel.setText("Disconnected");
            statusLabel.setForeground(Color.RED);
            setButtonsEnabled(false);
        }
    }

    private void setButtonsEnabled(boolean enabled) {
        addButton.setEnabled(enabled);
        subButton.setEnabled(enabled);
        mulButton.setEnabled(enabled);
        divButton.setEnabled(enabled);
    }

    private void disconnect() {
        try {
            if (out != null) {
                out.println("END");
                out.flush();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            // ignore on shutdown
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CalculatorClient());
    }
}
