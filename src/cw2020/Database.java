/*
 * Database.java is a class that models the operations of a Track-and-Trace database.
 * Is stores a List of registered People
 * It stores a Map associating People with the ist of Contacts that they have had.
 */
package cw2020;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author DAVID
 */
public class Database {
    
    private ArrayList<Person> registered;
    private HashMap<Person, ArrayList<Contact>> contactRecords;
    
    public Database(){
        registered = new ArrayList();
        contactRecords = new HashMap();
    }
    
    public void registerPhone(Person p){
        if(!registered.contains(p)){
            registered.add(p);
            contactRecords.put(p, new ArrayList<>());
        }
    }
    
    public void recordContact(Person p , Contact c){
        contactRecords.get(p).add(c);
    }
    
    public boolean isPhoneRegistered(Person p){
        return registered.contains(p);
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
        for(Person p: registered) 
            total += contactRecords.get(p).size();
        return total;
    }
    
    
}
