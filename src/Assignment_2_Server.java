import java.io.*;
import java.net.*;

// this is the basic hello world Implementation from class
public class Assignment_2_Server {

    private static final int timeOut = 10000;

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

                        // print input and echo
                        System.out.println(" server gets " + in);
                        write.println("You Said: " + in);
                    }

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
}
