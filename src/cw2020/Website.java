/*
 * Website.java is a class that models the operations of a Track-and-Trace website.
 * Associated with Database class that stores Person records of those people
 * who have had contact.
 * Website extends Thread to run independently of the GUI
 */
package cw2020;

import java.util.ArrayList;
import java.util.LinkedList;

/**
 * @author DAVID
 */
public class Website extends Thread {
    private int numberSubscribed;
    private int numberContactsRecorded;
    private int numberContactsNotified;
    private boolean running;
    
    private final GUI theGUI; // TODO: data members can be marked final
    private final DayCounter day;
    private final Database database;
    private final LinkedList<Person> infected; // TODO: use java.util.concurrent thread-safe collection instead or sync the entire Website interface that accesses it
    
    public Website(GUI gui){
        this.theGUI = gui;
        this.database = new Database();
        this.infected = new LinkedList();
        this.setDaemon(true);
        this.day = new DayCounter(1000L); // set to run at 1 day per second
        day.start();
    }
    
    @Override public void run(){
        running = true;
        while(running){
            theGUI.updateData(); 
            
            if(infected.size() > 0){ /* loop that reads People from list of Infected, and informs their contacts */
                Person p = infected.removeFirst();
                ArrayList<Contact> contacts = database.getContactRecords().get(p);
                for(Contact c: contacts) {
                    Person p2 = c.getPhone();
                    p2.notifiedAboutPositiveContact();
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
        numberContactsRecorded++;
        database.recordContact(p, c);
    }
    
    public void recordThatIsInfected(Person p){
        infected.add(p);       
    }
    
    public void registerPhone(Person p){ // TODO: sync method
        if(database.isPhoneRegistered(p)){
            System.out.println("Phone " + p.getPhoneID() + " is already registered");
            return;
        }
        numberSubscribed++;
        database.registerPhone(p);
    }
    
    public int getTheDay(){
        return day.getTheDay();
    }

    public int getNumberSubscribed() {
        return numberSubscribed;
    }

    public int getNumberContactsRecorded() {
        return numberContactsRecorded;
    }
    
    public int getNumberContactsNotified() {
        return numberContactsNotified;
    }

    public Database getDatabase() {
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
