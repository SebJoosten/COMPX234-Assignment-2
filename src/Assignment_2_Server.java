import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// this is the basic hello world Implementation from class
public class Assignment_2_Server {

    private static final int timeOut = 10000;
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
        try{
            port = Integer.parseInt(args[0]);
            System.out.println("Port: " + port + " OK ");

        } catch (NumberFormatException e){
            System.out.println("Server port input invalid integer");
            System.out.println(e.toString());
        }

        // Initial connection block with time out
        // This will set up connections then find a free socked and move it to a new connection with that socket

        try {
            // Listen on "port" for incoming connections
            // Set up the server instance
            ServerSocket ss = new ServerSocket(port);
            // Set the time-out for the retry loop
            ss.setSoTimeout(timeOut);
            boolean running = true;

            // While communicating
            while (running) {

                try {
                    // Accept incoming client connection
                    Socket s = ss.accept();

                    // Set up input buffer called read > make a stream reader to convert byte to text
                    // set up input stream from s
                    BufferedReader read = new BufferedReader(
                            new InputStreamReader(s.getInputStream())
                    );

                    // Send output to client
                    PrintWriter write = new PrintWriter(
                            s.getOutputStream(), true // autoFlush = true
                    );

                    // ***** This is where the data is actually send and received *****
                    // This will only send data once and echo

                    // Set up input string
                    String in;

                    // While there is data coming in keep printing it
                    while ((in = read.readLine()) != null) {

                        // Check for close call and break
                        if (in.trim().equals("CLOSE")) {
                            System.out.println("Server closed");
                            running = false;
                            break;
                        }

                        if (in.trim().equals("NEWPORT")) {
                            System.out.println("Server: Move to new port");

                            // Get new free port
                            ServerSocket newServer = new ServerSocket(0);
                            int newPort = newServer.getLocalPort();

                            // Send port and wait for echo
                            write.println("NEWPORT," + newPort);
                            in = read.readLine();
                            String[] split = in.split(",");

                            if (split.length != 2) {
                                System.out.println("Server: Wrong port echo");
                                break;
                            }

                            if (split[0].trim().equals("NEWPORT")) {
                                if (Integer.parseInt(split[1]) == newPort) {
                                    System.out.println("Server: New port found");
                                    System.out.println("Server: Move to new port" + newPort);

                                    clientConnection c = new clientConnection(newPort, newServer);
                                    clientConnections.add(c);
                                    c.start();
                                }
                            }
                        }

                        // print input and echo
                        System.out.println(" server gets " + in);
                        write.println("You Said: " + in);
                    }

                    // find a new free port and send it to the client



                    // kill writer, reader and close connection
                    write.close();
                    read.close();
                    s.close();

                // Catch for time out currently just set to retry
                }catch (SocketTimeoutException e){
                    System.out.println("Server timeout");
                    System.out.println("Retry on port " + port  );
                }
            }

        // Catch for if the socket fails
        } catch (Exception e) {
            System.err.println(e.toString());
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
