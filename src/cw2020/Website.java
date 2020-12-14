/*
 * Website.java is a class that models the operations of a Track-and-Trace website.
 * Associated with Database class that stores Person records of those people
 * who have had contact.
 * Website extends Thread to run independently of the GUI
 */
package cw2020;

import java.util.ArrayList;
//original import for linkedlist
//import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author DAVID
 */
public class Website extends Thread {
    
    //Queue management - Procuder-Consumer Pattern
    public class QueueManager {
    private Lock lock;
    private Condition bufferNotEmpty;
    private Condition bufferNotFull;
    
    private int[] buffer;
    private int bufferSize, in, out, numOfItems;

    public QueueManager(int size) {
        lock = new ReentrantLock(); // initialise the lock
/* initialise 2 condition variables associated with lock */
        bufferNotEmpty = lock.newCondition();
        bufferNotFull = lock.newCondition();
        /* initialise buffer data*/
        bufferSize = size;
        in = 0;
        out = 0;
        numOfItems = 0;
        buffer = new int[bufferSize];
    
    }
        public void add(int x) {
            try {
                lock.lock();
                while (numOfItems == bufferSize) {
                    try {
                        bufferNotFull.await();
                    } catch (InterruptedException e) {
                    }
                }
                buffer[in] = x;
                in = (in + 1) % bufferSize;
                numOfItems++;
                bufferNotEmpty.signal();
            } finally {
                lock.unlock();
            }
        }
        
        public int remove() {
            try {
                lock.lock();
                while (numOfItems == 0) {
                    try {
                        bufferNotEmpty.await();
                    } catch (InterruptedException e) {
                    }
                }
                int x = buffer[out];
                out = (out + 1) % bufferSize;
                numOfItems--;
                bufferNotFull.signal();
                return x;
            } finally {
                lock.unlock();
            }
        }

    }
    
    private QueueManager queueManager = new QueueManager(5);
    //volatile keyword to avoid memory inconsistencies
    private volatile int numberSubscribed;
    private Lock numberSubscribedLock;
    
    private volatile int numberContactsRecorded;
    private Lock numberContactsRecordedLock;
    
    private volatile int numberContactsNotified;
    
    
    private volatile boolean running;
    
    private final GUI theGUI;
    private final DayCounter day;
    private ExecutorService dayexecutor;
    private final Database database; //interface of Database synced
    private final ConcurrentLinkedQueue<Person> infected; 
    
    public Website(GUI gui){
        this.theGUI = gui;
        this.database = new Database();
        this.infected = new ConcurrentLinkedQueue();
        this.setDaemon(true);
        this.day = new DayCounter(1000L); // set to run at 1 day per second
        
        //locks are initialised (fair lock with true parameter)
        numberContactsRecordedLock = new ReentrantLock(true);
        numberSubscribedLock = new ReentrantLock(true);
        
        dayexecutor = Executors.newFixedThreadPool(1);
        
        dayexecutor.submit(day);
    }
    
    @Override public void run(){
        running = true;
        while(running){
            //GUIupdates itself periodically instead
            //theGUI.updateData(); 
            
            if(infected.size() > 0){ /* loop that reads People from list of Infected, and informs their contacts */
                
                //poll() retvieves and removes head (first element) from this queue, 
                //just like removeFirst() in LinkedList
                Person p = infected.poll(); 
                ArrayList<Contact> contacts = database.getContactRecords().get(p);               
                for(Contact c: contacts) {
                    Person p2 = c.getPhone();
                    p2.notifiedAboutPositiveContact();
                    
                    //this is an atomic operation and is only within the Website thread context -> no reentrantlock is applied
                    numberContactsNotified++;              
                }        
            }
            pause(100L);           
        }
    }
    
    public void recordContact(Person p, Contact c){
        queueManager.add(1);
        
        if(!database.isPhoneRegistered(p)){
            registerPhone(p);
        }
        
        //gettin mutex lock of dedicated Lock object
        numberContactsRecordedLock.lock();
        numberContactsRecorded++;
        numberContactsRecordedLock.unlock();
        
        database.recordContact(p, c);
        
        queueManager.remove();
    }
    
    public void recordThatIsInfected(Person p){
        queueManager.add(1);
        
        //thread-safe collection is used, since multiple Person threads call this method
        infected.add(p);       //inserts specified element at the end of this queue
        
        queueManager.remove();
    }
    
    public void registerPhone(Person p){ 
        queueManager.add(1);
        
        if(database.isPhoneRegistered(p)){
            System.out.println("Phone " + p.getPhoneID() + " is already registered");
            return;
        }
        
        //protected with explicit lock object, reentrantlock from multiple Person threads
        numberSubscribedLock.lock();
        numberSubscribed++;
        numberSubscribedLock.unlock();
        
        database.registerPhone(p);
        
        queueManager.remove();
    }
    
    public int getTheDay(){
        return day.getTheDay();
    }

    public int getNumberSubscribed() {
        
        //read operation on int is atomic, no interweaving with increment operations in Person thread context
        return numberSubscribed;
    }

    public int getNumberContactsRecorded() {
        
        //read operation on int is atomic, no interweaving with increment operations in Person thread context        
        return numberContactsRecorded;
    }
    
    public int getNumberContactsNotified() {
        
        //reading boolean is atomic in java
        return numberContactsNotified;
    }

    public Database getDatabase() {
        
        //whichever thread gets hold of the database reference, the db interface is thread-safe, see in Database.java
        return database;
    }
    
    public String report(){
        return numberSubscribed 
                + " Phones registered on Website\n"
                + numberContactsRecorded +
                " Contacts received by Website\n"
                + numberContactsNotified +
                " Contacts notified by Website\n";
    }
    
    private void pause(long ms){ /* convenience method to keep main code tidier */
        try { Thread.sleep(ms);
        } catch (InterruptedException ex) { /* ignore */}
    }
    
    public void shutdown(){
//        
        dayexecutor.shutdown();
        //wait until all threads are finished.
        try {
            if (!dayexecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                // we have waited long enough just shut down all threads now
                dayexecutor.shutdownNow();
                System.out.println("I am not going to wait any longer");
            }
        } catch (InterruptedException ie) {
            // don't wait any longer just shut down all threads now
            dayexecutor.shutdownNow();
            System.out.println("I am not going to wait any longer");
        }
    }
    
}
