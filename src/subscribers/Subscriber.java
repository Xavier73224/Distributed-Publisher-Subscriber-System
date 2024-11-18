/*
 * Name: Zhou Ze
 * Student ID: 1536407
 * Course: COMP90015
 * Assignment: Distributed Publisher-Subscriber System
 */
package subscribers;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Subscriber extends UnicastRemoteObject implements SubscriberInterface {

    private static final long serialVersionUID = 1L;  // Add serial version UID
    private String name;

    // Constructor
    protected Subscriber(String name) throws RemoteException {
        super();
        this.name = name;
    }

    // Remote method to receive messages from the broker
    @Override
    public void receiveMessage(String topicId, String topicName, String message) throws RemoteException {
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM HH:mm:ss");
        String timestamp = formatter.format(new Date());
        System.out.println("[" + timestamp + "] [" + topicId + ": " + topicName + ":] " + message);
    }

    @Override
    public String getSubscriberId() throws RemoteException {
        return this.name;
    }

    // Remote method to handle local topic removal
    @Override
    public void removeTopicLocally(String topicId) throws RemoteException {
        // Notify subscriber about the topic removal
        System.out.println("[info] Topic [" + topicId + "] has been removed locally.");

        // Remove the topic from the subscribedTopics list in SubscriberClient
        if (SubscriberClient.isSubscribed(topicId)) {  // Check if the topic is subscribed
            SubscriberClient.removeSubscribedTopic(topicId);  // Remove the topic from subscribedTopics list
            System.out.println("[info] Removed topic [" + topicId + "] from subscribed topics.");
        } else {
            System.out.println("[warning] Tried to remove non-subscribed topic [" + topicId + "].");
        }
    }

    // Implement isAlive method to check if the subscriber is still alive
    @Override
    public boolean isAlive() throws RemoteException {
        return true;  // You can implement actual logic if needed
    }
}
