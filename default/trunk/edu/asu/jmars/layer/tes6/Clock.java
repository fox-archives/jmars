package edu.asu.jmars.layer.tes6;

import java.util.EventListener;
import javax.swing.event.EventListenerList;
import edu.asu.jmars.layer.tes6.ClockEvent;
import edu.asu.jmars.layer.tes6.ClockListener;

public class Clock {
	public Clock(String name, long freqInMilliSeconds){
		clockRef = new ClockRef(name, freqInMilliSeconds);
		clockRef.start();
	}
	
	public void addClockListener(ClockListener l){
		clockRef.addClockListener(l);
	}
	
	public void removeClockListener(ClockListener l){
		clockRef.removeClockListener(l);
	}
	
	private ClockEvent makeClockEvent(long time){
		return new ClockEvent(this, System.currentTimeMillis());
	}
	
	private ClockRef clockRef;
	
	
	
	public class ClockRef extends Thread {
		public ClockRef(String name, long freqInMilliSeconds){
			super(name);
			setDaemon(true);
			this.freq = freqInMilliSeconds;
			listenerList = new EventListenerList();
		}
		
		public void addClockListener(ClockListener l){
	        listenerList.add(ClockListener.class, l);
		}
		
		public void removeClockListener(ClockListener l){
			listenerList.remove(ClockListener.class, l);
		}
		
		public void run(){
			while(true){
				try {
					EventListener[] listeners = 
						listenerList.getListeners(ClockListener.class);
					ClockEvent evt = null;
					
					for(int i = 0; i < listeners.length; i++){
						if (evt == null){
							// The containing object should be the source of the
							// event as compared to this object.
							//evt = new ClockEvent(this, System.currentTimeMillis());
							evt = makeClockEvent(System.currentTimeMillis());
						}
						
						((ClockListener)listeners[i]).clockEventOccurred(evt);
					}
				}
				catch(Exception ex){
					System.err.println(ex);
				}
				
				try {
					Thread.sleep(this.freq);
				}
				catch(InterruptedException ex){}
			}
		}
		
		long freq;
		EventListenerList listenerList;
	}
}