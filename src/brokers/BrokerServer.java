/*
 * Name: Zhou Ze
 * Student ID: 1536407
 * Course: COMP90015
 * Assignment: Distributed Dictionary Application
 */
package brokers;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;

public class BrokerServer {
    public static void main(String[] args) {
        try {
            if (args.length < 1) {
                System.out.println("Please specify the port number for this broker.");
                return;
            }

            int port;
            try {
                port = Integer.parseInt(args[0]); // Parse port number
            } catch (NumberFormatException e) {
                System.out.println("Invalid port number format. Please provide a valid integer.");
                return;
            }

            // Flag to check if -b is present
            boolean hasOtherBrokers = false;

            // Simulated hardcoded broker addresses, matching the ports
            List<String> otherBrokers = new ArrayList<>();

            // Check for the '-b' flag
            for (int i = 1; i < args.length; i++) {
                if (args[i].equals("-b")) {
                    hasOtherBrokers = true;
                }
            }

            // Hardcoded logic: manually assign the other broker addresses based on the port number
            if (port == 5001) {
                otherBrokers.add("localhost:5002");
                otherBrokers.add("localhost:5003");
            } else if (port == 5002) {
                otherBrokers.add("localhost:5001");
                otherBrokers.add("localhost:5003");
            } else if (port == 5003) {
                otherBrokers.add("localhost:5001");
                otherBrokers.add("localhost:5002");
            }

            // Create and export the broker object, passing the port and other brokers
            Broker broker = new Broker(port, otherBrokers);

            // Start the RMI registry on the specified port
            Registry registry = LocateRegistry.createRegistry(port);

            // Bind the broker object to the registry
            registry.rebind("BrokerService", broker);

            System.out.println("Broker is running on port " + port);

            // Print connection details if there are other brokers
            if (hasOtherBrokers) {
                System.out.println("Connected to other brokers");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
