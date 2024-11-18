/*
 * Name: Zhou Ze
 * Student ID: 1536407
 * Course: COMP90015
 * Assignment: Distributed Publisher-Subscriber System
 */
package publishers;

import brokers.BrokerInterface;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.List;

public class PublisherClient extends UnicastRemoteObject implements PublisherInterface {

    private String publisherId;

    // Constructor
    public PublisherClient(String publisherId) throws RemoteException {
        super();
        this.publisherId = publisherId;
    }

    // Implementing isAlive() method from PublisherInterface
    @Override
    public boolean isAlive() throws RemoteException {
        return true;  // Always return true as long as this publisher is running
    }

    @Override
    public String getPublisherId() throws RemoteException {
        return this.publisherId;
    }

    public static void main(String[] args) {
        try {
            // Ensure that username, broker IP, and port are provided via command-line arguments
            if (args.length < 3) {
                System.out.println("Usage: java -jar publisher.jar <username> <broker_ip> <broker_port>");
                return;
            }

            String username = args[0];  // Get the username from command-line arguments
            String brokerIp = args[1];
            int brokerPort = Integer.parseInt(args[2]);

            PublisherClient publisher = new PublisherClient(username);  // Use username as unique ID
            Registry registry = LocateRegistry.getRegistry(brokerIp, brokerPort);
            BrokerInterface broker = (BrokerInterface) registry.lookup("BrokerService");

            // Register this publisher with the broker
            broker.addPublisher(publisher.getPublisherId(), publisher);

            Scanner scanner = new Scanner(System.in);
            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM HH:mm:ss");

            while (true) {
                System.out.print("Please select command: create, publish, show, delete, exit: ");
                String command = scanner.nextLine().trim();

                if (command.startsWith("create")) {
                    String[] parts = command.split(" ");
                    if (parts.length == 3) {
                        String topicId = parts[1];
                        String topicName = parts[2];

                        // Call createTopic and capture the response
                        boolean isCreated = broker.createTopic(topicId, topicName, publisher.getPublisherId());

                        // Handle the response and show the appropriate message
                        if (isCreated) {
                            System.out.println("[success] Topic created.");
                        } else {
                            System.out.println("[error] Topic with ID '" + topicId + "' already exists.");
                        }
                    } else {
                        System.out.println("[error] Invalid input format. Usage: create {topic_id} {topic_name}");
                    }
                }else if (command.startsWith("publish")) {
                    String[] parts = command.split(" ", 3);
                    if (parts.length == 3) {
                        String topicId = parts[1];
                        String message = parts[2];

                        // Check if the message exceeds 100 characters
                        if (message.length() > 100) {
                            System.out.println("[error] Message length exceeds 100 characters. Please shorten your message.");
                        } else {
                            try {
                                // Try to get the topic name, this will ensure the topic exists before publishing
                                broker.getTopicName(topicId);
                                boolean isPublished = broker.publishMessage(topicId, message, publisher.getPublisherId());  // Access instance variable publisherId
                                if (isPublished) {
                                    String timestamp = formatter.format(new Date());
                                    System.out.println("[" + timestamp + "] [" + topicId + ": " + broker.getTopicName(topicId) + ":] " + message);
                                } else {
                                    System.out.println("[error] You do not have permission to publish to topic: " + topicId);
                                }
                            } catch (RemoteException e) {
                                if (e.getMessage().contains("Topic not found")) {
                                    System.out.println("[error] The topic with ID '" + topicId + "' does not exist. Please create the topic before publishing.");
                                } else {
                                    System.out.println("[error] An error occurred while trying to publish the message: " + e.getMessage());
                                }
                            }
                        }
                    } else {
                        System.out.println("[error] Invalid input format. Usage: publish {topic_id} {message}");
                    }
                } else if (command.startsWith("show")) {
                    String[] parts = command.split(" ");
                    if (parts.length == 2) {
                        String topicId = parts[1];
                        try {
                            int count = broker.showSubscriberCount(topicId);
                            System.out.println("[" + topicId + "] [" + broker.getTopicName(topicId) + "] [Subscribers: " + count + "]");
                        } catch (RemoteException e) {
                            System.out.println("[error] Topic with ID " + topicId + " not found.");
                        }
                    } else {
                        System.out.println("[error] Invalid input format. Usage: show {topic_id}");
                    }
                } else if (command.startsWith("delete")) {
                    String[] parts = command.split(" ");
                    if (parts.length == 2) {
                        String topicId = parts[1];
                        boolean isDeleted = broker.deleteTopic(topicId, publisher.getPublisherId());  // 传递 publisherId 进行权限检查
                        if (isDeleted) {
                            System.out.println("[success] Topic deleted.");
                        } else {
                            System.out.println("[error] You do not have permission to delete this topic: " + topicId);
                        }
                    } else {
                        System.out.println("[error] Invalid input format. Usage: delete {topic_id}");
                    }
                } else if (command.equalsIgnoreCase("exit")) {
                    System.out.println("[info] Exiting Publisher Client.");
                    broker.removePublisher(publisher.getPublisherId());  // Ensure broker removes this publisher
                    break;
                } else {
                    System.out.println("[error] Invalid command. Please select: create, publish, show, delete, exit");
                }
            }
        } catch (Exception e) {
            System.out.println("[error] " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public List<String> getPublishedTopics() throws RemoteException {
        // If needed, return the list of topics this publisher has published
        return new ArrayList<>();  // This is a placeholder. You can implement it as needed.
    }
}
