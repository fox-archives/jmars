package edu.asu.jmars.layer.tes6;

import java.util.EventListener;
import edu.asu.jmars.layer.tes6.ClockEvent;

public interface ClockListener extends EventListener {
	public void clockEventOccurred(ClockEvent evt);
}
