/*
 * Name: Zhou Ze
 * Student ID: 1536407
 * Course: COMP90015
 * Assignment: Distributed Publisher-Subscriber System
 */
package brokers;

import publishers.PublisherInterface;
import subscribers.SubscriberInterface;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface BrokerInterface extends Remote {

    // Method to create a new topic with a publisherId
    boolean createTopic(String topicId, String topicName, String publisherId) throws RemoteException;

    // Method to add a new topic received from another broker
    void addTopic(String topicId, String topicName, String publisherId) throws RemoteException;

    // Method to get the name of a topic based on its ID
    String getTopicName(String topicId) throws RemoteException;

    // Method to add/register a new publisher with the broker
    void addPublisher(String publisherId, PublisherInterface publisher) throws RemoteException;

    // Method to add/register a new subscriber with the broker
    void addSubscriber(String subscriberId, SubscriberInterface subscriber) throws RemoteException;

    // Method to publish a message to a topic
    boolean publishMessage(String topicId, String message, String publisherId) throws RemoteException;

    // Forward a published message to another broker for broadcasting
    void forwardMessage(String topicId, String message) throws RemoteException;

    // Method to subscribe a subscriber to a topic
    void subscribeToTopic(String topicId, SubscriberInterface subscriber) throws RemoteException;

    // Method to show the number of subscribers for a given topic
    int showSubscriberCount(String topicId) throws RemoteException;

    // Method to get the local subscriber count for a given topic
    int getLocalSubscriberCount(String topicId) throws RemoteException;

    // Method to get the current subscriptions of a subscriber
    List<Topic> getCurrentSubscriptions(String subscriberId) throws RemoteException;

    // Method to unsubscribe a subscriber from a topic
    void unsubscribeFromTopic(String topicId, SubscriberInterface subscriber) throws RemoteException;

    // Method to delete a topic from the broker
    boolean deleteTopic(String topicId, String publisherId) throws RemoteException;

    // Method to remove a publisher
    void removePublisher(String publisherId) throws RemoteException;

    // Method to get a list of all available topics
    List<Topic> getTopicList() throws RemoteException;
}
