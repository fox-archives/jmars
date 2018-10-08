package edu.asu.jmars.layer.tes6;


import java.util.*;
import javax.swing.event.EventListenerList;




class RunningStateCoordinator {
    protected RunningStateCoordinator(){
        listenerList = new EventListenerList();
    }

    public static RunningStateCoordinator getInstance(){
        return new RunningStateCoordinator();
    }

    public void fireDbReqStartEvent(Object source){
        fireRunningStateChangeEvent(
            source,
            RunningStateChangeEvent.SCOPE_DB_IO,
            RunningStateChangeEvent.EVENT_START);
    }

    public void fireDbReqEndEvent(Object source){
        fireRunningStateChangeEvent(
            source,
            RunningStateChangeEvent.SCOPE_DB_IO,
            RunningStateChangeEvent.EVENT_END);
    }

    public void fireRunningStateChangeEvent(Object source, int scope, int event){

        RunningStateChangeEvent e = null;
        Object[] listeners = listenerList.getListenerList();

        for(int i = 0; i < listeners.length; i+=2){
            if (listeners[i] == RunningStateChangeListener.class){
                if (e == null){
                    e = new RunningStateChangeEvent(source, scope, event);
                }

                RunningStateChangeListener l = (RunningStateChangeListener)listeners[i+1];
                l.runningStateChanged(e);
            }
        }
    }

    /**
     * Registers a {@link RunningStateChangeListener}.
     */
    public void addRunningStateChangeListener(RunningStateChangeListener l){
        listenerList.add(RunningStateChangeListener.class, l);
    }

    /**
     * Unregisters a {@link RunningStateChangeListener}.
     */
    public void removeRunningStateChangeListener(RunningStateChangeListener l){
        listenerList.remove(RunningStateChangeListener.class, l);
    }


    private EventListenerList listenerList;
}

