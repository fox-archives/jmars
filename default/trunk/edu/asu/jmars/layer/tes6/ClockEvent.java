package edu.asu.jmars.layer.tes6;

import java.util.EventObject;

public class ClockEvent extends EventObject {
	/**
	 * eclipse complained
	 */
	private static final long serialVersionUID = 1L;

	public ClockEvent(Object source, long time){
		super(source);
		this.time = time;
	}
	
	public long getTimeAtEvent(){
		return time;
	}
	
	private long time;
}
