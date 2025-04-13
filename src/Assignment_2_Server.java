import java.io.*;
import java.net.*;

// this is the basic hello world Implementation from class
public class Assignment_2_Server {
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

        // *********** HELLO WORLD TEST FROM CLASS ***********

        try {
            // Listen on "port" for incoming connections
            // Set up the server instance
            ServerSocket ss = new ServerSocket(port);

            // While communicating
            while (true) {
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

                // Read in a line
                String in = read.readLine();

                // write out a line
                write.println("You Said: " + in);

                // Close client connection
                s.close();

            }
        } catch (Exception e) {
            System.err.println(e);
        }

        // *********** END HELLO WORLD TEST ***********



    }
}
