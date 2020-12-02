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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author DAVID
 */
public class Website extends Thread {
    //volatile keyword to avoid memory inconsistencies
    private volatile int numberSubscribed;
    private Lock numberSubscribedLock;
    
    private volatile int numberContactsRecorded;
    private Lock numberContactsRecordedLock;
    
    private volatile int numberContactsNotified;
    
    
    private volatile boolean running;
    
    private final GUI theGUI;
    private final DayCounter day;
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
        
        day.start();
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
        if(!database.isPhoneRegistered(p)){
            registerPhone(p);
        }
        
        //gettin mutex lock of dedicated Lock object
        numberContactsRecordedLock.lock();
        numberContactsRecorded++;
        numberContactsRecordedLock.unlock();
        
        database.recordContact(p, c);
    }
    
    public void recordThatIsInfected(Person p){
        
        //thread-safe collection is used, since multiple Person threads call this method
        infected.add(p);       //inserts specified element at the end of this queue
    }
    
    public void registerPhone(Person p){ // TODO: sync method
        if(database.isPhoneRegistered(p)){
            System.out.println("Phone " + p.getPhoneID() + " is already registered");
            return;
        }
        
        //protected with explicit lock object, reentrantlock from multiple Person threads
        numberSubscribedLock.lock();
        numberSubscribed++;
        numberSubscribedLock.unlock();
        
        database.registerPhone(p);
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
    
}
