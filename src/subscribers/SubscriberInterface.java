/*
 * Name: Zhou Ze
 * Student ID: 1536407
 * Course: COMP90015
 * Assignment: Distributed Publisher-Subscriber System
 */
package subscribers;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface SubscriberInterface extends Remote {
    // Method for the broker to send a message to the subscriber
    void receiveMessage(String topicId, String topicName, String message) throws RemoteException;

    // Method to get the subscriber's unique ID
    String getSubscriberId() throws RemoteException;

    // New method to handle topic removal on the subscriber side
    void removeTopicLocally(String topicId) throws RemoteException;

    // Method to check if the subscriber is still alive (used to monitor crashes)
    boolean isAlive() throws RemoteException;
}
