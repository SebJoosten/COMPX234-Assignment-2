import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

// this is the basic hello world Implementation from class
public class Assignment_2_Server_1 {

    // The timeout interval for connection failure
    private static final int timeOut = 10000;
    // The number of retries before a thread closes the connection
    private static final int retries = 10;
    // A list of connected threads
    private static List<Thread> clientConnections = new ArrayList<>();
    // Hash map for pairs of strings
    private static HashMap<String, String> tupleSpace = new HashMap<>();
    // Semaphore for map edit
    private static Semaphore tupleLock = new Semaphore(1);


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
                        clientConnection handler = new clientConnection(s.getInetAddress(), s);
                        new Thread(handler).start();

                    // Catch for time out currently just set to retry
                    } catch (SocketTimeoutException e) {
                        printTupleSpace();
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

        }

    }

    /**
     * THis just prints out the hash map containing the tuple space
     */
    private static void printTupleSpace(){

        System.out.println("************************* CURRENT Tuple Space *************************");
        for (Map.Entry<String, String> entry : tupleSpace.entrySet()) {
            System.out.println("Key: " + entry.getKey() + ", Value: " + entry.getValue());
        }
        System.out.println("************************* CURRENT Tuple Space *************************");
        System.out.println("*************************  <-     END     ->  *************************");

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
        public clientConnection(InetAddress id, Socket clientConnect) {
            this.id = "Sub Connection " + id.toString() + " -> ";
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


                            String[] inPut = in.split(" ", 4);
                            int checkSum = 0;

                            // Check sum and check string length this also checks initial communication
                            try{
                                checkSum = Integer.parseInt(inPut[0]);
                                if (in.length() != checkSum) {
                                    throw new Exception("ERR Invalid checkSum: " + inPut[0] + " != " + checkSum);
                                }
                                if (checkSum > 999) {
                                    throw new Exception("ERR String too long: " + inPut[0] + " != " + checkSum);
                                }
                            }
                            catch(Exception e) {
                                System.out.println(id + "checkSum: " + inPut[0]);
                                if (Objects.equals(inPut[0], "Client-connecting")) {
                                    write.println("Connection established");
                                } else {
                                    write.println(e.toString());
                                }
                                continue;
                            }

                            // Set a default return string
                            String returnString = "ERR";

                            // CHeck for PUT READ and GET instructions then call them
                            if (Objects.equals("P" , inPut[1])) {
                                if(inPut[3] != null){
                                    returnString = tupleSpacePUT(inPut[2],inPut[3]);
                                }
                            }
                            if (Objects.equals("R" , inPut[1])) {
                                returnString = tupleSpaceREAD(inPut[2]);
                            }
                            if (Objects.equals("G" , inPut[1])) {
                                returnString = tupleSpaceGET(inPut[2]);
                            }

                            returnString = returnString.trim();
                            // Add the number to the front
                            returnString = String.format("%03d" , returnString.length() + 4) + " " + returnString;
                            write.println(returnString);

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

    /**
     * This method is to put things in the tuple space if k already exists returns error string
     * @param k key of the value your wanting to add to the tuple space
     * @param v the value that goes with that key
     * @return the formatted return string with the result of the function
     */
    private static String tupleSpacePUT(String k , String v ){
        String out = "ERR " + k + " EXISTS";

        // Try and acquire a lock The check if k exists.
        try {
            // if k docent exist ass k, v
            if (tupleLock.tryAcquire(1, 20, TimeUnit.SECONDS)) {
                if (!tupleSpace.containsKey(k)) {
                    tupleSpace.put(k, v);
                    out = "OK (" + tupleSpace.get(k) + " , " + k + ")" + " added";
                }
                tupleLock.release();
            }
        } catch (Exception e) {
            System.out.println("Server tuple TIMEOUT " + e.toString());
            out = "ERR tupleSpace TIMEOUT " + k ;
        }

        return out;

    }

    /**
     * This is to perform the GET operation on the tuple space
     * @param k The key your looking to retrieve information about
     * @return a formatted output string ready to send
     */
    private static String tupleSpaceGET(String k){

        String out = "ERR " + k + " dose not exist";

        // Try and acquire a lock The check if k exists. if exists return value
        try {
            if (tupleLock.tryAcquire(1, 20, TimeUnit.SECONDS)) {
                if (tupleSpace.containsKey(k)) {
                    out = "OK (" + tupleSpace.get(k) + " , " + k + ")" + " removed";
                    tupleSpace.remove(k);
                }
                tupleLock.release();
            }
        } catch (Exception e) {
            System.out.println("Server tuple TIMEOUT " + e.toString());
            out = "ERR tupleSpace TIMEOUT " + k ;
        }

        return out;
    }

    /**
     * This is to make a read call on the tuple space
     * @param k k is the key your wanting a value for
     * @return it will return a formatted output string with the result
     */
    private static String tupleSpaceREAD(String k) {

        // Set default string return
        String out = "ERR " + k + " dose not exist";

        // Try and acquire a lock The check if k exists. if exists return value
        try {
            if (tupleLock.tryAcquire(1, 20, TimeUnit.SECONDS)) {
                if (tupleSpace.containsKey(k)) {
                    out = "OK (" + tupleSpace.get(k) + " , " + k + ")" + " read";
                }
                tupleLock.release();
            }
        } catch (Exception e) {
            System.out.println("Server tuple TIMEOUT " + e.toString());
            out = "ERR tupleSpace TIMEOUT " + k ;
        }

        return out;
    }

}
