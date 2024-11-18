/*
 * Name: Zhou Ze
 * Student ID: 1536407
 * Course: COMP90015
 * Assignment: Distributed Publisher-Subscriber System
 */
package publishers;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface PublisherInterface extends Remote {

    // Method to check if the publisher is still alive
    boolean isAlive() throws RemoteException;

    // Method to get the list of topic IDs that this publisher manages
    List<String> getPublishedTopics() throws RemoteException;

    // Additional method for the publisher to notify about its status
    String getPublisherId() throws RemoteException;
}
