import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class Assignment_2_Client {

    // Instruction record with generics for Command, Key, and Value for inorder list Storage
    record Instruction<C, K, V>(C command, K key, V value) {};

    // a variable to store the instruction list
    private static InstructionStorage outPuts;

    /**
     * Main class of client takes in 3 arguments
     * 1 - Host Name
     * 2 - Port number
     * 3 - File path to load instructions
     * @param args hostName portNumber "filePath"
     */
    public static void main(String[] args) {

        // Check argument length
        if (args.length != 3) {
            System.out.println("Usage: java Client <hostname> <port> <file_path>");
            return;
        }

        // Set up argument variables
        String hostname = args[0];
        String filePath = args[2];
        int port = 0;

        // Try to parse and check port is valid
        try{

            port = Integer.parseInt(args[1]);
            if (port < 50000 || port > 59999) {
                System.out.println("************ INVALID PORT ************");
                System.out.println("Port must be between 0 and 65535.");
                return;
            }

            System.out.println("Port: " + port + " OK ");

        } catch (NumberFormatException e){
            System.out.println("Client port input invalid integer");
            System.out.println(e.toString());
        }

        // Load the text file
        loadTXTFile(filePath);

        // Get IP for thread and port set up
        InetAddress ia;
        try {
            ia = InetAddress.getByName(hostname);
        } catch (UnknownHostException e) {
            System.err.println("Unknown name for IP");
            return;
        }


        try {

            // Send something g
            Socket sock = new Socket(ia, port);
            PrintWriter writer = new PrintWriter(sock.getOutputStream(), true);
            BufferedReader read = new BufferedReader(new InputStreamReader(sock.getInputStream()));

            // first contact with name
            writer.println("Client-connecting");
            String line = read.readLine();

            // Get the list of instructions convert and send them
            for (Instruction<String, String, String> i : outPuts.getOutputs()) {

                writer.println(convertInstruction(i));

                line = read.readLine();

                // Edit output string removing value if empty
                String output = i.command() + " " + i.key() + " " + i.value();
                while (output.endsWith(" ")) {
                    output = output.substring(0, output.length() - 1);
                }
                System.out.println(output + ": " + line);
            }

            // Close connection
            writer.println("CLOSE");
            sock.close();

        } catch (IOException e) {
            System.err.println("IO Exception: " + e);
        }

    }

    /**
     * This is to load the text file in to the instruction list
     * It checks if the inputs are valid and adds and instruction or ignores and skips a line
     * @param filePath The file path of the instruction set you wish to load
     *                 Normally the last of 3 arguments in to this class
     */
    private static void loadTXTFile(String filePath){

        // Make a new instruction list or reset the old one
        outPuts = new InstructionStorage();
        System.out.println("**************************** Loading TXT file: " + filePath + " ***************************");

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            br.readLine();
            int lineCount = 0;
            while ((line = br.readLine()) != null) {
                String[] input = line.split(" ",3);
                lineCount++;

                // Check if the input has exactly three parts
                if (input.length == 2 || input.length == 3) {
                    String instruction = input[0];
                    String key = input[1];
                    String value = (input.length == 2) ? " " : input[2];


                    // Make sure the instruction is valid
                    if (!(Objects.equals(instruction, "PUT") ||
                            Objects.equals(instruction, "READ")  ||
                            Objects.equals(instruction, "GET") )) {
                        System.out.println("******** INVALID INSTRUCTION *********");
                        System.out.println("Line: " + lineCount + " Instruction " + instruction + " Must only be GET/READ/PUT");
                        continue;
                    }

                    // Make sure value is correct
                    if (value.length() > 999 ){
                        System.out.println("*********** INVALID VALUE ************");
                        System.out.println("Line: " + lineCount + " Value longer than 999 characters");
                        continue;
                    }

                    // Add it to the output list and print a Line for debugging
                    outPuts.add(instruction, key, value);
                    System.out.println("--> Loaded DATA -> Line: " + lineCount +
                            " Instruction: " + instruction +
                            " key: " + key +
                            ((Objects.equals(value, " ")) ? " " : " value: " + value));

                    int last = outPuts.getOutputs().size() - 1;
                    System.out.println(convertInstruction(outPuts.getOutputs().get(last)));

                } else {
                    // If anything else fails jump out and move to the next line
                    System.out.println("******** INVALID INPUT FORMAT ********");
                    System.out.println("Line: " + lineCount + " IGNORED");
                    continue;
                }

            }

            // Catch for file errors
        } catch (IOException err) {
            System.out.println("Error reading file");
            System.out.println(err.toString());
        }

        System.out.println("****************************  Loaded TXT file: " + filePath + " *************************** ");

    }

    //************************* Instruction storage/Retrieval and storage *************************

    /**
     * an object class for list input
     * This is so I can check the file load it and forget it knowing it's all valid
     */
    private static class InstructionStorage {
        // Store a list of Instruction objects
        private List<Instruction<String, String, String>> outputs = new ArrayList<>();

        // Getter
        private List<Instruction<String, String, String>> getOutputs() {
            return outputs;
        }

        // Add an instruction
        private void add(String command, String key, String value) {
            outputs.add(new Instruction<>(command, key, value));
        }


        // Print the list to confirm it's all there or check order
        private void printList(){
            for (Instruction<String, String, String> i : outputs) {
                System.out.println(i.toString());
            }
        }

    }

    /**
     * This is for conversion to the protocol "000 P key value"
     * @param i The instruction you want to translate in to the protocol
     * @return The instruction translated in to a string in the correct format
     */
    private static String convertInstruction(Instruction i){
        String instruction = (String) i.command();
        String key = (String) i.key();
        String value = (String) i.value();

        // Change put get and read to P G and R
        instruction = (Objects.equals(instruction, "PUT")) ? "P" :
                        (Objects.equals(instruction, "GET")) ? "G" :
                         (Objects.equals(instruction, "READ")) ? "R" : "ERROR";

        // Check for error
        if (Objects.equals(instruction, "ERROR")) {
            System.out.println("CONVERSION ERROR");
            System.out.println("Instruction -> " + i.toString());
            return "ERROR";
        }

        // Generate string
        String output = " " + instruction + " " + key + " " + value;

        // Trim the spaces off the end if value is empty - value is empty
        while (output.endsWith(" ")) {
            output = output.substring(0, output.length() - 1);
        }

        return String.format("%03d" , output.length() + 3) + output ;

    }

}
