import java.net.*;
import java.io.*;


public class Assignment_2_Client {


    // Starting demo from class
    public static void main(String[] args) {

        // Get the initialization port This way it can be changed easily
        // int port = 51234;
        // Int port = Integer.parseInt(args[0]);
        int port = 0;

        // Check valid port input this is to make sure It's something usable NOT connectable
        if (args.length > 1) {
            System.out.println("Usage: java Assignment_2_Client <port>");
            System.out.println("************ INVALID INPUT ************");
            return;
        }
        // Try and convert the input argument to int
        try{
            port = Integer.parseInt(args[0]);
            System.out.println("Port: " + port + " OK ");

        } catch (NumberFormatException e){
            System.out.println("Client port input invalid integer");
            System.out.println(e.toString());
        }




        InetAddress ia;

        // Get IP of local host IE this machine
        try {
            ia = InetAddress.getByName("localhost");
        } catch (UnknownHostException e) {
            System.err.println("Unknown name for IP");
            return;
        }


        try {

            // Send something g
            Socket sock = new Socket(ia, port);
            PrintWriter writer = new PrintWriter(sock.getOutputStream(), true);
            writer.println(args[0]);

            // Receive something
            BufferedReader read = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            String line = read.readLine();
            System.out.println("server sent: \"" + line + "\"");

            // Close connection
            sock.close();


        } catch (IOException e) {
            System.err.println("IO Exception: " + e);
        }


    }
}
