import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This is the main server class it takes in 1 argument the port number
 * This manages a tupleSpace of strings
 */
public class Assignment_2_Server_1 {

    // The timeout interval for connection failure
    private static final int timeOut = 10000;
    // The number of retries before a thread closes the connection
    private static final int retries = 10;
    // A list of connected threads
    private static List<Thread> clientConnections = new ArrayList<>();
    // Hash map for pairs of strings
    private static HashMap<String, String> tupleSpace = new HashMap<>();
    // Semaphore for tupleSpace edit
    private static Semaphore tupleLock = new Semaphore(1);
    // Debugging flag for printing protocol
    private static boolean printProticol = true;

    // A collection of general stats for the server that must only be accessed inside tupleSpace lock
    private static int operations = 0;
    private static int reads = 0;
    private static int gets = 0;
    private static int puts = 0;
    // Other general stats that can be incremented and decremented whenever necessary
    private static AtomicInteger errors = new AtomicInteger(0);
    private static AtomicInteger clients = new AtomicInteger(0);


    /**
     * The main thread for the server takes in 1 argument the port number it's to listen on
     * It will continue indefinitely until the program is terminated or something goes wrong
     * @param args The port number you want to listen to
     */
    public static void main(String[] args) {

        // Get the initialization port This way it can be changed easily
        // int port = 51234;
        int port = 0;

        // Check valid port input this is to make sure It's usable NOT connectable
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
            errors.incrementAndGet();
        }

        // Initial connection block with time out just in case a connection fails
        while (true) {

            try {

                // Set up the server instance and listen on port
                ServerSocket ss = new ServerSocket(port);
                ss.setSoTimeout(timeOut);

                // While communicating
                while (true) {
                    System.out.println("Main Server -> Listening on port " + port);
                    try {

                        // Listen for connection with set timeout
                        Socket s = ss.accept();
                        System.out.println("Main Server -> Client connected: " + s.getInetAddress());

                        // Pass the socket to a thread and start it
                        clientConnection handler = new clientConnection(s.getInetAddress(), s);
                        new Thread(handler).start();

                    // Catch for time out currently set to retry
                    } catch (SocketTimeoutException e) {
                        System.out.println("Main Server -> Socket timeout: " + port);
                        errors.incrementAndGet();
                    // Catch socket IO errors
                    } catch (IOException e) {
                        System.out.println("Main Server -> IO exception: "+ e.getMessage());
                        errors.incrementAndGet();
                    }

                }

            // Catch for ALL OTHER FAILURES
            } catch (IOException e) {
                System.err.println("Main Server -> general socket failure" + e.getMessage() + " Retry on port: " + port);
                errors.incrementAndGet();
            }

        }

    }

    /**
     * THis just prints out the hash map containing the tuple space
     * It does not print any of the stats with it, just the space itself
     */
    private static void printTupleSpace(){

        System.out.println("************************* CURRENT Tuple Space *************************");

        // Try and acquire to make sure the tuple space is not being edited at the time
        try {

            if (tupleLock.tryAcquire(1, 20, TimeUnit.SECONDS)) {
                // Print out all the tuples in tuple space
                int count = 1;
                for (Map.Entry<String, String> entry : tupleSpace.entrySet()) {
                    System.out.println(count + ": Key: " + entry.getKey() + ", Value: " + entry.getValue());
                    count++;
                }
                tupleLock.release();
            }

        } catch (Exception e) {
            System.out.println("Server tuple TIMEOUT " + e.toString());
            errors.incrementAndGet();
        }

        System.out.println("************************* CURRENT Tuple Space *************************");
        System.out.println("*************************  <-     END     ->  *************************");

    }

    /**
     * A method to print out the stats of the tuple space
     * It grabs current values all with in semaphore to make sure tuple space is not being edited at the time
     */
    private void printTupleSpaceStats(){

        // Try and acquire to make sure the tuple space is not being edited at the time
        try {

            if (tupleLock.tryAcquire(1, 20, TimeUnit.SECONDS)) {

                // Generate the average sizes,
                double averageKeySize = 0;
                double averageValueSize = 0;

                for (Map.Entry<String, String> entry : tupleSpace.entrySet()) {
                    averageKeySize += entry.getKey().length();
                    averageValueSize += entry.getValue().length();
                }

                // calculate averages
                averageKeySize /= tupleSpace.size();
                averageValueSize /= tupleSpace.size();
                double averageTupleSize = averageKeySize + averageValueSize;

                // Print out stats
                System.out.println("--- Tuple Space Stats ---");
                System.out.println("Tuples: " + tupleSpace.size());
                System.out.println("Avg Tuple Size: " + averageTupleSize);
                System.out.println("Avg Key Size: " + averageKeySize);
                System.out.println("Avg Value Size: " + averageValueSize);
                System.out.println("Operations: " + operations);
                System.out.println("READs: " + reads);
                System.out.println("GETs: " + gets);
                System.out.println("PUTs: " + puts );
                System.out.println("Errors: " + errors.get());

                // release lock
                tupleLock.release();
            }
        } catch (Exception e) {
            System.out.println("Server tuple TIMEOUT " + e.toString());
            errors.incrementAndGet();
        }

    }

    /**
     * New thread for client connection
     * This thread is passed the socked of the communication then terminates when the socket is closed or faults
     */
    public static class clientConnection extends Thread {
        private String id;
        private Socket clientConnect;

        /**
         * Pass an established socket connection to this thread to keep communicating until closed
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
            clients.incrementAndGet();
            System.out.println(id + "running");

            // While communicating "running" when communication stops or faults, it just drops it
            boolean running = true;
            int threadRetries = 0;

            try {

                // Set up read and write buffer and timeout
                clientConnect.setSoTimeout(timeOut);
                BufferedReader read = new BufferedReader(
                        new InputStreamReader(clientConnect.getInputStream())
                );
                PrintWriter write = new PrintWriter(
                        clientConnect.getOutputStream(), true
                );

                // While running and retry counter is still under the allotted amount
                while (running && threadRetries < retries) {

                    try {

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

                            // Check the sum and check string length
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
                                System.out.println(id + "checkSum: " + inPut[0] + " ERR: " + e.toString());
                                    write.println("007 ERR");
                                    errors.incrementAndGet();
                                continue;
                            }

                            // Set a default return string
                            String returnString = "ERR";

                            // Check for PUT READ and GET instructions, then pass to the appropriate function
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

                            // For mat the string for output
                            returnString = returnString.trim();
                            returnString = String.format("%03d" , returnString.length() + 4) + " " + returnString;
                            write.println(returnString);

                            // Print the line if debugging is enabled
                            if (printProticol) {
                                System.out.println("SENT --> " + returnString);
                            }

                            // Communication successfully reset retry count
                            threadRetries = 0;
                        }

                    // Catch for time out currently set to retry
                    } catch (SocketTimeoutException e) {
                        System.out.println(id + "Timeout retry: " + threadRetries);
                        threadRetries++;
                        errors.incrementAndGet();
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
                errors.incrementAndGet();
            }finally {
                clients.decrementAndGet();
            }

        }

    } // **** Thread end ****

    /**
     * This method is to put a tuple into the tuple space
     * @param k key of the value your wanting to add to the tuple space
     * @param v the value that goes with that key
     * @return the formatted return string with the result of the function ERR if duplicate
     */
    private static String tupleSpacePUT(String k , String v ){
        String out = "ERR " + k + " already exists";

        // Try and acquire a lock The check if k exists.
        try {

            if (tupleLock.tryAcquire(1, 20, TimeUnit.SECONDS)) {

                operations++;
                if (!tupleSpace.containsKey(k)) {
                    tupleSpace.put(k, v);
                    out = "OK (" + k + ", " + tupleSpace.get(k) + ")" + " added";
                    puts++;

                }else{
                    errors.incrementAndGet();
                }
                tupleLock.release();
            }

        } catch (Exception e) {
            System.out.println("Server tuple TIMEOUT " + e.toString());
            out = "ERR tupleSpace TIMEOUT " + k ;
            errors.incrementAndGet();
        }

        return out;

    }

    /**
     * This is to perform the GET operation on the tuple space
     * @param k The key you're looking to retrieve information about and then remove from the tuple space
     * @return a formatted output string ready to send
     */
    private static String tupleSpaceGET(String k){

        // Set default string
        String out = "ERR " + k + " does not exist";

        // Try and acquire a lock The check if k exists. if exists return value
        try {

            if (tupleLock.tryAcquire(1, 20, TimeUnit.SECONDS)) {
                operations++;
                if (tupleSpace.containsKey(k)) {
                    out = "OK (" + k + ", " + tupleSpace.get(k) + ")" + " removed";
                    tupleSpace.remove(k);
                    gets++;

                }else{
                    errors.incrementAndGet();
                }
                tupleLock.release();
            }

        } catch (Exception e) {
            System.out.println("Server tuple TIMEOUT " + e.toString());
            out = "ERR tupleSpace TIMEOUT " + k ;
            errors.incrementAndGet();
        }

        return out;
    }

    /**
     * This is to make a read call on the tuple space
     * @param k k is the key you're wanting a value for
     * @return it will return a formatted output string with the result
     */
    private static String tupleSpaceREAD(String k) {

        // Set default string return
        String out = "ERR " + k + " does not exist";

        // Try and acquire a lock The check if k exists. if exists return value
        try {

            if (tupleLock.tryAcquire(1, 20, TimeUnit.SECONDS)) {
                operations++;
                if (tupleSpace.containsKey(k)) {
                    out = "OK (" + k + ", " + tupleSpace.get(k) + ")" + " read";
                    reads++;

                }else{
                    errors.incrementAndGet();
                }
                tupleLock.release();
            }

        } catch (Exception e) {
            System.out.println("Server tuple TIMEOUT " + e.toString());
            out = "ERR tupleSpace TIMEOUT " + k ;
            errors.incrementAndGet();
        }

        return out;
    }



}
