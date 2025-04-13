import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

// this is the basic hello world Implementation from class
public class Assignment_2_Server {

    // The timeout interval for connection failure
    private static final int timeOut = 2000;
    // The number of retries before a thread closes the connection
    private static final int retries = 10;
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
            System.out.println("Main Server -> Port: " + port + " OK ");

        } catch (NumberFormatException e) {
            System.out.println("Main Server -> port input invalid integer");
            System.out.println(e.toString());
        }

        // Initial connection block with time out
        boolean listening = true;
        while (listening) {

            try {

                // Set up the server instance and listen on port
                ServerSocket ss = new ServerSocket(port);
                ss.setSoTimeout(timeOut);

                // While communicating
                while (true) {
                    System.out.println("Main Server -> Listening on port " + port);
                    try {

                        // Listen for connection
                        Socket s = ss.accept();
                        System.out.println("Main Server -> Client connected: " + s.getInetAddress());

                        // Pass socket to a thread and start
                        clientConnection handler = new clientConnection(s.getInetAddress().toString(), s);
                        new Thread(handler).start();

                    // Catch for time out currently just set to retry
                    } catch (SocketTimeoutException e) {
                        System.out.println("Main Server -> Socket timeout: " + port);
                    // Catch socket IO errors
                    } catch (IOException e) {
                        System.out.println("Main Server -> IO exception: "+ e.getMessage());
                    }

                }

            // Catch for ALL OTHER FAILURES
            } catch (IOException e) {
                System.err.println("Main Server -> general socket failure" + e.getMessage() + " Retry on port: " + port);
            }

        } // Server listen loop

    }


    /**
     * New thread for client connection
     * This thread is passed the socked of the communication then terminates when the socket is closed or faults
     */
    public static class clientConnection extends Thread {
        private String id;
        private Socket clientConnect;

        /**
         * Pass an established socket connection to this thread to keep communicating
         * @param id The id for this thread
         * @param clientConnect The socket you've established a connection on
         */
        public clientConnection(String id, Socket clientConnect) {
            this.id = "Sub Connection " + id + " -> ";
            this.clientConnect = clientConnect;
        }

        // The thread itself
        @Override
        public void run() {
            System.out.println(id + "running");

            // While communicating "running" when communication stops or faults it just drops it
            boolean running = true;
            int threadRetries = 0;

            try {   // IO catch

                // Set up read and write buffer and timeout
                clientConnect.setSoTimeout(timeOut);
                BufferedReader read = new BufferedReader(
                        new InputStreamReader(clientConnect.getInputStream())
                );
                PrintWriter write = new PrintWriter(
                        clientConnect.getOutputStream(), true
                );

                // While running and retry counter is still under allotted amount
                while (running && threadRetries < retries) {

                    try {   // Timeout catch

                        // Make a string used for reading in and checking data
                        String in;
                        while ((in = read.readLine()) != null) {

                            // Check for close call and break
                            if (in.trim().equals("CLOSE")) {
                                System.out.println(id + "Closing connection");
                                running = false;
                                break;
                            }

                            // print input and echo
                            System.out.println(id + "gets " + in);
                            write.println(id + "Said: " + in);

                            // Communication successful reset retry count
                            threadRetries = 0;
                        }

                        // Catch for time out currently just set to retry
                    } catch (SocketTimeoutException e) {
                        System.out.println(id + "Timeout retry: " + threadRetries);
                        threadRetries++;
                    }

                }

                // kill writer, reader and close connection
                write.close();
                read.close();
                clientConnect.close();
                System.out.println(id + "CLOSED");

            }
            // Catch socket IO errors
            catch (IOException e) {
                System.out.println(id + "IO exception: " + e.toString());
                running = false;
            }

        }

    } // **** Thread end ****
}
