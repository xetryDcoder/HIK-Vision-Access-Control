import java.io.*;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

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

        // Start the scheduled task to run every 10 seconds
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(this::runScheduledTask, 0, 10, TimeUnit.SECONDS);
    }

    private class SynchronizeButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            synchronize();
        }
    }

    private void synchronize() {
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
                    sendDataToNodeJS(output.toString());
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

    private void sendDataToNodeJS(String data) {
        // Escape special characters in the data
        String escapedData = data.replace("\\", "\\\\")
                                 .replace("\"", "\\\"")
                                 .replace("\n", "\\n")
                                 .replace("\r", "\\r");

        String payload = "{ \"data\": \"" + escapedData + "\" }";

        System.out.println("Payload: " + payload); // Log the payload for debugging

        try {
            HttpClient httpClient = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("http://localhost:3000/receive-data"))
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .header("Content-Type", "application/json")
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                System.out.println("Data sent successfully to Node.js API");
            } else {
                System.err.println("Failed to send data to Node.js API. Response code: " + response.statusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void runScheduledTask() {
        synchronize();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new AccessControlSync().setVisible(true);
        });
    }
}
