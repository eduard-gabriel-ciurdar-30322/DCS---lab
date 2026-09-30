package lab.dcs.net.calculator;

import java.io.*;
import java.net.*;

public class CalculatorServer {

    public static final int PORT = 1234;

    public static void main(String[] args) {
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("Calculator Server started on port " + PORT);
            System.out.println("Waiting for client connections...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: "
                        + clientSocket.getRemoteSocketAddress());
                new ClientHandler(clientSocket).start();
            }

        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

class ClientHandler extends Thread {

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public ClientHandler(Socket socket) throws IOException {
        this.socket = socket;
        this.in = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(
                new BufferedWriter(
                        new OutputStreamWriter(socket.getOutputStream())), true);
    }

    @Override
    public void run() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                System.out.println("Received: " + line);

                if (line.equalsIgnoreCase("END")) {
                    out.println("BYE");
                    out.flush();
                    break;
                }

                String result = processRequest(line);
                out.println(result);
                out.flush();
            }
        } catch (IOException e) {
            System.err.println("Client disconnected: " + e.getMessage());
        } finally {
            try {
                socket.close();
                System.out.println("Client connection closed.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String processRequest(String request) {
        try {
            String[] parts = request.split(";");
            if (parts.length != 3) {
                return "ERROR: Invalid format. Expected: number1;operation;number2";
            }

            double num1 = Double.parseDouble(parts[0].trim());
            String operation = parts[1].trim();
            double num2 = Double.parseDouble(parts[2].trim());

            double result;
            switch (operation) {
                case "+":
                    result = num1 + num2;
                    break;
                case "-":
                    result = num1 - num2;
                    break;
                case "*":
                    result = num1 * num2;
                    break;
                case "/":
                    if (num2 == 0) {
                        return "ERROR: Division by zero";
                    }
                    result = num1 / num2;
                    break;
                default:
                    return "ERROR: Unknown operation '" + operation
                            + "'. Supported: +, -, *, /";
            }

            // Format nicely: show integer if result is whole, otherwise decimal
            if (result == Math.floor(result) && !Double.isInfinite(result)) {
                return "RESULT: " + (long) result;
            } else {
                return "RESULT: " + result;
            }

        } catch (NumberFormatException e) {
            return "ERROR: Invalid number format";
        }
    }
}
