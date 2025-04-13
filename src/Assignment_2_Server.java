import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// this is the basic hello world Implementation from class
public class Assignment_2_Server {

    // The timeout interval for connection failure
    private static final int timeOut = 2000;
    // A list of connected threads
    private static List<Thread> clientConnections = new ArrayList<>();


    public static void main(String[] args) {

        // Get the initialization port This way it can be changed easily
        // int port = 51234;
        // Int port = Integer.parseInt(args[0]);
        int port = 0;

        // Check valid port input this is to make sure It's something usable NOT connectable
        if (args.length > 1) {
            System.out.println("Usage: java Assignment_2_Server <port>");
            System.out.println("************ INVALID INPUT ************");
            return;
        }
        // Try and convert the input argument to int
        try {
            port = Integer.parseInt(args[0]);
            System.out.println("Port: " + port + " OK ");

        } catch (NumberFormatException e) {
            System.out.println("Server port input invalid integer");
            System.out.println(e.toString());
        }

        // Initial connection block with time out
        while (true) {
            try {
                // Set up the server instance and listen on port
                ServerSocket ss = new ServerSocket(port);
                ss.setSoTimeout(timeOut);
                boolean running = true;

                // While communicating
                while (running) {

                    try {
                        // Accept incoming client connection
                        Socket s = ss.accept();

                        // Set up read and write buffer
                        BufferedReader read = new BufferedReader(
                                new InputStreamReader(s.getInputStream())
                        );
                        PrintWriter write = new PrintWriter(
                                s.getOutputStream(), true
                        );

                        // Make a string used for reading in and checking data
                        String in;
                        while ((in = read.readLine()) != null) {

                            // Check for close call and break
                            if (in.trim().equals("CLOSE")) {
                                System.out.println("Server closed");
                                running = false;
                                break;
                            }

                            // if client is ready call for new port allocation
                            if (in.trim().equals("NEWPORT")) {

                                // Get new available port
                                System.out.println("Server: Move to new port");
                                ServerSocket newServer = new ServerSocket(0);
                                int newPort = newServer.getLocalPort();

                                // Send port and wait for echo
                                write.println("NEWPORT," + newPort);
                                in = read.readLine();
                                String[] split = in.split(",");

                                // Check echo and assign thread
                                if (split.length == 2 &&
                                        (split[0].trim().equals("NEWPORT")) &&
                                        Integer.parseInt(split[1]) == newPort) {
                                    System.out.println("Server: New port found");
                                    System.out.println("Server: Move to new port" + newPort);
                                    clientConnection c = new clientConnection(newPort, newServer);
                                    clientConnections.add(c);
                                    c.start();

                                } else {
                                    System.out.println("Server: New port not found");
                                    break;

                                }

                            }

                            // print input and echo
                            System.out.println(" server gets " + in);
                            write.println("You Said: " + in);

                        }

                        // kill writer, reader and close connection
                        write.close();
                        read.close();
                        s.close();

                        // Catch for time out currently just set to retry
                    } catch (SocketTimeoutException e) {
                        System.out.println("Server timeout");
                        System.out.println("Retry on port " + port);
                    } catch (IOException e) {
                        // Catches but then waits for timeout
                        System.out.println("Server exception");
                    }

                }

                // Catch for server set up failure
            } catch (IOException e) {
                System.err.println("Server: Failed to connect " + e.getMessage());
                System.out.println("Retrying server setup...");
            }
        }
    }




    // New receive thread
    public static class clientConnection extends Thread {
        private int port;
        private String id;
        private ServerSocket clientConnect;

        // Set up new thread with thread connection
        public clientConnection(int number, ServerSocket clientConnect) {
            this.port = number;
            this.id = "Server thread " + Integer.toString(number);
            this.clientConnect = clientConnect;
        }

        @Override
        public void run() {
            System.out.println(id + " starting");

            try {
                // Accept connection on new port
                Socket newSocket = clientConnect.accept();
                System.out.println("Server: Client connected on new port");

                // Set up new read and write
                BufferedReader read = new BufferedReader(
                        new InputStreamReader(newSocket.getInputStream())
                );

                PrintWriter write = new PrintWriter(
                        newSocket.getOutputStream(), true
                );

                // Same loop as before but on new thread
                String in;

                // Read and respond with echo
                while ((in = read.readLine()) != null) {
                    if (in.trim().equals("CLOSE")) {
                        System.out.println("Server closed");
                        break;
                    }

                    System.out.println(id + " received: " + in);
                    write.println("Echo: " + in);
                }

                // Close sockets
                newSocket.close();
                clientConnect.close();
                System.out.println(id + " finished");

            } catch (Exception e) {
                System.out.println(id + " error: " + e);
            }
        }


    }

}
