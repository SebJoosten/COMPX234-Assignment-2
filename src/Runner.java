import java.io.File;

public class Runner {

    /**
     * This is the runner it will optionally start a server and 10 clients
     * @param args
     * @throws InterruptedException
     */
    public static void main(String[] args) throws InterruptedException {

        System.out.println("Runner is running!");

        // Set the port number you want the client and server to use
        int port = 51234;

        // set the host name
        String hostname = "localhost";

        // The path of the file
        //String path = "E:\\OneDrive\\Uni\\Year 2\\COMPX234\\School projects\\Assignment-2\\Assignment-2-tcp server\\src\\client_";

        // root scr
        String path = "client_";

        // Starts a server thread
        //  startServer();
        Thread.sleep(1000);

        // Start the clients one after the other
        for (int j = 0; j < 100; j++) {
            for (int i = 1; i < 11; i++) {


                File file = new File(path + i + ".txt");
                if (!file.exists()) {
                    System.out.println("Missing file: " + file.getAbsolutePath());
                    continue; // skip this client
                }

                // Construct the arguments for the client
                String[] clientArgs = {
                        hostname,
                        String.valueOf(port),
                        path + i + ".txt"
                };

                clientThread(clientArgs);

                // Run the client with the constructed arguments
                //Assignment_2_Client.main(clientArgs);

                // delay if needed
                Thread.sleep(1); // 200ms
            }
        }
    }


    private static void clientThread(String[] clientArgs) {

        System.out.println("**********************************************************" + clientArgs[2]);

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
                System.out.println(e.getMessage());
            }
        }).start();
    }

}
