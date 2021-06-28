/*
 *
 */
import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.io.Serializable;

/**
 * Non-visual Bean used to manage a Bus object with reduced capacity.
 * The Class will pose a veto if the new number of passengers exceeds the 
 *  reduced capacity.
 * 
 * @see Bus
 * @author Marco Costa
 */
public class CovidController implements VetoableChangeListener, Serializable {
    
    private final int reducedCapacity;
    private Bus bus;
    
    private void registerToBusListener() {
        this.bus.addVetoableChangeListener(this);
    }
    
    public CovidController(Bus bus) {
        this.reducedCapacity = 25;
        this.bus = bus;
        registerToBusListener();
    }

    public CovidController(int reducedCapacity, Bus bus) {
        this.reducedCapacity = reducedCapacity;
        this.bus = bus;
        registerToBusListener();
    }
    
    
    public void destroy() {
        bus.removeVetoableChangeListener(this);
    }
    
    /**
     * Throws a PropertyVetoException if the number of passengers exceeds the
     *  reduced capacity.
     * 
     * @param arg0 the new event
     * @throws PropertyVetoException if the number of passengers exceeds the
     *                               reduced capacity
     */
    @Override
    public void vetoableChange(PropertyChangeEvent arg0) throws PropertyVetoException {
        if (arg0.getPropertyName().equals("numPassengers") && ((int) arg0.getNewValue()) > reducedCapacity) 
            throw new PropertyVetoException("The value exceeds the reduced capacity", arg0);
    }
    
}
