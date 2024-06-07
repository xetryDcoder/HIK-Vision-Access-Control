import java.io.*;
import java.net.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;


public class AccessControlSync extends JFrame {
    private JTextField ipTextField;
    private JTextArea outputTextArea;

    public AccessControlSync() {
        setTitle("Access Control Synchronization");
        setSize(400, 300);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel mainPanel = new JPanel(new BorderLayout());

        JPanel inputPanel = new JPanel(new FlowLayout());
        JLabel ipLabel = new JLabel("IP Address:");
        ipTextField = new JTextField(15);
        inputPanel.add(ipLabel);
        inputPanel.add(ipTextField);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton synchronizeButton = new JButton("Synchronize");
        synchronizeButton.addActionListener(new SynchronizeButtonListener());
        buttonPanel.add(synchronizeButton);

        outputTextArea = new JTextArea(10, 30);
        JScrollPane scrollPane = new JScrollPane(outputTextArea);

        mainPanel.add(inputPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);

        // Start the listener thread
        new NodeDataListener().start();
    }

    private class SynchronizeButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            String ipAddress = ipTextField.getText();
            if (!ipAddress.isEmpty()) {
                try {
                    ProcessBuilder pb = new ProcessBuilder("python", "AccessControllAccess.py", ipAddress);
                    pb.redirectErrorStream(true);
                    Process process = pb.start();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                    StringBuilder output = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                    }

                    int exitCode = process.waitFor();
                    outputTextArea.setText(output.toString());
                    if (exitCode == 0) {
                        String fileName = "access_control_events_" + getCurrentDate() + ".json";
                        saveJSON(output.toString(), fileName);
                    } else {
                        outputTextArea.append("Failed to synchronize data.\n");
                    }
                } catch (IOException | InterruptedException ex) {
                    ex.printStackTrace();
                    outputTextArea.setText("Error occurred: " + ex.getMessage());
                }
            } else {
                outputTextArea.setText("Please provide IP address.");
            }
        }
    }

    private void saveJSON(String data, String fileName) {
        try (PrintWriter writer = new PrintWriter(fileName)) {
            writer.println(data);
            outputTextArea.append("Data saved to " + fileName + "\n");
        } catch (IOException e) {
            e.printStackTrace();
            outputTextArea.append("Error occurred while saving data to " + fileName + ": " + e.getMessage() + "\n");
        }
    }

    private String getCurrentDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        return dateFormat.format(new Date());
    }

    private class NodeDataListener extends Thread {
        public void run() {
            try {
                // Create HTTP server on port 8000
                HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);

                // Set handler for "/data" endpoint
                server.createContext("/data", new HttpHandler() {
                    public void handle(HttpExchange exchange) throws IOException {
                        // Execute Python script
                        String ipAddress = ipTextField.getText();
                        if (!ipAddress.isEmpty()) {
                            try {
                                ProcessBuilder pb = new ProcessBuilder("python", "AccessControllAccess.py", ipAddress);
                                pb.redirectErrorStream(true);
                                Process process = pb.start();
                                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                                StringBuilder output = new StringBuilder();
                                String line;
                                while ((line = reader.readLine()) != null) {
                                    output.append(line).append("\n");
                                }

                                int exitCode = process.waitFor();
                                if (exitCode == 0) {
                                    // Send response with data
                                    String response = output.toString();
                                    exchange.sendResponseHeaders(200, response.getBytes().length);
                                    OutputStream os = exchange.getResponseBody();
                                    os.write(response.getBytes());
                                    os.close();
                                } else {
                                    // Send error response
                                    exchange.sendResponseHeaders(500, 0);
                                    exchange.getResponseBody().close();
                                }
                            } catch (IOException | InterruptedException ex) {
                                ex.printStackTrace();
                                exchange.sendResponseHeaders(500, 0);
                                exchange.getResponseBody().close();
                            }
                        } else {
                            // Send error response
                            exchange.sendResponseHeaders(400, 0);
                            exchange.getResponseBody().close();
                        }
                    }
                });

                // Start the server
                server.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new AccessControlSync().setVisible(true);
        });
    }
}
