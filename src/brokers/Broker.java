/*
 * Name: Zhou Ze
 * Student ID: 1536407
 * Course: COMP90015
 * Assignment: Distributed Dictionary Application
 */
package brokers;

import publishers.PublisherInterface;
import subscribers.SubscriberInterface;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Broker implements BrokerInterface {

    private final Map<String, Topic> topics;
    private final Map<String, PublisherInterface> publishers;  // Track publishers
    private final Map<String, SubscriberInterface> subscribers;
    private final List<String> otherBrokers;
    private static final Logger logger = Logger.getLogger(Broker.class.getName());

    // Constructor for Broker, exporting it as an RMI object
    protected Broker(int port, List<String> otherBrokers) throws RemoteException {
        topics = new HashMap<>();
        publishers = new HashMap<>();  // Track active publishers
        subscribers = new HashMap<>();

        // Initialize the otherBrokers list
        this.otherBrokers = new ArrayList<>(otherBrokers);

        // Export this broker as an RMI object to make it accessible remotely
        UnicastRemoteObject.exportObject(this, port);

        // If no brokers were provided via command-line, use hardcoded defaults
        if (this.otherBrokers.isEmpty()) {
            if (port == 5001) {
                this.otherBrokers.add("localhost:5002");
                this.otherBrokers.add("localhost:5003");
            } else if (port == 5002) {
                this.otherBrokers.add("localhost:5001");
                this.otherBrokers.add("localhost:5003");
            } else if (port == 5003) {
                this.otherBrokers.add("localhost:5001");
                this.otherBrokers.add("localhost:5002");
            }
        }

        // Start monitoring publishers and subscribers for crashes
        new Thread(this::monitorPublishersAndSubscribers).start();
    }

    // Monitor publishers and subscribers
    private void monitorPublishersAndSubscribers() {
        while (true) {
            try {
                // Check for crashed publishers
                for (String publisherId : new ArrayList<>(publishers.keySet())) {
                    PublisherInterface publisher = publishers.get(publisherId);
                    try {
                        if (!publisher.isAlive()) {
                            handlePublisherCrash(publisherId);
                        }
                    } catch (RemoteException e) {
                        handlePublisherCrash(publisherId);
                    }
                }

                // Check for crashed subscribers
                for (String subscriberId : new ArrayList<>(subscribers.keySet())) {
                    SubscriberInterface subscriber = subscribers.get(subscriberId);
                    try {
                        if (!subscriber.isAlive()) {
                            System.out.println("[info] Subscriber " + subscriberId + " is not alive.");
                            handleSubscriberCrash(subscriberId);
                        }
                    } catch (RemoteException e) {
                        System.out.println("[error] Subscriber " + subscriberId + " has crashed (RemoteException).");
                        handleSubscriberCrash(subscriberId);
                    }
                }

                Thread.sleep(5000);  // Monitor every 5 seconds
            } catch (InterruptedException e) {
                logger.log(Level.SEVERE, "Monitoring thread interrupted", e);
            }
        }
    }

    // Handle publisher crash: remove all associated topics and notify subscribers
    private void handlePublisherCrash(String publisherId) {
        System.out.println("[error] Publisher " + publisherId + " has crashed.");
        publishers.remove(publisherId);

        List<String> topicsToRemove = new ArrayList<>();
        for (Map.Entry<String, Topic> entry : topics.entrySet()) {
            if (entry.getValue().getPublisher().equals(publisherId)) {
                topicsToRemove.add(entry.getKey());
            }
        }

        // For each topic created by the crashed publisher, remove the topic and notify subscribers
        for (String topicId : topicsToRemove) {
            try {
                deleteTopic(topicId, topics.get(topicId).getPublisher());  // Ensure all topics associated with this publisher are deleted
            } catch (RemoteException e) {
                logger.log(Level.SEVERE, "Failed to delete topic " + topicId + " after publisher crash", e);
            }
        }
    }

    // Handle subscriber crash: remove all its subscriptions
    private void handleSubscriberCrash(String subscriberId) {
        System.out.println("[error] Subscriber " + subscriberId + " has crashed.");
        subscribers.remove(subscriberId);

        // Remove this subscriber from all topics they are subscribed to
        for (Topic topic : topics.values()) {
            topic.removeSubscriberById(subscriberId);
        }
    }

    // Add publisher (register publisher to broker)
    public void addPublisher(String publisherId, PublisherInterface publisher) {
        publishers.put(publisherId, publisher);
    }

    // Add subscriber (register subscriber to broker)
// Add subscriber (register subscriber to a topic)
    public void addSubscriber(String topicId, SubscriberInterface subscriber) {
        try {
            String subscriberId = subscriber.getSubscriberId();  // Fetch the subscriber ID
            System.out.println("Debug: Retrieved subscriberId = " + subscriberId);
            subscribers.put(subscriberId, subscriber);  // Store the subscriber by ID

            // Log the correct IDs
            System.out.println("Subscriber with ID: " + subscriberId + " added ");
        } catch (RemoteException e) {
            System.out.println("[error] Failed to retrieve subscriber ID during subscription.");
            e.printStackTrace();
        }
    }



    // Create a new topic and propagate it to other brokers
    @Override
    public boolean createTopic(String topicId, String topicName, String publisherId) throws RemoteException {
        // Check if the topic already exists
        if (topics.containsKey(topicId)) {
            System.out.println("[error] Topic with ID '" + topicId + "' already exists. Please choose a different topic ID.");
            return false; // Return false if topic already exists
        }

        // Create the topic if it doesn't exist
        topics.put(topicId,  new Topic(topicId, topicName, publisherId));
        System.out.println("Topic created: " + topicName + " by publisher: " + publisherId);

        // Propagate the new topic to other brokers
        propagateTopicToOtherBrokers(topicId, topicName, publisherId);
        return true; // Return true if the topic was successfully created
    }
    // Method to propagate the newly created topic to other brokers
    private void propagateTopicToOtherBrokers(String topicId, String topicName, String publisherId) {
        for (String brokerAddress : otherBrokers) {
            try {
                String[] addressParts = brokerAddress.split(":");
                String host = addressParts[0];
                int port = Integer.parseInt(addressParts[1]);

                Registry registry = LocateRegistry.getRegistry(host, port);
                BrokerInterface otherBroker = (BrokerInterface) registry.lookup("BrokerService");

                // Inform other brokers about the new topic, including publisherId
                otherBroker.addTopic(topicId, topicName, publisherId);
            } catch (Exception e) {
                System.out.println("[error] Failed to propagate topic to " + brokerAddress + ": " + e.getMessage());
            }
        }
    }

    // Method for other brokers to add a topic locally
    @Override
    public void addTopic(String topicId, String topicName, String publisherId) throws RemoteException {
        if (!topics.containsKey(topicId)) {
            // Store the topic with the publisherId provided by the broker that created it
            topics.put(topicId, new Topic(topicId, topicName, publisherId));
            System.out.println("New topic received and added: " + topicName + " from publisher: " + publisherId);
        }
    }

    // Forward the published message to other brokers
    private void forwardMessageToOtherBrokers(String topicId, String message) {
        for (String brokerAddress : otherBrokers) {
            try {
                String[] addressParts = brokerAddress.split(":");
                String host = addressParts[0];
                int port = Integer.parseInt(addressParts[1]);

                Registry registry = LocateRegistry.getRegistry(host, port);
                BrokerInterface otherBroker = (BrokerInterface) registry.lookup("BrokerService");

                // Forward the message to other brokers
                otherBroker.forwardMessage(topicId, message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // Receive forwarded message and broadcast locally
    @Override
    public void forwardMessage(String topicId, String message) throws RemoteException {
        Topic topic = topics.get(topicId);
        if (topic != null) {
            topic.broadcast(message);  // Broadcast to local subscribers
        }
    }

    // Add a subscriber to the topic
    @Override
    public void subscribeToTopic(String topicId, SubscriberInterface subscriber) throws RemoteException {
        Topic topic = topics.get(topicId);
        if (topic != null) {
            topic.addSubscriber(subscriber);
            System.out.println("Subscriber added to topic: " + topicId);
        } else {
            throw new RemoteException("Topic with ID " + topicId + " does not exist.");
        }
    }

    // Get the subscriber count from the local broker and other brokers
    @Override
    public int showSubscriberCount(String topicId) throws RemoteException {
        int totalCount = 0;

        // Get local subscriber count
        Topic topic = topics.get(topicId);
        if (topic != null) {
            totalCount = topic.getSubscriberCount();
            System.out.println("Local subscriber count for topic " + topicId + ": " + totalCount);
        } else {
            throw new RemoteException("Topic not found.");
        }

        // Get subscriber counts from other brokers
        for (String brokerAddress : otherBrokers) {
            try {
                String[] addressParts = brokerAddress.split(":");
                String host = addressParts[0];
                int port = Integer.parseInt(addressParts[1]);

                Registry registry = LocateRegistry.getRegistry(host, port);
                BrokerInterface otherBroker = (BrokerInterface) registry.lookup("BrokerService");

                try {
                    int remoteCount = otherBroker.getLocalSubscriberCount(topicId);
                    totalCount += remoteCount;
                    System.out.println("Subscriber count from broker " + brokerAddress + ": " + remoteCount);
                } catch (RemoteException e) {
                    if (e.getMessage().contains("Topic not found")) {
                        logger.log(Level.WARNING, "Topic " + topicId + " not found in broker: " + brokerAddress);
                    } else {
                        throw e;
                    }
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to get subscriber count from broker: " + brokerAddress, e);
            }
        }

        System.out.println("Total subscriber count for topic " + topicId + ": " + totalCount);
        return totalCount;
    }

    @Override
    public int getLocalSubscriberCount(String topicId) throws RemoteException {
        Topic topic = topics.get(topicId);
        if (topic != null) {
            return topic.getSubscriberCount();
        } else {
            System.out.println("[error] Topic with ID " + topicId + " not found.");
            return 0;
        }
    }

    // Get the local subscriber count for a topic
    @Override
    public boolean publishMessage(String topicId, String message, String publisherId) throws RemoteException {
        Topic topic = topics.get(topicId);

        if (topic != null) {
            // Check if the publisher ID matches the one that created the topic
            if (topic.getPublisher().equals(publisherId)) {
                // Broadcast the message to local subscribers
                topic.broadcast(message);

                // Forward the message to other brokers
                forwardMessageToOtherBrokers(topicId, message);
                return true; // Operation succeeded
            } else {
                // Publisher is not authorized to publish to this topic
                System.out.println("[error] Publisher with ID '" + publisherId + "' is not authorized to publish to topic '" + topicId + "'.");
                return false; // Operation failed due to unauthorized access
            }
        } else {
            // The specified topic does not exist
            System.out.println("[error] The topic with ID '" + topicId + "' does not exist. Please create the topic before publishing.");
            return false; // Operation failed due to missing topic
        }
    }

    // Delete a topic from the broker
    @Override
    public boolean deleteTopic(String topicId, String publisherId) throws RemoteException {
        Topic topic = topics.get(topicId);

        if (topic != null) {
            // Check if the publisher ID matches the one that created the topic
            if (topic.getPublisher().equals(publisherId)) {
                List<SubscriberInterface> subscribersToRemove = new ArrayList<>(topic.getSubscribers());

                // Notify all subscribers about the topic deletion
                for (SubscriberInterface subscriber : subscribersToRemove) {
                    try {
                        String message = "Topic [" + topicId + "] has been deleted. You have been unsubscribed.";
                        subscriber.receiveMessage(topicId, topic.getTopicName(), message);

                        // Ask subscribers to remove the topic from their local subscription list
                        subscriber.removeTopicLocally(topicId); // This method is invoked on the client side to update their local list
                    } catch (RemoteException e) {
                        // Error occurred while notifying the subscriber
                        System.out.println("[error] Failed to notify subscriber: " + e.getMessage());
                    }
                }

                // Save the publisher ID before removing the topic
                String topicPublisherId = topic.getPublisher();

                // Remove the subscribers after notification
                for (SubscriberInterface subscriber : subscribersToRemove) {
                    topic.removeSubscriber(subscriber);
                }

                // Remove the topic from the system
                topics.remove(topicId);
                System.out.println("Topic with ID '" + topicId + "' has been deleted.");

                // Notify other brokers to delete the topic
                notifyOtherBrokersToDeleteTopic(topicId, topicPublisherId); // Use saved publisher ID
                return true; // Operation succeeded
            } else {
                // Publisher is not authorized to delete this topic
                System.out.println("[error] Publisher with ID '" + publisherId + "' is not authorized to delete topic '" + topicId + "'.");
                return false; // Operation failed due to unauthorized access
            }
        } else {
            // The specified topic does not exist
            System.out.println("[error] Topic with ID '" + topicId + "' does not exist.");
            return false; // Operation failed due to missing topic
        }
    }

    private void notifyOtherBrokersToDeleteTopic(String topicId, String topicPublisherId) {
        for (String brokerAddress : otherBrokers) {
            try {
                String[] addressParts = brokerAddress.split(":");
                String host = addressParts[0];
                int port = Integer.parseInt(addressParts[1]);

                Registry registry = LocateRegistry.getRegistry(host, port);
                BrokerInterface otherBroker = (BrokerInterface) registry.lookup("BrokerService");

                // Notify the other broker to delete the topic, using the saved publisher ID
                otherBroker.deleteTopic(topicId, topicPublisherId);
            } catch (Exception e) {
                System.out.println("[error] Failed to notify broker at " + brokerAddress + ": " + e.getMessage());
            }
        }
    }

    // Get the name of a topic based on its ID
    @Override
    public String getTopicName(String topicId) throws RemoteException {
        Topic topic = topics.get(topicId);
        if (topic != null) {
            return topic.getTopicName();
        } else {
            throw new RemoteException("Topic not found.");
        }
    }

    // Get the list of all topics in the broker
    @Override
    public List<Topic> getTopicList() throws RemoteException {
        return new ArrayList<>(topics.values());
    }

    // Get the current subscriptions for a subscriber
    @Override
    public List<Topic> getCurrentSubscriptions(String subscriberId) throws RemoteException {
        List<Topic> subscribedTopics = new ArrayList<>();
        for (Topic topic : topics.values()) {
            if (topic.hasSubscriber(subscriberId)) {
                subscribedTopics.add(topic);
            }
        }
        return subscribedTopics;
    }

    // Unsubscribe a subscriber from a topic
    @Override
    public void unsubscribeFromTopic(String topicId, SubscriberInterface subscriber) throws RemoteException {
        Topic topic = topics.get(topicId);
        if (topic != null) {
            topic.removeSubscriber(subscriber);
            System.out.println("Subscriber removed from topic: " + topicId);
        } else {
            throw new RemoteException("Topic not found.");
        }
    }

    @Override
    public void removePublisher(String publisherId) throws RemoteException {
        System.out.println("[info] Removing publisher with ID: " + publisherId);
        publishers.remove(publisherId);

        // 如果该 Publisher 关联了任何 topic，可以选择移除它们
        List<String> topicsToRemove = new ArrayList<>();
        for (Map.Entry<String, Topic> entry : topics.entrySet()) {
            if (entry.getValue().getPublisher().equals(publisherId)) {
                topicsToRemove.add(entry.getKey());
            }
        }

        for (String topicId : topicsToRemove) {
            deleteTopic(topicId, topics.get(topicId).getPublisher());
        }
    }
}
