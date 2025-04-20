public class Runner {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("Runner is running!");


        int port = 51234;
        String hostname = "localhost";
        // The path of the file
        String path = "E:\\OneDrive\\Uni\\Year 2\\COMPX234\\School projects\\Assignment-2\\test-workload\\test-workload\\client_";
        //String path = "\\client_instructions\\client_";

        //startServer();

        // Start the clients one after the other
        for (int i = 10; i > 0; i--) {
            String[] clientArgs = {
                    hostname,
                    String.valueOf(port),
                    path + i + ".txt"
            };
            Assignment_2_Client.main(clientArgs);

            // delay if needed
            Thread.sleep(1); // 200ms
        }
    }

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
