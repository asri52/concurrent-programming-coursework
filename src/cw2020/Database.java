/*
 * Database.java is a class that models the operations of a Track-and-Trace database.
 * Is stores a List of registered People
 * It stores a Map associating People with the ist of Contacts that they have had.
 */
package cw2020;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
/**
 * @author DAVID
 */
public class Database {
    
    private ArrayList<Person> registered;
    private HashMap<Person, ArrayList<Contact>> contactRecords;
    
    private ReadWriteLock lock;
    
    public Database(){
        registered = new ArrayList();
        contactRecords = new HashMap();
        lock = new ReentrantReadWriteLock();
    }
    
    public void registerPhone(Person p){
        lock.writeLock().lock();
        try {
            if(!registered.contains(p)){
                registered.add(p);
                contactRecords.put(p, new ArrayList<>());
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public void recordContact(Person p , Contact c){
        lock.writeLock().lock();
        try {
            contactRecords.get(p).add(c);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public boolean isPhoneRegistered(Person p){
        lock.readLock().lock();
        boolean isRegistered;
        try {
            isRegistered = registered.contains(p);
        } finally {
            lock.readLock().unlock();
        }
        return isRegistered;
    }
    
    public String report(){
        return getNumberSubscribers() 
                + " Phones registered in DB \n"
                + getNumberContacts() 
                + " Contacts registered in DB";
    }

    public ArrayList<Person> getRegistered() {
        return registered;
    }

    public HashMap<Person, ArrayList<Contact>> getContactRecords() {
        return contactRecords;
    }
    
    public int getNumberSubscribers() {
        return registered.size();
    }

    public int getNumberContacts() {
        int total = 0;
        lock.readLock().lock();
        try {
            total = registered.stream().map((p) -> contactRecords.get(p).size()).reduce(total, Integer::sum);
        } finally {
            lock.readLock().unlock();
        }
        return total;
    }
    
    
}
