/*
 * Populatio.java is a class that extends Thread, so as to run independely of the GUI.
 * It maintains a list of people in the population.
 * Its run task is to randomly simulate contacts between People in the Population.

 * fractionRegistered is a parameter that can be set to represent the fraction 
 * of people in the population who will register with the Track-and-Trace system
 * 
 * fractionInitiallyPositive is a parameter that can be set to represent the fraction 
 * of people in the population who has COVID at the start of the simulation
 */
package cw2020;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @author DAVID
 */
public class Population extends Thread {
    private int populationSize;
 
    
  
    private ArrayList<Person> phones;
    private boolean active;
    private Website theWebsite;
    private static Random numberGenerator = new Random();
    
    //executorservice
    ExecutorService executor;
    
    
    private double fractionRegistering = 1.0;
    private double fractionInitiallyPositive = 0.0;
    //TODO:Reentrantlock
    private int connectionCount;
    private int numberContactsPerHour;

    public Population(int ps, Website w) {
        this.populationSize = ps;
        this.theWebsite = w;
        this.phones = new ArrayList();
        this.setDaemon(true);
        Person.resetCounts();
        active = true;
        /* Estimate approx number of contacts per hour in population 
         * assume each person has 12 contacts per day */ 
        numberContactsPerHour = 10*populationSize/24;
        
        //initialising executorservice for person threads
        executor = Executors.newFixedThreadPool(ps);
     
    }
    
    @Override public void run(){
        while(active){
            for(int n = 0; n < numberContactsPerHour ; n++){
                Person phone1 = getRandomPhone();
                Person phone2 = getRandomPhone();
                if(phone1 == phone2  || phone1.isIsolating() || phone2.isIsolating()) continue;
                phone1.contactWith(phone2);
                phone2.contactWith(phone1);
                connectionCount +=2;
            }
            
            pause(40L); /* set to approx 1 hour in system time 1000ms/24 =approx 40ms per hour */ 
        }
    }
    
    public void populate(){
        for(int i = 0; i< populationSize; i++){
            boolean toRegister = (Math.random() < fractionRegistering);
            boolean isInfected = (Math.random() < fractionInitiallyPositive);
            Person p = new Person(theWebsite, toRegister, isInfected);
            
            //not storing futures, only making use of cancellability
            executor.submit(p);
            phones.add(p);
        }
        
//        phones.forEach((p) -> {
//            p.start();
//        });
    }
    
    public void shutdown(){
//        phones.forEach((p) -> {
//            p.stop();
//        });
        executor.shutdown();
        //wait until all threads are finished.
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                // we have waited long enough just shut down all threads now
                executor.shutdownNow();
                System.out.println("I am not going to wait any longer");
            }
        } catch (InterruptedException ie) {
            // don't wait any longer just shut down all threads now
            executor.shutdownNow();
            System.out.println("I am not going to wait any longer");
        }
    }

    public Website getTheWebsite() {
        return theWebsite;
    }

    public ArrayList<Person> getPhones() {
        return phones;
    }
    
    public int numberInfected(){
        int count = 0;
        for(Person p: phones) if(p.isInfected()) count++;
        return count;
    }
    
    public int numberIsolating(){
        int count = 0;
        for(Person p: phones) if(p.isIsolating()) count++;
        return count;
    }

    public void setPopulationSize(int populationSize) {
        this.populationSize = populationSize;
    }
    
    public int getPopulationSize() {
        return populationSize;
    }

    public int getConnectionCount() {
        return connectionCount;
    }
       
    public Person getRandomPhone(){
        return phones.get(numberGenerator.nextInt(populationSize));
    }
    
    public String report(){
        return numberInfected() + " infected and "
                + numberIsolating() + " self isolating "
                + "in population of " + populationSize + "\n"
                + fractionRegistering*100 + "% register\n"
                + fractionInitiallyPositive*100 + "% initially positive\n";
    }
    
    public void setFractionRegistering(double frac) {
        fractionRegistering = frac;
    }
    
    public void setFractionInitiallyPositive(double frac) {
        fractionInitiallyPositive = frac;
    }
    
    private void pause(long ms){ /* convenience method to keep main code tidier */
        try { Thread.sleep(ms);
        } catch (InterruptedException ex) { /* ignore */}
    }
}
