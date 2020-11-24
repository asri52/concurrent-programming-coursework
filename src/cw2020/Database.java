/*
 * Database.java is a class that models the operations of a Track-and-Trace database.
 * Is stores a List of registered People
 * It stores a Map associating People with the ist of Contacts that they have had.
 */
package cw2020;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 * @author DAVID
 */
public class Database {
    
    /*for previous equal priority implementation, retained for review purposes
    private ReadWriteLock lock;
    private Lock readLock; //read lock
    private Lock writeLock; //write lock
    */
    
    //fields to implement monitor that prioretises Writers
    private ReentrantLock theEntryLock ;
    private Condition okToRead ;
    private Condition okToWrite ;
    private int numReaders ;
    private int numWriters ;
    private int numReadersWaiting ;
    private int numWritersWaiting ;
    
    //original fields
    private ArrayList<Person> registered;
    private HashMap<Person, ArrayList<Contact>> contactRecords;
    
    public Database(){
        registered = new ArrayList();
        contactRecords = new HashMap();
        
        theEntryLock = new ReentrantLock();
        okToWrite = theEntryLock.newCondition();
        okToRead = theEntryLock.newCondition();
        numReaders = 0;
        numWriters = 0;
        numReadersWaiting = 0;
        numWritersWaiting = 0;
        
        /*equal priority implementation
        //initialise locks
        lock = new ReentrantReadWriteLock();
        readLock = lock.readLock();
        writeLock = lock.writeLock();
*/
    }
    
     //methods for prioretising Writers
    public void startWrite() throws InterruptedException{
        try {    
            theEntryLock.lock();
            //while (!((numReaders == 0) && (numWriters == 0)) ) {
            while ((numReaders > 0) || (numWriters > 0) ) {
                numWritersWaiting++;
                okToWrite.await();
                numWritersWaiting--;
            }
            numWriters++;
        } finally {
            theEntryLock.unlock();
        }
    }
    

    public void endWrite(){
        try { 
            theEntryLock.lock();
            numWriters--;
            if (numWritersWaiting > 0) {
                okToWrite.signal();
            } else {
                okToRead.signal();
            }
        } finally {
            theEntryLock.unlock();
        }
    }    
    

    public void startRead() throws InterruptedException{
         try {
            theEntryLock.lock();
            //while (!((numWriters == 0) && (numWritersWaiting == 0))) {
            while ((numWriters > 0) || (numWritersWaiting > 0)) {
                numReadersWaiting++;
                okToRead.await();
                numReadersWaiting--;
            }
            numReaders++;
            okToRead.signal();
        } finally {
            theEntryLock.unlock();
        }
    }

    public void endRead(){
        theEntryLock.lock();
        try {
            numReaders--;
            if (numReaders == 0) {
                okToWrite.signal();
            }
        } finally {
            theEntryLock.unlock();
        }
    }   
    
    public void registerPhone(Person p){
        
        try {
            //start writing by locking the writeLock
            //writeLock.lock();

            startWrite();
            if(!registered.contains(p)){
                registered.add(p);
                contactRecords.put(p, new ArrayList<>());
            }
         
            //end reading by unlocking the write lock
            //writeLock.unlock();
        } catch (InterruptedException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        }
        finally {
            endWrite();
        }
        
    }
    
    public void recordContact(Person p , Contact c){
        //writeLock.lock();
        try {
            startWrite();
            contactRecords.get(p).add(c);
        } 
        catch (InterruptedException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        }
        finally {
            endWrite();
           // writeLock.unlock();
        }
    }
    
    public boolean isPhoneRegistered(Person p){
        //readLock.lock();
        boolean isRegistered = false;
        try {
            startRead();
            isRegistered = registered.contains(p);
        } 
        catch (InterruptedException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        }
        finally {
            //readLock.unlock();
            endRead();
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
        
        //readLock.lock();
        try {
            startRead();
            total = registered.stream().map((p) -> contactRecords.get(p).size()).reduce(total, Integer::sum);
        } 
        catch (InterruptedException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        }
        finally {
            //readLock.unlock();
            endRead();
        }
        return total;
    }
    
    
}
