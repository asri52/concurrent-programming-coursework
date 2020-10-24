/*
 * DayCounter.java is a class that extends Thread.
 * It's task is to run in the background and simulate the passage of days.
 * The simulation rate is that increase theDay once every msPerDay of System time
 */
package cw2020;

/**
* @author DAVID
 */
public class DayCounter extends Thread {
    private int theDay;
    private long msPerDay;
    
    public DayCounter(long ms){
        theDay = 0;
        msPerDay = ms;
        this.setDaemon(true);
    }
    
    public int getTheDay(){
        return theDay;
    }
    
    @Override public void run(){
        while(true){
            pause(msPerDay);
            theDay++;
        }        
    }
    
    @Override public String toString(){
        return "" + theDay;
    }
    
    private void pause(long ms){ /* convenience method to keep main code tidier */
        try { Thread.sleep(ms);
        } catch (InterruptedException ex) { /* ignore */}
    }
}
