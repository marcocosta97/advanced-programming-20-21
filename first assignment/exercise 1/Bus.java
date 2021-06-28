/*
 * 
 */
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.beans.VetoableChangeSupport;
import java.io.Serializable;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Non-visual Bean simulating a Bus object with its own capacity and 
 *  random decreasing of the number of passengers.
 * The class provides the interfaces to register a VetoableChangeListener
 *  (which can possibly pose a veto over the increase/decrease of the number
 *   of passengers) and a PropertyChangeListener (which will be informed about
 *   the change of the number of passengers and if the doors are open or not)
 * 
 * @author Marco Costa
 */
public class Bus implements Serializable{
    /* random decreasing each 20 seconds */
    private static long ACTIVATE_LOOP_TIME = TimeUnit.SECONDS.toMillis(20);
    
    private final int capacity;
    private boolean doorOpen = false;
    private int numPassengers;
    
    private PropertyChangeSupport changes = new PropertyChangeSupport(this);
    private VetoableChangeSupport vetos = new VetoableChangeSupport(this);
    
    private ReentrantLock lock = new ReentrantLock();
    
    private static Logger logger = Logger.getLogger(Bus.class.getName());
    
    /**
     * Constructor with default capacities.
     */
    public Bus () {
        this.capacity = 50;
        this.numPassengers = 20;
    }

    public Bus(int capacity, int numPassengers) {
        this.capacity = capacity;
        this.numPassengers = numPassengers;
    }
    
    /**
     * Sets the bus doors as open if they are currently closed or viceversa.
     */
    private void toggleDoorOpen() {
        try {
            vetos.fireVetoableChange("doorOpen", doorOpen, doorOpen);
            doorOpen = !doorOpen;
            changes.firePropertyChange("doorOpen", !doorOpen, doorOpen);
            
            if (doorOpen) logger.info("Door opened");
            else logger.info("Door closed");
            
        } catch (PropertyVetoException ex) {
            Logger.getLogger(Bus.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Increases (or decreases as well) the number of passengers on the Bus if
     *  not blocked by a veto.
     * Note, the access over the `numPassengers` property is protected by a Lock.
     * 
     * @param value the number of passengers to add or decrease
     */
    private void increaseNumPassengers(int value)  {
        lock.lock();
        try {            
            vetos.fireVetoableChange("numPassengers", numPassengers, numPassengers + value);
            toggleDoorOpen(); // opening the doors
            Thread.sleep(2000);
            numPassengers += value;
            changes.firePropertyChange("numPassengers", numPassengers + value, numPassengers);
            logger.log(Level.INFO, "Number of passengers modified. Previous value: {0}, new value: {1}", 
                    new Object[]{numPassengers - value, numPassengers});
            Thread.sleep(1000);
            toggleDoorOpen(); // closing the doors
        } catch (PropertyVetoException ex) { 
            logger.log(Level.INFO, "The method has been blocked by a veto");
        } catch (InterruptedException ex) {
            Logger.getLogger(Bus.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            lock.unlock(); /* finally release the lock */
        }
    }
    
    /**
     * Decreases randomly the number of passengers each `ACTIVATE_LOOP_TIME`
     * seconds.
     */
    public void activate () {
        /**
         * If `increaseNumPassengers` and the `activate` cycle loop
         *  happen at "near" the same time (particularly when an `activate`
         *  should occurr during an external called `increaseNumPassengers` which
         *  is possibly inside a Thread.sleep()) this would cause all the routines to "shift", losing the possibility
         *  to have the executions correctly timed
         * For this reason the `activate` loop is moved on another thread
         *  and the access to `increaseNumPassengers` regulated by means of a
         *  ReentrantLock (since `activate` calls itself `increaseNumPassengers`)
         *  and IF the lock is currently locked (so an execution of 
         *  `increaseNumPassengers` is currently happening) we SKIP the 
         *  random decreasing
         */
        new Thread(() -> {
            try {
                Random r = new Random();
                
                for (; ;)
                {
                    Thread.sleep(ACTIVATE_LOOP_TIME);
                    if (lock.tryLock()) /* does not hold on the lock if busy */
                    {
                        try {
                            if (numPassengers > 0) 
                            {
                                int decreaseBy = r.nextInt(numPassengers / 2) + 1;   
                                increaseNumPassengers(-decreaseBy); 
                            }
                        }
                        finally {
                            lock.unlock();
                        }
                    }
                    /* else skipping this "slot" */
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(Bus.class.getName()).log(Level.SEVERE, null, ex);
            }
        }).start();
    }
    
    /**
     * Asks the Bus object to increase the number of passengers if allowed
     *  by the VetoableListeners and if not exceeding the Bus capacity.
     * 
     * @param increaseBy the number of passengers to add
     */
    public void setNumPassenger(int increaseBy) {
        if (increaseBy <= 0) throw new IllegalArgumentException("The value must be > 0");
        if (numPassengers + increaseBy > capacity) throw new IllegalArgumentException("The value exceeds the bus capacity");

        increaseNumPassengers(increaseBy);
    }
    
    /**
     * Returns the current number of passengers.
     * 
     * @return the current number of passengers
     */
    public int getNumPassenger() {
        return numPassengers;
    }
    
    public void addPropertyChangeListener(PropertyChangeListener l) {
        changes.addPropertyChangeListener(l);
    }
    
    public void removePropertyChangeListener(PropertyChangeListener l) {
        changes.removePropertyChangeListener(l);
    }
    
    public void addVetoableChangeListener(VetoableChangeListener l) {
        vetos.addVetoableChangeListener(l);
    }
    
    public void removeVetoableChangeListener(VetoableChangeListener l) {
        vetos.removeVetoableChangeListener(l);
    }
    
}
