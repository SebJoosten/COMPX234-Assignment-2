import java.io.File;

public class Runner {

    /**
     * This is the runner it will optionally start a server and 10 clients
     * @param args
     * @throws InterruptedException
     */
    public static void main(String[] args) throws InterruptedException {

        System.out.println("Runner is running!");

        // Set the port number for clients and server
        int port = 51234;
        // Set the number of times the 10 workloads are run
        int multiplier = 1;

        // Set the host name
        String hostname = "localhost";
        // Root scr
        String path = "client_";

        // Starts a server thread
        startServer();
        // Delay
        Thread.sleep(1000);

        // Start the clients one after the other
        for (int j = 0; j < multiplier; j++) {
            for (int i = 1; i < 11; i++) {

                // Generate the file path
                File file = new File(path + i + ".txt");

                // Check file exists otherwise skip the client
                if (!file.exists()) {
                    System.out.println("Missing file: " + file.getAbsolutePath());
                    continue;
                }

                // Construct the arguments for the client
                String[] clientArgs = {
                        hostname,
                        String.valueOf(port),
                        path + i + ".txt"
                };

                // Run clients All at the same time
                // clientThread(clientArgs);

                // Run clients in order from 0 to 10
                Assignment_2_Client.main(clientArgs);

                // delay if needed
                Thread.sleep(1); // 200ms
            }
        }
    }

    /**
     * This is to run mutable threads of the clients simultaneously
     * @param clientArgs - The array of string arguments passed to the client
     */
    private static void clientThread(String[] clientArgs) {

        // Make and start the client thread
        new Thread(() -> {
            for(String arg : clientArgs) {
                System.out.println(arg);
            }
            try {
                Assignment_2_Client.main(clientArgs);
            } catch (Exception e) {
                System.out.println("Client start error ");
                System.out.println(e.getMessage());
            }
        }).start();
    }

    /**
     * This just starts the server in its own thread
     * This was to make it easier to disable, and the server side often stays running
     */
    private static void startServer(){

        // Start the server in its own thread
        new Thread(() -> {
            try {
                Assignment_2_Server_1.main(new String[]{"51234"});
            } catch (Exception e) {
                System.out.println("Server start error ");
                System.out.println(e.getMessage());
            }
        }).start();
    }

} // Runner end
