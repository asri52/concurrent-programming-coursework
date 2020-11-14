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
/**
 * @author DAVID
 */
public class Person extends Thread implements Comparable<Person> { // TODO: sync entire interface
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
    public static double probabilityInfectedIfInContact = 1.0;
    public static double probabilitySelfisolatesIfAlerted = 1.0;
    
    /* class-level counts for checking thread-safe operation */
    private static int contactCount = 0;
    private static int phonesRegistered = 0;
    private static int numberIsolating;
    private static int numberInfected;
    private static int numberRecovered;

    
    public Person(Website w, boolean reg, boolean positive) {
        this.phoneID = generateRandomID();
        this.contacts = new LinkedList<>();
        this.theWebsite = w;
        this.registered = reg;  if(registered) register(theWebsite);
        this.infected = positive;
        this.setDaemon(true);
        if(infected) {
            numberInfected++; // TODO: nonatomic operation, synchronize
            if(registered) theWebsite.recordThatIsInfected(this); //TODO:több person, sync entire interface of website by marking methods as synchronized
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
                    infected = false; numberInfected--; // TODO: nonatomic operation, synchronize
                    recovered = true; numberRecovered++; // TODO: nonatomic operation, synchronize
                }
                pause(40L); /* approx every hour in system time */
            }
            /* if isolating has 14 days passed */ 
            while(isolating){
                if(theWebsite.getTheDay() > dayStartedIsolation + 14){
                    isolating = false; numberIsolating--;
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

    /* Phone interactions with Website */
    public void register(Website w) {
        registered = true;
        phonesRegistered++; // TODO: nonatomic operation, sync on static lock object
        this.theWebsite = w;
        theWebsite.registerPhone(this);
    }
       
    public void contactWith(Person other) {
        Contact aContact = new Contact(other);
        contacts.add(aContact);
    }
    
    public void handleContact() { // todo sync
        Contact c = contacts.removeFirst();
        if(registered) theWebsite.recordContact(this, c);
        contactCount++;
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
            numberInfected++;
            if(registered) theWebsite.recordThatIsInfected(this); 
            decideWhetherToSelfIsolate();
        }
    }
    
    public void decideWhetherToSelfIsolate(){
        double decide = numberGenerator.nextDouble();
        if(decide < probabilitySelfisolatesIfAlerted) {
            isolating = true;
            numberIsolating++;
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
                + " self-isolating: " + isolating;
    }

    public static int getContactCount() {
        return contactCount;
    }

    public static int getNumberInfected() {
        return numberInfected;
    }

    public static int getNumberRecovered() {
        return numberRecovered;
    }

    public static int getNumberIsolating() {
        return numberIsolating;
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
    
    public static void resetCounts(){
        contactCount = 0;
        phonesRegistered = 0;
        numberIsolating = 0;
        numberInfected = 0;
        numberRecovered = 0;
    }
    
    public static String report(){
        return contactCount + " contacts counted in run of Phone\n"
                + phonesRegistered + " Phones tried to register\n"
        

    + probabilityInfectedIfInContact*100 + "% chance of infection per contact\n"
    + probabilitySelfisolatesIfAlerted*100 + "% isolate";
    }

    private void pause(long ms){
        try { Thread.sleep(ms); } 
        catch (InterruptedException ex) { /* ignore exception */}
    }
}
