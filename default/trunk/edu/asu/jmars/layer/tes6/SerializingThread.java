package edu.asu.jmars.layer.tes6;

import java.util.LinkedList;

/**
 * Implementation of a generic thread which runs the
 * added runnables in a serialized fashion.
 * 
 * Usage:
 * 1) Create an instance of this thread and start it.
 * 2) Add new tasks to it using its add(request) method.
 *    The requests are Runnable objects.
 * 
 * Thread's run method catches and absorbs all the
 * Exceptions thrown in running a request.
 * 
 * @author saadat
 *
 */
public class SerializingThread extends Thread {
	public SerializingThread(String name){
		super(name);
		
		outstanding = new LinkedList();
		busy = false;
	}
	
    public synchronized void add(Runnable req){
        // Add the request to the outstanding requests queue.
        outstanding.add(req);

        // Signal the worker thread to pickup this request and run with it.
        notify();
    }

    private synchronized Runnable getNextRequest(){
    	busy = false;
        while(outstanding.size() == 0){
            try { wait(); }
            catch(InterruptedException ex){}
        }
        busy = true;
        return (Runnable)outstanding.removeFirst();
    }
    
    public synchronized int getOutstandingRequestsCount(){
    	return outstanding.size();
    }
    
    public synchronized boolean isIdle(){
    	return (!busy && outstanding.size() == 0);
    }
    
	public void run(){
		while(true){
			req = getNextRequest();
			try { req.run(); }
			catch(Exception ex){ System.err.println(ex); }
			req = null;
		}
	}
	
	Runnable req;
	LinkedList outstanding;
	boolean busy;
}
