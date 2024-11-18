/*
 * Name: Zhou Ze
 * Student ID: 1536407
 * Course: COMP90015
 * Assignment: Distributed Publisher-Subscriber System
 */
package subscribers;

import brokers.BrokerInterface;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

public class SubscriberClient {

    // Store all subscribed topics
    private static List<String> subscribedTopics = new ArrayList<>();

    // Check if a topic is subscribed
    public static boolean isSubscribed(String topicId) {
        return subscribedTopics.contains(topicId);
    }

    // Remove a topic from the subscribed list
    public static void removeSubscribedTopic(String topicId) {
        subscribedTopics.remove(topicId);
    }

    public static void main(String[] args) {
        try {
            // Ensure that username, broker IP, and port are provided via command-line arguments
            if (args.length < 3) {
                System.out.println("Usage: java -jar subscriber.jar <username> <broker_ip> <broker_port>");
                return;
            }

            String username = args[0];  // Get the username from command-line arguments
            String brokerIp = args[1];
            int brokerPort = Integer.parseInt(args[2]);

            // Connect to the RMI registry using provided IP and port
            Registry registry = LocateRegistry.getRegistry(brokerIp, brokerPort);
            BrokerInterface broker = (BrokerInterface) registry.lookup("BrokerService");

            Subscriber subscriber = new Subscriber(username);  // Use username as unique ID

            // Register this subscriber with the broker
            broker.addSubscriber(subscriber.getSubscriberId(), subscriber);

            // Only export the object if it's not already exported
            if (UnicastRemoteObject.toStub(subscriber) == null) {
                UnicastRemoteObject.exportObject(subscriber, 0);  // Export the subscriber object for remote access
            }

            Scanner scanner = new Scanner(System.in);

            while (true) {
                System.out.print("Please select command: list, sub, current, unsub, exit: ");
                String command = scanner.nextLine().trim();

                if (command.equals("list all")) {
                    // List all topics
                    System.out.println("Available topics:");
                    broker.getTopicList().forEach(topic -> {
                        System.out.println("[" + topic.getTopicId() + "] [" + topic.getTopicName() + "] [" + topic.getPublisher() + "]");
                    });

                } else if (command.startsWith("sub ")) {
                    // Subscribe to a topic
                    String[] parts = command.split(" ");
                    if (parts.length == 2) {
                        String topicId = parts[1];
                        try {
                            // Check if already subscribed
                            if (!subscribedTopics.contains(topicId)) {
                                broker.subscribeToTopic(topicId, subscriber);  // Pass the subscriber object
                                subscribedTopics.add(topicId);  // Add the subscribed topic to the list
                                System.out.println("[success] Subscribed to topic " + topicId);
                            } else {
                                System.out.println("[info] Already subscribed to topic " + topicId);
                            }
                        } catch (RemoteException e) {
                            if (e.getMessage().contains("Topic with ID")) {
                                System.out.println("[info] The topic you tried to subscribe to does not exist. Please check the available topics and try again.");
                            } else {
                                System.out.println("[error] Something went wrong while subscribing: " + e.getMessage());
                            }
                        }
                    } else {
                        System.out.println("[error] Invalid input format. Usage: sub {topic_id}");
                    }

                } else if (command.equals("current")) {
                    // Show current subscriptions for this subscriber
                    System.out.println("Current subscriptions:");
                    broker.getCurrentSubscriptions(subscriber.getSubscriberId()).forEach(topic -> {
                        System.out.println("[" + topic.getTopicId() + "] [" + topic.getTopicName() + "] [" + topic.getPublisher() + "]");
                    });

                } else if (command.startsWith("unsub ")) {
                    // Unsubscribe from a topic
                    String[] parts = command.split(" ");
                    if (parts.length == 2) {
                        String topicId = parts[1];
                        if (isSubscribed(topicId)) {
                            broker.unsubscribeFromTopic(topicId, subscriber);  // Pass the subscriber object
                            removeSubscribedTopic(topicId);  // Remove the unsubscribed topic from the list
                            System.out.println("[success] Unsubscribed from topic " + topicId);
                        } else {
                            System.out.println("[info] Not subscribed to topic " + topicId);
                        }
                    } else {
                        System.out.println("[error] Invalid input format. Usage: unsub {topic_id}");
                    }

                } else if (command.equalsIgnoreCase("exit")) {
                    try {
                        // Unsubscribe from all topics before exiting
                        for (String topicId : subscribedTopics) {
                            broker.unsubscribeFromTopic(topicId, subscriber);  // Unsubscribe from each topic
                        }
                        UnicastRemoteObject.unexportObject(subscriber, true);  // Clean up the subscriber RMI object
                        System.out.println("[info] Exiting Subscriber Client.");
                    } catch (Exception e) {
                        System.out.println("[error] Failed to clean up subscriber before exit: " + e.getMessage());
                    }
                    break;

                } else {
                    System.out.println("[error] Invalid command. Please select: list, sub, current, unsub, exit");
                }
            }
        } catch (Exception e) {
            System.out.println("[error] " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Method to remove the topic from the subscriber's local list (called when topic is deleted)
    public static void removeTopicLocally(String topicId) {
        if (isSubscribed(topicId)) {
            removeSubscribedTopic(topicId);
            System.out.println("[info] Removed topic " + topicId + " from local subscription list.");
        } else {
            System.out.println("[warning] Tried to remove non-subscribed topic " + topicId);
        }
    }
}
