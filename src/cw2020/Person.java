/*
 * Person.java is a class that implements Thread so as to run independetly of GUI and Population
 * It stores the phne ID and list of contacts with other People.
 * It tracks the status of whether the Person is infected, isolating, 
 * recovered, and resgistered with the website.
 * When infected it tracks the days from the start of infection, 
 * and the person recoveres after 14 days.

 * Whether the Person gets infected when contacting an infected person, and whether
 * they choose to self-isolate are randomly decided.
 */
package cw2020;

import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
/**
 * @author DAVID
 */
public class Person extends Thread implements Comparable<Person> {
    /* data describing Phone */
    private final String phoneID;
    private LinkedList<Contact> contacts;
    
    /* data relating to status of the Person */
    private boolean isolating;
    private boolean infected;
    private boolean recovered;
    private boolean registered;
    private int dayStartedIsolation;
    private int dayInfected;
   
    /* references to objects within program */
    private static final Random numberGenerator = new Random(); // Thread safe since Java 7
    private Website theWebsite;

    /* data describing average behaviour of population */      
    public static volatile double probabilityInfectedIfInContact = 1.0;
    public static volatile double probabilitySelfisolatesIfAlerted = 1.0;
    
    /* class-level counts for checking thread-safe operation */
    //Atomic ensures volatile and atomicity
    private static AtomicInteger contactCount = new AtomicInteger(0);
    //private static Lock contactCountLock = new ReentrantLock(true);
    
    private static AtomicInteger phonesRegistered = new AtomicInteger(0);
    //private static Lock phonesRegisteredLock = new ReentrantLock(true);
    
    private static AtomicInteger numberIsolating = new AtomicInteger(0);
    //private static Lock numberIsolatingLock = new ReentrantLock(true);
    
    private static AtomicInteger numberInfected = new AtomicInteger(0);
    //private static Lock numberInfectedLock = new ReentrantLock(true);
    
    private static AtomicInteger numberRecovered = new AtomicInteger(0);
    //private static Lock numberRecoveredLock = new ReentrantLock(true);

    
    public Person(Website w, boolean reg, boolean positive) {        
        this.phoneID = generateRandomID();
        this.contacts = new LinkedList<>();
        this.theWebsite = w;
        this.registered = reg;  if(registered) register(theWebsite);
        this.infected = positive;
        this.setDaemon(true);
        if(infected) {
            
//            try {
//                numberInfectedLock.lock();
//                numberInfected++;
//            } finally {
//                numberInfectedLock.unlock();
//            }
          
           numberInfected.addAndGet(1);
            
            //thread-safety ensured in Website interface
            if(registered) theWebsite.recordThatIsInfected(this);
            decideWhetherToSelfIsolate();
        }       
    }
    
    @Override
    public void run() {
        while(true){
            while(!isolating ){
                /* process a contact list of contacts */
                if(contacts.size() > 0) handleContact();
                /* if infected has 14 days passed */
                if(infected && theWebsite.getTheDay() > dayInfected + 14){
                    infected = false; 
           
//                    try {
//                        numberInfectedLock.lock();
//                        numberInfected--;
//                    } finally {
//                        numberInfectedLock.unlock();
//                    }
                    numberInfected.decrementAndGet();
                    recovered = true; 
              
//                    try {
//                        numberRecoveredLock.lock();
//                        numberRecovered++;
//                    } finally {
//                        numberRecoveredLock.unlock();
//                    }
                    numberRecovered.addAndGet(1);
                }
                pause(40L); /* approx every hour in system time */
            }
            /* if isolating has 14 days passed */ 
            while(isolating){
                if(theWebsite.getTheDay() > dayStartedIsolation + 14){
                    isolating = false; 
                
                    /*try {
                        numberIsolatingLock.lock();
                        numberIsolating--;
                    } finally {
                        numberIsolatingLock.unlock();
                    }
                    */
                    numberIsolating.decrementAndGet();
                    
                }
                pause(40L); /* approx every hour in system time */
            }
        }
    }
    
    public boolean isInfected(){
        return infected;
    }
    
    public boolean isIsolating(){
        return isolating;
    }
       
    public void setInfected(){
        infected = true;
    }
    
    //added for report purposes
    public boolean isRegistered(){
        return registered;
    }
    
    public boolean isRecovered() {
        return recovered;
    }

    /* Phone interactions with Website */
    public void register(Website w) {
        registered = true;
        
        /*try {
            phonesRegisteredLock.lock();
            phonesRegistered++;
        } finally {
            phonesRegisteredLock.unlock();
        }
        */
        phonesRegistered.addAndGet(1);
        
        this.theWebsite = w;
        theWebsite.registerPhone(this);
    }
       
    public void contactWith(Person other) {
        Contact aContact = new Contact(other);
        contacts.add(aContact);
    }
    
    public void handleContact() {
        Contact c = contacts.removeFirst();
        if(registered) theWebsite.recordContact(this, c);

        /*try {           
            contactCountLock.lock();
            contactCount++;
        } finally {
            contactCountLock.unlock();
        }
        */
        contactCount.addAndGet(1);
        
        if(recovered) return;
        if(!infected && c.getPhone().infected) chanceOfInfectionOnContact();
    }
       
    public void notifiedAboutPositiveContact() {
        if(!this.isolating) decideWhetherToSelfIsolate();
    }
    
    public void chanceOfInfectionOnContact(){
        double risk = numberGenerator.nextDouble();
        if(risk < probabilityInfectedIfInContact) {
            infected = true;
            
//            try {
//                numberInfectedLock.lock();
//                numberInfected++;
//            } finally {
//                numberInfectedLock.unlock();
//            }
            numberInfected.addAndGet(1);
            if(registered) theWebsite.recordThatIsInfected(this); 
            decideWhetherToSelfIsolate();
        }
    }
    
    public void decideWhetherToSelfIsolate(){
        double decide = numberGenerator.nextDouble();
        if(decide < probabilitySelfisolatesIfAlerted) {
            isolating = true;
           
            /*try {
                numberIsolatingLock.lock();
                numberIsolating++;
            } finally {
                numberIsolatingLock.unlock();
            }
            */
            numberIsolating.addAndGet(1);
            
            dayStartedIsolation = theWebsite.getTheDay();
        }
    }
    
    private static String generateRandomID() {
        String newID = "07";
        for (int digit = 3; digit <= 11; digit++) {
            newID += numberGenerator.nextInt(10);
            if (digit == 5) {
                newID += "-";
            }
        }
        return newID;
    }

    public String getPhoneID() {
        return phoneID;
    }

    public LinkedList<Contact> getContacts() {
        return contacts;
    }

    @Override
    public String toString() {
        return phoneID + " has " + contacts.size() + " contacts,"
                + " registered: " + registered
                + " infected: " + infected
                + " recovered: " + recovered
                + " self-isolating: " + isolating;
    }

    public static int getContactCount() {
       /* try {
            contactCountLock.lock();
            return contactCount;
        } finally {
            contactCountLock.unlock();
        }
        */
       //returns the current value atomically
       return contactCount.get();
    }

    public static int getNumberInfected() {
       int temp;
        
        /*try {
            //reading of volatile data is atomic but a more predictable way to
            //ensure thread safety is to acquire the lock from the same object as
            //other operations on this data
            numberInfectedLock.lock();
            temp = numberInfected;
        } finally {
            numberInfectedLock.unlock();
        }
       */
        return numberInfected.getAcquire();
    }

    public static int getNumberRecovered() {
        
        /*int temp;
        try {
            numberRecoveredLock.lock();
            temp = numberRecovered;
        } finally {
            numberRecoveredLock.unlock();
        }
        
        return temp;
        */
        return numberRecovered.getAcquire();
    }

    public static int getNumberIsolating() {
        
        /*int temp;
        try {
            numberIsolatingLock.lock();
            temp = numberIsolating;
        } finally {
            numberIsolatingLock.unlock();
        }
        */
      return numberIsolating.getAcquire();
    }
       
    public int compareTo(Person otherPhone) {
        return this.phoneID.compareTo(otherPhone.phoneID);
    }
    
    public static void setProbabilityInfectedIfInContact(double prob) {
        probabilityInfectedIfInContact = prob;
    }

    public static void setProbabilitySelfisolatesIfAlerted(double prob) {
        probabilitySelfisolatesIfAlerted = prob;
    }

    public static double getProbabilityInfectedIfInContact() {
        return probabilityInfectedIfInContact;
    }

    public static double getProbabilitySelfisolatesIfAlerted() {
        return probabilitySelfisolatesIfAlerted;
    }
    
    //getting the mutex lock of all separately dedicated locks
    public static void resetCounts(){
        
       /* try {
            contactCountLock.lock();
            phonesRegisteredLock.lock();
            numberIsolatingLock.lock();
            numberInfectedLock.lock();
            numberRecoveredLock.lock();
            
            contactCount = 0;
            phonesRegistered = 0;
            numberIsolating = 0;
            numberInfected = 0;
            numberRecovered = 0;
        } finally {
            contactCountLock.unlock();
            phonesRegisteredLock.unlock();
            numberIsolatingLock.unlock();
            numberInfectedLock.unlock();
            numberRecoveredLock.unlock();
        }
        */
        contactCount.set(0);
        phonesRegistered.set(0);
        numberIsolating.set(0);
        numberInfected.set(0);
        numberRecovered.set(0);
        
    }
    
    
    public static String report(){
        /*int cc;
        int pr;
        
        try {
            contactCountLock.lock();
            phonesRegisteredLock.lock();
            cc = contactCount;
            pr = phonesRegistered;
        } finally {
            contactCountLock.unlock();
            phonesRegisteredLock.unlock();
        }
        */
        
        return contactCount.getAcquire() + " contacts counted in run of Phone\n"
                + phonesRegistered.getAcquire() + " Phones tried to register\n"
        

    + probabilityInfectedIfInContact*100 + "% chance of infection per contact\n"
    + probabilitySelfisolatesIfAlerted*100 + "% isolate";
    }

    private void pause(long ms){
        try { Thread.sleep(ms); } 
        catch (InterruptedException ex) { /* ignore exception */}
    }
}
