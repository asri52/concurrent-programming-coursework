/*
 * COntact.java is a simple class that stores a record of a contact between 
 * one Person and another. It stores the time and dration of the contact 
 */
package cw2020;

import java.time.LocalDateTime;

/**
@author DAVID
 */
public class Contact {
    private final Person phone;
    private final LocalDateTime start;
    private final long minutes;

    public Contact(Person phone) {
        this.phone = phone;
        this.start = LocalDateTime.now();
        this.minutes = randomMinutes();
    }

    public Person getPhone() {
        return phone;
    }

    public LocalDateTime getStart() {
        return start;
    }

    public long getMinutes() {
        return minutes;
    }
    
    public void disconnected(){
        start.plusMinutes(randomMinutes());
    }
    
    public static long randomMinutes(){
        return (long)(Math.random()*30);
    }
    
    @Override public String toString(){
        return phone.getPhoneID() + " for " + minutes + " mins on " + start;
    }
}
