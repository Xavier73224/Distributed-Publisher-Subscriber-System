/*
 * Name: Zhou Ze
 * Student ID: 1536407
 * Course: COMP90015
 * Assignment: Distributed Publisher-Subscriber System
 */
package brokers;

import subscribers.SubscriberInterface;
import java.io.Serializable;  // Add Serializable
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

public class Topic implements Serializable {  // Implement Serializable

    // Add this new field to store subscriber IDs
    private List<String> subscriberIds = new ArrayList<>();
    private static final long serialVersionUID = 1L;  // Ensure version control

    private String topicId;
    private String topicName;
    private String publisher;
    private List<SubscriberInterface> subscribers;

    public Topic(String topicId, String topicName, String publisher) {
        this.topicId = topicId;
        this.topicName = topicName;
        this.publisher = publisher;
        this.subscribers = new ArrayList<>();
    }

    // Getter for topicId
    public String getTopicId() {
        return topicId;
    }

    // Getter for topicName
    public String getTopicName() {
        return topicName;
    }

    // Getter for publisher
    public String getPublisher() {
        return publisher;
    }

    // Add a subscriber to this topic
// Modify the addSubscriber method to store the subscriber ID as well
    public void addSubscriber(SubscriberInterface subscriber) throws RemoteException {
        subscribers.add(subscriber);
        subscriberIds.add(subscriber.getSubscriberId());
    }

    // Get the number of subscribers to this topic
    public int getSubscriberCount() {
        return subscribers.size();
    }

    // Get all subscribers of this topic
    public List<SubscriberInterface> getSubscribers() {  // Add this method
        return subscribers;
    }

    public boolean hasSubscriber(String subscriberId) {
        return subscribers.stream().anyMatch(subscriber -> {
            try {
                return subscriber.getSubscriberId().equals(subscriberId);
            } catch (RemoteException e) {
                e.printStackTrace();
                return false;
            }
        });
    }

    public void removeSubscriber(SubscriberInterface subscriber) {
        List<SubscriberInterface> subscribersToRemove = new ArrayList<>();

        for (SubscriberInterface s : subscribers) {
            try {
                if (s.getSubscriberId().equals(subscriber.getSubscriberId())) {
                    subscribersToRemove.add(s);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        subscribers.removeAll(subscribersToRemove);
    }

    public void removeSubscriberById(String subscriberId) {
        boolean subscriberRemoved = subscribers.removeIf(subscriber -> {
            try {
                // Directly compare the subscriberId without relying on the remote call
                return subscriberId.equals(subscriber.getSubscriberId());
            } catch (RemoteException e) {
                // If a RemoteException occurs, assume the subscriber has crashed and remove it
                System.out.println("[error] Subscriber with ID: " + subscriberId + " has crashed and will be removed.");
                return true;  // Remove the subscriber in case of a RemoteException
            }
        });

        if (subscriberRemoved) {
            System.out.println("Subscriber with ID: " + subscriberId + " removed from topic.");
        } else {
            System.out.println("Subscriber with ID: " + subscriberId + " not found in topic.");
        }
    }

    // Broadcast a message to all subscribers of this topic
    public void broadcast(String message) throws RemoteException {
        System.out.println("Broadcasting message to local subscribers of topic: " + topicId);
        List<SubscriberInterface> crashedSubscribers = new ArrayList<>();  // Track crashed subscribers

        for (SubscriberInterface subscriber : subscribers) {
            try {
                subscriber.receiveMessage(topicId, topicName, message);  // Call the remote method for each subscriber
                System.out.println("Message sent to subscriber: " + subscriber.getSubscriberId());
            } catch (RemoteException e) {
                System.out.println("[error] Failed to send message to subscriber: " + subscriber.getSubscriberId());
                crashedSubscribers.add(subscriber);  // Add crashed subscriber to list
            }
        }

        // Remove crashed subscribers after the broadcast
        for (SubscriberInterface crashedSubscriber : crashedSubscribers) {
            removeSubscriber(crashedSubscriber);
        }
    }
}
